package org.scictrl.csshell;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * This is timestamp object with nanosecond resolution. It holds two long values. One is with millisoecnd
 * resolution and represents Java standart UTC format. Second long value is with nanosecond resolution
 * and its absolute value is lower than 1ms or 1000000ns.
 *
 * @author igor@scictrl.com
 */
public final class Timestamp implements Comparable<Timestamp>
{
	private long milliseconds;
	private long nanoseconds;
	private final static SimpleDateFormat formatFull = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");
	private final static SimpleDateFormat formatDateTimeSeconds = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss");
	private final static SimpleDateFormat formatDateTime = new SimpleDateFormat(
    		"yyyy-MM-dd'T'HH:mm");
	private final static SimpleDateFormat formatDate = new SimpleDateFormat(
    		"yyyy-MM-dd");

	/**
	 * Date formatting constants.
	 */
    public enum Format
    {
        /** Format to ISO with "YYYY-MM-DD". */
        Date,
        
        /** Format to ISO with "YYYY-MM-DDTHH:MM". */
        DateTime,
        
        /** Format to ISO with "YYYY-MM-DDTHH:MM:SS". */
        DateTimeSeconds,
        
        /** Format to ISO with full precision "YYYY/MM/DD HH:MM:SS.000000000". */
        Full;
    }

	
	
	private final static long currentSecondInNano()
	{
		long l = System.nanoTime();

		return l - ((l / 1000000000) * 1000000000);
	}

	/**
	 * Default constructor, uses system time for initialization.
	 */
	public Timestamp()
	{
		this((System.currentTimeMillis() / 1000) * 1000, currentSecondInNano());
	}

	/**
	 * Creates timestamp representing provided values. If nanoseconds exceed 1000000 or -1000000 then they are
	 * truncated to nanoseconds within millisecond and millisecond is corrected.
	 *
	 * @param milli a long
	 * @param nano a long
	 */
	public Timestamp(long milli, long nano)
	{
		// correction if there is more nanoseconds than it fits in 
		if (nano >= 1000000) {
			long t = nano / 1000000;
			milliseconds = milli + t;
			nanoseconds = nano - t * 1000000;
		} else if (nano <= -1000000) {
			long t = nano / 1000000;
			milliseconds = milli + t - 1;
			nanoseconds = nano - t * 1000000 + 1000000;
		} else if (nano < 0) {
			milliseconds = milli - 1;
			nanoseconds = nano + 1000000;
		} else {
			milliseconds = milli;
			nanoseconds = nano;
		}
	}

	/**
	 * Returns time in milliseconds since epoch (standard Java UTC time, as returned by System.currentTimeMillis())
	 *
	 * @return Returns the milliseconds.
	 */
	public long getMilliseconds()
	{
		return milliseconds;
	}

