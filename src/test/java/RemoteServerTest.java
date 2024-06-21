

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>RemoteServerTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class RemoteServerTest {

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

	private String[] pvs={"A:TEST:Test1","A:TEST:Test2","A:TEST:State:01:state","A:TEST:Alarm1","A:TEST:Alarm2"};

	private EPICSConnector connector;
	
	/**
	 * Constructor.
	 */
	public RemoteServerTest() {
	}

	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
		
		connector= EPICSConnector.newInstance(null);

	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@After
	public void tearDown() throws Exception {
		
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
			assertEquals(double[].class, p.getValue().getClass());
			//assertEquals(pvs[i],0.0, ((double[])p.getValue())[0],0.0001);
		}
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Double> ec1= (EPICSConnection<Double>) connector.newConnection(pvs[0], DataType.DOUBLE);
		@SuppressWarnings("unchecked")
		EPICSConnection<Double> ec2= (EPICSConnection<Double>) connector.newConnection(pvs[1], DataType.DOUBLE);
		
		ec1.setValue(0.0);
		ec2.setValue(0.0);
		synchronized (pvs) {
			pvs.wait(100);
		}

		
		// initial value
		Double d1= ec1.getValue();
		Double d2= ec2.getValue();
		
		//set to 2.0
		Double d3= 2.0;
		ec2.setValue(d3);
		
		synchronized (pvs) {
			pvs.wait(100);
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

		ec1.setValue(0.0);
		ec2.setValue(0.0);
		synchronized (pvs) {
			pvs.wait(200);
		}
		assertEquals(0.0, ec1.getValue(),0.0001);
		assertEquals(0.0, ec2.getValue(),0.0001);

		
		// test alarms
		
		
		
		Poop<Double, DBR> poop1= ec1.getPoop();
		assertNotNull(poop1);
		assertEquals(Severity.MINOR_ALARM, ((STS)poop1.getVector()).getSeverity());
		assertEquals(Status.UDF_ALARM, ((STS)poop1.getVector()).getStatus());
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec3= (EPICSConnection<Long>) connector.newConnection(pvs[3], DataType.LONG);
		Poop<Long, DBR> poop2= ec3.getPoop();
		
		assertEquals(Severity.MINOR_ALARM, ((STS)poop2.getVector()).getSeverity());
		assertEquals(Status.UDF_ALARM, ((STS)poop2.getVector()).getStatus());
		assertEquals(Long.class, poop2.getValue().getClass());
		assertEquals(1, ((Long)poop2.getValue()).longValue());
		
		@SuppressWarnings("unchecked")
		EPICSConnection<Long> ec4= (EPICSConnection<Long>) connector.newConnection(pvs[2], DataType.ENUM);
		
		ec4.setValue(2l);
		synchronized (pvs) {
			pvs.wait(100);
		}
		long state = ec4.getValue();
		assertEquals(2l, state);

		state= ec4.getValue();
		
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

		ec4.setValue(2l);
		synchronized (pvs) {
			pvs.wait(100);
		}
		state= ec4.getValue();
		assertEquals(2l, state);
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

}
