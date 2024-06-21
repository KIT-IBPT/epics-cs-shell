/**
 * 
 */
package org.scictrl.csshell.epics;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.Connection;
import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.Request;
import org.scictrl.csshell.RequestImpl;
import org.scictrl.csshell.ResponseImpl;
import org.scictrl.csshell.ResponseListener;
import org.scictrl.csshell.Status;
import org.scictrl.csshell.Status.State;
import org.scictrl.csshell.Timestamp;

import com.cosylab.epics.caj.CAJChannel;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Channel.ConnectionState;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

/**
 * <p>EPICSConnection class.</p>
 *
 * @author igor@scictrl.com
 * 
 * @param <T> data type
 */
public class EPICSConnection<T> implements Connection<EPICSConnector,T,DBR>, ConnectionListener {
	
	class MetaDataInterceptor implements PropertyChangeListener {
		PropertyChangeListener peer;
		
		public MetaDataInterceptor(PropertyChangeListener l) {
			this.peer=l;
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			metaData=(MetaData) evt.getNewValue();
			peer.propertyChange(evt);
			fireMetaDataChange(metaData);
			connector.getLogger().debug('['+getName()+']'+" metadata is "+metaData);
		}
	}
	
	
	class MonitorProxy implements MonitorListener {
		
		
		@SuppressWarnings("unused")
		private Map<String, Object> parameters;
		private Monitor monitor;
		private boolean destroyed;
		
		public MonitorProxy(Map<String,Object> param) throws CAException {
			this.parameters=param;
			
			int mask= connector.getDefaultMonitorMask();
			
			if (param!=null && param.get(EPICSConnector.PARAMETER_MONITOR_MASK) instanceof Integer) {
				mask= (Integer)param.get(EPICSConnector.PARAMETER_MONITOR_MASK);
			}
			
			monitor = channel.addMonitor(
					EPICSUtilities.toTimeDBRType(dataType),
					channel.getElementCount(),
					mask, this);
			connector.flushIO();
		}

		@Override
		public void monitorChanged(MonitorEvent ev) {
			if (destroyed) {
				return;
			}
			DBR dbr = ev.getDBR();
			
			//System.err.println("M "+dbr);
			
			if(dbr==null 
					|| dbr.getValue()==null 
					|| !ev.getStatus().isSuccessful()) {
				
				return;
			}
			
			updateStatus(dbr);
			fireValueChange(ev);
		}
		
		public void destroy() {
			
			if (destroyed) {
				return;
			}			
			destroyed = true;

			// destroy remote instance
			if (monitor != null) {
				try {
					monitor.clear();
				} catch (CAException e) {
					// noop
				} catch (RuntimeException e) {
					//might happen in the CA - disconnect monitor and it should eventually be gc
					throw e;
				} finally {
					monitor.removeMonitorListener(this);
				}
			}
		}
	}
	
	/**
	 * Get listener implementation to implement sync. get.
	 */
	private class GetListenerImpl implements gov.aps.jca.event.GetListener {
		volatile gov.aps.jca.event.GetEvent event = null;

		@Override
        public synchronized void getCompleted(final gov.aps.jca.event.GetEvent ev) {
			event = ev;
			this.notifyAll();
		}
	}
	
	/**
	 * Put request object. Callback object for put requests.
	 */
	public class PutRequest extends RequestImpl<EPICSConnector> implements PutListener
	{
		/**
		 * Proxy of this request.
		 */
		protected EPICSConnection<?> conn;

		/**
		 * Constructor of put request.
	     * @param conn the connection to make put to
	     * @param l listener to the notify event
	     */
		public PutRequest(Connection<EPICSConnector,T,DBR> conn, ResponseListener<EPICSConnector> l)
		{
			super(conn,l,null);
		}

