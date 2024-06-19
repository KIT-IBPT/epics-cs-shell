/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.ValueLevelAlarmProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ValueLevelAlarmProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueLevelAlarmProcessorTest extends AbstractConfiguredServerTest {


	/**
	 * Constructor.
	 */
	public ValueLevelAlarmProcessorTest() {
		pvCount+=2;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record rec= server.getDatabase().getRecord("A:TEST:PS:01:Current:Setpoint:Alarm");
		ValueLevelAlarmProcessor proc= (ValueLevelAlarmProcessor) rec.getProcessor();
		
		assertEquals(Severity.NO_ALARM, proc.getDefaultSeverity());
		assertEquals(Status.NO_ALARM, proc.getDefaultStatus());
		
		double p= 0.01;
		double low= -1.0;
		double lolo= -2.0;
		double high= 1.0;
		double hihi= 2.0;
				
		assertEquals(0.01, p,0.001);
		
		assertEquals(low, proc.getLowerWarningLimit(),proc.getPrecision());
		assertEquals(Severity.MINOR_ALARM, proc.getLowerWarningSeverity());
		assertEquals(Status.LOW_ALARM, proc.getLowerWarningStatus());
		
		assertEquals(lolo, proc.getLowerAlarmLimit(),proc.getPrecision());
		assertEquals(Severity.MAJOR_ALARM, proc.getLowerAlarmSeverity());
		assertEquals(Status.LOLO_ALARM, proc.getLowerAlarmStatus());

		assertEquals(high, proc.getUpperWarningLimit(),proc.getPrecision());
		assertEquals(Severity.MINOR_ALARM, proc.getUpperWarningSeverity());
		assertEquals(Status.HIGH_ALARM, proc.getUpperWarningStatus());
		
		assertEquals(hihi, proc.getUpperAlarmLimit(),proc.getPrecision());
		assertEquals(Severity.MAJOR_ALARM, proc.getUpperAlarmSeverity());
		assertEquals(Status.HIHI_ALARM, proc.getUpperAlarmStatus());

		Record pv= server.getDatabase().getRecord("A:TEST:PS:01:Current:Setpoint");

		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(0.0, pv.getValueAsDouble(), p);
		
		// getting rid of undefined

		double d=p;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		// over high

		d= high+p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MINOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.HIGH_ALARM, rec.getAlarmStatus());

		// in deadbend
		
		d= high-p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MINOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.HIGH_ALARM, rec.getAlarmStatus());

		// over hihi
		
		d= hihi+p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.HIHI_ALARM, rec.getAlarmStatus());

		// in deadband
		
		d= hihi-p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.HIHI_ALARM, rec.getAlarmStatus());
		
		// back to normal

		d= p;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());

		// under low
		
		d= low-p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MINOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.LOW_ALARM, rec.getAlarmStatus());

		// in deadband
		
		d= low+p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MINOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.LOW_ALARM, rec.getAlarmStatus());

		// under lolo
		
		d= lolo-p/2.0;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.LOLO_ALARM, rec.getAlarmStatus());

		// back to normal

		d= -p;
		pv.setValue(d);
		Thread.yield();
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		assertEquals(d, pv.getValueAsDouble(), 0.0001);
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
	}
}
