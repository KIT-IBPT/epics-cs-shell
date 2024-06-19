/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Application;
import org.scictrl.csshell.epics.server.Record;

/**
 * This State Machine delegated too sequence of state machines..
 *
 * @author igor@scictrl.com
 */
public class SequenceStateMachine extends StateMachine {

	class ActivateTask extends Thread {
		
		boolean active=true;
		
		public ActivateTask() {
			super("ActivateTask-"+System.currentTimeMillis());
		}
		
		public boolean isActive() {
			return active;
		}
		
		public void abort() {
			active=false;
		}
		
		public void run() {
			
			try {
				
				for (int i = 0; i < sequence.size(); i++) {
					
					if (!active) {
						return;
					}
					StateMachine sm= sequence.get(i);
					String n= sm.getName();
					int k= n.lastIndexOf(':');
					if (k>-1) {
						n=n.substring(k+1, n.length());
					}
					
					setStatus("Starting "+n+" ("+sm.getState().toString()+"): "+sm.getStatus());
					
					if (sm.isEnabled()) {

						sm.stateMachineActivate(isDryRun());
	
						while(sm.getState()==StateMachine.State.BUSY && active) {
							setStatus("Busy with '"+n+"' ("+sm.getState().toString()+"): "+sm.getStatus());
							try {
								this.wait(100);
							} catch (Exception e) {
							}
						}
						if (sm.isAbortOnFail() && sm.getState()!=StateMachine.State.ACTIVE) {
							abort();
						}
						if (active) {
							setStatus("Done "+n+" ("+sm.getState().toString()+"): "+sm.getStatus());
						}
						
					}
				}
				
				if (SequenceStateMachine.this.getState()==StateMachine.State.BUSY && active) {
					setState(StateMachine.State.ACTIVE);
					updateProgress();
				}

			} catch (Exception e) {
				log4error("Activation failed!", e);
				SequenceStateMachine.this.setState(StateMachine.State.FAILED);
			} finally {
				active=false;
			}
			
		};
		
	}
	
	/**
	 * <p>newApplication.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param timeout a double
	 * @param names an array of {@link java.lang.String} objects
	 * @return a {@link org.scictrl.csshell.epics.server.application.automata.SequenceStateMachine} object
	 */
	public static final SequenceStateMachine newApplication(String name, double timeout, String[] names) {
		
		HierarchicalConfiguration conf= new HierarchicalConfiguration();
		conf.setProperty("timeout", timeout);
		
		StringBuilder sb= new StringBuilder(names.length*64);
		if (names.length>0) {
			sb.append(names[0]);
		}
		for (String s : names) {
			sb.append(",");
			sb.append(s);
		}
		conf.setProperty("sequence", sb.toString());
		
		SequenceStateMachine app= new SequenceStateMachine();
		app.configure(name, conf);
		
		return app;
	}

	
	private String[] sequenceNames;
	private List<StateMachine> sequence;
	private ActivateTask task;

	/**
	 * <p>Constructor for SequenceStateMachine.</p>
	 */
	public SequenceStateMachine() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		sequenceNames= config.getStringArray("sequence");
		
		if (sequenceNames==null || sequenceNames.length==0) {
			throw new IllegalArgumentException("No sequence defined");
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		sequence= new ArrayList<StateMachine>(sequenceNames.length);
		
		for (int i = 0; i < sequenceNames.length; i++) {
			
			Record r= database.getRecord(sequenceNames[i].trim()+getNameDelimiter()+STATE);
			if (r==null) {
				log.fatal("No record with name: '"+sequenceNames[i].trim()+getNameDelimiter()+STATE+"'.");
				setStateInitializationFailed();
				return;
			}
			Application a= r.getApplication();
			
			if (a==null) {
				log.fatal("No application for record with name: '"+sequenceNames[i].trim()+getNameDelimiter()+STATE+"'.");
				setStateInitializationFailed();
				return;
			}
			if (!StateMachine.class.isAssignableFrom(a.getClass())) {
				log.fatal("Wrong application "+a.getClass()+" for record with name: '"+sequenceNames[i].trim()+getNameDelimiter()+STATE+"'.");
				setStateInitializationFailed();
				return;
			}
			
			sequence.add((StateMachine)a);
			/*
			 * r.addPropertyChangeListener(new PropertyChangeListener() {
			 * 
			 * @Override public void propertyChange(PropertyChangeEvent evt) {
			 * updateState(); } });
			 */

			r= database.getRecord(sequenceNames[i].trim()+getNameDelimiter()+PROGRESS);
			r.addPropertyChangeListener(new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					updateProgress();
				}
			});
		}
		
	}
	
	/**
	 * <p>updateProgress.</p>
	 */
	public void updateProgress() {
		
		double p = 0.0;
		int c=0;
		
		for (StateMachine sm : sequence) {
			if (sm.isEnabled()) {
				p+=sm.getProgress();
				c++;
			}
		}
		
		p = p/(double)c;
		
		setProgress(p);
	}

	/**
	 * <p>updateState.</p>
	 */
	public void updateState() {
		
		if (getState()==State.FAILED) {
			return;
		}
		
		boolean busy=false;
		boolean failed=false;
		boolean on=true;
		
		for (StateMachine sm : sequence) {
			if (sm.isEnabled()) {
				State st= sm.getState();
				failed= failed||st==State.FAILED;
				busy= busy||st==State.BUSY;
				on= on&&st==State.ACTIVE;
			}
		}
		
		if (failed) {
			setState(State.FAILED);
		} else if (busy) {
			setState(State.BUSY);
		} else if (on) {
			setState(State.ACTIVE);
		} else {
			setState(State.INACTIVE);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void stateMachinePrepare() {
		super.stateMachinePrepare();
		
		long t=60000;
		for (StateMachine sm : sequence) {
			if (sm.isEnabled()) {
				sm.stateMachinePrepare();
				t=t+sm.getTimeout();
			}
		}
		setTimeout(t);
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineActivate(final boolean dryrun) {
		super.activate();
		
		SequenceStateMachine.this.setState(StateMachine.State.BUSY);

		stateMachinePrepare();
		
		if (task==null || !task.isActive()) {
			task= new ActivateTask();
			task.start();
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void stateMachineAbort() {
		super.stateMachineAbort();
		
		ActivateTask t= task;
		
		if (t!=null) {
			t.abort();
		}
		
		try {
			for (int i = 0; i < sequence.size(); i++) {
				StateMachine sm= sequence.get(i);
				if (sm.isEnabled()) {
					sm.stateMachineAbort();
				}
			}
		} catch (Exception e) {
			log4error("Abort failed!", e);
			SequenceStateMachine.this.setState(StateMachine.State.FAILED);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void setState(State st) {
		State old= getState();
		super.setState(st);
		
		if (old==State.ACTIVE && st==State.INACTIVE) {
			stateMachinePrepare();
		}
	}
}
