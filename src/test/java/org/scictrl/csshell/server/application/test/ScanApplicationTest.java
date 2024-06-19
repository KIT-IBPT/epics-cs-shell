/**
 * 
 */
package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.ScanApplication;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;
import org.scictrl.csshell.server.test.RecordMonitor;

import gov.aps.jca.dbr.Status;

/**
 * <p>ScanApplicationTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ScanApplicationTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for ScanApplicationTest.</p>
	 */
	public ScanApplicationTest() {
		pvCount+=18+18+1;
	}

	/**
	 * Test.
	 */
	@Test
	public void testSetpointPV() {
		
		String pv= "A:TEST:Value";
		
		Record recValue= server.getDatabase().getRecord(pv);
		
		Record recSetpoint= server.getDatabase().getRecord("A:TEST:Scan1:Setpoint");
		Record recSetpointPv= server.getDatabase().getRecord("A:TEST:Scan1:SetpointPv");

		ScanApplication scan= (ScanApplication) recSetpoint.getApplication();
		
		assertNotNull(scan);
		
		pause(100);

		checkRecord(recValue);
		checkRecord(recSetpoint);
		checkRecord(recSetpointPv);

		assertEquals(Status.NO_ALARM,recSetpoint.getAlarmStatus());
		
		String s= recSetpointPv.getValueAsString();
		assertNotNull(s);
		s=s.trim();
		assertEquals(0, s.length());
	
		recSetpointPv.setValueAsString("TEST");
		s= recSetpointPv.getValueAsString();
		assertEquals("TEST", s);
		assertEquals(Status.NO_ALARM,recSetpoint.getAlarmStatus());
		
		recSetpointPv.write("TEST1".getBytes());
		pause(1000);
		s= recSetpointPv.getValueAsString();
		assertEquals("TEST1", s);
		assertEquals(Status.UDF_ALARM,recSetpoint.getAlarmStatus());

		recSetpointPv.write(pv.getBytes());
		pause(1000);
		s= recSetpointPv.getValueAsString();
		assertEquals(pv, s);
		assertEquals(Status.NO_ALARM,recSetpoint.getAlarmStatus());
	}
	
	
	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record recValue= server.getDatabase().getRecord("A:TEST:Value");
		
		Record recSetpoint= server.getDatabase().getRecord("A:TEST:Scan:Setpoint");
		Record recStatus= server.getDatabase().getRecord("A:TEST:Scan:Status");
		Record recStatusStepping= server.getDatabase().getRecord("A:TEST:Scan:Status:Scanning");
		Record recRate= server.getDatabase().getRecord("A:TEST:Scan:Rate");
		Record recStep= server.getDatabase().getRecord("A:TEST:Scan:Step");
		Record recStart= server.getDatabase().getRecord("A:TEST:Scan:Start");
		Record recEnd= server.getDatabase().getRecord("A:TEST:Scan:End");
		Record recCmdStart= server.getDatabase().getRecord("A:TEST:Scan:Cmd:Start");
		Record recCount= server.getDatabase().getRecord("A:TEST:Scan:Count");
		Record recRepeat= server.getDatabase().getRecord("A:TEST:Scan:Repeat");

		
		ScanApplication scan= (ScanApplication) recSetpoint.getApplication();
		
		assertNotNull(scan);
		
		pause(100);
		
		checkRecord(recValue);
		checkRecord(recSetpoint);
		checkRecord(recStatus);
		checkRecord(recStatusStepping);
		checkRecord(recRate);
		checkRecord(recStep);
		checkRecord(recStart);
		checkRecord(recEnd);
		checkRecord(recCmdStart);
		checkRecord(recCount);
		checkRecord(recRepeat);
		
		recStart.setValue(0.0);
		recEnd.setValue(.0);
		recStep.setValue(0.0);
		recRate.setValue(0.0);
		recCount.setValue(0.0);
		recRepeat.setValue(0);
		
		assertEquals(0, recStatus.getValueAsInt());
		assertEquals(0, recCount.getValueAsInt());
		assertEquals(0, recRepeat.getValueAsInt());
		
		recValue.setValue(0.5);
		
		pause(100);

		assertEquals(0.5, recValue.getValueAsDouble(),0.000001);
		assertEquals(0.5, recSetpoint.getValueAsDouble(),0.000001);
		
		RecordMonitor mon= new RecordMonitor(recValue);
		
		recStart.setValue(1.0);
		recEnd.setValue(2.0);
		recStep.setValue(0.1);
		recRate.setValue(0.1);
		
		assertEquals(1.0, recStart.getValueAsDouble(),0.000001);
		assertEquals(2.0, recEnd.getValueAsDouble(),0.000001);
		assertEquals(0.1, recStep.getValueAsDouble(),0.000001);
		assertEquals(0.1, recRate.getValueAsDouble(),0.000001);
		
		recCmdStart.write(1);
		
		pause(100);
		
		assertEquals(1, recStatus.getValueAsInt());
		assertEquals(1, recStatusStepping.getValueAsInt());
		
		pause(1500);
		
		assertEquals(0, recStatus.getValueAsInt());
		
		assertEquals(11, mon.values.size());
		assertEquals(1.0, mon.values.get(0),0.000001);
		assertEquals(1.1, mon.values.get(1),0.000001);
		assertEquals(1.9, mon.values.get(9),0.000001);
		assertEquals(2.0, mon.values.get(10),0.000001);

		// TEST RAMP with 2 COUNTS
		
		recRepeat.setValue(1);
		recCount.setValue(2);
		
		assertEquals(2, recCount.getValueAsInt());
		assertEquals(1, recRepeat.getValueAsInt());
		
		mon= new RecordMonitor(recValue);

		recCmdStart.write(1);
		
		pause(2500);
		
		assertEquals(0, recStatus.getValueAsInt());
		
		assertEquals(22, mon.values.size());
		assertEquals(1.0, mon.values.get(0),0.000001);
		assertEquals(1.1, mon.values.get(1),0.000001);
		assertEquals(1.9, mon.values.get(9),0.000001);
		assertEquals(2.0, mon.values.get(10),0.000001);
		assertEquals(1.0, mon.values.get(11),0.000001);
		assertEquals(1.1, mon.values.get(12),0.000001);
		assertEquals(1.9, mon.values.get(20),0.000001);
		assertEquals(2.0, mon.values.get(21),0.000001);
		
		// TEST TOGGLE with 2 COUNTS
		
		recRepeat.setValue(2);
		recCount.setValue(2);
		
		assertEquals(2, recCount.getValueAsInt());
		assertEquals(2, recRepeat.getValueAsInt());
		
		mon= new RecordMonitor(recValue);

		recCmdStart.write(1);
		
		pause(4500);
		
		assertEquals(0, recStatus.getValueAsInt());
		
		assertEquals(41, mon.values.size());
		assertEquals(1.0, mon.values.get(0),0.000001);
		assertEquals(1.1, mon.values.get(1),0.000001);
		assertEquals(1.9, mon.values.get(9),0.000001);
		assertEquals(2.0, mon.values.get(10),0.000001);
		assertEquals(1.9, mon.values.get(11),0.000001);
		assertEquals(1.1, mon.values.get(19),0.000001);
		assertEquals(1.0, mon.values.get(20),0.000001);
		assertEquals(1.1, mon.values.get(21),0.000001);
		assertEquals(1.9, mon.values.get(29),0.000001);
		assertEquals(2.0, mon.values.get(30),0.000001);
		assertEquals(1.9, mon.values.get(31),0.000001);
		assertEquals(1.1, mon.values.get(39),0.000001);
		assertEquals(1.0, mon.values.get(40),0.000001);
	}

}
