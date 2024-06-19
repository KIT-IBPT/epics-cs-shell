/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>UpdateRateProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class UpdateRateProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	private String inputPV;
	private ValueLinks input;
	private int count;
	private Deque<Long> vals;

	/**
	 * <p>Constructor for UpdateRateProcessor.</p>
	 */
	public UpdateRateProcessor() {
		type=DBRType.DOUBLE;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		count= config.getInt("count", 10);
		
		inputPV= config.getString("inputPV");
		
		if (inputPV==null || inputPV.length()==0) {
			log.error("Configuration for '"+record.getName()+"' has no inputPV parameter!");
		}
		
		input= new ValueLinks(record.getName(), new String[]{inputPV}, this, Record.PROPERTY_VALUE);

		vals= new LinkedList<Long>();
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		input.activate(record.getDatabase());
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource()==input) {

			if (input==null) {
				return;
			}
			if (input.isInvalid()) {
				record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			if (!input.isReady()) {
				return;
			}

			ValueHolder[] vh= input.consume();
			
			if (vh==null || vh.length<1 || vh[0]==null) {
				return;
			}
			
			long l1= vh[0].timestamp;

			vals.add(l1);
			
			while(vals.size()>count) {
				vals.pollFirst();
			}
			
			long l2= vals.peekFirst();
			
			if (l1==l2) {
				return;
			}
			
			double rate=(double)vals.size()*1000.0/(double)(l1-l2);
			
			_setValue(rate,Severity.NO_ALARM,Status.NO_ALARM, true);
		}

	}
	
}
