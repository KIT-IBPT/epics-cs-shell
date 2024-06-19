package org.scictrl.csshell;

import java.util.BitSet;

/**
 * Enumeration for data types supported by DAL and this broker.
 *
 * @author igor@scictrl.com
 */
public enum DataType {
	/**
	 * Generic object type.
	 */
	OBJECT(Object.class,Object.class),
	/**
	 * Generic object type, as array.
	 */
	OBJECTS(Object[].class,Object[].class),
	/**
	 * Double data type.
	 */
	DOUBLE(Double.class,double.class),
	/**
	 * Double data type, as array.
	 */
	DOUBLES(double[].class,double[].class),
	/**
	 * Long data type.
	 */
	LONG(Long.class,long.class),
	/**
	 * Long data type, as array.
	 */
	LONGS(long[].class,long[].class),
	/**
	 * String data type.
	 */
	STRING(String.class,String.class),
	/**
	 * String data type, as array.
	 */
	STRINGS(String[].class,String[].class),
	/**
	 * Bit set pattern data type.
	 */
	PATTERN(BitSet.class,long.class),
	/**
	 * Enumerated data type.
	 */
	ENUM(Long.class,long.class);
	
	private Class<?> javaClass;
	private Class<?> primitiveType;

	private DataType(Class<?> javaClass, Class<?> primitiveType) {
		this.javaClass=javaClass;
		this.primitiveType=primitiveType;
	}

	/**
	 * <p>Getter for the field <code>javaClass</code>.</p>
	 *
	 * @return a {@link java.lang.Class} object
	 */
	public Class<?> getJavaClass() {
		return javaClass;
	}

	/**
	 * <p>Getter for the field <code>primitiveType</code>.</p>
	 *
	 * @return a {@link java.lang.Class} object
	 */
	public Class<?> getPrimitiveType() {
		return primitiveType;
	}

	/**
	 * Converts Java data type to one of supported Java data types,
	 * such as <code>Double</code>, <code>Long</code>, <code>String</code> and similar.
	 *
	 * @param type Java data type
	 * @return supported Java data type
	 */
	public static DataType fromJavaClass(Class<?> type) {
		
		if (type==null) {
			return null;
		}
		
		if (type == Double.class 
				|| type == Float.class
				|| type == double.class
				|| type == float.class) {
			return DOUBLE;
		}
		
		if (type == Double[].class 
				|| type == Float[].class
				|| type == double[].class
				|| type == float[].class) {
			return DOUBLES;
		}

		if (type == Long.class 
				|| type == Integer.class
				|| type == Short.class
				|| type == long.class
				|| type == int.class
				|| type == short.class
				|| type == char.class) {
			return LONG;
		}
		
		if (type == Long[].class 
				|| type == Integer[].class
				|| type == Short[].class
				|| type == long[].class
				|| type == int[].class
				|| type == short[].class
				|| type == char[].class) {
			return LONGS;
		}
		
		if (type == String.class) {
			return STRING;
		}
		
		if (type == String[].class) {
			return STRINGS;
		}
		if (type == BitSet.class) {
			return PATTERN;
		}
		if (type == Object[].class) {
			return OBJECTS;
		}
		if (type == Object.class) {
			return OBJECT;
		}
		return null;

	}
	
}
