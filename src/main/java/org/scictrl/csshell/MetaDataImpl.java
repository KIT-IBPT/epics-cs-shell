package org.scictrl.csshell;


/**
 *
 * <code>MetaDataImpl</code> is a default implementation of the {@link org.scictrl.csshell.MetaData}
 * interface, which receives all data through a constructor and returns it
 * through the interface methods.
 *
 * @author igor@scictrl.com
 */
public final class MetaDataImpl implements MetaData {

	private final String name;
	private final String description;
	private final double minimum;
	private final double maximum;
	private final double displayMin;
	private final double displayMax;
	private final double warningMin;
	private final double warningMax;
	private final double alarmMin;
	private final double alarmMax; 
	private final String[] enumDescriptions;
	private final Object[] enumValues;
	private final String format;
	private final String units;
	private final int precision;
	private final DataType dataType;
	private final Class<?> remoteDataType;
	private final boolean readAccess;
	private final boolean writeAccess;
	private final String hostname;
	private final int sequenceLength;
	private final boolean valid;
	
	/**
	 * <p>Constructor for MetaDataImpl.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param minimum a {@link java.lang.Number} object
	 * @param maximum a {@link java.lang.Number} object
	 * @param displayMin a {@link java.lang.Number} object
	 * @param displayMax a {@link java.lang.Number} object
	 * @param warningMin a {@link java.lang.Number} object
	 * @param warningMax a {@link java.lang.Number} object
	 * @param alarmMin a {@link java.lang.Number} object
	 * @param alarmMax a {@link java.lang.Number} object
	 * @param enumDescriptions an array of {@link java.lang.String} objects
	 * @param enumValues an array of {@link java.lang.Object} objects
	 * @param format a {@link java.lang.String} object
	 * @param units a {@link java.lang.String} object
	 * @param sequenceLength a {@link java.lang.Integer} object
	 * @param precision a {@link java.lang.Integer} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @param remoteDataType a {@link java.lang.Class} object
	 * @param readAccess a {@link java.lang.Boolean} object
	 * @param writeAccess a {@link java.lang.Boolean} object
	 * @param hostname a {@link java.lang.String} object
	 * @param valid a boolean
	 */
	public MetaDataImpl(String name, String description,
			Number minimum, Number maximum,
			Number displayMin, Number displayMax, 
			Number warningMin, Number warningMax, 
			Number alarmMin, Number alarmMax, 
			String[] enumDescriptions, Object[] enumValues,	String format, String units,
			Integer sequenceLength, Integer precision, DataType dataType, Class<?> remoteDataType,
			Boolean readAccess, Boolean writeAccess, String hostname, boolean valid) {
		this.name = name;
		this.description = description;
		this.minimum = minimum != null ? minimum.doubleValue() : Double.NaN;
		this.maximum = maximum != null ? maximum.doubleValue() : Double.NaN;
		this.displayMin = displayMin != null ? displayMin.doubleValue() : Double.NaN;
		this.displayMax = displayMax != null ? displayMax.doubleValue() : Double.NaN;
		this.warningMin = warningMin != null ? warningMin.doubleValue() : Double.NaN;
		this.warningMax = warningMax != null ? warningMax.doubleValue() : Double.NaN;
		this.alarmMin = alarmMin != null ? alarmMin.doubleValue() : Double.NaN;
		this.alarmMax = alarmMax != null ? alarmMax.doubleValue() : Double.NaN;
		this.enumDescriptions = enumDescriptions;
		this.enumValues = enumValues;
		this.format = format;
		this.units = units;
		this.precision = precision != null ? precision : 0;
		this.dataType = dataType;
		this.remoteDataType= remoteDataType;
		this.readAccess= readAccess !=null ? readAccess : false;
		this.writeAccess= writeAccess !=null ? writeAccess : false;
		this.hostname = hostname;
		this.sequenceLength = sequenceLength != null ? sequenceLength : 1;
		this.valid=valid;
	}
	
