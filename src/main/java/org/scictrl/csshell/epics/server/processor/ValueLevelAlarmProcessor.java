/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ConfigurationManager;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ValueLevelAlarmProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueLevelAlarmProcessor extends DefaultAlarmProcessor {

	private Severity defaultSeverity;
	private Status defaultStatus;

	private int inDeadband = 0;

	private ValueLinks input;

	private Severity lastValueSeverity;

	private Status lastValuetStatus;

	private double lowerAlarmLimit;

	private Severity lowerAlarmSeverity;

	private Status lowerAlarmStatus;

	private double lowerWarningLimit;

	private Severity lowerWarningSeverity;

	private Status lowerWarningStatus;

	private double precision;

	private double upperAlarmLimit;

	private Severity upperAlarmSeverity;

	private Status upperAlarmStatus;

	private double upperWarningLimit;

	private Severity upperWarningSeverity;
	private Status upperWarningStatus;
	private boolean skipInvalid;
	private boolean noAlarmForward;
	
	private boolean enabled=true;
	
	/**
	 * <p>Constructor for ValueLevelAlarmProcessor.</p>
	 */
	public ValueLevelAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();

		input.activate(record.getDatabase());
	}

	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {

		super.configure(record, config);

		String[] names = config.getStringArray("input.links");

		if (names != null && names.length > 0) {
			this.input = new ValueLinks(record.getName(), names, this, Record.PROPERTY_VALUE);
			update(true, Severity.INVALID_ALARM, Status.UDF_ALARM, false);
		} else {
			_setValue(false, null,null,false, true);
		}

		skipInvalid= config.getBoolean("skipInvalid",false);
		noAlarmForward= config.getBoolean("noAlarmForward",false);

		precision = config.getDouble("precision",0.001);

		defaultSeverity = ConfigurationManager.getSeverity(config, "default", Severity.NO_ALARM);
		defaultStatus = ConfigurationManager.getStatus(config, "default", Status.NO_ALARM);

		upperWarningLimit = config
				.getDouble("upperWarning.limit", Double.MAX_VALUE);
		upperWarningSeverity = ConfigurationManager.getSeverity(config, "upperWarning",
				Severity.MINOR_ALARM);
		upperWarningStatus = ConfigurationManager.getStatus(config, "upperWarning",
				Status.HIGH_ALARM);

		lowerWarningLimit = config
				.getDouble("lowerWarning.limit", -Double.MAX_VALUE);
		lowerWarningSeverity = ConfigurationManager.getSeverity(config, "lowerWarning",
				Severity.MINOR_ALARM);
		lowerWarningStatus = ConfigurationManager.getStatus(config, "lowerWarning",
				Status.LOW_ALARM);

		upperAlarmLimit = config.getDouble("upperAlarm.limit", Double.MAX_VALUE);
		upperAlarmSeverity = ConfigurationManager.getSeverity(config, "upperAlarm",
				Severity.MAJOR_ALARM);
		upperAlarmStatus = ConfigurationManager.getStatus(config, "upperAlarm", Status.HIHI_ALARM);

		lowerAlarmLimit = config.getDouble("lowerAlarm.limit", -Double.MAX_VALUE);
		lowerAlarmSeverity = ConfigurationManager.getSeverity(config, "lowerAlarm",
				Severity.MAJOR_ALARM);
		lowerAlarmStatus = ConfigurationManager.getStatus(config, "lowerAlarm", Status.LOLO_ALARM);

	}

	/**
	 * <p>Getter for the field <code>defaultSeverity</code>.</p>
	 *
	 * @return the defaultSeverity
	 */
	public Severity getDefaultSeverity() {
		return defaultSeverity;
	}

	/**
	 * <p>Getter for the field <code>defaultStatus</code>.</p>
	 *
	 * @return the defaultStatus
	 */
	public Status getDefaultStatus() {
		return defaultStatus;
	}

	/**
	 * <p>Getter for the field <code>lowerAlarmLimit</code>.</p>
	 *
	 * @return the lowerAlarmLimit
	 */
	public double getLowerAlarmLimit() {
		return lowerAlarmLimit;
	}

	/**
	 * <p>Getter for the field <code>lowerAlarmSeverity</code>.</p>
	 *
	 * @return the lowerAlarmSeverity
	 */
	public Severity getLowerAlarmSeverity() {
		return lowerAlarmSeverity;
	}

	/**
	 * <p>Getter for the field <code>lowerAlarmStatus</code>.</p>
	 *
	 * @return the lowerAlarmStatus
	 */
	public Status getLowerAlarmStatus() {
		return lowerAlarmStatus;
	}

	/**
	 * <p>Getter for the field <code>lowerWarningLimit</code>.</p>
	 *
	 * @return the lowerWarningLimit
	 */
	public double getLowerWarningLimit() {
		return lowerWarningLimit;
	}

	/**
	 * <p>Getter for the field <code>lowerWarningSeverity</code>.</p>
	 *
	 * @return the lowerWarningSeverity
	 */
	public Severity getLowerWarningSeverity() {
		return lowerWarningSeverity;
	}

	/**
	 * <p>Getter for the field <code>lowerWarningStatus</code>.</p>
	 *
	 * @return the lowerWarningStatus
	 */
	public Status getLowerWarningStatus() {
		return lowerWarningStatus;
	}

	/**
	 * <p>Getter for the field <code>precision</code>.</p>
	 *
	 * @return the precision
	 */
	public double getPrecision() {
		return precision;
	}
	
	/**
	 * <p>Setter for the field <code>precision</code>.</p>
	 *
	 * @param precision a double
	 */
	public void setPrecision(double precision) {
		this.precision = precision;
	}

	/**
	 * <p>Getter for the field <code>upperAlarmLimit</code>.</p>
	 *
	 * @return the upperAlarmLimit
	 */
	public double getUpperAlarmLimit() {
		return upperAlarmLimit;
	}

	/**
	 * <p>Getter for the field <code>upperAlarmSeverity</code>.</p>
	 *
	 * @return the upperAlarmSeverity
	 */
	public Severity getUpperAlarmSeverity() {
		return upperAlarmSeverity;
	}

	/**
	 * <p>Getter for the field <code>upperAlarmStatus</code>.</p>
	 *
	 * @return the upperAlarmStatus
	 */
	public Status getUpperAlarmStatus() {
		return upperAlarmStatus;
	}

	/**
	 * <p>Getter for the field <code>upperWarningLimit</code>.</p>
	 *
	 * @return the upperWarningLimit
	 */
	public double getUpperWarningLimit() {
		return upperWarningLimit;
	}

	/**
	 * <p>Getter for the field <code>upperWarningSeverity</code>.</p>
	 *
	 * @return the upperWarningSeverity
	 */
	public Severity getUpperWarningSeverity() {
		return upperWarningSeverity;
	}

	/**
	 * <p>Getter for the field <code>upperWarningStatus</code>.</p>
	 *
	 * @return the upperWarningStatus
	 */
	public Status getUpperWarningStatus() {
		return upperWarningStatus;
	}
	
	/**
	 * <p>isEnabled.</p>
	 *
	 * @return a boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Sets this processor to be enabled. If false, it will always be alarm false and on default severity and status.
	 *
	 * @param enabled a boolean
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		updateAlarmStatus();
	}

	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);

		if (evt.getSource() == input) {
			updateAlarmStatus();
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void updateAlarmStatus() {
		super.updateAlarmStatus();
		
		if (!enabled) {
			update(false, defaultSeverity, defaultStatus, true);
			return;
		}

		Severity newSeverity;
		Status newStatus;

		if (input.isInvalid()) {
			newSeverity = Severity.INVALID_ALARM;
			newStatus = Status.LINK_ALARM;
			update(true, newSeverity, newStatus, true);
			return;
		}
		if (!input.isReady()) {
			return;
		}

		ValueHolder[] vh = input.consume();

		newSeverity = defaultSeverity;
		newStatus = defaultStatus;

		for (int i = 0; i < vh.length; i++) {
			if (newSeverity.isLessThan(vh[i].severity)) {
				newSeverity = vh[i].severity;
				newStatus = vh[i].status;
			}
		}
		
		if (skipInvalid && newSeverity.isGreaterThanOrEqual(Severity.INVALID_ALARM)) {
			return;
		}
		
		Severity valueSeverity = defaultSeverity;
		Status valueStatus = defaultStatus;

		for (int i = 0; i < vh.length; i++) {
			double d = vh[i].doubleValue();

			Severity se = lastValueSeverity != null ? lastValueSeverity : defaultSeverity;
			Status st = lastValuetStatus != null ? lastValuetStatus : defaultStatus;

			// check the deadbands first
			// if prvious turn value was in deadband and it is still, then
			// no change

			if ((inDeadband != 0)
					&& ((inDeadband == -2 && Math.abs(d - lowerAlarmLimit) < precision)
							|| (inDeadband == -1 && Math.abs(d - lowerWarningLimit) < precision)
							|| (inDeadband == 2 && Math.abs(d - upperAlarmLimit) < precision) 
							|| (inDeadband == 1 && Math.abs(d - upperWarningLimit) < precision))) {
				valueSeverity = se;
				valueStatus = st;
				continue;
			}
			// check levels
			if (d <= lowerAlarmLimit) {
				se = lowerAlarmSeverity;
				st = lowerAlarmStatus;
			} else if (d <= lowerWarningLimit) {
				se = lowerWarningSeverity;
				st = lowerWarningStatus;
			} else if (d >= upperAlarmLimit) {
				se = upperAlarmSeverity;
				st = upperAlarmStatus;
			} else if (d >= upperWarningLimit) {
				se = upperWarningSeverity;
				st = upperWarningStatus;
			} else {
				se = defaultSeverity;
				st = defaultStatus;
			}
			// check if in deadbend
			if (Math.abs(d - lowerAlarmLimit) < precision) {
				inDeadband = -2;
			} else if (Math.abs(d - lowerWarningLimit) < precision) {
				inDeadband = -1;
			} else if (Math.abs(d - upperAlarmLimit) < precision) {
				inDeadband = 2;
			} else if (Math.abs(d - upperWarningLimit) < precision) {
				inDeadband = 1;
			} else {
				inDeadband=0;
			}

			if (valueSeverity.isLessThan(se)) {
				valueSeverity = se;
				valueStatus = st;
			}
		}

		lastValuetStatus = valueStatus;
		lastValueSeverity = valueSeverity;

		if (noAlarmForward || newSeverity.isLessThan(valueSeverity)) {
			newSeverity = valueSeverity;
			newStatus = valueStatus;
		}

		update(newSeverity != Severity.NO_ALARM, newSeverity, newStatus, true);
	}

	/**
	 * <p>Setter for the field <code>lowerAlarmLimit</code>.</p>
	 *
	 * @param lowerAlarmLimit a double
	 */
	public void setLowerAlarmLimit(double lowerAlarmLimit) {
		this.lowerAlarmLimit = lowerAlarmLimit;
		updateAlarmStatus();
	}

	/**
	 * <p>Setter for the field <code>lowerWarningLimit</code>.</p>
	 *
	 * @param lowerWarningLimit a double
	 */
	public void setLowerWarningLimit(double lowerWarningLimit) {
		this.lowerWarningLimit = lowerWarningLimit;
		updateAlarmStatus();
	}

	/**
	 * <p>Setter for the field <code>upperAlarmLimit</code>.</p>
	 *
	 * @param upperAlarmLimit a double
	 */
	public void setUpperAlarmLimit(double upperAlarmLimit) {
		this.upperAlarmLimit = upperAlarmLimit;
		updateAlarmStatus();
	}

	/**
	 * <p>Setter for the field <code>upperWarningLimit</code>.</p>
	 *
	 * @param upperWarningLimit a double
	 */
	public void setUpperWarningLimit(double upperWarningLimit) {
		this.upperWarningLimit = upperWarningLimit;
		updateAlarmStatus();
	}

}
