/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * <p>DummyStateMachine class used for testing and simulation.</p>
 *
 * @author igor@scictrl.com
 */
public class DummyStateMachine extends StateMachine {

	/**
	 * <p>newApplication.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param timeout a double
	 * @return a {@link org.scictrl.csshell.epics.server.application.automata.DummyStateMachine} object
	 */
	public static final DummyStateMachine newApplication(String name, double timeout) {
		
		HierarchicalConfiguration conf= new HierarchicalConfiguration();
		conf.setProperty("timeout", timeout);
		
		DummyStateMachine app= new DummyStateMachine();
		app.configure(name, conf);
		
		return app;
	}

	/**
	 * DummyAction specifies state machine dummy state.
	 */
	public enum DummyAction {
		/**
		 * Does nothing.
		 */
		NOTHING,
		/**
		 * Is busy.
		 */
		BUSY,
		/** 
		 * Is active.
		 */
		ACTIVE,
		/**
		 * Has failed.
		 */
		FAIL};
	
	private DummyAction dummyAction= DummyAction.ACTIVE;
	
	/**
	 * <p>Constructor for DummyStateMachine.</p>
	 */
	public DummyStateMachine() {
	}
	
	/**
	 * <p>Setter for the field <code>dummyAction</code>.</p>
	 *
	 * @param dummyAction a {@link org.scictrl.csshell.epics.server.application.automata.DummyStateMachine.DummyAction} object
	 */
	public void setDummyAction(DummyAction dummyAction) {
		this.dummyAction = dummyAction;
	}

	
	/** {@inheritDoc} */
	@Override
	public void stateMachineActivate(final boolean dryrun) {
		
		switch (dummyAction) {
		case BUSY:
			setState(State.BUSY);
			setProgress(50.0);
			break;
		case ACTIVE:
			setState(State.ACTIVE);
			setProgress(100.0);
			break;
		case FAIL:
			setState(State.FAILED);
			setProgress(10.0);
			break;

		default:
			break;
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineAbort() {
		super.stateMachineAbort();
		setState(State.INACTIVE);
	}
	
	@Override
	public void stateMachinePrepare() {
		super.stateMachinePrepare();
		setState(State.INACTIVE);
	}
}