	/**
	 * <p>Constructor for MetaDataImpl.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param minimum a {@link java.lang.Number} object
	 * @param maximum a {@link java.lang.Number} object
	 * @param displayMin a {@link java.lang.Number} object
	 * @param displayMax a {@link java.lang.Number} object
	 * @param warningMin a {@link java.lang.Number} object
	 * @param warningMax a {@link java.lang.Number} object
	 * @param alarmMin a {@link java.lang.Number} object
	 * @param alarmMax a {@link java.lang.Number} object
	 * @param enumDescriptions an array of {@link java.lang.String} objects
	 * @param enumValues an array of {@link java.lang.Object} objects
	 * @param format a {@link java.lang.String} object
	 * @param units a {@link java.lang.String} object
	 * @param sequenceLength a {@link java.lang.Integer} object
	 * @param precision a {@link java.lang.Integer} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @param remoteDataType a {@link java.lang.Class} object
	 * @param readAccess a {@link java.lang.Boolean} object
	 * @param writeAccess a {@link java.lang.Boolean} object
	 * @param hostname a {@link java.lang.String} object
	 */
	public MetaDataImpl(String name, String description, Number minimum,
			Number maximum, Number displayMin, Number displayMax,
			Number warningMin, Number warningMax, Number alarmMin,
			Number alarmMax, String[] enumDescriptions, Object[] enumValues,
			String format, String units, Integer sequenceLength,
			Integer precision, DataType dataType, Class<?> remoteDataType,
			Boolean readAccess, Boolean writeAccess, String hostname) {
		this(name, description, minimum, maximum, displayMin, displayMax,
				warningMin, warningMax, alarmMin, alarmMax, enumDescriptions,
				enumValues, format, units, sequenceLength, precision, dataType,
				remoteDataType, readAccess, writeAccess, hostname, true);
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isReadAccess() {
		return readAccess;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isWriteAccess() {
		return writeAccess;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isValid() {
		return valid;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getAlarmHigh()
	 */
	/** {@inheritDoc} */
	@Override
	public double getAlarmMax() {
		return alarmMax;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getAlarmLow()
	 */
	/** {@inheritDoc} */
	@Override
	public double getAlarmMin() {
		return alarmMin;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getDataType()
	 */
	/** {@inheritDoc} */
	@Override
	public DataType getDataType() {
		return dataType;
	}
	
	/** {@inheritDoc} */
	@Override
	public Class<?> getRemoteDataType() {
		return remoteDataType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getDescription()
	 */
	/** {@inheritDoc} */
	@Override
	public String getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getDisplayHigh()
	 */
	/** {@inheritDoc} */
	@Override
	public double getDisplayMax() {
		return displayMax;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getDisplayLow()
	 */
	/** {@inheritDoc} */
	@Override
	public double getDisplayMin() {
		return displayMin;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getState(int)
	 */
	/** {@inheritDoc} */
	@Override
	public String getState(int index) {
		if (enumDescriptions == null) return null;
		return enumDescriptions[index];
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getStates()
	 */
	/** {@inheritDoc} */
	@Override
	public String[] getStates() {
		if (enumDescriptions == null) return null;
		String[] s = new String[enumDescriptions.length];
		System.arraycopy(enumDescriptions, 0, s, 0, s.length);
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getStateValue(int)
	 */
	/** {@inheritDoc} */
	@Override
	public Object getStateValue(int index) {
		if (enumValues == null) return null;
		return enumValues[index];
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getStateValues()
	 */
	/** {@inheritDoc} */
	@Override
	public Object[] getStateValues() {
		if (enumValues == null) return null;
		String[] s = new String[enumValues.length];
		System.arraycopy(enumValues, 0, s, 0, s.length);
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getFormat()
	 */
	/** {@inheritDoc} */
	@Override
	public String getFormat() {
		return format;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getHostname()
	 */
	/** {@inheritDoc} */
	@Override
	public String getHostname() {
		return hostname;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getName()
	 */
	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getPrecision()
	 */
	/** {@inheritDoc} */
	@Override
	public int getPrecision() {
		return precision;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getUnits()
	 */
	/** {@inheritDoc} */
	@Override
	public String getUnits() {
		return units;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getWarnHigh()
	 */
	/** {@inheritDoc} */
	@Override
	public double getWarnMax() {
		return warningMax;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getWarnLow()
	 */
	/** {@inheritDoc} */
	@Override
	public double getWarnMin() {
		return warningMin;
	}
	
	/** {@inheritDoc} */
	@Override
	public double getMaximum() {
		return maximum;
	}
	
	/** {@inheritDoc} */
	@Override
	public double getMinimum() {
		return minimum;
	}

	/*
	 * (non-Javadoc)
	 * @see org.epics.css.dal.simple.MetaData#getSequenceLength()
	 */
	/** {@inheritDoc} */
	@Override
	public int getSequenceLength() {
		return sequenceLength;
	}
	
	/**
	 * <p>createUninitializedMetaData.</p>
	 *
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public static MetaData createUninitializedMetaData() {
		return new MetaDataImpl(null, null, null, null, null, null, null, null, null, null, 
				null, null,	null, null, null, null, null, null, false,false, null, false);
	}
	
	/**
	 * <p>deriveMetaData.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param minimum a {@link java.lang.Number} object
	 * @param maximum a {@link java.lang.Number} object
	 * @param displayMin a {@link java.lang.Number} object
	 * @param displayMax a {@link java.lang.Number} object
	 * @param warningMin a {@link java.lang.Number} object
	 * @param warningMax a {@link java.lang.Number} object
	 * @param alarmMin a {@link java.lang.Number} object
	 * @param alarmMax a {@link java.lang.Number} object
	 * @param enumDescriptions an array of {@link java.lang.String} objects
	 * @param enumValues an array of {@link java.lang.Object} objects
	 * @param format a {@link java.lang.String} object
	 * @param units a {@link java.lang.String} object
	 * @param sequenceLength a {@link java.lang.Integer} object
	 * @param precision a {@link java.lang.Integer} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @param remoteDataType a {@link java.lang.Class} object
	 * @param readAccess a {@link java.lang.Boolean} object
	 * @param writeAccess a {@link java.lang.Boolean} object
	 * @param hostname a {@link java.lang.String} object
	 * @param valid a {@link java.lang.Boolean} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData deriveMetaData(String name, String description,
			Number minimum, Number maximum,
			Number displayMin, Number displayMax, 
			Number warningMin, Number warningMax, 
			Number alarmMin, Number alarmMax, 
			String[] enumDescriptions, Object[] enumValues,	String format, String units,
			Integer sequenceLength, Integer precision, DataType dataType, Class<?> remoteDataType,
			Boolean readAccess, Boolean writeAccess, String hostname, Boolean valid) {
		
		return new MetaDataImpl(
				name==null ? this.name : name, 
				description==null ? this.description : description, 
				minimum == null ? this.minimum : minimum, 
				maximum == null ? this.maximum : maximum, 
				displayMin == null ? this.displayMin : displayMin, 
				displayMax == null ? this.displayMax : displayMax, 
				warningMin == null ? this.warningMin : warningMin, 
				warningMax == null ? this.warningMax : warningMax, 
				alarmMin == null ? this.alarmMin : alarmMin, 
				alarmMax == null ? this.alarmMax : alarmMax, 
				enumDescriptions == null ? this.enumDescriptions : enumDescriptions, 
				enumValues == null ? this.enumValues : enumValues,	
				format == null ? this.format : format, 
				units == null ? this.units : units, 
				sequenceLength == null ? this.sequenceLength : sequenceLength, 
				precision == null ? this.precision : precision, 
				dataType == null ? this.dataType : dataType, 
				remoteDataType == null ? this.remoteDataType : remoteDataType, 
				readAccess == null ? this.readAccess : readAccess, 
				writeAccess == null ? this.writeAccess : writeAccess, 
				hostname == null ? this.hostname : hostname,
				valid == null ? this.valid : valid);
	}
}
