/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Basic unit of automation control. State machine is either in ON state either in OFF state either
 * BUSY while going from OFF to ON. Or can be in error.
 * It has two functions:
 * prepare: issued to all relevant machines in combined statement before going to be activated
 * activate: set ON.
 * It goes to off only if conditions changes by some outside process.
 * So for going off, some other State machine should go to ON.
 * Also has a link to the next State machine that sould be activated after this one is done.
 *
 * @author igor@scictrl.com
 */
public abstract class StateMachine extends AbstractApplication {

	/** Constant for PV with description of state machine, <code>DESCRIPTION="Description"</code>. */
	public static final String DESCRIPTION="Description";
	/** Constant for PV with progress in precentage, <code>PROGRESS="Progress"</code>. */
	public static final String PROGRESS="Progress";
	/** Constant for PV with enable <code>ENABLED="Enabled"</code> */
	public static final String ENABLED="Enabled";
	/** Constant for PV with enable <code>ENABLED="Enabled"</code> */
	public static final String DRYRUN="DryRun";

	/** Constant <code>STATE="State"</code> */
	public static final String STATE="State";
	/** Constant <code>STATE_STRING="State:String"</code> */
	public static final String STATE_STRING="State:String";

	/** Constant <code>ABORT="Cmd:Abort"</code> */
	public static final String ABORT="Cmd:Abort";
	/** Constant <code>ACTIVATE="Cmd:Activate"</code> */
	public static final String ACTIVATE="Cmd:Activate";
	/** Constant <code>PREPARE="Cmd:Prepare"</code> */
	public static final String PREPARE="Cmd:Prepare";
	
	/**
	 * Operational state of this state machine.
	 */
	public enum State {
		/**
		 * Inactive.
		 */
		INACTIVE,
		/**
		 * It is active.
		 */
		ACTIVE,
		/**
		 * Operation in progress.
		 */
		BUSY,
		/**
		 * Operation failed.
		 */
		FAILED;

		/**
		 * Names array.
		 */
		public static final String[] names= new String[] {INACTIVE.toString(),ACTIVE.toString(),BUSY.toString(),FAILED.toString()};
		
		/**
		 * Returns names.
		 * @return names
		 */
		public static final String[] names() {
			return names; 
		}
		
		/**
		 * Converts index to a State.
		 * @param i index
		 * @return state
		 */
		public static final State fromInt(int i) {
			if (i<0 || i>values().length-1) {
				return State.FAILED;
			}
			return values()[i];
		}
	};
	
	private class TimeoutWatchdog implements Runnable {
		
		private long t=0;
		private State lastState=null;
		
		public void check(State st) {
			if (st==State.BUSY && lastState!=State.BUSY) {
				t=System.currentTimeMillis();
			}
			lastState=st;
		}
		
		public boolean isTimeout() {
			return lastState==State.BUSY && (System.currentTimeMillis()-t)>timeout;
		}
		
		public void run() {
			if (isTimeout() && isEnabled()) {
				internalAbort();
				setState(State.FAILED);
				setStatus("Timeout error!");
				log4error("Timeout after "+(System.currentTimeMillis()-t)+", limit "+timeout);
			}
		}
	}
	
	private String description;
	private String status;
	private boolean initializationFailed;
	private long timeout;
	private TimeoutWatchdog timeoutWatchdog;
	private String activeString;
	private String busyString;
	private boolean abortOnFail;
	private boolean enabledFixed=false;
	private boolean enabled=true;


	/**
	 * <p>Constructor for StateMachine.</p>
	 */
	public StateMachine() {
	}

	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		description= config.getString("description","");
		activeString= config.getString("activeString",description);
		busyString= config.getString("busyString",description);
		abortOnFail= config.getBoolean("abortOnFail",false);
		enabledFixed= config.getBoolean("enabledFixed",false);
		enabled= config.getBoolean("enabled",true);
		boolean dryRun= config.getBoolean("dryrun",false);
		
		double t= config.getDouble("timeout", 60.0);
		if (t<=0.0) {
			t=60.0;
		}
		
		timeout= (long)t*1000;
		
		addRecordOfMemoryValueProcessor(DESCRIPTION, "Description", DBRType.STRING, description);
		addRecordOfMemoryValueProcessor(STATE, "State Machine state", State.names, (short)0);
		addRecordOfMemoryValueProcessor(STATE_STRING, "State Machine state string", 256, DBRType.BYTE);
		addRecordOfMemoryValueProcessor(PROGRESS, "Activation progress", 0.0, 100.0, "%", (short)1, 0.0);
		
		org.scictrl.csshell.epics.server.Record r= addRecordOfMemoryValueProcessor(ENABLED, "Step is enabled", DBRType.BYTE, enabled);
		if (enabledFixed) {
			((MemoryValueProcessor)r.getProcessor()).setFixed(true);
		} else {
			r.setPersistent(true);
		}
		addRecordOfMemoryValueProcessor(DRYRUN, "Step is in dry-run mode", DBRType.BYTE, dryRun).setPersistent(true);
		
		addRecordOfCommandProcessor(PREPARE, "Prepares or resets the state machine before activation", 1000L);
		addRecordOfCommandProcessor(ACTIVATE, "Activates the state machine", 1000L);
		addRecordOfCommandProcessor(ABORT, "Aborts the state machine", 1000L);
		
