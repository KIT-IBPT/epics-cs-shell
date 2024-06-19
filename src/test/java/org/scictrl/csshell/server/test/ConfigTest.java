/**
 * 
 */
package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.InetAddress;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scictrl.csshell.epics.server.ConfigurationManager;
import org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor;
import org.scictrl.csshell.epics.server.Record;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>ConfigTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ConfigTest {

	
	private String[] pvs={"A:${host}:SoftServer:default:shutdown1","A:${host}:SoftServer:default:ping1","A:${host}:SoftServer:default:list1","A:TEST:Test001","A:TEST:Test002","A:TEST:State:01:state","A:TEST:Alarm001","A:TEST:Alarm002","A:TEST:Alarm003","A:TEST:Alarm004"};
	private String host;

	/**
	 * <p>setUp.</p>
	 *
	 * @throws java.lang.Exception if fails
	 */
	@Before
	public void setUp() throws Exception {
		
		Configurator.initialize(new DefaultConfiguration());
		
		host= InetAddress.getLocalHost().getHostName().toUpperCase();
		
		for (int i = 0; i < pvs.length; i++) {
			pvs[i]=pvs[i].replace("${host}", host);
		}

	}

	/**
	 * <p>tearDown.</p>
	 *
	 * @throws java.lang.Exception if fails
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test.
	 */
	@Test
	public void testLading() {
		
		
		try {
			
			String f= "src/test/config/server.xml";
			String name= "default";
			
			Record[] r= ConfigurationManager.loadConfig(f, name);
			
			assertEquals(pvs.length, r.length);
			
			for (int i = 0; i < r.length; i++) {
				assertEquals(pvs[i], r[i].getName());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testAlarmConf() {
		
		
		try {
			
			String f= "src/test/config/server.xml";
			String a= "alarm.xml";
			String name= "default";
			
			DocumentBuilderFactory dbf= DocumentBuilderFactory.newInstance();
			DocumentBuilder db= dbf.newDocumentBuilder();
			Document alarmDoc = db.newDocument();
			
			ConfigurationVisitor cv= new ConfigurationVisitor(name, alarmDoc);
			
			Record[] r= ConfigurationManager.loadConfig(f, cv);
				
			assertEquals(pvs.length, r.length);

			Element el= alarmDoc.getDocumentElement();
			
			assertNotNull(el);
			assertEquals("config", el.getNodeName());
			assertEquals("", el.getAttribute("name"));

			alarmDoc.getDocumentElement().setAttribute("name", name);

			assertEquals(name, el.getAttribute("name"));
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			DOMSource source = new DOMSource(alarmDoc);
			StreamResult result = new StreamResult(a);
			t.transform(source, result); 
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
}
