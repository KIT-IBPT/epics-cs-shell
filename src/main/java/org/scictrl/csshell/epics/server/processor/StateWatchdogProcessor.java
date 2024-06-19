/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>StateWatchdogProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class StateWatchdogProcessor extends DefaultAlarmProcessor {

	private static final int DEFAULT_MONITOR_FAILS = 5;
	private static final byte DEFAULT_MONITOR_RESET_VALUE = 0;
	private static final long DEFAULT_MONITOR_TRIGGER = 1000;
	
	private Severity severityOn;
	private Status statusOn;
	private Severity severityOff;
	private Status statusOff;

	private int monitorFailTreshold;

	private boolean monitorResetValue=false;


	/**
	 * <p>Constructor for StateWatchdogProcessor.</p>
	 */
	public StateWatchdogProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		String severity= config.getString("valueOn.severity", Severity.NO_ALARM.getName());
		String status= config.getString("valueOn.status", Status.NO_ALARM.getName());
		
		severityOn = Severity.forName(severity);
		statusOn= Status.forName(status);
		
		severity= config.getString("valueOff.severity", Severity.NO_ALARM.getName());
		status= config.getString("valueOff.status", Status.NO_ALARM.getName());
		
		severityOff = Severity.forName(severity);
		statusOff= Status.forName(status);
		
		if (config.configurationsAt("monitor").size()>0 || trigger>0) {
			
			monitorFailTreshold= config.getInt("monitor.fails", DEFAULT_MONITOR_FAILS);
			monitorResetValue= config.getByte("monitor.resetValue", DEFAULT_MONITOR_RESET_VALUE)>0;
			if (trigger<1) {
				trigger=DEFAULT_MONITOR_TRIGGER;
			}
		}
		

		this.fixed=false;
		
		_setValue(monitorResetValue,null,null,false,true);
		
	}

	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		super.propertyChange(evt);
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected boolean _setValue(Object value, Severity severity, Status status, boolean notify, boolean force) {
		boolean ch= super._setValue(value, null, null, false,force);
		ch|=update(notify);
		return ch;
	}
	
	/** {@inheritDoc} */
	@Override
	protected boolean _setValue(boolean value, Severity severity, Status status, boolean notify, boolean force) {
		boolean ch= super._setValue(value, null, null, false,force);
		ch|=update(notify);
		return ch;
	}

	/**
	 * <p>update.</p>
	 *
	 * @param notify a boolean
	 * @return a boolean
	 */
	protected boolean update(boolean notify) {
		
		Severity severity;
		Status status;
		if (getValueAsBoolean()) {
			severity=severityOn;
			status=statusOn;
		} else {
			severity=severityOff;
			status=statusOff;
		}

		return updateOrSupress(severity, status, notify);
	}


	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		long lastReset= EPICSUtilities.toUTC(getTimestamp());
		long now= System.currentTimeMillis();

//		if (getName().equals("A:GL:Machine:01:SQLArchiver:ReadError")) {
//			System.out.println("> "+getName()+" "+change+" "+(now-change)+" "+getValueAsBoolean()+" "+record.getAlarmSeverity().getName());
//		}
		
		if (now-lastReset>monitorFailTreshold*trigger) {
			_setValue(monitorResetValue, null,null,true, false);
		}
		
	}
	
}
