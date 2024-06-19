/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.ValueLevelAlarmProcessor;

import gov.aps.jca.dbr.DBRType;

/**
 * This is wrapper around {@link org.scictrl.csshell.epics.server.processor.ValueLevelAlarmProcessor}, it makes alarm and warning levels configurable
 * trough PV records.
 * <p>
 * except for remote link there is no other configuration for this application in XML, all alarm levels are to be provided trough PVs by user,
 * The setting are stored in default persistence store.
 * </p>
 *
 * @author igor@scictrl.com
 */
public class ValueLevelAlarmApplication extends AbstractApplication {
	
	
	/** Constant <code>ALARM_LIMIT_HIGH="alarmLimitHigh"</code> */
	public static final String ALARM_LIMIT_HIGH   = "alarmLimitHigh";
	/** Constant <code>ALARM_LIMIT_LOW="alarmLimitLow"</code> */
	public static final String ALARM_LIMIT_LOW    = "alarmLimitLow";
	/** Constant <code>ALARM="Alarm"</code> */
	public static final String ALARM			  = "Alarm";
	/** Constant <code>ENABLED="Enabled"</code> */
	public static final String ENABLED			  = "Enabled";
	

	private ValueLevelAlarmProcessor alarmProc;

	/**
	 * <p>Constructor for ValueLevelAlarmApplication.</p>
	 */
	public ValueLevelAlarmApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		alarmProc= new ValueLevelAlarmProcessor();
		Record r= Record.forProcessor(getName(), DBRType.BYTE, 1, "Alarm record", alarmProc, config);
		alarmProc.setPrecision(Double.MIN_NORMAL);
		
		addRecord(ALARM, r);
		
		addRecordOfMemoryValueProcessor(ALARM_LIMIT_HIGH, "High alarm limit", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(ALARM_LIMIT_LOW, "Low alarm limit", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor(ENABLED, "alarm enabled", DBRType.BYTE, 1);
		
		getRecord(ALARM_LIMIT_HIGH).setPersistent(true);
		getRecord(ALARM_LIMIT_LOW).setPersistent(true);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (ALARM==name) {
			return;
		} else if (ALARM_LIMIT_HIGH==name) {
			alarmProc.setUpperAlarmLimit(getRecord(ALARM_LIMIT_HIGH).getValueAsDouble());
		} else if (ALARM_LIMIT_LOW==name) {
			alarmProc.setLowerAlarmLimit(getRecord(ALARM_LIMIT_LOW).getValueAsDouble());
		} else if (ENABLED==name) {
			alarmProc.setEnabled(getRecord(ENABLED).getValueAsBoolean());
		}

	}

}
