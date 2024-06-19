package org.scictrl.csshell.server.processor.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <p>AllTests class.</p>
 *
 * @author igor@scictrl.com
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	ValueDiffCheckProcessorTest.class, 
	ValueLevelAlarmProcessorTest.class,
	SemaphoreProcessorTest.class, 
	CounterProcessorsTest.class, 
	TimeValueProcessorTest.class, 
	ValueCheckProcessorTest.class,
	StatusCheckAlarmProcessorTest.class,
	SummaryAlarmProcessorTest.class,
	ForwardValueProcessorTest.class,
	MemoryValueProcessorsTest.class
	})

public class AllTests {

}
