package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Value processor, which connects to provided remote names and captures their value.
 * By default first value from array of linked values is used.
 * Value update events wit captured value are produced at the configured heartbeat rate.
 *
 * @author igor@scictrl.com
 */
public class HeartbeatValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	enum Filter {
		AVG,AVGPEAKPEAK;
	}
	
	
	private static final Object ZERO = new double[]{0.0};

	/**
	 * Creates processor, configures it and returns it embedded within the returned record.
	 *
	 * @param name name of the returned record
	 * @param description the description of the record
	 * @param link the link of the returned processor
	 * @return new record with embedded and configured processor
	 */
	public static final HeartbeatValueProcessor newProcessor(String name, String description, String link) {
		
		Record r= new Record(name, DBRType.DOUBLE, 1);

		HeartbeatValueProcessor lvp= new HeartbeatValueProcessor();
		lvp.configure(r,new HierarchicalConfiguration());
		lvp.configure(link, Record.PROPERTY_VALUE);
		
		r.setProcessor(lvp);
		
		return lvp;
	}

	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;
	/**
	 * Buffer that stores data between two heartbeat updated.
	 */
	protected List<Double> buffer= new ArrayList<Double>(100);
	private Double high;
	private Double low;
	private Double avg;
	private PolynomialTransformation transform;
	private Filter filter;

	/**
	 * <p>Constructor for HeartbeatValueProcessor.</p>
	 */
	public HeartbeatValueProcessor() {
		type=DBRType.DOUBLE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		record.setPersistent(true);
		
		String name= config.getString("input.link");
		
		String type= Record.toPropertyName(config.getString("input.type",Record.PROPERTY_VALUE));
		
		transform= new PolynomialTransformation();
		List<?> l=config.configurationsAt("transform"); 
		if (l.size()==1) {
			transform.configure((HierarchicalConfiguration) l.get(0));
		}
		
		filter= Filter.valueOf(config.getString("filter", Filter.AVGPEAKPEAK.toString()).toUpperCase());
		
		if (name!=null) {
			configure(name, type);
		}
		
	}
	
	private void configure(String name, String type) {
		this.input= new ValueLinks(record.getName(), new String[]{name}, this, type);
		record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (input!=null) {
			input.activate(getRecord().getDatabase());
			input.copyMetaData(getRecord());
		}
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
			
			if (vh.length!=1 || vh[0]==null) {
				return;
			}
			
			record.updateAlarm(vh[0].severity, vh[0].status, true);

			synchronized (buffer) {
				buffer.add(vh[0].doubleValue());
			}
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		synchronized (buffer) {
			if (filter==Filter.AVG) {
				if (buffer.size()==0) {
					avg=null;
				} else if (buffer.size()==1) {
					this.avg=transform.transformX(buffer.get(0));
				} else if (buffer.size()==2) {
					Double d1= transform.transformX(buffer.get(0));
					Double d2= transform.transformX(buffer.get(1));
					this.avg=(d1+d2)/2.0;
				} else if (buffer.size()>2) {
					double sum=0;
					for (Double d : buffer) {
						sum+=d;
					}
					this.avg=transform.transformX(sum/buffer.size());
				}
				buffer.clear();
			} else {
				if (buffer.size()==0) {
					high=null;
					low=null;
					avg=null;
				} else if (buffer.size()==1) {
					Double d= transform.transformX(buffer.get(0));
					this.high=d;
					this.low=d;
					this.avg=d;
				} else if (buffer.size()==2) {
					Double d1= transform.transformX(buffer.get(0));
					Double d2= transform.transformX(buffer.get(1));
					this.high=d1>d2 ? d1 : d2;
					this.low=d1<d2 ? d1 : d2;
					this.avg=d1;
				} else if (buffer.size()==3) {
					Collections.sort(buffer);
					this.low= transform.transformX(buffer.get(0));
					this.avg= transform.transformX((buffer.get(0)+buffer.get(1)+buffer.get(2))/3.0);
					this.high= transform.transformX(buffer.get(2));
				} else if (buffer.size()>3) {
					double sum=0;
					double high=-Double.MAX_VALUE;
					double low=Double.MAX_VALUE;
					for (Double d : buffer) {
						sum+=d;
						if (high<d) {
							high=d;
						}
						if (low>d) {
							low=d;
						}
					}
					this.high=transform.transformX(high);
					this.low=transform.transformX(low);
					this.avg=transform.transformX(sum/buffer.size());
				}
				buffer.clear();
			}
		}
		
		if (avg!=null) {
			_setValue(avg, null, null, true, true);
			if (filter==Filter.AVGPEAKPEAK) {
				if (low!=null && low!=avg && Math.abs(low-avg)>0.000001) {
					_setValue(low,null, null, true,true);
				}
				if (high!=null && high!=avg && Math.abs(high-avg)>0.000001) {
					_setValue(high,null, null, true,true);
				}
			}
		} else {
			if (value!=null) {
				_forceValueUpdateEvent();
			} else {
				_setValue(ZERO, null, null, true, true);
			}
		}
		
	}
	

}
