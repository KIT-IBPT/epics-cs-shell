/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>SemaphoreProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class SemaphoreProcessorTest extends AbstractConfiguredServerTest {

	private String pv;

	/**
	 * <p>Constructor for SemaphoreProcessorTest.</p>
	 */
	public SemaphoreProcessorTest() {
		pv="A:TEST:Machine:01:SemaphoreLock";
		pvCount+=1;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record rec= server.getDatabase().getRecord(pv);
		//ValueDiffAlarmProcessor proc= (ValueDiffAlarmProcessor) rec.getProcessor();
		
		assertEquals("", rec.getValueAsString());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		String idA= "ProcA";
		String idB= "ProcB";
		
		rec.setValueAsString(idA);
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals(idA, rec.getValueAsString());

		// trying overtake
		rec.setValueAsString(idB);
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// already locked to A
		assertEquals(idA, rec.getValueAsString());
		
		// releasing
		rec.setValueAsString("");
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// free
		assertEquals("", rec.getValueAsString());
		
		// now qill work
		rec.setValueAsString(idB);
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// locked to B
		assertEquals(idB, rec.getValueAsString());
		
		
		// waiting timeout reset
		synchronized (this) {
			try {
				this.wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// free
		assertEquals("", rec.getValueAsString());

	}

}
