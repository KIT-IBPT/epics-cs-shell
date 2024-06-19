/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>DefaultAlarmProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class DefaultAlarmProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	class SupressionUpdater extends Thread {
		@Override
		public synchronized void run() {
			while (record!=null && record.getDatabase().isActive() && !supressTimeDisabled) {
				if (armed!=null) {
					long t= armed+supressTime+1-System.currentTimeMillis();
					if (t>0) {
						try {
							this.wait(t);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						try {
							this.wait(waitTime());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						this.wait(waitTime());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (record==null || !record.getDatabase().isActive()) {
					return;
				}
				try {
					updateAlarmStatus();
				} catch (Exception e) {
					log.error("Update failed: "+e.toString(), e);
				}
			}
		}
		
		private long waitTime() {
			if (record!=null && record.isAlarmUndefined()) {
				return 10000L;
			}
			return 0L;
		}
		
		public synchronized void triggerUpdate() {
			this.notifyAll();
		}
	}

	
	/**
	 * Creates new instance of DefaultAlarmProcessor and initializes it with Record from provided parameters.
	 *
	 * @param name the name of the record/processor
	 * @param description the description of the record/processor
	 * @param value the alarm value, high or low
	 * @param fixed if value is fixed
	 * @param conf configuration which could include parameters gate.mask and gate.link.
	 * @return new instance of DefaultAlarmProvider, configured with own Record
	 */
	public static final DefaultAlarmProcessor newProcessor(String name, String description, boolean value, boolean fixed, HierarchicalConfiguration conf) {
		
		Record r= new Record(name, DBRType.BYTE, 1);
		
		DefaultAlarmProcessor mvp= new DefaultAlarmProcessor();
		mvp.configure(r,conf);
		r.setProcessor(mvp);
		
		mvp.fixed=fixed;
		try {
			mvp._setValue(value, null, null, false, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mvp;
	}

	
	
	private long gateMask;
	private ValueLinks gateInput;
	private boolean gateOpen=true;
	private boolean gateInvalid=false;
	private Severity outSeverity= Severity.NO_ALARM;
	private Status outStatus= Status.NO_ALARM;
	private Severity gateSeverity= Severity.NO_ALARM;
	private Status gateStatus= Status.NO_ALARM;

	private boolean supressTimeDisabled=false;
	private long supressTime=0L;
	private Long armed=null;
	private SupressionUpdater updater;

	
	/**
	 * <p>Constructor for DefaultAlarmProcessor.</p>
	 */
	public DefaultAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		supressTime= config.getLong("supress_time", 0L);

		
		String mask= config.getString("gate.mask", "0b11111111111111111111111111111111"); 

		if (mask.startsWith("0b")) {
			gateMask= Long.parseLong(mask.substring(2), 2);
		} else {
			gateMask = Long.parseLong(mask);
		}
		
		String[] name= config.getStringArray("gate.link");
		
		if (name!=null && name.length>0) {
			this.gateInput= new ValueLinks(record.getName(), name, this, Record.PROPERTY_VALUE);
			gateOpen=false;
		}
		
	}

	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		if (gateInput==evt.getSource()) {
			
			updateAlarmStatus();
			
		}
		
	}
	
	/**
	 * Updates alarm state. It looks at newSeverity and newStatus fields and sets them to record if necessary.
	 *
	 * @param notify if notification event should be fired from the record.
	 * @return <code>true</code> if alarm status was changed
	 * @param newSeverity a {@link gov.aps.jca.dbr.Severity} object
	 * @param newStatus a {@link gov.aps.jca.dbr.Status} object
	 */
	public boolean updateOrSupress(Severity newSeverity, Status newStatus, boolean notify) {
		
		outSeverity=newSeverity !=null ? newSeverity : Severity.NO_ALARM;
		outStatus=newStatus !=null ? newStatus : Status.NO_ALARM;
		
		Severity newSev= gateInvalid ? Severity.INVALID_ALARM : (gateOpen ? outSeverity : Severity.NO_ALARM);
		Status newSta= gateInvalid ? Status.LINK_ALARM : (gateOpen ? outStatus: Status.NO_ALARM);
		
		if (newSev.isLessThan(gateSeverity)) {
			newSev=gateSeverity;
			newSta=gateStatus;
		}
		
		boolean alarm= newSev.isGreaterThan(Severity.NO_ALARM) || newSta.isGreaterThan(Status.NO_ALARM);

		if (alarm && !supressTimeDisabled && supressTime>0 && updater!=null) {
			// we check time window
			if (armed==null) {
				// this is first event, it is ignored, alarm check is armed 
				armed= System.currentTimeMillis();
				//System.out.println("SUPRESS ARMED "+alarm+" "+supressTime+" "+armed);
				updater.triggerUpdate();
				return false;
			} else if (System.currentTimeMillis()-armed>supressTime) {
				// alarm check is armed and it is outside time window
				//System.out.println("SUPRESS OUT "+alarm+" "+supressTime+" "+(System.currentTimeMillis()-armed));
				record.updateAlarm(newSev, newSta, notify);
				return true;
			}
		} else {
			armed=null;
			record.updateAlarm(newSev, newSta, notify);
			return true;
		}
		
		return false;
	}

	/**
	 * Updates value and alarm values. Alarm values go trough gate filter.
	 *
	 * @param value new alarm value
	 * @param newSeverity new alarm severity
	 * @param newStatus new alarm status
	 * @param notify if listeners of this objects should be notified
	 */
	public void update(boolean value, Severity newSeverity, Status newStatus, boolean notify) {
		// we do not force, since update is "soft" method and indicates that it 
		// should do something only on change
		boolean change= _setValue(value, null, null, false, false);
		change|= updateOrSupress(newSeverity, newStatus, false);
		if (change && notify) {
			record.fireValueChange();
		}
	}

	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (gateInput!=null) {
			gateInput.activate(getRecord().getDatabase());
		}
		
		if (supressTime>0) {
			updater= new SupressionUpdater();
			updater.start();
		}

	}
	
	/**
	 * <p>Alarm processor should check connections and other parameters and update alarm status.</p>
	 * <p>This method should be called by processor itself, for example from PropertyChange update loop.</p>
	 * <p>As well could be called by asynchronous update loop, which does the supress_time handling.</p>
	 */
	public synchronized void updateAlarmStatus() {
		
		//System.out.println("UPDATE "+(System.currentTimeMillis()%10000));
		
		if (gateInput==null) {
			return;
		}
		
		gateSeverity=Severity.NO_ALARM;
		gateStatus=Status.NO_ALARM;

		if (gateInput.isInvalid()) {
			gateSeverity= Severity.INVALID_ALARM;
			gateStatus= Status.LINK_ALARM;
			gateInvalid=true;
			updateOrSupress(outSeverity,outStatus,true);
			return;
		}
		if (!gateInput.isReady()) {
			return;
		}
		
		gateInvalid=false;
		ValueHolder[] vh= gateInput.consume();
		
		Severity sev= record.getAlarmSeverity();
		
		if (sev.isLessThan(vh[0].severity)) {
			gateSeverity = vh[0].severity;
			gateStatus = vh[0].status;
		}

		long rawVal= vh[0].longValue();
		
		if (vh[0].type==DBRType.ENUM) {
			rawVal = 1 << rawVal;
		}
		
		long val= rawVal & gateMask;
		
		gateOpen= val>0;

		//log.debug("Gate: "+Long.toBinaryString(rawVal)+" Mask: "+Long.toBinaryString(gateMask)+" Open:"+gateOpen);
		
		updateOrSupress(outSeverity,outStatus,true);

	}
	
	/**
	 * <p>disableSupressTime.</p>
	 */
	public void disableSupressTime() {
		this.supressTimeDisabled = true;
		updater=null;
	}
	
}
