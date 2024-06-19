package org.scictrl.csshell;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * This class holds DataBush constants.
 *
 * @author igor@scictrl.com
 */
public final class Tools {

	/** Constant <code>EOL_WIN="\r\n"</code> */
	public final static String EOL_WIN="\r\n";
	
	/** Constant <code>ISO_DATE_TIME_MILLI_PATTERN="yyyy-MM-dd'T'HH:mm:ss.SSS"</code> */
	public final static String ISO_DATE_TIME_MILLI_PATTERN="yyyy-MM-dd'T'HH:mm:ss.SSS";
	/** Constant <code>ISO_DATE_TIME_PATTERN="yyyy-MM-dd'T'HH:mm:ss"</code> */
	public final static String ISO_DATE_TIME_PATTERN="yyyy-MM-dd'T'HH:mm:ss";
	/** Constant <code>ISO_DATE_NO_T_TIME_PATTERN="yyyy-MM-dd' 'HH:mm:ss"</code> */
	public final static String ISO_DATE_NO_T_TIME_PATTERN="yyyy-MM-dd' 'HH:mm:ss";
	
	/** Constant <code>FORMAT_ISO_DATE_TIME_MILLI</code> */
	public final static FastDateFormat FORMAT_ISO_DATE_TIME_MILLI= FastDateFormat.getInstance(ISO_DATE_TIME_MILLI_PATTERN);
	/** Constant <code>FORMAT_ISO_DATE_TIME</code> */
	public final static FastDateFormat FORMAT_ISO_DATE_TIME= FastDateFormat.getInstance(ISO_DATE_TIME_PATTERN);
	/** Constant <code>FORMAT_ISO_DATE_NO_T_TIME</code> */
	public final static FastDateFormat FORMAT_ISO_DATE_NO_T_TIME= FastDateFormat.getInstance(ISO_DATE_NO_T_TIME_PATTERN);
	
	/** Constant <code>PARSE_ISO_DATE_TIME_MILLI</code> */
	public final static SimpleDateFormat PARSE_ISO_DATE_TIME_MILLI = new SimpleDateFormat(ISO_DATE_TIME_MILLI_PATTERN);
	/** Constant <code>PARSE_ISO_DATE_NO_T_TIME</code> */
	public final static SimpleDateFormat PARSE_ISO_DATE_NO_T_TIME = new SimpleDateFormat(ISO_DATE_NO_T_TIME_PATTERN);
	
	
	/** Constant <code>FORMAT_F4</code> */
	public final static DecimalFormat FORMAT_F4= new DecimalFormat("0.0000");
	/** Constant <code>FORMAT_F3</code> */
	public final static DecimalFormat FORMAT_F3= new DecimalFormat("0.000");
	
	/**
	 * <p>nowIsoDateTimeMilli.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public static final String nowIsoDateTimeMilli() {
		return FORMAT_ISO_DATE_TIME_MILLI.format(new Date());
	}

	/**
	 * <p>nowIsoDateNoTTime.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public static final String nowIsoDateNoTTime() {
		return FORMAT_ISO_DATE_NO_T_TIME.format(new Date());
	}
	
	private static DecimalFormat FORMAT_4D = new DecimalFormat("0.0000");
	private static DecimalFormat FORMAT_3D = new DecimalFormat("0.000");
	private static DecimalFormat FORMAT_2D = new DecimalFormat("0.00");
	private static DecimalFormat FORMAT_1D = new DecimalFormat("0.0");
	private static DecimalFormat FORMAT_4E = new DecimalFormat("0.0000E0");
	private static DecimalFormat FORMAT_3E = new DecimalFormat("0.000E0");
	private static DecimalFormat FORMAT_2E = new DecimalFormat("0.00E0");
	private static DecimalFormat FORMAT_1E = new DecimalFormat("0.0E0");
	private static SimpleDateFormat FORMAT_dMHmsS = new SimpleDateFormat("(dd:MM_HH:mm:ss.SSS)");

	/**
	 * <p>format4E.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format4E(double d) {
		return FORMAT_4E.format(d);
	}

	/**
	 * <p>format3E.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format3E(double d) {
		return FORMAT_3E.format(d);
	}

	/**
	 * <p>format2E.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format2E(double d) {
		return FORMAT_2E.format(d);
	}

	/**
	 * <p>format1E.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format1E(double d) {
		return FORMAT_1E.format(d);
	}
	
	/**
	 * <p>format3D.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format3D(double d) {
		return FORMAT_3D.format(d);
	}

	/**
	 * <p>format2D.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format2D(double d) {
		return FORMAT_2D.format(d);
	}

	/**
	 * <p>format1D.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format1D(double d) {
		return FORMAT_1D.format(d);
	}

	/**
	 * <p>format4D.</p>
	 *
	 * @param d a double
	 * @return a {@link java.lang.String} object
	 */
	public static String format4D(double d) {
		return FORMAT_4D.format(d);
	}

	/**
	 * <p>formatdMHmsS.</p>
	 *
	 * @param d a {@link java.util.Date} object
	 * @return a {@link java.lang.String} object
	 */
	public static String formatdMHmsS(Date d) {
		return FORMAT_dMHmsS.format(d);
	}
	
	/**
	 * <p>formatdMHmsS.</p>
	 *
	 * @param d a long
	 * @return a {@link java.lang.String} object
	 */
	public static String formatdMHmsS(long d) {
		return FORMAT_dMHmsS.format(new Date(d));
	}

	/**
	 * <p>formatdMHmsS.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public static String formatdMHmsS() {
		return FORMAT_dMHmsS.format(new Date());
	}


}