		/*
		 * @see gov.aps.jca.event.PutListener#putCompleted(gov.aps.jca.event.PutEvent)
		 */
		@Override
		public void putCompleted(PutEvent ev)
		{
			addResponse(new ResponseImpl<EPICSConnector>(source, this, null,
			        ev.getStatus().isSuccessful(), null, Poop.createTimestampPoop(), true));
		}
	}
	
	/**
	 * Get request object, a callback for get calls.
	 */
	public class GetRequest extends RequestImpl<EPICSConnector> implements GetListener
	{

		/**
		 * Constructor of get request.
	     * @param conn the connection to get from
	     * @param l listener to the notify event
	     */
		public GetRequest(Connection<EPICSConnector, ?, DBR> conn, ResponseListener<EPICSConnector> l)
		{
			super(conn, l, 1);
		}

		/* (non-Javadoc)
		 * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
		 */
		@Override
		public void getCompleted(GetEvent ev)
		{
			if (ev.getDBR()==null || ev.getStatus() == null || !ev.getStatus().isSuccessful()) {
				addResponse(new ResponseImpl<EPICSConnector>(source, this, null, false, null, Poop.createTimestampPoop(), true));
			} else {
				addResponse(new ResponseImpl<EPICSConnector>(source, this, null, true, null, new Poop<Object,DBR>(EPICSUtilities.toJavaValue(ev.getDBR(), dataType.getJavaClass(), channel.getFieldType()), new Timestamp(), metaData, EPICSUtilities.toStatus(ev.getDBR()), ev.getDBR()), true));
			}
		}
	}

	//private final static Status READY= Status.fromStates(State.CONNECTED);
	private final static Logger log= LogManager.getLogger(EPICSConnection.class);
	
	
	@SuppressWarnings("unused")
	private static Map<String, Object> MONITOR_PARAMETERS_ALARM;
	
	{
		
		Map<String, Object> m= new HashMap<String, Object>(1);
		m.put(EPICSConnector.PARAMETER_MONITOR_MASK,Integer.valueOf(Monitor.ALARM));
		
		MONITOR_PARAMETERS_ALARM= Collections.unmodifiableMap(m);
		
	}
	
	private String name;
	private Channel channel;
	private EPICSConnector connector;
	private MetaData metaData;
	private DataType dataType;
	private PropertyChangeSupport support;
	private MonitorProxy monitor;
	//private MonitorProxy monitorAlarm;
	private Poop<T,DBR> lastPoop;
	private boolean destroyed;
	private Status status;
	
