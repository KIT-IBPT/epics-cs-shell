/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>ShutdownManagementProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class ShutdownManagementProcessor extends ManagementProcessor {
	
	
	/**
	 * <p>Constructor for ShutdownManagementProcessor.</p>
	 */
	public ShutdownManagementProcessor() {
		super();
		type= DBRType.STRING;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		_setValue(new String[]{"What is the magic word?"},null,null,false,true);
		this.fixed=true;
	}
	
	/** {@inheritDoc} */
	@Override
	public void setValue(Object value) {
		LogManager.getLogger(getClass()).info("Shutdown initiated trough management!");
		if (record.getDatabase().getServer().requestShutdown(String.valueOf(value))) {
			System.exit(0);
		}
	}
}
