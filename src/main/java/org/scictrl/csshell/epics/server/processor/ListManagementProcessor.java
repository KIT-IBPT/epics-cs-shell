/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Database;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>ListManagementProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class ListManagementProcessor extends ManagementProcessor implements PropertyChangeListener {

	
	/**
	 * <p>Constructor for ListManagementProcessor.</p>
	 */
	public ListManagementProcessor() {
		type=DBRType.STRING;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		record.addPropertyChangeListener(Record.PROPERTY_RECORD,this);
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName()==Record.PROPERTY_RECORD) {
			
			record.removePropertyChangeListener(Record.PROPERTY_RECORD, this);
			record.getDatabase().addPropertyChangeListener(this);
			updatValue();
			
		} else if (evt.getPropertyName()==Database.PROPERTY_RECORDS) {
			
			updatValue();
			
		}
	}
	
	private void updatValue() {
		String[] s= record.getDatabase().getNames();
		Arrays.sort(s);
		_setValue(s,null, null, true,false);
	}
}

