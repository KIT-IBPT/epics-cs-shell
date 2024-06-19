/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.TimeValueProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>CounterProcessorsTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class CounterProcessorsTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for CounterProcessorsTest.</p>
	 */
	public CounterProcessorsTest() {
		pvCount+=7;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		TimeValueProcessor proc= new TimeValueProcessor("DURATION");
		
		Record recA= server.getDatabase().getRecord("A:TEST:01:ValueA");
		Record recB= server.getDatabase().getRecord("A:TEST:01:ValueB");
		Record recCheck= server.getDatabase().getRecord("A:TEST:01:Check");
		Record recCountA= server.getDatabase().getRecord("A:TEST:01:CountA");
		Record recCountB= server.getDatabase().getRecord("A:TEST:01:CountB");
		Record recStrA= server.getDatabase().getRecord("A:TEST:01:CountA:String");
		Record recStrB= server.getDatabase().getRecord("A:TEST:01:CountB:String");
		
		assertEquals(0.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(Severity.NO_ALARM, recA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recA.getAlarmStatus());

		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recB.getAlarmStatus());
		
		TimeStamp ts= recCheck.getTimestamp();
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());

		// it is persistant, so we have to set it to 0, we do no know what is in persistancy file.
		assertTrue(recCountB.isPersistent());
		recCountB.setValue(0.0);
		assertEquals(0.0, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());

		assertEquals(Severity.NO_ALARM, recStrA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recStrA.getAlarmStatus());
		assertEquals(Severity.NO_ALARM, recStrB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recStrB.getAlarmStatus());

		// some changes, below criteria, no timestamp update
		
		recA.setValue(1.0);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(1.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(0.0, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals("0:00:00", recStrB.getValueAsString());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()))[0], recStrB.getValueAsString());
		
		
		recA.setValue(9.0);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(9.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(0.0, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals("0:00:00", recStrB.getValueAsString());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()))[0], recStrB.getValueAsString());
		
		recB.setValue(1);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(9.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(true, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(0.0, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals("0:00:00", recStrB.getValueAsString());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()))[0], recStrB.getValueAsString());

		// now within criteria, value update should happend
		
		recA.setValue(11.0);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(true, recB.getValueAsBoolean());
		assertEquals(true, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertNotEquals(ts, recCheck.getTimestamp());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(0.0, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals("0:00:00", recStrB.getValueAsString());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()))[0], recStrB.getValueAsString());

		// wait for counter to update
		
		try {
			Thread.yield();
			Thread.sleep(1100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(true, recCheck.getValueAsBoolean());
		long countA= recCountA.getValueAsInt();
		assertTrue(countA>0);
		long countB= recCountB.getValueAsInt();
		assertTrue(countB>0);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()*1000))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// add some errors, should knock out criteria immediately
		
		recA.updateAlarm(Severity.MAJOR_ALARM, Status.COMM_ALARM);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(true, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCheck.getAlarmStatus());
		assertNotEquals(ts, recCheck.getTimestamp());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();

		assertEquals(0,recCountA.getValueAsInt());
		assertEquals(countB,recCountB.getValueAsInt());
		assertEquals(Severity.INVALID_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.INVALID_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// remove errors, should put back to criteria immediately
		
		recA.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM);
		try {
			Thread.yield();
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(true, recB.getValueAsBoolean());
		assertEquals(true, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertNotEquals(ts, recCheck.getTimestamp());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();
		assertEquals(0.0, recCountA.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		
		assertEquals(countB, recCountB.getValueAsDouble(),0.000001);
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// wait for counter to update
		
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		countA= recCountA.getValueAsInt();
		assertTrue(countA>0);
		countB= recCountB.getValueAsInt();
		assertTrue(countB>0);
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertNotEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountA.getValueAsInt()*1000))[0], recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// now out of criteria, value update should happend
		
		recB.setValue(0);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertNotEquals(ts, recCheck.getTimestamp());
		assertTrue(ts.LT(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();
		assertEquals(0,recCountA.getValueAsInt());
		assertTrue(countB<=recCountB.getValueAsInt());
		countB=recCountB.getValueAsInt();
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// add some errors
		
		recA.updateAlarm(Severity.MAJOR_ALARM, Status.COMM_ALARM);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCheck.getAlarmStatus());
		assertTrue(ts.LE(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();

		assertEquals(0,recCountA.getValueAsInt());
		assertEquals(countB,recCountB.getValueAsInt());
		assertEquals(Severity.INVALID_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.INVALID_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.COMM_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

		// remove errors
		
		recA.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(11.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recB.getValueAsBoolean());
		assertEquals(false, recCheck.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recCheck.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCheck.getAlarmStatus());
		assertTrue(ts.LE(recCheck.getTimestamp()));
		ts= recCheck.getTimestamp();

		assertEquals(0,recCountA.getValueAsInt());
		assertEquals(countB,recCountB.getValueAsInt());
		assertEquals(Severity.NO_ALARM, recCountA.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountA.getAlarmStatus());
		assertEquals(Severity.NO_ALARM, recCountB.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recCountB.getAlarmStatus());
		assertEquals("0:00:00", recStrA.getValueAsString());
		assertEquals(((String[])proc.convert(recCountB.getValueAsInt()*1000))[0], recStrB.getValueAsString());

	}

}
