/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.scictrl.csshell.epics;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.Status;
import org.scictrl.csshell.Status.State;
import org.scictrl.csshell.Timestamp;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.CTRL;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.LABELS;
import gov.aps.jca.dbr.PRECISION;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;
import gov.aps.jca.event.PutListener;

/**
 * Convenience method for work with JCA and CAJ.
 *
 * @author igor@scictrl.com
 */
public final class EPICSUtilities
{
	/** Seconds of epoch start since UTC time start. */
	public static long TS_EPOCH_SEC_PAST_1970 = 7305 * 86400;

	/**
	 * Tries to create dummy default value for provided DBRType.
	 *
	 * @return new dummy default, basically 0 or empty string wrapped in corresponding primitive data type
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public static Object suggestDefaultValue(DBRType type) {
		if (type.isDOUBLE()) {
			return new double[]{0.0};
		} else if (type.isFLOAT()) {
			return new float[]{0.0f};
		} else if (type.isENUM() || type.isSHORT()) {
			return new short[]{(short) 0};
		} else if (type.isINT()) {
			return new int[]{0};
		} else if (type.isBYTE()) {
			return new byte[]{(byte) 0};
		} else if (type.isSTRING()) {
			return new String[]{""};
		} else {
			return new double[]{0.0};
		}
	}
	/**
	 * Tries to create dummy default value for provided DBRType
	 *
	 * @param type the DBRType to which returned dummy should correspond
	 * @param count the requested value count, must be larger than 0
	 * @return new dummy default, basically 0 or empty string wrapped in corresponding primitive data type
	 */
	public static Object suggestDefaultValue(DBRType type, int count) {
		if (count<1) {
			count=1;
		}
		if (type.isDOUBLE()) {
			double[] r=new double[count];
			Arrays.fill(r, 0.0);
			return r;
		} else if (type.isFLOAT()) {
			float[] r=new float[count];
			Arrays.fill(r, 0.0f);
			return r;
		} else if (type.isENUM() || type.isSHORT()) {
			short[] r=new short[count];
			Arrays.fill(r, (short)0);
			return r;
		} else if (type.isINT()) {
			int[] r=new int[count];
			Arrays.fill(r, 0);
			return r;
		} else if (type.isBYTE()) {
			byte[] r=new byte[count];
			Arrays.fill(r, (byte)0);
			return r;
		} else if (type.isSTRING()) {
			String[] r=new String[count];
			Arrays.fill(r, "");
			return r;
		} else {
			double[] r=new double[count];
			Arrays.fill(r, 0.0);
			return r;
		}
	}
	
