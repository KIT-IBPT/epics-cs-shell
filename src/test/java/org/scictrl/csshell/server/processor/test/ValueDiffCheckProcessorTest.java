/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.ValueDiffAlarmProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>ValueDiffCheckProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueDiffCheckProcessorTest extends AbstractConfiguredServerTest {
	
	/**
	 * Constructor.
	 */
	public ValueDiffCheckProcessorTest() {
		pvCount+=3;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record rec= server.getDatabase().getRecord("A:TEST:PS:01:Current:Diff");
		ValueDiffAlarmProcessor proc= (ValueDiffAlarmProcessor) rec.getProcessor();
		
		assertEquals(1.0,proc.getValueWindow(),0.00001);
		assertEquals(2000,proc.getTimeWindow());
		TimeStamp ts= rec.getTimestamp();
		
		Record getR= server.getDatabase().getRecord("A:TEST:PS:01:Current:Readback");
		Record setR= server.getDatabase().getRecord("A:TEST:PS:01:Current:Setpoint:Get");

		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());
		
		assertEquals(0.0, getR.getValueAsDouble(), 0.0001);
		assertEquals(0.0, setR.getValueAsDouble(), 0.0001);
		
		// setpoint changes over treshold
		
		setR.setValue(2.0);
		
		assertEquals(0.0, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		
		// slow readback response, but still within time window
		
		getR.setValue(0.1);
		
		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		// waiting for end of time window
		try {
			Thread.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// no update no status change, should we internally trigger update?
		
		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));
		
		// no status change, first readback peak is suppressed for timewindow/10, because readback 
		// could overpass the current change update   

		getR.setValue(0.2);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		assertEquals(0.2, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		//assertEquals(1, rec.getValueAsInt());
		//assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		//assertEquals(Status.STATE_ALARM, rec.getAlarmStatus());
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// second radeback update still within readback peak supress of timewindow/10
		
		getR.setValue(0.3);

		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		assertEquals(0.3, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		//assertEquals(1, rec.getValueAsInt());
		//assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		//assertEquals(Status.STATE_ALARM, rec.getAlarmStatus());
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// waiting for end of readback peak time window/2
		try {
			Thread.sleep(1010);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// third radeback update outside readback peak supress of timewindow/2
		
		getR.setValue(0.4);

		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ts= rec.getTimestamp();
		
		assertEquals(0.4, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.STATE_ALARM, rec.getAlarmStatus());
		//assertEquals(0, rec.getValueAsInt());
		//assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		//assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		// readback catches with setpoint, alarm is off

		getR.setValue(2.0);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ts= rec.getTimestamp();

		assertEquals(2.0, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		// setpoint moved to new value, because readback peak time is not reset, alarm appears
		getR.setValue(0.1);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ts= rec.getTimestamp();

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(2.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(1, rec.getValueAsInt());
		assertEquals(Severity.MAJOR_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.STATE_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		// now also setpoint event catches up with the readback event
		setR.setValue(0.0);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ts= rec.getTimestamp();

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(0.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertEquals(ts,rec.getTimestamp());

		// let reset it a bit
		
		getR.setValue(0.2);
		getR.setValue(0.3);
		getR.setValue(0.4);
		getR.setValue(0.5);
		getR.setValue(0.6);
		getR.setValue(0.5);
		getR.setValue(0.4);
		getR.setValue(0.3);
		getR.setValue(0.2);
		getR.setValue(0.1);
		
		synchronized (this) {
			try {
				this.wait(2100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(0.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		getR.setValue(0.2);
		getR.setValue(0.3);
		getR.setValue(0.4);
		getR.setValue(0.5);
		getR.setValue(0.6);
		getR.setValue(0.5);
		getR.setValue(0.4);
		getR.setValue(0.3);
		getR.setValue(0.2);
		getR.setValue(0.1);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(0.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));
		
		// now do a big jump with setpoint first
		
		setR.setValue(10.0);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// wait for middle of time window
		
		synchronized (this) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// now valid readback comes
		
		getR.setValue(10.1);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(10.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// let reset it a bit
		
		getR.setValue(10.2);
		getR.setValue(10.3);
		getR.setValue(10.4);
		getR.setValue(10.5);
		getR.setValue(10.6);
		getR.setValue(10.5);
		getR.setValue(10.4);
		getR.setValue(10.3);
		getR.setValue(10.2);
		getR.setValue(10.1);
		
		synchronized (this) {
			try {
				this.wait(2100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(10.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		getR.setValue(10.2);
		getR.setValue(10.3);
		getR.setValue(10.4);
		getR.setValue(10.5);
		getR.setValue(10.6);
		getR.setValue(10.5);
		getR.setValue(10.4);
		getR.setValue(10.3);
		getR.setValue(10.2);
		getR.setValue(10.1);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(10.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));
		
		// now do a big jump with readback first
		
		getR.setValue(0.1);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// wait for middle of time window
		
		synchronized (this) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(10.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));

		// now valid setpoint comes
		
		setR.setValue(0.0);
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(0.1, getR.getValueAsDouble(), 0.0001);
		assertEquals(0.0, setR.getValueAsDouble(), 0.0001);
		
		assertEquals(0, rec.getValueAsInt());
		assertEquals(Severity.NO_ALARM, rec.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, rec.getAlarmStatus());
		assertTrue(ts.LT(rec.getTimestamp()));
	}

}
