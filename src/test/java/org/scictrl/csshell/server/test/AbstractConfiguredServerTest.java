/**
 * 
 */
package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.scictrl.csshell.epics.EPICSConnector;
import org.scictrl.csshell.epics.server.ConfigurationManager;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.Server;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>AbstractConfiguredServerTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class AbstractConfiguredServerTest extends AbstractSimpleServerTest {
	
	/**
	 * Configuration file name, it matches test class name.
	 */
	protected String configFile= getConfigDir()+getClass().getSimpleName()+".xml";
	
	/**
	 * Configuration name for server to be run in server configuration file, 
	 */
	protected String configName= "default";
	
	/**
	 * Initial number of PVs, if no other PVs are declared in application.
	 */
	protected int pvCount=3; 

	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if fails
	 */
	@Before
	public void setUp() throws Exception {
		
		connector= EPICSConnector.newInstance(null);
		assertNotNull(connector);
			
		server= new Server(true);
		assertNotNull(server);
			
		Record[] r= ConfigurationManager.loadConfig(configFile, configName);
		
		//System.out.println(Arrays.toString(r));
		
		//assertEquals(pvCount, r.length);

		server.getDatabase().addAll(r);
		assertEquals(pvCount, server.recordCount());
		
		server.activate();
		assertTrue(server.isActive());

		assertEquals(0, server.pvCount());

	}
	
	/**
	 * <p>checkRecord.</p>
	 *
	 * @param r a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	protected void checkRecord(Record r) {
		assertNotNull(r);
		assertEquals(Severity.NO_ALARM, r.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, r.getAlarmStatus());
	}
	
	/**
	 * <p>pause.</p>
	 *
	 * @param time a long
	 */
	protected void pause(long time) {
		Thread.yield();
		synchronized (this) {
			try {
				this.wait(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * <p>getConfigDir.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	protected String getConfigDir() {
		return "src/test/config/";
	}

}
