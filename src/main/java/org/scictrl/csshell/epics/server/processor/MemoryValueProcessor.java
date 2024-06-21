package org.scictrl.csshell.epics.server.processor;

import java.lang.reflect.Array;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.math.NumberUtils;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;


/**
 * <p>MemoryValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class MemoryValueProcessor extends AbstractValueProcessor {

	/**
	 * <p>newDoubleProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param value a double
	 * @param fixed a boolean
	 * @return a {@link org.scictrl.csshell.epics.server.processor.AbstractValueProcessor} object
	 */
	public static final AbstractValueProcessor newDoubleProcessor(String name, String description, double value, boolean fixed) {
		return newProcessor(name,DBRType.DOUBLE,1,description,new double[]{value},fixed,false);
	}

	/**
	 * <p>newBooleanProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param value a boolean
	 * @param fixed a boolean
	 * @param undefined a boolean
	 * @return a {@link org.scictrl.csshell.epics.server.processor.AbstractValueProcessor} object
	 */
	public static final AbstractValueProcessor newBooleanProcessor(String name, String description, boolean value, boolean fixed, boolean undefined) {
		return newProcessor(name,DBRType.BYTE,1,description,new byte[]{(byte)(value?1:0)},fixed,undefined);
	}

	/**
	 * <p>newProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param description a {@link java.lang.String} object
	 * @param value a {@link java.lang.Object} object
	 * @param fixed a boolean
	 * @param undefined a boolean
	 * @return a {@link org.scictrl.csshell.epics.server.processor.AbstractValueProcessor} object
	 */
	public static final AbstractValueProcessor newProcessor(String name, DBRType type, int count, String description, Object value, boolean fixed, boolean undefined) {
		
		Record r= new Record(name, type, count);
		
		MemoryValueProcessor mvp= new MemoryValueProcessor();
		mvp.configure(r,new HierarchicalConfiguration());
		r.setProcessor(mvp);
		
		if (value==null) {
			value=EPICSUtilities.suggestDefaultValue(type,count);
		}
		
		
		if (undefined) {
			mvp._setValue(value, Severity.INVALID_ALARM, Status.UDF_ALARM, false, true);
		} else {
			mvp._setValue(value, null, null, false, true);
		}
		
		mvp.fixed=fixed;
		
		return mvp;
	}
	
	/**
	 * <p>newProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param description a {@link java.lang.String} object
	 * @param value a {@link java.lang.Object} object
	 * @param fixed a boolean
	 * @param min a {@link java.lang.Number} object
	 * @param max a {@link java.lang.Number} object
	 * @param units a {@link java.lang.String} object
	 * @param precision a short
	 * @return a {@link org.scictrl.csshell.epics.server.processor.AbstractValueProcessor} object
	 */
	public static final AbstractValueProcessor newProcessor(String name, DBRType type, int count, String description, Object value, boolean fixed, Number min, Number max, String units, short precision) {
		
		Record r= new Record(name, type, count, units, max, min, max, min, max, min, max, min, precision, null, description);
		
		MemoryValueProcessor mvp= new MemoryValueProcessor();
		mvp.configure(r,new HierarchicalConfiguration());
		r.setProcessor(mvp);
		
		if (value==null) {
			value=EPICSUtilities.suggestDefaultValue(type,count);
		}

		mvp.fixed=fixed;
		mvp._setValue(value, null, null, false, true);
		
		return mvp;
	}

	/**
	 * Value held by this processor.
	 */
	protected Object value;
	/**
	 * Value is fixed.
	 */
	protected boolean fixed;
	/**
	 * Timestamp of last value update.
	 */
	protected TimeStamp timestamp;
	/**
	 * Timestamp of last value change.
	 */
	protected TimeStamp lastChangeTimestamp;

	/**
	 * <p>Constructor for MemoryValueProcessor.</p>
	 */
	public MemoryValueProcessor() {
		super();
	}
	
	/** {@inheritDoc} */
	@Override
	public TimeStamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * This method is used by remote EPICS entity or other record within same Database.
	 * By definition calling setValue must always update value, because further processing
	 * must be triggered.
	 */
	@Override
	public void setValue(Object value) {
		_setValue(value, null, null, true, false);
	}

	/**
	 * Internal set: sets new value to this processor.
	 * It checks if it is fixed, it never sets fixed value.
	 * Fires value update event only if value was changes and notify is true.
	 * If value was not changed, update is not forced.
	 * If the value is not an array it tries to convert it to a single element array.
	 * Updates timestamp.
	 * If required fires notify event.
	 *
	 * @param value value to be set
	 * @param severity the severity to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param status the status to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param notify if <code>true</code> fire notify event if value was change, <code>false</code> suppresses events
	 * @return true if value was updated, regardless if based on difference or force
	 */
	protected boolean _setValue(Object value, Severity severity, Status status, boolean notify) {
		return _setValue(value, severity, status, notify, false);
	}
	/**
	 * Internal set: sets new value to this processor. It checks if it is fixed, it never sets fixed value.
	 * Fires value update event only if value was changes and notify is true.
	 * If force is true, then updates value and fires notify, if notify is true, even if value was not changed.
	 * If the value is not an array it tries to convert it to a single element array.
	 * Updates timestamp.
	 * If required fires notify event.
	 *
	 * @param value value to be set
	 * @param severity the severity to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param status the status to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param notify if <code>true</code> fire notify event if value was change, <code>false</code> suppresses events
	 * @param force if <code>true</code> it will update value and timestamp even if there is not new value and proceeds with notify
	 * @return true if value was updated, regardless if based on difference or force
	 */
	protected boolean _setValue(Object value, Severity severity, Status status, boolean notify, boolean force) {
		
		if (fixed) {
			return false;
		}
		
		this.timestamp= new TimeStamp();

		force= force || record.isAlarmUndefined();
		
		if (record.isAlarmUndefined() && (severity==null || status==null)) {
			severity=Severity.NO_ALARM;
			status=Status.NO_ALARM;
		}

		value= convert(value);
		
		if (force || !EPICSUtilities.deepEquals(this.value, value)) {
			// value has changed or we force update regardles the value change
			this.value=value;
			this.lastChangeTimestamp=this.timestamp;
			if (notify) {
				if (severity!=null && status!=null) {
					record.updateAlarm(severity, status, false);
				}
				record.fireValueChange();
			}
			return true;
		} else {
			// value has not been changed, we update just alarm
			if (severity!=null && status!=null) {
				record.updateAlarm(severity, status, notify);
			}
			return false;
		}

	}
	
	
	/**
	 * <p>convert.</p>
	 *
	 * @param val a {@link java.lang.Object} object
	 * @return a {@link java.lang.Object} object
	 */
	protected Object convert(Object val) {
		return EPICSUtilities.convertToDBRValue(val, type);
		/*if (!val.getClass().isArray()) {
			return EPICSUtilities.convertToDBRValue(val, type);
		}
		return val;*/
	}

	/**
	 * Updates the timestamp and fires value update event, if processor has value other than <code>null</code>.
	 *
	 * @return true if update was fired
	 */
	protected boolean _forceValueUpdateEvent() {
		if (value==null) {
			return false;
		}
		this.timestamp= new TimeStamp();
		record.fireValueChange();
		return true;
	}

	/**
	 * Internal set: sets new value to this processor. It checks if it is fixed, it never sets fixed value.
	 * Fires value update event only if value was changes and notify is true.
	 * If force is true, then updates value and fires notify, if notify is true, even if value was not changed.
	 * If the value is not an array it tries to convert it to a single element array.
	 * Updates timestamp.
	 * If required fires notify event.
	 *
	 * @param value value to be set
	 * @param severity the severity to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param status the status to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param notify if <code>true</code> fire notify event if value was change, <code>false</code> suppresses events
	 * @return true if value was updated, regardless if based on difference or force
	 */
	protected boolean _setValue(boolean value, Severity severity, Status status, boolean notify) {
		return _setValue(value, severity, status, notify, false);
	}
	/**
	 * Internal set: sets new value to this processor. It checks if it is fixed, it never sets fixed value.
	 * Fires value update event only if value was changes and notify is true.
	 * If force is true, then updates value and fires notify, if notify is true, even if value was not changed.
	 * If the value is not an array it tries to convert it to a single element array.
	 * Updates timestamp.
	 * If required fires notify event.
	 *
	 * @param value value to be set
	 * @param severity the severity to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param status the status to be used together with the value update. If <code>null</code> no alarm update is fired.
	 * @param notify if <code>true</code> fire notify event if value was change, <code>false</code> suppresses events
	 * @param force if <code>true</code> it will update value and timestamp even if there is not new value and proceeds with notify
	 * @return true if value was updated, regardless if based on difference or force
	 */
	protected boolean _setValue(boolean value, Severity severity, Status status, boolean notify, boolean force) {

		if (fixed) {
			return false;
		}
		
		this.timestamp= new TimeStamp();

		if (record.isAlarmUndefined() && (severity==null || status==null)) {
			severity=Severity.NO_ALARM;
			status=Status.NO_ALARM;
		}

		if (force || this.value==null || getValueAsBoolean()!=value) {
			// value has changed or we force update regardles the value change
			this.value = new byte[]{value ? (byte)1 : (byte)0};
			this.lastChangeTimestamp= this.timestamp;
			if (notify) {
				if (severity!=null && status!=null) {
					record.updateAlarm(severity, status, false);
				}
				record.fireValueChange();
			}
			return true;
		} else {
			// value has not been changed, we update just alarm
			if (severity!=null && status!=null) {
				record.updateAlarm(severity, status, notify);
			}
			return false;
		}
		
	}
	
	/**
	 * <p>getValueAsBoolean.</p>
	 *
	 * @return a boolean
	 */
	public boolean getValueAsBoolean() {
		if (value==null) {
			return false;
		}
		try {
			return Array.getByte(value,0)==1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Object getValue() {
		return value;
	}

	/**
	 * Returns timestamp of last value change.
	 *
	 * @return timestamp of last value change
	 */
	public TimeStamp getChangeAccessTimestamp() {
		return lastChangeTimestamp;
	}

	/**
	 * <p>isFixed.</p>
	 *
	 * @return a boolean
	 */
	public boolean isFixed() {
		return fixed;
	}

	/**
	 * <p>Setter for the field <code>fixed</code>.</p>
	 *
	 * @param fixed a boolean
	 */
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	/** {@inheritDoc} */
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		if (getType().isDOUBLE()) {
			if (record.getCount()==1) {
				_setValue(new double[]{config.getDouble("value",0.0)},null,null,false,true);
			} else {
				String[] s= config.getStringArray("value");
				if (s!=null && s.length>0) {
					double[] d= new double[s.length];
					for (int i = 0; i < d.length; i++) {
						d[i]= NumberUtils.toDouble(s[i]);
					}
					_setValue(d,null,null,false,true);
				}
			}
		} else if (getType().isFLOAT()) {
			if (record.getCount()==1) {
				_setValue(new float[]{config.getFloat("value",0.0f)},null,null,false,true);
			} else {
				String[] s= config.getStringArray("value");
				if (s!=null && s.length>0) {
					float[] d= new float[s.length];
					for (int i = 0; i < d.length; i++) {
						d[i]= NumberUtils.toFloat(s[i]);
					}
					_setValue(d,null,null,false,true);
				}
			}
		} else if (getType().isENUM() || getType().isSHORT()) {
			_setValue(new short[]{config.getShort("value",(short) 0)},null,null,false,true);
		} else if (getType().isINT()) {
			if (record.getCount()==1) {
				_setValue(new int[]{config.getInt("value",0)},null,null,false,true);
			} else {
				String[] s= config.getStringArray("value");
				if (s!=null && s.length>0) {
					int[] d= new int[s.length];
					for (int i = 0; i < d.length; i++) {
						d[i]= NumberUtils.toInt(s[i]);
					}
					_setValue(d,null,null,false,true);
				}
			}
		} else if (getType().isBYTE()) {
			_setValue(new byte[]{config.getByte("value",(byte) 0)},null,null,false,true);
		} else if (getType().isSTRING()) {
			String[] s= config.getStringArray("value");
			if (s!=null) {
				_setValue(s,null,null,false,true);
			} else {
				_setValue(new String[]{""},null,null,false,true);
			}
		} else {
			_setValue(new double[1],null,null,false,true);
		}
		
		this.fixed=config.getBoolean("fixed", false);
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (value==null) {
			log.warn("["+record.getName()+"] value is null!");
		}
		if (timestamp==null) {
			log.warn("["+record.getName()+"] timestamp is null!");
		}
	}
	
}
