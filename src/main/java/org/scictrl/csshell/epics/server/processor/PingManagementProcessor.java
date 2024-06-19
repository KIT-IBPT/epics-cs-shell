/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>PingManagementProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class PingManagementProcessor extends ManagementProcessor {

	
	/**
	 * <p>Constructor for PingManagementProcessor.</p>
	 */
	public PingManagementProcessor() {
		super();
		type= DBRType.INT;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		_setValue(new int[]{1},null, null, false,true);
	}
}
