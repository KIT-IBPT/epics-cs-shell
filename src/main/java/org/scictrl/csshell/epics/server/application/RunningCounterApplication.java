/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.processor.Checks;
import org.scictrl.csshell.epics.server.processor.Checks.Check;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>
 * RunningCounterApplication calculates average of input value updates based on defined time window. Valid values are accepted into calculation, based on strict or relaxed option.
 * </p>
 * 
 * <p>
 * If newest value timestamp is newer than current time minus time interval, then interval is set from newest timestamp. This helps counter if there is time shift between timestamps and local clock and if updates are fairly irregular.
 * If newest recorded timestamp is older than current time minus interval, then it is taken out.
 * </p>
 *
 * @author igor@scictrl.com
 */
public class RunningCounterApplication extends AbstractApplication {
	
	/**
	 * Calculates average and other statistics on predefine time window.
	 * 
	 * @author igor@scictrl.com
	 *
	 */
	public static class AverageCalculator {
		
		/**
		 *  Collected data.
		 */
		public LinkedList<ValueHolder> data= new LinkedList<ValueLinks.ValueHolder>();
		/**
		 * number of data points.
		 */
		public int size;
		/**
		 * Calculated average.
		 */
		public double avg;
		/**
		 * Calculated RMS
		 */
		public double rms;
		/**
		 * Calculated STD
		 */
		public double std;
		/**
		 * Averaging interval.
		 */
		public long interval= 60000; // default 1 minute
		/**
		 * Time span of data.
		 */
		public long span;
		/**
		 * Last data timestamp.
		 */
		public long last;
		
		/**
		 * Adds new value to the data pool.
		 * @param vh value to be added
		 */
		public void add(ValueHolder vh) {
			data.addFirst(vh);
			last=vh.timestamp;
		}

		/**
 		 * Adds new value to the data pool.
		 * @param value value to be added
		 * @param timestamp the timestamp of the value
		 */
		public void add(double value, long timestamp) {
			add(new ValueHolder(null, value, null, null, null, timestamp));
		}
		
		/**
		 * Adds ValueHolder data to the data pool if meets criteria
		 * @param vh the ValueHolder for which data is added
		 * @param strictAlarmFilter if <code>true</code> it will accept only data if there is NO_ALARM status.
		 * 		If <code>false</code> then data is added unless it is INVALID alarm. This is useful, because for MINOR or MAJOR alarms the data is still valid.
		 * @return <code>true</code> if data has been added, otherwise <code>false</code>
		 */
		public boolean addValid(ValueHolder vh, boolean strictAlarmFilter) {
			if (strictAlarmFilter && vh.isAlarm()) {
				return false;
			} else if (Severity.INVALID_ALARM.isLessThanOrEqual(vh.severity)) {
				return false;
			}

			add(vh);
			return true;
		}

		/**
		 * Adds record data to the data pool if meets criteria
		 * 
		 * @param r the Record for which data is added
		 * @param strictAlarmFilter if <code>true</code> it will accept only data if there is NO_ALARM status.
		 * 		If <code>false</code> then data is added unless it is INVALID alarm. This is useful, because for MINOR or MAJOR alarms the data is still valid.
		 * @return <code>true</code> if data has been added, otherwise <code>false</code>
		 */
		public boolean addValid(Record r, boolean strictAlarmFilter) {
			if (strictAlarmFilter && r.isAlarm()) {
				return false;
			} else if (Severity.INVALID_ALARM.isLessThanOrEqual(r.getAlarmSeverity())) {
				return false;
			}

			add(r.getValueAsDouble(),EPICSUtilities.toUTC(r.getTimestamp()));
			return true;
		}
		
		/**
		 * Trims data pool to the predefined time window.
		 */
		public void trim() {
			if (data.isEmpty()) {
				return;
			}
			
			ValueHolder first= data.getFirst();
			long c= System.currentTimeMillis()-interval;
			long t= first.timestamp<c ? c : first.timestamp-interval;
			
			while(data.peekLast().timestamp<t) {
				data.removeLast();
			}
		}
		
