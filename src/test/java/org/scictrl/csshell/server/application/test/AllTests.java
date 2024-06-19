package org.scictrl.csshell.server.application.test;

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
	EmbeddedApplicationServerTest.class, 
	FeedbackLoopApplicationTest.class, 
	ScanApplicationTest.class,
	RunningCounterApplicationTest.class})
public class AllTests {

}
