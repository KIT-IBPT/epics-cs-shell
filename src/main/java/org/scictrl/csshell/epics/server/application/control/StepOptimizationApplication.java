/**
 * 
 */
package org.scictrl.csshell.epics.server.application.control;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.application.control.Optimizer.State;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>StepOptimizationApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class StepOptimizationApplication extends AbstractApplication {

	/** Constant <code>OUTPUT="Output"</code> */
	public static final String OUTPUT = "Output";
	/** Constant <code>OUTPUT_IN_RANGE="Output:InRange"</code> */
	public static final String OUTPUT_IN_RANGE = "Output:InRange";
	/** Constant <code>INPUT="Input"</code> */
	public static final String INPUT = "Input";
	/** Constant <code>INPUT_MIN="Input:Min"</code> */
	public static final String INPUT_MIN = "Input:Min";
	/** Constant <code>INPUT_MAX="Input:Max"</code> */
	public static final String INPUT_MAX = "Input:Max";
	/** Constant <code>INPUT_PREC="Input:Prec"</code> */
	public static final String INPUT_PREC = "Input:Prec";
	/** Constant <code>OUTPUT_WAIT="Output:Wait"</code> */
	public static final String OUTPUT_WAIT = "Output:Wait";
	/** Constant <code>INPUT_START_MIN="Input:Start:Min"</code> */
	public static final String INPUT_START_MIN = "Input:Start:Min";
	/** Constant <code>INPUT_START_MAX="Input:Start:Max"</code> */
	public static final String INPUT_START_MAX = "Input:Start:Max";
	/** Constant <code>CMD_STOP="Cmd:Stop"</code> */
	public static final String CMD_STOP = "Cmd:Stop";
	/** Constant <code>CMD_START="Cmd:Start"</code> */
	public static final String CMD_START = "Cmd:Start";
	/** Constant <code>STATUS="Status"</code> */
	public static final String STATUS = "Status";
	/** Constant <code>BEST_INP="Best:Inp"</code> */
	public static final String BEST_INP = "Best:Inp";
	/** Constant <code>BEST_OUT="Best:Out"</code> */
	public static final String BEST_OUT = "Best:Out";
	/** Constant <code>LAST_INP="Last:Inp"</code> */
	public static final String LAST_INP = "Last:Inp";
	/** Constant <code>LAST_OUT="Last:Out"</code> */
	public static final String LAST_OUT = "Last:Out";
	

	class ScanningTask implements Runnable {
		
		private boolean aborted=false;

		public ScanningTask() {
		}
		
		public synchronized void abort() {
			aborted=true;
			notify();
		}
		
		public boolean isAborted() {
			return aborted;
		}
		
		@Override
		public synchronized void run() {
			
			log.debug("Scanning initiated");
			
			try {

				runLoop(this);
				
			} catch (Exception e) {
				log4error("Ramping failed!", e);
			}
			
			log.debug("Aborted");
			aborted=true;
			
			ScanningTask t= task;
			if (t==this) {
				getRecord(STATUS).setValue(0);
				task=null;
			}
		}
	}
	
	private ScanningTask task;
	private String inputPV;
	private String outputPV;
	private Optimizer optimizer;
	private String outputRangePV;

	
	/**
	 * <p>Constructor for StepOptimizationApplication.</p>
	 */
	public StepOptimizationApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		inputPV= config.getString("inputPv");
		outputPV= config.getString("outputPv");
		outputRangePV= config.getString("outputRangePv");
		
		addRecordOfOnLinkValueProcessor(OUTPUT, "Control output of system", DBRType.DOUBLE, outputPV);
		addRecordOfOnLinkValueProcessor(OUTPUT_IN_RANGE, "Control output in range", DBRType.BYTE, outputRangePV);
		
		addRecordOfMemoryValueProcessor(OUTPUT_WAIT, "Measurement wait", 0.0, 100.0, "s", (short)1, 1.0);

		addRecordOfOnLinkValueProcessor(INPUT, "Control input of system", DBRType.DOUBLE, inputPV);
		
		addRecordOfMemoryValueProcessor(INPUT_MIN, "Input optimization min", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(INPUT_MAX, "Input optimization max", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(INPUT_START_MIN, "Input optimization start value min", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(INPUT_START_MAX, "Input optimization start value max", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(INPUT_PREC, "Input optimization precision", 0.0, 1.0, "", (short)4, 0.0001);
		addRecordOfMemoryValueProcessor(CMD_STOP, "Stops scanning task", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CMD_START, "Start scanning task", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS, "Scanning status", new String[]{"READY","SCANNING","ERROR"}, (short)0);
		addRecordOfMemoryValueProcessor(BEST_INP, "Best input value", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(BEST_OUT, "Best output value", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(LAST_INP, "Last input value", -1000.0, 1000.0, "", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(LAST_OUT, "Last output value", -1000.0, 1000.0, "", (short)2, 0.0);
		
		getRecord(INPUT_MAX).setPersistent(true);
		getRecord(INPUT_MIN).setPersistent(true);
		getRecord(INPUT_START_MAX).setPersistent(true);
		getRecord(INPUT_START_MIN).setPersistent(true);
		getRecord(INPUT_PREC).setPersistent(true);
		getRecord(OUTPUT_WAIT).setPersistent(true);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		database.schedule(new Runnable() {
			
			@Override
			public void run() {
				Record r= getRecord(INPUT);
				getRecord(INPUT_MAX).copyFields(r);
				getRecord(INPUT_MIN).copyFields(r);
				getRecord(INPUT_START_MAX).copyFields(r);
				getRecord(INPUT_START_MIN).copyFields(r);
				getRecord(BEST_INP).copyFields(r);
				
				getRecord(BEST_OUT).copyFields(getRecord(OUTPUT));
			}
		}, 5000);
	}
	
	private void runLoop(ScanningTask task) {

		double min= getRecord(INPUT_MIN).getValueAsDouble();
		double max= getRecord(INPUT_MAX).getValueAsDouble();
		double smin= getRecord(INPUT_START_MIN).getValueAsDouble();
		double smax= getRecord(INPUT_START_MAX).getValueAsDouble();
		double prec= getRecord(INPUT_PREC).getValueAsDouble();
		
		optimizer= new ThreePointOptimizer();
		
		optimizer.initialize(min, max, prec, prec);
		
		
		if (smin<min) {
			smin=min;
		}
		
		if (smax>max) {
			smax=max;
		}
		
		ProbePoint[] points= new ProbePoint[3];
		points[0]=new ProbePoint(smin,0.0,false);
		points[1]=new ProbePoint(smin+(smax-smin)/2.0,0.0,false);
		points[2]=new ProbePoint(smax,0.0,false);

		if (task.isAborted()) {
			return;
		}
		takeMeasurements(task,points);
		if (task.isAborted()) {
			return;
		}

		getRecord(LAST_INP).setValue(points[0].inp);
		getRecord(LAST_OUT).setValue(points[0].out);
		getRecord(LAST_INP).setValue(points[1].inp);
		getRecord(LAST_OUT).setValue(points[1].out);
		getRecord(LAST_INP).setValue(points[2].inp);
		getRecord(LAST_OUT).setValue(points[2].out);
		getRecord(BEST_INP).setValue(points[1].inp);
		getRecord(BEST_OUT).setValue(points[1].out);
		
		while (optimizer.nextStep(points)==State.STEPPING) {
			if (task.isAborted()) {
				return;
			}
			ProbePoint best= optimizer.getBest();
			if (task.isAborted()) {
				return;
			}
			if (best!=null) {
				getRecord(BEST_INP).setValue(best.inp);
				getRecord(BEST_OUT).setValue(best.out);
			}
			
			getRecord(INPUT_MIN).setValue(optimizer.getMin());
			getRecord(INPUT_MAX).setValue(optimizer.getMax());
			
			points= optimizer.getInputs();
			takeMeasurements(task,points);
			getRecord(LAST_INP).setValue(points[0].inp);
			getRecord(LAST_OUT).setValue(points[0].out);
			getRecord(LAST_INP).setValue(points[1].inp);
			getRecord(LAST_OUT).setValue(points[1].out);
			getRecord(LAST_INP).setValue(points[2].inp);
			getRecord(LAST_OUT).setValue(points[2].out);
			if (task.isAborted()) {
				return;
			}
		}		

		
		
	}
	
	private void takeMeasurements(ScanningTask task, ProbePoint[] points) {
		
		Record inp= getRecord(INPUT);
		Record out= getRecord(OUTPUT);
		Record outInRange= getRecord(OUTPUT_IN_RANGE);
		
		long wait= (long)(getRecord(OUTPUT_WAIT).getValueAsDouble()*1000.0); 
		
		StringBuilder sb= new StringBuilder(128);
		sb.append("Points ");
		
		for (int i = 0; i < points.length; i++) {
			if (task.isAborted()) {
				return;
			}
			inp.setValue(points[i].inp);
			try {
				synchronized (task) {
					task.wait(wait);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (task.isAborted()) {
				return;
			}
			
			points[i].out= out.getValueAsDouble();
			points[i].valid= outInRange.getValueAsBoolean();
			
			sb.append(points[i].inp);
			sb.append(',');
			sb.append(points[i].out);
			sb.append(',');
			sb.append(points[i].valid);
			sb.append(';');

			if (task.isAborted()) {
				return;
			}
		}
		
		log.debug(sb.toString());
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name==CMD_START) {
			if (task!=null) {
				log.debug("Scan request denied, scan in progress");
				return;
			}
			
			getRecord(STATUS).setValue(1);

			task= new ScanningTask();
			database.schedule(task, 0);
			
		} else if (name==CMD_STOP) {

			getRecord(STATUS).setValue(0);

			ScanningTask t=task;
			task=null;
			if (t!=null) {
				t.abort();
			}
		}
	}


}
