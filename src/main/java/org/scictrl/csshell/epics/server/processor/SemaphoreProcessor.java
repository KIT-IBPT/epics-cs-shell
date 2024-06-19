/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * Semaphore is a processor which acts as remote process semaphore: it allows only one
 * remote process ID to be registered as active.
 *
 * Remote process sets own ID as string to the semaphore. If semaphore is free,
 * this means string value is empty, it will accept the ID for a minute.
 * Process will host semaphore as long as it will reset it's
 * ID before expires. Setting semaphore to empty string will release semaphore.
 * Process, which ID is set on semaphore, is being allowed to access some resource.
 * Before a process can access such a resource, it must check the semaphore and proceed
 * only if semaphore accepted it's ID.
 *
 * @author igor@scictrl.com
 */
public class SemaphoreProcessor extends MemoryValueProcessor {
	
	private static final String FREE="";
	
	private String id=FREE;
	private long timeout=60000;
	
	/**
	 * <p>Constructor for SemaphoreProcessor.</p>
	 */
	public SemaphoreProcessor() {
		super();
		type=DBRType.STRING;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		timeout=config.getLong("timeout",60000L);
		if (timeout<1000) {
			timeout=1000;
		}
		
		setValue(FREE);
		trigger= 1000;
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized boolean _setValue(Object value, Severity severity, Status status, boolean notify, boolean force) {
		String lock= EPICSUtilities.toString(value, getType(), 1);
		if (lock!=null) {
			lock=lock.trim();
			if (this.value==null) {
				id=lock;
				return super._setValue(lock, severity, status, notify, force);
			} else if (id.equals(lock)) {
				timestamp= new TimeStamp();
				return true;
			} else if (FREE.equals(lock)) {
				id=FREE;
				return super._setValue(FREE, severity, status, notify, force);
			} else if (id.equals(FREE)) {
				id=lock;
				return super._setValue(lock, severity, status, notify, force);
			}
		}
		return false;
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		if (FREE!=id) {
			if (System.currentTimeMillis()-EPICSUtilities.convertTimestamp(timestamp).getMilliseconds()>timeout) {
				_setValue(FREE, null, null, true, true);
			}
		}
	}

}
