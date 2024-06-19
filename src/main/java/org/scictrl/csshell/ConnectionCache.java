package org.scictrl.csshell;

/**
 * Interfaces for simple connection cache, which it is used inside a Connector.
 *
 * @param <C> class implementing {@link org.scictrl.csshell.Connection} interface
 * @author igor@scictrl.com
 */
public interface ConnectionCache<C extends Connection<?,?,?>> {

	/**
	 * Adds connection to the cache by it's name.
	 * If any connection by same name already exists, it is released and returned.
	 *
	 * @param ch the connection to be placed in cache
	 * @return previous connection with this name, if it was any, otherwise <code>null</code>
	 */
	public abstract C add(C ch);

	/**
	 * Removes connection from cache.
	 *
	 * @param name the name of connection to be removed from cache
	 * @return the removed connection, is exists, otherwise <code>null</code>
	 */
	public abstract C remove(String name);

	/**
	 * Returns connection, which has provided name. Or <code>null</code> if no such connection.
	 *
	 * @param name the connection name
	 * @return the connection, or null
	 */
	public abstract C get(String name);

	/**
	 * Removes all dead references and deleted connections.
	 */
	public abstract void cleanup();
}
