/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.automata.SequenceStateMachine;
import org.scictrl.csshell.epics.server.application.automata.StateMachine;
import org.scictrl.csshell.epics.server.application.automata.ValueStateMachine;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

/**
 * <p>ValueStateMachineTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueStateMachineTest extends AbstractConfiguredServerTest {

	String pvValue="A:TEST:SM:01:Value";
	String pvSet="A:TEST:SM:01:Set";
	String pvSetCond="A:TEST:SM:01:SetCond";
	String pvSetOW="A:TEST:SM:01:SetOW";
	String pvRamp="A:TEST:SM:01:Ramp";
	String pvMonitor="A:TEST:SM:01:Monitor";
	String pvSequence="A:TEST:SM:01:Sequence";
	private Record recValue;
	private ValueStateMachine vsmSet;
	private ValueStateMachine vsmSetCond;
	private ValueStateMachine vsmSetOW;
	private ValueStateMachine vsmRamp;
	private ValueStateMachine vsmMonitor;
	private SequenceStateMachine vsmSequence;
	
	/**
	 * Constructor.
	 */
	public ValueStateMachineTest() {
		pvCount+=6*9+4;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * SetUp.
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		recValue= server.getDatabase().getRecord(pvValue);
		assertNotNull(recValue);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);

		assertEquals(6,  server.getDatabase().applicationCount());
		String[] an= server.getDatabase().getApplicationNames();
		assertNotNull(an);
		assertEquals(6, an.length);

		vsmSet= (ValueStateMachine)server.getDatabase().getApplication(pvSet);
		assertNotNull(vsmSet);
		vsmSetCond= (ValueStateMachine)server.getDatabase().getApplication(pvSetCond);
		assertNotNull(vsmSetCond);
		vsmSetOW= (ValueStateMachine)server.getDatabase().getApplication(pvSetOW);
		assertNotNull(vsmSetOW);
		vsmRamp= (ValueStateMachine)server.getDatabase().getApplication(pvRamp);
		assertNotNull(vsmRamp);
		vsmMonitor= (ValueStateMachine)server.getDatabase().getApplication(pvMonitor);
		assertNotNull(vsmMonitor);
		vsmMonitor= (ValueStateMachine)server.getDatabase().getApplication(pvMonitor);
		assertNotNull(vsmMonitor);
		vsmSequence= (SequenceStateMachine)server.getDatabase().getApplication(pvSequence);
		assertNotNull(vsmSequence);
		
	}
	

	/**
	 * Test.
	 */
	@Test
	public void testSet() {
		
		assertEquals(StateMachine.State.INACTIVE, vsmSet.getState());
		assertEquals(0.0, vsmSet.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		vsmSet.stateMachinePrepare();
		
		assertEquals(StateMachine.State.INACTIVE, vsmSet.getState());
		assertEquals(0.0, vsmSet.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);

		vsmSet.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmSet.getState());
		assertEquals(100.0, vsmSet.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmSet.stateMachinePrepare();

		assertEquals(StateMachine.State.ACTIVE, vsmSet.getState());
		assertEquals(0.0, vsmSet.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmSet.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmSet.getState());
		assertEquals(100.0, vsmSet.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);
		
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testSetCond() {
		
		recValue.setValue(5.5);
		wait(0.5);
		
		//assertEquals(StateMachine.State.INACTIVE, vsmSetCond.getState());
		assertEquals(0.0, vsmSetCond.getProgress(),0.00001);
		assertEquals(5.5, recValue.getValueAsDouble(),0.00001);
		
		vsmSetCond.stateMachinePrepare();
		
		//assertEquals(StateMachine.State.INACTIVE, vsmSetCond.getState());
		assertEquals(0.0, vsmSetCond.getProgress(),0.00001);
		assertEquals(5.5, recValue.getValueAsDouble(),0.00001);

		vsmSetCond.stateMachineActivate(false);
		wait(0.5);
		
		assertEquals(StateMachine.State.ACTIVE, vsmSetCond.getState());
		assertEquals(100.0, vsmSetCond.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		recValue.setValue(15.5);
		wait(0.5);

		vsmSetCond.stateMachinePrepare();

		assertEquals(StateMachine.State.ACTIVE, vsmSetCond.getState());
		assertEquals(0.0, vsmSetCond.getProgress(),0.00001);
		assertEquals(15.5, recValue.getValueAsDouble(),0.00001);

		vsmSetCond.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmSetCond.getState());
		assertEquals(100.0, vsmSetCond.getProgress(),0.00001);
		assertEquals(15.5, recValue.getValueAsDouble(),0.00001);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testSequence() {
		
		vsmSequence.stateMachinePrepare();
		
		//assertEquals(StateMachine.State.INACTIVE, vsmSequence.getState());

		vsmSequence.stateMachineActivate(false);
		wait(0.5);
		
		assertEquals(StateMachine.State.ACTIVE, vsmSequence.getState());
		assertEquals(100.0, vsmSequence.getProgress(),0.00001);
	}

	/**
	 * Test.
	 */
	@Test
	public void testSetOW() {
		
		assertEquals(StateMachine.State.INACTIVE, vsmSetOW.getState());
		assertEquals(0.0, vsmSetOW.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		vsmSetOW.stateMachinePrepare();
		
		assertEquals(StateMachine.State.INACTIVE, vsmSetOW.getState());
		assertEquals(0.0, vsmSetOW.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);

		vsmSetOW.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmSetOW.getState());
		assertEquals(100.0, vsmSetOW.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmSetOW.stateMachinePrepare();

		assertEquals(StateMachine.State.INACTIVE, vsmSetOW.getState());
		assertEquals(0.0, vsmSetOW.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmSetOW.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmSetOW.getState());
		assertEquals(100.0, vsmSetOW.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testRamp() {
		
		assertEquals(StateMachine.State.INACTIVE, vsmRamp.getState());
		assertEquals(0.0, vsmRamp.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		vsmRamp.stateMachinePrepare();
		
		assertEquals(StateMachine.State.INACTIVE, vsmRamp.getState());
		assertEquals(0.0, vsmRamp.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);

		vsmRamp.stateMachineActivate(false);

		assertEquals(StateMachine.State.BUSY, vsmRamp.getState());
		assertEquals(0.0, vsmRamp.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		wait(0.5);
		
		assertTrue(100.0>vsmRamp.getProgress());
		assertTrue(0.0<vsmRamp.getProgress());
		assertTrue(10.5>recValue.getValueAsDouble());
		assertTrue(0.0<recValue.getValueAsDouble());

		wait(1.0);
		
		assertEquals(StateMachine.State.ACTIVE, vsmRamp.getState());
		assertEquals(100.0, vsmRamp.getProgress(),0.00001);
		assertEquals(10.5, recValue.getValueAsDouble(),0.00001);

		vsmRamp.stateMachinePrepare();

		assertEquals(StateMachine.State.ACTIVE, vsmRamp.getState());
		assertEquals(0.0, vsmRamp.getProgress(),0.00001);
		assertEquals(10.5, recValue.getValueAsDouble(),0.00001);

		vsmRamp.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmRamp.getState());
		assertEquals(100.0, vsmRamp.getProgress(),0.00001);
		assertEquals(10.5, recValue.getValueAsDouble(),0.00001);
		
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testMonitor() {
		
		assertEquals(StateMachine.State.INACTIVE, vsmMonitor.getState());
		assertEquals(0.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		vsmMonitor.stateMachinePrepare();
		
		assertEquals(StateMachine.State.INACTIVE, vsmMonitor.getState());
		assertEquals(0.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);

		vsmMonitor.stateMachineActivate(false);

		assertEquals(StateMachine.State.BUSY, vsmMonitor.getState());
		assertEquals(0.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(0.0, recValue.getValueAsDouble(),0.00001);
		
		recValue.setValue(10.0);
		
		wait(0.1);

		assertEquals(StateMachine.State.ACTIVE, vsmMonitor.getState());
		assertEquals(100.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmMonitor.stateMachinePrepare();

		assertEquals(StateMachine.State.ACTIVE, vsmMonitor.getState());
		assertEquals(0.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);

		vsmMonitor.stateMachineActivate(false);

		assertEquals(StateMachine.State.ACTIVE, vsmMonitor.getState());
		assertEquals(100.0, vsmMonitor.getProgress(),0.00001);
		assertEquals(10.0, recValue.getValueAsDouble(),0.00001);
		
	}

}
