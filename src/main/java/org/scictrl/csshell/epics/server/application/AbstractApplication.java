/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Application;
import org.scictrl.csshell.epics.server.Database;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.processor.CommandValueProcessor;
import org.scictrl.csshell.epics.server.processor.EnumValueProcessor;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;
import org.scictrl.csshell.epics.server.processor.OnDemandValueProcessor;
import org.scictrl.csshell.epics.server.processor.OnDemandValueProcessor.ValueProvider;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import tools.BootstrapLoader;

/**
 * <p>AbstractApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class AbstractApplication implements Application, ValueProvider {

	private static final String NO_ALARMS = "No alarms";
	/** Constant <code>LINK_ERROR="Status:LinkError"</code> */
	public static final String LINK_ERROR="Status:LinkError";
	/** Constant <code>LINK_ERROR_STRING="Status:LinkError:String"</code> */
	public static final String LINK_ERROR_STRING = "Status:LinkError:String";
	/** Constant <code>ERROR_SUM="Status:ErrorSum"</code> */
	public static final String ERROR_SUM="Status:ErrorSum";
	
	/** Constant <code>DEFAULT_NAME_DELIMITER=":"</code> */
	public static final String DEFAULT_NAME_DELIMITER = ":";
	/** Constant <code>NAME_DELIMITER="nameDelimiter"</code> */
	public static final String NAME_DELIMITER = "nameDelimiter";
	private static final Map<String, PropertiesConfiguration> stores =new HashMap<String, PropertiesConfiguration>(8) ;

	/**
	 * Logger.
	 */
	protected final Logger log= LogManager.getLogger(this.getClass());


	/**
	 * Creates new PropertiesConfiguration object, which loads and stores properties from configuration file.
	 * Configuration file is assembled from default configuration folder, application name and file name.
	 *
	 * @param appName the application name which is used as folder name within default configuration folder
	 * @param fileName the filename for the configuration within the appName folder
	 * @return new PRopertiesConfiguration object with autosave to a config file.
	 */
	protected static final PropertiesConfiguration getStore(String appName,String fileName) {
		String name=appName+'/'+fileName;
		PropertiesConfiguration store = stores.get(name);
		
		if (store==null) {
			synchronized (stores) {
				store = stores.get(name);
				if (store==null) {
					try {
						store= new PropertiesConfiguration(BootstrapLoader.getInstance().getApplicationConfigFile(appName, fileName));
						store.setAutoSave(true);
						stores.put(name, store);
					} catch (ConfigurationException e) {
						e.printStackTrace();
						LogManager.getLogger(AbstractApplication.class).error("Application "+name+" parameters will not be preserved.", e);
					}
				}
			}
		}
		return store;
	}
	
	/**
	 * <p>store.</p>
	 *
	 * @param store a {@link org.apache.commons.configuration.PropertiesConfiguration} object
	 * @param md a {@link org.scictrl.csshell.MetaData} object
	 */
	protected static final void store(PropertiesConfiguration store, MetaData md) {
		store.setProperty(md.getName()+":"+MetaData.ALARM_MAX, md.getAlarmMax());
		store.setProperty(md.getName()+":"+MetaData.ALARM_MIN, md.getAlarmMin());
		store.setProperty(md.getName()+":"+MetaData.DISPLAY_MAX, md.getDisplayMax());
		store.setProperty(md.getName()+":"+MetaData.DISPLAY_MIN, md.getDisplayMin());
		store.setProperty(md.getName()+":"+MetaData.MAXIMUM, md.getMaximum());
		store.setProperty(md.getName()+":"+MetaData.MINIMUM, md.getMinimum());
		store.setProperty(md.getName()+":"+MetaData.UNITS, md.getUnits());
		store.setProperty(md.getName()+":"+MetaData.WARN_MAX, md.getWarnMax());
		store.setProperty(md.getName()+":"+MetaData.WARN_MIN, md.getAlarmMin());
	}

	/**
	 * <p>restore.</p>
	 *
	 * @param store a {@link org.apache.commons.configuration.PropertiesConfiguration} object
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	protected static final MetaData restore(PropertiesConfiguration store, String name) {
		if (store.containsKey(name+":"+MetaData.MINIMUM)
				|| store.containsKey(name+":"+MetaData.MAXIMUM)
				|| store.containsKey(name+":"+MetaData.DISPLAY_MIN)
				|| store.containsKey(name+":"+MetaData.DISPLAY_MAX)
				|| store.containsKey(name+":"+MetaData.WARN_MIN)
				|| store.containsKey(name+":"+MetaData.WARN_MAX)
				|| store.containsKey(name+":"+MetaData.ALARM_MIN)
				|| store.containsKey(name+":"+MetaData.ALARM_MAX)) {
		
			MetaData md= new MetaDataImpl(
					name, 
					null, 
					store.getDouble(name+":"+MetaData.MINIMUM),
					store.getDouble(name+":"+MetaData.MAXIMUM), 
					store.getDouble(name+":"+MetaData.DISPLAY_MIN), 
					store.getDouble(name+":"+MetaData.DISPLAY_MAX),
					store.getDouble(name+":"+MetaData.WARN_MIN), 
					store.getDouble(name+":"+MetaData.WARN_MAX),
					store.getDouble(name+":"+MetaData.ALARM_MIN), 
					store.getDouble(name+":"+MetaData.ALARM_MAX), 
					null,
					null,
					null, 
					store.getString(name+":"+MetaData.UNITS), 
					null, 
					null, 
					null, 
					null,
					null,
					null,
					null);
			return md;
		} else {
			return null;
		}
	}
	
	/**
	 * Application name, part of PV.
	 */
	protected String name;
	/**
	 * Application records
	 */
	protected Map<String,Record> records;
	/**
	 * Application links
	 */
	protected Map<String,ValueLinks> links;
	/**
	 * <code>true</code> if can create new records after initialization.
	 */
	protected boolean dynamicRecordCreator=false;
	/**
	 * PV name delimiter.
	 */
	protected String nameDelimiter;
	/**
	 * PV database.
	 */
	protected Database database;
	private PropertiesConfiguration store;
	private Record linkErrorRecord;
	private Record linkErrorStringRecord;
	private Record errorSumRecord;
	private Severity sumSeverity=Severity.NO_ALARM;
	private Status sumStatus=Status.NO_ALARM;
	private boolean activated;

	
	private class RecordChangeListener implements PropertyChangeListener {
		
		private String name;

		public RecordChangeListener(String name) {
			this.name=name;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			notifyRecordChange(name, false);
		}
		
	}

	private class RecordAlarmListener implements PropertyChangeListener {
		
		private String name;

		public RecordAlarmListener(String name) {
			this.name=name;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			notifyRecordChange(name, true);
		}
		
	}

	private class RecordWriteListener implements PropertyChangeListener {
		
		private String name;

		public RecordWriteListener(String name) {
			this.name=name;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			notifyRecordWrite(name);
		}
		
	}

	class LinkListener implements PropertyChangeListener {
		
		private String name;

		public LinkListener(String name) {
			this.name=name;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			processLinkChange(name);
		}
		
	}

	/**
	 * <p>Constructor for AbstractApplication.</p>
	 */
	public AbstractApplication() {
		records= new HashMap<String,Record>();
		links= new HashMap<String,ValueLinks>();
	}
	
	/** {@inheritDoc} */
	@Override
	public final String fullRecordName(String name) {
		StringBuilder sb= new StringBuilder(this.name.length()+this.nameDelimiter.length()+name.length());
		sb.append(this.name);
		sb.append(this.nameDelimiter);
		sb.append(name);
		return sb.toString();
	}
	
	private final String format4log(String message, Throwable t) {
		StringBuilder sb= new StringBuilder(256);
		sb.append('[');
		sb.append(name);
		sb.append(']');
		
		if (message!=null) {
			sb.append(' ');
			sb.append(message);
		}
		
		if (t!=null) {
			sb.append(" (");
			sb.append(t.toString());
			sb.append(')');
		}
		
		return sb.toString();
	}
	
	/**
	 * <p>log4debug.</p>
	 *
	 * @param message a {@link java.lang.String} object
	 */
	public final void log4debug(String message) {
		log.debug(format4log(message, null));
	}
	
	/**
	 * <p>log4info.</p>
	 *
	 * @param message a {@link java.lang.String} object
	 */
	public final void log4info(String message) {
		log.info(format4log(message, null));
	}
	
	/**
	 * <p>log4error.</p>
	 *
	 * @param message a {@link java.lang.String} object
	 * @param t a {@link java.lang.Throwable} object
	 */
	public final void log4error(String message, Throwable t) {
		log.error(format4log(message, t),t);
	}

	/**
	 * <p>log4error.</p>
	 *
	 * @param message a {@link java.lang.String} object
	 */
	public final void log4error(String message) {
		log.error(format4log(message, null));
	}

	/**
	 * <p>processLinkChange.</p>
	 *
	 * @param name a name of the registered value link
	 */
	protected void processLinkChange(String name) {
		
		Status sta= Status.NO_ALARM;
		Severity sev= Severity.NO_ALARM;

		StringBuilder sb= new StringBuilder(1024);
		sb.append("Alarms: ");

		ValueLinks[] vls= links.values().toArray(new ValueLinks[links.size()]);
		for (ValueLinks vl : vls) {
			Status st= vl.getLastStatus();
			Severity se= vl.getLastSeverity();
			if (se.isGreaterThan(sev)) {
				sev=se;
				sta=st;
			}
			if (se!=Severity.NO_ALARM) {
				ValueHolder[] vh= vl.consume();
				vl.resetConsumedFlag();
				for (ValueHolder vh1 : vh) {
					if (vh1!=null && vh1.severity!=Severity.NO_ALARM) {
						sb.append(vh1.name);
						sb.append("(");
						sb.append(vh1.severity.getName()).append(",");
						sb.append(vh1.status.getName()).append("), ");
					}
				}
			}
		}
		
		if (sev!=Severity.NO_ALARM) {
			linkErrorRecord.updateAlarm(sev, sta,false);
			linkErrorStringRecord.updateAlarm(sev, sta,false);
			linkErrorRecord.setValue(Boolean.TRUE);
			linkErrorStringRecord.setValue(sb.toString());
		} else {
			linkErrorRecord.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM,false);
			linkErrorStringRecord.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM,false);
			linkErrorRecord.setValue(Boolean.FALSE);
			linkErrorStringRecord.setValue(NO_ALARMS);
		}
		updateErrorSum();
		
		notifyLinkChange(name);
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		this.configure(name,config.getString(NAME_DELIMITER, DEFAULT_NAME_DELIMITER));

	}

	/**
	 * Configures application internals, such as error links. This is called from {@link #configure(String, HierarchicalConfiguration)}.
	 *
	 * @param name a {@link java.lang.String} object
	 * @param nameDelimiter a {@link java.lang.String} object
	 */
	public void configure(String name, String nameDelimiter) {
		this.name=name;
		
		if (this.name==null) {
			throw new NullPointerException("Application name can not be null!");
		}
		
		this.nameDelimiter= nameDelimiter;
		
		if (this.nameDelimiter==null) {
			this.nameDelimiter=DEFAULT_NAME_DELIMITER;
		}

		errorSumRecord= addRecordOfMemoryValueProcessor(ERROR_SUM, "", DBRType.BYTE, (byte)0);
		linkErrorRecord= addRecordOfMemoryValueProcessor(LINK_ERROR, "", DBRType.BYTE, (byte)0);
		linkErrorStringRecord= addRecordOfMemoryValueProcessor(LINK_ERROR_STRING, "", DBRType.STRING, NO_ALARMS);

	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}   
	
	/**
	 * <p>Getter for the field <code>nameDelimiter</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getNameDelimiter() {
		return nameDelimiter;
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getRecordNames() {
		String[] names= records.keySet().toArray(new String[records.size()]);
		Arrays.sort(names);
		return names; 
	}
	      
	/** {@inheritDoc} */
	@Override
	public Record[] getRecords() {
		Record[] r= records.values().toArray(new Record[records.size()]);
		return r; 
	}

	/* (non-Javadoc)
	 * @see org.scictrl.csshell.epics.server.Application#getRecord(java.lang.String)
	 */
	/** {@inheritDoc} */
	@Override
	public Record getRecord(String name) {
		return records.get(name);
	}

	/* (non-Javadoc)
	 * @see org.scictrl.csshell.epics.server.Application#isDynamicRecordCreator()
	 */
	/** {@inheritDoc} */
	@Override
	public boolean isDynamicRecordCreator() {
		return dynamicRecordCreator;
	}
	
	/**
	 * Adds a record to internal storage, which manages records, which belongs to this application.
	 * Records are stored and retrieved on name base Map.
	 *
	 * @param name the unique name for the record, tow records with same name can not be used
	 * @param r the record to be added
	 * @return returns record which was added
	 */
	protected Record addRecord(String name, Record r) {
		
		if (records.containsKey(name)) {
			throw new InvalidParameterException("Record name '"+name+"' is already in use.");
		}
		
		r.setApplication(this);
		records.put(name, r);
		r.addPropertyChangeListener(Record.PROPERTY_VALUE, new RecordChangeListener(name));
		r.addPropertyChangeListener(Record.PROPERTY_ALARM, new RecordAlarmListener(name));
		r.addPropertyChangeListener(Record.PROPERTY_WRITE, new RecordWriteListener(name));
		
		if (r.getProcessor() instanceof OnDemandValueProcessor) {
			OnDemandValueProcessor p= (OnDemandValueProcessor) r.getProcessor();
			p.setKey(name);
			p.setProvider(this);
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
	protected ValueLinks connectLinks(String name, String... linkNames) {
		
		if (name==null) {
			throw new InvalidParameterException("link reference name is null.");
		}
		
		if (links.containsKey(name)) {
			throw new InvalidParameterException("link reference ID '"+name+"' is already used.");
		}
		log4debug("Connecting with "+Arrays.toString(linkNames)+" ...");
		ValueLinks vl= new ValueLinks(name, this.name, linkNames, new LinkListener(name), Record.PROPERTY_VALUE);
		log4info("Connected with "+Arrays.toString(linkNames));

		links.put(name, vl);
		
		if (activated) {
			vl.activate(database);
		}
		
		return vl;
	}
	
	/**
	 * <p>reconnectLinks.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param linkNames a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.ValueLinks} object
	 */
	protected ValueLinks reconnectLinks(String name, String... linkNames) {
		if (name==null) {
			throw new InvalidParameterException("link reference name is null.");
		}
		
		ValueLinks vl= links.remove(name);
			
		if (vl!=null) {
			log4info("Disconnecting "+Arrays.toString(vl.getLinkNames()));
			vl.deactivate();
		}
		
		if (linkNames==null || linkNames.length==0 || linkNames[0]==null || linkNames[0].trim().length()==0) {
			return null;
		}
		
		return connectLinks(name, linkNames);
	}
	
	

	/**
	 * Returns ValueLinks object with provided name
	 *
	 * @param name the name of ValueLinks
	 * @return the ValueLinks object with requested name
	 */
	public ValueLinks getLinks(String name) {
		return links.get(name);
	}

	/**
	 * This method is called whenever record value changed for any record, which belongs
	 * to this application (it was added to the application {@link #addRecord(String, Record)}).
	 * Implementation class should override this method to intercept the update.
	 *
	 * @param name the name of record, that triggered change.
	 * @param alarmOnly if <code>true</code> then only alarm has been changed
	 */
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		// nothing
	}
	
	/**
	 * This method is called whenever record value has been written for any record, which belongs
	 * to this application (it was added to the application {@link #addRecord(String, Record)}).
	 * Implementation class should override this method to intercept the update.
	 *
	 * @param name the name of record, that triggered change.
	 */
	protected synchronized void notifyRecordWrite(String name) {
		// nothing
	}

	/**
	 * This method is called whenever record value changed for any record, which belongs
	 * to this application (it was added to the application {@link #addRecord(String, Record)}).
	 * Implementation class should override this method to intercept the update.
	 *
	 * @param name the name of record, that triggered change.
	 */
	protected synchronized void notifyLinkChange(String name) {
		// nothing
	}

	/** {@inheritDoc} */
	public void initialize(Database database) {
		this.database=database;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuffer sb= new StringBuffer(128);
		sb.append("Application{n:");
		sb.append(name);
		sb.append(",c:");
		sb.append(getClass().getName());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Returns storage for this object. By default storage is a properties file APP/APP.properties
	 * in configuration folder location. To change file name, override and implement method {@link #createNewStore()}.
	 *
	 * @return lazy created store object.
	 */
	protected PropertiesConfiguration getStore() {
		if (store == null) {
			synchronized (this) {
				if (store == null) {
					store=createNewStore();
				}
			}
		}
		return store;
	}
	
	/**
	 * Creates new store configuration object with file APP/APP.properties
	 * in configuration folder location. Override this method to provide different file name as storage.
	 *
	 * NEw store is created by call to {@link #getStore(String, String)}.
	 *
	 * @return new properties store
	 */
	protected PropertiesConfiguration createNewStore() {
		String name= getClass().getSimpleName();
		return getStore(name,name+".properties");
	}

	private final Object normalize(DBRType type, Object[] value) {
		if (value==null) {
			return EPICSUtilities.suggestDefaultValue(type);
		}
		if (value.length==1 && value[0]==null) {
			return EPICSUtilities.suggestDefaultValue(type);
		}
		if (value.length==1 && value[0]!=null && value[0].getClass().isArray()) {
			return value[0];
		}
		return value;
	}
	
	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param count a int
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, int count, DBRType type) {
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, count, desc, null, false, false).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param value a {@link java.lang.Object} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, DBRType type, Object... value) {
		Object val= normalize(type, value);
		int count= Array.getLength(val);
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, count, desc, val, false, false).getRecord());
	}
	
	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param value an array of {@link byte} objects
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, byte[] value) {
		Object val= value;
		if (value==null) {
			val= EPICSUtilities.suggestDefaultValue(DBRType.BYTE);
		}
		int count= value.length;
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), DBRType.BYTE, count, desc, val, false, false).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param fixed a boolean
	 * @param undefined a boolean
	 * @param value a {@link java.lang.Object} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, DBRType type, boolean fixed, boolean undefined, Object... value) {
		Object val= normalize(type, value);
		int count= Array.getLength(val);
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, count, desc, val, fixed, undefined).getRecord());
	}

	/**
	 * <p>addRecordOfCommandProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param timeout a long
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfCommandProcessor(String name, String desc, long timeout) {
		return addRecord(name, CommandValueProcessor.newProcessor(fullRecordName(name), desc, timeout).getRecord());
	}

	/**
	 * <p>pushDoneCommandProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 */
	protected void pushDoneCommandProcessor(String name) {
		Record r= getRecord(name);
		if (r!=null) {
			CommandValueProcessor p = (CommandValueProcessor) r.getProcessor();
			p.pushDone();
		}
	}

	/**
	 * <p>resetOnDemandProcessor.</p>
	 *
	 * @param key a {@link java.lang.String} object
	 */
	protected void resetOnDemandProcessor(String key) {
		Record r= getRecord(key);
		if (r!=null) {
			OnDemandValueProcessor p = (OnDemandValueProcessor) r.getProcessor();
			p.reset();
		}
	}

	/**
	 * <p>addRecordOfOnDemandProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfOnDemandProcessor(String name, String desc, DBRType type, int count) {
		return addRecord(name, OnDemandValueProcessor.newProcessor(fullRecordName(name), type, count, desc).getRecord());
	}
	
	/**
	 * <p>addRecordOfOnLinkValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param link a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfOnLinkValueProcessor(String name, String desc, DBRType type, String link) {
		return addRecord(name, LinkedValueProcessor.newProcessor(fullRecordName(name), type, desc, link).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param min a {@link java.lang.Double} object
	 * @param max a {@link java.lang.Double} object
	 * @param units a {@link java.lang.String} object
	 * @param precision a short
	 * @param value a {@link java.lang.Double} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, Double min, Double max, String units, short precision, Double value) {
		Object val= value;
		DBRType type= DBRType.DOUBLE;
		// if null we should just put some dummy value
		if (value==null) {
			val= EPICSUtilities.suggestDefaultValue(type);
		}
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, 1, desc, val, false,min,max,units,precision).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param min a {@link java.lang.Integer} object
	 * @param max a {@link java.lang.Integer} object
	 * @param units a {@link java.lang.String} object
	 * @param value a {@link java.lang.Integer} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, Integer min, Integer max, String units, Integer value) {
		Object val= value;
		DBRType type= DBRType.INT;
		// if null we should just put some dummy value
		if (value==null) {
			val= EPICSUtilities.suggestDefaultValue(type);
		}
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, 1, desc, val, false,min,max,units,(short)0).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param min a {@link java.lang.Double} object
	 * @param max a {@link java.lang.Double} object
	 * @param units a {@link java.lang.String} object
	 * @param precision a short
	 * @param value an array of {@link double} objects
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, Double min, Double max, String units, short precision, double[] value) {
		Object val= value;
		DBRType type= DBRType.DOUBLE;
		// if null we should just put some dummy value
		int count = 1;
		if (value==null) {
			val= EPICSUtilities.suggestDefaultValue(type);
		} else {
			count=Array.getLength(value);
		}
		// what if provided value is already proper array, then we must unpack from vararg
		if (value.length==1) {
			val=value[0];
		}
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, count, desc, val, false,min,max,units,precision).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param min a {@link java.lang.Integer} object
	 * @param max a {@link java.lang.Integer} object
	 * @param units a {@link java.lang.String} object
	 * @param value an array of {@link int} objects
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, Integer min, Integer max, String units, int[] value) {
		Object val= value;
		DBRType type= DBRType.INT;
		// if null we should just put some dummy value
		int count = 1;
		if (value==null) {
			val= EPICSUtilities.suggestDefaultValue(type);
		} else {
			count=Array.getLength(value);
		}
		// what if provided value is already proper array, then we must unpack from vararg
		if (value.length==1) {
			val=value[0];
		}
		return addRecord(name, MemoryValueProcessor.newProcessor(fullRecordName(name), type, count, desc, val, false,min,max,units,(short)0).getRecord());
	}

	/**
	 * <p>addRecordOfMemoryValueProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param desc a {@link java.lang.String} object
	 * @param labels an array of {@link java.lang.String} objects
	 * @param value a short
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record addRecordOfMemoryValueProcessor(String name, String desc, String[] labels, short value) {
		return addRecord(name, EnumValueProcessor.newProcessor(fullRecordName(name), desc, value, false,labels).getRecord());
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		this.activated=true;
		
		for (ValueLinks vl : links.values()) {
			
			vl.activate(database);
			
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isActivated() {
		return activated;
	}
	
	/**
	 * <p>updateLinkError.</p>
	 *
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 * @param errorString a {@link java.lang.String} object
	 */
	protected void updateLinkError(Severity severity, Status status, String errorString) {
		if (errorString==null) {
			errorString="";
		}
		linkErrorRecord.updateAlarm(severity, status, false);
		linkErrorRecord.setValue(severity!=Severity.NO_ALARM);
		linkErrorStringRecord.setValue(errorString);
		updateErrorSum();
	}
	
	/**
	 * <p>updateLinkError.</p>
	 *
	 * @param error a boolean
	 * @param errorString a {@link java.lang.String} object
	 */
	protected void updateLinkError(boolean error, String errorString) {
		if (errorString==null) {
			errorString="";
		}
		if (error) {
			linkErrorRecord.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM, false);
			linkErrorRecord.setValue(Boolean.TRUE);
			linkErrorStringRecord.setValue(errorString);
		} else {
			linkErrorRecord.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM, false);
			linkErrorRecord.setValue(Boolean.FALSE);
			linkErrorStringRecord.setValue(errorString);
		}
		updateErrorSum();
	}

	/**
	 * <p>updateErrorSum.</p>
	 *
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 */
	protected void updateErrorSum(Severity severity, Status status) {
		sumSeverity=severity;
		sumStatus=status;
		updateErrorSum();
	}
	/**
	 * <p>updateErrorSum.</p>
	 */
	protected void updateErrorSum() {
		Severity sev;
		Status stat;
		
		if (sumSeverity.isGreaterThan(linkErrorRecord.getAlarmSeverity())) {
			sev=sumSeverity;
			stat=sumStatus;
		} else {
			sev= linkErrorRecord.getAlarmSeverity();
			stat= linkErrorRecord.getAlarmStatus();
		}
		errorSumRecord.updateAlarm(sev, stat, false);
		errorSumRecord.setValue(sev!=Severity.NO_ALARM);
	}
	
	/** {@inheritDoc} */
	@Override
	public Object getValue(Object key) {
		throw new IllegalArgumentException("The application '"+getName()+"' does not provide vlue for key '"+key+"'.");
	}
	
	/**
	 * <p>getRecordErrorSum.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record getRecordErrorSum() {
		return errorSumRecord;
	}
	
	/**
	 * <p>getRecordLinkError.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected Record getRecordLinkError() {
		return linkErrorRecord;
	}

	/**
	 * <p>getNotNull.</p>
	 *
	 * @throws java.lang.IllegalArgumentException if config does not contain property
	 * @param prop a {@link java.lang.String} object
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @param def a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	protected String getNotNull(String prop, HierarchicalConfiguration config, String def) {
		String val= config.getString(prop,def);
		if (val==null || val.length()==0) {
			throw new IllegalArgumentException("Property '"+prop+"' is not defined!");
		}
		return val;
	}
 }