	/**
	 * Convert DBR to Java object.
	 *
	 * @param dbr DBR to convet.
	 * @param javaType type to convert to.
	 * @return converted java object.
	 * @param <T> a T class
	 * @param originalType a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public static <T> T toJavaValue(DBR dbr, Class<T> javaType, DBRType originalType)
	{
		if (javaType == null) {
			throw new NullPointerException("javaType");
		}

		if (dbr == null || dbr.getValue() == null) {
			throw new NullPointerException("dbr");
		}

		if (javaType.equals(Double.class)) {
			if (dbr.isDOUBLE()) {
				return javaType.cast(Double.valueOf(((double[])dbr.getValue())[0]));
			}

			if (dbr.isFLOAT()) {
				return javaType.cast(Double.valueOf(((float[])dbr.getValue())[0]));
			}
		}

		if (javaType.equals(double[].class)) {
			if (dbr.isDOUBLE()) {
				return javaType.cast(dbr.getValue());
			}

			if (dbr.isFLOAT()) {
				float[] f = (float[])dbr.getValue();
				double[] d = new double[f.length];

				for (int i = 0; i < d.length; i++) {
					d[i] = f[i];
				}

				return javaType.cast(d);
			}
		}

		if (javaType.equals(Long.class)) {
			if (dbr.isINT()) {
				return javaType.cast(Long.valueOf(((int[])dbr.getValue())[0]));
			}

			if (dbr.isBYTE()) {
				return javaType.cast(Long.valueOf(((byte[])dbr.getValue())[0]));
			}

			if (dbr.isSHORT()) {
				return javaType.cast(Long.valueOf(((short[])dbr.getValue())[0]));
			}

			if (dbr.isENUM()) {
				return javaType.cast( Long.valueOf(((short[])dbr.getValue())[0]));
			}
		}

		if (javaType.equals(long[].class)) {
			if (dbr.isINT()) {
				int[] f = (int[])dbr.getValue();
				long[] d = new long[f.length];

				for (int i = 0; i < d.length; i++) {
					d[i] = f[i];
				}

				return javaType.cast(d);
			}

			if (dbr.isBYTE()) {
				byte[] f = (byte[])dbr.getValue();
				long[] d = new long[f.length];

				for (int i = 0; i < d.length; i++) {
					d[i] = f[i];
				}

				return javaType.cast(d);
			}

			if (dbr.isSHORT()) {
				short[] f = (short[])dbr.getValue();
				long[] d = new long[f.length];

				for (int i = 0; i < d.length; i++) {
					d[i] = f[i];
				}

				return javaType.cast(d);
			}

			if (dbr.isENUM()) {
				short[] f = (short[])dbr.getValue();
				long[] d = new long[f.length];

				for (int i = 0; i < d.length; i++) {
					d[i] = f[i];
				}

				return javaType.cast(d);
			}
		}

		if (javaType.equals(String.class)) {
			if (dbr.isSTRING()) {
				//if type is char, return string composed of chars else return first element

				if (originalType.isBYTE()) {
					String[] val = (String[])dbr.getValue();
					int ascii;
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < val.length; i++) {
						// convert string into integer (represents the ASCII value of the character)
						ascii = Integer.valueOf(val[i]).intValue();
						if (ascii != 0 ) {
							// create 'real' char from ASCII number
							// char singleChar = (char) ascii;
							
							// create new string from list of chars
							sb.append( (char) ascii);
						} else {
							break;
						}
					}
					return javaType.cast(sb.toString());
				}
				return javaType.cast(((String[])dbr.getValue())[0]);
			}
		}

		if (javaType.equals(String[].class)) {
			if (dbr.isSTRING()) {
				return javaType.cast(dbr.getValue());
			}
		}

		if (javaType.equals(BitSet.class)) {
			if (dbr.isENUM()) {
				return javaType.cast(fromLong(((short[])dbr.getValue())[0]));
			}
			if (dbr.isBYTE()) {
				return javaType.cast(fromLong(((byte[])dbr.getValue())[0]));
			}
			if (dbr.isSHORT()) {
				return javaType.cast(fromLong(((short[])dbr.getValue())[0]));
			}
			if (dbr.isINT()) {
				return javaType.cast(fromLong(((int[])dbr.getValue())[0]));
			}
		}
		
		if (javaType.equals(Object.class)) {
			return javaType.cast(Array.get(dbr.getValue(), 0));
		}

		if (javaType.equals(Object[].class)) {
			return javaType.cast(dbr.getValue());
		}

		return javaType.cast(dbr.getValue());
	}

	/**
	 * Get DBR type from java object.
	 *
	 * @return DBR type.
	 * @throws gov.aps.jca.CAException remote exception
	 * @throws java.lang.NullPointerException parameter is null
	 * @param type a {@link org.scictrl.csshell.DataType} object
	 */
	public static DBRType toDBRType(DataType type) throws CAException
	{
		if (type == null) {
			throw new NullPointerException("type");
		}

		if (type == DataType.DOUBLE || type == DataType.DOUBLES) {
			return DBRType.DOUBLE;
		}

		if (type == DataType.LONG || type == DataType.LONGS || type == DataType.PATTERN) {
			return DBRType.INT;
		}

		if (type == DataType.ENUM) {
			return DBRType.ENUM;
		}

		if (type == DataType.STRING || type == DataType.STRINGS || type == DataType.OBJECT || type == DataType.OBJECTS) {
			return DBRType.STRING;
		}

		throw new CAException("Class " + type + " is not supported by CA.");
	}

	/**
	 * Get DBR type from java object.
	 *
	 * @return DBR type.
	 * @throws java.lang.NullPointerException parameter is null
	 * @param dbr a {@link gov.aps.jca.dbr.DBR} object
	 */
	public static DataType toDataType(DBR dbr)
	{
		if (dbr == null) {
			throw new NullPointerException("dbr");
		}
		DBRType type = dbr.getType();
		int c = dbr.getCount();
		
		return toDataType(type,c);
	}
	
	/**
	 * <p>toDataType.</p>
	 *
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @return a {@link org.scictrl.csshell.DataType} object
	 */
	public static DataType toDataType(DBRType type, int count)
	{
		if (type.isDOUBLE()|| type.isFLOAT()) {
			if (count==1) {
				return DataType.DOUBLE;
			}
			return DataType.DOUBLES;
		}

		if (type.isINT() || type.isBYTE() || type.isSHORT() || type.isENUM()) {
			if (count==1) {
				return DataType.LONG;
			}
			return DataType.LONGS;
		}

		if (type.isSTRING()) {
			if (count==1) {
				return DataType.STRING;
			}
			return DataType.STRINGS;
		}

		if (type.isENUM()) {
			return DataType.ENUM;
		}

		return null;
	}

