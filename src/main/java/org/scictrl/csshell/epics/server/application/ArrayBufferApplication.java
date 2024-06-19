/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;

/**
 * Stores last N values of array in ring-buffer kind of store.
 * If trigger PV goes to 1, it locks itself, therfore state is frozen and no new updates are updating ring buffer.
 * If reset PV goes to 1, it resets the loc and again accepts updates to the ring buffer.
 *
 * @author igor@scictrl.com
 */
public class ArrayBufferApplication extends AbstractApplication {
	
	private static final String TRIGGER = "Trigger";
	private static final String RESET = "Reset";
	private static final String INPUT = "Input";
	private static final String BUFFER = "Buffer";
	private static final String TIME = "Time";
	private static final String LOCKED = "Locked";

	private int idx=-1;
	private int size;
	private String inputPV;
	private MemoryValueProcessor[] buffer;
	private MemoryValueProcessor[] time;
	private ValueLinks input;
	private SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private Record locked;
	private String triggerPV;
	private ValueLinks trigger;
	private Boolean last;
	private String resetPV;
	private ValueLinks reset;
	
	/**
	 * <p>Constructor for ArrayBufferApplication.</p>
	 */
	public ArrayBufferApplication() {
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		size= config.getInt("size", 5); 

		inputPV= config.getString("inputPV");
		
		if ( inputPV==null || inputPV.length()<1 ) {
			throw new IllegalArgumentException("Parameter 'input' can not be empty");
		}
		
		input= connectLinks(INPUT, inputPV);

		triggerPV= config.getString("triggerPV");
		if (triggerPV!=null && triggerPV.length()>0) {
			trigger= connectLinks(TRIGGER, triggerPV);
		}
		
		resetPV= config.getString("resetPV");
		if (resetPV!=null && resetPV.length()>0) {
			reset= connectLinks(RESET, resetPV);
		}

		buffer= new MemoryValueProcessor[size];
		time= new MemoryValueProcessor[size];
		
		double[] d= new double[2048];
		
		for (int i = 0; i < size; i++) {
			String n= String.format("%02d", i+1);
			buffer[i]= (MemoryValueProcessor)addRecordOfMemoryValueProcessor(BUFFER+":"+n, "Buffer no. "+n,DBRType.DOUBLE,d).getProcessor();
			time[i]= (MemoryValueProcessor)addRecordOfMemoryValueProcessor(BUFFER+":"+n+":"+TIME, "Update time no. "+n,DBRType.STRING, "").getProcessor();
		}

		locked= addRecordOfMemoryValueProcessor(LOCKED, "Locks current state", DBRType.BYTE, (byte)0);
		

	}
	
	private boolean isLocked() {
		return locked.getValueAsBoolean();
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		if (name==INPUT) {
			if (!input.isInvalid() && input.isReady() && !input.isLastSeverityInvalid()) {
				
				ValueHolder[] vh= input.consume();
				
				if (vh==null || vh[0]==null) {
					return;
				}
				
				ValueHolder vh1 = vh[0];
				
				if (!isLocked()) {
					update(vh1.timestamp,vh1.doubleArrayValue());
				}
				
			}
		} else if (name==TRIGGER) {
			if (trigger!=null && trigger.isReady() && !trigger.isInvalid() && !trigger.isLastSeverityInvalid()) {
				Boolean b= trigger.consumeAsBooleanAnd();
				if (b!=null) {
					if (last!=null) {
						if (last.booleanValue() != b.booleanValue() && b) {
							lock();
						}
					} else {
						last=b;
					}
				}
			}
		} else if (name==RESET) {
			if (reset!=null && reset.isReady() && !reset.isInvalid() && !reset.isLastSeverityInvalid()) {
				Boolean b= reset.consumeAsBooleanAnd();
				if (b!=null) {
					if (b) {
						unlock();
					}
				}
			}
		}
	}
	
	private void lock() {
		locked.setValue(true);
	}
	
	private void unlock() {
		locked.setValue(false);
	}

	private void update(long ts, double[] d) {
		
		idx++;
		
		idx= idx%size;
		
		buffer[idx].setValue(d);
		time[idx].setValue(format.format(new Date(ts)));
		
	}

}
