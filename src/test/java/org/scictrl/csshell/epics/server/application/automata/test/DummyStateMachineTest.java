/**
 * 
 */
package org.scictrl.csshell.epics.server.application.automata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.server.application.automata.DummyStateMachine;
import org.scictrl.csshell.epics.server.application.automata.DummyStateMachine.DummyAction;
import org.scictrl.csshell.epics.server.application.automata.StateMachine.State;
import org.scictrl.csshell.server.test.AbstractSimpleServerTest;

import gov.aps.jca.dbr.DBR;

/**
 * <p>DummyStateMachineTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class DummyStateMachineTest extends AbstractSimpleServerTest {
	
	String appName="A:TEST:Dummy:01";
	String pvState=appName+":State";
	String pvProgress=appName+":Progress";
	private DummyStateMachine dsm;
	
	
	/**
	 * Constructor.
	 */
	public DummyStateMachineTest() {
	}

	/**
	 * {@inheritDoc}
	 *
	 * SetUp.
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();

		dsm= DummyStateMachine.newApplication(appName,1.0);

		assertNotNull(dsm);
		assertEquals(1000L, dsm.getTimeout());
		
		server.getDatabase().addAll(dsm.getRecords());
		
		assertEquals(12, server.getDatabase().count());
	}

	/**
	 * <p>assertStateMachine.</p>
	 *
	 * @param progress a double
	 * @param state a {@link org.scictrl.csshell.epics.server.application.automata.StateMachine.State} object
	 */
	@SuppressWarnings("unchecked")
	public void assertStateMachine(double progress, State state) {
		
		try {
			
			assertEquals(progress, dsm.getProgress(), 0.000001);
			assertEquals(state, dsm.getState());
			
			assertEquals(progress, ((Poop<Double,DBR>)connector.getOneShot(pvProgress)).getValue(), 0.000001);
			assertEquals(Long.valueOf(state.ordinal()), ((Poop<Long,DBR>)connector.getOneShot(pvState)).getValue());
			
		} catch (Exception e) {
			fail(e.toString());
		}
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testActivate() {
		
		dsm.setDummyAction(DummyAction.ACTIVE);
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(100.0, State.ACTIVE);
		dsm.stateMachineAbort();
		assertStateMachine(100.0, State.INACTIVE);
		
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(100.0, State.ACTIVE);

		wait(2.5);
		
		assertStateMachine(100.0, State.ACTIVE);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testBusy() {
		
		dsm.setDummyAction(DummyAction.BUSY);
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(50.0, State.BUSY);
		dsm.stateMachineAbort();
		assertStateMachine(50.0, State.INACTIVE);
		
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(50.0, State.BUSY);

		wait(2.5);
		
		assertStateMachine(50.0, State.FAILED);
	}

	/**
	 * Test.
	 */
	@Test
	public void testFail() {
		
		dsm.setDummyAction(DummyAction.FAIL);
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(10.0, State.FAILED);
		dsm.stateMachineAbort();
		assertStateMachine(10.0, State.INACTIVE);
		
		dsm.stateMachinePrepare();
		assertStateMachine(0.0, State.INACTIVE);
		dsm.stateMachineActivate(false);
		assertStateMachine(10.0, State.FAILED);

		wait(2.5);
		
		assertStateMachine(10.0, State.FAILED);
	}
}
