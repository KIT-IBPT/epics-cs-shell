/**
 * 
 */
package org.scictrl.csshell.epics.server.application.devicegroup;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.application.AbstractApplication;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>DeviceTableApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class DeviceTableApplication extends AbstractApplication {

	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		String[] columnNames= config.getStringArray("columnNames");
		//String[] columnTypes= config.getStringArray("columnTypes");
		
		//String[] pvs= config.getStringArray("devices");
		
		//String prefix= this.name+this.nameDelimiter;
		
		addRecordOfMemoryValueProcessor("Rows:Count", "Number of rows in table.", DBRType.INT, new int[]{0});
		addRecordOfMemoryValueProcessor("Columns:Count", "Number of columns in table.", DBRType.INT, new int[]{0});
		addRecordOfMemoryValueProcessor("Columns:Names", "Number of columns in table.", DBRType.STRING, (Object[])columnNames);
		
	}
	
	
	
	/**
	 * <p>Constructor for DeviceTableApplication.</p>
	 */
	public DeviceTableApplication() {
	}

}
