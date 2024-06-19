package org.scictrl.csshell.epics.server;

import org.apache.commons.configuration.HierarchicalConfiguration;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>ValueProcessor interface.</p>
 *
 * @author igor@scictrl.com
 */
public interface ValueProcessor {

	/**
	 * Configures the internals of value processor, provides reference to parent record and to configuration
	 * with root inside processor tag.
	 * Parent record is not yet initialized, so does not have reference to Database,
	 * the Database reference will be available during activate call.
	 *
	 * @param record the parent record.
	 * @param config configuration with room inside processor tag.
	 */
	public void configure(Record record, HierarchicalConfiguration config);
	
	/**
	 * Sets new value to the processor.
	 * If value is different from current processor value, then event is fired.
	 * Timestamp is updated regardless if value is changed, since timestamp indicates last value update or confirmation.
	 *
	 * @param value a {@link java.lang.Object} object
	 */
	public void setValue(Object value);
	/**
	 * <p>getValue.</p>
	 *
	 * @return a {@link java.lang.Object} object
	 */
	public Object getValue();
	
	/**
	 * Timestamp indicates last time value has been updated or confirmed. This does not necessary means value change.
	 *
	 * @return a {@link gov.aps.jca.dbr.TimeStamp} object
	 */
	public TimeStamp getTimestamp();
	/**
	 * Return trigger interval in milliseconds, which indicates on which interval this processor wants to be processed by
	 * Database. If trigger is 0 or less, then it means that no processing is required.
	 *
	 * @return update interval in milliseconds or disabled if 0.
	 */
	public long getTrigger();
	/**
	 * Called by Database in interval set by the trigger parameter in milliseconds. Not called if trigger in 0.
	 */
	public void process();
	/**
	 * Record has now reference to Database.
	 * processor might want to connect to other records on this server or PVs on other servers.
	 * This can not be done until all records from configuration has been loaded.
	 * Call to this method signals to the processor that records has been loaded and linking to other values
	 * can be commenced.
	 */
	public void activate();

	/**
	 * <p>getName.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getName();
	
	/**
	 * <p>getRecord.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public Record getRecord();
	
	/**
	 * Returns preferred type of the processor.
	 * Alternatively the processor can inspect actual type of parent record
	 * and adopt.
	 * This type will overwrite type from record, when record will be initialized with this processor.
	 *
	 * @return the type which processor works with, possibly same as associated record.
	 */
	public DBRType getType();
}
