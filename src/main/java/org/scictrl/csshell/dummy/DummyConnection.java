/**
 * 
 */
package org.scictrl.csshell.dummy;

import java.beans.PropertyChangeListener;

import org.scictrl.csshell.Connection;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.Request;
import org.scictrl.csshell.ResponseListener;
import org.scictrl.csshell.Status;

/**
 * <p>DummyConnection class.</p>
 *
 * @author igor@scictrl.com
 */
public class DummyConnection<T> implements Connection<DummyConnector, T, Object> {

	private String name;
	private DummyConnector connector;

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public DummyConnector getConnector() {
		return connector;
	}

	/** {@inheritDoc} */
	@Override
	public MetaData getMetaData() {
		return null;
	}
	
	/** {@inheritDoc} */
	@Override
	public void getMetaDataAsync(PropertyChangeListener l) {
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public DataType getDataType() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public T getValue() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/** {@inheritDoc} */
	@Override
	public Poop<T,Object> getPoop() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public void setValue(T value) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	public void addPropertyChangeListener(String pName, PropertyChangeListener l)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	public void removePropertyChangeListener(String pName,
			PropertyChangeListener l) {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isDestroyed() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void waitTillConnected() {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasLastPoop() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/** {@inheritDoc} */
	@Override
	public T getLastValue() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Poop<T,Object> getLastPoop() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Request<DummyConnector> setValue(T value,
			ResponseListener<DummyConnector> l) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Request<DummyConnector> getValue(ResponseListener<DummyConnector> l)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

}
