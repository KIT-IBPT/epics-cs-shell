package org.scictrl.csshell.epics.server.application.cycling;

/**
 * This type was created in VisualAge.
 *
 * @author igor@scictrl.com
 */
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.server.Database;

/**
 * Thread performing ramping procedure.
 */
public class RampingThread extends Thread {
	/** Constant <code>ABORT="ABORT"</code> */
	public static final String ABORT = "ABORT";
	/** Constant <code>END="END"</code> */
	public static final String END = "END";
	/** Constant <code>PROGRESS="PROGRESS"</code> */
	public static final String PROGRESS = "PROGRESS";
	/** Constant <code>ERROR="ERROR"</code> */
	public static final String ERROR = "ERROR";
	/** Constant <code>CONNECTION_FAIL="CONNECTION_FAIL"</code> */
	public static final String CONNECTION_FAIL= "CONNECTION_FAIL";
	
	class ProgressCalculator {
		private double value=0.0;
		private int steps;
		private double advance=1.0;
		private double perStep;
		private int stepCount=0;
		private boolean advanced= false;
		private java.util.List<ProgressCalculator> calc= new ArrayList<RampingThread.ProgressCalculator>();
		
		/**
		 * Creates new instance of calculator
		 * @param steps number of steps to increase progress value for the advance
		 * @param advance the progress wearched after number of steps
		 */
		public ProgressCalculator(int steps, double advance) {
			this.steps=steps;
			this.advance=advance;
			perStep=steps>0 ? advance/steps : 0.0;
		}
		/**
		 * Creates new instance of calculator.
		 * @param steps number of steps to increase progress value to 1.0
		 */
		public ProgressCalculator(int steps) {
			this(steps,1.0);
		}
		/**
		 * Creates new instance of calculator with 0 change per step. Reaches 1.0 after first step.
		 */
		public ProgressCalculator() {
			this(0,1.0);
		}
		/**
		 * Advances progress value for 1/steps.
		 * After full number of steps are reached, then stops advancing.
		 */
		void step() {
			stepCount++;
			if (stepCount>steps) {
				advance();
				return;
			}
			value=value+perStep;
		}
		/**
		 * Advances progress to full steps value in one go. No more advanccing after this is called.
		 */
		void advance() {
			stepCount=steps;
			value=advance;
			advanced=true;
		}
		/**
		 * Forks progress to new calculator whcih has 0 steps.  
		 * @param branches defines number of equivalent branches to which calculator is forked.
		 * @return new caluclator
		 */
		ProgressCalculator fork(int branches) {
			return fork(0,1.0/(double)branches);
		}
		/**
		 * Forks progress to new calculator  
		 * @param steps number of steps for forked calculator to reach max value
		 * @param part defined maximum progress value as part of maximum advance of this calculator
		 * @return new caluclator
		 */
		ProgressCalculator fork(int steps, double part) {
			ProgressCalculator pc= new ProgressCalculator(steps, part*advance);
			calc.add(pc);
			return pc;
		}
		/**
		 * Splits this calculator to a  
		 * @param steps number of steps for forked calculator to reach max value
		 * @param part defined maximum progress value as part of maximum advance of this calculator
		 * @return new caluclator
		 */
		ProgressCalculator split(int steps) {
			ProgressCalculator pc= new ProgressCalculator(steps, advance);
			calc.add(pc);
			return pc;
		}
		double getProgress() {
			if (advanced) {
				return advance;
			}
			double d= calc.size()>0 ? 0.0 : value;
			for (ProgressCalculator pc : calc) {
				d+=pc.getProgress();
			}
			return d;
		}
		@Override
		public String toString() {
			return "CALC "+stepCount+"/"+steps+" "+advance;
		}
	}
	
	
	private CyclingParameters cyclingParameters;
	private double step = 0.0;
	private EPICSConnection<Double> get;
	private EPICSConnection<Double> set;
	private boolean aborted=false;
	private PropertyChangeListener listener;
	private String message;
	private boolean cycling=false;
	//private double progressPhaseStep;
	private boolean error=false;
	private MetaData limits;
	private ProgressCalculator pCalc;
	private double progress=0.0;
	private int totalSteps;
	private String getPV;
	private String setPV;
	private Database db;
	private boolean linear;
	/**
	 * PSCyclingBean constructor comment.
	 *
	 * @param cyclingParameters a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 * @param getPV a {@link java.lang.String} object
	 * @param setPV a {@link java.lang.String} object
	 * @param limits a {@link org.scictrl.csshell.MetaData} object
	 * @param database a {@link org.scictrl.csshell.epics.server.Database} object
	 * @param linear a boolean
	 */
	public RampingThread(CyclingParameters cyclingParameters, String getPV, String setPV, MetaData limits, Database database, boolean linear) {
		super();
		this.cyclingParameters = cyclingParameters;
		this.getPV=getPV;
		this.setPV=setPV;
		this.limits=limits;
		this.db=database;
		this.pCalc= new ProgressCalculator();
		this.linear=linear;
	}
	
