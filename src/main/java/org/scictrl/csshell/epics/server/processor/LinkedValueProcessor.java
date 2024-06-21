package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>Value processor, which connects to provided remote names and captures their value.
 * By default first value from array of linked values is used. Implementation can extend this behavior.
 * To implement own processing of linked values, then override the {@link #processInput(ValueHolder[])} method. </p>
 *
 * <p>Link works in both ways, setting this processor forwards value to remote link as well.
 * Polynomial transformation can be used to convert value in both directions.</p>
 *
 * @author igor@scictrl.com
 */
public class LinkedValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {


	/**
	 * Creates processor, configures it and returns it embedded within the returned record.
	 *
	 * @param name name of the returned record
	 * @param type type of the returned record
	 * @param description the description of the record
	 * @param link the link of the returned processor
	 * @return new record with embedded and configured processor
	 */
	public static final LinkedValueProcessor newProcessor(String name, DBRType type, String description, String link) {
		
		Record r= new Record(name, type, 1);

		LinkedValueProcessor lvp= new LinkedValueProcessor();
		lvp.configure(r,new HierarchicalConfiguration());
		lvp.reconnect(link, Record.PROPERTY_VALUE);
		
		r.setProcessor(lvp);
		
		return lvp;
	}

	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;
	private String[] inpNames;
	private String inpType;
	private PolynomialTransformation transform;
	private PolynomialTransformation transformInv;
	private boolean hasMetadata = false;
	private boolean writable = false;

	/**
	 * <p>Constructor for LinkedValueProcessor.</p>
	 */
	public LinkedValueProcessor() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		writable= config.getBoolean("writable",true);

		inpNames= config.getStringArray("input.links");
		
		inpType= Record.toPropertyName(config.getString("input.type",Record.PROPERTY_VALUE));
		
		List<HierarchicalConfiguration> l=config.configurationsAt("transform"); 
		if (l.size()==1) {
			transform= new PolynomialTransformation();
			transform.configure(l.get(0));
		}

		l=config.configurationsAt("transformInv"); 
		if (l.size()==1) {
			transformInv= new PolynomialTransformation();
			transformInv.configure(l.get(0));
		}
		
		hasMetadata=false;

		reconnect();
		
	}
	
	/**
	 * <p>reconnect.</p>
	 */
	public void reconnect() {
		reconnect(inpNames, inpType);
	}
	
	
	/**
	 * <p>reconnect.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link java.lang.String} object
	 */
	public void reconnect(String name, String type) {
		if (name==null || name.trim().length()==0) {
			return;
		}
		reconnect(new String[]{name}, type);
	}
	/**
	 * <p>reconnect.</p>
	 *
	 * @param names an array of {@link java.lang.String} objects
	 * @param type a {@link java.lang.String} object
	 */
	public void reconnect(String[] names, String type) {
		
		if (names==null || names.length==0 || names[0]==null || names[0].trim().length()==0) {
			return;
		}
		
		if (type==null) {
			type=Record.PROPERTY_VALUE;
		}
		
		synchronized (this) {
			if (input != null) {
				input.deactivate();
			}
			
			hasMetadata=false;
			
			input = new ValueLinks(record.getName(), names, this, type);
			record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
			
			if (getRecord()!=null && getRecord().isActivated() && input!=null) {
				input.activate(getRecord().getDatabase());
			}

		}
		
		copyMetadata();
		
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (input!=null) {
			input.activate(getRecord().getDatabase());
			hasMetadata=false;
			copyMetadata();
		}
	}
	
	
	/** {@inheritDoc} */
	public void setValue(Object val) {
		if ( input!=null && !input.isInvalid() ) {
			
			Object valInv=val;
			if (transformInv!=null) {
				double d=EPICSUtilities.toDouble(val);
				d= transformInv.transformX(d);
				valInv=Double.valueOf(d);
			}

			// we force because we want to fire further processing and we want to update timestamp
			_setValue(val, null, null, true, false);

			// remote channel should reset the UDF alarm not me, in case of linked
			//if (record.getAlarmStatus()==Status.UDF_ALARM) {
			//	record.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM, true);
			//}
			
			if (writable) {
				try {
					input.setValueToAll(valInv);
				} catch (Exception e) {
					throw new IllegalStateException("Remote set failed", e);
				}
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		if (evt.getSource()==input) {

			if (input==null) {
				return;
			}
			
			if (!input.isReady()) {
				return;
			}

			if (input.isInvalid()) {
				record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			
			if (!hasMetadata) {
				copyMetadata();
			}
			
			_setValue(processInput(input.consume()),null, null, true,false);
		}
		
	}
	
	/**
	 * Override this method to provide own conversion from input values to new processor value.
	 *
	 * @param inputValues array wit input values
	 * @return new value which is set to this processor
	 */
	protected Object processInput(ValueHolder[] inputValues) {
		
		Severity severity = Severity.NO_ALARM;
		Status status = Status.NO_ALARM;
		
		for (int i = 0; i < inputValues.length; i++) {
			if (severity.isLessThan(inputValues[i].severity)) {
				severity = inputValues[i].severity;
				status = inputValues[i].status;
			}
		}
		
		record.updateAlarm(severity, status, true);
		
		if (transform!=null) {
			return transform.transformX(inputValues[0].doubleValue());
		}

		return inputValues[0].value;
	}
	
	/**
	 * Returns true if links are connected and ready to be used.
	 *
	 * @return a boolean
	 */
	public boolean isInvalid() {
		if (input==null) {
			return true;
		}
		
		return input.isInvalid(); 
	}
	
	/**
	 * <p>printLinkDebug.</p>
	 *
	 * @param ap a {@link java.lang.Appendable} object
	 * @throws java.io.IOException if any.
	 */
	public void printLinkDebug(Appendable ap) throws IOException {
		ap.append("LinkedValueProcessor["+toString()+"]:");
		ap.append("Name:");
		ap.append(getName());
		ap.append(",Invalid:");
		ap.append(Boolean.toString(isInvalid()));
		ap.append(",");
		if (input==null) {
			ap.append("Links are NULL!");
		} else {
			input.printDebug(ap);
		}
	}
	
	private void copyMetadata() {
		
		if (!hasMetadata && getRecord()!=null && getRecord().isActivated() && input!=null && input.isReady()) {

			if (transform!=null) {
				MetaData data= input.getMetaData(0);
				MetaData trans= new MetaDataImpl(
						data.getName(),
						null,
						transform.transformX(data.getMinimum()),
						transform.transformX(data.getMaximum()),
						transform.transformX(data.getDisplayMin()),
						transform.transformX(data.getDisplayMax()),
						transform.transformX(data.getWarnMin()),
						transform.transformX(data.getWarnMax()),
						transform.transformX(data.getAlarmMin()),
						transform.transformX(data.getAlarmMax()),
						null,
						null,
						null,
						null,
						1,
						(int) getRecord().getPrecision(),
						DataType.DOUBLE,
						null,
						true,
						false,
						null);
				getRecord().copyFields(trans);
			} else {
				input.copyMetaData(getRecord());
			}
			
			hasMetadata=true;
			
		}

	}

}
