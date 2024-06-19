/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.Checks.Check;
import org.scictrl.csshell.epics.server.processor.Checks.Condition;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ValueCheckProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueCheckProcessorTest extends AbstractConfiguredServerTest {

	/**
	 * Constructor.
	 */
	public ValueCheckProcessorTest() {
		pvCount+=6;
	}

	private Check toCheck(String conf) {
		
		XMLConfiguration c= new XMLConfiguration();
		try {
			c.load(new StringReader(conf));
		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
		//System.out.println(c.getRootElementName());
		//System.out.println(c.getRootNode().getValue());
		//System.out.println(c.getRoot().getValue());
		//System.out.println(c.getProperty(""));
		//System.out.println(c.getProperty("."));
		
		
		Check ch= Check.fromConfiguration(c, null, null);
	
		return ch;
	}
	
	private void testCheck(String check, Condition c, String link) {
		Check ch= toCheck(check);
		assertNotNull(ch);
		assertEquals(c, ch.getCondition());
		assertEquals(link, ch.getLink());
	}

	/**
	 * Test.
	 */
	@Test
	public void testCheckClass() {
		
		testCheck("<check always=\"1\">L</check>",Condition.ALWAYS,null);
		testCheck("<check never=\"1\">L</check>",Condition.NEVER,null);
		testCheck("<check equal=\"1\">L</check>",Condition.EQUAL,"L");
		testCheck("<check nonequal=\"1\">L</check>",Condition.NONEQUAL,"L");
		testCheck("<check same=\"1\"></check>",Condition.SAME,null);
		testCheck("<check different=\"1\">L</check>",Condition.DIFFERENT,"L");
		testCheck("<check greater=\"1\">L</check>",Condition.GREATER,"L");
		testCheck("<check less=\"1\">L</check>",Condition.LESS,"L");
		testCheck("<check greatereq=\"1\">L</check>",Condition.GREATEREQ,"L");
		testCheck("<check lesseq=\"1\">L</check>",Condition.LESSEQ,"L");
	
		
	}
	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record recA= server.getDatabase().getRecord("A:TEST:VC:01:A");
		Record recB= server.getDatabase().getRecord("A:TEST:VC:01:B");
		Record recC= server.getDatabase().getRecord("A:TEST:VC:01:C");
		Record recAnd= server.getDatabase().getRecord("A:TEST:VC:01:AND");
		Record recOr= server.getDatabase().getRecord("A:TEST:VC:01:OR");
		Record recSum= server.getDatabase().getRecord("A:TEST:VC:01:SUM");
		
		assertNotNull(recA);
		assertNotNull(recB);
		assertNotNull(recC);
		assertNotNull(recAnd);
		assertNotNull(recOr);
		assertNotNull(recSum);
		
		assertEquals(0.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, recB.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, recC.getValueAsDouble(), 0.0000001);
		
		assertEquals(false, recAnd.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recAnd.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recAnd.getAlarmStatus());

		assertEquals(false, recOr.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recOr.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recOr.getAlarmStatus());
		
		assertEquals(false, recSum.getValueAsBoolean());
		assertEquals(Severity.NO_ALARM, recSum.getAlarmSeverity());
		assertEquals(Status.NO_ALARM, recSum.getAlarmStatus());
		
		recA.setValue(1.0);
		
		wait(0.1);
		
		assertEquals(1.0, recA.getValueAsDouble(), 0.0000001);
		assertEquals(false, recAnd.getValueAsBoolean());
		assertEquals(true, recOr.getValueAsBoolean());
		assertEquals(false, recSum.getValueAsBoolean());
		
		recB.setValue(1.0);
		
		wait(0.1);
		
		assertEquals(1.0, recB.getValueAsDouble(), 0.0000001);
		assertEquals(false, recAnd.getValueAsBoolean());
		assertEquals(true, recOr.getValueAsBoolean());
		assertEquals(false, recSum.getValueAsBoolean());

		recC.setValue(25.0);
		
		wait(0.1);
		
		assertEquals(25.0, recC.getValueAsDouble(), 0.0000001);
		assertEquals(false, recAnd.getValueAsBoolean());
		assertEquals(true, recOr.getValueAsBoolean());
		assertEquals(false, recSum.getValueAsBoolean());
	
		recC.setValue(15.0);
		
		wait(0.5);
		
		assertEquals(15.0, recC.getValueAsDouble(), 0.0000001);
		assertEquals(true, recAnd.getValueAsBoolean());
		assertEquals(true, recOr.getValueAsBoolean());
		assertEquals(true, recSum.getValueAsBoolean());
		
		recC.setValue(5.0);
		
		wait(0.1);
		
		assertEquals(5.0, recC.getValueAsDouble(), 0.0000001);
		assertEquals(false, recAnd.getValueAsBoolean());
		assertEquals(true, recOr.getValueAsBoolean());
		assertEquals(false, recSum.getValueAsBoolean());

	}
	
}
