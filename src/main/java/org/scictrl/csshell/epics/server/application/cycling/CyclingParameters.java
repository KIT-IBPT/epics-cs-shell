package org.scictrl.csshell.epics.server.application.cycling;

/**
 * This type was created in VisualAge.
 *
 * @author igor@scictrl.com
 */
public final class CyclingParameters implements Cloneable {
	
	/** Constant <code>NO_CYCLES="NoCycles"</code> */
	public static final String NO_CYCLES = "NoCycles";
	/** Constant <code>STEPS_PER_RAMP="StepsPerRamp"</code> */
	public static final String STEPS_PER_RAMP = "StepsPerRamp";
	/** Constant <code>WAIT_BETWEEN_STEPS="WaitBetweenSteps"</code> */
	public static final String WAIT_BETWEEN_STEPS = "WaitBetweenSteps";
	/** Constant <code>WAIT_AT_LIMITS="WaitAtLimits"</code> */
	public static final String WAIT_AT_LIMITS = "WaitAtLimits";
	/** Constant <code>FINAL_VALUE="FinalValue"</code> */
	public static final String FINAL_VALUE = "FinalValue";
	/** Constant <code>STARTING_AS_FINAL="StartingAsFinal"</code> */
	public static final String STARTING_AS_FINAL = "StartingAsFinal";
	/** Constant <code>CYCLE_DECREMENT="CycleDecrement"</code> */
	public static final String CYCLE_DECREMENT = "CycleDecrement";
	/** Constant <code>USE_DEVICE_LIMITS="UseDeviceLimits"</code> */
	public static final String USE_DEVICE_LIMITS = "UseDeviceLimits";
	/** Constant <code>MAX_LIMIT="MaxLimit"</code> */
	public static final String MAX_LIMIT = "MaxLimit";
	/** Constant <code>MIN_LIMIT="MinLimit"</code> */
	public static final String MIN_LIMIT = "MinLimit";	
	/** Constant <code>TOP_SCALE="TopScale"</code> */
	public static final String TOP_SCALE = "TopScale";	

	
	private int noCycles = 0;
	private int stepsPerRamp = 10;
	private double waitBetweenSteps = 1.0;
	private double waitAtLimits = 30.0;
	private double maxLimit = 0.0;
	private double minLimit = 0.0;	
	private double finalValue = 0.0;	
	private boolean startingAsFinal = false;	
	private double cycleDecrement = 0.0;
	private boolean useDeviceLimits = true;
	private double topScale = 0.05;
	
	/**
	 * DefaultDataValues constructor comment.
	 */
	public CyclingParameters() {
	
	}

	/**
	 * Constructor to include max/min values
	 *
	 * @param noCycles a int
	 * @param stepsPerRamp a int
	 * @param waitBetweenSteps a double
	 * @param waitAtLimits a double
	 * @param maxLimit a double
	 * @param minLimit a double
	 * @param finalValue a double
	 * @param cycleDecrement a double
	 * @param useDeviceLimits a boolean
	 * @param startingAsFinal a boolean
	 * @param topScale a double
	 */
	public CyclingParameters(int noCycles,int stepsPerRamp, double waitBetweenSteps, double waitAtLimits, double maxLimit, double minLimit, double finalValue, double cycleDecrement, boolean useDeviceLimits, boolean startingAsFinal, double topScale) {
		this.noCycles = noCycles;
		this.stepsPerRamp = stepsPerRamp;
		this.waitBetweenSteps = waitBetweenSteps;
		this.waitAtLimits = waitAtLimits;
		this.maxLimit = maxLimit;
		this.minLimit = minLimit;
		this.finalValue = finalValue;
		this.cycleDecrement = cycleDecrement;
		this.useDeviceLimits = useDeviceLimits;
		this.startingAsFinal = startingAsFinal;
		this.topScale=topScale;
	}
	/**
	 * DefaultDataValues constructor comment.
	 *
	 * @param parameters a {@link java.lang.String} object
	 * @throws java.lang.NumberFormatException if any.
	 */
	public CyclingParameters(String parameters) throws NumberFormatException {
		decode(parameters);
	}
	