	/**
	 * Convert java object to DBR value, it does expect that Java value is already of right
	 * data type, so only repackaging is necessary, no deep value conversion is done.
	 *
	 * @param value java object to convert.
	 * @return DBR value.
	 * @throws java.lang.NullPointerException parameter is null
	 * @param originalType a {@link gov.aps.jca.dbr.DBRType} object
	 */
	public static Object toDBRValue(Object value, DBRType originalType)
	{
		if (value == null) {
			throw new NullPointerException("value");
		}

		if (value.getClass().equals(Double.class)) {
			return new double[]{ (Double)value };
			}

		if (value.getClass().equals(Float.class)) {
			return new double[]{ (Float)value };
		}

		if (value.getClass().equals(Long.class)) {
			return new int[]{ ((Long)value).intValue() };
		}

		if (value.getClass().equals(Integer.class)) {
			return new int[]{ ((Integer)value).intValue() };
		}

		if (value.getClass().equals(Byte.class)) {
			return new byte[]{ ((Byte)value).byteValue() };
		}

		if (value.getClass().equals(Boolean.class)) {
			return new byte[]{ (byte) (((Boolean)value).booleanValue()?1:0) };
		}

		if (value.getClass().equals(String.class)) {
			if (originalType.isBYTE()) {
				String sVal = (String)value;
				String[] retVal = new String[sVal.length()];
				for (int i = 0; i < retVal.length; i++) {
					retVal[i] = String.valueOf(sVal.charAt(i));
				}
				return retVal;
			} 
			return new String[]{ (String)value };
		}

		if (value.getClass().equals(BitSet.class)) {
			return new int[]{ ( (int)toLong((BitSet)value)) };
		}

		if (value.getClass().equals(double[].class)
		    || value.getClass().equals(String[].class)
		    || value.getClass().equals(int[].class)) {
			return value;
		}

		if (value.getClass().equals(long[].class)) {
			long[] l = (long[])value;
			int[] a = new int[l.length];

			for (int i = 0; i < a.length; i++) {
				a[i] = (int)l[i];
			}

			return a;
		}
		if (value.getClass().equals(float[].class)) {
			float[] l = (float[])value;
			double[] a = new double[l.length];

			for (int i = 0; i < a.length; i++) {
				a[i] = l[i];
			}

			return a;
		}

		if (value.getClass().equals(Object[].class)) {
			Object[] o = (Object[])value;
			String[] s = new String[o.length];

			for (int i = 0; i < s.length; i++) {
				s[i] = o[i].toString();
			}

			return s;
		}

		if (value.getClass().equals(Object.class)) {
			return new String[]{ value.toString() };
		}

		throw new IllegalArgumentException("Class " + value.getClass().getName() + " is not supported by CA.");
	}

