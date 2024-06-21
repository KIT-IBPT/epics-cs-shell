/**
 * 
 */
package org.scictrl.csshell.epics.casperr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cosylab.epics.caj.cas.util.DefaultServerImpl;

import gov.aps.jca.CAException;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;

/**
 * <p>Server class.</p>
 *
 * @author igor@scictrl.com
 */
public class Server extends DefaultServerImpl {
	
	/** Constant <code>PROPERTY_LAST_ADDED="lastAdded"</code> */
	public static final String PROPERTY_LAST_ADDED = "lastAdded";
	
	class Dispatch implements Runnable {
		
		
		private String added;

		public Dispatch(String added) {
			this.added=added;
		}
		
		@Override
		public void run() {
			support.firePropertyChange(PROPERTY_LAST_ADDED, null, added);
		}
	}
	
	ExecutorService exec= Executors.newSingleThreadExecutor();
	Set<String> discovered = new HashSet<String>();
	PropertyChangeSupport support = new PropertyChangeSupport(this);

	/** {@inheritDoc} */
	@Override
	public synchronized ProcessVariableExistanceCompletion processVariableExistanceTest(
			String aliasName, InetSocketAddress clientAddress,
			ProcessVariableExistanceCallback asyncCompletionCallback)
			throws CAException, IllegalArgumentException, IllegalStateException {
		
		if (!discovered.contains(aliasName)) {
			discovered.add(aliasName);
			exec.execute(new Dispatch(aliasName));
		}
		
		return super.processVariableExistanceTest(aliasName, clientAddress,
				asyncCompletionCallback);
	}
	
	/**
	 * <p>addPropertyChangeListener.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void addPropertyChangeListener(String name, PropertyChangeListener l) {
		support.addPropertyChangeListener(name, l);
	}
	
	/**
	 * <p>Constructor for Server.</p>
	 */
	public Server() {
	}
}
