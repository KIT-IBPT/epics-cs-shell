/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ValueLinks;

/**
 * A StateMachine, which remembers last not Shutdown operation state and sets it when activated.
 *
 * @author igor@scictrl.com
 */
public class OperationModeStateMachine extends StateMachine {

	private static final String CONF_MODE_PV = "link";

	private static final String PV_MODE_TARGET = "ModeTarget";

	private ValueLinks mode;
	
	/**
	 * <p>Constructor for ValueStateMachine.</p>
	 */
	public OperationModeStateMachine() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		String modePV = config.getString(CONF_MODE_PV);
		
		if (modePV==null) {
			setStateInitializationFailed();
			throw new IllegalArgumentException("The 'modePV' configuration paramter is missing!");
		}
		
		mode= connectLinks(CONF_MODE_PV, modePV);
		
		addRecordOfMemoryValueProcessor(PV_MODE_TARGET, "", new String[] {"Shutdown","Laser Operation","RF Operation","Beam Operation"}, (short)0);
		
		setState(State.INACTIVE);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		if (isInitializationFailed()) {
			return;
		}
		if (name==CONF_MODE_PV) {
			if (mode.isReady() && !mode.isInvalid() &&  !mode.isLastSeverityInvalid()) {
				long m= mode.consumeAsLong();
				
				if (m>0L) {
					getRecord(PV_MODE_TARGET).setValue((short)m);
				}
			}
			
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

		} else {
			
			setState(State.BUSY);
			try {
				
				short value= (short)getRecord(PV_MODE_TARGET).getValueAsInt();
				
				if (!mode.isInvalid() && mode.isReady()) {
					mode.setValue(value);
					log4info(" set to "+value);
				} else {
					log4info("Operation mode set skipped");
				}

				setProgress(100.0);
				setState(State.ACTIVE);
			} catch (Exception e) {
				log4error("Set failed!", e);
				setState(State.FAILED);
			}
			
		}
	}
	
}
