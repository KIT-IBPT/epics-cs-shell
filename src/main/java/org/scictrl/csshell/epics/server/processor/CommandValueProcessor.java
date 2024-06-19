/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * The Command Value Processor is intended for commands or actions, which are
 * triggered from client (e.g. CSS) by sending 1 or true to this processor.
 * Implementor should watch for write notification or change notification
 * where new value is 1.
 * Record will stay 1 until action is finished or timeout is expired.
 * Implementor should signal this or by calling {@link #pushDone()} or by setting
 * value to false (or 0). Setting timeout will automatically flip value to 0 after
 * timeout time has expired
 *
 * @author igor@scictrl.com
 */
public class CommandValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	/**
	 * <p>newProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param timeout a long
	 * @return a {@link org.scictrl.csshell.epics.server.processor.CommandValueProcessor} object
	 */
	public static final CommandValueProcessor newProcessor(String name, String description, long timeout) {
		
		Record r= new Record(name, DBRType.BYTE, 1);
		
		CommandValueProcessor p= new CommandValueProcessor();
		p.configure(r,new HierarchicalConfiguration());
		r.setProcessor(p);
		p._setValue(false, null, null, false, true);
		p.fixed=false;
		p.setTimeout(timeout);
		
		return p;

	}

	/**
	 * <p>After timeout period command value is returned to 0.</p>
	 */
	protected long timeout=0;
	private String linkPV;
	private ValueLinks link;
	private Double linkVal;
	
	
	/**
	 * <p>Constructor for CommandValueProcessor.</p>
	 */
	public CommandValueProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		
		if (record.getType()!= DBRType.BYTE) {
			throw new IllegalArgumentException("Record '"+record.getName()+"' type is '"+record.getType()+"', only BYTE is allowed for this processor.");
		}
		
		super.configure(record, config);
		
		String ln= config.getString("out.link");
		
		if (ln!=null && ln.length()>0) {
			this.linkPV= ln;
			this.link= new ValueLinks(ln, ln, this, Record.PROPERTY_VALUE);
			
			if (config.containsKey("out.value")) {
				this.linkVal= config.getDouble("out.value");
			} else {
				this.linkVal=null;
			}
			
		} else {
			this.linkPV= null;
			this.linkVal=null;
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (link!=null) {
			link.activate(record.getDatabase());
		}
	}
	
	/**
	 * <p>Getter for the field <code>timeout</code>.
	 * After timeout period command value is returned to 0.</p>
	 *
	 * @return a long
	 */
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * <p>Setter for the field <code>timeout</code>.
	 * After timeout period command value is returned to 0.</p>
	 *
	 * @param timeout a long
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * <p>pushDone.</p>
	 */
	public void pushDone() {
		_setValue(false, null, null, true, false);
	}
	
	/** {@inheritDoc} */
	@Override
	public void setValue(Object value) {
		
		boolean change= _setValue(value, null, null, true, false);
		
		if (link!=null) {
			if (link.isInvalid()) {
				log.error("Command link '"+linkPV+"' has no connection");
				record.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM);
			} else {
				if (linkVal!=null) {
					try {
						link.setValue(linkVal);
					} catch (Exception e) {
						log.error("Failed to set '"+linkPV+"' with '"+linkVal+"': "+e.toString(), e);
						record.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM);
					}
				} else {
					try {
						link.setValue(value);
					} catch (Exception e) {
						log.error("Failed to set '"+linkPV+"' with '"+value+"': "+e.toString(), e);
						record.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM);
					}
				}
				record.updateNoAlarm();
			}
		}
		
		if (change && getValueAsBoolean() && timeout>0) {
			record.getDatabase().schedule(new Runnable() {
				
				@Override
				public void run() {
					pushDone();
				}
			}, timeout);
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// nothing to do
	}

}
