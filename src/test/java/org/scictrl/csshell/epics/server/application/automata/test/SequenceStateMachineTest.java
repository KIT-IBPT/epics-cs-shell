/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.server.application.automata.DummyStateMachine;
import org.scictrl.csshell.epics.server.application.automata.DummyStateMachine.DummyAction;
import org.scictrl.csshell.epics.server.application.automata.SequenceStateMachine;
import org.scictrl.csshell.epics.server.application.automata.StateMachine;
import org.scictrl.csshell.epics.server.application.automata.StateMachine.State;
import org.scictrl.csshell.server.test.AbstractSimpleServerTest;

import gov.aps.jca.dbr.DBR;

/**
 * <p>SequenceStateMachineTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class SequenceStateMachineTest extends AbstractSimpleServerTest {
	
	String appName="A:TEST:DummySeq:01";
	String pvState=":State";
	String pvProgress=":Progress";
	String[] seqNames= {appName+":Dummy1",appName+":Dummy2",appName+":Dummy3"};
	private DummyStateMachine[] seq;
	private SequenceStateMachine ssm;
	
	/**
	 * <p>assertStateMachine.</p>
	 *
	 * @param progress a double
	 * @param state a {@link org.scictrl.csshell.epics.server.application.automata.StateMachine.State} object
	 * @param sm a {@link org.scictrl.csshell.epics.server.application.automata.StateMachine} object
	 */
	@SuppressWarnings("unchecked")
	public void assertStateMachine(double progress, State state, StateMachine sm) {
		
		try {
			
			assertEquals(progress, sm.getProgress(), 0.000001);
			assertEquals(state, sm.getState());
			
			assertEquals(progress, ((Poop<Double,DBR>)connector.getOneShot(sm.getName()+pvProgress)).getValue(), 0.000001);
			assertEquals(Long.valueOf(state.ordinal()), ((Poop<Long,DBR>)connector.getOneShot(sm.getName()+pvState)).getValue());
			
		} catch (Exception e) {
			fail(e.toString());
		}
		
	}
	
	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		seq= new DummyStateMachine[3];
		
		seq[0]= DummyStateMachine.newApplication(seqNames[0], 1.0);
		seq[1]= DummyStateMachine.newApplication(seqNames[1], 1.0);
		seq[2]= DummyStateMachine.newApplication(seqNames[2], 1.0);
	
		seq[0].setDummyAction(DummyAction.ACTIVE);
		seq[1].setDummyAction(DummyAction.ACTIVE);
		seq[2].setDummyAction(DummyAction.ACTIVE);

		server.getDatabase().addAll(seq[0].getRecords());
		server.getDatabase().addAll(seq[1].getRecords());
		server.getDatabase().addAll(seq[2].getRecords());
		assertEquals(12*3, server.getDatabase().count());
		
		ssm= SequenceStateMachine.newApplication(appName,10.0,seqNames);
		assertNotNull(ssm);
		server.getDatabase().addAll(ssm.getRecords());
		assertEquals(12*3+12, server.getDatabase().count());

		seq[0].setEnabled(true);
		seq[1].setEnabled(true);
		seq[2].setEnabled(true);

		assertNotNull(seq[0]);
		assertEquals(1000L, seq[0].getTimeout());
		assertTrue(seq[0].isEnabled());
		assertNotNull(seq[1]);
		assertEquals(1000L, seq[1].getTimeout());
		assertTrue(seq[1].isEnabled());
		assertNotNull(seq[2]);
		assertEquals(1000L, seq[2].getTimeout());
		assertTrue(seq[2].isEnabled());
		
		
		assertStateMachine(0.0, State.INACTIVE, seq[0]);
		assertStateMachine(0.0, State.INACTIVE, seq[1]);
		assertStateMachine(0.0, State.INACTIVE, seq[2]);
		assertStateMachine(0.0, State.INACTIVE, ssm);
		
		// simple run
		ssm.stateMachinePrepare();
		wait(1.0);
		assertStateMachine(0.0, State.INACTIVE, seq[0]);
		assertStateMachine(0.0, State.INACTIVE, seq[1]);
		assertStateMachine(0.0, State.INACTIVE, seq[2]);
		assertStateMachine(0.0, State.INACTIVE, ssm);
		
		ssm.stateMachineActivate(false);
		wait(2.0);
		assertStateMachine(100.0, State.ACTIVE, seq[0]);
		assertStateMachine(100.0, State.ACTIVE, seq[1]);
		assertStateMachine(100.0, State.ACTIVE, seq[2]);
		assertStateMachine(100.0, State.ACTIVE, ssm);
		
		// run with some disabled

		ssm.stateMachinePrepare();
		wait(1.0);
		assertStateMachine(0.0, State.INACTIVE, seq[0]);
		assertStateMachine(0.0, State.INACTIVE, seq[1]);
		assertStateMachine(0.0, State.INACTIVE, seq[2]);
		assertStateMachine(0.0, State.INACTIVE, ssm);
		
		seq[0].getRecord("Enabled").setValue(0);
		seq[1].getRecord("Enabled").setValue(0);

		ssm.stateMachineActivate(false);
		wait(1.0);
		assertStateMachine(0.0, State.INACTIVE, seq[0]);
		assertStateMachine(0.0, State.INACTIVE, seq[1]);
		assertStateMachine(100.0, State.ACTIVE, seq[2]);
		assertStateMachine(100.0, State.ACTIVE, ssm);

	}
		
}
