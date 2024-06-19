package org.scictrl.csshell.server.test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;

/**
 * <p>RecordMonitor class.</p>
 *
 * @author igor@scictrl.com
 */
public class RecordMonitor implements PropertyChangeListener {

	private Record record;
	
	/**
	 * Catched values.
	 */
	public List<Double> values; 

	/**
	 * <p>Constructor for RecordMonitor.</p>
	 *
	 * @param record a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public RecordMonitor(Record record) {
		this.record=record;
		this.record.addPropertyChangeListener(Record.PROPERTY_VALUE, this);
		
		values= new ArrayList<Double>(128);
	}
	
	/**
	 * <p>release.</p>
	 */
	public void release() {
		record.removePropertyChangeListener(Record.PROPERTY_VALUE, this);
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		values.add(EPICSUtilities.toDouble(evt.getNewValue()));
	}

}
