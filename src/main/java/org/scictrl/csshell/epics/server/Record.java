/**
 * 
 */
package org.scictrl.csshell.epics.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.epics.EPICSUtilities;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>Record class.</p>
 *
 * @author igor@scictrl.com
 */
public class Record {
	
	/** Constant <code>PROPERTY_RECORD="record"</code> */
	public static final String PROPERTY_RECORD="record";
	/** Constant <code>PROPERTY_VALUE="value"</code> */
	public static final String PROPERTY_VALUE="value";
	/** Constant <code>PROPERTY_ALARM="alarm"</code> */
	public static final String PROPERTY_ALARM="alarm";
	/** Constant <code>PROPERTY_WRITE="write"</code> */
	public static final String PROPERTY_WRITE="write";
	
	/**
	 * <p>extractMetaData.</p>
	 *
	 * @param r a {@link org.scictrl.csshell.epics.server.Record} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public static MetaData extractMetaData(Record r) {
		
		DataType dt= EPICSUtilities.toDataType(r.type,r.count);
		String host="localhost";
		
		try {
			host= InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		MetaDataImpl mf= new MetaDataImpl(
				r.name, 
				r.description, 
				r.lowerCtrlLimit, 
				r.upperCtrlLimit, 
				r.lowerDispLimit, 
				r.upperDispLimit, 
				r.lowerWarningLimit, 
				r.upperWarningLimit, 
				r.lowerAlarmLimit, 
				r.upperAlarmLimit, 
				r.enumLabels, 
				null, 
				null, 
				r.units, 
				r.count,
				(int)r.precision, 
				dt, 
				dt.getJavaClass(), 
				true, 
				r.writable, 
				host);
		
		return mf;
	}
	
	
	/**
	 * Converts property name to PROPERTY_VALUE or PROPERTY_ALARM, useful when parsing configuration input.
	 *
	 * @param name unformatted property name, should be "value" or "alarm".
	 * @return return PROPERTY_VALUE or PROPERTY_ALARM if matched or <code>null</code> if not matched.
	 */
	public static final String toPropertyName(String name) {
		if (PROPERTY_VALUE.equalsIgnoreCase(name)) {
			return PROPERTY_VALUE;
		}
		if (PROPERTY_ALARM.equalsIgnoreCase(name)) {
			return PROPERTY_ALARM;
		}
		return null;
	}
	
	/**
	 * Return new Record containing the provided processor.
	 *
	 * @param proc a {@link org.scictrl.csshell.epics.server.ValueProcessor} object
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param description a {@link java.lang.String} object
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public static Record forProcessor(String name, DBRType type, int count, String description, ValueProcessor proc, HierarchicalConfiguration config) {
		
		Record r= new Record(name, type, count, description);
		
		if (config==null) {
			config=new HierarchicalConfiguration();
		}
		
		proc.configure(r,config);
		
		r.setProcessor(proc);
		
		return r;
	}
	

	private String name;
	private DBRType type;
	private int count;
	private String units;
	private Number upperDispLimit;
	private Number lowerDispLimit;
	private Number upperAlarmLimit;
	private Number upperWarningLimit;
	private Number lowerWarningLimit;
	private Number lowerAlarmLimit;
	private Number upperCtrlLimit;
	private Number lowerCtrlLimit;
	private short precision = 0;
	private String[] enumLabels = null;
	private ValueProcessor processor;
	private Severity alarmSeverity=Severity.NO_ALARM;
	private Status alarmStatus=Status.NO_ALARM;
	private Application application;
	
	private PropertyChangeSupport support= new PropertyChangeSupport(this);
	private Database database;
	private String description;
	
	private boolean persistent= false;
	private boolean writable= true;
	
	private boolean activated= false;
	
	private Logger log= LogManager.getLogger(getClass());
	
	/**
	 * <p>Constructor for Record.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 */
	public Record(String name, DBRType type, int count) {
		if (name==null) {
			throw new NullPointerException("name is null");
		}
		if (type==null) {
			type= DBRType.UNKNOWN;
		}
		this.name=name;
		this.type=type;
		this.count=count;
	}
	
	
	/**
	 * <p>Constructor for Record.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param description a {@link java.lang.String} object
	 */
	public Record(String name, DBRType type, int count, String description) {
		this(name,type,count);
		this.description=description;
	}

	
	/**
	 * <p>Constructor for Record.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param units a {@link java.lang.String} object
	 * @param upperDispLimit a {@link java.lang.Number} object
	 * @param lowerDispLimit a {@link java.lang.Number} object
	 * @param upperWarningLimit a {@link java.lang.Number} object
	 * @param lowerWarningLimit a {@link java.lang.Number} object
	 * @param upperAlarmLimit a {@link java.lang.Number} object
	 * @param lowerAlarmLimit a {@link java.lang.Number} object
	 * @param upperCtrlLimit a {@link java.lang.Number} object
	 * @param lowerCtrlLimit a {@link java.lang.Number} object
	 * @param precision a short
	 * @param enumLabels an array of {@link java.lang.String} objects
	 * @param description a {@link java.lang.String} object
	 */
	public Record(String name, DBRType type, int count,
			String units, Number upperDispLimit,
			Number lowerDispLimit,
			Number upperWarningLimit, Number lowerWarningLimit,
			Number upperAlarmLimit, Number lowerAlarmLimit, Number upperCtrlLimit,
			Number lowerCtrlLimit, short precision, String[] enumLabels, String description) {
		
		this(name,type,count);
		
		this.units = units;
		this.upperDispLimit = upperDispLimit;
		this.lowerDispLimit = lowerDispLimit;
		this.upperWarningLimit = upperWarningLimit;
		this.lowerWarningLimit = lowerWarningLimit;
		this.upperAlarmLimit = upperAlarmLimit;
		this.lowerAlarmLimit = lowerAlarmLimit;
		this.upperCtrlLimit = upperCtrlLimit;
		this.lowerCtrlLimit = lowerCtrlLimit;
		this.precision = precision;
		this.enumLabels = enumLabels;
		this.description=description;
	}
	