	/**
	 * <p>start.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void start(PropertyChangeListener l) {
		this.listener=l;
		start();
	}
	
	/**
	 * This method was created in VisualAge.
	 * @throws RemoteException 
	 */
	private void ramp(double first, double last, ProgressCalculator pc) throws RemoteException {
	
		if (isAborted()) return;
	
		int steps= (int)Math.ceil(Math.abs(last-first)/step);
		double myStep= last>first ? step : -step;
	
		ProgressCalculator pc1= pc.split(steps);
		
		for (int i = 0; i < steps-1; i++) {
	
			if (isAborted()) return;
				
			setCurrent(first + (i+1) * myStep );
			pc1.step();
			reportProgress();
			sleeping((long)(cyclingParameters.getWaitBetweenSteps()*1000));
		}
	
		if (isAborted()) return;
	
		setCurrent(last);
		pc1.advance();
		pc.advance();
		reportProgress();
		sleeping((long)(cyclingParameters.getWaitBetweenSteps()*1000));
	
		/*long time= System.currentTimeMillis();
		while (Math.abs(getPV.getValue() - last) > 0.0001
			&& System.currentTimeMillis() - time < (long)(cyclingParameters.waitAtLimits*1000)) {
	
			setCurrent(last);
			sleeping(100);
			if (isAborted()) return;
		}*/
	}
	