	/**
	 * <p>Getter for the field <code>nanoseconds</code>.</p>
	 *
	 * @return Returns the nanoseconds within the millisecond.
	 */
	public long getNanoseconds()
	{
		return nanoseconds;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(T)
	 */
	/**
	 * <p>compareTo.</p>
	 *
	 * @param o a {@link org.scictrl.csshell.Timestamp} object
	 * @return a int
	 */
	public int compareTo(Timestamp o)
	{
		if (o instanceof Timestamp) {
			Timestamp t = (Timestamp)o;
			long d = milliseconds - t.milliseconds;

			if (d != 0) {
				return (int)d;
			}

			d = nanoseconds - t.nanoseconds;

			return (int)d;
		}

		return 0;
	}

	/**
	 * Returns time in nanoseconds since epoch. Not that this in only usefull for calculating
	 * time difference for up to 292 years (2<sup>63</sup> nanoseconds) since this is maximum time possible in
	 * nanoseconds due to long value range overflow.
	 *
	 * @return up to approx. 292 years big nano time
	 */
	public long toNanoTime()
	{
		return milliseconds * 1000000 + nanoseconds;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Timestamp) {
			Timestamp t = (Timestamp)obj;

			return t.milliseconds == milliseconds
			&& t.nanoseconds == nanoseconds;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	/** {@inheritDoc} */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(32);
		formatFull.format(new Date(milliseconds), sb,
		    new FieldPosition(DateFormat.FULL));

		if (nanoseconds < 100000) {
			sb.append('0');

			if (nanoseconds < 10000) {
				sb.append('0');

				if (nanoseconds < 1000) {
					sb.append('0');

					if (nanoseconds < 100) {
						sb.append('0');

						if (nanoseconds < 10) {
							sb.append('0');
						}
					}
				}
			}
		}
		
		sb.append(nanoseconds);

		return sb.toString();
	}

	/**
	 * <p>toString.</p>
	 *
	 * @return Returns timestamp as string formated as specified.
	 * @param format a {@link org.scictrl.csshell.Timestamp.Format} object
	 */
	public String toString(Format format)
	{
		StringBuffer sb = new StringBuffer(32);
		switch (format.ordinal()) {
		case 0:
			formatDate.format(new Date(milliseconds), sb,
			    new FieldPosition(DateFormat.FULL));
			return sb.toString();
		case 1:
			formatDateTime.format(new Date(milliseconds), sb,
			    new FieldPosition(DateFormat.FULL));
			return sb.toString();
		case 2:
			formatDateTimeSeconds.format(new Date(milliseconds), sb,
			    new FieldPosition(DateFormat.FULL));
			return sb.toString();
		default:
			return toString();
		}
	}

	
    /**
     * Get seconds since epoch, i.e. 1 January 1970 0:00 UTC.
     *
     * @return a long
     */
    public long getSeconds() {
    	return milliseconds/1000L;
    }
    
    /**
     * Converts timestamp to double.
     *
     * @return a double
     */
    public double toDouble() {
    	return (double)milliseconds/1000.0+(double)nanoseconds/1000000000.0;
    }

    /**
     * <p>isValid.</p>
     *
     * @return Returns <code>true</code> if time fields &gt; 0.
     */
    public boolean isValid() {
    	return milliseconds>0;
    }
    
    /**
     * <p>isGreaterThan.</p>
     *
     * @return Returns <code>true</code> if this time stamp is greater than
     *          the <code>other</code> time stamp.
     *  @param other Other time stamp
     */
    public boolean isGreaterThan(final Timestamp other) {
    	if (milliseconds<other.milliseconds) {
    		return false;
    	}
    	if (milliseconds==other.milliseconds) {
    		return nanoseconds>other.nanoseconds;
    	}
    	return true;
    }

    /**
     * <p>isGreaterOrEqual.</p>
     *
     * @return Returns <code>true</code> if this time stamp is greater than or
     *          equal to the <code>other</code> time stamp.
     *  @param other Other time stamp
     */
    public boolean isGreaterOrEqual(final Timestamp other) {
    	if (milliseconds<other.milliseconds) {
    		return false;
    	}
    	if (milliseconds==other.milliseconds) {
    		return nanoseconds>=other.nanoseconds;
    	}
    	return true;
    }

    /**
     * <p>isLessThan.</p>
     *
     * @return Returns <code>true</code> if this time stamp is less than
     *          the <code>other</code> time stamp.
     *  @param other Other time stamp
     */
    public boolean isLessThan(final Timestamp other) {
    	if (milliseconds>other.milliseconds) {
    		return false;
    	}
    	if (milliseconds==other.milliseconds) {
    		return nanoseconds<other.nanoseconds;
    	}
    	return true;
    }

    /**
     * <p>isLessOrEqual.</p>
     *
     * @return Returns <code>true</code> if this time stamp is less than or
     *          equal to the <code>other</code> time stamp.
     *  @param other Other time stamp
     */
    public boolean isLessOrEqual(final Timestamp other) {
    	if (milliseconds>other.milliseconds) {
    		return false;
    	}
    	if (milliseconds==other.milliseconds) {
    		return nanoseconds<=other.nanoseconds;
    	}
    	return true;
    }


}

/* __oOo__ */