		timeoutWatchdog=new TimeoutWatchdog();
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		database.schedule(timeoutWatchdog, timeout, timeout);
	}

	/**
	 * <p>Setter for the field <code>status</code>.</p>
	 *
	 * @param status a {@link java.lang.String} object
	 */
	public void setStatus(String status) {
		this.status = status;
		getRecord(STATE_STRING).setValue(status);
	}
	
	/**
	 * <p>Getter for the field <code>status</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * <p>setState.</p>
	 *
	 * @param st a {@link org.scictrl.csshell.epics.server.application.automata.StateMachine.State} object
	 */
	public void setState(State st) {
		if (initializationFailed) {
			return;
		}
		getRecord(STATE).setValue(st.ordinal());

		if (st==State.BUSY && busyString!=null && busyString.length()>0) {
			setStatus(busyString);
		}
		if (st==State.ACTIVE && activeString!=null && activeString.length()>0) {
			setStatus(activeString);
		}
		if (st==State.INACTIVE) {
			setStatus(description);
		}
	}
	
	/**
	 * <p>getState.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.automata.StateMachine.State} object
	 */
	public State getState() {
		return State.fromInt(getRecord(STATE).getValueAsInt());
	}

	/**
	 * <p>isInitializationFailed.</p>
	 *
	 * @return a boolean
	 */
	public boolean isInitializationFailed() {
		return initializationFailed;
	}
	
	/**
	 * This is to be used by extending class, once this is sate, the State Machine is not usable.
	 * Indicates serious configuration problem, which can not be remedied by itself.
	 */
	public void setStateInitializationFailed() {
		initializationFailed=true;
		getRecord(STATE).setValue(State.FAILED);
		setStatus("Initialization failed!");
		updateErrorSum(Severity.INVALID_ALARM, Status.UDF_ALARM);
	}
	
	/**
	 * <p>stateMachinePrepare.</p>
	 */
	public void stateMachinePrepare() {
		if (!isActivated()) {
			throw new IllegalStateException("Application '"+getName()+"' hans not been activated");
		}
		setProgress(0.0);
		// to be implemented by extending class
	}

	/**
	 * <p>Activates state machine. If this state machine is in dry-run mode or dry-run parameter with <code>true</code> is called, then this step is activate in dry-run mode.</p>
	 *
	 * @param dryrun dry-run mode request
	 */
	public void stateMachineActivate(final boolean dryrun) {
		if (!isActivated()) {
			throw new IllegalStateException("Application '"+getName()+"' hans not been activated");
		}
		// to be implemented by extending class
	}
	
	/**
	 * <p>stateMachineAbort.</p>
	 */
	public void stateMachineAbort() {
		if (!isActivated()) {
			throw new IllegalStateException("Application '"+getName()+"' hans not been activated");
		}
		// to be implemented by extending class
	}
	
	private void internalPrepare() {
		
		if (!isEnabled()) {
			return;
		}

		try {
			stateMachinePrepare();
		} catch (Exception e) {
			log4error("Activate failed!", e);
		}
		
	}

	private void internalActivate() {
		
		if (!isEnabled()) {
			return;
		}

		try {
			stateMachineActivate(isDryRun());
		} catch (Exception e) {
			log4error("Activate failed!", e);
		}
		
	}

	private void internalAbort() {
		
		if (!isEnabled()) {
			return;
		}
		
		try {
			stateMachineAbort();
		} catch (Exception e) {
			log4error("Abort failed!", e);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);

		try {
			if (name==PREPARE) {
				internalPrepare();
			} else if (name==ACTIVATE) {
				internalActivate();
			} else if (name==ABORT) {
				stateMachineAbort();
		}
		} catch (Exception e) {
			log4error("Operation '"+name+"' failed!",e);
		}			
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (name==STATE) {
			timeoutWatchdog.check(getState());
		} 
	}
	
	/**
	 * <p>setProgress.</p>
	 *
	 * @param d a double
	 */
	public void setProgress(double d) {
		if (d>100.0) {
			d=100.0;
		}
		getRecord(PROGRESS).setValue(d);
	}
	
	/**
	 * <p>getProgress.</p>
	 *
	 * @return a double
	 */
	public double getProgress() {
		return getRecord(PROGRESS).getValueAsDouble();
	}


	/**
	 * <p>Getter for the field <code>timeout</code>.</p>
	 *
	 * @return a long
	 */
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * <p>Setter for the field <code>timeout</code>.</p>
	 *
	 * @param timeout a long
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * If <code>true</code> sequence should abort if this step fails.
	 *
	 * @return if <code>true</code> sequence should abort if this step fails
	 */
	public boolean isAbortOnFail() {
		return abortOnFail;
	}
	
	/**
	 * If not enabled, sequence executor will skip this step.
	 *
	 * @return is enabled
	 */
	public boolean isEnabled() {
		
		if (enabledFixed) {
			return enabled;
		}
		
		boolean e= getRecord(ENABLED).getValueAsBoolean();
		
		return e;
	}
	
	/**
	 * Sets enabled flag, if it is enabledFixed on, then fixed value is set, otherwise enable PV.
	 *
	 * @param b new enable flag
	 */
	public void setEnabled(boolean b) {
		if (enabledFixed) {
			enabled =b;
		}
		getRecord(ENABLED).setValue(b);
	}
	
	/**
	 * If this step is in dru-run mode, this means that when activated pretends that actions has been successfully completed without actually doing something.
	 *
	 * @return if this step is in dru-run mode
	 */
	public boolean isDryRun() {
		boolean b = getRecord(DRYRUN).getValueAsBoolean();
		return b;
	}
	
}
