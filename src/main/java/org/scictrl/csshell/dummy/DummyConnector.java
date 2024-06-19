/**
 * 
 */
package org.scictrl.csshell.dummy;

import gov.aps.jca.Channel;

import java.beans.PropertyChangeListener;
import java.util.Properties;

import org.scictrl.csshell.AbstractConnector;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.RemoteException;

/**
 * <p>DummyConnector class.</p>
 *
 * @author igor@scictrl.com
 */
public class DummyConnector extends AbstractConnector<DummyConnection<?>> {

	/** Constant <code>TYPE="DUMMY"</code> */
	public static final String TYPE="DUMMY";

	
	/** {@inheritDoc} */
	public static AbstractConnector<?> getInstance(Properties configuration)
			throws RemoteException
		{
			return new DummyConnector(configuration);
		}

	
	
	
	/**
	 * <p>Constructor for DummyConnector.</p>
	 *
	 * @param configuration a {@link java.util.Properties} object
	 */
	public DummyConnector(Properties configuration) {
		super(configuration);
	}

	
	/** {@inheritDoc} */
	@Override
	public String getType() {
		return TYPE;
	}

	/** {@inheritDoc} */
	@Override
	public MetaData getMetaData(String name, DataType type) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public DummyConnection<?> newConnection(String name, DataType dataType)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Object getValue(String name, DataType type) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/** {@inheritDoc} */
	@Override
	public void getMetaDataAsync(Channel channel, PropertyChangeListener l)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	
	
}
