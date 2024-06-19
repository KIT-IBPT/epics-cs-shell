/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.lang.reflect.Array;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>SimAlarmProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class SimAlarmProcessor extends MemoryValueProcessor {

	
	private Severity alarmSeverity;
	private Status alarmStatus;

	/**
	 * <p>Constructor for SimAlarmProcessor.</p>
	 */
	public SimAlarmProcessor() {
		type= DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		alarmSeverity= Severity.forName(config.getString("alarmSeverity", Severity.NO_ALARM.getName()));
		alarmStatus= Status.forName(config.getString("alarmStatus", Status.NO_ALARM.getName()));

		if (timestamp==null) {
			_setValue(false, alarmSeverity, alarmStatus, false, true);
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		if (Array.getByte(value,0)==0) {
			_setValue(true,alarmSeverity, alarmStatus, true,true);
		} else {
			
			_setValue(true,Severity.NO_ALARM, Status.NO_ALARM,true,true);
		}
		
	}
}
