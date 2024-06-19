/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Counts time when control PV is on in seconds.
 *
 * @author igor@scictrl.com
 */
public class TimeCounterProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	private boolean accumulative;
	private ValueLinks link;
	private long timestamp=0;

	/**
	 * <p>Constructor for TimeCounterProcessor.</p>
	 */
	public TimeCounterProcessor() {
		super();
		type=DBRType.INT;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		String pv= config.getString("link");
		
		link= new ValueLinks(pv,new String[] {pv},this,Record.PROPERTY_VALUE);
		
		accumulative= config.getBoolean("accumulative", false);
		
		record.setPersistent(accumulative);
		
		if (trigger==0) {
			trigger=1000;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		link.activate(record.getDatabase());
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		update();
	}
	
	private void update() {
		if (!link.isReady()) {
			record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM);
			return;
		}
		if (link.consume()[0].longValue()!=0) {
			long t= System.currentTimeMillis();
			if (timestamp!=0) {
				long l= EPICSUtilities.toLong(value);
				//long l1=l;
				l= l+Math.round((t-timestamp)/1000.0);
				//System.out.println(getName()+" "+l1+" "+l+" "+(t-timestamp)+" "+link.getLastSeverity());
				_setValue(l,link.getLastSeverity(), link.getLastStatus(), true);
			} else {
				record.updateAlarm(link.getLastSeverity(), link.getLastStatus(), true);
			}
			timestamp=t;
		} else {
			timestamp=0;
			//System.out.println(getName()+" "+accumulative);
			if (!accumulative) {
				//System.out.println(getName()+" 0"+" "+link.getLastSeverity());
				_setValue(0L,link.getLastSeverity(), link.getLastStatus(), true);
			} else {
				record.updateAlarm(link.getLastSeverity(), link.getLastStatus(), true);
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		update();
	}

}