	/**
	 * <p>withParameter.</p>
	 *
	 * @param param a {@link java.lang.String} object
	 * @param value a {@link java.lang.Number} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 */
	public CyclingParameters withParameter(String param, Number value) {
		try {
			CyclingParameters c= (CyclingParameters) this.clone();
			if (param==CYCLE_DECREMENT) {
				c.cycleDecrement=value.doubleValue();
			} else if (param==FINAL_VALUE) {
				c.finalValue=value.doubleValue();
			} else if (param==MAX_LIMIT) {
				c.maxLimit=value.doubleValue();
			} else if (param==MIN_LIMIT) {
				c.minLimit=value.doubleValue();
			} else if (param==NO_CYCLES) {
				c.noCycles=value.intValue();
			} else if (param==STEPS_PER_RAMP) {
				c.stepsPerRamp=value.intValue();
			} else if (param==USE_DEVICE_LIMITS) {
				c.useDeviceLimits=value.intValue()>0;
			} else if (param==WAIT_AT_LIMITS) {
				c.waitAtLimits=value.doubleValue();
			} else if (param==WAIT_BETWEEN_STEPS) {
				c.waitBetweenSteps=value.doubleValue();
			} else if (param==STARTING_AS_FINAL) {
				c.startingAsFinal=value.intValue()>0;
			} else if (param==TOP_SCALE) {
				c.topScale=value.doubleValue();
			}
			return c;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		// Never ever
		return null;
	}
	
	/**
	 * Insert the method's description here.
	 * Creation date: (10.11.2001 22:00:33)
	 * @param param java.lang.String
	 */
	private void decode(String param) throws NumberFormatException {
		java.util.StringTokenizer st= new java.util.StringTokenizer(param," \0\t;");
	
		if (st.hasMoreTokens())
			this.noCycles = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
			this.stepsPerRamp = Integer.parseInt(st.nextToken());
		if (st.hasMoreTokens())
			this.waitBetweenSteps = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.waitAtLimits = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.maxLimit = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.minLimit = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.finalValue = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.cycleDecrement = Double.parseDouble(st.nextToken());
		if (st.hasMoreTokens())
			this.useDeviceLimits = Boolean.parseBoolean(st.nextToken());
		if (st.hasMoreTokens())
			this.startingAsFinal = Boolean.parseBoolean(st.nextToken());
		if (st.hasMoreTokens())
			this.topScale = Double.parseDouble(st.nextToken());
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (10.11.2001 22:10:13)
	 *
	 * @return java.lang.String
	 */
	public String toString() {
		StringBuffer sb= new StringBuffer();
		sb.append(getNoCycles());
		sb.append(';');
		sb.append(getStepsPerRamp());
		sb.append(';');
		sb.append(getWaitBetweenSteps());
		sb.append(';');
		sb.append(getWaitAtLimits());
		sb.append(';');
		sb.append(getMaxLimit());
		sb.append(';');
		sb.append(getMinLimit());
		sb.append(';');
		sb.append(getFinalValue());
		sb.append(';');
		sb.append(getCycleDecrement());
		sb.append(';');
		sb.append(isUseDeviceLimits());
		sb.append(';');
		sb.append(isStartingAsFinal());
		sb.append(';');
		sb.append(getTopScale());
	
	
		return sb.toString();
	}
	
	/**
	 * <p>Getter for the field <code>topScale</code>.</p>
	 *
	 * @return a double
	 */
	public double getTopScale() {
		return topScale;
	}

	/**
	 * <p>Getter for the field <code>cycleDecrement</code>.</p>
	 *
	 * @return the cycleDecrement
	 */
	public double getCycleDecrement() {
		return cycleDecrement;
	}

	/**
	 * <p>Getter for the field <code>finalValue</code>.</p>
	 *
	 * @return the finalValue
	 */
	public double getFinalValue() {
		return finalValue;
	}

	/**
	 * <p>Getter for the field <code>maxLimit</code>.</p>
	 *
	 * @return the maxLimit
	 */
	public double getMaxLimit() {
		return maxLimit;
	}

	/**
	 * <p>Getter for the field <code>minLimit</code>.</p>
	 *
	 * @return the minLimit
	 */
	public double getMinLimit() {
		return minLimit;
	}

	/**
	 * <p>Getter for the field <code>noCycles</code>.</p>
	 *
	 * @return the noCycles
	 */
	public int getNoCycles() {
		return noCycles;
	}

	/**
	 * <p>Getter for the field <code>stepsPerRamp</code>.</p>
	 *
	 * @return the stepsPerRamp
	 */
	public int getStepsPerRamp() {
		return stepsPerRamp;
	}

	/**
	 * <p>isUseDeviceLimits.</p>
	 *
	 * @return the useDeviceLimits
	 */
	public boolean isUseDeviceLimits() {
		return useDeviceLimits;
	}

	/**
	 * <p>Getter for the field <code>waitAtLimits</code>.</p>
	 *
	 * @return the waitAtLimits
	 */
	public double getWaitAtLimits() {
		return waitAtLimits;
	}

	/**
	 * <p>Getter for the field <code>waitBetweenSteps</code>.</p>
	 *
	 * @return the waitBetweenSteps
	 */
	public double getWaitBetweenSteps() {
		return waitBetweenSteps;
	}
	
	/**
	 * <p>isStartingAsFinal.</p>
	 *
	 * @return a boolean
	 */
	public boolean isStartingAsFinal() {
		return startingAsFinal;
	}
}
