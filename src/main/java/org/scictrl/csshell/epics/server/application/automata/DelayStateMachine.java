/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * <p>DelayStateMachine class.</p>
 *
 * @author igor@scictrl.com
 */
public class DelayStateMachine extends StateMachine {

	private double delay;
	private Runnable updater;
	private double step=0;

	/**
	 * <p>Constructor for DelayStateMachine.</p>
	 */
	public DelayStateMachine() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		delay= config.getDouble("delay", 1.0);
		
		setState(State.INACTIVE);
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineActivate(final boolean dryrun) {
		super.stateMachineActivate(dryrun);
		
		setProgress(0.0);
		
		if (delay>0.0) {
			
			setState(State.BUSY);
			
			long l= (long)(delay*1000.0);
			
			database.schedule(new Runnable() {
				
				@Override
				public void run() {
					
					if (getState()==State.BUSY) {
						setProgress(100.0);
						setState(State.ACTIVE);
					}
					
				}
			}, l);
			
			if (l>1000) {
				step=100.0/delay;
				
				updater= new Runnable() {
					
					@Override
					public void run() {
						if (this==updater && getState()==State.BUSY && getProgress()<100.0) {
							setProgress(getProgress()+step);
							database.schedule(this, 1000);
						}
					}
				};
				
				database.schedule(updater, 1000);
				
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineAbort() {
		updater=null;
		super.stateMachineAbort();
		setState(State.INACTIVE);
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachinePrepare() {
		updater=null;
		super.stateMachinePrepare();
		setState(State.INACTIVE);
	}

}