	/**
	 * <p>isActivated.</p>
	 *
	 * @return a boolean
	 */
	public boolean isActivated() {
		return activated;
	}
	
	/**
	 * Sets persistency flag. Persistent record will save it's value when changed on disk and
	 * receive last saved value at restart.
	 *
	 * @param persistent a boolean
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
		checkPersistency();
	}
	
	/**
	 * <p>isPersistent.</p>
	 *
	 * @return a boolean
	 */
	public boolean isPersistent() {
		return persistent;
	}
	
	/**
	 * If this record belongs to an application, then this method returns that application, otherwise <code>null</code> is returned.
	 *
	 * @return an application if exist or <code>null</code>
	 */
	public Application getApplication() {
		return application;
	}
	
	/**
	 * This method is called by application instances for all records created by that application.
	 *
	 * @param application a {@link org.scictrl.csshell.epics.server.Application} object
	 */
	public void setApplication(Application application) {
		this.application = application;
	}

	/**
	 * <p>Getter for the field <code>description</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * <p>Getter for the field <code>name</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * <p>Getter for the field <code>type</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public DBRType getType() {
		return type;
	}
	
	/**
	 * <p>Getter for the field <code>count</code>.</p>
	 *
	 * @return a int
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * <p>Getter for the field <code>enumLabels</code>.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getEnumLabels() {
		return enumLabels;
	}
	
	/**
	 * <p>Getter for the field <code>lowerAlarmLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getLowerAlarmLimit() {
		return lowerAlarmLimit!=null ? lowerAlarmLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>lowerCtrlLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getLowerCtrlLimit() {
		return lowerCtrlLimit != null ? lowerCtrlLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>lowerDispLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getLowerDispLimit() {
		return lowerDispLimit != null ? lowerCtrlLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>lowerWarningLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getLowerWarningLimit() {
		return lowerWarningLimit != null ? lowerWarningLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>precision</code>.</p>
	 *
	 * @return a short
	 */
	public short getPrecision() {
		return precision;
	}
	
	/**
	 * <p>getTimestamp.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.TimeStamp} object
	 */
	public TimeStamp getTimestamp() {
		return processor.getTimestamp();
	}
	
	/**
	 * <p>Getter for the field <code>units</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getUnits() {
		return units == null ? "" : units;
	}
	
	/**
	 * <p>Getter for the field <code>upperAlarmLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getUpperAlarmLimit() {
		return upperAlarmLimit != null ? upperAlarmLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>upperCtrlLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getUpperCtrlLimit() {
		return upperCtrlLimit != null ? upperCtrlLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>upperDispLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getUpperDispLimit() {
		return upperDispLimit != null ? upperDispLimit : 0.0;
	}
	
	/**
	 * <p>Getter for the field <code>upperWarningLimit</code>.</p>
	 *
	 * @return a {@link java.lang.Number} object
	 */
	public Number getUpperWarningLimit() {
		return upperWarningLimit != null ? upperWarningLimit : 0.0;
	}
	
	/**
	 * <p>getValue.</p>
	 *
	 * @return a {@link java.lang.Object} object
	 */
	public Object getValue() {
		return processor.getValue();
	}
	
