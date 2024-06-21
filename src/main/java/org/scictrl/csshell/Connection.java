/**
 * 
 */
package org.scictrl.csshell;

import java.beans.PropertyChangeListener;

/**
 * Simple easy to use interface for remote value connection.
 * Rquired generics: C - Connector, T - data type, V - vector delivering value, such as DBR.
 *
 * @author igor@scictrl.com
 * 
 * @param <C> connector implementation
 * @param <T> data type
 * @param <V> vector delivering value, such as DBR
 */
public interface Connection<C extends AbstractConnector<?>,T,V> {
	
	/**
	 * Property name for poop. Used to indicate interest in events which update value in form of Poop object. 
	 */
	public static final String PROPERTY_POOP="poop";
	/**
	 * Property name for value. Used to indicate interest in events which update value. 
	 */
	public static final String PROPERTY_VALUE="value";
	/**
	 * Property name for status. Used to indicate interest in events which update status, such as 
	 * connected, disconnected, etc. 
	 */
	public static final String PROPERTY_STATUS="status";
	/**
	 * Property name for metaData. Used to indicate interest in events which update metaData structure. 
	 */
	public static final String PROPERTY_META_DATA="metaData";
	/**
	 * Property name for alarm. Used to indicate interest in events which update alarm states. 
	 */
	public static final String PROPERTY_ALARM="alarm";
	
	
	/**
	 * Returns remote name for this connection.
	 *
	 * @return remote name
	 */
	public String getName();
	
	/**
	 * Returns Connector object responsible for this connection.
	 *
	 * @return a C object
	 */
	public C getConnector();
	
	/**
	 * <p>getMetaData.</p>
	 *
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData getMetaData();
	
	/**
	 * <p>getMetaDataAsync.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void getMetaDataAsync(PropertyChangeListener l);

	/**
	 * <p>getDataType.</p>
	 *
	 * @return a {@link org.scictrl.csshell.DataType} object
	 */
	public DataType getDataType();

	/**
	 * Synchronously obtains remote value and returns it. This call is blocked until value is returned.
	 *
	 * @return remote value
	 * @throws org.scictrl.csshell.RemoteException if retrieving value fails
	 */
	public T getValue() throws RemoteException;

	/**
	 * <p>getPoop.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Poop} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public Poop<T,V> getPoop() throws RemoteException;
	
	/**
	 * <p>setValue.</p>
	 *
	 * @param value a T object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public void setValue(T value) throws RemoteException;
	
	/**
	 * <p>addPropertyChangeListener.</p>
	 *
	 * @param pName a {@link java.lang.String} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public void addPropertyChangeListener(String pName, PropertyChangeListener l) throws RemoteException;

	/**
	 * <p>removePropertyChangeListener.</p>
	 *
	 * @param pName a {@link java.lang.String} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void removePropertyChangeListener(String pName, PropertyChangeListener l);
	
	/**
	 * <p>destroy.</p>
	 */
	public void destroy();
	
	/**
	 * Returns true, when this connection object is connected and has received at least one remote update.
	 * This means that remote connection is successfully established (CONNECTED status state)
	 * and alive (no warnings, alarms or errors states in status).
	 * As remote update counts that hasLastPoop() returns true, which is consequence of least one successful
	 * get call was made or there is property listener registered on this object and
	 * remote monitor has already returned at least one monitor update.
	 *
	 * @return returns true, when connection is ready to be used
	 */
	public boolean isReady();

	/**
	 * Returns <code>true</code> after destroy method was called.
	 *
	 * @return <code>true</code> after destroy method was called
	 */
	public boolean isDestroyed();
	
	/**
	 * <p>isConnected.</p>
	 *
	 * @return a boolean
	 */
	public boolean isConnected();
	
	/**
	 * It will block this call until this connection is connected or timeout occurs and connection fails.
	 */
	public void waitTillConnected();

	/**
	 * <p>hasLastPoop.</p>
	 *
	 * @return a boolean
	 */
	public boolean hasLastPoop();

	/**
	 * <p>getLastPoop.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Poop} object
	 */
	public Poop<T,V> getLastPoop();
	
	/**
	 * Returns value from last Poop converted to data type of the connection, if possible.
	 *
	 * @return the last value from last Poop
	 */
	public T getLastValue();

	/**
	 * <p>setValue.</p>
	 *
	 * @param value a T object
	 * @param l a {@link org.scictrl.csshell.ResponseListener} object
	 * @return a {@link org.scictrl.csshell.Request} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public Request<C> setValue(T value, ResponseListener<C> l) throws RemoteException;
	
	/**
	 * <p>getValue.</p>
	 *
	 * @param l a {@link org.scictrl.csshell.ResponseListener} object
	 * @return a {@link org.scictrl.csshell.Request} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public Request<C> getValue(ResponseListener<C> l) throws RemoteException;
	
	/**
	 * <p>getStatus.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public Status getStatus();
}
