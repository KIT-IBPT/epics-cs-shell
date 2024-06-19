package org.scictrl.csshell.epics.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cosylab.epics.caj.cas.handlers.AbstractCASResponseHandler;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Monitor;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableReadCallback;
import gov.aps.jca.cas.ProcessVariableWriteCallback;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.GR;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.TIME;

/**
 * <p>ProcessVariable4Record class.</p>
 *
 * @author igor@scictrl.com
 */
public class ProcessVariable4Record extends ProcessVariable implements PropertyChangeListener {

	Logger log= LogManager.getLogger(getClass()); 
	
	private Record record;

	/**
	 * <p>Constructor for ProcessVariable4Record.</p>
	 *
	 * @param record a {@link org.scictrl.csshell.epics.server.Record} object
	 * @param eventCallback a {@link gov.aps.jca.cas.ProcessVariableEventCallback} object
	 */
	public ProcessVariable4Record(Record record,
			ProcessVariableEventCallback eventCallback) {
		super(record.getName(), eventCallback);
		this.record=record;
		this.record.addPropertyChangeListener(this);
	}

	/** {@inheritDoc} */
	@Override
	public DBRType getType() {
		return record.getType();
	}
	
	/** {@inheritDoc} */
	@Override
	public int getDimensionSize(int dimension) {
		return record.getCount();
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getEnumLabels() {
		return record.getEnumLabels();
	}

	/** {@inheritDoc} */
	@Override
	public CAStatus read(DBR value,
			ProcessVariableReadCallback asyncReadCallback) throws CAException {
		
		try {
			fillRecordMeta(value);
			boolean ok= fillRecordValue(value,"read");
			if (ok) {
				return CAStatus.NORMAL;
			}
		} catch (Throwable t) {
			log.error("Internal error reading from '"+record.getName()+"':"+t, t);
			return CAStatus.INTERNAL;
		}
		return CAStatus.GETFAIL;
	}

	/**
	 * Fills dbr with value from the record, if value is not valid or fill fails, returns <code>false</code>.
	 * @param dbr the DBR to be filled in
	 * @param id the string identifying log report
	 * @return returns <code>false</code> if not possible to fill with valid value
	 */
	private boolean fillRecordValue(DBR dbr, String id) {
			Object o = record.getValue();
			if (o==null) {
				log.error("Value of record ("+record.getName()+") value can never be null!");
				return false;
			}
			if (!o.getClass().isArray()) {
				log.error("Record ("+record.getName()+") value is not an array!");
				return false;
			}
			int dataCount= Array.getLength(o);
			if (dataCount==0) {
				log.error("Record ("+record.getName()+") value not initialized, array length 0!");
				return false;
			}
			int minCount = Math.min(dataCount, dbr.getCount());
			if (log.isDebugEnabled()) {
				log.debug("["+record.getName()+"] "+id+" "+dbr.getType().getName()+" dbr_count "+dbr.getCount()+" data_count "+dataCount);
			}
			System.arraycopy(o, 0, dbr.getValue(), 0, minCount);
			return true;
	}

	private void fillRecordMeta(DBR value) {
		
		if (value.isTIME()) {
			TIME t = (TIME)value;
			if (record.getTimestamp()==null) {
				log.warn("["+record.getName()+"] timestamp is NULL!!");
			}
			t.setTimeStamp(record.getTimestamp());
		}
		
		if (value.isSTS()) {
			STS sts = (STS)value;
			sts.setSeverity(record.getAlarmSeverity());
			sts.setStatus(record.getAlarmStatus());
		}

		if (value.isGR()) {
			// fill GR
			GR gr = (GR)value;
			gr.setUnits(record.getUnits());
			gr.setUpperDispLimit(record.getUpperDispLimit());
			gr.setLowerDispLimit(record.getLowerDispLimit());
			gr.setUpperAlarmLimit(record.getUpperAlarmLimit());
			gr.setUpperWarningLimit(record.getUpperWarningLimit());
			gr.setLowerWarningLimit(record.getLowerWarningLimit());
			gr.setLowerAlarmLimit(record.getLowerAlarmLimit());
		}

		if (value.isCTRL()) {
			// fill-up GR to CTRL
			CTRL ctrl = (CTRL)value;
			ctrl.setUpperCtrlLimit(record.getUpperCtrlLimit());
			ctrl.setLowerCtrlLimit(record.getLowerCtrlLimit());
		}

		if (value.isPRECSION()) {
			// fill PRECISION
			PRECISION precision = (PRECISION)value;
			precision.setPrecision(record.getPrecision());
		}

		if (value.isLABELS()) {
			// fill LABELS
			LABELS labels = (LABELS)value;
			labels.setLabels(record.getEnumLabels());
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public CAStatus write(DBR value,
			ProcessVariableWriteCallback asyncWriteCallback) throws CAException {

		try {
			log.debug("["+record.getName()+"] write "+value.getType()+" count "+value.getCount()+" "+value.getValue());
			record.write(value.getValue());
		    // notify
		    if (interest)
		    {
		    	DBR monitorDBR = AbstractCASResponseHandler.createDBRforReading(this);
				fillRecordMeta(monitorDBR);
				Object val= record.getValue();
				System.arraycopy(val, 0, monitorDBR.getValue(), 0, Array.getLength(val));
		    	eventCallback.postEvent(Monitor.VALUE|Monitor.LOG, monitorDBR);
		    }
			return CAStatus.NORMAL;
		} catch (Throwable t) {
			log.error("Internal error writing to '"+record.getName()+"':"+t, t);
			return CAStatus.INTERNAL;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	    if (!interest) {
	    	return;
	    }
		
    	DBR monitorDBR = AbstractCASResponseHandler.createDBRforReading(this);
		fillRecordMeta(monitorDBR);
		if (fillRecordValue(monitorDBR,"monitor")) {
			if (evt.getPropertyName()==Record.PROPERTY_VALUE) {
		    	eventCallback.postEvent(Monitor.VALUE|Monitor.ALARM|Monitor.LOG, monitorDBR);
			} else if (evt.getPropertyName()==Record.PROPERTY_ALARM) {
				eventCallback.postEvent(Monitor.ALARM, monitorDBR);
			}
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void destroy() {
		super.destroy();
		record.getDatabase().getServer().unregisterProcessVariable(name);
		record.removePropertyChangeListener(this);
		record=null;
	}

}
