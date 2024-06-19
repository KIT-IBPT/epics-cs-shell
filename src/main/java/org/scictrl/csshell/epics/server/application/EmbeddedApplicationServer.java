/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.server.Database;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.Server;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.CAException;
import gov.aps.jca.dbr.DBRType;

/**
 * <p>EmbeddedApplicationServer class.</p>
 *
 * @author igor@scictrl.com
 */
public class EmbeddedApplicationServer extends AbstractApplication {

	/**
	 * <p>newInstance.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.EmbeddedApplicationServer} object
	 */
	public static final EmbeddedApplicationServer newInstance(String name) {
		
		EmbeddedApplicationServer eas= new EmbeddedApplicationServer(name);
		return eas;
	}

	private Server server;
	private final PropertyChangeSupport recordChangeSupport= new PropertyChangeSupport(this);
	private final PropertyChangeSupport recordWriteSupport= new PropertyChangeSupport(this);
	
	/**
	 * Creates new instance, which is configured with provided name and delimiter.
	 * @param name name of the application, it is used as prefix for all PVs name for application properties.
	 * @param nameDelimiter a PV name delimiter used to bind name prefix with application property suffix.
	 */
	private EmbeddedApplicationServer(String name, String nameDelimiter) {
		super();
		configure(name, nameDelimiter);
	}

	/**
	 * Creates new instance, which is configured with provided name and delimiter.
	 * It uses default name delimiter (:).
	 * @param name name of the application, it is used as prefix for all PVs name for application properties.
	 */
	private EmbeddedApplicationServer(String name) {
		super();
		configure(name, DEFAULT_NAME_DELIMITER);
	}
	
	/**
	 * Collects all created records from this application and starts new server with them.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException server error
	 * @throws org.scictrl.csshell.RemoteException server error
	 * @throws gov.aps.jca.CAException server error
	 */
	public void startServer() throws ConfigurationException, RemoteException, CAException {

		Record[] l= records.values().toArray(new Record[records.values().size()]);
		
		this.server= new Server(true);
		server.getDatabase().addAll(l);
		
		server.activate();
		
	}
	
	/**
	 * Collects all created records from this application and starts new server wth them.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException server error
	 * @throws org.scictrl.csshell.RemoteException server error
	 * @throws gov.aps.jca.CAException server error
	 * @param properties a {@link java.util.Properties} object
	 */
	public void startServer(Properties properties) throws ConfigurationException, RemoteException, CAException {

		Record[] l= records.values().toArray(new Record[records.values().size()]);
		
		this.server= new Server(properties, true);
		server.getDatabase().addAll(l);
		
		server.activate();
		
	}

	/**
	 * The server instance which hosts this application. It is available only after {@link #startServer()} has been called.
	 *
	 * @return server instance which hosts this application
	 */
	public Server getServer() {
		return server;
	}
	
	/**
	 * Returns Database instance which holds this applications's active records.
	 *
	 * @return Database instance which holds this applications's active records
	 */
	public Database getDatabase() {
		return database;
	}
	
	/**
	 * <p>addRecordChangeListener.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void addRecordChangeListener(PropertyChangeListener l) {
		recordChangeSupport.addPropertyChangeListener(l);
	}
	
	/**
	 * <p>addRecordWriteListener.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void addRecordWriteListener(PropertyChangeListener l) {
		recordWriteSupport.addPropertyChangeListener(l);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		recordChangeSupport.firePropertyChange(name, false, true);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		recordWriteSupport.firePropertyChange(name, false, true);
	}
	
	/**
	 * Creates new application record with double data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param min the record field value for minimum
	 * @param max the record field value for maximum
	 * @param units the record field value for units
	 * @param precision the record field value for precision
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, Double min, Double max, String units, short precision, Double value) {
		Record r= addRecordOfMemoryValueProcessor(name, desc, min, max, units, precision, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}
	
	/**
	 * Creates new application record with double data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param min the record field value for minimum
	 * @param max the record field value for maximum
	 * @param units the record field value for units
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, Integer min, Integer max, String units, Integer value) {
		Record r= addRecordOfMemoryValueProcessor(name, desc, min, max, units, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with double data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param min the record field value for minimum
	 * @param max the record field value for maximum
	 * @param units the record field value for units
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, Integer min, Integer max, String units, int[] value) {
		Record r= addRecordOfMemoryValueProcessor(name, desc, min, max, units, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with double array data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param min the record field value for minimum
	 * @param max the record field value for maximum
	 * @param units the record field value for units
	 * @param precision the record field value for precision
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, Double min, Double max, String units, short precision, double[] value) {
		Record r= addRecordOfMemoryValueProcessor(name, desc, min, max, units, precision, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}
	
	/**
	 * Creates new application record with Byte data type, which can be used as boolean record.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param value the record initial value
	 * @return newly created record
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public Record createRecord(String name, String desc, DBRType type, Object... value) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, type, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with Byte data type, which can be used as boolean record.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, boolean value) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, DBRType.BYTE, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with Byte data type, which can be used as boolean record. Record value is fixed.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param value the record initial value
	 * @param fixed if <code>true</code> then record value is fixed and can not be changed
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, boolean value, boolean fixed) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, DBRType.BYTE, fixed, false, new Object[]{value});
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with String array data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, String... value) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, DBRType.STRING, (Object[])value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * <p>createRecord.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param value an array of {@link byte} objects
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public Record createRecord(String name, String desc, byte[] value) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, DBRType.BYTE, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates new application record with Enum data type.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param labels the record field with enum labels
	 * @param value the record initial value
	 * @return newly created record
	 */
	public Record createRecord(String name, String desc, String[] labels, short value) {
		Record r=addRecordOfMemoryValueProcessor(name, desc, labels, value);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}

	/**
	 * Creates record with command processor.
	 *
	 * @param name the record name, together with application name as prefix makes PV name of record.
	 * @param desc the record description
	 * @param timeout timeout for command to return value to 0;
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public Record createRecord(String name, String desc, long timeout) {
		Record r=addRecordOfCommandProcessor(name, desc, timeout);
		if (database!=null) {
			database.addRecord(r);
		}
		return r;
	}
	
	/**
	 * Starts ValueLink connection to provided list of link names.
	 *
	 * @param name the unique name of ValueLink connection
	 * @param linkNames list of link names to be managed by single ValueLinks connection
	 * @see #getLinks(String)
	 * @return a {@link org.scictrl.csshell.epics.server.ValueLinks} object
	 */
	public ValueLinks connectLinks(String name, String... linkNames) {
		return super.connectLinks(name, linkNames);
	}

	/**
	 * Closes the application and shuts down the embedded server.
	 */
	public void shutdown() {
		server.destroy();
	}

}
