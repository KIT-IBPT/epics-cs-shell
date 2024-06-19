/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>InterlockValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class InterlockValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	private double treshold;
	private boolean interlockAbove;
	private String inputPV;
	private String outputPV;
	private ValueLinks input;
	private ValueLinks output;
	private double outputValue;
	private String enablePV;
	private ValueLinks enabled;

	/**
	 * <p>Constructor for InterlockValueProcessor.</p>
	 */
	public InterlockValueProcessor() {
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		interlockAbove = config.getBoolean("interlockAbove",true);
		treshold= config.getDouble("treshold", 0.1);
		outputValue= config.getDouble("outputValue", 0.1);
		
		inputPV= config.getString("inputPV");
		
		if (inputPV==null || inputPV.length()==0) {
			log.error("Configuration for '"+record.getName()+"' has no inputPV parameter!");
		}
		
		outputPV= config.getString("outputPV");
		
		if (outputPV==null || outputPV.length()==0) {
			log.error("Configuration for '"+record.getName()+"' has no outputPV parameter!");
		}

		enablePV= config.getString("enablePV");
		
		input= new ValueLinks(record.getName(), new String[]{inputPV}, this, Record.PROPERTY_VALUE);
		output= new ValueLinks(record.getName(), new String[]{outputPV}, this, Record.PROPERTY_VALUE);

		if (enablePV!=null) {
			enabled= new ValueLinks(record.getName(), new String[]{enablePV}, this, Record.PROPERTY_VALUE);
		}
		
		if(trigger==0) {
			trigger=1000;
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		input.activate(record.getDatabase());
		output.activate(record.getDatabase());
	}
	
	private boolean isEnabled() {
		if (enabled==null) {
			return true;
		}
		if (!enabled.isInvalid() && enabled.isReady()) {
			boolean b= enabled.consumeAsBooleanAnd();
			return b;
		}
		return false;
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource()==input) {

			if (input.isInvalid()) {
				record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			if (!input.isReady()) {
				return;
			}
			
			double[] val= input.consumeAsDoubles();
			
			
			if (val==null || val.length!=1) { 
				return;
			}
			
			checkInterlock(val[0]);
		}
	}
	
	/**
	 * <p>checkInterlock.</p>
	 *
	 * @param val a double
	 */
	public synchronized void checkInterlock(double val) {
		
		if ((interlockAbove && val>treshold) || (!interlockAbove && val<treshold)) {
			log.debug("Value "+val+" treshold "+treshold+" trigger above"+interlockAbove);
			try {
				if(isEnabled()) {
					output.setValue(outputValue);
				}
				_setValue(1, Severity.NO_ALARM,Status.NO_ALARM,true);
			} catch (Exception e) {
				log.error("Setting "+outputPV+" with "+outputValue+" failed!", e);
				record.updateAlarm(Severity.MAJOR_ALARM, Status.WRITE_ALARM);
			}
		} else {
			_setValue(0, Severity.NO_ALARM,Status.NO_ALARM,true);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		if (input.isInvalid()) {
			record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
			return;
		}
		if (!input.isReady()) {
			return;
		}
		
		ValueHolder[] val;
		try {
			val = input.getValue();
		} catch (Exception e) {
			log.error("Failed to read value", e);
			record.updateAlarm(Severity.MAJOR_ALARM, Status.WRITE_ALARM);
			return;
		}
		
		if (val==null || val.length!=1) { 
			return;
		}
		
		if (val[0].isAlarm()) { 
			record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
			return;
		}
		
		checkInterlock(val[0].doubleValue());

	}
	
}
