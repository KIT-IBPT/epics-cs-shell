/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.csstudio.archive.reader.ArchiveReader;
import org.csstudio.archive.reader.ValueIterator;
import org.epics.util.array.ListNumber;
import org.epics.util.time.Timestamp;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.scictrl.csshell.Tools;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.application.RunningCounterApplication.AverageCalculator;

import com.aquenos.csstudio.archive.json.reader.JsonArchiveReaderFactory;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ArrayAssemblyApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class ArrayAssemblyApplication extends AbstractApplication implements Runnable {

	private class Element {
		private int id;
		@SuppressWarnings("unused")
		private String key;
		private String label;
		private ValueLinks link;
		private String pv;
		private AverageCalculator calc;
		private Record error;

		public Element(int id, String key, String pv, String label, long interval) {
			this.id=id;
			this.key=key;
			this.label=label;
			this.pv=pv;
			this.calc= new AverageCalculator();
			this.calc.interval=interval;
			
			if (pv!=null && pv.length()>0) {
				this.link=connectLinks(key, pv);
			}
		}
		
		public ValueHolder getValue() {
			if (link!=null && !link.isInvalid() && link.isReady()) {
				return link.consume()[0];
			}
			return null;
		}
		
		public boolean isValid() {
			return link==null || (!link.isInvalid() && link.isReady() && !link.isLastSeverityInvalid());
		}
		
		public boolean isAlarm() {
			if (!isValid()) {
				return true;
			}
			return link.isLastSeverityHigh();
		}

		public void updateError() {
			if (error==null) {
				return;
			}
			
			if (isValid()) {
				error.updateAlarm(getLastSeverity(), getLastStatus());
			} else {
				error.updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM);
			}
			
		}

		private Status getLastStatus() {
			if (link==null) {
				return Status.NO_ALARM;
			}
			return link.getLastStatus();
		}

		private Severity getLastSeverity() {
			if (link==null) {
				return Severity.NO_ALARM;
			}
			return link.getLastSeverity();
		}

		public void add(ValueHolder vh) {
			calc.add(vh);
		}
		
	}

	private enum ErrorState {
		OK, Warning, Error;
	}

	private static final String VALUES =   "Values";
	private static final String STD =      "STD";
	private static final String AVG =      "AVG";
	private static final String SIZE=      "Size";
	private static final String STATUS =   "Status";
	private static final String SEVERITY = "Severity";
	private static final String ARCH_VALUES = "Arch:Values";
	private static final String ARCH_STD = "Arch:STD";
	private static final String ARCH_AVG = "Arch:AVG";
	private static final String ARCH_TIME = "Arch:Time";
	private static final String GET = "Arch:Get";
	private static final String ERROR_= "Error:";
	private static final String PV_= "PV:";
	private static final String LABEL_= "Label:";
	

	private List<Element> elements;
	private Map<String, Element> key2el;
	private Record values;
	private Record avg;
	private Record std;
	private double[] data;
	private Thread updater;
	private long lastUpdate=0L;
	private long rate=1000;
	private Record size;
	private String archiveLink;
	private ArchiveReader reader;
	private Record status;
	private Record severity;
	
	/**
	 * <p>Constructor for ArrayAssemblyApplication.</p>
	 */
	public ArrayAssemblyApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		archiveLink= config.getString("archive_url".toLowerCase());
		
		rate= (long)(config.getDouble("rate",1.0)*1000.0);
		long interval= (long)(config.getDouble("interval",60.0)*1000.0);
		double min= config.getDouble("min", 0.0);
		double max= config.getDouble("max", 100.0);
		String units= config.getString("units","Misc");
		int precision= config.getInt("precision",2);

		elements= new ArrayList<ArrayAssemblyApplication.Element>();
		key2el= new HashMap<String, ArrayAssemblyApplication.Element>();
		
		int i=0;
		
		while (i<101) {
			String key= "el"+i;
			String kpv= key+".pv";
			String klabel= key+".label";
			
			if (config.containsKey(kpv)) {
				
				String pv= config.getString(kpv);
				String label= config.getString(klabel);
				
				Element el = new Element(i, key, pv, label, interval);
				
				elements.add(el);
				key2el.put(key, el);
				
				i++;

			} else {
				break;
			}
		}
		
		data= new double[elements.size()];
		
		values= addRecordOfMemoryValueProcessor(VALUES, "Values array", min, max, units, (short)precision, new double[elements.size()]);
		std= addRecordOfMemoryValueProcessor(STD, "STD array", min, max, units, (short)precision, new double[elements.size()]);
		avg= addRecordOfMemoryValueProcessor(AVG, "AVG array", min, max, units, (short)precision, new double[elements.size()]);
		size= addRecordOfMemoryValueProcessor(SIZE, "Accumulated values", 0, 100000, "", new int[elements.size()]);
		status= addRecordOfMemoryValueProcessor(STATUS, "Alarm Status", 0, 16, "", new int[elements.size()]);
		severity= addRecordOfMemoryValueProcessor(SEVERITY, "Alarm Severity", 0, 16, "", new int[elements.size()]);
		
		for (int j = 0; j < elements.size(); j++) {
			addRecordOfMemoryValueProcessor(LABEL_+j, "Label", DBRType.STRING, elements.get(j).label);
			addRecordOfMemoryValueProcessor(PV_+j, "PV", DBRType.STRING, elements.get(j).pv);
			elements.get(j).error=addRecordOfMemoryValueProcessor(ERROR_+j, "Error State", new String[]{ErrorState.OK.toString(),ErrorState.Warning.toString(),ErrorState.Error.toString()}, (short)0);
		}
		
		addRecordOfMemoryValueProcessor(ARCH_TIME, "Interval start", DBRType.STRING, Tools.FORMAT_ISO_DATE_NO_T_TIME.format(System.currentTimeMillis()-24*60*60*1000));
		addRecordOfMemoryValueProcessor(ARCH_VALUES, "Values array", min, max, units, (short)precision, new double[elements.size()]);
		addRecordOfMemoryValueProcessor(ARCH_STD, "STD array", min, max, units, (short)precision, new double[elements.size()]);
		addRecordOfMemoryValueProcessor(ARCH_AVG, "AVG array", min, max, units, (short)precision, new double[elements.size()]);
		addRecordOfCommandProcessor(GET, "Get data", 1000);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		updater = new Thread(this, "ArrayAssemblyApplication-"+name);
		updater.start();
	}
	
	/** {@inheritDoc} */
	@Override
	public void run() {
		
		try {
			synchronized (updater) {
				updater.wait();
			}
		} catch (Exception e) {
			//
		}
		
		while(database.isActive()) {
			long t= System.currentTimeMillis();
			long dif= t-lastUpdate;
			if (dif>rate) {
				try {
					pushValueUpdate();
				} catch (Throwable tr) {
					log4error("Update failed: "+tr.toString(), tr);
				}
			
				lastUpdate=System.currentTimeMillis();
			} else {
				dif=Math.abs(dif);
				dif= Math.min(dif, 1000);
				try {
					synchronized (updater) {
						updater.wait(dif);
					}
				} catch (Exception e) {
				}
			}
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		
		Element el= key2el.get(name);
		
		if (el!=null) {

			ValueHolder vh= el.getValue(); 

			if (vh!=null) {
				data[el.id]= vh.doubleValue();
				el.add(vh);
			} else {
				data[el.id]= 0.0;
			}
			
			triggerUpdate();
			
		}
		
	}
	
	
	private void triggerUpdate() {
		if (updater==null) {
			return;
		}
		synchronized (updater) {
			updater.notify();
		}
	}
	
	private void pushValueUpdate() {
		
		double[] d= new double[elements.size()];
		double[] dAvg= new double[elements.size()];
		double[] dStd= new double[elements.size()];
		int[] dSize= new int[elements.size()];
		int[] dStatus= new int[elements.size()];
		int[] dSeverity= new int[elements.size()];

		synchronized (this) {

			System.arraycopy(data, 0, d, 0, data.length);
			
			for (Element el : elements) {
				el.calc.trim();
				el.calc.update();
				dAvg[el.id]= el.calc.avg;
				dStd[el.id]= el.calc.std;
				dSize[el.id]= el.calc.size;
				dStatus[el.id]= el.getLastStatus().getValue();
				dSeverity[el.id]= el.getLastSeverity().getValue();
			}
		}
		
		values.setValue(d);
		std.setValue(dStd);
		avg.setValue(dAvg);
		size.setValue(dSize);
		status.setValue(dStatus);
		severity.setValue(dSeverity);
	
		updateAlarms();
			
	}
	
	private void updateAlarms() {
		
		StringBuilder sb= new StringBuilder(128);
		
		for (Element el : elements) {
			el.updateError();
			if (!el.isValid()) {
				sb.append(' ');
				sb.append(el.pv);
				getRecord(ERROR_+el.id).setValue(ErrorState.Error.ordinal());
			} else if (el.isAlarm()) {
				getRecord(ERROR_+el.id).setValue(ErrorState.Warning.ordinal());
			} else {				
				getRecord(ERROR_+el.id).setValue(ErrorState.OK.ordinal());
			}
		}

		if (sb.length()>0) {
			updateLinkError(true, "Link problem with"+sb.toString());
		} else {
			updateLinkError(false, "OK");
		}
		
		//values.updateAlarm(getRecordErrorSum().getAlarmSeverity(), getRecordErrorSum().getAlarmStatus());
		//avg.updateAlarm(getRecordErrorSum().getAlarmSeverity(), getRecordErrorSum().getAlarmStatus());
		//std.updateAlarm(getRecordErrorSum().getAlarmSeverity(), getRecordErrorSum().getAlarmStatus());
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (GET==name) {
			getData();
		}
	}

	private synchronized void getData() {
		
        try {
        	
        	if (archiveLink==null) {
				log4error("Archive link not configured");
        		return;
        	}
			if (reader == null) {
		        JsonArchiveReaderFactory readerFactory = new JsonArchiveReaderFactory();
				reader = readerFactory.getArchiveReader(archiveLink);
			}
			
			if (reader==null) {
				//updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				log4error("No archive reader");
				return;
			}
			
			String pvVal= values.getName();
			String pvAvg= avg.getName();
			String pvStd= std.getName();
			String timeStr= getRecord(ARCH_TIME).getValueAsString();
			
			if (timeStr==null || timeStr.trim().length()==0) {
				//updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				log4error("No time given");
				return;
			}
			
			Date time= Tools.PARSE_ISO_DATE_NO_T_TIME.parse(timeStr.trim());
			
			if (time==null) {
				//updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				log4error("could not parse time "+timeStr);
				return;
			}
			
			double[] val= new double[elements.size()];
			ValueIterator it= reader.getRawValues(1,pvVal.trim(),Timestamp.of(time),Timestamp.of(time));
			if (it.hasNext()) {
				VType vt= it.next();
				ListNumber ln= ((VNumberArray)vt).getData();
				int i= Math.min(val.length, ln.size());
				for (int j = 0; j < i; j++) {
					val[j]=ln.getDouble(j);
				}
			}
			
			double[] avg= new double[elements.size()];
			it= reader.getRawValues(1,pvAvg.trim(),Timestamp.of(time),Timestamp.of(time));
			if (it.hasNext()) {
				VType vt= it.next();
				ListNumber ln= ((VNumberArray)vt).getData();
				int i= Math.min(avg.length, ln.size());
				for (int j = 0; j < i; j++) {
					avg[j]=ln.getDouble(j);
				}
			}
			
			double[] std= new double[elements.size()];
			it= reader.getRawValues(1,pvStd.trim(),Timestamp.of(time),Timestamp.of(time));
			if (it.hasNext()) {
				VType vt= it.next();
				ListNumber ln= ((VNumberArray)vt).getData();
				int i= Math.min(std.length, ln.size());
				for (int j = 0; j < i; j++) {
					std[j]=ln.getDouble(j);
				}
			}

			getRecord(ARCH_AVG).setValue(avg);
			getRecord(ARCH_STD).setValue(std);
			getRecord(ARCH_VALUES).setValue(val);

        } catch (Exception e) {
			log4error("Retrieving data failed: "+e.toString(), e);
		}
		
	}

}
