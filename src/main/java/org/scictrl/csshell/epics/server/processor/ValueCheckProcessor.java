/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.Checks.LinkCheck;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Checks values of PVs and returns tru or false if values match the criteria. Each checked PV has own criteria.
 *
 * @author igor@scictrl.com
 */
public class ValueCheckProcessor extends MemoryValueProcessor implements PropertyChangeListener {
	
	
	Checks<LinkCheck> checks;

	/**
	 * <p>Constructor for ValueCheckProcessor.</p>
	 */
	public ValueCheckProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);

		checks= Checks.linkChecks(config, "checks", this);
		
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		checks.activate(getRecord().getDatabase());
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		Status st= Status.NO_ALARM;
		
		boolean b= checks.check(st);
		
		st= checks.getLastStatus();
		
		if (st==Status.NO_ALARM) {
			_setValue(b, Severity.NO_ALARM, Status.NO_ALARM, true);
		} else {
			_setValue(false, Severity.INVALID_ALARM, st, true);
		}
	}
	
}

