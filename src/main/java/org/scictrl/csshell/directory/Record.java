/**
 * 
 */
package org.scictrl.csshell.directory;


/**
 * <p>Record class.</p>
 *
 * @author igor@scictrl.com
 */
public class Record {
	
	/** Constant <code>EPICS_CONNECTION_TYPE="EPICS"</code> */
	public static final String EPICS_CONNECTION_TYPE="EPICS";
	/** Constant <code>ACS_CONNECTION_TYPE="ACS"</code> */
	public static final String ACS_CONNECTION_TYPE="ACS";
	/**
	 * Type.
	 */
	public static enum EntityType {
		/**
		 * Device
		 */
		DEVICE, 
		/**
		 * Channel
		 */
		CHANNEL};

	private String name;
	private String alias;
	private String connectionType;
	private EntityType entityType;
	private String entityClass;
	
	
	/**
	 * <p>Constructor for Record.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param alias a {@link java.lang.String} object
	 * @param connectionType a {@link java.lang.String} object
	 */
	public Record(String name, String alias, String connectionType) {
		super();
		this.name = name;
		this.alias = alias;
		this.connectionType = connectionType;
		this.entityType= connectionType == ACS_CONNECTION_TYPE ? EntityType.DEVICE : EntityType.CHANNEL;
	}

	/**
	 * <p>Constructor for Record.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param alias a {@link java.lang.String} object
	 * @param connectionType a {@link java.lang.String} object
	 * @param entityType a {@link org.scictrl.csshell.directory.Record.EntityType} object
	 * @param entityClass a {@link java.lang.String} object
	 */
	public Record(String name, String alias, String connectionType, EntityType entityType, String entityClass) {
		this(name,alias,connectionType);
		this.entityType= entityType;
		this.entityClass=entityClass;
	}

	/**
	 * The alternative name for this remote entity, if it has one.
	 *
	 * @return the alternative name if device has one
	 */
	public String getAlias() {
		return alias;
	}
	
	/**
	 * The connection type for this remote entity.
	 *
	 * @return connection type for this remote entity
	 */
	public String getConnectionType() {
		return connectionType;
	}

	/**
	 * Possibly unique name of remote entity
	 *
	 * @return the name of remote entity
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * The representation class of entity. This might be device type if entity is device or data type if entity is channel.
	 *
	 * @return the representation class of entity
	 */
	public String getEntityClass() {
		return entityClass;
	}
	
	/**
	 * The type of entity, possible two values: DEVICE or CHANNEL.
	 *
	 * @return the type of entity, possible two values: DEVICE or CHANNEL
	 */
	public EntityType getEntityType() {
		return entityType;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Returns short string representation of record.
	 */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		
		sb.append("Record::{");
		sb.append(name);
		sb.append(",");
		sb.append(alias);
		sb.append(",");
		sb.append(connectionType);
		sb.append("}");
		
		return sb.toString();
	}
	
	
}
