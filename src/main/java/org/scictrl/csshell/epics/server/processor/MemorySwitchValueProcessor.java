package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Value processor, which sets forward PVs to off/on, remembers of forward PV was OOn before switching on and keeps that state. 
 *
 * @author igor@scictrl.com
 */
public class MemorySwitchValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	private static final String FORWARDS="forwards";

	/**
	 * Creates processor, configures it and returns it embedded within the returned record.
	 *
	 * @param name name of the returned record
	 * @param type type of the returned record
	 * @param description the description of the record
	 * @param link the link of the returned processor
	 * @return new record with embedded and configured processor
	 */
	public static final MemorySwitchValueProcessor newProcessor(String name, DBRType type, String description, String link) {
		
		Record r= new Record(name, type, 1);

		MemorySwitchValueProcessor lvp= new MemorySwitchValueProcessor();
		lvp.configure(r,new HierarchicalConfiguration());
		
		r.setProcessor(lvp);
		
		return lvp;
	}

	
	private String[] forwardPVs;
	private ValueLinks forward;
	private boolean[] select;

	/**
	 * <p>Constructor for ForwardValueProcessor.</p>
	 */
	public MemorySwitchValueProcessor() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		forwardPVs= config.getStringArray("forwardPVs");
		
		if (forwardPVs==null || forwardPVs.length==0 || forwardPVs[0]==null) {
			throw new IllegalArgumentException("Parameter 'forwardsPVs' is missing!");
		}

		forward = new ValueLinks(FORWARDS, forwardPVs, this, Record.PROPERTY_VALUE);
		
		select= new boolean[forwardPVs.length];
		Arrays.fill(select, true);
		
		record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		forward.activate(getRecord().getDatabase());

	}

	/** {@inheritDoc} */
	public void setValue(Object value) {
		
		// we force because we want to fire further processing and we want to update timestamp
		_setValue(value, null, null, true, false);

		//this.timestamp= lastAccessTimestamp;
		
		boolean b= getValueAsBoolean();
		
		if (b) {
			try {
				forward.setValueToAll(this.value,select);
			} catch (Exception e) {
				throw new IllegalStateException("Remote set failed", e);
			}
		} else {
			
			try {
				ValueHolder[] vh=  forward.getValue();
				
				for (int i=0; i < vh.length; i++) {
					ValueHolder v= vh[i];
					if (v!=null && !v.failed && v.severity!=null && v.severity.isLessThan(Severity.INVALID_ALARM)) {
						long l= v.longValue();
						select[i] = l!=0L;
					} else {
						select[i] = false;
					}
				}
				
				forward.setValueToAll(0L,select);
				
			} catch (Exception e) {
				throw new IllegalStateException("Remote set failed", e);
			}
			
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}
	
}
