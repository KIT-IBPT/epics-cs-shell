package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.text.SimpleDateFormat;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ValueDiffAlarmProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueDiffAlarmProcessor extends DefaultAlarmProcessor {

	private static Logger log= LogManager.getLogger(ValueDiffAlarmProcessor.class);
	private static SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	private String device;
	private String getPV;
	private String setPV;
	private long timeWindow;
	private double valueWindow;
	private ValueLinks setLink;
	private ValueLinks getLink;
	private Long lastSet;
	private double precision;
	private Long lastReadPeak;
	private int lastReadCount;
	private String enablePV;
	private ValueLinks enableLink;

	/**
	 * <p>Constructor for ValueDiffAlarmProcessor.</p>
	 */
	public ValueDiffAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		device= config.getString("device");
		if (device == null) {
			log.error("Device name is missing!");
			throw new IllegalStateException("Device name is missing!");
		}

		getPV= config.getString("get_pv",device+":Current:Readback");
		setPV= config.getString("set_pv",device+":Current:Setpoint:Get");
		enablePV= config.getString("enable_pv");

		setLink= new ValueLinks(setPV,getName(), new String[]{setPV}, this, Record.PROPERTY_VALUE);
		getLink= new ValueLinks(getPV,getName(), new String[]{getPV}, this, Record.PROPERTY_VALUE);
		
		if (enablePV!=null && enablePV.length()>0) {
			enableLink= new ValueLinks(enablePV,getName(), new String[]{enablePV}, this, Record.PROPERTY_VALUE);
		}
		
		timeWindow= config.getLong("time_window", 10000L);
		valueWindow= config.getDouble("value_window", 0.001);
		precision= config.getDouble("precision", 0.001);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		setLink.activate(getRecord().getDatabase());
		getLink.activate(getRecord().getDatabase());
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void propertyChange(PropertyChangeEvent evt) {
		super.propertyChange(evt);
		
		if (setLink.equals(evt.getSource())) {
			if (setLink.isInvalid()) {
				update(true, Severity.INVALID_ALARM, Status.LINK_ALARM, true);
				return;
			}
			if (!setLink.isReady()) {
				return;
			}
			lastSet=Long.valueOf(System.currentTimeMillis());
			lastReadPeak=null;
			lastReadCount=0;
			if (getLink.isReady()) {
				checkValueDiff(setLink.consumeAsDoubles()[0],getLink.consumeAsDoubles()[0]);
			}
		} else if (getLink.equals(evt.getSource())) {
			if (getLink.isInvalid()) {
				update(true, Severity.INVALID_ALARM, Status.LINK_ALARM, true);
				return;
			}
			if (!getLink.isReady()) {
				return;
			}
			if (setLink.isReady()) {
				checkValueDiff(setLink.consumeAsDoubles()[0],getLink.consumeAsDoubles()[0]);
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void updateAlarmStatus() {
		super.updateAlarmStatus();
		
		if (setLink.isInvalid() || getLink.isInvalid()) {
			update(true, Severity.INVALID_ALARM, Status.LINK_ALARM, true);
			return;
		}

		if (!setLink.isReady() || !getLink.isReady()) {
			return;
		}
		
		lastSet=Long.valueOf(System.currentTimeMillis());
		lastReadPeak=null;
		lastReadCount=0;

		checkValueDiff(setLink.consumeAsDoubles()[0],getLink.consumeAsDoubles()[0]);
		
	}

	private void checkValueDiff(double set, double get) {
		//System.out.println(getValueAsBoolean()+", diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+(lastSet!=null?sdf.format(lastSet):"NULL")+", lastPeak="+(lastReadPeak!=null?sdf.format(lastReadPeak):"NULL")+" cound="+lastReadCount);
		
		if (enabled() && lastSet!=null && System.currentTimeMillis()-lastSet>=timeWindow) {
			// process if it outside setpoint time window
			
			if (getValueAsBoolean()) {
				// if it is in alarm 
				if (Math.abs(set-get)>valueWindow-precision) {
					// if it is in alarm and outside value window, then keep the alarm
					return;
				}
				// if within value window then reset alarm to 0
			} else {
				
				if (timeWindow==0) {
					// just simple diff, no time checking
					if (Math.abs(set-get)>valueWindow+precision) {
						// if it is outside value window, then switch on alarm
						lastReadCount++;
						log.debug("1, diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+sdf.format(lastSet)+" count="+lastReadCount);
						//System.out.println("1, diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+sdf.format(lastSet)+", lastPeak="+sdf.format(lastReadPeak)+" count="+lastReadCount+" "+sdf.format(new Date()));
						update(true, Severity.MAJOR_ALARM, Status.STATE_ALARM, true);
						return;
					}
				} else {
					// if it is not an alarm
					if (lastReadPeak!=null) {
						// if early readback event is active
						if (System.currentTimeMillis()-lastReadPeak>timeWindow/2.0) {
							// if it is outside half of setpoint time window, then it is outside early readback event
							// then it is not suppresses 
							if (Math.abs(set-get)>valueWindow+precision) {
								// if it is outside time and value window, then swithc on alarm
								lastReadCount++;
								log.debug("1, diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+sdf.format(lastSet)+", lastPeak="+sdf.format(lastReadPeak)+" count="+lastReadCount);
								//System.out.println("1, diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+sdf.format(lastSet)+", lastPeak="+sdf.format(lastReadPeak)+" count="+lastReadCount+" "+sdf.format(new Date()));
								update(true, Severity.MAJOR_ALARM, Status.STATE_ALARM, true);
								return;
							} else if (System.currentTimeMillis()-lastReadPeak>timeWindow*5.0 && lastReadCount==0) {
								// if it is outside five time of setpoint window and inside value window, and
								// there has not been an alarm since last set
								// then early readback event gets disarmed 
								lastReadPeak=null;
							}
						} 
					} else {
						// if early readback event is not active/armed
						if (Math.abs(set-get)>valueWindow+precision) {
							// if early readback event is not active and value is outside value window,
							// then activate early readback event and do nothing 
							lastReadPeak = Long.valueOf(System.currentTimeMillis());
						}
					}
				}
			}
		}
		//System.out.println("0, diff="+(set-get)+", limit="+(valueWindow+precision)+", set="+set+", get="+get+", lastSet="+(lastSet!=null?sdf.format(lastSet):"NULL")+", lastPeak="+(lastReadPeak!=null?sdf.format(lastReadPeak):"NULL")+" count="+lastReadCount+" "+sdf.format(new Date()));
		update(false, Severity.NO_ALARM, Status.NO_ALARM, true);
		
	}
	
	private boolean enabled() {
		if (enableLink==null) {
			return true;
		}
		
		if (enableLink.isInvalid() || !enableLink.isReady() || enableLink.isLastSeverityInvalid()) {
			return true;
		}
		return enableLink.consumeAsBooleanAnd();
	}

	/**
	 * <p>Getter for the field <code>timeWindow</code>.</p>
	 *
	 * @return a long
	 */
	public long getTimeWindow() {
		return timeWindow;
	}
	
	/**
	 * <p>Getter for the field <code>valueWindow</code>.</p>
	 *
	 * @return a double
	 */
	public double getValueWindow() {
		return valueWindow;
	}

}