	/**
	 * Convert java object to DBR value of provided DBR type.
	 *
	 * @param value java object to convert.
	 * @param type the channel field type of converted value
	 * @return Object value.
	 * @throws java.lang.NullPointerException parameter is null
	 */
	public static Object convertToDBRValue(Object value, DBRType type)
	{
		
		if (value == null) {
			throw new NullPointerException("value");
		}
		
		if (value.getClass().isArray()) {
			int l= Array.getLength(value);
			if (l==0) {
				if (type.isDOUBLE()) {
					return new double[0];
				} 
				if (type.isFLOAT()) {
					return new float[0];
				}
				if (type.isENUM() || type.isSHORT()) {
					return new short[0];
				}
				if (type.isINT()) {
					return new int[0];
				} 
				if (type.isBYTE()) {
					return new byte[0];
				} 
				if (type.isSTRING()) {
					return new String[0];
				} 
				return null;
			}
			
			if (Array.get(value, 0)==null) {
				return null;
			}
			Class<?> cl= Array.get(value, 0).getClass();
			if (type.isDOUBLE()) {
				double[] a= new double[l];
				if (cl.equals(Boolean.class)) {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Boolean)Array.get(value, i))?1.0:0.0;
					}
				} else if (cl.equals(String.class) ) {
					for (int i = 0; i < a.length; i++) {
						a[i]=Double.valueOf(Array.get(value, i).toString());
					}
				} else {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Number)Array.get(value, i)).doubleValue();
					}
				}
				return a;
			} else if (type.isFLOAT()) {
				float[] a= new float[l];
				if (cl.equals(Boolean.class)) {
					for (int i = 0; i < a.length; i++) {
						a[i]=(float) (((Boolean)Array.get(value, i))?1.0:0.0);
					}
				} else if (cl.equals(String.class) ) {
					for (int i = 0; i < a.length; i++) {
						a[i]=Float.valueOf(Array.get(value, i).toString());
					}
				} else {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Number)Array.get(value, i)).floatValue();
					}
				}
				return a;
			} else if (type.isENUM() || type.isSHORT()) {
				short[] a= new short[l];
				if (cl.equals(Boolean.class)) {
					for (int i = 0; i < a.length; i++) {
						a[i]=(short) (((Boolean)Array.get(value, i))?1:0);
					}
				} else if (cl.equals(String.class) ) {
					for (int i = 0; i < a.length; i++) {
						String num= Array.get(value, i).toString().trim();
						if (num.length()==0) {
							num="0";
						}
						int dec= num.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
						if (dec>-1) num=num.substring(0, dec);
						a[i]=Short.valueOf(num);
					}
				} else {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Number)Array.get(value, i)).shortValue();
					}
				}
				return a;
			} else if (type.isINT()) {
				int[] a= new int[l];
				if (cl.equals(Boolean.class)) {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Boolean)Array.get(value, i))?1:0;
					}
				} else if (cl.equals(String.class) ) {
					for (int i = 0; i < a.length; i++) {
						String num= Array.get(value, i).toString().trim();
						if (num.length()==0) {
							num="0";
						}
						int dec= num.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
						if (dec>-1) num=num.substring(0, dec);
						a[i]=Integer.valueOf(num);
					}
				} else {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Number)Array.get(value, i)).intValue();
					}
				}
				return a;
			} else if (type.isBYTE()) {
				byte[] a= new byte[l];
				if (cl.equals(Boolean.class)) {
					for (int i = 0; i < a.length; i++) {
						a[i]=(byte) (((Boolean)Array.get(value, i))?1:0);
					}
				} else if (cl.equals(String.class) ) {
					for (int i = 0; i < a.length; i++) {
						String num= Array.get(value, i).toString().trim();
						if (num.length()==0) {
							num="0";
						}
						int dec= num.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
						if (dec>-1) num=num.substring(0, dec);
						a[i]=Byte.valueOf(num);
					}
				} else {
					for (int i = 0; i < a.length; i++) {
						a[i]=((Number)Array.get(value, i)).byteValue();
					}
				}
				return a;
			} else if (type.isSTRING()) {
				String[] a= new String[l];
				for (int i = 0; i < a.length; i++) {
					a[i]=String.valueOf(Array.get(value, i));
				}
				return a;
			} else {
				return null;
			}
		}

		if (Number.class.isAssignableFrom(value.getClass())) {
			if (type.isBYTE()) {
				return new byte[]{ ((Number)value).byteValue() };
			} else if (type.isDOUBLE()) {
				return new double[]{ ((Number)value).doubleValue() };
			} else if (type.isFLOAT()) {
				return new float[]{ ((Number)value).floatValue() };
			} else if (type.isINT()) {
				return new int[]{ ((Number)value).intValue() };
			} else if (type.isENUM() || type.isSHORT()) {
				return new short[]{ ((Number)value).shortValue() };
			} else if (type.isSTRING()) {
				return new String[]{ ((Number)value).toString() };
			}
		}
		
		if (value.getClass().equals(Boolean.class)) {
			if (type.isBYTE()) {
				return new byte[]{ (byte) (((Boolean)value).booleanValue()?1:0) };
			} else if (type.isDOUBLE()) {
				return new double[]{ (((Boolean)value).booleanValue()?1.0:0.0) };
			} else if (type.isFLOAT()) {
				return new float[]{ (((Boolean)value).booleanValue()?1.0f:0.0f) };
			} else if (type.isINT()) {
				return new int[]{ (((Boolean)value).booleanValue()?1:0) };
			} else if (type.isENUM() || type.isSHORT()) {
				return new short[]{ (short)(((Boolean)value).booleanValue()?1:0) };
			} else if (type.isSTRING()) {
				return new String[]{ ((Boolean)value).toString() };
			}
		}
		
		if (value.getClass().equals(String.class)) {
			String val= (String)value;
			String num=val.trim();
			if (num.length()==0) {
				num="0";
			}
			int dec= num.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
			if (type.isBYTE()) {
				if (num.equalsIgnoreCase(Boolean.FALSE.toString())) {
					byte[] b = {0};
					return b;
				} else if (num.equalsIgnoreCase(Boolean.TRUE.toString())) {
					byte[] b = {1};
					return b;
				} else if (num.length()==1 && Character.isDigit(num.charAt(0))) {
					return new byte[]{ Byte.parseByte(num) };
				} 
				byte[] b = num.getBytes();
				return b;
			} else if (type.isDOUBLE()) {
				return new double[]{ Double.parseDouble(num) };
			} else if (type.isFLOAT()) {
				return new float[]{ Float.parseFloat(num) };
			} else if (type.isINT()) {
				if (dec>-1) num=num.substring(0, dec);
				return new int[]{ Integer.parseInt(num) };
			} else if (type.isENUM() || type.isSHORT()) {
				if (dec>-1) num=num.substring(0, dec);
				return new short[]{ Short.parseShort(num) };
			} else if (type.isSTRING()) {
				return new String[]{ val };
			}
		}
		
		if (value.getClass().equals(BitSet.class)) {
			Long l= Long.valueOf(toLong((BitSet)value));
			if (type.isBYTE()) {
				return new byte[]{ l.byteValue() };
			} else if (type.isDOUBLE()) {
				return new double[]{ l.doubleValue() };
			} else if (type.isFLOAT()) {
				return new float[]{ l.floatValue() };
			} else if (type.isINT()) {
				return new int[]{ l.intValue() };
			} else if (type.isENUM() || type.isSHORT()) {
				return new short[]{ l.shortValue() };
			} else if (type.isSTRING()) {
				return new String[]{ l.toString() };
			}
		}

		throw new IllegalArgumentException("Value to convert with class '" + value.getClass().getName() + "' is not supported by CA for target type '"+type.toString()+"'.");
	}

	/**
	 * <p>toString.</p>
	 *
	 * @param value a {@link java.lang.Object} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @return a {@link java.lang.String} object
	 */
	public static String toString(Object value, DBRType type, int count)
	{
		if (value==null) {
			return "";
		}
		if (value instanceof byte[] && type==DBRType.BYTE && count>1) {
			
			byte[] b= (byte[])value;
			
			StringBuilder sb= new StringBuilder(count);
			for (int i = 0; i < b.length; i++) {
				if (b[i]!=0) {
					sb.appendCodePoint(b[i]);
				}
			}
			return sb.toString();
		}
		
		DataType dt= toDataType(type, count);
		
		if (dt==DataType.DOUBLE) {
			if (value.getClass().isArray()) {
				if (Array.getLength(value)>0) {
					return Double.toString(Array.getDouble(value, 0));
				} else return "";
			} else {
				return value.toString();
			}
		} else if (dt==DataType.DOUBLES) {
			int l=Array.getLength(value);
			StringBuilder sb= new StringBuilder(128);
			if (l>0) {
				sb.append(Array.getDouble(value, 0));
			}
			for (int i = 1; i < l; i++) {
				sb.append(',');
				sb.append(Array.getDouble(value, i));
			}
			return sb.toString();
		} else if (dt==DataType.LONG || dt==DataType.ENUM) {
			if (value.getClass().isArray()) {
				if (Array.getLength(value)>0) {
					return Long.toString(Array.getLong(value, 0));
				} else return "";
			} else {
				return value.toString();
			}
		} else if (dt==DataType.LONGS) {
			int l=Array.getLength(value);
			StringBuilder sb= new StringBuilder(128);
			if (l>0) {
				sb.append(Array.getLong(value, 0));
			}
			for (int i = 1; i < l; i++) {
				sb.append(',');
				sb.append(Array.getLong(value, i));
			}
			return sb.toString();
		} else if (dt==DataType.STRING || dt==DataType.OBJECT) {
			if (value.getClass().isArray()) {
				if (Array.getLength(value)>0 && Array.get(value, 0)!=null) {
					return String.valueOf(Array.get(value, 0).toString());
				} else return "";
			} else {
				return String.valueOf(value);
			}
		} else if (dt==DataType.STRINGS || dt==DataType.OBJECTS) {
			int l=Array.getLength(value);
			StringBuilder sb= new StringBuilder(128);
			if (l>0) {
				sb.append(Array.get(value, 0));
			}
			for (int i = 1; i < l; i++) {
				sb.append(',');
				sb.append(Array.get(value, i));
			}
			return sb.toString();
		}
		
		return String.valueOf(value);
		
	}

	/**
	 * Returns double value if possible or {@link java.lang.Double#NaN}
	 *
	 * @param v a {@link java.lang.Object} object
	 * @return double value if possible or {@link java.lang.Double#NaN}
	 */
	public static double toDouble(Object v)
	{
		if (v instanceof Number) {
			return ((Number)v).doubleValue();
		} if (v.getClass().isArray()) {
			if (Array.getLength(v)>0) {
				return Array.getDouble(v, 0);
			};
		}
		
		return Double.NaN;
		
	}

	/**
	 * Returns double value if possible or {@link java.lang.Double#NaN}
	 *
	 * @param v a {@link java.lang.Object} object
	 * @return double value if possible or {@link java.lang.Double#NaN}
	 */
	public static long toLong(Object v)
	{
		if (v instanceof Number) {
			return ((Number)v).longValue();
		} if (v.getClass().isArray()) {
			if (Array.getLength(v)>0) {
				return Array.getLong(v, 0);
			};
		}
		
		return Long.MAX_VALUE;
		
	}

	/**
	 * Returns integer value if possible or {@link java.lang.Integer#MIN_VALUE}
	 *
	 * @param v a {@link java.lang.Object} object
	 * @return integer value if possible or {@link java.lang.Integer#MIN_VALUE}
	 */
	public int toInteger(Object v)
	{
		if (v instanceof Number) {
			return ((Number)v).intValue();
		} if (v.getClass().isArray()) {
			if (Array.getLength(v)>0) {
				return Array.getInt(v, 0);
			};
		}
		
		return Integer.MIN_VALUE;
		
	}

	/**
	 * Get TIME DBR type.
	 *
	 * @param type DBR type.
	 * @return TIME DBR type
	 * @throws gov.aps.jca.CAException remote exception
	 * @throws java.lang.NullPointerException parameter is null
	 */
	public static DBRType toTimeDBRType(DataType type) throws CAException
	{
		return toTimeDBRType(toDBRType(type));
	}
	/**
	 * Get TIME DBR type.
	 *
	 * @param type DBR type.
	 * @return TIME DBR type or input type if failed to convert to TIME.
	 * @throws java.lang.NullPointerException parameter is null
	 */
	public static DBRType toTimeDBRType(DBRType type)
	{
		if (type == null) {
			throw new NullPointerException("type");
		}

		if (type.isTIME()) {
			return type;
		}

		if (type.isBYTE()) {
			return DBRType.TIME_BYTE;
		}

		if (type.isDOUBLE()) {
			return DBRType.TIME_DOUBLE;
		}

		if (type.isENUM()) {
			return DBRType.TIME_ENUM;
		}

		if (type.isFLOAT()) {
			return DBRType.TIME_FLOAT;
		}

		if (type.isINT()) {
			return DBRType.TIME_INT;
		}

		if (type.isSHORT()) {
			return DBRType.TIME_SHORT;
		}

		if (type.isSTRING()) {
			return DBRType.TIME_STRING;
		}

		return type;
	}

	/**
	 * Converts CA timestamp to UTC Java time.
	 *
	 * @param ts CA timestamp
	 * @return Java UTC
	 */
	public static long toUTC(TimeStamp ts)
	{
		return (ts.secPastEpoch() + TS_EPOCH_SEC_PAST_1970) * 1000
		+ ts.nsec() / 1000000;
	}

	/**
	 * Converts CA timestamp to DAL timestamp.
	 *
	 * @param ts CA timestamp
	 * @return DAL timestamp
	 */
	public static Timestamp convertTimestamp(TimeStamp ts)
	{
		return new Timestamp((ts.secPastEpoch() + TS_EPOCH_SEC_PAST_1970) * 1000, ts.nsec());
	}

	/**
	 * Converts CA timestamp to UTC Java time.
	 *
	 * @param ts CA timestamp
	 * @return Java UTC
	 */
	public static Date toDate(TimeStamp ts)
	{
		return new Date(toUTC(ts));
	}
	
	/**
	 * Converts <code>BitSet</code> to <code>long</code> value if possible.
	 *
	 * @param value the <code>BitSet</code> object
	 * @return long representatnion of the bit set
	 */
	public static final long toLong(BitSet value)
	{
		long longValue = 0;

		for (int i = Math.min(value.length() - 1, 63); i >= 0; i--) {
			longValue <<= 1;

			if (value.get(i)) {
				longValue++;
			}
		}

		return longValue;
	}
	
	/**
	 * Converts <code>long</code> value to <code>BitSet</code>.
	 *
	 * @param value the long value
	 * @return the <code>BitSet</code> corresponding to the value
	 */
	public static final BitSet fromLong(long value)
	{
		BitSet bs = new BitSet();

		int i = 0;

		while (value > 0) {
			bs.set(i++, (value & 1) > 0);
			value = value >> 1;
		}

		return bs;
	}
	
	/**
	 * Puts value of <code>Object</code> parameter to the <code>Channel</code>.
	 *
	 * @param channel the <code>Channel</code> to put value to.
	 * @param value the <code>Object</code> parameter to put to the <code>Channel</code>.
	 * @throws gov.aps.jca.CAException  remote exception
	 * @throws java.lang.NullPointerException parameter is null
	 */
	public static void put(Channel channel, Object value) throws CAException {
		put(channel, value, null);
	}
	
	/**
	 * Puts value of <code>Object</code> parameter to the <code>Channel</code>.
	 *
	 * @param channel the <code>Channel</code> to put value to.
	 * @param value the <code>Object</code> parameter to put to the <code>Channel</code>.
	 * @param listener the <code>PutListener</code> to use (if <code>null</code> no listener is used)
	 * @throws gov.aps.jca.CAException remote exception
	 * @throws java.lang.NullPointerException parameter is null
	 */
	public static void put(Channel channel, Object value, PutListener listener) throws CAException
	{
		if (value == null) {
			throw new NullPointerException("value");
		}

		if (listener == null) {
			if (value.getClass().equals(Double.class)) channel.put((Double) value);
			else if (value.getClass().equals(double[].class)) channel.put((double[]) value);
			else if (value.getClass().equals(Integer.class)) channel.put((Integer) value);
			else if (value.getClass().equals(int[].class)) channel.put((int[]) value);
			else if (value.getClass().equals(Long.class)) channel.put(((Long) value).intValue());
			else if (value.getClass().equals(String.class)) channel.put((String) value);
			else if (value.getClass().equals(String[].class)) channel.put((String[]) value);
			else if (value.getClass().equals(Float.class)) channel.put((Float) value);
			else if (value.getClass().equals(float[].class)) channel.put((float[]) value);
			else if (value.getClass().equals(Byte.class)) channel.put((Byte) value);
			else if (value.getClass().equals(byte[].class)) channel.put((byte[]) value);
			else if (value.getClass().equals(Short.class)) channel.put((Short) value);
			else if (value.getClass().equals(short[].class)) channel.put((short[]) value);
			else if (value.getClass().equals(Boolean.class)) channel.put(((Boolean)value).booleanValue() ? (byte)1 : (byte)0 );
			else throw new CAException("Class " + value.getClass().getName() + " is not supported by CA.");
		}
		else {
			if (value.getClass().equals(Double.class)) channel.put((Double) value, listener);
			else if (value.getClass().equals(double[].class)) channel.put((double[]) value, listener);
			else if (value.getClass().equals(Integer.class)) channel.put((Integer) value, listener);
			else if (value.getClass().equals(int[].class)) channel.put((int[]) value, listener);
			else if (value.getClass().equals(Long.class)) channel.put(((Long) value).intValue());
			else if (value.getClass().equals(String.class)) channel.put((String) value, listener);
			else if (value.getClass().equals(String[].class)) channel.put((String[]) value, listener);
			else if (value.getClass().equals(Float.class)) channel.put((Float) value, listener);
			else if (value.getClass().equals(float[].class)) channel.put((float[]) value, listener);
			else if (value.getClass().equals(Byte.class)) channel.put((Byte) value, listener);
			else if (value.getClass().equals(byte[].class)) channel.put((byte[]) value, listener);
			else if (value.getClass().equals(Short.class)) channel.put((Short) value, listener);
			else if (value.getClass().equals(short[].class)) channel.put((short[]) value, listener);
			else if (value.getClass().equals(Boolean.class)) channel.put(((Boolean)value).booleanValue() ? (byte)1 : (byte)0 , listener);
			else throw new CAException("Class " + value.getClass().getName() + " is not supported by CA.");
		}

	}
	
	/**
	 * Checks the given type and constructs the data type name,
	 * that the given type is associated with.
	 *
	 * @return the datatype
	 * @param t a {@link java.lang.Throwable} object
	 */
	/*public static String getDataType(DBRType type) {
		if (type != null) {
    		if (type.isBYTE()) return CommonDataTypes.BYTE;
    		if (type.isDOUBLE()) return CommonDataTypes.DOUBLE;
    		if (type.isFLOAT()) return CommonDataTypes.FLOAT;
    		if (type.isINT()) return CommonDataTypes.INT;
    		if (type.isSHORT()) return CommonDataTypes.SHORT;
    		if (type.isSTRING()) return CommonDataTypes.STRING;
    		if (type.isENUM()) return CommonDataTypes.ENUM;
		}
		return CommonDataTypes.UNKNOWN;
	}*/
	public static final String toShortErrorReport(Throwable t) {
		StringBuilder sb= new StringBuilder(128);
		
		try {
			appendShortErrorReport(t, sb);
			
			while (t.getCause()!=null) {
				sb.append(", caused by ");
				appendShortErrorReport(t.getCause(), sb);
				t= t.getCause();
			}
		} catch (IOException e) {
			LogManager.getLogger(EPICSUtilities.class).warn("Unhandled exception.", e);
		}
		
		
		return sb.toString();
	}

	/**
	 * <p>appendShortErrorReport.</p>
	 *
	 * @param t a {@link java.lang.Throwable} object
	 * @param buffer a {@link java.lang.Appendable} object
	 * @throws java.io.IOException if any.
	 */
	public static final void appendShortErrorReport(Throwable t, Appendable buffer) throws IOException {
		//if (t instanceof CAException) {
		if (t instanceof CAStatusException) {
			CAStatusException e= (CAStatusException)t;
			buffer.append("CA status error:'");
			buffer.append(e.getStatus().toString());
			buffer.append("'");
			if (e.getMessage()!=null) {
				buffer.append(", message'");
				buffer.append(e.getMessage());
				buffer.append("'");
			}
		//} else if (t instanceof TimeoutException) {
		//} else if (t instanceof ConfigurationException) {
		/*} else if (t instanceof JNIException){
			JNIException e= (JNIException)t;
			buffer.append("JNI error:'");
			buffer.append(e.getStatus().toString());
			buffer.append("'");
			if (e.getMessage()!=null) {
				buffer.append(", message'");
				buffer.append(e.getMessage());
				buffer.append("'");
			}*/
		} else {
			buffer.append("error:'");
			buffer.append(t.toString());
			buffer.append("'");
		}
	}
	
	
	/**
	 * <p>toMetaData.</p>
	 *
	 * @param channel a {@link gov.aps.jca.Channel} object
	 * @param dbr a {@link gov.aps.jca.dbr.DBR} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public static final MetaData toMetaData(Channel channel, DBR dbr) {
		
		Boolean read= channel != null ? channel.getReadAccess() : null;
		Boolean write= channel != null ? channel.getWriteAccess() : null;
		String host= channel != null ? channel.getHostName() : "unknown";
		String name = channel != null ? channel.getName() : "unknown";
		String description = name;
		Class<?> defaultDataType = channel != null ? EPICSUtilities.toDataType(channel.getFieldType(),channel.getElementCount()).getJavaClass() : null;
		
		DataType dataType = EPICSUtilities.toDataType(dbr);
		

		Integer count= dbr.getCount();
		

		String units=null;
		Number min=null;
		Number max=null;
		Number minDL=null;
		Number maxDL=null;
		Number minWL=null;
		Number maxWL=null;
		Number minAL=null;
		Number maxAL=null;
		
		if (dbr.isCTRL())
		{
			CTRL gr = (CTRL)dbr;
			
			units= gr.getUnits();

			// Integer -> Long needed here
			if (dbr.isINT())
			{
				min = Long.valueOf(gr.getLowerCtrlLimit().longValue());
				max = Long.valueOf(gr.getUpperCtrlLimit().longValue());

				minDL = Long.valueOf(gr.getLowerDispLimit().longValue());
				maxDL = Long.valueOf(gr.getUpperDispLimit().longValue());

				minWL = Long.valueOf(gr.getLowerWarningLimit().longValue());
				maxWL = Long.valueOf(gr.getUpperWarningLimit().longValue());

				minAL = Long.valueOf(gr.getLowerAlarmLimit().longValue());
				maxAL = Long.valueOf(gr.getUpperAlarmLimit().longValue());
			}
			else
			{
				min = gr.getLowerCtrlLimit().doubleValue();
				max = gr.getUpperCtrlLimit().doubleValue();

				minDL = gr.getLowerDispLimit().doubleValue();
				maxDL = gr.getUpperDispLimit().doubleValue();

				minWL = gr.getLowerWarningLimit().doubleValue();
				maxWL = gr.getUpperWarningLimit().doubleValue();

				minAL = gr.getLowerAlarmLimit().doubleValue();
				maxAL = gr.getUpperAlarmLimit().doubleValue();
			}
		} else {
			units = "N/A";
		}

		Integer precision = null;
		String format = null;
		
		if (dbr.isPRECSION())
		{
			precision = Integer.valueOf(((PRECISION)dbr).getPrecision());
			format = "%."  + precision + "f";
		} else if (dbr.isSTRING()) {
			format = "%s";
		} else {
			format = "%d";
		}

		String[] states = null;
		Object[] stateValues = null;
		
		if (dbr.isLABELS())
		{
			states = ((LABELS)dbr).getLabels();

			// create array of values (Long values)
			stateValues = new Object[states.length];
			for (int i = 0; i < states.length; i++) {
				stateValues[i] = Long.valueOf(i);
			}
		}

		MetaDataImpl md= new MetaDataImpl(name, description, min, max, minDL, maxDL, minWL, maxWL, minAL, maxAL, states, stateValues, format, units, count, precision, dataType, defaultDataType, read, write, host);
	
		return md;
	}

	/**
	 * Tries to extract timestamp from DBR.
	 * If this fails, then current time is returned.
	 *
	 * @param dbr reciving DBR
	 * @return timestamp from DBR if possible or current local time
	 */
	public static Timestamp toTimestamp(DBR dbr) {
		if (dbr !=null && dbr.isTIME()) {
			TimeStamp ts= ((TIME)dbr).getTimeStamp();
			if (ts==null) {
				return new Timestamp();
			}
			return convertTimestamp(ts);
		}
		return new Timestamp();
	}
	
	/**
	 * Converts Java (UTC) time to EPICS CA Timestamp.
	 *
	 * @param utc JAva time
	 * @return EPICS CA TimeStamp
	 */
	public static TimeStamp toTimeStamp(final long utc) {
		long secPastEpoch= utc/1000 - TS_EPOCH_SEC_PAST_1970;
	    long nsec= (utc%1000)*1000000;
	    return new TimeStamp(secPastEpoch, nsec);
	}
	
	/**
	 * <p>toStatus.</p>
	 *
	 * @param dbr a {@link gov.aps.jca.dbr.DBR} object
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public static final Status toStatus(final DBR dbr) {
		if (dbr== null  || !dbr.isSTS()) {
			return Status.fromStates(State.UNDEFINED);
		}
		final STS sts= (STS)dbr;

		final Severity se = sts.getSeverity();
		
		Status st;

		if (se == Severity.NO_ALARM) {
			st = Status.fromStates(State.NORMAL);
		} else if (se == Severity.MINOR_ALARM) {
			st = Status.fromStates(State.WARNING);
		} else if (se == Severity.MAJOR_ALARM) {
			st = Status.fromStates(State.ALARM);
		} else if (se == Severity.INVALID_ALARM) {
			st = Status.fromStates(State.INVALID);
		} else {
			st = Status.fromStates(State.UNDEFINED);
		}
		
		return st;
	}
	
	
	/**
	 * <p>deepEquals.</p>
	 *
	 * @param o1 a {@link java.lang.Object} object
	 * @param o2 a {@link java.lang.Object} object
	 * @return a boolean
	 */
	public static final boolean deepEquals(Object o1, Object o2) {
		
		if (o1==null && o2 == null) {
			return true;
		}
		
		if (o1==null || o2 == null) {
			return false;
		}
		
		if (o1==o2) {
			return true;
		}

		if (!o1.getClass().isArray() && !o2.getClass().isArray()) {
			return o1.equals(o2);
		}
		
		if (!o1.getClass().isArray() || !o2.getClass().isArray()) {
			return false;
		}
		
		int l=Array.getLength(o1); 

		if (l != Array.getLength(o2)) {
			return false;
		}
		
		for (int i = 0; i < l; i++) {
			Object oo1= Array.get(o1, i);
			Object oo2= Array.get(o2, i);
			if (!oo1.equals(oo2)) {
				return false;
			}
		}

		return true;
	}
	
	
}

/* __oOo__ */