	/**
	 * <p>getValueAsDouble.</p>
	 *
	 * @return a double
	 */
	public double getValueAsDouble() {
		return Array.getDouble(processor.getValue(),0);
	}

	/**
	 * <p>getValueAsInt.</p>
	 *
	 * @return a int
	 */
	public int getValueAsInt() {
		return Array.getInt(processor.getValue(),0);
	}
	
	/**
	 * <p>getValueAsBoolean.</p>
	 *
	 * @return a boolean
	 */
	public boolean getValueAsBoolean() {
		return Array.getLong(processor.getValue(),0)==1L;
	}
	
	/**
	 * <p>getValueAsDoubleArray.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] getValueAsDoubleArray() {
		Object val= processor.getValue();
		double[] d= new double[Array.getLength(val)];
		System.arraycopy(val, 0, d, 0, d.length);
		return d;
	}
	
	/**
	 * <p>getValueAsString.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getValueAsString() {
		return EPICSUtilities.toString(processor.getValue(), type, count);
	}

	/**
	 * This method sets new value to the value processor for this record. If
	 * current value at the processor is the same, than no value-change event
	 * is fired and none will be notified, that attempt at value change has
	 * been done.
	 * <p>
	 * To force value processing regardless of the matching values, then
	 * call write.
	 * </p>
	 *
	 * @param value the value to be set to the processor
	 */
	public void setValue(Object value) {
		this.processor.setValue(value);
	}
	
	/**
	 * This method sets new value to the value processor for this record.
	 * This call might first fire the {@link org.scictrl.csshell.epics.server.Record#PROPERTY_VALUE} only if value was actually changed,
	 * then it will always fire the {@link org.scictrl.csshell.epics.server.Record#PROPERTY_WRITE} change event to signal that value change
	 * change attempt was done. This method is called from CAJ remote call handler each time a remote request is made.
	 * It is also called from {@link org.scictrl.csshell.epics.server.ValueLinks} object when an local application or a processor wants to mimic a
	 * remote write and it is this ways processed as a remote call.
	 *
	 * @param value the value to be set to the processor
	 */
	public void write(Object value) {
		if (writable) {
			this.processor.setValue(value);
			fireWriteEvent();
		}
	}
	
	/**
	 * Returns <code>true</code> if this record supports value writing. If not, calling write (remote call)
	 * will not be passed to the processor.
	 *
	 * @return <code>true</code> if this record supports value writing
	 */
	public boolean isWrittable() {
		return writable;
	}
	
	/**
	 * Sets the writable flag. If <code>true</code> then this record supports value writing.
	 * If not, calling write (remote call)
	 * will not be passed to the processor.
	 *
	 * @param writable the writable flag, if <code>true</code> then this record supports value writing
	 */
	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	/**
	 * <p>Setter for the field <code>count</code>.</p>
	 *
	 * @param count a int
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * <p>activate.</p>
	 */
	public void activate() {
		activated=true;
		processor.activate();
	}
	
	/**
	 * This method is called by Database when record is added to the Database.
	 *
	 * @param database a Database object to which this record was added.
	 */
	public void initialize(Database database) {
		this.database=database;
		
		checkPersistency();
		
		fireRecordChange();
	}

	private void checkPersistency() {
		if (database==null) {
			return;
		}
		if (isPersistent()) {
			database.getPeristencyStore().registerValue(this);
		} else {
			database.getPeristencyStore().deregister(this);
		}
		
		if (processor instanceof PersistentValueProcessor) {
			
			PersistentValueProcessor pvp= (PersistentValueProcessor)processor;
			
			if (pvp.isPersistent()) {
				database.getPeristencyStore().registerProcessor(pvp);
			}
			
			
		}
	}


	/**
	 * This is part of initialization of Record, it is called after {@link org.scictrl.csshell.epics.server.ValueProcessor#configure(Record, org.apache.commons.configuration.HierarchicalConfiguration)}
	 * has been called, so processor is already configured.
	 *
	 * @param vp a {@link org.scictrl.csshell.epics.server.ValueProcessor} object
	 */
	public void setProcessor(ValueProcessor vp) {
		this.processor = vp;

		if (processor.getType() != null && processor.getType() != DBRType.UNKNOWN
				&& processor.getType() != type) {
			
			if (this.type!=DBRType.UNKNOWN) {
				StringBuilder sb= new StringBuilder(128);
				sb.append("Record for ");
				sb.append(name);
				sb.append(" with type "); 
				sb.append(type!=null ? type.getName() : "NULL"); 
				sb.append(" is switching type to ");
				sb.append(processor.getType()!=null ? processor.getType().getName() : "NULL"); 
				sb.append(" from processor '"); 
				sb.append(processor.getClass()); 
				sb.append("'.");
				log.warn(sb.toString());
			}
			type = processor.getType();
		}

	}
	