		/**
		 * Updates the statistics from data pool.
		 */
		public void update() {
			
			size= data.size();
			
			if (size<1) {
				avg=Double.NaN;
				rms=Double.NaN;
				std=Double.NaN;
				span=0;
			} else {
				avg=0.0;
				rms=0.0;
				std=0.0;
				span=data.peekFirst().timestamp-data.peekLast().timestamp;
			}
			for (ValueHolder vh : data) {
				double d= vh.doubleValue();
				avg+=d;
				rms+=d*d;
			}
			if (size>1) {
				avg/=size;
				rms/=size;
				std=Math.sqrt(Math.abs(rms-avg*avg));
			}
			rms=Math.sqrt(rms);
		}
		
		/**
		 * Clears the data pool.
		 */
		public void clear() {
			data.clear();
			update();
		}
	}
	
	private static final String AVG="AVG";
	private static final String RMS="RMS";
	private static final String STD="STD";
	//private static final String STD_VALID="STD:Valid";
	private static final String STD_RATE="STD:Rate";
	//private static final String STD_RATE_VALIDATED="STD:Rate:Validated";
	private static final String INTERVAL="Interval";
	private static final String SIZE="Size";
	private static final String SPAN="Span";
	private static final String INPUT="Input";
	private static final String INPUT_PV="InputPv";
	private static final String RESET="Reset";
	private static final String LAST_UPDATE = "LastUpdate";
	private static final String LAST_UPDATE_STR = "LastUpdate:Str";
	/**
	 * PV name for value input, to be averaged.
	 */
	private String inputPV;
	private AverageCalculator calc= new AverageCalculator();
	private boolean configurable;
	/**
	 * If true only input values with NO_ALARM are accepted. 
	 */
	private boolean strictAlarmFilter;
	//private double avgCut;
	private SimpleDateFormat format= new SimpleDateFormat("yy-MM-dd' 'HH:mm:ss");
	private Checks<Check> checks;
	private Double nanValue;

