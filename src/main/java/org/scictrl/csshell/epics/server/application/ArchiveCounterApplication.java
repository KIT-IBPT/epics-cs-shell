/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.util.Date;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.csstudio.archive.reader.ArchiveReader;
import org.csstudio.archive.reader.ValueIterator;
import org.epics.util.time.Timestamp;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Time;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.scictrl.csshell.Tools;

import com.aquenos.csstudio.archive.json.reader.JsonArchiveReaderFactory;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ArchiveCounterApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class ArchiveCounterApplication extends AbstractApplication {
	
	final class AverageCalculator {
		
		ValueIterator data;
		int size;
		double avg=0.0;
		double rms=0.0;
		double std=0.0;
		double span=0.0;
		
		public AverageCalculator(ValueIterator data) {
			this.data=data;
		}
		
		public void update() throws Exception {
			
			Timestamp start=null;
			Timestamp end=null;
			
			while(data.hasNext()) {
				VType vt= data.next();
				Timestamp t= ((Time)vt).getTimestamp();
				AlarmSeverity a= ((Alarm)vt).getAlarmSeverity();
				double d= ((VNumber)vt).getValue().doubleValue();
				
				if (AlarmSeverity.NONE==a || (!strictAlarmFilter && AlarmSeverity.INVALID!=a)) {
					size++;
					if (start==null) {
						start=t;
					}
					end=t;

					avg+=d;
					rms+=d*d;
				}
			}
			
			if (size>1) {
				avg/=size;
				rms/=size;
				std=Math.sqrt(Math.abs(rms-avg*avg));
				rms=Math.sqrt(rms);
			}
			
			if (end!=null && start!=null) {
				span=(double)(end.getSec()-start.getSec())/60.0;
			}
		}
		
	}
	
	private static final String AVG="AVG";
	private static final String RMS="RMS";
	private static final String STD="STD";
	private static final String STD_RATE="STD:Rate";
	private static final String START="Start";
	private static final String END="End";
	private static final String SIZE="Size";
	private static final String SPAN="Span";
	private static final String INPUT="Input";
	private static final String STATUS="Status";
	private static final String GET="Cmd:Get";
	private static final String STRICT = "StrictAlarms";
	private String archiveLink;
	private ArchiveReader reader;
	private boolean strictAlarmFilter;

	/**
	 * <p>Constructor for ArchiveCounterApplication.</p>
	 */
	public ArchiveCounterApplication() {
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		strictAlarmFilter= config.getBoolean("strictAlarmFilter",false);

		archiveLink= config.getString("archive_url".toLowerCase());
		
		if (archiveLink==null || archiveLink.length()==0) {
			throw new IllegalArgumentException("Configuration has no archive parameter!");
		}
		
		addRecordOfMemoryValueProcessor(STRICT, "Strict alarm filter", DBRType.BYTE, strictAlarmFilter);
		addRecordOfMemoryValueProcessor(AVG, "average", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(RMS, "RMS", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(STD, "STD", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(STD_RATE, "STD as percent of AVG", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(START, "Interval start", DBRType.STRING, Tools.FORMAT_ISO_DATE_NO_T_TIME.format(System.currentTimeMillis()-24*60*60*1000));
		addRecordOfMemoryValueProcessor(END, "Interval end", DBRType.STRING, Tools.FORMAT_ISO_DATE_NO_T_TIME.format(System.currentTimeMillis()));
		addRecordOfMemoryValueProcessor(SIZE, "Accumulated values", DBRType.INT, 0.0);
		addRecordOfMemoryValueProcessor(SPAN, "Accumulated span in minutes", 0.0, 129600.0, "min", (short)0, 0.0);
		addRecordOfMemoryValueProcessor(INPUT, "Input PV", new byte[128]);

		addRecordOfMemoryValueProcessor(STATUS, "Status string", DBRType.STRING, "No data");
		addRecordOfCommandProcessor(GET, "Get data", 1000);
		
		getRecord(INPUT).setPersistent(true);
		getRecord(START).setPersistent(true);
		getRecord(END).setPersistent(true);
		
	}
	
	/**
	 * <p>updateLinkError.</p>
	 *
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 */
	protected void updateLinkError(Severity severity, Status status) {
		
		getRecord(AVG).updateAlarm(severity, status);
		getRecord(RMS).updateAlarm(severity, status);
		getRecord(STD).updateAlarm(severity, status);
		getRecord(STD_RATE).updateAlarm(severity, status);
		
		super.updateLinkError(severity, status, "");
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (STRICT==name) {
			strictAlarmFilter=getRecord(STRICT).getValueAsBoolean();
		}
		
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
			if (reader == null) {
		        JsonArchiveReaderFactory readerFactory = new JsonArchiveReaderFactory();
				reader = readerFactory.getArchiveReader(archiveLink);
			}
			
			if (reader==null) {
				getRecord(STATUS).setValueAsString("FAILED: no link to "+archiveLink);
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}
			
			String pv= getRecord(INPUT).getValueAsString();
			String startStr= getRecord(START).getValueAsString();
			String endStr= getRecord(END).getValueAsString();
			
			if (pv==null || pv.trim().length()==0) {
				getRecord(STATUS).setValueAsString("FAILED: PV link not defined");
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}

			if (startStr==null || startStr.trim().length()==0) {
				getRecord(STATUS).setValueAsString("FAILED: Start time not defined"+archiveLink);
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}
			
			if (endStr==null || endStr.trim().length()==0) {
				getRecord(STATUS).setValueAsString("FAILED: End time not defined"+archiveLink);
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}
			
			Date start= Tools.PARSE_ISO_DATE_NO_T_TIME.parse(startStr.trim());
			
			if (start==null) {
				getRecord(STATUS).setValueAsString("FAILED: Start time parse: "+startStr);
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}
			
			Date end= Tools.PARSE_ISO_DATE_NO_T_TIME.parse(endStr.trim());
			
			if (end==null) {
				getRecord(STATUS).setValueAsString("FAILED: End time parse: "+endStr);
				updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
				return;
			}

			getRecord(STATUS).setValueAsString("BUSY: retrieving data");
			
			long t= System.currentTimeMillis();
			ValueIterator it= reader.getRawValues(1,pv.trim(),Timestamp.of(start),Timestamp.of(end));
			AverageCalculator calc= new AverageCalculator(it);
			calc.update();
			t= System.currentTimeMillis()-t;

			getRecord(AVG).setValue(calc.avg);
			getRecord(RMS).setValue(calc.rms);
			getRecord(STD).setValue(calc.std);
			getRecord(STD_RATE).setValue(calc.avg<0.0000001 ? 0.0 : calc.std/calc.avg*100.0);
			getRecord(SIZE).setValue(calc.size);
			getRecord(SPAN).setValue(calc.span);

			getRecord(STATUS).setValueAsString("DONE: got "+calc.size+" samples in "+(long)(t/1000.0)+" seconds");

			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
        } catch (Exception e) {
			log4error("Retrieving data failed: "+e.toString(), e);
			updateErrorSum(Severity.INVALID_ALARM, Status.CALC_ALARM);
			getRecord(STATUS).setValueAsString("ERROR: internal error "+e.toString());
		}
		
	}
}
