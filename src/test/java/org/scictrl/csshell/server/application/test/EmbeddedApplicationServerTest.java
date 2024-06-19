 package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.epics.server.PersistencyStore;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.application.EmbeddedApplicationServer;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>EmbeddedApplicationServerTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class EmbeddedApplicationServerTest {

	private String dir="src/test/config/";
	
	EmbeddedApplicationServer s1;
	EmbeddedApplicationServer s2;

	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	@After
	public void tearDown() throws Exception {
		
		if (s1!=null) {
			s1.shutdown();
		}
		if (s2!=null) {
			s2.shutdown();
		}
		
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		String appName= "TEST:TestServer:01";
		s1= EmbeddedApplicationServer.newInstance(appName);
		
		String appPrefix= appName+":";

		Record r1= s1.createRecord("Test:b1", "boolean 1", false);

		String[] names= s1.getRecordNames();
		
		assertEquals(4,names.length);
		assertEquals(appPrefix+names[3],r1.getName());
		
		try {
			s1.startServer();
		} catch (Exception e) {
			fail("Exception "+e);
			e.printStackTrace();
		}
		
		Record r2= s1.createRecord("Test:b2", "boolean 1", false);
		
		names= s1.getRecordNames();
		
		assertEquals(5,names.length);
		assertEquals(appPrefix+names[4],r2.getName());

	}

	/**
	 * Test.
	 */
	@Test
	public void testLinkError() {
		
		Properties p= new Properties();
		p.setProperty(ConnectorUtilities.CONNECTION_TIMEOUT, "100");
		
		String appName1= "TEST:TestServer:01";
		String appName2= "TEST:TestServer:02";
		//String appPrefix1= appName1+":";
		//String appPrefix2= appName2+":";
		
		s1= EmbeddedApplicationServer.newInstance(appName1);
		s2= EmbeddedApplicationServer.newInstance(appName2);
		
		Record r1= s1.createRecord("Test:b1", "boolean 1", false);

		try {
			s1.startServer();
		} catch (Exception e) {
			fail("Exception "+e);
			e.printStackTrace();
		}
		try {
			s2.startServer(p);
		} catch (Exception e) {
			fail("Exception "+e);
			e.printStackTrace();
		}
		
		Record sle2= s2.getRecord("Status:LinkError");
		
		assertNotNull(sle2);
		assertEquals(false, sle2.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, sle2.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, sle2.getAlarmStatus());
		
		Record ses2= s2.getRecord("Status:ErrorSum");
		
		assertNotNull(ses2);
		assertEquals(false, ses2.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, ses2.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, ses2.getAlarmStatus());

		ValueLinks vl1= s2.connectLinks("vl1", r1.getName());
		
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		assertNotNull(vl1);
		assertEquals(true, vl1.isReady());
		assertEquals(0, vl1.getNotConnected().length);
		assertEquals(Severity.NO_ALARM, vl1.getLastSeverity());

		assertNotNull(sle2);
		assertEquals(false, sle2.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, sle2.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, sle2.getAlarmStatus());

		ValueLinks vl2= s2.connectLinks("vl2", "BugsBunny");
		
		synchronized (this) {
			try {
				this.wait(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		assertNotNull(vl2);
		assertEquals(false, vl2.isReady());
		assertEquals(true, vl2.isInvalid());
		assertEquals(1, vl2.getNotConnected().length);
		assertEquals(Severity.INVALID_ALARM, vl2.getLastSeverity());
		assertEquals(Status.LINK_ALARM, vl2.getLastStatus());

		assertEquals(true, sle2.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, sle2.getAlarmSeverity());
		assertEquals(Status.LINK_ALARM, sle2.getAlarmStatus());
		
		assertEquals(true, ses2.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, ses2.getAlarmSeverity());
		assertEquals(Status.LINK_ALARM, ses2.getAlarmStatus());

		assertEquals(false, vl1.isConsumed());
		ValueHolder[] vh= vl1.consume();
		assertEquals(true, vl1.isConsumed());
		assertNotNull(vh);
		assertEquals(1, vh.length);
		assertEquals(0L, vh[0].value);
		
		assertEquals(false, r1.getValueAsBoolean());
		r1.setValue(1);
		assertEquals(true, r1.getValueAsBoolean());

		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(false, vl1.isConsumed());
		vh= vl1.consume();
		assertEquals(true, vl1.isConsumed());
		assertNotNull(vh);
		assertEquals(1, vh.length);
		assertEquals(1L, vh[0].value);
		
		assertEquals(true, sle2.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, sle2.getAlarmSeverity());
		assertEquals(Status.LINK_ALARM, sle2.getAlarmStatus());
		
		assertEquals(true, ses2.getValueAsBoolean());
		assertEquals(Severity.INVALID_ALARM, ses2.getAlarmSeverity());
		assertEquals(Status.LINK_ALARM, ses2.getAlarmStatus());
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testStore() {
		
		String appName= "TEST:TestServer:01";
		s1= EmbeddedApplicationServer.newInstance(appName);
		
		//String appPrefix= appName+":";

		try {
			s1.startServer();
		} catch (Exception e) {
			fail("Exception "+e);
			e.printStackTrace();
		}
		
		File f= new File(dir, "persistancy1.xml");
		if (f.exists()) {
			f.delete();
		}
		
		PersistencyStore ps=null;
		try {
			ps= new PersistencyStore(f, s1.getDatabase());
		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
		Record r1= s1.createRecord("S1", "String 1", "A1");
		assertEquals("A1", r1.getValueAsString());
		
		ps.registerValue(r1);

		r1.setValueAsString("A1,A2");
		assertEquals("A1,A2", r1.getValueAsString());

		ps.deregister(r1);
		
		r1.setValueAsString("B1");
		assertEquals("B1", r1.getValueAsString());

		r1.setValueAsString("B1");
		assertEquals("B1", r1.getValueAsString());

	}

	/**
	 * Test.
	 */
	@Test
	public void testRecord() {
		
		String appName= "TEST:TestServer:01";
		s1= EmbeddedApplicationServer.newInstance(appName);
		
		//String appPrefix= appName+":";

		try {
			s1.startServer();
		} catch (Exception e) {
			fail("Exception "+e);
			e.printStackTrace();
		}

		Record r1= s1.createRecord("S1", "String 1", "A1");

		assertEquals("A1", r1.getValueAsString());
		
		r1.setValueAsString("A1,A2");
		
		assertEquals("A1,A2", r1.getValueAsString());
	}
}
