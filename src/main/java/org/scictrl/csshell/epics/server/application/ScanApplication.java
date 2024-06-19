/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.application.ScanApplication.ScanningTask.ScanController;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ScanApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class ScanApplication extends AbstractApplication {
	
	private static final String SETPOINT_PV = "SetpointPv";
	private static final String CMD_STOP = "Cmd:Stop";
	private static final String CMD_START = "Cmd:Start";
	private static final String CMD_USE_SET = "Cmd:UseSet";
	private static final String CMD_SWAP = "Cmd:Swap";
	private static final String STATUS_SCANNING = "Status:Scanning";
	private static final String STATUS_COUNTDOWN = "Status:CountDown";

	/** Constant <code>STATUS="Status"</code> */
	public static final String STATUS = "Status";
	private static final String STEP = "Step";
	
	private static final String RATE = "Rate";
	private static final String START = "Start";
	private static final String END = "End";
	
	/** Constant <code>SETPOINT="Setpoint"</code> */
	public static final String SETPOINT = "Setpoint";
	
	/** Constant <code>REPEAT="Repeat"</code> */
	public static final String REPEAT= "Repeat";
	
	/** Constant <code>COUNT="Count"</code> */
	public static final String COUNT= "Count";

	/**
	 * Repeat instruction.
	 */
	public enum Repeat {
		/**
		 * Single run, no repeat.
		 */
		SINGLE,
		/**
		 * Ramp scan up and down.
		 */
		RAMP,
		/**
		 * Ramp scan up from start to end, and then repeat from start.
		 */
		TOGGLE}

	/**
	 * Start point instruction.
	 */
	public static enum StartPoint {
		/**
		 * Use start point.
		 */
		START,
		/**
		 * USe end point.
		 */
		END,
		/**
		 * USe current value.
		 */
		CURRENT}

	/**
	 * Runnable performing scan.
	 */
	public static class ScanningTask implements Runnable {
		
		/**
		 * Interface that facilitates connection to undelaying services and context.
		 * @author igor@scictrl.com
		 *
		 */
		public static interface ScanController {
			/**
			 * Logger for general logging
			 * @return Logger for general logging
			 */
			Logger log();
			/**
			 * Set action, sets value to the setpoint
			 * @param d the value to be set
			 * @return returns <code>true</code> if set command succeeded.
			 */
			boolean setValue(double d);
			/**
			 * Checks if setpoint control point is ready for set action. 
			 * @return <code>true</code> if setpoint can be set
			 */
			boolean isSetReady();
			/**
			 * Reports error to underlying context
			 * @param message error message
			 * @param e Throwable causing error
			 */
			void error(String message, Throwable e);
			/**
			 * Notifies that scan is done
			 * @param task the task that is done
			 */
			void done(ScanningTask task);
		}
		
		private boolean aborted=false;
		private boolean cancled=false;
		double start;
		double end;
		long rate;
		double step;
		double time=-1;
		int count;
		Repeat repeat;
		double precision;
		private ScanController ctrl;
		private Long nextStepTime;
		private int overflowCount;

		/**
		 * Creates scan/ramp task
		 * @param start the start value
		 * @param end the end value
		 * @param step the step size
		 * @param rate the time to wait between steps in ms
		 * @param count the repetition count, 0 means one run without repetition
		 * @param repeat repetition specification
		 * @param precision value precision, determines how precise end value can be
		 * @param ctrl the scan control that provides underlying context services
		 * @param overflowCount hard limit that prevents runaway stepping
		 */
		public ScanningTask(double start, double end, double step, long rate, int count, Repeat repeat, double precision, ScanController ctrl, int overflowCount) {
			this.start = start;
			this.end = end;
			this.rate = rate;
			this.count= count;
			this.repeat=repeat;
			this.precision=precision;
			this.ctrl=ctrl;
			// hard limit that prevents runaway stepping
			this.overflowCount=overflowCount;
			
			if (ctrl==null) {
				throw new NullPointerException("Parameter 'ScanController ctrl' is null!");
			}
			
			if (start<=end) {
				this.step=Math.abs(step);
			} else {
				this.step=-Math.abs(step);
			}
			
			
		}
		
		/**
		 * Returns next time step should happen, could be null if there is no next step 
		 * @return next time step should happen
		 */
		public Long getNextStepTime() {
			return nextStepTime;
		}
		
		/**
		 * Abort scan. Scan progress is stopped. ScanController is notified that scan has been aborted.
		 */
		public synchronized void abort() {
			_abort();
			notify();
		}
		
		/**
		 * Cancels and aborts the scan. Scan progress is stopped, there is no notification to ScanController. 
		 */
		public synchronized void cancle() {
			cancled=true;
			_abort();
			notify();
		}
		
		public boolean isCancled() {
			return cancled;
		}

		/**
		 * Returns <code>true</code> if scan has been aborted.
		 * @return <code>true</code> if scan has been aborted
		 */
		public boolean isAborted() {
			return aborted;
		}
		
		@Override
		public synchronized void run() {
			
			ctrl.log().debug("Scanning initiated "+start+" "+end+" "+step+" "+rate+" "+count);
			
			if (Math.abs(step)<precision) {
				aborted=true;
				ctrl.log().debug("Aborted, step smaller thatn precision "+precision);
			}

			try {
				
				int c=0;
				while (
						ctrl.isSetReady() &&
						!aborted && 
						(count==0 || c<count)) {

					ctrl.log().debug("Scanning run "+c+" started.");

					ramp(start, end, step);

					if (repeat==Repeat.SINGLE) {
						aborted=true;
					}
					
					if (aborted) {
						continue;
					}
					
					if (repeat==Repeat.TOGGLE) {
						ramp(end, start, -step);
					}
					
					c++;
					
				}
				
			} catch (Exception e) {
				ctrl.error("Scanning failed!", e);
			}
			
			nextStepTime=null;
			aborted=true;
	
			if (cancled) {
				ctrl.log().debug("Cancled");
			} else {
				ctrl.log().debug("Aborted");
				ctrl.done(this);
			}

		}
		
		private void _abort() {
			nextStepTime=null;
			aborted=true;
		}
		
		/**
		 * Ramps remote value from start to end in steps.
		 * @param rStart start value
		 * @param rEnd end value
		 * @param rStep step size
		 */
		public void ramp(double rStart, double rEnd, double rStep) {
			
			ctrl.log().debug("Ramping "+rStart+" "+rEnd+" "+rStep+" "+rate);
			
			try {

				int count=0;
				
				ctrl.log().debug("Setting start value "+rStart);
				
				double next=rStart;
				boolean b= ctrl.setValue(next);
				if (!b) {
					_abort();
					return;
				}
				
				while (
						ctrl.isSetReady() &&
						!aborted && 
						count++<overflowCount) {
					
					if (Math.abs(rEnd-next)<precision) {
						ctrl.log().debug("value at the end "+next);
						nextStepTime=null;
						return;
					}

					if (rate>0) {
						nextStepTime= System.currentTimeMillis()+rate;
						synchronized (this) {
							this.wait(rate);
						}
					}

					if (aborted) {
						_abort();
						return;
					}

					if (Math.abs(rEnd-next)<Math.abs(rStep)) {
						next=rEnd;
					} else {
						next=next+rStep;
					}
					
					if (aborted) {
						_abort();
						return;
					}

					ctrl.log().debug("Setting next step "+next);

					b= ctrl.setValue(next);
					if (!b) {
						_abort();
						return;
					}

				}
				
			} catch (Exception e) {
				ctrl.error("Ramping failed!", e);
				nextStepTime=null;
				aborted=true;
			}
			
			nextStepTime=null;
		}
	}
	
	private ScanningTask task;
	private String pvSetpoint;
	private double precision;
	private boolean configurable;
	private boolean manualScan=false;
	private Double manualSet;
	private String pvSetpointCmd;
	private ValueLinks setCmd;
	private int scanCount=0;
	private Record set;
	
	/**
	 * <p>Constructor for ScanApplication.</p>
	 */
	public ScanApplication() {
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		precision= config.getDouble("precision",0.000001);
		pvSetpoint=config.getString("setpointPV");
		pvSetpointCmd=config.getString("setpointCmdPV");
		
		configurable= pvSetpoint==null;
		
		addRecordOfMemoryValueProcessor(SETPOINT_PV, "PV for setpoint", new byte[128]);

		set= addRecordOfOnLinkValueProcessor(SETPOINT, "Setpoint", DBRType.DOUBLE, pvSetpoint);
		
		addRecordOfMemoryValueProcessor(STATUS_COUNTDOWN, "Next step countdown",null,null,"s",(short)0, 0.0);
		addRecordOfMemoryValueProcessor(STEP, "Step size",null,null,null,(short)2, 0.0);
		addRecordOfMemoryValueProcessor(RATE, "Step rate in s",null,null,"s",(short)2, 0.0);
		addRecordOfMemoryValueProcessor(START, "Start value",null,null,null,(short)2, 0.0);
		addRecordOfMemoryValueProcessor(END, "End value",null,null,null,(short)2, 0.0);
		addRecordOfMemoryValueProcessor(COUNT, "Repetition count, unlimited if 0.",0,Integer.MAX_VALUE,"count",0);
		addRecordOfMemoryValueProcessor(REPEAT, "Repeat mode.",new String[]{Repeat.SINGLE.name(),Repeat.RAMP.name(),Repeat.TOGGLE.name()},(short)0);
		addRecordOfMemoryValueProcessor(STATUS_SCANNING, "Flag indicating scanning in progress", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS, "Scanning status", new String[]{"READY","SCANNING","ERROR"}, (short)0);
		addRecordOfMemoryValueProcessor(CMD_STOP, "Stops scanning task", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CMD_START, "Start scanning task", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CMD_USE_SET, "Use current setpoint as start value", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CMD_SWAP, "Swap start and end values", DBRType.BYTE, 0);

		if (getRecord(ERROR_SUM).getAlarmSeverity()==Severity.INVALID_ALARM) {
			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
		}
		if (getRecord(LINK_ERROR).getAlarmSeverity()==Severity.INVALID_ALARM) {
			updateLinkError(false, "");
		}
		
		getRecord(STEP).setPersistent(true);
		getRecord(RATE).setPersistent(true);
		getRecord(START).setPersistent(true);
		getRecord(END).setPersistent(true);
		getRecord(REPEAT).setPersistent(true);
		getRecord(COUNT).setPersistent(true);
		
		if (configurable) {
			getRecord(SETPOINT_PV).setPersistent(true);
		} else {
			getRecord(SETPOINT_PV).setValueAsString(pvSetpoint);
		}
		
		if (pvSetpointCmd !=null) {
			setCmd= connectLinks("SetpointCmd", pvSetpointCmd);
		}
		
	}
	
	private Repeat getRepeat() {
		int i= getRecord(REPEAT).getValueAsInt();
		if (i<0 || i>2) {
			i=0;
			getRecord(REPEAT).setValue(0);
		}
		Repeat repeat=Repeat.values()[i];	
		return repeat;
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name==CMD_START) {
			if (manualScan) {
				log.info("Scan request denied, interactive scanning enabled.");
				return;
			}
			if (task!=null) {
				log.debug("Scan request denied, scan in progress");
				return;
			}
			double start= getRecord(START).getValueAsDouble();
			double end= getRecord(END).getValueAsDouble();
			long rate= (long)(getRecord(RATE).getValueAsDouble()*1000.0);
			double step= getRecord(STEP).getValueAsDouble();
			int count= getRecord(COUNT).getValueAsInt();
			Repeat repeat=getRepeat();

			if (Math.abs(start-end)<0.001) {
				log.info("Scan request denied, change too small, to '"+end+"' from '"+start+"'.");
				return;
			}
			
			log.info("Scan request from '"+start+"' to '"+end+"'.");
			
			getRecord(STATUS).setValue(1);
			getRecord(STATUS_SCANNING).setValue(1);
			task= new ScanningTask(start,end,step,rate,count,repeat,precision,new ScanController() {
				@Override
				public boolean isSetReady() {
					return !((LinkedValueProcessor)set.getProcessor()).isInvalid() && set.getAlarmStatus()==Status.NO_ALARM;
				}
				@Override
				public Logger log() {
					return log;
				}
				@Override
				public boolean setValue(double d) {
					try {
						set.setValue(d);
						if (setCmd!=null) {
							setCmd.setValue(1);
						}
						return true;
					} catch (Exception e) {
						log4error("Set failed", e);
					}
					return false;
				}
				@Override
				public void error(String message, Throwable e) {
					log4error(message, e);
				}
				@Override
				public void done(ScanningTask t) {
					if (t==task) {
						getRecord(STATUS).setValue(0);
						getRecord(STATUS_SCANNING).setValue(0);
						task=null;
					}
				}
				
			},1000);
			database.schedule(task, 0);
			
		} else if (name==CMD_STOP) {
			if (manualScan) {
				log.info("Scan request denied, interactive scanning enabled.");
				return;
			}
			getRecord(STATUS).setValue(0);
			getRecord(STATUS_SCANNING).setValue(0);
			ScanningTask t=task;
			task=null;
			if (t!=null) {
				t.abort();
			}
		} else if (name==SETPOINT_PV) {
			if (configurable) {
				database.schedule(new Runnable() {
					
					@Override
					public void run() {
						((LinkedValueProcessor)set.getProcessor()).reconnect(getRecord(SETPOINT_PV).getValueAsString(), Record.PROPERTY_VALUE);
					}
				}, 100);
			}
		} else if (name==CMD_USE_SET) {
			double d= set.getValueAsDouble();
			getRecord(START).setValue(d);
		} else if (name==CMD_SWAP) {
			double start= getRecord(START).getValueAsDouble();
			double end= getRecord(END).getValueAsDouble();
			getRecord(START).setValue(end);
			getRecord(END).setValue(start);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (SETPOINT==name) {
			
			Record r= set;
			String u1= r.getUnits();
			String u2= getRecord(STEP).getUnits();
			
			if (u1!=null && u1!=u2) {
				getRecord(STEP).copyUnitsControlLimits(r);
				getRecord(START).copyUnitsControlLimits(r);
				getRecord(END).copyUnitsControlLimits(r);
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();

		if (configurable) {
			((LinkedValueProcessor)set.getProcessor()).reconnect(getRecord(SETPOINT_PV).getValueAsString(), Record.PROPERTY_VALUE);
		}
		
		database.schedule(new Runnable() {

			@Override
			public void run() {
				Long t= task!=null ? task.getNextStepTime() : null;
				if (t!=null) {
					getRecord(STATUS_COUNTDOWN).setValue((double)(Math.max(t-System.currentTimeMillis()-200,0L)/1000.0));
				} else {
					getRecord(STATUS_COUNTDOWN).setValue(0.0);
				}
			}
			
		}, 1000, 1000);

	}
	
	/**
	 * <p>isConfigurable.</p>
	 *
	 * @return a boolean
	 */
	public boolean isConfigurable() {
		return configurable;
	}
	
	/**
	 * <p>isManualScan.</p>
	 *
	 * @return a boolean
	 */
	public boolean isManualScan() {
		return manualScan;
	}
	
	/**
	 * <p>Setter for the field <code>manualScan</code>.</p>
	 *
	 * @param interactive a boolean
	 */
	public void setManualScan(boolean interactive) {
		this.manualScan = interactive;
		log.info("Interactive scanning set "+interactive);
	}

	/**
	 * <p>getStartValue.</p>
	 *
	 * @return a double
	 */
	public double getStartValue() {
		return getRecord(START).getValueAsDouble();
	}
	
	/**
	 * <p>getEndValue.</p>
	 *
	 * @return a double
	 */
	public double getEndValue() {
		return getRecord(END).getValueAsDouble();
	}

	/**
	 * <p>startManualScan.</p>
	 */
	public void startManualScan() {
		scanCount=0;
		double start= getRecord(START).getValueAsDouble();
		_startManualScan(start);
	}
	
	/**
	 * <p>startManualScan.</p>
	 *
	 * @param sp a {@link org.scictrl.csshell.epics.server.application.ScanApplication.StartPoint} object
	 */
	public void startManualScan(StartPoint sp) {
		scanCount=0;
		if (sp==StartPoint.START) {
			_startManualScan(getRecord(START).getValueAsDouble());
		} else if (sp==StartPoint.END) {
			_startManualScan(getRecord(END).getValueAsDouble());
		} else if (sp==StartPoint.CURRENT) {
			_startManualScan(set.getValueAsDouble());
		} 
	}

	private void _startManualScan(Double start) {

		log4info("Manual scan start "+start);

		getRecord(STATUS).setValue(1);
		getRecord(STATUS_SCANNING).setValue(1);
		
		manualSet=start;
		set.setValue(manualSet);

	}
	
	/**
	 * <p>getStepCount.</p>
	 *
	 * @return a int
	 */
	public int getStepCount() {
		if (Math.abs(getRecord(STEP).getValueAsDouble())<precision) {
			return 1;
		}
		int i= (int)Math.ceil(Math.abs((precision+getRecord(END).getValueAsDouble()-getRecord(START).getValueAsDouble())/getRecord(STEP).getValueAsDouble()));
		
		if (i<1) {
			return 1;
		}
		
		return i;
	}

	/**
	 * <p>stopManualScan.</p>
	 */
	public void stopManualScan() {
		manualSet=null;
		getRecord(STATUS).setValue(0);
		getRecord(STATUS_SCANNING).setValue(0);
		
	}
	
	/**
	 * <p>stepManualScan.</p>
	 *
	 * @return a boolean
	 */
	public boolean stepManualScan() {
		return stepManualScan(true);
	}
	
	/**
	 * <p>stepManualScanInv.</p>
	 *
	 * @return a boolean
	 */
	public boolean stepManualScanInv() {
		return stepManualScan(false);
	}
	
	/**
	 * <p>stepManualScan.</p>
	 *
	 * @param inc a boolean
	 * @return a boolean
	 */
	public boolean stepManualScan(boolean inc) {
		
		if (manualSet==null) {
			log4info("Step denied, manual scan not active, start manual scan first!");
			return false;
		}
		
		double end= getRecord(END).getValueAsDouble();
		double step= getRecord(STEP).getValueAsDouble();
		Repeat repeat= getRepeat();
		double manualEnd=0.0;

		if ( (scanCount>0 && repeat==Repeat.RAMP) || !inc ) {
			double start= getRecord(START).getValueAsDouble();
			manualEnd=start;
		} else {
			manualEnd=end;
		}
			
		if (((LinkedValueProcessor)set.getProcessor()).isInvalid() 
				|| set.getAlarmStatus()!=Status.NO_ALARM) {
			
			log4error("Aborting manual scan, errors on set PV");
			stopManualScan();
			return false;
			
		}
			
		if (Math.abs(step)<precision) {
			
			log4info("Ended, step '"+step+"' smaller than precision.");
			stopManualScan();
			return false;
			
		}

		if (Math.abs(manualEnd-manualSet)<precision) {
			log4info("Ended, value at the end '"+manualEnd+"'.");
			
			int count= getRecord(COUNT).getValueAsInt();
			
			scanCount++;
			
			if (scanCount<count && repeat==Repeat.TOGGLE) {
			
				log.debug("Scanning run "+scanCount+" started.");
				double start= getRecord(START).getValueAsDouble();
				_startManualScan(start);
				return true;
				
			} else if (scanCount<count && repeat==Repeat.RAMP) {
				
				log.debug("Scanning run "+scanCount+" started.");
				_startManualScan(end);
				return true;
				
			} else {
				
				stopManualScan();
				return false;
				
			}
		}

		if (Math.abs(manualEnd-manualSet)<Math.abs(step)) {
			manualSet=manualEnd;
		} else {
			if (manualSet<=manualEnd) {
				manualSet=manualSet+Math.abs(step);
			} else {
				manualSet=manualSet-Math.abs(step);
			}
		}
			
		log.debug("Setting next step "+manualSet);

		set.setValue(manualSet);
			
		/*if (Math.abs(end-manualSet)<precision) {
			log.debug("Stopped, value at the end "+manualSet+" "+end);
			stopManualScan();
			return false;
		}*/
		
		return true;
	}
	
	/**
	 * <p>isManualScanActive.</p>
	 *
	 * @return a boolean
	 */
	public boolean isManualScanActive() {
		return manualSet!=null;
	}
	
	/**
	 * <p>getSetpoint.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public Record getSetpoint() {
		return set;
	}
	
	/**
	 * <p>getSetpointValue.</p>
	 *
	 * @return a double
	 */
	public double getSetpointValue() {
		return set.getValueAsDouble();
	}
	/**
	 * <p>getRateValue.</p>
	 *
	 * @return a double
	 */
	public double getRateValue() {
		return getRecord(RATE).getValueAsDouble();
	}
	/**
	 * <p>isAtEnd.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAtEnd() {
		return Math.abs(set.getValueAsDouble()-getRecord(END).getValueAsDouble())<precision;
	}

	/**
	 * <p>isAtStart.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAtStart() {
		return Math.abs(set.getValueAsDouble()-getRecord(START).getValueAsDouble())<precision;
	}
}
