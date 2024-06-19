package org.scictrl.csshell.server.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <p>AllTests class.</p>
 *
 * @author igor@scictrl.com
 */
@RunWith(Suite.class)
@SuiteClasses({ ConfigTest.class, ServerTest.class, SingleConnectionTest.class, PersistencyStoreTest.class})
public class AllTests {

}
