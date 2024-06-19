/**
 * 
 */
package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.RunningCounterApplication;
import org.scictrl.csshell.epics.server.application.RunningCounterApplication.AverageCalculator;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>RunningCounterApplicationTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class RunningCounterApplicationTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for RunningCounterApplicationTest.</p>
	 */
	public RunningCounterApplicationTest() {
		pvCount+=13+13+1+4+4+11;
	}

	/**
	 * Test.
	 */
	@Test
	public void testSetpointPV() {
		
		String pv= "A:TEST:Value";
		
		Record recValue= server.getDatabase().getRecord(pv);
		
		Record recCalc1= server.getDatabase().getRecord("A:TEST:Calc1:AVG");
		Record recCalc2= server.getDatabase().getRecord("A:TEST:Calc2:AVG");
		Record recCalc3= server.getDatabase().getRecord("A:TEST:Calc3:AVG");
		Record recPv1= server.getDatabase().getRecord("A:TEST:Calc1:InputPv");
		Record recPv2= server.getDatabase().getRecord("A:TEST:Calc2:InputPv");

		RunningCounterApplication calc1= (RunningCounterApplication) recCalc1.getApplication();
		RunningCounterApplication calc2= (RunningCounterApplication) recCalc2.getApplication();
		RunningCounterApplication calc3= (RunningCounterApplication) recCalc2.getApplication();
		
		assertNotNull(calc1);
		assertNotNull(calc2);
		assertNotNull(calc3);
		
		assertEquals(pv, recPv1.getValueAsString());
		assertEquals(pv, recPv2.getValueAsString());
		
		pause(100);

		checkRecord(recValue);
		checkRecord(recCalc1);
		checkRecord(recCalc2);

		assertEquals(Severity.NO_ALARM,recValue.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc3.getAlarmSeverity());
		
		assertEquals(0.0, recValue.getValueAsDouble(),0.000001);
		assertEquals(0.0, recCalc1.getValueAsDouble(),0.000001);
		assertEquals(0.0, recCalc2.getValueAsDouble(),0.000001);
		assertEquals(0.0, recCalc3.getValueAsDouble(),0.000001);
		
		recValue.setValue(1.0);
		
		pause(300);

		assertEquals(1.0, recValue.getValueAsDouble(), 0.000001);
		assertEquals(1.0, recCalc1.getValueAsDouble(), 0.000001);
		assertEquals(1.0, recCalc2.getValueAsDouble(), 0.000001);
		assertEquals(0.0, recCalc3.getValueAsDouble(), 0.000001);
		
		recValue.updateAlarm(Severity.MAJOR_ALARM, Status.HIHI_ALARM, false);

		assertEquals(Severity.MAJOR_ALARM,recValue.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recCalc3.getAlarmSeverity());
		
		recValue.setValue(2.0);
		
		pause(300);

		assertEquals(Severity.MAJOR_ALARM,recValue.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc1.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc3.getAlarmSeverity());

		assertEquals(2.0, recValue.getValueAsDouble(), 0.000001);
		assertEquals(1.0, recCalc1.getValueAsDouble(), 0.000001);
		assertEquals(1.5, recCalc2.getValueAsDouble(), 0.000001);
		assertEquals(0.0, recCalc3.getValueAsDouble(), 0.000001);

		recValue.setValue(3.0);
		
		pause(300);

		assertEquals(3.0, recValue.getValueAsDouble(), 0.000001);
		assertEquals(1.0, recCalc1.getValueAsDouble(), 0.000001);
		assertEquals(2.0, recCalc2.getValueAsDouble(), 0.000001);
		assertEquals(3.0, recCalc3.getValueAsDouble(), 0.000001);

		recValue.updateAlarm(Severity.INVALID_ALARM, Status.COMM_ALARM, false);

		assertEquals(Severity.INVALID_ALARM,recValue.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc1.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recCalc3.getAlarmSeverity());
		
		recValue.setValue(4.0);
		
		pause(300);

		assertEquals(Severity.INVALID_ALARM,recValue.getAlarmSeverity());
		assertEquals(Severity.INVALID_ALARM,recCalc1.getAlarmSeverity());
		assertEquals(Severity.INVALID_ALARM,recCalc2.getAlarmSeverity());
		assertEquals(Severity.INVALID_ALARM,recCalc3.getAlarmSeverity());

		assertEquals(4.0, recValue.getValueAsDouble(), 0.000001);
		assertEquals(1.0, recCalc1.getValueAsDouble(), 0.000001);
		assertEquals(2.0, recCalc2.getValueAsDouble(), 0.000001);
		assertEquals(3.0, recCalc3.getValueAsDouble(), 0.000001);
	}
	
	/**
	 * Test.
	 */
	@Test
	public void calculatorTest( ) {
		
		AverageCalculator calc= new AverageCalculator();
		
		
		calc.update();
		
		assertEquals(60000, calc.interval);
		assertEquals(0, calc.size);
		assertEquals(0, calc.span);
		assertTrue(Double.isNaN(calc.avg));
		assertTrue(Double.isNaN(calc.rms));
		assertTrue(Double.isNaN(calc.std));
	
		calc.add(10.0, 10);
		
		calc.update();
		
		assertEquals(60000, calc.interval);
		assertEquals(1, calc.size);
		assertEquals(0, calc.span);
		assertEquals(10.0, calc.avg, 0.00001);
		assertEquals(10.0, calc.rms, 0.00001);
		assertEquals(0.0, calc.std, 0.00001);
		
		calc.add(10.0, 20);
		
		calc.update();
		
		assertEquals(60000, calc.interval);
		assertEquals(2, calc.size);
		assertEquals(10, calc.span);
		assertEquals(10.0, calc.avg, 0.00001);
		assertEquals(10.0, calc.rms, 0.00001);
		assertEquals(0.0, calc.std, 0.00001);

		calc.add(10.0, 30);
		
		calc.update();
		
		assertEquals(60000, calc.interval);
		assertEquals(3, calc.size);
		assertEquals(20, calc.span);
		assertEquals(10.0, calc.avg, 0.00001);
		assertEquals(10.0, calc.rms, 0.00001);
		assertEquals(0.0, calc.std, 0.00001);

	}

}
