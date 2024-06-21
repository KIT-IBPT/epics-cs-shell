package org.scictrl.csshell.server.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <p>AllTestSuites class.</p>
 *
 * @author igor@scictrl.com
 */
@RunWith(Suite.class)
@SuiteClasses({ 
		AllTests.class, 
		org.scictrl.csshell.epics.test.AllTests.class,
		org.scictrl.csshell.server.application.test.AllTests.class,
		org.scictrl.csshell.server.processor.test.AllTests.class,
		org.scictrl.csshell.epics.server.application.automata.test.AllTests.class })
public class AllTestSuites {
	
	private AllTestSuites() {
	}

}