	/**
	 * <p>Constructor for EPICSConnection.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @param connector a {@link org.scictrl.csshell.epics.EPICSConnector} object
	 * @throws java.lang.IllegalArgumentException if any.
	 * @throws java.lang.IllegalStateException if any.
	 * @throws gov.aps.jca.CAException if any.
	 */
	public EPICSConnection(String name, DataType dataType, EPICSConnector connector) throws IllegalArgumentException, IllegalStateException, CAException {
		this.name=name;
		this.dataType=dataType;
		this.connector=connector;
		
		status= Status.INITIAL;

		synchronized (this) {
			channel= connector.getContext().createChannel(name,this);
		}
		
		connector.schedule(new Runnable() {
			
			@Override
			public void run() {
				if (channel.getConnectionState()==ConnectionState.NEVER_CONNECTED) {
					synchronized (EPICSConnection.this) {
						setStatus(State.FAILED);
						EPICSConnection.this.notifyAll();
					}
				}
				
			}
		}, ConnectorUtilities.getInitialConnectionTimeout(connector.getConfiguration(),connector.getTimeout()), 0);
		
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void connectionChanged(ConnectionEvent ev) {
		connector.getLogger().debug('['+getName()+']'+" connection update to "+channel.getConnectionState().getName());
		if (ev.isConnected()) {
			if (dataType==null) {
				DBRType type= channel.getFieldType();
				int count= channel.getElementCount();
				dataType= EPICSUtilities.toDataType(type, count);
				if (dataType==null) {
					dataType= DataType.DOUBLE;
				}
			}
		} else {
			connector.getLogger().debug('['+getName()+']'+" connection failed "+channel.getConnectionState().getName());
		}
		updateConnectionState();
		try {
			ensureMonitor();
		} catch (RemoteException e) {
			e.printStackTrace();
			connector.getLogger().debug('['+getName()+']'+" monitor creation failed "+e,e);
		}
	}
	
	private synchronized void updateConnectionState() {
		
		ConnectionState cd= channel.getConnectionState();
		
		if (cd==ConnectionState.CLOSED) {
			destroy();
		} else if (cd==ConnectionState.CONNECTED) {
			setStatus(State.CONNECTED);
		} else if (cd==ConnectionState.DISCONNECTED) {
			setStatus(State.LOST);
		} else if (cd==ConnectionState.NEVER_CONNECTED) {
			setStatus(State.INITIAL);
		}
		
		notifyAll();
	}
	
	private void setStatus(State... state) {
		Status st= Status.fromStates(state);
		setStatus(st);
	}
	
	private void setStatus(Status status) {
		if (!this.status.equals(status)) {
			this.status=status;
			synchronized (this) {
				notifyAll();
			}
			
			if (support!=null && support.hasListeners(PROPERTY_STATUS)) {
				support.firePropertyChange(PROPERTY_STATUS, null, status);
			}

		}
	}

	private void updateStatus(final DBR dbr) {
		if (dbr== null  || !dbr.isSTS()) {
			return;
		}
		final STS sts= (STS)dbr;

		final Severity se = sts.getSeverity();
		
		Status st= status;

		if (se == Severity.NO_ALARM) {
			st = st.unset(State.WARNING);
			st = st.unset(State.ALARM);
			st = st.unset(State.INVALID);
		} else if (se == Severity.MINOR_ALARM) {
			st = st.set(State.WARNING);
			st = st.unset(State.ALARM);
			st = st.unset(State.INVALID);
		} else if (se == Severity.MAJOR_ALARM) {
			st = st.unset(State.WARNING);
			st = st.set(State.ALARM);
			st = st.unset(State.INVALID);
		} else if (se == Severity.INVALID_ALARM) {
			st = st.unset(State.WARNING);
			st = st.unset(State.ALARM);
			st = st.set(State.INVALID);
		}
		
		if (!st.equals(status)) {
			setStatus(st);
		}

	}

	/**
	 * <p>fireValueChange.</p>
	 *
	 * @param ev a {@link gov.aps.jca.event.MonitorEvent} object
	 */
	public void fireValueChange(MonitorEvent ev) {
		@SuppressWarnings("unchecked")
		T value= (T)EPICSUtilities.toJavaValue(ev.getDBR(), dataType.getJavaClass(), channel.getFieldType());
		lastPoop= new Poop<T,DBR>(value, EPICSUtilities.toTimestamp(ev.getDBR()), metaData, EPICSUtilities.toStatus(ev.getDBR()), ev.getDBR());
		
		if (support!=null) {
			if (support.hasListeners(PROPERTY_VALUE)) {
				support.firePropertyChange(PROPERTY_VALUE, null, value);
			}
			if (support.hasListeners(PROPERTY_POOP)) {
				support.firePropertyChange(PROPERTY_POOP, null, lastPoop);
			}
		}
	}

	/**
	 * <p>fireMetaDataChange.</p>
	 *
	 * @param md a {@link org.scictrl.csshell.MetaData} object
	 */
	public void fireMetaDataChange(MetaData md) {
		if (support!=null && support.hasListeners(PROPERTY_META_DATA)) {
			support.firePropertyChange(PROPERTY_META_DATA, null, md);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void addPropertyChangeListener(String pName, PropertyChangeListener l) throws RemoteException {
		if (support==null) {
			support= new PropertyChangeSupport(this);
		}
		support.addPropertyChangeListener(pName, l);
		if (PROPERTY_VALUE.equals(pName) && !ensureMonitor() && hasMonitor() && hasLastPoop()) {
			log.debug('['+getName()+']'+" initial listener update.");
			l.propertyChange(new PropertyChangeEvent(this, PROPERTY_VALUE, null, getLastPoop().getValue()));
		}
		if (PROPERTY_POOP.equals(pName) && !ensureMonitor() && hasMonitor() && hasLastPoop()) {
			log.debug('['+getName()+']'+" initial listener update.");
			l.propertyChange(new PropertyChangeEvent(this, PROPERTY_POOP, null, getLastPoop()));
		}
		if (PROPERTY_STATUS.equals(pName)) {
			l.propertyChange(new PropertyChangeEvent(this, PROPERTY_STATUS, null, status));
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void removePropertyChangeListener(String pName,
			PropertyChangeListener l) {
		if (support!=null) {
			support.removePropertyChangeListener(pName, l);
			
			if (PROPERTY_VALUE.equals(pName) && !(support.hasListeners(PROPERTY_VALUE)||support.hasListeners(PROPERTY_POOP)) && monitor!=null) {
				monitor.destroy();
				monitor=null;
			}
		}
	}

	/**
	 * Return <code>true</code> if monitor was created.
	 * @return <code>true</code> if monitor was created
	 * @throws RemoteException if monitor creation fails
	 */
	private synchronized boolean ensureMonitor() throws RemoteException {
		log.debug('['+getName()+']'+" monitor request:"+(support!=null && (support.hasListeners(PROPERTY_VALUE)||support.hasListeners(PROPERTY_POOP)))+","+(monitor==null)+","+(channel.getConnectionState()==ConnectionState.CONNECTED)+","+isConnected());
		
		if (support!=null 
				&& (support.hasListeners(PROPERTY_VALUE) || support.hasListeners(PROPERTY_POOP)) 
				&& monitor==null 
				&& channel.getConnectionState()==ConnectionState.CONNECTED
				&& isConnected()) 
		{
			try {
				monitor= new MonitorProxy(null);
				log.debug('['+getName()+']'+" monitor created.");

				/*Poop<?, DBR> p=getLastPoop();
				if (p==null) {
					try {
						OneShotGet get= new OneShotGet(channel, null);
						lastPoop=(Poop<T, DBR>) (p= get.getFuturePoop(connector.getTimeout()));
						fireValueChange(new MonitorEvent(channel, p.getVector(), CAStatus.NORMAL));
						return true;
					} catch (RemoteException e) {
						log.debug('['+getName()+']'+" initial value retrival failed "+e,e);
					
				} else {
					fireValueChange(new MonitorEvent(channel, p.getVector(), CAStatus.NORMAL));
					return true;
				}*/
				
				return true;
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Monitor creation failed.", e),e);
			}
		}
		return false;
	}

	/*private boolean ensureAlarmMonitor() throws RemoteException {
		if (support!=null 
				&& support.hasListeners(PROPERTY_ALARM) 
				&& monitorAlarm==null 
				&& channel.getConnectionState()==ConnectionState.CONNECTED) 
		{
			try {
				monitorAlarm= new MonitorProxy(null);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Alarm monitor creation failed.", e),e);
			}
		}
		return false;
	}*/

	/** {@inheritDoc} */
	@Override
	public DataType getDataType() {
		return dataType;
	}
	
	/** {@inheritDoc} */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <p>Getter for the field <code>channel</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.Channel} object
	 */
	public Channel getChannel() {
		return channel;
	}
	
	/** {@inheritDoc} */
	@Override
	public EPICSConnector getConnector() {
		return connector;
	}
	
	/** {@inheritDoc} */
	@Override
	public MetaData getMetaData() {
		if (metaData==null) {
			waitTillConnected();
			connector.getLogger().debug('['+getName()+']'+" metadata requested...");
			try {
				metaData= connector.getMetaData(name, dataType);
			} catch (Exception e) {
				e.printStackTrace();
				metaData= MetaDataImpl.createUninitializedMetaData();
			}
			connector.getLogger().debug('['+getName()+']'+" metadata is "+metaData);
			fireMetaDataChange(metaData);
		}
		return metaData;
	}
	
	/** {@inheritDoc} */
	@Override
	public void getMetaDataAsync(PropertyChangeListener l) {
		if (metaData==null) {
			waitTillConnected();
			connector.getLogger().debug('['+getName()+']'+" metadata requested...");
			try {
				connector.getMetaDataAsync(this.getChannel(),new MetaDataInterceptor(l));
			} catch (Exception e) {
				connector.getLogger().error('['+getName()+']'+" metadata request failed: "+e.toString(),e);
				l.propertyChange(new PropertyChangeEvent(this, PROPERTY_META_DATA, null, MetaDataImpl.createUninitializedMetaData()));
			}
			
		} else {
			l.propertyChange(new PropertyChangeEvent(this, PROPERTY_META_DATA, null, metaData));
		}
	}
	
	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public T getValue() throws RemoteException {
		connector.getLogger().debug('['+getName()+']'+" value requested... ");
		//Object o= getPoop().getValue();
		
		waitTillConnected();
		try
		{
			final GetListenerImpl listener = new GetListenerImpl();
	         synchronized (listener) {
	        	
	    		DBRType type = EPICSUtilities.toDBRType(dataType); 
				channel.get(type, channel.getElementCount(), listener);
				connector.flushIO();

				try {
					listener.wait(connector.getTimeout());
				} catch (final InterruptedException e) {
					// noop
				}
			}

			final gov.aps.jca.event.GetEvent event = listener.event;
			if (event == null) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, timeout ("+(int)(connector.getTimeout()/1000.0)+"s).", null));
            }

			// status check
			if (event.getStatus() != CAStatus.NORMAL) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, status error.", null));
            }

			// sanity check
			if (event.getDBR() == null) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, DBR is null.", null));
            }

			Object o= EPICSUtilities.toJavaValue(event.getDBR(), dataType.getJavaClass(), channel.getFieldType());
			connector.getLogger().debug('['+getName()+']'+" value is "+o);
			return (T)o;
		} catch (Exception e) {
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed.", e),e);
		}
	}
	
	/**
	 * <p>getPoop.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Poop} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public Poop<T,DBR> getPoop() throws RemoteException {
		
		waitTillConnected();
		try
		{
			final GetListenerImpl listener = new GetListenerImpl();
	         synchronized (listener) {
	        	
	    		DBRType type = EPICSUtilities.toDBRType(dataType); 
	    				
	    		if (metaData==null) {
		     		int CTRL_OFFSET = 28;
    				type= DBRType.forValue(type.getValue() + CTRL_OFFSET);
	    		} else {
	    			type=EPICSUtilities.toTimeDBRType(type);
	    		}
	        	 
				channel.get(type, channel.getElementCount(), listener);
				connector.flushIO();

				try {
					listener.wait(connector.getTimeout());
				} catch (final InterruptedException e) {
					// noop
				}
			}

			final gov.aps.jca.event.GetEvent event = listener.event;
			if (event == null) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, timeout ("+(int)(connector.getTimeout()/1000.0)+"s).", null));
            }

			// status check
			if (event.getStatus() != CAStatus.NORMAL) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, status error.", null));
            }

			// sanity check
			if (event.getDBR() == null) {
				throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed, DBR is null.", null));
            }

			if (metaData==null) {
				metaData = EPICSUtilities.toMetaData(channel, event.getDBR());
			}

			@SuppressWarnings("unchecked")
			Poop<T,DBR> p= new Poop<T,DBR>((T)EPICSUtilities.toJavaValue(event.getDBR(), dataType.getJavaClass(), channel.getFieldType()),EPICSUtilities.toTimestamp(event.getDBR()),getMetaData(), EPICSUtilities.toStatus(event.getDBR()), event.getDBR());
			return lastPoop=p;
		} catch (Exception e) {
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Get failed.", e),e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void setValue(T value) throws RemoteException {
		waitTillConnected();
		if (connector.isReadOnly()) {
			return;
		}
		try {
			log.debug('['+getName()+']'+" setting '"+value+"'");
			EPICSUtilities.put(channel, value);
			connector.flushIO();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(this, "Set failed.", e),e);
		}
		
	};
	
	/** {@inheritDoc} */
	@Override
	public Poop<T,DBR> getLastPoop() {
		return lastPoop;
	}
	
	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public T getLastValue() {
		return (T) EPICSUtilities.toJavaValue(lastPoop.getVector(), dataType.getJavaClass(), channel.getFieldType());
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasLastPoop() {
		return lastPoop!=null;
	}
	
	/**
	 * <p>hasMonitor.</p>
	 *
	 * @return a boolean
	 */
	public boolean hasMonitor() {
		return monitor!=null;
	}

	
	/** {@inheritDoc} */
	@Override
	public synchronized void destroy() {
		if (destroyed) {
			return;
		}
		destroyed=true;
		setStatus(State.CLOSED);
		
		if (monitor!=null) {
			monitor.destroy();
		}
		
		if (channel.getConnectionState() != Channel.CLOSED) { // FIXME workaround because CAJChannel.removeConnectionListener throws IllegalStateException: "Channel closed."
			try {
				channel.removeConnectionListener(this);
			} catch (final IllegalStateException e) {
				// we ignore
			} catch (final CAException e) {
				LogManager.getLogger(this.getClass()).warn("Removing CA listener: "+EPICSUtilities.toShortErrorReport(e), e);
			}
		}
		// destory channel
		channel.dispose();

	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isReady() {
		return status.isSet(State.CONNECTED);
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isConnected() {
		return status.isSet(State.CONNECTED);
	}
	
	/** {@inheritDoc} */
	@Override
	public void waitTillConnected() {
		if (!status.isSet(State.CONNECTED)) {
			_waitTillConnected();
		}
	}
	
	private synchronized void _waitTillConnected() {
		if (!status.isSet(State.CONNECTED)) {
			try {
				wait(connector.getTimeout());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public Request<EPICSConnector> setValue(T value, ResponseListener<EPICSConnector> l) throws RemoteException {
		
		final PutRequest r = new PutRequest(this, l);
		if (connector.isReadOnly()) {
			r.addResponse(new ResponseImpl<EPICSConnector>(this, r, null, true,null,Poop.createTimestampPoop(), true));
			return r;
		}
		try {
			final Object o = EPICSUtilities.toDBRValue(value, channel.getFieldType());
			if (channel instanceof CAJChannel) {
                ((CAJChannel) channel).put(EPICSUtilities.toDBRType(dataType), Array.getLength(o), o, r);
            } else {
				// TODO workaround until Channel supports put(DBRType, int, Object, PutListener)
				EPICSUtilities.put(channel, o, r);
			}
			connector.flushIO();
		} catch (final Exception e) {
			r.addResponse(new ResponseImpl<EPICSConnector>(this, r, null, false, e, Poop.createTimestampPoop(), true));
		}
		return r;
	};
	
	/** {@inheritDoc} */
	@Override
	public Request<EPICSConnector> getValue(ResponseListener<EPICSConnector> l) throws RemoteException {
		final GetRequest r = new GetRequest(this, l);
		try {
			channel.get(EPICSUtilities.toDBRType(dataType), channel.getElementCount(), r);
			connector.flushIO();
		} catch (final Exception e) {
			r.addResponse(new ResponseImpl<EPICSConnector>(this, r, null, false, e, Poop.createTimestampPoop(), true));
		}
		return r;
	}
	
	/** {@inheritDoc} */
	@Override
	public Status getStatus() {
		return status;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		sb.append("EPICSConnection{");
		sb.append(name);
		sb.append(",");
		sb.append(dataType);
		sb.append(",");
		sb.append(status);
		sb.append("}");
		return sb.toString();
	}
}
