/**
 * 
 */
package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.ArrayAssemblyApplication;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ArrayAssemblyApplicationTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ArrayAssemblyApplicationTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for ArrayAssemblyApplicationTest.</p>
	 */
	public ArrayAssemblyApplicationTest() {
		pvCount+=2; // pv1 and pv2
		pvCount+=(4+4*3+5)*2; // 2x app
	}

	/**
	 * Test.
	 */
	@Test
	public void test1() {
		
		String pref= "A:TEST:";
		String pv= pref+"Value:";
		
		Record recVal1= server.getDatabase().getRecord(pv+1);
		Record recVal2= server.getDatabase().getRecord(pv+2);

		pref= "A:TEST:Array:1:";

		Record recValues= server.getDatabase().getRecord(pref+"Values");
		Record recAvg= server.getDatabase().getRecord(pref+"AVG");
		Record recStd= server.getDatabase().getRecord(pref+"STD");
		Record recLabel0= server.getDatabase().getRecord(pref+"Label:0");
		Record recLabel1= server.getDatabase().getRecord(pref+"Label:1");
		Record recLabel2= server.getDatabase().getRecord(pref+"Label:2");
		Record recPv0= server.getDatabase().getRecord(pref+"PV:0");
		Record recPv1= server.getDatabase().getRecord(pref+"PV:1");
		Record recPv2= server.getDatabase().getRecord(pref+"PV:2");
		Record recError0= server.getDatabase().getRecord(pref+"Error:0");
		Record recError1= server.getDatabase().getRecord(pref+"Error:1");
		Record recError2= server.getDatabase().getRecord(pref+"Error:2");
		Record recErr= server.getDatabase().getRecord(pref+"Status:ErrorSum");
		Record recLnk= server.getDatabase().getRecord(pref+"Status:LinkError");
		
		wait(1.7);

		assertEquals(Severity.NO_ALARM,recVal1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recVal2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recValues.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recAvg.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recStd.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recErr.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLnk.getAlarmSeverity());

		
		@SuppressWarnings("unused")
		ArrayAssemblyApplication app= (ArrayAssemblyApplication) recValues.getApplication();
		
		assertEquals(3,recValues.getCount());
		
		assertEquals("Element 1", recLabel0.getValueAsString());
		assertEquals("Element 2", recLabel1.getValueAsString());
		assertEquals("Element 3", recLabel2.getValueAsString());
		
		double[] d= recValues.getValueAsDoubleArray();
		double[] da= recAvg.getValueAsDoubleArray();
		double[] ds= recStd.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertNotNull(da);
		assertNotNull(ds);
		
		assertEquals(3, d.length);
		assertEquals(3, da.length);
		assertEquals(3, ds.length);
		
		assertEquals(0.1, d[0], 0.0000001);
		assertEquals(0.0, d[1], 0.0000001);
		assertEquals(0.2, d[2], 0.0000001);
		assertEquals(0.1, da[0], 0.0000001);
		assertEquals(Double.NaN, da[1], 0.0000001);
		assertEquals(0.2, da[2], 0.0000001);
		assertEquals(0.0, ds[0], 0.0000001);
		assertEquals(Double.NaN, ds[1], 0.0000001);
		assertEquals(0.0, ds[2], 0.0000001);
		
		recVal1.updateAlarm(Severity.INVALID_ALARM, Status.TIMEOUT_ALARM);
		recVal1.setValue(0.3);
		
		wait(1.7);

		assertEquals(Severity.INVALID_ALARM,recVal1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recVal2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recValues.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recAvg.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recStd.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv2.getAlarmSeverity());
		assertEquals(Severity.INVALID_ALARM,recError0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recErr.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recLnk.getAlarmSeverity());

		d= recValues.getValueAsDoubleArray();
		da= recAvg.getValueAsDoubleArray();
		ds= recStd.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertNotNull(da);
		assertNotNull(ds);
		
		assertEquals(3, d.length);
		assertEquals(3, da.length);
		assertEquals(3, ds.length);
		
		assertEquals(0.3, d[0], 0.0000001);
		assertEquals(0.0, d[1], 0.0000001);
		assertEquals(0.2, d[2], 0.0000001);
		assertEquals(0.2, da[0], 0.0000001);
		assertEquals(Double.NaN, da[1], 0.0000001);
		assertEquals(0.2, da[2], 0.0000001);
		assertEquals(0.1, ds[0], 0.0000001);
		assertEquals(Double.NaN, ds[1], 0.0000001);
		assertEquals(0.0, ds[2], 0.0000001);

		recVal1.updateAlarm(Severity.MAJOR_ALARM, Status.LOW_ALARM);
		recVal1.setValue(0.4);
		
		wait(1.7);

		assertEquals(Severity.MAJOR_ALARM,recVal1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recVal2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recValues.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recAvg.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recStd.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recError0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recErr.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLnk.getAlarmSeverity());

		d= recValues.getValueAsDoubleArray();
		da= recAvg.getValueAsDoubleArray();
		ds= recStd.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertNotNull(da);
		assertNotNull(ds);
		
		assertEquals(3, d.length);
		assertEquals(3, da.length);
		assertEquals(3, ds.length);
		
		assertEquals(0.4, d[0], 0.0000001);
		assertEquals(0.0, d[1], 0.0000001);
		assertEquals(0.2, d[2], 0.0000001);
		assertEquals(0.26666666, da[0], 0.0000001);
		assertEquals(Double.NaN, da[1], 0.0000001);
		assertEquals(0.2, da[2], 0.0000001);
		assertEquals(0.124721913, ds[0], 0.0000001);
		assertEquals(Double.NaN, ds[1], 0.0000001);
		assertEquals(0.0, ds[2], 0.0000001);

		recVal1.updateAlarm(Severity.NO_ALARM, Status.NO_ALARM);
		recVal1.setValue(0.5);
		
		wait(1.7);

		assertEquals(Severity.NO_ALARM,recVal1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recVal2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recValues.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recAvg.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recStd.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recErr.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLnk.getAlarmSeverity());

		d= recValues.getValueAsDoubleArray();
		da= recAvg.getValueAsDoubleArray();
		ds= recStd.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertNotNull(da);
		assertNotNull(ds);
		
		assertEquals(3, d.length);
		assertEquals(3, da.length);
		assertEquals(3, ds.length);
		
		assertEquals(0.5, d[0], 0.0000001);
		assertEquals(0.0, d[1], 0.0000001);
		assertEquals(0.2, d[2], 0.0000001);
		assertEquals(0.325, da[0], 0.0000001);
		assertEquals(Double.NaN, da[1], 0.0000001);
		assertEquals(0.2, da[2], 0.0000001);
		assertEquals(0.14790199457749037, ds[0], 0.0000001);
		assertEquals(Double.NaN, ds[1], 0.0000001);
		assertEquals(0.0, ds[2], 0.0000001);

	}

	/**
	 * Test.
	 */
	@Test
	public void test2() {
		
		String pref= "A:TEST:";
		String pv= pref+"Value:";
		
		Record recVal1= server.getDatabase().getRecord(pv+1);
		Record recVal2= server.getDatabase().getRecord(pv+2);

		pref= "A:TEST:Array:2:";

		Record recValues= server.getDatabase().getRecord(pref+"Values");
		Record recAvg= server.getDatabase().getRecord(pref+"AVG");
		Record recStd= server.getDatabase().getRecord(pref+"STD");
		Record recLabel0= server.getDatabase().getRecord(pref+"Label:0");
		Record recLabel1= server.getDatabase().getRecord(pref+"Label:1");
		Record recLabel2= server.getDatabase().getRecord(pref+"Label:2");
		Record recPv0= server.getDatabase().getRecord(pref+"PV:0");
		Record recPv1= server.getDatabase().getRecord(pref+"PV:1");
		Record recPv2= server.getDatabase().getRecord(pref+"PV:2");
		Record recError0= server.getDatabase().getRecord(pref+"Error:0");
		Record recError1= server.getDatabase().getRecord(pref+"Error:1");
		Record recError2= server.getDatabase().getRecord(pref+"Error:2");
		Record recErr= server.getDatabase().getRecord(pref+"Status:ErrorSum");
		Record recLnk= server.getDatabase().getRecord(pref+"Status:LinkError");
		
		wait(1.7);

		assertEquals(Severity.NO_ALARM,recVal1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recVal2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recValues.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recAvg.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recStd.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recLabel2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv1.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recPv2.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError0.getAlarmSeverity());
		assertEquals(Severity.NO_ALARM,recError1.getAlarmSeverity());
		assertEquals(Severity.INVALID_ALARM,recError2.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recErr.getAlarmSeverity());
		assertEquals(Severity.MAJOR_ALARM,recLnk.getAlarmSeverity());

		
		@SuppressWarnings("unused")
		ArrayAssemblyApplication app= (ArrayAssemblyApplication) recValues.getApplication();
		
		assertEquals(3,recValues.getCount());
		
		assertEquals("Element 1", recLabel0.getValueAsString());
		assertEquals("Element 2", recLabel1.getValueAsString());
		assertEquals("Element 3", recLabel2.getValueAsString());
		
		wait(1.7);
		
		double[] d= recValues.getValueAsDoubleArray();
		
		assertNotNull(d);
		assertEquals(3, d.length);
		
		assertEquals(0.1, d[0], 0.0000001);
		assertEquals(0.0, d[1], 0.0000001);
		assertEquals(0.0, d[2], 0.0000001);
	}
}
