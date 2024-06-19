package org.scictrl.csshell.epics.server.application;

import java.util.ArrayList;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>FeedbackLoopMicrotron class.</p>
 *
 * @author igor@scictrl.com
 */
public class FeedbackLoopMicrotron extends AbstractApplication {

	/** Constant <code>ARMED="armed"</code> */
	public static final String ARMED = "armed";
	/** Constant <code>ENABLED="enabled"</code> */
	public static final String ENABLED = "enabled";
	/** Constant <code>ERROR_SUM="ErrorSum"</code> */
	public static final String ERROR_SUM = "ErrorSum";
	/** Constant <code>OUTPUT="output"</code> */
	public static final String OUTPUT = "output";
	/** Constant <code>OUTPUT_SET="outputSet"</code> */
	public static final String OUTPUT_SET = "outputSet";
	/** Constant <code>INPUT="input"</code> */
	public static final String INPUT = "input";
	/** Constant <code>INC_SET="increment"</code> */
	public static final String INC_SET = "increment";
	/** Constant <code>ACTIVE="feedbackActive"</code> */
	public static final String ACTIVE = "feedbackActive";
	/** Constant <code>MIN_INP="minInp"</code> */
	public static final String MIN_INP = "minInp";
	/** Constant <code>MAX_INP="maxInp"</code> */
	public static final String MAX_INP = "maxInp";
	/** Constant <code>SAMPLES="samples"</code> */
	public static final String SAMPLES = "samples";
	/** Constant <code>USE_MAX="maxenb"</code> */
	public static final String USE_MAX = "maxenb";
	/** Constant <code>USE_MIN="minenb"</code> */
	public static final String USE_MIN = "minenb";
	/** Constant <code>USER_INP="userInput"</code> */
	public static final String USER_INP = "userInput";
	/** Constant <code>USE_USER="enableUser"</code> */
	public static final String USE_USER = "enableUser";
	/** Constant <code>USE_MAXOUT="outmaxenb"</code> */
	public static final String USE_MAXOUT = "outmaxenb";
	/** Constant <code>USE_MINOUT="outminenb"</code> */
	public static final String USE_MINOUT = "outminenb";
	/** Constant <code>MIN_OUT="minOut"</code> */
	public static final String MIN_OUT = "minOut";
	/** Constant <code>MAX_OUT="maxOut"</code> */
	public static final String MAX_OUT = "maxOut";
	private String inputPV;
	private String outputPV;
	@SuppressWarnings("unused")
	private boolean setting;
	private double minInput;
	private double maxInput;
	private double increment;
	private long controlDelay;
	private double lastMeasurement = -1;
	private double sampleCount = 1;
	private ArrayList<Double> samples = new ArrayList<Double>();

	private AdjustmentThread adjustmentThread;

	private enum SM {
		INITIAL, IDLE, INCREASE_POWER, DECREASE_POWER, FEEDBACK_FAILURE
	};

	SM state = SM.INITIAL;
	private double RFout;

