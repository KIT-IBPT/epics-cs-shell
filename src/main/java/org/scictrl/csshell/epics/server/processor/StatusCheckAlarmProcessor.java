/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>StatusCheckAlarmProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class StatusCheckAlarmProcessor extends DefaultAlarmProcessor {

	
	private Severity alarmSeverity;
	private Status alarmStatus;
	private long maskOn;
	private long maskOff;
	
	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;

	/**
	 * <p>Constructor for StatusCheckAlarmProcessor.</p>
	 */
	public StatusCheckAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		
		alarmSeverity= Severity.forName(config.getString("alarmSeverity", Severity.NO_ALARM.getName()));
		alarmStatus= Status.forName(config.getString("alarmStatus", Status.NO_ALARM.getName()));

		String mask= config.getString("maskOn", "0b11111111111111111111111111111111"); 
		if (mask.startsWith("0b")) {
			maskOn= Long.parseLong(mask.substring(2), 2);
		} else {
			maskOn = Long.parseLong(mask);
		}

		mask= config.getString("maskOff", "0"); 
		if (mask.startsWith("0b")) {
			maskOff= Long.parseLong(mask.substring(2), 2);
		} else {
			maskOff = Long.parseLong(mask);
		}
		
		
		String[] names= config.getStringArray("input.links");
		
		String type= Record.toPropertyName(config.getString("input.type",Record.PROPERTY_VALUE));
		
		if (names!=null && names.length>0) {
			this.input= new ValueLinks(record.getName(), names, this, type);
			update(true, Severity.INVALID_ALARM, Status.UDF_ALARM,false);
		}			

		
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();

		if (input!=null) {
			input.activate(getRecord().getDatabase());
		}
	
	}
	
	/**
	 * <p>Getter for the field <code>alarmSeverity</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.Severity} object
	 */
	public Severity getAlarmSeverity() {
		return alarmSeverity;
	}
	
	/**
	 * <p>Getter for the field <code>alarmStatus</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.Status} object
	 */
	public Status getAlarmStatus() {
		return alarmStatus;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
		
		if (input==evt.getSource()) {
			updateAlarmStatus();
		}

	}

	/** {@inheritDoc} */
	@Override
	public synchronized void updateAlarmStatus() {

		super.updateAlarmStatus();
		
		if (!input.isReady()) {
			return;
		}

		Severity newSeverity = Severity.NO_ALARM;
		Status newStatus = Status.NO_ALARM;
		boolean alarm=false;

		if (input.isInvalid()) {

			newSeverity = Severity.INVALID_ALARM;
			newStatus = Status.LINK_ALARM;
			alarm=true;
			
		} else {

			ValueHolder[] vh= input.consume();
			
			for (int i = 0; i < vh.length; i++) {
				if (newSeverity.isLessThan(vh[i].severity)) {
					newSeverity = vh[i].severity;
					newStatus = vh[i].status;
					break;
				}
				
				long status= 0L;
				
				if (vh[i].value.getClass().isArray()) {
					status= vh[i].longValue();
				} else {
					status= ((Number)vh[i].value).longValue();
				}
					
				long test= maskOn & status;
				if (test>0) {
					newSeverity=alarmSeverity;
					newStatus=alarmStatus;
					alarm=true;
				} else {
					status= ~status;
					test= maskOff & status;
					status= ~status;
					if (test>0) {
						newSeverity=alarmSeverity;
						newStatus=alarmStatus;
						alarm=true;
					}					
				}
				
				//System.out.println("UPDATE "+(System.currentTimeMillis()%10000)+" "+status+" "+test+" "+alarm+" "+newSeverity);
			}
		}
		
		//System.out.println("UPDATE "+(System.currentTimeMillis()%10000)+" "+alarm+" "+newSeverity);

		update(alarm,newSeverity,newStatus,true);
			
	}

}
