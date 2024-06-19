package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Array;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;
import org.scictrl.csshell.epics.server.processor.SummaryAlarmProcessor;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

/**
 * <p>SingleConnectionTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class SingleConnectionTest extends AbstractSimpleServerTest {

	private String pvs = "A:TEST:Test001";
	
	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testOneShot_MemoryValueProcessor() throws Exception {
		
		Record[] r= new Record[1];
		
		r[0]= new Record(pvs, DBRType.DOUBLE, 1);
		r[0].setProcessor(new MemoryValueProcessor());
		
		HierarchicalConfiguration hc= new HierarchicalConfiguration();
		hc.setProperty("value", Double.valueOf(0.0));
		
		r[0].getProcessor().configure(r[0], hc);
		
		server.getDatabase().addAll(r);
		
		assertEquals(1, server.recordCount());
		
		assertEquals(0, server.pvCount());

		@SuppressWarnings("unchecked")
		Poop<Double, DBR> p= (Poop<Double, DBR>) connector.getOneShot(pvs);

		assertNotNull(p);
	}

	
	private void prepare(DBRType type, Object value, int count) throws Exception {
		
		Record[] r= new Record[1];
		
		r[0]= new Record(pvs, type, count);
		r[0].setProcessor(new MemoryValueProcessor());
		
		HierarchicalConfiguration hc= new HierarchicalConfiguration();
		
		if (count>1) {
			r[0].getProcessor().configure(r[0], hc);
			r[0].setValue(value);
		} else {
			hc.setProperty("value", value);
			r[0].getProcessor().configure(r[0], hc);
		}
		
		
		server.getDatabase().addAll(r);
		
		assertEquals(1, server.recordCount());
		assertEquals(0, server.pvCount());

		Poop<?, ?> p= connector.getOneShot(pvs);

		assertNotNull(p);
		if (value.getClass().isArray()) {
			Object o= p.getValue();
			int len= Array.getLength(value);
			assertEquals(len, Array.getLength(o));
			for (int i = 0; i < len; i++) {
				assertEquals(Array.get(value, i), Array.get(o,i));
			}
		} else {
			assertEquals(value, p.getValue());
		}

		MetaData md= connector.getMetaData(pvs, EPICSUtilities.toDataType(type, count));
		assertNotNull(md);
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testDouble() throws Exception {
		
		DBRType t= DBRType.DOUBLE; 
		prepare(t, Double.valueOf(1.2), 1);
		
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testDoubleArray() throws Exception {
		
		DBRType t= DBRType.DOUBLE;
		double[] value= {1.1,2.2,3.3};
		prepare(t, value, value.length);
		
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testString() throws Exception {
		
		DBRType t= DBRType.STRING;
		String value= "test";
		prepare(t, value, 1);
		
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testStringArray() throws Exception {
		
		DBRType t= DBRType.STRING;
		String[] value= {"test","me","some"};
		prepare(t, value, value.length);
		
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testOneShot_SummaryAlarmProcessor() throws Exception {
		
		Record[] r= new Record[1];
		
		r[0]= new Record(pvs, DBRType.BYTE, 1);
		r[0].setProcessor(new SummaryAlarmProcessor());
		
		HierarchicalConfiguration hc= new HierarchicalConfiguration();
		hc.setProperty("value.links", "A:SR:OperationStatus:01:Mode");
		
		r[0].getProcessor().configure(r[0], hc);
		
		server.getDatabase().addAll(r);
		
		assertEquals(1, server.recordCount());
		assertEquals(0, server.pvCount());

		@SuppressWarnings("unchecked")
		Poop<Long, DBR> p= (Poop<Long, DBR>) connector.getOneShot(pvs);

		assertNotNull(p);
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testConnect_SummaryAlarmProcessor() throws Exception {
		
		Record[] r= new Record[1];
		
		r[0]= new Record(pvs, DBRType.BYTE, 1);
		r[0].setProcessor(new SummaryAlarmProcessor());
		
		HierarchicalConfiguration hc= new HierarchicalConfiguration();
		hc.setProperty("value.links", "A:SR:OperationStatus:01:Mode");
		
		r[0].getProcessor().configure(r[0], hc);
		
		server.getDatabase().addAll(r);
		
		assertEquals(1, server.recordCount());
		assertEquals(0, server.pvCount());

		@SuppressWarnings("unchecked")
		EPICSConnection<Long> con= (EPICSConnection<Long>) connector.newConnection(pvs, DataType.LONG);
		
		assertNotNull(con);
		
		con.waitTillConnected();
		
		assertEquals(true, con.isConnected());
	}

}
