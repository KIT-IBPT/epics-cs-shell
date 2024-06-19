/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>MemoryValueProcessorsTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class MemoryValueProcessorsTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for MemoryValueProcessorsTest.</p>
	 */
	public MemoryValueProcessorsTest() {
		pvCount+=2;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record recA= server.getDatabase().getRecord("A:TEST:01:ValueA");
		Record recB= server.getDatabase().getRecord("A:TEST:01:ValueB");
		
		assertEquals(10, recA.getCount());
		double[] d= recA.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertEquals(10, d.length);
		
		for (int i = 0; i < d.length; i++) {
			assertEquals(i, d[i], 0.000001);
		}
		

		assertEquals("0123456789", recB.getValueAsString());
		assertEquals(Severity.NO_ALARM, recB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recB.getAlarmStatus());
		

	}

}
