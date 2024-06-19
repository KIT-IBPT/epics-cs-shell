package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Value processor, which distributes value to provided forward PV links.
 *
 * @author igor@scictrl.com
 */
public class ForwardValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	private static final String FORWARDS="forwards";
	private static final String INPUT="input";
	private static final String ENABLE="enable";

	/**
	 * Creates processor, configures it and returns it embedded within the returned record.
	 *
	 * @param name name of the returned record
	 * @param type type of the returned record
	 * @param description the description of the record
	 * @param link the link of the returned processor
	 * @return new record with embedded and configured processor
	 */
	public static final ForwardValueProcessor newProcessor(String name, DBRType type, String description, String link) {
		
		Record r= new Record(name, type, 1);

		ForwardValueProcessor lvp= new ForwardValueProcessor();
		lvp.configure(r,new HierarchicalConfiguration());
		
		r.setProcessor(lvp);
		
		return lvp;
	}

	
	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;
	private String[] forwardPVs;
	private String inputPV;
	private String enablePv;
	private ValueLinks forward;
	private ValueLinks enable;
	private Double setValue=null;

	/**
	 * <p>Constructor for ForwardValueProcessor.</p>
	 */
	public ForwardValueProcessor() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		forwardPVs= config.getStringArray("forwardPVs");
		inputPV= config.getString("inputPV");
		enablePv= config.getString("enablePV");
		if (config.containsKey("setValue")) {
			setValue= config.getDouble("setValue");
		}
		
		if (forwardPVs==null || forwardPVs.length==0 || forwardPVs[0]==null) {
			throw new IllegalArgumentException("Parameter 'forwardsPVs' is missing!");
		}

		forward = new ValueLinks(FORWARDS, forwardPVs, this, Record.PROPERTY_VALUE);
		
		if (inputPV!=null) {
			input = new ValueLinks(INPUT, inputPV, this, Record.PROPERTY_VALUE);
		}
		
		if (enablePv!=null) {
			enable = new ValueLinks(ENABLE, enablePv, this, Record.PROPERTY_VALUE);
		}
		
		record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		forward.activate(getRecord().getDatabase());

		if (input!=null) {
			input.activate(getRecord().getDatabase());
		}
		
		if (enable!=null) {
			enable.activate(getRecord().getDatabase());
		}
	}

	/** {@inheritDoc} */
	public void setValue(Object value) {
		
		if (setValue!=null) {
			value=setValue;
		}
		
		// we force because we want to fire further processing and we want to update timestamp
		_setValue(value, null, null, true, false);

		//this.timestamp= lastAccessTimestamp;
		
		try {
			
			boolean enabled= true;
			if (enable!=null) {
				enabled= enable.consumeAsBooleanAnd();
			}
			
			//System.out.println("SET "+enabled+" "+value);

			if (enabled) {
				forward.setValueToAll(this.value);
			}
			
		} catch (Exception e) {
			throw new IllegalStateException("Remote set failed", e);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		//System.out.println("PROP "+evt.getPropertyName()+" "+((ValueHolder[])evt.getNewValue())[0].doubleValue() );
		
		if (evt.getPropertyName()==INPUT) {
			
			if ( input!=null && input.isReady() && !input.isInvalid() && !input.isLastSeverityInvalid() ) {
				
				ValueHolder[] vh= input.consume();
				
				if (vh!=null && vh.length>0 && vh[0]!=null) {
					//System.out.println("VALU "+evt.getPropertyName()+" "+vh[0].doubleValue());
					setValue(vh[0].doubleValue());
				}
				
			}
			
		}
	}
	
	/**
	 * <p>Setter for the field <code>setValue</code>.</p>
	 *
	 * @param setValue a {@link java.lang.Double} object
	 */
	public void setSetValue(Double setValue) {
		this.setValue = setValue;
	}
	
	/**
	 * <p>Getter for the field <code>setValue</code>.</p>
	 *
	 * @return a {@link java.lang.Double} object
	 */
	public Double getSetValue() {
		return setValue;
	}
	
}
