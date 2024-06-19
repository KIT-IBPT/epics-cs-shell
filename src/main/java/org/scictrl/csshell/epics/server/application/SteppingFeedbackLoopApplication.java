package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.Tools;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>SteppingFeedbackLoopApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class SteppingFeedbackLoopApplication extends AbstractApplication {

	private static final String CMD_SET_TARGET = "Cmd:SetTarget";
	private static final String OUTPUT_PV = "OutputPV";
	/** Constant <code>ENABLED="Enabled"</code> */
	public static final String ENABLED = "Enabled";
	/** Constant <code>ERROR_SUM="ErrorSum"</code> */
	public static final String ERROR_SUM = "ErrorSum";
	/** Constant <code>OUTPUT="Output"</code> */
	public static final String OUTPUT = "Output";
	/** Constant <code>OUTPUT_SET="OutputSet"</code> */
	public static final String OUTPUT_SET = "OutputSet";
	/** Constant <code>INPUT="Input"</code> */
	public static final String INPUT = "Input";
	/** Constant <code>INPUT_TARGET="InputTarget"</code> */
	public static final String INPUT_TARGET = "InputTarget";
	/** Constant <code>TRIGGER="Trigger"</code> */
	public static final String TRIGGER = "Trigger";
	/** Constant <code>TRIGGER_WINDOW="TriggerWindow"</code> */
	public static final String TRIGGER_WINDOW = "TriggerWindow";

	private class SteppingThread extends Thread {
		boolean alive=true;
		long lastTrigger=0L;
		@Override
		public void run() {
			try {
				boolean waiting=false;
				while(alive) {
					
					
					synchronized (this) {
						try {
							this.wait(controlDelay);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					
					long t= System.currentTimeMillis();
					if (!enabled || t-lastTrigger>triggerWindow) {
						if (!waiting) {
							log4debug("Going to wait after: "+(t-lastTrigger));
						}
						waiting=true;
						synchronized (this) {
							try {
								this.wait(1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						waiting=false;
						step();
					}
				}
			} catch (Exception e) {
				log4error("Stepping failed!", e);
			}
		}
		
		/*public synchronized void shutdown() {
			alive=false;
			this.notifyAll();
		}*/
		
		public synchronized void trigger() {
			lastTrigger= System.currentTimeMillis();
			this.notifyAll();
		}
		
		public long getLastTrigger() {
			return lastTrigger;
		}
	}
	
	
	private String inputBPM;
	private String outputPV;
	private double outputPrecision;
	private double inputPrecision;
	private int triggerWindow;
	private String triggerPV;
	private double minStep;
	private double maxStep;
	private double minStepInputDelta;
	private double maxStepInputDelta;
	private double triggerPrecision;
	private SteppingThread steppingThread;
	private double outputMin;
	private double outputMax;
	private boolean enabled;
	private double lastTrigger;
	private double lastInput;
	private TimeStamp lastTS;
	private long controlDelay;

	/**
	 * <p>Constructor for SteppingFeedbackLoopApplication.</p>
	 */
	public SteppingFeedbackLoopApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		inputBPM= config.getString("inputPV");
		outputPV= config.getString("outputPV");
		triggerPV= config.getString("triggerPV");
		
		if (inputBPM==null || inputBPM.length()==0) {
			log4error("Configuration has no inputPV parameter!");
		}
		if (outputPV==null || outputPV.length()==0) {
			log4error("Configuration has no outputPV parameter!");
		}
		if (triggerPV==null || triggerPV.length()==0) {
			log4error("Configuration has no outputPV parameter!");
		}
		
		outputMin= config.getDouble("outputMin", 0.0);
		outputMax= config.getDouble("outputMax", 2.0);
		outputPrecision= config.getDouble("outputPrecision", 0.01);
		inputPrecision= config.getDouble("inputPrecision", 0.01);
		triggerWindow= config.getInt("triggerWindow", 1000);
		triggerPrecision= config.getDouble("triggerPrecision", 0.1);
		controlDelay= config.getInt("controlDelay", 300);
		
		minStep= config.getDouble("minStep", 0.001);
		maxStep= config.getDouble("maxStep", 0.005);
		minStepInputDelta= config.getDouble("minStepInputDelta", 0.01);
		maxStepInputDelta= config.getDouble("maxStepInputDelta", 0.1);
		
		
		boolean enabled = config.getBoolean(ENABLED, false);

		addRecord(TRIGGER, LinkedValueProcessor.newProcessor(fullRecordName(TRIGGER), DBRType.DOUBLE, "Inpit value for the feedback.", triggerPV).getRecord());
		addRecord(INPUT, LinkedValueProcessor.newProcessor(fullRecordName(INPUT), DBRType.DOUBLE, "Inpit value for the feedback.", inputBPM).getRecord());
		addRecord(OUTPUT, LinkedValueProcessor.newProcessor(fullRecordName(OUTPUT), DBRType.DOUBLE, "Current output value.", outputPV).getRecord());
		addRecordOfCommandProcessor(CMD_SET_TARGET, "Stores current Input as Target", 1000);
		addRecordOfMemoryValueProcessor(OUTPUT_SET, "Calculated output value according to feedback input.", 0.0,2.0,"A",(short)3,0.0);
		addRecordOfMemoryValueProcessor(INPUT_TARGET, "Target value for input", -10.0,10.0,"mm",(short)3,0.0);
		addRecordOfMemoryValueProcessor(OUTPUT_PV, "Output PV name.", DBRType.STRING, outputPV);
		addRecord(ENABLED, MemoryValueProcessor.newBooleanProcessor(fullRecordName(ENABLED), "Feedback loop is enabled and active.", enabled,false, false).getRecord());
		
		getRecord(OUTPUT_SET).updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
		
		if (getRecord(TRIGGER).getValueAsDouble()<triggerPrecision) {
			getRecord(INPUT_TARGET).setValue(getRecord(INPUT).getValueAsDouble());
			log4debug("New input target value from trigger: "+getRecord(INPUT_TARGET).getValueAsDouble());
		}

		if (enabled && getRecord(TRIGGER).getValueAsDouble()>=triggerPrecision) {
			getSteppingTread().trigger();
		}
	}

	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		if (name==TRIGGER) {
			double d= getRecord(TRIGGER).getValueAsDouble();
			if (Math.abs(d-lastTrigger)>triggerPrecision) {
				if (d<triggerPrecision*3) {
					getRecord(INPUT_TARGET).setValue(getRecord(INPUT).getValueAsDouble());
					log4debug("New input target value from trigger: "+getRecord(INPUT_TARGET).getValueAsDouble());
				}
				lastTrigger=d;
				getSteppingTread().trigger();
			}
		} else if (name==INPUT) {
			if (enabled) {
				double d= getRecord(INPUT).getValueAsDouble();
				if (Math.abs(d-lastInput)>inputPrecision) {
					if (System.currentTimeMillis()-getSteppingTread().getLastTrigger()>triggerWindow*2) {
						getRecord(INPUT_TARGET).setValue(d);
						log4debug("New input target value from input: "+getRecord(INPUT_TARGET).getValueAsDouble());
					}
					lastInput=d;
				}
			}
		} else if (name==ENABLED) {
			enabled = getRecord(ENABLED).getValueAsBoolean();
			log4debug("Enabled change "+enabled);
			getRecord(INPUT_TARGET).setValue(getRecord(INPUT).getValueAsDouble());
			log4debug("New input target value from enable: "+getRecord(INPUT_TARGET).getValueAsDouble());
			if (enabled && getRecord(TRIGGER).getValueAsDouble()>triggerPrecision) {
				getSteppingTread().trigger();
			}
		} else if (name==CMD_SET_TARGET) {
			getRecord(INPUT_TARGET).setValue(getRecord(INPUT).getValueAsDouble());
		}
	}

	private SteppingThread getSteppingTread() {
		if (steppingThread == null) {
			steppingThread = new SteppingThread();
			steppingThread.start();
		}

		return steppingThread;
	}

	/**
	 * Sets output parameter to output PV.
	 * @param output the output parameter to be set 
	 */
	private void setOutputValue(double output) {
		//setting=true;
		try {
			((LinkedValueProcessor)getRecord(OUTPUT).getProcessor()).setValue(new double[]{output});
			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
			//lastSet=System.currentTimeMillis();
			//log.debug("Setting out '"+output+"' to '"+outputPV+"'.");
		} catch (Exception e) {
			log4error("Remote setting of '"+outputPV+"' failed!", e);
			updateErrorSum(Severity.MAJOR_ALARM, Status.LINK_ALARM);
			return;
		} finally {
			//setting=false;
		}
	}
	
	private void step() {
		TimeStamp ts= getRecord(INPUT).getTimestamp();
		
		if (lastTS!=null && lastTS.EQ(ts)) {
			log4debug("Skip, timestamp same");
			return;
		}
			
		lastTS= ts;
		
		double input= getRecord(INPUT).getValueAsDouble();
		double output= getRecord(OUTPUT).getValueAsDouble();
		double inputTarget= getRecord(INPUT_TARGET).getValueAsDouble();
		
		double inputDelta= input-inputTarget;
		
		double inputSingn= Math.signum(inputDelta);
		double inputStep = Math.abs(inputDelta);

		if (inputStep<inputPrecision) {
			//log.debug("Skip, delta: "+Tools.format4D(inputDelta)+" smaller than: "+Tools.format4D(inputPrecision));
			return;
		}
		double step=0.0;
		
		if (inputStep<=minStepInputDelta) {
			step= inputSingn * minStep;
		} else if (inputStep>minStepInputDelta && inputStep<maxStepInputDelta) {
			step= inputSingn * (minStep + (inputStep-minStepInputDelta)/(maxStepInputDelta-minStepInputDelta)*(maxStep-minStep));
		} else if (inputStep>=maxStepInputDelta) {
			step= inputSingn * maxStep;
		}
		
		if (Math.abs(step)<outputPrecision) {
			//log.debug("Skip, step: "+Tools.format4D(step)+" smaller than: "+Tools.format4D(outputPrecision));
			return;
		}

		double outputSet= output+step;
		
		if (outputSet<outputMin) {
			outputSet=outputMin;
		} else if (outputSet>outputMax) {
			outputSet=outputMax;
		}
		
		log4debug("Set: "+Tools.format3D(outputSet)+" step: "+Tools.format3D(step)+" inpD: "+Tools.format3D(inputDelta)+" inp: "+Tools.format3D(input));
		
		getRecord(OUTPUT_SET).setValue(new double[]{outputSet});
		setOutputValue(outputSet);
			
	}
	
	

}
