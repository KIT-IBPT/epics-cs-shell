/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.processor.Checks.Check;
import org.scictrl.csshell.epics.server.processor.Checks.Condition;

/**
 * A StateMachine, which is Activate, when certain value is reacched, or bz monitoring it or by setting it.
 *
 * @author igor@scictrl.com
 */
public class ValueStateMachine extends StateMachine {

	private static final String CONDITION_SET = "conditionSet";
	private static final String LINK = "link";
	private static final String VALUE = "value";
	private static final String VALUE_PRECISION = "valuePrecision";
	private static final String VALUE_STEP = "valueStep";
	private static final String STEP_TIME = "stepTime";
	private static final String ACTION = "action";
	private static final String CONDITION_ACTIVE = "conditionActive";

	enum Action {
		SET,SET_ONEWAY,RAMP,MONITOR;
		
		public static final Action fromString(String s) {
			if (s==null) {
				return null;
			}
			if (s.toUpperCase().contentEquals(SET.name())) {
				return SET;
			}
			if (s.toUpperCase().contentEquals(SET_ONEWAY.name())) {
				return SET_ONEWAY;
			}
			if (s.toUpperCase().contentEquals(RAMP.name())) {
				return RAMP;
			}
			if (s.toUpperCase().contentEquals(MONITOR.name())) {
				return MONITOR;
			}
			return null;
		}
	}
	
	class RampThread extends Thread {
		private double step;
		private ValueLinks vl;
		private long timer;
		private boolean aborted=false;
		public RampThread(ValueLinks vl,double value,double step, long timer) {
			super("RampThread");
			this.step=step;
			this.vl=vl;
			this.timer=timer;
		}
		@Override
		public void run() {
			
			try {

				int count=0;
				Double start=null;
				while (!aborted && !vl.isInvalid() && vl.isReady() && count++<10000) {
					
					double val= vl.consumeAsDoubles()[0];
					if (start==null) {
						start=val;
					}
					
					if (Math.abs(val-value)<valuePrecision) {
						aborted=true;
						setProgress(100.0);
						continue;
					}
					
					double st= value-val;
					if (Math.abs(st)>step) {
						st= Math.signum(st)*step;
					}
					double next= val+st;
					if (Math.abs(st)<step) {
						next=value;
					}
					
					log4debug(linkName+" ramp step to "+next);
					vl.setValue(next);
					
					if (start!=null) {
						double p= Math.abs(next-start) / Math.abs(value-start);
						setProgress(p*100.0);
					}
					
					synchronized (this) {
						this.wait(timer);
					}
					
				}
				
			} catch (Exception e) {
				log4error("Ramping failed!", e);
				ValueStateMachine.this.setState(StateMachine.State.FAILED);
			}
			
			aborted=true;
			
			checkValue();
		}
		
		public void abort() {
			aborted=true;
		}
		
		public boolean isAborted() {
			return aborted;
		}
		
	}
	
	private String linkName;
	private Double value;
	private Action action;
	private ValueLinks link;
	private double valuePrecision;
	private double valueStep;
	private long stepTime;
	private RampThread rampThread;
	private Check conditionSet;
	private Check conditionActive;
	

	/**
	 * <p>Constructor for ValueStateMachine.</p>
	 */
	public ValueStateMachine() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		linkName = config.getString(LINK);
		value = config.getDouble(VALUE);
		valuePrecision = config.getDouble(VALUE_PRECISION,0.00001);
		action = Action.valueOf(config.getString(ACTION));
		
		if (linkName==null) {
			setStateInitializationFailed();
			throw new IllegalArgumentException("The 'link' configuration paramter is missing!");
		}
		if (value==null) {
			setStateInitializationFailed();
			throw new IllegalArgumentException("The 'value' configuration paramter is missing or not a double!");
		}
		if (action==null) {
			setStateInitializationFailed();
			throw new IllegalArgumentException("The 'action' configuration paramter is missing or not SET or MONITOR!");
		}
		
		if (action==Action.RAMP) {
			
			valueStep = config.getDouble(VALUE_STEP,1.0);
			double d= config.getDouble(STEP_TIME,1.0);
			stepTime= (long)(d*1000.0);
			
		}

