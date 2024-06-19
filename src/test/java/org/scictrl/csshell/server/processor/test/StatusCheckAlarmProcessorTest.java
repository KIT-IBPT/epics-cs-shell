/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.StatusCheckAlarmProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>StatusCheckAlarmProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class StatusCheckAlarmProcessorTest extends AbstractConfiguredServerTest {


	/**
	 * Constructor.
	 */
	public StatusCheckAlarmProcessorTest() {
		pvCount+=2;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record rec= server.getDatabase().getRecord("A:TEST:PS:01:Status:Alarm");
		StatusCheckAlarmProcessor proc= (StatusCheckAlarmProcessor) rec.getProcessor();
		
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(false, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(Severity.MAJOR_ALARM, proc.getAlarmSeverity());
		assertEquals(Status.STATE_ALARM, proc.getAlarmStatus());
		
		Record pv= server.getDatabase().getRecord("A:TEST:PS:01:Status");
		assertEquals(0, pv.getValueAsInt());

		// arm the processor 
		pv.setValue(1);
		//System.out.println("SET "+pv.getValueAsDouble());

		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(1, pv.getValueAsInt());
		assertEquals(true, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		Thread.yield();
		try {Thread.sleep(600);} catch (InterruptedException e) {e.printStackTrace();}
		
		// disarm the processor 
		pv.setValue(0);
		//System.out.println("SET "+pv.getValueAsDouble());

		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(0, pv.getValueAsInt());
		assertEquals(false, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		Thread.yield();
		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

		// still disarmed
		assertEquals(0, pv.getValueAsInt());
		assertEquals(false, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		// arm the processor 
		pv.setValue(1);
		//System.out.println("SET "+pv.getValueAsDouble());

		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(1, pv.getValueAsInt());
		assertEquals(true, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		Thread.yield();
		try {Thread.sleep(1100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(1, pv.getValueAsInt());
		assertEquals(true, proc.getValueAsBoolean());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.STATE_ALARM, rec.getAlarmStatus());
		
		// disarm the processor 
		pv.setValue(0);
		//System.out.println("SET "+pv.getValueAsDouble());

		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(0, pv.getValueAsInt());
		assertEquals(false, proc.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
	}
}