	/**
	 * <p>Getter for the field <code>database</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Database} object
	 */
	public Database getDatabase() {
		return database;
	}

	/**
	 * Adds listener to one of three change events: PROPERTY_RECORD, PROPERTY_VALUE and PROPERTY_ALARM.
	 *
	 * @param name the name of property, supported are: PROPERTY_RECORD, PROPERTY_VALUE, PROPERTY_ALARM
	 * @param l the listener
	 */
	public void addPropertyChangeListener(String name, PropertyChangeListener l) {
		support.addPropertyChangeListener(name, l);
	}

	/**
	 * Adds listener to all three change events: PROPERTY_RECORD, PROPERTY_VALUE and PROPERTY_ALARM.
	 *
	 * @param l the listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}

	/**
	 * <p>removePropertyChangeListener.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void removePropertyChangeListener(String name, PropertyChangeListener l) {
		support.removePropertyChangeListener(name, l);
	}
	
	/**
	 * <p>removePropertyChangeListener.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}

	/**
	 * <p>fireRecordChange.</p>
	 */
	public void fireRecordChange() {
		support.firePropertyChange(PROPERTY_RECORD, null, this);
	}

	
	/**
	 * <p>fireValueChange.</p>
	 */
	public void fireValueChange() {
		support.firePropertyChange(PROPERTY_VALUE, null, getValue());
	}
	/**
	 * <p>fireWriteEvent.</p>
	 */
	public void fireWriteEvent() {
		support.firePropertyChange(PROPERTY_WRITE, null, getValue());
	}
	/**
	 * <p>fireAlarmChange.</p>
	 */
	public void fireAlarmChange() {
		support.firePropertyChange(PROPERTY_ALARM, null, getAlarmSeverity());
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
	
	/**
	 * <p>isAlarmUndefined.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAlarmUndefined() {
		return alarmStatus==Status.UDF_ALARM;
	}
	
	/**
	 * <p>isAlarm.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAlarm() {
		return (
				alarmStatus!=null && alarmStatus!=Status.NO_ALARM) || 
				(alarmSeverity!=null && alarmSeverity!=Severity.NO_ALARM);
	}

	/**
	 * <p>updateNoAlarm.</p>
	 */
	public void updateNoAlarm() {
		updateAlarm(Severity.NO_ALARM, Status.NO_ALARM, true);
	}

	/**
	 * <p>updateAlarm.</p>
	 *
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 */
	public void updateAlarm(Severity severity, Status status) {
		updateAlarm(severity, status, true);
	}

	/**
	 * <p>updateAlarm.</p>
	 *
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 * @param notify a boolean
	 */
	public void updateAlarm(Severity severity, Status status, boolean notify) {
		if (severity==null || status==null) {
			return;
		}
		if (severity!=alarmSeverity || status!=alarmStatus) {
			alarmSeverity=severity;
			alarmStatus=status;
			if (notify) {
				fireAlarmChange();
			}
		}
		
	}
	
	/**
	 * <p>Getter for the field <code>processor</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.ValueProcessor} object
	 */
	public ValueProcessor getProcessor() {
		return processor;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuffer sb= new StringBuffer(128);
		sb.append("Record{n:");
		sb.append(name);
		sb.append(",d:");
		sb.append(description);
		sb.append("}");
		return sb.toString();
	}


	/**
	 * <p>copyFields.</p>
	 *
	 * @param record a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public void copyFields(Record record) {
		description= record.description;
		enumLabels= record.enumLabels;
		lowerAlarmLimit= record.lowerAlarmLimit;
		lowerCtrlLimit= record.lowerCtrlLimit;
		lowerDispLimit= record.lowerDispLimit;
		lowerWarningLimit= record.lowerWarningLimit;
		precision= record.precision;
		units= record.units;
		upperAlarmLimit= record.upperAlarmLimit;
		upperCtrlLimit= record.upperCtrlLimit;
		upperDispLimit= record.upperDispLimit;
		upperWarningLimit= record.upperWarningLimit;
	}

	/**
	 * <p>copyUnitsControlLimits.</p>
	 *
	 * @param record a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public void copyUnitsControlLimits(Record record) {
		enumLabels= record.enumLabels;
		lowerCtrlLimit= record.lowerCtrlLimit;
		lowerDispLimit= record.lowerDispLimit;
		precision= record.precision;
		units= record.units;
		upperCtrlLimit= record.upperCtrlLimit;
		upperDispLimit= record.upperDispLimit;
	}

	/**
	 * <p>copyFields.</p>
	 *
	 * @param md a {@link org.scictrl.csshell.MetaData} object
	 */
	public void copyFields(MetaData md) {
		if (md.getDescription()!=null) {
			description= md.getDescription();
		}
		if (md.getStates()!=null) {
			enumLabels= md.getStates();
		}
		if (md.getUnits()!=null) {
			units= md.getUnits();
		}
		precision= (short) md.getPrecision();
		lowerAlarmLimit= md.getAlarmMin();
		lowerCtrlLimit= md.getMinimum();
		lowerDispLimit= md.getDisplayMin();
		lowerWarningLimit= md.getWarnMin();
		upperAlarmLimit= md.getAlarmMax();
		upperCtrlLimit= md.getMaximum();
		upperDispLimit= md.getDisplayMax();
		upperWarningLimit= md.getWarnMax();
	}


	/**
	 * <p>setValueAsString.</p>
	 *
	 * @param value a {@link java.lang.String} object
	 */
	public void setValueAsString(String value) {
		if (value == null) {
			return;
		}
		if (count==1) {
			String num= value;
			if (value==null || value.length()==0) {
				num="0";
			}
			String[] vals= num.split(",");
			int l= vals.length;
			if (l>1 && !type.isSTRING()) {
				num=vals[0];
			}

			if (type.isDOUBLE()) {
				processor.setValue(new double[]{Double.valueOf(num)});
			} else if (type.isFLOAT()) {
				processor.setValue(new float[]{Float.valueOf(num)});
			} else if (type.isENUM() || type.isSHORT()) {
				processor.setValue(new short[]{Short.valueOf(num)});
			} else if (type.isINT()) {
				processor.setValue(new int[]{Integer.valueOf(num)});
			} else if (type.isBYTE()) {
				processor.setValue(new byte[]{Byte.valueOf(num)});
			} else if (type.isSTRING()) {
					processor.setValue(new String[]{value});
			} else {
				processor.setValue(new double[]{0.0});
			}
		} else {
			if (value==null || value.length()==0) {
				value="";
			}
			String[] vals= value.split(",");
			int l= vals.length;

			if (type.isDOUBLE()) {
				double[] val= new double[l];
				for (int i = 0; i < val.length; i++) {
					if (vals[i]==null || vals[i].trim().length()==0) vals[i]="0";
					val[i]=Double.valueOf(vals[i]);
				}
				processor.setValue(val);
			} else if (type.isFLOAT()) {
				float[] val= new float[l];
				for (int i = 0; i < val.length; i++) {
					if (vals[i]==null || vals[i].trim().length()==0) vals[i]="0";
					val[i]=Float.valueOf(vals[i]);
				}
				processor.setValue(val);
			} else if (type.isENUM() || type.isSHORT()) {
				short[] val= new short[l];
				for (int i = 0; i < val.length; i++) {
					if (vals[i]==null || vals[i].trim().length()==0) vals[i]="0";
					val[i]=Short.valueOf(vals[i]);
				}
				processor.setValue(val);
			} else if (type.isINT()) {
				int[] val= new int[l];
				for (int i = 0; i < val.length; i++) {
					if (vals[i]==null || vals[i].trim().length()==0) vals[i]="0";
					val[i]=Integer.valueOf(vals[i]);
				}
				processor.setValue(val);
			} else if (type.isBYTE()) {
				byte[] val= new byte[]{};
				if (value.length()>0 && !Character.isDigit(value.charAt(0))) {
					val= value.getBytes();
				} else {
					val= new byte[l];
					for (int i = 0; i < val.length; i++) {
						if (vals[i]==null || vals[i].trim().length()==0) vals[i]="0";
						val[i]=Byte.valueOf(vals[i]);
					}
				}
				processor.setValue(val);
			} else if (type.isSTRING()) {
				processor.setValue(vals);
			} else {
				processor.setValue(new double[]{0.0});
			}
		}
	}


	/**
	 * <p>setMinMax.</p>
	 *
	 * @param min a {@link java.lang.Number} object
	 * @param max a {@link java.lang.Number} object
	 */
	public void setMinMax(Number min, Number max) {
		lowerAlarmLimit=lowerCtrlLimit=lowerDispLimit=lowerWarningLimit=min;
		upperAlarmLimit=upperCtrlLimit=upperDispLimit=upperWarningLimit=max;
	}

}