		link= connectLinks(LINK, linkName);
		
		List<HierarchicalConfiguration> cl= config.configurationsAt(CONDITION_SET);
		if (cl!=null && cl.size()==1) {
			conditionSet= Check.fromConfiguration(cl.get(0), value, valuePrecision);
		} else {
			conditionSet= Check.ALWAYS;
		}
		
		cl= config.configurationsAt(CONDITION_ACTIVE);
		if (cl!=null && cl.size()==1) {
			conditionActive= Check.fromConfiguration(cl.get(0), value, valuePrecision);
		} else {
			conditionActive= new Check(Condition.EQUAL, value, valuePrecision);
		}
		
		setState(State.INACTIVE);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		if (isInitializationFailed()) {
			return;
		}
		if (name==LINK) {
			checkValue();
		}
	}
	
	private void checkValue() {
		
		if (action==Action.SET_ONEWAY) {
			//setState(State.INACTIVE);
			return;
		}

		if (!link.isInvalid() && link.isReady()) {
			
			double[] d= link.consumeAsDoubles();
			
			if (d.length==1) {
				if (conditionActive.check(d[0])) {
					if (getState()==State.BUSY) {
						setProgress(100.0);
					}
					setState(State.ACTIVE);
					return;
				}
			}
		}
		if (getState()!=State.BUSY && getState()!=State.FAILED) {
			setState(State.INACTIVE);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineActivate(final boolean dryrun) {
		super.stateMachineActivate(dryrun);
		
		setProgress(0.0);
		
		boolean dr= isDryRun() || dryrun;
		
		if(dr) {
			
			setState(State.BUSY);
			try {
				synchronized (this) {
					wait(1000);
				}
			} catch (Exception e) {
				log4error("Wait failed: "+e.toString(), e);
			}
			setProgress(100.0);
			setState(State.ACTIVE);

		} else if (action==Action.SET || action==Action.SET_ONEWAY) {
			
			setState(State.BUSY);
			try {
				
				if (action==Action.SET_ONEWAY || conditionSet.isAlways()) {
					link.setValue(value);
					log4info(linkName+" set to "+value);
				} else {
					if (!link.isInvalid() && link.isReady()) {
						double[] d= link.consumeAsDoubles();
						if (d.length==1) {
							if (conditionSet.check(d[0])) {
								link.setValue(value);
								log4info(linkName+" set to "+value);
							} else {
								log4info(linkName+" set skipped");
							}
						}
					} else {
						log4error(linkName+" set condition could not be checked");
						setState(State.FAILED);
					}
				}

				setProgress(100.0);
				if (action==Action.SET_ONEWAY) {
					setState(State.ACTIVE);
				} else {
					checkValue();
				}
			} catch (Exception e) {
				log4error("Set failed!", e);
				setState(State.FAILED);
			}
			
		} else if (action==Action.RAMP) {
			
			if (getState()!=State.ACTIVE && !isRampingAlive()) {
				setState(State.BUSY);
				log4info(linkName+" ramping started");
				rampThread=new RampThread(link, value, valueStep, stepTime);
				rampThread.start();
			} else if (getState()==State.ACTIVE) {
				setProgress(100.0);
			}
			
		} else if (action==Action.MONITOR) {
			
			setState(State.BUSY);
			checkValue();
			
		}
	}
	
	private boolean isRampingAlive() {
		if (rampThread==null) {
			return false;
		}
		return !(rampThread.isAborted() || !rampThread.isAlive());  
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachinePrepare() {
		super.stateMachinePrepare();
		
		if (action==Action.SET_ONEWAY) {
			setState(State.INACTIVE);
		}

	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineAbort() {
		super.stateMachineAbort();
		
		RampThread rt= rampThread;
		if (rt!=null) {
			rt.abort();
		}
		setState(State.INACTIVE);
		checkValue();
	}

	/**
	 * <p>Getter for the field <code>conditionActive</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.processor.Checks.Check} object
	 */
	public Check getConditionActive() {
		return conditionActive;
	}
	
	/**
	 * <p>Getter for the field <code>conditionSet</code>.</p>
	 *
	 * @return a {@link java.lang.Object} object
	 */
	public Object getConditionSet() {
		return conditionSet;
	}
}
