package org.scictrl.csshell;


/**
 * Base interface for a sample's meta data.
 *
 * @author igor@scictrl.com
 */
public interface MetaData
{
	/** Constant <code>ALARM_MAX="AlarmMax"</code> */
	public String ALARM_MAX="AlarmMax";
	/** Constant <code>ALARM_MIN="AlarmMin"</code> */
	public String ALARM_MIN="AlarmMin";
	/** Constant <code>DATA_TYPE="DataType"</code> */
	public String DATA_TYPE="DataType";
	/** Constant <code>DESCRIPTION="Description"</code> */
	public String DESCRIPTION="Description";
	/** Constant <code>DISPLAY_MAX="DisplayMax"</code> */
	public String DISPLAY_MAX="DisplayMax";
	/** Constant <code>DISPLAY_MIN="DisplayMin"</code> */
	public String DISPLAY_MIN="DisplayMin";
	/** Constant <code>FORMAT="Format"</code> */
	public String FORMAT="Format";
	/** Constant <code>MAXIMUM="Maximum"</code> */
	public String MAXIMUM="Maximum";
	/** Constant <code>MINIMUM="Minimum"</code> */
	public String MINIMUM="Minimum";
	/** Constant <code>PRECISION="Precesion"</code> */
	public String PRECISION="Precesion";
	/** Constant <code>SEQUENCE_LENGTH="SequenceLength"</code> */
	public String SEQUENCE_LENGTH="SequenceLength";
	/** Constant <code>STATES="States"</code> */
	public String STATES="States";
	/** Constant <code>STATE_VALUES="StateValues"</code> */
	public String STATE_VALUES="StateValues";
	/** Constant <code>UNITS="Units"</code> */
	public String UNITS="Units";
	/** Constant <code>WARN_MAX="WarnMax"</code> */
	public String WARN_MAX="WarnMax";
	/** Constant <code>WARN_MIN="WarnMin"</code> */
	public String WARN_MIN="WarnMin";
	/** Constant <code>READ_ACCESS="ReadAccess"</code> */
	public String READ_ACCESS="ReadAccess";
	/** Constant <code>WRITE_ACCESS="WriteAccess"</code> */
	public String WRITE_ACCESS="WriteAccess";

	
    /**
     * <p>getMinimum.</p>
     *
     * @return Suggested lower control limit
     */
    public double getMinimum();

    /**
     * <p>getMaximum.</p>
     *
     * @return Suggested upper control limit
     */
    public double getMaximum();
    
    /**
     * <p>getDisplayMin.</p>
     *
     * @return Suggested lower display limit
     */
    public double getDisplayMin();

    /**
     * <p>getDisplayMax.</p>
     *
     * @return Suggested upper display limit
     */
    public double getDisplayMax();

    /**
     * <p>getWarnMin.</p>
     *
     * @return Low warning limit
     */
    public double getWarnMin();

    /**
     * <p>getWarnMax.</p>
     *
     * @return High warning limit
     */
    public double getWarnMax();

    /**
     * <p>getAlarmMin.</p>
     *
     * @return Low alarm limit
     */
    public double getAlarmMin();

    /**
     * <p>getAlarmMax.</p>
     *
     * @return High alarm limit
     */
    public double getAlarmMax();

    /**
     * <p>getPrecision.</p>
     *
     * @return Suggested display precision (fractional digits)
     */
    public int getPrecision();

    /**
     * <p>getUnits.</p>
     *
     * @return The engineering units string
     */
    public String getUnits();
    
    /**
     *  Obtains the states.
     *  <p>
     *  The array element <code>i</code> represents enum number <code>i</code>.
     *
     * @return an array of {@link java.lang.String} objects
     */
    public String[] getStates();

    /**
     *  Convenience routine for getting the state.
     *
     * @return a {@link java.lang.String} object
     * @param index a int
     */
    public String getState(int index);
    
    /**
     *  Obtains the enumeration values.
     *  <p>
     *  The array element <code>i</code> represents enum number <code>i</code>.
     *
     * @return an array of {@link java.lang.Object} objects
     */
    public Object[] getStateValues();

    /**
     *  Convenience routine for getting a state value.
     *
     * @return a {@link java.lang.Object} object
     * @param index a int
     */
    public Object getStateValue(int index);
    
    /**
     * Return the display format for the numerical values.
     *
     * @return the display format
     */
    public String getFormat();
    
    /**
     * Returns the access type.
     *
     * @return the access type
     */
    public boolean isReadAccess();
    
    /**
     * Returns the access type.
     *
     * @return the access type
     */
    public boolean isWriteAccess();

    /**
     * Returns <code>true</code> if contains valid data.
     *
     * @return <code>true</code> if contained data is valid
     */
    public boolean isValid();

    /**
     * Returns the host name of the channel that this meta data belongs to.
     *
     * @return the host name
     */
    public String getHostname();
    
    /**
     * Returns the datatype of the channel. This method returns one of the
     * string in {@link org.scictrl.csshell.DataType}.
     *
     * @return the datatype
     */
    public DataType getDataType();
    
    /**
     * Returns the description of the channel.
     *
     * @return the description
     */
    public String getDescription();
    
    /**
     * Returns the name of the channel.
     *
     * @return the name of the channel
     */
    public String getName();
 
    /**
     * Returns the sequence length
     *
     * @return the sequence length
     */
    public int getSequenceLength();
    /**
     * <p>getRemoteDataType.</p>
     *
     * @return a {@link java.lang.Class} object
     */
    public Class<?> getRemoteDataType();
}
