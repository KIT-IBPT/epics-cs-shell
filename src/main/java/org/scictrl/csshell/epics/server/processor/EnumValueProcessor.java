package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>EnumValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class EnumValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	
	/**
	 * <p>newProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param value a short
	 * @param fixed a boolean
	 * @param enumLabels an array of {@link java.lang.String} objects
	 * @return a {@link org.scictrl.csshell.epics.server.processor.EnumValueProcessor} object
	 */
	public static final EnumValueProcessor newProcessor(String name, String description, short value, boolean fixed, String[] enumLabels) {
		
		Record r= new Record(name, DBRType.ENUM, 1, null,null,null,null,null,null,null,null,null,(short)0,enumLabels,description);

		
		EnumValueProcessor mvp= new EnumValueProcessor();
		mvp.configure(r,new HierarchicalConfiguration());
		r.setProcessor(mvp);
		
		mvp.fixed=fixed;
		if (value>-1 && value<enumLabels.length) {
			mvp.value=new short[]{value};
		} else {
			LogManager.getLogger(EnumValueProcessor.class).warn("Requested value '"+value+"' ignored, it is out of enum bounds (0,"+enumLabels.length+")!");
		}
		mvp.timestamp= new TimeStamp();
		
		return mvp;
	}

	private double[] values;
	private ValueLinks output;
	
	/**
	 * <p>Constructor for EnumValueProcessor.</p>
	 */
	public EnumValueProcessor() {
		type=DBRType.ENUM;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		
		String pv= config.getString("output.link");
		
		if (pv!=null && pv.length()>0) {
			String type= Record.toPropertyName(config.getString("output.type",Record.PROPERTY_VALUE));
			String[] val = config.getStringArray("output.values");
			this.values= new double[val.length];
			
			if (val!=null) {
				for (int i = 0; i < val.length; i++) {
					try {
						double d= Double.parseDouble(val[i]);
						this.values[i]=d;
					} catch (Exception e) {
						log.warn("Failed to convert '"+val[i]+"' to double: "+e, e);
						this.values=null;
					}
					
				}
				if (values!=null && values.length==val.length) {
					
					this.output= new ValueLinks(record.getName(), pv, this, type);
					
				} else {
					
				}
			}
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource()==output) {

			if (output==null) {
				return;
			}
			if (output.isInvalid()) {
				record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			if (!output.isReady()) {
				return;
			}

			double[] vv= output.consumeAsDoubles();
			
			if (vv==null || vv.length<1) {
				return;
			}
			double v= vv[0];
			
			for (int i = 0; i < values.length; i++) {
				if (values[i]==v) {
					_setValue(i, null, null, true, false);
					return;
				}
			}
		}

		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (output!=null) {
			output.activate(getRecord().getDatabase());
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void setValue(Object value) {
		boolean change= super._setValue(value, null, null, true, false);
		
		if (change) {
			if (output!=null) {
				int i=-1;
				try {
					i = (int)Array.getLong(value, 0);
					if (i>=0 && i<values.length) {
						output.setValue(values[i]);
					}
				} catch (Throwable t) {
					log.error("Failed to set value with index '"+i+"': "+t.toString(), t);
					record.updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM, true);
				}
			}
		}
		
		
	}
	
	
}
