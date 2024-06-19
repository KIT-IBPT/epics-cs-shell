package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>FeedbackLoopApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class FeedbackLoopApplication extends AbstractApplication {

	/** Constant <code>ENABLED="enabled"</code> */
	public static final String ENABLED = "enabled";
	/** Constant <code>ERROR_SUM="ErrorSum"</code> */
	public static final String ERROR_SUM = "ErrorSum";
	/** Constant <code>OUTPUT_POINTS="outputPoints"</code> */
	public static final String OUTPUT_POINTS = "outputPoints";
	/** Constant <code>BREAK_POINTS="breakPoints"</code> */
	public static final String BREAK_POINTS = "breakPoints";
	/** Constant <code>OUTPUT="output"</code> */
	public static final String OUTPUT = "output";
	/** Constant <code>OUTPUT_SET="outputSet"</code> */
	public static final String OUTPUT_SET = "outputSet";
	/** Constant <code>INPUT="input"</code> */
	public static final String INPUT = "input";
	private static final String IN_SYNC = "inSync";
	private double[] breakPoints;
	private double[] outputPoints;
	private String inputPV;
	private String outputPV;
	private int syncFails;
	private double outputPrecision;
	private double inputPrecision;
	private int reactionWindow;
	private long lastSet;
	private boolean syncing;
	private boolean setting;
	private boolean differential;
	private Double lastInput;
	private Double lastOutputPoint;

	/**
	 * <p>Constructor for FeedbackLoopApplication.</p>
	 */
	public FeedbackLoopApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		inputPV= config.getString("inputPV");
		outputPV= config.getString("outputPV");
		
		if (inputPV==null || inputPV.length()==0) {
			log4error("Configuration has no inputPV parameter!");
		}
		if (outputPV==null || outputPV.length()==0) {
			log4error("Configuration has no outputPV parameter!");
		}
		
		outputPrecision= config.getDouble("outputPrecision", 0.0001);
		inputPrecision= config.getDouble("inputPrecision", 0.001);
		reactionWindow= config.getInt("reactionWindow", 1000);
		
		differential=config.getBoolean("differential", false);
		
		lastInput=null;
		lastOutputPoint=null;
		
		String[] breakPointsStr= config.getStringArray(BREAK_POINTS);
		String[] currentPointsStr= config.getStringArray(OUTPUT_POINTS);

		if (breakPointsStr.length+1!=currentPointsStr.length) {
			log4error("App does not have 1 curret point more then number of break points!");
		}
		
		String[] s = getStore().getStringArray(BREAK_POINTS);
		if (s!=null && s.length>0) {
			breakPointsStr=s;
		}
		s = getStore().getStringArray(OUTPUT_POINTS);
		if (s!=null && s.length>0) {
			currentPointsStr=s;
		}
		
		int points = Math.min(breakPointsStr.length, currentPointsStr.length-1);
		
		breakPoints= new double[points];
		outputPoints= new double[points+1];

		for (int i = 0; i < points; i++) {
			breakPoints[i]= Double.parseDouble(breakPointsStr[i]);
			outputPoints[i]= Double.parseDouble(currentPointsStr[i]);
		}
		outputPoints[points]= Double.parseDouble(currentPointsStr[points]);

		boolean enabled = config.getBoolean(ENABLED, false);

		addRecord(INPUT, LinkedValueProcessor.newProcessor(fullRecordName("Input"), DBRType.DOUBLE, "Inpit value for the feedback.", inputPV).getRecord());
		addRecord(OUTPUT, LinkedValueProcessor.newProcessor(fullRecordName("Output"), DBRType.DOUBLE, "Current output value.", outputPV).getRecord());
		addRecord(OUTPUT_SET, MemoryValueProcessor.newDoubleProcessor(fullRecordName("OutputSet"), "Calculated output value according to feedback input.", 0.0,false).getRecord());
		addRecord(BREAK_POINTS, MemoryValueProcessor.newProcessor(fullRecordName("BreakPoints"), DBRType.DOUBLE, 4, "Input values at which output value changes.", breakPoints,false, false).getRecord());
		addRecord(OUTPUT_POINTS, MemoryValueProcessor.newProcessor(fullRecordName("OutputPoints"), DBRType.DOUBLE, 5, "Output values between input break points.", outputPoints,false, false).getRecord());
		addRecord("OutputPV", MemoryValueProcessor.newProcessor(fullRecordName("OutputPV"), DBRType.STRING, 1, "Output PV name.", outputPV,true, false).getRecord());
		addRecord(ENABLED, MemoryValueProcessor.newBooleanProcessor(fullRecordName("Enabled"), "Feedback loop is enabled and active.", enabled,false, false).getRecord());
		addRecord(IN_SYNC, MemoryValueProcessor.newBooleanProcessor(fullRecordName("InSync"), "Feedback loop current value is in sycn with actual value.", false,false,true).getRecord());
		
		
		
		getRecord(OUTPUT_SET).updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		if (name==INPUT) {
			if (getRecord(INPUT).isAlarmUndefined()) {
				return;
			}
			//log.debug("Input '"+inputPV+"' change "+getRecord(INPUT).getValueAsDouble());
			setWithInput(false);
		} else if (name==OUTPUT_POINTS) {
			outputPoints=getRecord(OUTPUT_POINTS).getValueAsDoubleArray();
			saveOutputPoints();
			setWithInput(true);
		} else if (name==BREAK_POINTS) {
			breakPoints=getRecord(BREAK_POINTS).getValueAsDoubleArray();
			saveBreakPoints();
			setWithInput(true);
		} else if (name==ENABLED) {
			boolean enabled = getRecord(ENABLED).getValueAsBoolean();
			log4debug("Enabled change "+enabled);
			if (enabled) {
				setWithInput(true);
			}
		} else if (name==OUTPUT && !setting) {
			//log.debug("Output '"+outputPV+"' change "+getRecord(OUTPUT).getValueAsDouble());
			checkInSync();
			trySync();
		} else if (name==OUTPUT_SET) {
			//checkInSync();
		} else if (name==IN_SYNC) {
			//log.debug("InSync change "+getRecord(IN_SYNC).getValueAsBoolean());
		} 
	}

	private void checkInSync() {
		
		Thread.yield();
		
		if (differential) {
			double input= getRecord(INPUT).getValueAsDouble();
			boolean sync= lastInput!=null && Math.abs(lastInput-input)<inputPrecision;
			getRecord(IN_SYNC).setValue(sync);
		} else {
			Record out= getRecord(OUTPUT);
			double outGet= out.getValueAsDouble(); 
			double outSet= getRecord(OUTPUT_SET).getValueAsDouble(); 
			
			boolean sync= Math.abs(outGet-outSet)<outputPrecision;
			
			long time=System.currentTimeMillis();

			if (sync) {
				syncFails=0;
				getRecord(IN_SYNC).setValue(true);
			} else {
				if (reactionWindow==0 || lastSet+reactionWindow<time) {
					if (!sync) {
						syncFails++;
					} else {
						syncFails=0;
					}
					getRecord(IN_SYNC).setValue(syncFails<1);
				} else {
					long delay= lastSet+reactionWindow-time+10;
					if (delay<10) {
						delay=10;
					}
					database.schedule(new Runnable() {
						
						@Override
						public void run() {
							checkInSync();
						}
					}, delay);
				}
			}
		}		
	}
	
	private void trySync() {
		if (getRecord(IN_SYNC).isAlarmUndefined()) {
			checkInSync();
		}
		if (differential) {
			return;
		}
		Record out= getRecord(OUTPUT);
		double outGet= out.getValueAsDouble(); 
		double outSet= getRecord(OUTPUT_SET).getValueAsDouble(); 
		
		boolean sync= Math.abs(outGet-outSet)<outputPrecision;

		if (!sync) {
			if (!syncing && getRecord(ENABLED).getValueAsBoolean()) {
				syncing=true;
				setWithInput(true);
				syncing=false;
				checkInSync();
			}			
		}
	}

	private void saveBreakPoints() {
		StringBuilder sb= new StringBuilder(128);
		sb.append(breakPoints[0]);
		for (int i = 1; i < breakPoints.length; i++) {
			sb.append(',');
			sb.append(breakPoints[i]);
		}

		getStore().setProperty(name+"."+BREAK_POINTS, sb.toString());
	}

	private void saveOutputPoints() {
		StringBuilder sb= new StringBuilder(128);
		sb.append(outputPoints[0]);
		for (int i = 1; i < outputPoints.length; i++) {
			sb.append(',');
			sb.append(outputPoints[i]);
		}

		getStore().setProperty(name+"."+OUTPUT_POINTS, sb.toString());
	}
	
	/**
	 * Converts input value into output_set.
	 * If feedback is enabled, then output_set is sent to the remote output.  
	 */
	private void setWithInput(boolean force) {
		double input= getRecord(INPUT).getValueAsDouble();
		setWithInput(input,force);
	}

	/**
	 * Converts input value into output_set.
	 * If feedback is enabled, then output_set is sent to the remote output.  
	 * @param input the input value
	 */
	private void setWithInput(double input, boolean force) {
		double out,point=Double.NaN;
		
		int g=0;
		
		while (g<breakPoints.length) {
			if (input<=breakPoints[g]) {
				point=outputPoints[g];
				break;
			}
			g++;
		}
		
		if (g==outputPoints.length-1) {
			point=outputPoints[outputPoints.length-1];
		}
		
		if (differential) {
			if (lastInput!=null) {
				if (Math.abs(lastInput-input)<inputPrecision) {
					return;
				}
			}
			if (lastOutputPoint==null) {
				lastOutputPoint=point;
			}
			double outGet= getRecord(OUTPUT).getValueAsDouble();
			out=outGet+point-lastOutputPoint;
		} else {
			out=point;
		}
		
		if (force 
				|| getRecord(OUTPUT_SET).isAlarmUndefined() 
				|| Math.abs(getRecord(OUTPUT_SET).getValueAsDouble()-out)>0.0001
				/*|| (differential && lastInput!=null && Math.abs(lastInput-input)>precision)*/) {
			
			
			
			getRecord(OUTPUT_SET).setValue(new double[]{out});
		
			lastOutputPoint=point;
			lastInput=input;
			
			if (getRecord(ENABLED).getValueAsBoolean()) {
				log4debug("Setting out '"+out+"' out_point '"+point+"' out_no '"+g+"' to '"+outputPV+"'.");
				setOutputValue(out);
			} else {
				log4debug("Not set out '"+out+"' out_point '"+point+"' out_no '"+g+"' to '"+outputPV+"'.");
			}
			
			checkInSync();
		} 
		
	}

	/**
	 * Sets output parameter to output PV.
	 * @param output the output parameter to be set 
	 */
	private void setOutputValue(double output) {
		setting=true;
		try {
			((LinkedValueProcessor)getRecord(OUTPUT).getProcessor()).setValue(new double[]{output});
			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
			lastSet=System.currentTimeMillis();
			log4debug("Setting out '"+output+"' to '"+outputPV+"'.");
		} catch (Exception e) {
			log4error("Remote setting of '"+outputPV+"' failed!", e);
			updateErrorSum(Severity.MAJOR_ALARM, Status.LINK_ALARM);
			return;
		} finally {
			setting=false;
		}
	}

}
