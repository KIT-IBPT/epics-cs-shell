package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.scictrl.csshell.epics.server.processor.RunningAverageValueProcessor.RunningAverageBufferCalculator;

/**
 * <p>RunningAverageBufferCalculatorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class RunningAverageBufferCalculatorTest {

	/**
	 * Test.
	 */
	@Test
	public void test() {
		RunningAverageBufferCalculator calc= new RunningAverageBufferCalculator(3);
		
		assertEquals(0, calc.size);
		assertEquals(0.0,calc.avg,0.000000001);
		assertEquals(0.0,calc.std,0.000000001);

		calc.add(100);
		
		assertEquals(1, calc.size);
		assertEquals(100.0,calc.avg,0.000000001);
		assertEquals(0.0,calc.std,0.000000001);

		calc.add(90);
		calc.add(110);
		
		assertEquals(3, calc.size);
		assertEquals(100.0,calc.avg,0.000000001);
		assertEquals(10.0,calc.std,0.000000001);
	}

}