	/**
	 * <p>Constructor for FeedbackLoopMicrotron.</p>
	 */
	public FeedbackLoopMicrotron() {
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		inputPV = config.getString("inputPV");
		outputPV = config.getString("outputPV");
		controlDelay = config.getInt("controlDelay", 300);

		increment = config.getDouble("increment");

		adjustmentThread = new AdjustmentThread();
		adjustmentThread.start();

		if (inputPV == null || inputPV.length() == 0) {
			log4error("Configuration has no inputPV parameter!");
		}
		if (outputPV == null || outputPV.length() == 0) {
			log4error("Configuration has no outputPV parameter!");
		}

		minInput = config.getDouble("minInput", 0);
		maxInput = config.getDouble("maxInput", 10000);

		boolean enabled = config.getBoolean(ENABLED, false);

		// creates records of variables to be stored in IOC shell
		addRecordOfMemoryValueProcessor(ARMED, "If true feedback is ready to be executed.",
				DBRType.BYTE, false);
		addRecordOfMemoryValueProcessor(ACTIVE, "If true feedback is being processed.",
				DBRType.BYTE, false);
		addRecordOfMemoryValueProcessor(USE_MAX, "Trigger on upper limit.", DBRType.BYTE, true);
		addRecordOfMemoryValueProcessor(USE_MIN, "Trigger on lower limit.", DBRType.BYTE, true);
		addRecordOfMemoryValueProcessor(USE_USER, "Use user input value.", DBRType.BYTE, false);
		addRecordOfMemoryValueProcessor(USE_MINOUT, "Stop at maximum out.", DBRType.BYTE, false);
		addRecordOfMemoryValueProcessor(USE_MAXOUT, "Stop at minimum out.", DBRType.BYTE, false);
		addRecordOfMemoryValueProcessor(MIN_INP, "The minimum input value/trigger threshold.",
				DBRType.DOUBLE, minInput);
		addRecordOfMemoryValueProcessor(MAX_INP, "The maximum input value/trigger threshold.",
				DBRType.DOUBLE, maxInput);
		addRecordOfMemoryValueProcessor(INC_SET, "The amount that the power will be adjusted.",
				DBRType.DOUBLE, increment);
		addRecordOfMemoryValueProcessor(USER_INP, "Input taken from CSS screen.", DBRType.DOUBLE, 0);
		addRecordOfMemoryValueProcessor(SAMPLES,
				"The number of sameples to take before adjusting.", DBRType.DOUBLE, sampleCount);
		addRecord(
				INPUT,
				LinkedValueProcessor.newProcessor(fullRecordName("Input"),
						DBRType.DOUBLE, "Input value for the feedback.", inputPV).getRecord());
		addRecord(
				OUTPUT,
				LinkedValueProcessor.newProcessor(fullRecordName("Output"),
						DBRType.DOUBLE, "Current output value.", outputPV).getRecord());
		addRecord(
				OUTPUT_SET,
				MemoryValueProcessor.newDoubleProcessor(fullRecordName("OutputSet"),
						"Calculated output value according to feedback input.", 0.0, false)
						.getRecord());
		addRecord(
				"OutputPV",
				MemoryValueProcessor.newProcessor(fullRecordName("OutputPV"),
						DBRType.STRING, 1, "Output PV name.", outputPV, true, false).getRecord());
		addRecord(
				ENABLED,
				MemoryValueProcessor.newBooleanProcessor(fullRecordName("Enabled"),
						"Feedback loop is enabled and active.", enabled, false, false).getRecord());

		getRecord(OUTPUT_SET).updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		sampleCount = getRecord(SAMPLES).getValueAsDouble();
		// getRecord(STATE).setValue(state);
		if (name == INPUT) {
			if (getRecord(INPUT).isAlarmUndefined() || getRecord(USE_USER).getValueAsBoolean()) {
				return;
			}
			// log.debug("Input '"+inputPV+"' change "+getRecord(INPUT).getValueAsDouble());
			notifyInput();
		} else if (name == USER_INP) {
			if (getRecord(USER_INP).isAlarmUndefined() || !getRecord(USE_USER).getValueAsBoolean()) {
				return;
			}
			// log.debug("Input '"+inputPV+"' change "+getRecord(INPUT).getValueAsDouble());
			notifyUInput();
		} else if (name == ARMED) {
			// reset state machine if armed is toggled
			if (!getRecord(ARMED).getValueAsBoolean()) {
				lastMeasurement = -1;
				getRecord(ACTIVE).setValue(false);
				if (state != SM.INITIAL) {
					state = SM.IDLE;
				}
			}
		} else if (name == USE_USER) {
			samples.clear();
		} else if (name == OUTPUT) {
			//System.out.println("notify " + RFout + ", " + getRecord(OUTPUT).getValueAsDouble());
			double o = getRecord(OUTPUT).getValueAsDouble();
			double os = getRecord(OUTPUT_SET).getValueAsDouble();
			if (o - os > 0.000001) {
				this.state = SM.IDLE;
				RFout = o;
			}
		}
	}

	private void notifyInput() {
		if (!getRecord(ARMED).getValueAsBoolean() || sampleCount < 1
				|| getRecord(USE_USER).getValueAsBoolean()) {
			return;
		}
		increment = getRecord(INC_SET).getValueAsDouble();
		minInput = getRecord(MIN_INP).getValueAsDouble();
		maxInput = getRecord(MAX_INP).getValueAsDouble();

		// sample averaging
		samples.add(getRecord(INPUT).getValueAsDouble());
		if (samples.size() >= sampleCount) {
			double average = 0;
			for (int i = 0; i < sampleCount; i++) {
				average += samples.get(i);
			}
			average /= sampleCount;
			adjustmentThread.setMeasurement(average);
			adjustmentThread.trigger();
		} else if (sampleCount == 1) {
			adjustmentThread.setMeasurement(samples.get(0));
			adjustmentThread.trigger();
		}

	}

	private void notifyUInput() {
		if (!getRecord(ARMED).getValueAsBoolean() || sampleCount < 1
				|| !getRecord(USE_USER).getValueAsBoolean()) {
			return;
		}

		// sample averaging
		samples.add(getRecord(USER_INP).getValueAsDouble());
		if (sampleCount == 1) {
			triggerFeedbackLoop(samples.get(0));
		} else if (samples.size() >= sampleCount) {
			double average = 0;
			for (int i = 0; i < sampleCount; i++) {
				average += samples.get(i);
			}
			average /= sampleCount;
			triggerFeedbackLoop(average);
		}

	}

