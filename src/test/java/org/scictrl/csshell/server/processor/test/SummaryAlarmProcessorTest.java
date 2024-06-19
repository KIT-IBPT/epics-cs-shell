/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.SummaryAlarmProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>SummaryAlarmProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class SummaryAlarmProcessorTest extends AbstractConfiguredServerTest {


	/**
	 * Constructor.
	 */
	public SummaryAlarmProcessorTest() {
		pvCount+=7;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record recBit1= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit1:P");
		Record recBit2= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit2:P");
		Record recBit3= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit3:P");
		@SuppressWarnings("unused")
		Record recBit1Dis= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit1:AlarmDisable");
		Record recBit2Dis= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit2:AlarmDisable");
		@SuppressWarnings("unused")
		Record recBit3Dis= server.getDatabase().getRecord("A:TEST:Alarm:01:Bit3:AlarmDisable");
		Record recSum= server.getDatabase().getRecord("A:TEST:Alarm:01:Sum");
		@SuppressWarnings("unused")
		SummaryAlarmProcessor proc= (SummaryAlarmProcessor) recSum.getProcessor();
		
		wait(1.0);

		assertEquals(0, recBit1.getValueAsInt());
		assertEquals(0, recBit2.getValueAsInt());
		assertEquals(0, recBit3.getValueAsInt());
		
		assertEquals(Severity.NO_ALARM, recSum.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recSum.getAlarmStatus());
		assertEquals(0, recSum.getValueAsInt());
		
		recBit1.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM,true);
		recBit2.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM,true);
		recBit3.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM,true);
		
		wait(1.0);

		assertEquals(Severity.INVALID_ALARM, recSum.getAlarmSeverity());
		assertEquals(Status.UDF_ALARM, recSum.getAlarmStatus());
		assertEquals(1, recSum.getValueAsInt());

		recBit2Dis.setValue(1);
		
		wait(2.0);

		assertEquals(Severity.NO_ALARM, recSum.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recSum.getAlarmStatus());
		assertEquals(0, recSum.getValueAsInt());

	}
}
