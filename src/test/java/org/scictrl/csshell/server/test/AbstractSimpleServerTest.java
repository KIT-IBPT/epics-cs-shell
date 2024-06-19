package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.scictrl.csshell.epics.EPICSConnector;
import org.scictrl.csshell.epics.server.Server;

/**
 * <p>AbstractSimpleServerTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class AbstractSimpleServerTest {

	/**
	 * <p>setUpBeforeClass.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		Configurator.initialize(new DefaultConfiguration());
	}

	/**
	 * <p>tearDownAfterClass.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * The server instance, a subject of this test.
	 */
	protected Server server;
	
	/**
	 * EPICS connector available for this test.
	 */
	protected EPICSConnector connector;
	
	

	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
		
		connector= EPICSConnector.newInstance(null);
		assertNotNull(connector);
		
		server= new Server();
		
		assertNotNull(server);
		
		server.activate();
		
		assertTrue(server.isActive());

	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@After
	public void tearDown() throws Exception {
		
		if (server!=null) {
			server.destroy();
		}
		
		if (connector!=null) {
			connector.shutdown();
		}
	}
	
	/**
	 * <p>wait.</p>
	 *
	 * @param seconds a double
	 */
	protected synchronized void wait(double seconds) {
		try {
			Thread.yield();
			this.wait((long)(seconds*1000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
