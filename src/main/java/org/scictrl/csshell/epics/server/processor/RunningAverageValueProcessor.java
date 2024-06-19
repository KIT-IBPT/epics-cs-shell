/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>RunningAverageValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class RunningAverageValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	/**
	 * Average calculator.
	 */
	public static class RunningAverageBufferCalculator {
		/**
		 * Accumulated data, circular buffer.
		 */
		public double[] data;
		/**
		 * Index in circular buffer;
		 */
		public int idx=-1;
		/**
		 * Actual number of accumulated points
		 */
		public int size=0;
		/**
		 * Calculated average.
		 */
		public double avg;
		/**
		 * Calculated STD.
		 */
		public double std;
		/**
		 * Minimal value.
		 */
		public double min;
		/**
		 * Maximal value.
		 */
		public double max;
		
		/**
		 * Constructor.
		 * @param size size of buffer
		 */
		public RunningAverageBufferCalculator(int size) {
			this.data= new double[size];
			reset();
		}
		
		/**
		 * Resets buffer to be empty.
		 */
		public void reset() {
			idx=-1;
			size=0;
		}

		/**
		 * Adds value
		 * @param value new value
		 */
		public void add(double value) {
			idx++;
			if (idx>=data.length) {
				idx=0;
			}
			data[idx]=value;
			if (size<data.length) {
				size++;
			}
			
			double d=0;
			double dd=0;
			double mi=Double.POSITIVE_INFINITY;
			double ma=Double.NEGATIVE_INFINITY;
			for (int i = 0; i < size; i++) {
				d+=data[i];
				dd+=data[i]*data[i];
				if (data[i]>ma) {
					ma=data[i];
				}
				if (data[i]<mi) {
					mi=data[i];
				}
			}
			
			if (size==0) {
				avg=std=min=max=0.0;
			} else if (size==1) {
				avg=min=max=d;
				std=0.0;
			} else {
				avg=d/(double)size;
				dd=dd/(double)size;
				std=dd-avg*avg;
				if (size>2) {
					std=std*size/(size-1);
				}
				std=Math.sqrt(std);
				max=ma;
				min=mi;
			}
		}
	}
	
	private double treshold=Double.NaN;
	private String inputPV;
	private ValueLinks input;
	private int count;
	private RunningAverageBufferCalculator calc;

	/**
	 * <p>Constructor for RunningAverageValueProcessor.</p>
	 */
	public RunningAverageValueProcessor() {
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		if (config.containsKey("treshold")) {
			treshold= config.getDouble("treshold");
		}
		
		count= config.getInt("count", 10);
		
		inputPV= config.getString("inputPV");
		
		if (inputPV==null || inputPV.length()==0) {
			log.error("Configuration for '"+record.getName()+"' has no inputPV parameter!");
		}
		
		input= new ValueLinks(record.getName(), new String[]{inputPV}, this, Record.PROPERTY_VALUE);

		calc= new RunningAverageBufferCalculator(count);
		
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
			
			double[] val= input.consumeAsDoubles();
			
			
			if (val==null || val.length!=1) { 
				return;
			}
			
			if (!Double.isNaN(treshold) && Math.abs(val[0])<treshold) {
				return;
			}
			
			calc.add(val[0]);

			_setValue(calc.avg,Severity.NO_ALARM,Status.NO_ALARM, true);
		}

	}
	
}
