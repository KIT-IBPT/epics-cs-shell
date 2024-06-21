package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.scictrl.csshell.Connection;
import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;
import org.scictrl.csshell.epics.server.ConfigurationManager;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.Server;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ServerTest class.</p>
 *
 * @author igor@scictrl.com
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {

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

	private Server server;
	
	private String[] pvs={
			"A:TEST:Test001", //0
			"A:TEST:Test002", //1 
			"A:TEST:State:01:state", //2
			"A:TEST:Alarm001", //3
			"A:TEST:Alarm002", //4
			"A:TEST:Alarm03", //5
			"A:TEST:Alarm004", //6
			"A:localhost:default:shutdown1", //7
			"A:localhost:default:ping1", //8
			"A:localhost:default:list1"}; //9

	private EPICSConnector connector;
	
	
	/**
	 * Constructor.
	 */
	public ServerTest() {
	}
	
	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
		
		String f= "src/test/config/server.xml";
		String name= "default";
		
		Record[] r= ConfigurationManager.loadConfig(f, name);
		
		//assertEquals(pvs.length, r.length);

		Properties p= new Properties();
		p.setProperty(ConnectorUtilities.CONNECTION_TIMEOUT, "100");
		
		server= new Server(p);
		server.getDatabase().addAll(r);
		
		//assertEquals(pvs.length, server.recordCount());
		
		server.activate();
		
		assertEquals(0, server.pvCount());
		
		p= new Properties();
		p.setProperty(ConnectorUtilities.CONNECTION_TIMEOUT, "100");
		connector= EPICSConnector.newInstance(p);

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
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testConnect() throws Exception {
		
		
		for (int i = 0; i < 2; i++) {
			Poop<?, DBR> p= connector.getOneShot(pvs[i]);
			
			assertNotNull(p);
			//assertEquals(Status.NO_ALARM, ((STS)p.getVector()).getStatus());
			assertNotNull(p.getValue());
			assertEquals(Double.class, p.getValue().getClass());
			assertEquals(0.0, (Double)p.getValue(),0.0001);
		}
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Double> ec1= (EPICSConnection<Double>) connector.newConnection(pvs[0], DataType.DOUBLE);
		@SuppressWarnings("unchecked")
		EPICSConnection<Double> ec2= (EPICSConnection<Double>) connector.newConnection(pvs[1], DataType.DOUBLE);
		
		// initial value
		Double d1= ec1.getValue();
		Double d2= ec2.getValue();
		
		//set to 2.0
		Double d3= 2.0;
		ec2.setValue(d3);
		
		synchronized (pvs) {
			pvs.wait(200);
		}

		// check if 2.0 was properly propagated
		Double d4= ec2.getValue();
		Double d5= ec1.getValue();
		
		// check initial values
		assertEquals(0.0, d1.doubleValue(),0.0001);
		assertEquals(0.0, d2.doubleValue(),0.0001);
		
		// check if 2.0 was properly propagated
		assertEquals(d3.doubleValue(), d4.doubleValue(),0.0001);
		assertEquals(d3.doubleValue(), d5.doubleValue(),0.0001);
		
		
		// test alarms
		
		Poop<Double, DBR> poop1= ec1.getPoop();
		assertNotNull(poop1);
		assertEquals(Severity.NO_ALARM, ((STS)poop1.getVector()).getSeverity());
		assertEquals(Status.NO_ALARM, ((STS)poop1.getVector()).getStatus());
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec3= (EPICSConnection<Long>) connector.newConnection(pvs[3], DataType.LONG);
		Poop<Long, DBR> poop2= ec3.getPoop();
		
		// TODO: this test behaves unexpectedly, check
		assertEquals(Severity.NO_ALARM, ((STS)poop2.getVector()).getSeverity());
		assertEquals(Status.NO_ALARM, ((STS)poop2.getVector()).getStatus());
		assertEquals(Long.class, poop2.getValue().getClass());
		assertEquals(0, ((Long)poop2.getValue()).longValue());
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec4= (EPICSConnection<Long>) connector.newConnection(pvs[2], DataType.ENUM);
		
		long state= ec4.getValue();
		
		assertEquals(Long.parseLong("00010", 2), state);

		long shutdown= Long.parseLong("10000", 2);
		ec4.setValue(shutdown);
		
		synchronized (pvs) {
			pvs.wait(100);
		}

		state= ec4.getValue();
		
		assertEquals(shutdown, state);

		poop2= ec4.getPoop();
		
		assertEquals(Severity.NO_ALARM, ((STS)poop2.getVector()).getSeverity());
		assertEquals(Status.NO_ALARM, ((STS)poop2.getVector()).getStatus());
		assertEquals(Long.class, poop2.getValue().getClass());
		assertEquals(shutdown, ((Long)poop2.getValue()).longValue());

	}
	
	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testStatusServer() throws Exception {
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec1= (EPICSConnection<Long>) connector.newConnection(pvs[2], DataType.ENUM);

		Long v1 = ec1.getValue();
		
		assertNotNull(v1);
		assertEquals(2, v1.longValue());
		
		String[] s= ec1.getMetaData().getStates();
		
		assertNotNull(s);

		Poop<Long, DBR> p= ec1.getPoop();
		
		assertNotNull(p);
		assertEquals(Status.NO_ALARM, ((STS)p.getVector()).getStatus());
		assertNotNull(p.getValue());
		assertEquals(Long.class, p.getValue().getClass());
		assertEquals(2, ((Long)p.getValue()).longValue());

		
		
	}
	
	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void testChange() throws Exception {
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Double> ec1= (EPICSConnection<Double>) connector.newConnection(pvs[0], DataType.DOUBLE);

		// initial value
		Double d1= ec1.getValue();
		
		assertEquals(0.0, d1.doubleValue(),0.0001);

		ec1.addPropertyChangeListener(Connection.PROPERTY_VALUE, new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				//System.out.println(evt);
			}
		});
		
		synchronized (pvs) {
			pvs.wait(100);
		}

		assertEquals(true, ec1.hasLastPoop());
		
		Poop<?, ?> p1= ec1.getLastPoop();
		
		assertNotNull(p1);
		
		//set to 2.1
		Double d3= 2.1;
		ec1.setValue(d3);
		
		
		// check if 2.1 was properly propagated
		Double d5= ec1.getValue();
		
		assertEquals(d3.doubleValue(), d5.doubleValue(),0.0001);

		Poop<?, ?> p2= ec1.getLastPoop();
		
		assertEquals(false, p1==p2);
		assertEquals(d3.doubleValue(), ((Number)p2.getValue()).doubleValue(),0.0001);
		
		
		
	}

	/**
	 * Test.
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Test
	public void test1InvlidConnect() throws Exception {
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec1= (EPICSConnection<Long>) connector.newConnection(pvs[6], DataType.LONG);

		Long l1= ec1.getValue();
		
		assertNotNull(l1);
		assertEquals(1L, l1.longValue());
		
		
		Poop<Long, DBR> p1= ec1.getPoop();
		
		assertNotNull(p1);
		assertEquals(true, p1.getVector().isSTS());
		assertEquals(Severity.INVALID_ALARM, ((STS)p1.getVector()).getSeverity());
		assertEquals(Status.LINK_ALARM, ((STS)p1.getVector()).getStatus());
		
		synchronized (this) {
			wait(200);
		}
		
		Poop<Long, DBR> p2= ec1.getPoop();
		
		assertNotNull(p2);
		assertEquals(true, p2.getVector().isSTS());
		assertEquals(Severity.INVALID_ALARM, ((STS)p2.getVector()).getSeverity());
		assertEquals(Status.LINK_ALARM, ((STS)p2.getVector()).getStatus());
	}
}
