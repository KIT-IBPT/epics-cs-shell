package org.scictrl.csshell.epics.server.processor;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueProcessor;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>Abstract AbstractValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public abstract class AbstractValueProcessor implements ValueProcessor {

	/**
	 * Record owning this processor.
	 */
	protected Record record;
	/**
	 * Update time 
	 */
	protected long trigger;
	/**
	 * Value type.
	 */
	protected DBRType type;
	/**
	 * Logger.
	 */
	protected Logger log= LogManager.getLogger(getClass());

	/**
	 * <p>Constructor for AbstractValueProcessor.</p>
	 */
	public AbstractValueProcessor() {
		super();
	}

	/** {@inheritDoc} */
	public void configure(Record record, HierarchicalConfiguration config) {
		this.record=record;
		this.trigger=config.getLong("trigger", 0);
		if (type==null) {
			type=record.getType();
			if (type==DBRType.UNKNOWN) {
				type= DBRType.DOUBLE;
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public DBRType getType() {
		return type;
	}

	/** {@inheritDoc} */
	@Override
	public long getTrigger() {
		return trigger;
	}

	/** {@inheritDoc} */
	@Override
	public void process() {
		
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return record.getName();
	}

	/** {@inheritDoc} */
	@Override
	public Record getRecord() {
		return record;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		sb.append(getClass().getSimpleName());
		sb.append("{");
		sb.append(getName());
		sb.append("}");
		return sb.toString();
	}

}