	/**
	 * <p>Constructor for RunningCounterApplication.</p>
	 */
	public RunningCounterApplication() {
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		nanValue= config.getDouble("nanValue",null);
		inputPV= config.getString("inputPV");
		strictAlarmFilter= config.getBoolean("strictAlarmFilter",true);
		/**
		 * Time interval in minutes over which average is calculated for input values.
		 */
		double interval= config.getDouble("interval",1.0);
		
		//if (config.getProperty("valid")!=null) {
			checks= Checks.checks(config, "valid");
		//}
		
		
		configurable= inputPV==null;

		/* seems to be not really used */
		//avgCut= config.getDouble("avgCutStdLevel",Double.NEGATIVE_INFINITY);

		addRecordOfMemoryValueProcessor(INPUT_PV, "PV for input", DBRType.STRING, inputPV);
		addRecordOfOnLinkValueProcessor(INPUT, "Input value", DBRType.DOUBLE, inputPV);
		
		addRecordOfMemoryValueProcessor(AVG, "Running average", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(RMS, "Running RMS", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(STD, "Running STD", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(STD_RATE, "Running STD as percent of AVG", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(INTERVAL, "Running interval in minutes", 0.0, 129600.0, "min", (short)0, interval);
		addRecordOfMemoryValueProcessor(SIZE, "Accumulated values", DBRType.INT, 0.0);
		addRecordOfMemoryValueProcessor(SPAN, "Accumulated span in minutes", 0.0, 129600.0, "min", (short)0, 0.0);
		addRecordOfCommandProcessor(RESET, "Clears accumulated data points", 1000);
		//addRecordOfMemoryValueProcessor(STD_VALID, "Valid for AVG above cut level", DBRType.BYTE, 0);
		//addRecordOfMemoryValueProcessor(STD_RATE_VALIDATED, "Valid STD rate, or 0", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(LAST_UPDATE, "Last update timestamp in s", DBRType.INT, 0.0);
		addRecordOfMemoryValueProcessor(LAST_UPDATE_STR, "Last update timestamp", DBRType.STRING, 0.0);
		
		Record r= getRecord(INTERVAL);
		r.setPersistent(true);

		calc.interval=(long)(getRecord(INTERVAL).getValueAsDouble()*60.0*1000.0);
		
		if (configurable) {
			getRecord(INPUT_PV).setPersistent(true);
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();

		if (configurable) {
			((LinkedValueProcessor)getRecord(INPUT).getProcessor()).reconnect(getRecord(INPUT_PV).getValueAsString(), Record.PROPERTY_VALUE);
		}

	}
	
	private synchronized void updateData() {
		calc.trim();
		calc.update();

		if (Double.isNaN(calc.avg) && nanValue!=null) {
			getRecord(AVG).setValue(nanValue);
		} else {
			getRecord(AVG).setValue(calc.avg);
		}
		if (Double.isNaN(calc.rms) && nanValue!=null) {
			getRecord(RMS).setValue(nanValue);
		} else {
			getRecord(RMS).setValue(calc.rms);
		}
		if (Double.isNaN(calc.std) && nanValue!=null) {
			getRecord(STD).setValue(nanValue);
		} else {
			getRecord(STD).setValue(calc.std);
		}
		
		double d= calc.std/calc.avg*100.0;
		getRecord(STD_RATE).setValue(d);
		getRecord(SIZE).setValue(calc.size);
		getRecord(SPAN).setValue(calc.span/60.0/1000.0);
		
		//boolean b= calc.avg>=avgCut;
		//getRecord(STD_VALID).setValue(b);
		//getRecord(STD_RATE_VALIDATED).setValue(b ? d : 0.0);
		
		long l= calc.last;
		String s= format.format(new Date(l));
		getRecord(LAST_UPDATE).setValue((int)(l/1000L));
		getRecord(LAST_UPDATE_STR).setValue(s);
		
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
		
		if (name==INTERVAL) {
			calc.interval=(long)(getRecord(INTERVAL).getValueAsDouble()*60.0*1000.0);
		} else if (name==INPUT_PV) {
			if (configurable) {
				database.schedule(new Runnable() {
					
					@Override
					public void run() {
						((LinkedValueProcessor)getRecord(INPUT).getProcessor()).reconnect(getRecord(INPUT_PV).getValueAsString(), Record.PROPERTY_VALUE);
						reset();
					}
				}, 100);
			}
		} else if (name==INPUT && !alarmOnly) {
			
			Record r= getRecord(INPUT);
			
			if (r.isAlarm()) {
				updateLinkError(r.getAlarmSeverity(),r.getAlarmStatus());
			} else {
				updateLinkError(Severity.NO_ALARM, Status.NO_ALARM);
			}

			if (checks!=null && !checks.check(r.getValueAsDouble())) {
				updateData();
				return;
			}

			calc.addValid(r, strictAlarmFilter);
			updateData();
			
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (RESET==name) {
			boolean invalid= ((LinkedValueProcessor)getRecord(INPUT).getProcessor()).isInvalid();
			log4info("Reset: link valid: '"+!invalid+"' last: '"+getRecord(LAST_UPDATE_STR).getValueAsString()+"'");
			
			if (invalid) {
				database.schedule(new Runnable() {
					
					@Override
					public void run() {
						((LinkedValueProcessor)getRecord(INPUT).getProcessor()).reconnect(configurable ? getRecord(INPUT_PV).getValueAsString() : inputPV, Record.PROPERTY_VALUE);
						reset();
					}
				}, 100);
			} else {
				reset();
			}

		}
	}

	private void reset() {
		synchronized (this) {
			calc.clear();
			updateData();
		}
	}
}
