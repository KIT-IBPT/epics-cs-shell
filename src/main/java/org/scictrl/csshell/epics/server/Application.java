/**
 * 
 */
package org.scictrl.csshell.epics.server;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * <p>Application interface.</p>
 *
 * @author igor@scictrl.com
 */
public interface Application {
	
	/**
	 * <p>fullRecordName.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	public String fullRecordName(String name);
	
	/**
	 * Configures the internals of this application, provides reference to configuration
	 * with root inside applications tag.
	 * Server structure is not yet initialized, so does not have reference to Database,
	 * the Database reference will be available during activate call.
	 *
	 * @param config configuration with room inside application tag.
	 * @param name a {@link java.lang.String} object
	 */
	public void configure(String name, HierarchicalConfiguration config);
	
	/**
	 * Return configured Record instance with prepared ValueProcessor. Of <code>null</code>
	 * if record with this name does not exist.
	 * By default a full PV name of returned record should consist of name of application with appended name of the record.
	 *
	 * @param name the record name, appended to application name makes full record name (PV name)
	 * @return a record with this name or <code>null</code>
	 */
	public Record getRecord(String name);
	
	/**
	 * Return array of configured Record instances with prepared ValueProcessor.
	 *
	 * @return an array of records
	 */
	public Record[] getRecords();

	/**
	 * Returns array of record names. Important: these are not full PV names, just prefixes valid within context of the application.
	 *
	 * @return array of application relative record names.
	 */
	public String[] getRecordNames();
	/**
	 * Returns true if this application is capable of creating new records when asked to.
	 * Applications which return true, will be asked if they have record, when a EPICS request
	 * with PV name arrives and beginning of PV name matches with application name.
	 *
	 * @return a boolean
	 */
	public boolean isDynamicRecordCreator();

	/**
	 * <p>getName.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public abstract String getName();
	
	/**
	 * <p>initialize.</p>
	 *
	 * @param database a {@link org.scictrl.csshell.epics.server.Database} object
	 */
	public void initialize(Database database);
	
	/**
	 * Application has now reference to Database.
	 * Application might want to connect to other records on this server or PVs on other servers.
	 * This can not be done until all records from configuration has been loaded.
	 * Call to this method signals application that records has been loaded and linking to other values
	 * can be commenced.
	 */
	public void activate();
	
	/**
	 * Activate has been called, application has reference to Database.
	 *
	 * @return if <code>true</code> application has been activated
	 */
	public boolean isActivated();

}
