/**
 * 
 */
package org.scictrl.csshell.epics.server;

import org.apache.commons.configuration.Configuration;

/**
 * Implemented by those ValueProcessors, which want to be restored to a previous state after
 * server is restarted. This functionality is active to those processors,
 * which return <code>true</code> for method isPersistent() during activation procedure.
 *
 * @author igor@scictrl.com
 */
public interface PersistentValueProcessor extends ValueProcessor {
	
	/**
	 * Stores internal state so it could be restored after server restart.
	 *
	 * @param store the persistence configuration store
	 */
	public void store(Configuration store);
	/**
	 * ValueProcessor has a chance to restore it's internal state after server has been restart.
	 * This is called after configuration call and before activation call.
	 *
	 * @param store the persistence configuration store
	 */
	public void restore(Configuration store);
	/**
	 * Presidency is only enabled, if this call returns <code>true</code> after configuration has been
	 * applied to value producer.
	 *
	 * @return a boolean
	 */
	public boolean isPersistent();
}