	/*
	 * Feedback State Machine This is a simple state machine: it is active when
	 * ARMED is set, waits until some condition, then attempts two power
	 * corrections in order to resolve this condition. This function should NOT
	 * contain blocking code
	 */
	private void triggerFeedbackLoop(double input) {

		increment = getRecord(INC_SET).getValueAsDouble();
		minInput = getRecord(MIN_INP).getValueAsDouble();
		maxInput = getRecord(MAX_INP).getValueAsDouble();

		// log.debug("State: "+this.state);
		boolean decreasing = measured_improvement(input);
		lastMeasurement = input;
		double output = getRecord(OUTPUT).getValueAsDouble();

		//System.out.println("statmc " + RFout + ", " + output);

		switch (this.state) {

		// initializes state machine variables
		case INITIAL:
			this.state = SM.IDLE;

			break;

		// waits until receiving an alarm
		case IDLE:
			// condition to initiate feedback adjustments
			RFout = output;
			getRecord(ACTIVE).setValue(false);
			if (decreasing) { // condition improves but still not in threshold
				if ((output + increment < maxInput) && (output + increment > minInput)) {
					getRecord(ACTIVE).setValue(true);
					this.state = SM.INCREASE_POWER;
					setOutputValue(RFout + increment);
				} else if((output - increment > minInput) && (output - increment < maxInput)) {
					getRecord(ACTIVE).setValue(true);
					this.state = SM.DECREASE_POWER;
					setOutputValue(RFout - increment);
				}
			}
			break;

		// tries increasing the power
		case INCREASE_POWER:

			if (decreasing && (output - increment > minInput) && (output - increment < maxInput)) {
				this.state = SM.DECREASE_POWER;
				setOutputValue(RFout - increment);
			} else { 
				if (output + increment > maxInput) {
					getRecord(ACTIVE).setValue(false);
					this.state = SM.IDLE;
				} else {
					this.state = SM.INCREASE_POWER;
					RFout = output;
					setOutputValue(RFout + increment);
				}
			}

			// condition has been resolved
			/*
			 * else { getRecord(ACTIVE).setValue(false); this.state = SM.IDLE; }
			 */
			break;

		// tries decreasing the power
		case DECREASE_POWER:
			if (decreasing || output - increment < minInput) {
				getRecord(ACTIVE).setValue(false);
				this.state = SM.IDLE;
			} else {
				this.state = SM.DECREASE_POWER;
				RFout = output;
				setOutputValue(RFout - increment);
			}
			
			/*
			 * else { getRecord(ACTIVE).setValue(false); this.state = SM.IDLE; }
			 */
			break;

		// feedback has failed
		case FEEDBACK_FAILURE:
			// if machine is rearmed, return to IDLE state
			if (getRecord(ARMED).getValueAsBoolean()) {
				this.state = SM.IDLE;
			}
			break;
		}
		samples.clear();
	}

	/*private boolean limit_trigger(double input) {
		boolean upper = getRecord(USE_MAX).getValueAsBoolean();
		boolean lower = getRecord(USE_MIN).getValueAsBoolean();
		if (upper && lower)
			return (input < minInput || input > maxInput);
		// if(upper && !lower) return (input > maxInput);
		// if(!upper && lower) return (input < minInput);
		if (!upper && !lower)
			return false;
		return false;
	}*/

	private boolean measured_improvement(double current) {
		return (current < lastMeasurement);
	}

	/**
	 * Sets output parameter to output PV.
	 * 
	 * @param output
	 *            the output parameter to be set
	 */
	private void setOutputValue(double output) {
		setting = true;
		try {
			getRecord(OUTPUT_SET).setValue(new double[] { output });
			((LinkedValueProcessor) getRecord(OUTPUT).getProcessor())
					.setValue(new double[] { output });
			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
			// lastSet=System.currentTimeMillis();
			log4debug("Setting out '" + output + "' to '" + outputPV + "'.");
		} catch (Exception e) {
			log4error("Remote setting of '" + outputPV + "' failed!", e);
			updateErrorSum(Severity.MAJOR_ALARM, Status.LINK_ALARM);
			return;
		} finally {
			setting = false;
		}
	}

	private class AdjustmentThread extends Thread {
		private double measurement;
		boolean alive = true;
		long lastTrigger = 0L;

		@Override
		public void run() {
			try {
				boolean waiting = false;
				while (alive) {

					synchronized (this) {
						try {
							this.wait(controlDelay);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					long t = System.currentTimeMillis();
					if (!getRecord(ARMED).getValueAsBoolean() || samples.size() <= sampleCount) {
						if (!waiting) {
							log4debug("Going to wait after: " + (t - lastTrigger));
						}
						waiting = true;
						synchronized (this) {
							try {
								this.wait(1000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						waiting = false;
						triggerFeedbackLoop(measurement);
					}
				}
			} catch (Exception e) {
				log4error("Stepping failed!", e);
			}
		}

		/*
		 * public synchronized void shutdown() { alive=false; this.notifyAll();
		 * }
		 */

		public void setMeasurement(double inval) {
			this.measurement = inval;
		}

		public synchronized void trigger() {
			lastTrigger = System.currentTimeMillis();
			this.notifyAll();
		}

		/*public long getLastTrigger() {
			return lastTrigger;
		}*/
	}

}
