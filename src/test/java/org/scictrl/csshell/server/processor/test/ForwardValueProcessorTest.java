/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.ForwardValueProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

/**
 * <p>ForwardValueProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class ForwardValueProcessorTest extends AbstractConfiguredServerTest {

	/**
	 * <p>Constructor for ForwardValueProcessorTest.</p>
	 */
	public ForwardValueProcessorTest() {
		pvCount+=5;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record valueA= server.getDatabase().getRecord("A:TEST:01:ValueA");
		Record valueB= server.getDatabase().getRecord("A:TEST:01:ValueB");
		Record trigger= server.getDatabase().getRecord("A:TEST:01:Trigger");
		Record enable= server.getDatabase().getRecord("A:TEST:01:Enable");
		Record forward= server.getDatabase().getRecord("A:TEST:01:Forward");
		
		wait(5.0);

		assertEquals(0.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, forward.getValueAsDouble(), 0.0000001);

		forward.setValue(1.0);
		wait(1.0);
		
		assertEquals(0.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, forward.getValueAsDouble(), 0.0000001);
		
		trigger.setValue(1.0);
		wait(1.0);
		
		assertEquals(0.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, forward.getValueAsDouble(), 0.0000001);

		enable.setValue(Boolean.TRUE);
		wait(1.0);

		assertEquals(0.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(0.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, forward.getValueAsDouble(), 0.0000001);
		
		forward.setValue(1.0);
		wait(1.0);
		
		assertEquals(1.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, forward.getValueAsDouble(), 0.0000001);

		trigger.setValue(2.0);
		wait(1.0);
		
		assertEquals(2.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(2.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(2.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(2.0, forward.getValueAsDouble(), 0.0000001);
		
		assertEquals(ForwardValueProcessor.class,forward.getProcessor().getClass());
		
		ForwardValueProcessor fwp= (ForwardValueProcessor)forward.getProcessor();
		
		fwp.setSetValue(11.0);
		
		trigger.setValue(3.0);
		wait(1.0);
		
		assertEquals(11.0, valueA.getValueAsDouble(), 0.0000001);
		assertEquals(11.0, valueB.getValueAsDouble(), 0.0000001);
		assertEquals(3.0, trigger.getValueAsDouble(), 0.0000001);
		assertEquals(1.0, enable.getValueAsDouble(), 0.0000001);
		assertEquals(11.0, forward.getValueAsDouble(), 0.0000001);
		
	}

}