	private void reportError(String message) {
		this.message=message;
		this.aborted=true;
		this.error=true;
		this.cycling=false;
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, ERROR, false, true));
		}
	}
	private void reportConnectionFail(String message) {
		this.message=message;
		this.aborted=true;
		this.error=true;
		this.cycling=false;
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, CONNECTION_FAIL, false, true));
		}
	}
	private void reportAbort(String message) {
		this.message=message;
		this.aborted=true;
		this.cycling=false;
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, ABORT, false, true));
		}
	}
	private void reportEnd(String message) {
		this.message=message;
		this.cycling=false;
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, END, false, true));
		}
	}
	private void reportDeviceValue(double d) {
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, CyclingApplication.DEVICE_FINAL_VALUE, null, d));
		}
	}
	/**
	 * This method was created in VisualAge.
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			
			get= (EPICSConnection<Double>)db.getConnector().newConnection(getPV, DataType.DOUBLE); 
			set= (EPICSConnection<Double>)db.getConnector().newConnection(setPV, DataType.DOUBLE);
			
			get.waitTillConnected();
			set.waitTillConnected();
			
			if (get.getStatus().isSet(org.scictrl.csshell.Status.State.FAILED) || set.getStatus().isSet(org.scictrl.csshell.Status.State.FAILED)) {
				reportConnectionFail("Connection failed!");
				return;
			}
			
			reportProgress();

			if (aborted) {
				reportAbort("Aborted before even started.");
				return;
			}
	
			if (cyclingParameters.getNoCycles() == 0) {
				pCalc.advance();
				reportProgress();
				reportError("No cycles to perform.");
				return;
			}
	
			
			cycling=true;
	
			double max = cyclingParameters.getMaxLimit();	
			double min = cyclingParameters.getMinLimit();

			if (cyclingParameters.isUseDeviceLimits()) {
				if (limits!=null) {
					max= limits.getMaximum();
					min= limits.getMinimum();
				} else {
					max= set.getMetaData().getMaximum();
					min= set.getMetaData().getMinimum();
				}
			}
	
			if (max<min || max==min || max-min<0.1) {
				pCalc.advance();
				reportProgress();
				reportError("Min ("+min+") and max ("+max+") not usable.");
				return;
			}
			
			double mmax=max;
			double mmin=min;

			step= max / cyclingParameters.getStepsPerRamp();
	
			if (step<0 || step>99) {
				pCalc.advance();
				reportProgress();
				reportError("Number of steps ("+step+") out of range.");
				return;
			}

			double initialCurrent= get.getValue();
			
			reportDeviceValue(initialCurrent);
	
			// first cycle
			// goes from current value to maxand than to min
	
			totalSteps= cyclingParameters.getNoCycles()*4;
			if (cyclingParameters.isStartingAsFinal()) {
				if (initialCurrent != min) {
					totalSteps+=1;
				}
			} else {
				if (cyclingParameters.getFinalValue() != min) {
					totalSteps+=1;
				}
			}
			//progressPhaseStep = 1.0/totalSteps;

			ProgressCalculator pc= pCalc.fork(totalSteps);
			ramp(initialCurrent, max, pc);
			pc.advance();
			reportProgress();
			if (aborted) return;
	
			pc= pCalc.fork(totalSteps);
			waitAtLimit(max, pc);
			pc.advance();
			reportProgress();
			if (aborted) return;
	
			pc= pCalc.fork(totalSteps);
			ramp(max, min, pc);
			pc.advance();
			reportProgress();
			if (aborted) return;
	

			for (int i= 0; i < cyclingParameters.getNoCycles() - 1; i++) {
	
				pc= pCalc.fork(totalSteps);
				waitAtLimit(min,pc);
				pc.advance();
				reportProgress();
				if (aborted) return;
	
				// reduce max by decreement procantage
				if (min<0.0) {
					if (linear) {
						max= max - mmax * cyclingParameters.getCycleDecrement()/100.0;
						if (max<0.0) {
							max=0.0;
						}
					} else {
						max=max*(1.0-cyclingParameters.getCycleDecrement()/100.0);
					}
				}
	
				pc= pCalc.fork(totalSteps);
				ramp(min, max, pc);
				pc.advance();
				reportProgress();
				if (aborted) return;
	
				pc= pCalc.fork(totalSteps);
				waitAtLimit(max, pc);
				pc.advance();
				reportProgress();
				if (aborted) return;
	
				// reduce max by decreement procantage
				if (min<0.0) {
					if (linear) {
						min= min - mmin * cyclingParameters.getCycleDecrement()/100.0;
						if (min>0.0) {
							min=0.0;
						}
					} else {
						min=min*(1.0-cyclingParameters.getCycleDecrement()/100.0);
					}
				}
	
				pc= pCalc.fork(totalSteps);
				ramp(max, min, pc);
				pc.advance();
				reportProgress();
				if (aborted) return;
	
			}
	
			pc= pCalc.fork(totalSteps);
			waitAtLimit(min, pc);
			pc.advance();
			reportProgress();
			if (aborted) return;

			if (cyclingParameters.isStartingAsFinal()) {
				if (initialCurrent != min) {
					pc= pCalc.fork(totalSteps);
					ramp(min, initialCurrent, pc);
					pc.advance();
					reportProgress();
					if (aborted) return;
				}
				
			} else {
				if (cyclingParameters.getFinalValue() != min) {
					pc= pCalc.fork(totalSteps);
					ramp(min, cyclingParameters.getFinalValue(), pc);
					pc.advance();
					reportProgress();
					if (aborted) return;
				}
			}
			
	
		} catch (Exception e) {
			e.printStackTrace();
			reportError("Abnormal abort, error: "+e.toString());
			return;
		} 
		
		pCalc.advance();
		reportProgress();
		reportEnd("Cycling has ended.");
	}
	
	/**
	 * <p>Getter for the field <code>message</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * <p>isCycling.</p>
	 *
	 * @return a boolean
	 */
	public boolean isCycling() {
		return cycling;
	}
	
	/**
	 * <p>Getter for the field <code>progress</code>.</p>
	 *
	 * @return a double
	 */
	public double getProgress() {
		return progress;
	}
	
	private void reportProgress() {
		double old=progress;
		progress= pCalc.getProgress();
		if (listener!=null) {
			listener.propertyChange(new PropertyChangeEvent(this, PROGRESS, old, progress));
		}
	}
	
	/**
	 * Insert the method's description here.
	 * Creation date: (05.05.02 19:27:06)
	 *
	 * @param c double
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public void setCurrent(double c) throws RemoteException {
		set.setValue(c);
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (05.05.02 19:22:24)
	 *
	 * @param mili int
	 */
	public synchronized void sleeping(long mili) {
		if (mili > 0) {
			try {
				wait(mili);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * <p>isAborted.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAborted() {
		return aborted;
	}
	
	/**
	 * <p>isError.</p>
	 *
	 * @return a boolean
	 */
	public boolean isError() {
		return error;
	}
	
	/**
	 * This method was created in VisualAge.
	 * @throws RemoteException 
	 */
	private void waitAtLimit(double limit, ProgressCalculator pc) throws RemoteException {
	
		if (isAborted()) return;
		
		
		long time= System.currentTimeMillis();
	
		int steps= (int)(cyclingParameters.getWaitAtLimits()*10.0);
		ProgressCalculator pc1= pc.split(steps);
		
		while (Math.abs(get.getValue() - limit) > 0.0001
			&& System.currentTimeMillis() - time < (long)(cyclingParameters.getWaitAtLimits()*1000)) {
	
			try {
				set.setValue(limit);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (isAborted()) return;
	
			sleeping(100);
			pc1.step();
			reportProgress();
			if (isAborted()) return;
		}
	
		while (System.currentTimeMillis() - time < (long)(cyclingParameters.getWaitAtLimits()*1000)) {
			
			sleeping(100);
			pc1.step();
			reportProgress();
			
			if (isAborted()) return;
		}
		pc1.advance();
		pc.advance();
		reportProgress();
	}

	/**
	 * <p>abort.</p>
	 */
	public synchronized void abort() {
		aborted=true;
		reportAbort("Aborted by user!");
		notify();
	}
	
	/**
	 * <p>release.</p>
	 */
	public void release() {
		
		if (isCycling()) {
			abort();
		}
		
		//get.destroy();
		//set.destroy();
		
	}
}
