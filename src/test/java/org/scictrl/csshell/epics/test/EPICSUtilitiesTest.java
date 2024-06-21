/**
 * 
 */
package org.scictrl.csshell.epics.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;

import org.junit.Test;
import org.scictrl.csshell.epics.EPICSUtilities;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>EPICSUtilitiesTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class EPICSUtilitiesTest {

	/**
	 * Constructor.
	 */
	public EPICSUtilitiesTest() {
	}
	
	/**
	 * Test.
	 */
	@Test
	public void test_toString() {
		
		test_toString(new int[]{0,1}, DBRType.GR_INT, 2, "0,1");
		test_toString(new int[]{0}, DBRType.GR_INT, 1, "0");
		test_toString(new int[]{}, DBRType.GR_INT, 1, "");

		test_toString(new double[]{0.0}, DBRType.CTRL_DOUBLE, 1, "0.0");
		test_toString(new double[]{0.0,1.1111}, DBRType.CTRL_DOUBLE, 2, "0.0,1.1111");
		test_toString(new double[]{}, DBRType.CTRL_DOUBLE, 1, "");
		test_toString(new double[]{}, DBRType.CTRL_DOUBLE, 0, "");
		
		test_toString(new String[]{"A"}, DBRType.CTRL_STRING, 1, "A");
		test_toString(new String[]{"A","B"}, DBRType.CTRL_STRING, 2, "A,B");
		test_toString(new String[]{""}, DBRType.CTRL_STRING, 1, "");
		test_toString(new String[]{}, DBRType.CTRL_STRING, 1, "");
		
		test_toString(new Object[]{}, DBRType.CTRL_STRING, 1, "");
		test_toString(new Object[]{null}, DBRType.CTRL_STRING, 1, "");

	}

	private void test_toString(Object in, DBRType type, int count, String out) {
		String res= EPICSUtilities.toString(in, type, count);
		
		assertEquals(in+" "+type+" "+count, out, res);
		
	}
	
	/**
	 * Test.
	 */
	@Test
	public void test_convertToDBRValue() {
		test_convertToDBRValue(0.1, DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue("0.1", DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue("0", DBRType.CTRL_DOUBLE, new double[]{0.0});
		test_convertToDBRValue(" 0.1 ", DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(" 0 ", DBRType.CTRL_DOUBLE, new double[]{0.0});
		test_convertToDBRValue("", DBRType.CTRL_DOUBLE, new double[]{0.0});
		test_convertToDBRValue(1, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(1L, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(true, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(false, DBRType.CTRL_DOUBLE, new double[]{0.0});
		test_convertToDBRValue(new double[]{0.1}, DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(new double[]{0.1,0.2}, DBRType.CTRL_DOUBLE, new double[]{0.1,0.2});
		test_convertToDBRValue(new int[]{1}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new int[]{1,2}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new long[]{1L}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new long[]{1L,2L}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_DOUBLE, new double[]{1.0,0.0});
		test_convertToDBRValue(new String[]{"0.1"}, DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(new String[]{"0.1","0.2"}, DBRType.CTRL_DOUBLE, new double[]{0.1,0.2});
		
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(Integer.valueOf(1), DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(Long.valueOf(1L), DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_DOUBLE, new double[]{0.0});
		test_convertToDBRValue(new Double[]{0.1}, DBRType.CTRL_DOUBLE, new double[]{0.1});
		test_convertToDBRValue(new Double[]{0.1,0.2}, DBRType.CTRL_DOUBLE, new double[]{0.1,0.2});
		test_convertToDBRValue(new Integer[]{1}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new Integer[]{1,2}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new Long[]{1L}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new Long[]{1L,2L}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_DOUBLE, new double[]{1.0,2.0});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_DOUBLE, new double[]{1.0});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_DOUBLE, new double[]{1.0,0.0});

		test_convertToDBRValue(0.1, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue("0", DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue("0.1", DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue("0.9", DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue("", DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(1, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(1L, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(true, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(false, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(new double[]{0.1}, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(new double[]{0.1,0.2}, DBRType.CTRL_INT, new int[]{0,0});
		test_convertToDBRValue(new int[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new int[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new short[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new short[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_INT, new int[]{1,0});
		test_convertToDBRValue(new long[]{1L}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new long[]{1L,2L}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new String[]{"0.2"}, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(new String[]{"1.5","0.6"}, DBRType.CTRL_INT, new int[]{1,0});

		test_convertToDBRValue(Integer.valueOf(0), DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(Long.valueOf(1), DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(Short.valueOf((short) 1), DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(new Double[]{0.1}, DBRType.CTRL_INT, new int[]{0});
		test_convertToDBRValue(new Double[]{0.1,0.2}, DBRType.CTRL_INT, new int[]{0,0});
		test_convertToDBRValue(new Integer[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new Integer[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new Short[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new Short[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_INT, new int[]{1,2});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_INT, new int[]{1,0});
		test_convertToDBRValue(new Long[]{1L}, DBRType.CTRL_INT, new int[]{1});
		test_convertToDBRValue(new Long[]{1L,2L}, DBRType.CTRL_INT, new int[]{1,2});

		test_convertToDBRValue(0.1, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue("0", DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue("0.1", DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue("0.9", DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue("", DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(1, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(1L, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(true, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(false, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(new double[]{0.1}, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(new double[]{0.1,0.2}, DBRType.CTRL_ENUM, new short[]{0,0});
		test_convertToDBRValue(new int[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new int[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new short[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new short[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_ENUM, new short[]{1,0});
		test_convertToDBRValue(new long[]{1L}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new long[]{1L,2L}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new String[]{"0.3"}, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(new String[]{"1.2","0.1"}, DBRType.CTRL_ENUM, new short[]{1,0});

		test_convertToDBRValue(Integer.valueOf(1), DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(Long.valueOf(1L), DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(new Double[]{0.1}, DBRType.CTRL_ENUM, new short[]{0});
		test_convertToDBRValue(new Double[]{0.1,0.2}, DBRType.CTRL_ENUM, new short[]{0,0});
		test_convertToDBRValue(new Integer[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new Integer[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new Short[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new Short[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_ENUM, new short[]{1,2});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_ENUM, new short[]{1,0});
		test_convertToDBRValue(new Long[]{1L}, DBRType.CTRL_ENUM, new short[]{1});
		test_convertToDBRValue(new Long[]{1L,2L}, DBRType.CTRL_ENUM, new short[]{1,2});

		test_convertToDBRValue(0.1, DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue("0", DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue("0.1", DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue("0.9", DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue("", DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue(1, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(1L, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(true, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(false, DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue(new short[]{1}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new short[]{1,2}, DBRType.CTRL_SHORT, new short[]{1,2});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_SHORT, new short[]{1,2});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_SHORT, new short[]{1,0});
		test_convertToDBRValue(new String[]{"0.3"}, DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue(new String[]{"1.2","0.1"}, DBRType.CTRL_SHORT, new short[]{1,0});
	
		test_convertToDBRValue(Integer.valueOf(1), DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(Long.valueOf(1L), DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_SHORT, new short[]{0});
		test_convertToDBRValue(new Short[]{1}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new Short[]{1,2}, DBRType.CTRL_SHORT, new short[]{1,2});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_SHORT, new short[]{1,2});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_SHORT, new short[]{1});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_SHORT, new short[]{1,0});

		test_convertToDBRValue(0.1, DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue("0", DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue("0.1", DBRType.CTRL_BYTE, new byte[]{'0','.','1'});
		test_convertToDBRValue("0.9", DBRType.CTRL_BYTE, new byte[]{'0','.','9'});
		test_convertToDBRValue("", DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue(1, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(1L, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(true, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(false, DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_BYTE, new byte[]{1,2});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_BYTE, new byte[]{1,0});
		test_convertToDBRValue(new String[]{"0.3"}, DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue(new String[]{"1.2","0.1"}, DBRType.CTRL_BYTE, new byte[]{1,0});

		test_convertToDBRValue(Integer.valueOf(1), DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(Long.valueOf(1L), DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_BYTE, new byte[]{0});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_BYTE, new byte[]{1,2});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_BYTE, new byte[]{1});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_BYTE, new byte[]{1,0});

		test_convertToDBRValue(0.1, DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue(Double.valueOf(0.1), DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue("0.1", DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue("0", DBRType.CTRL_STRING, new String[]{"0"});
		test_convertToDBRValue("", DBRType.CTRL_STRING, new String[]{""});
		test_convertToDBRValue(1, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(1L, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(true, DBRType.CTRL_STRING, new String[]{"true"});
		test_convertToDBRValue(false, DBRType.CTRL_STRING, new String[]{"false"});
		test_convertToDBRValue(new double[]{0.1}, DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue(new double[]{0.1,0.2}, DBRType.CTRL_STRING, new String[]{"0.1","0.2"});
		test_convertToDBRValue(new int[]{1}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new int[]{1,2}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new long[]{1L}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new long[]{1L,2L}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new String[]{"0.1"}, DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue(new String[]{"0.1","0.2"}, DBRType.CTRL_STRING, new String[]{"0.1","0.2"});
		test_convertToDBRValue(new byte[]{1}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new byte[]{1,2}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new boolean[]{true}, DBRType.CTRL_STRING, new String[]{"true"});
		test_convertToDBRValue(new boolean[]{true,false}, DBRType.CTRL_STRING, new String[]{"true","false"});

		test_convertToDBRValue(Integer.valueOf(1), DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(Long.valueOf(1L), DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(Boolean.TRUE, DBRType.CTRL_STRING, new String[]{"true"});
		test_convertToDBRValue(Boolean.FALSE, DBRType.CTRL_STRING, new String[]{"false"});
		test_convertToDBRValue(new Double[]{0.1}, DBRType.CTRL_STRING, new String[]{"0.1"});
		test_convertToDBRValue(new Double[]{0.1,0.2}, DBRType.CTRL_STRING, new String[]{"0.1","0.2"});
		test_convertToDBRValue(new Integer[]{1}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new Integer[]{1,2}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new Long[]{1L}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new Long[]{1L,2L}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new Byte[]{1}, DBRType.CTRL_STRING, new String[]{"1"});
		test_convertToDBRValue(new Byte[]{1,2}, DBRType.CTRL_STRING, new String[]{"1","2"});
		test_convertToDBRValue(new Boolean[]{true}, DBRType.CTRL_STRING, new String[]{"true"});
		test_convertToDBRValue(new Boolean[]{true,false}, DBRType.CTRL_STRING, new String[]{"true","false"});

		test_convertToDBRValue(Double.NaN, DBRType.CTRL_DOUBLE, new double[]{Double.NaN});

		test_convertToDBRNull(new Object[]{null}, DBRType.CTRL_DOUBLE);
	}
	
	/**
	 * <p>test_convertToDBRValue.</p>
	 *
	 * @param in a {@link java.lang.Object} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param out a {@link java.lang.Object} object
	 */
	public void test_convertToDBRValue(Object in, DBRType type, Object out) {
		Object res= EPICSUtilities.convertToDBRValue(in, type);
		assertNotNull(res);
		assertTrue(res.getClass().isArray());
		assertEquals(out.getClass(), res.getClass());
		int l=Array.getLength(out);
		assertEquals(l, Array.getLength(res));
		for (int i = 0; i < l; i++) {
			assertEquals(Array.get(out, i), Array.get(res, i));
		}
	}
	
	/**
	 * <p>test_convertToDBRNull.</p>
	 *
	 * @param in a {@link java.lang.Object} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public void test_convertToDBRNull(Object in, DBRType type) {
		Object res= EPICSUtilities.convertToDBRValue(in, type);
		assertNull(res);
	}
}
