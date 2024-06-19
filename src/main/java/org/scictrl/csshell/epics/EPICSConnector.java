/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.scictrl.csshell.epics;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;
import org.scictrl.csshell.AbstractConnector;
import org.scictrl.csshell.Connection;
import org.scictrl.csshell.ConnectionCache;
import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;

import com.cosylab.epics.caj.CAJContext;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Channel.ConnectionState;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.QueuedEventDispatcher;

/**
 * Implementation of EPICS plugin.
 *
 * @author igor@scictrl.com
 */
public class EPICSConnector extends AbstractConnector<EPICSConnection<?>> implements ContextExceptionListener, ContextMessageListener {

	/**
	 * Makes one time asynchronous requests.
	 */
	public static class OneShotGet implements ConnectionListener, GetListener {
		
		private Channel channel;
		private DBRType type;
		private boolean getting= false;
		private Exception error;
		private DBR dbr;
		private Context ctx;
		private String property;
		private PropertyChangeListener callback;

		/**
		 * Constructor.
		 * @param name the name of remote channel 
		 * @param ctx  the context for the connection
		 * @param type the data type of the connection
		 * @throws IllegalStateException if connection fails
		 * @throws CAException if connection fails
		 */
		public OneShotGet(String name, Context ctx, DBRType type) throws IllegalStateException, CAException {
			this.type=type;
			this.ctx=ctx;
			channel= ctx.createChannel(name, this);
		}
		
		/**
		 * Constructor
		 * @param channel the channel to make requests to
		 * @param type the default data type
		 */
		public OneShotGet(Channel channel, DBRType type) {
			this.type=type;
			this.channel= channel;
		}

		@Override
		public synchronized void connectionChanged(ConnectionEvent ev) {
			if (ev!=null && ev.isConnected() && ev.getSource() instanceof Channel) {
				channel = (Channel)ev.getSource();
			}
			if (!getting && channel!=null && channel.getConnectionState()==ConnectionState.CONNECTED) {
				getting= true;
				if (type==null) {
					type= EPICSUtilities.toTimeDBRType(channel.getFieldType());
				}
				try {
					channel.get(type, channel.getElementCount(), this);
					channel.getContext().pendIO(DEFAULT_PENDIO_TIMEOUT_VALUE);
				} catch (Exception e) {
					error= e;
					notifyAll();
				}
			}
		}
		
		@Override
		public synchronized void getCompleted(GetEvent ev) {
			dbr= ev.getDBR();
			notifyAll();
			if (property==Connection.PROPERTY_META_DATA) {
				if (dbr!=null) {
					MetaData md= EPICSUtilities.toMetaData(channel, dbr);
					callback.propertyChange(new PropertyChangeEvent(channel, Connection.PROPERTY_META_DATA, null, md));
				} else {
					callback.propertyChange(new PropertyChangeEvent(channel, Connection.PROPERTY_META_DATA, null, MetaDataImpl.createUninitializedMetaData()));
				}
				if (ctx!=null) {
					try {
						channel.destroy();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (CAException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * Requests {@link DBR} data and waits till result or timeout.
		 * @param timeout the timeout to wait for data
		 * @return returns {@link DBR} for connection
		 * @throws Exception if request fails
		 */
		private synchronized DBR getFuture(long timeout) throws Exception {
			connectionChanged(null);
			if (error!=null) {
				if (ctx!=null) channel.destroy();
				throw error;
			}
			if (dbr==null) {
				wait(timeout);
			}
			if (dbr!=null) {
				return dbr;
			}
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(channel.getName(), "Timeout ("+(int)(timeout/1000.0)+"s) while performing get.", null));
		}

		/**
		 * Start asynchronous request.
		 * @param property the property to be requested
		 * @param callback the callback to receive notification
		 * @throws Exception if request fails
		 */
		private void startRequest(String property, PropertyChangeListener callback) throws Exception {
			this.property=property;
			this.callback=callback;
			connectionChanged(null);
			if (error!=null) {
				if (ctx!=null)
					try {
						channel.destroy();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (CAException e) {
						e.printStackTrace();
					}
				throw error;
			}
		}

		/**
		 * Requests {@link DBR} data and waits till result or timeout.
		 * @param timeout the timeout to wait for data
		 * @return returns {@link DBR} for connection
		 * @throws Exception if request fails
		 */
		public synchronized DBR getFutureDBR(long timeout) throws Exception {
			getFuture(timeout);
			if (ctx!=null) channel.destroy();
			if (dbr!=null) {
				return dbr;
			}
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(channel.getName(), "Timeout ("+(int)(timeout/1000.0)+"s) while performing get.", null));
		}
		
		/**
		 * Requests metadata and waits for result or timeout.
		 * @param timeout the timeout to wait for data
		 * @return returns {@link MetaData} object for channel.
		 * @throws Exception if request fails
		 */
		public synchronized MetaData getFutureMetaData(long timeout) throws Exception {
			getFuture(timeout);
			if (dbr!=null) {
				MetaData md= EPICSUtilities.toMetaData(channel, dbr);
				if (ctx!=null) channel.destroy();
				return md;
			}
			if (ctx!=null) channel.destroy();
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(channel.getName(), "Timeout ("+(int)(timeout/1000.0)+"s) while performing get.", null));
		}
		
		/**
		 * Requests metadata in asynchronous way and delivers result to callback.
		 * @param callback the callback for the request
		 * @throws Exception if request fails
		 */
		public synchronized void requestMetaData(PropertyChangeListener callback) throws Exception {
			startRequest(Connection.PROPERTY_META_DATA,callback);
		}

		/**
		 * Blacks call and waits for result in future.
		 * @param timeout timeout for wiating
		 * @return result
		 * @throws Exception if error
		 */
		public synchronized Poop<?,DBR> getFuturePoop(long timeout) throws Exception {
			getFuture(timeout);
			if (dbr!=null) {
				Object value= EPICSUtilities.toJavaValue(dbr, EPICSUtilities.toDataType(dbr).getJavaClass(), channel.getFieldType());
				Poop<?,DBR> p=new Poop<Object,DBR>(value, EPICSUtilities.toTimestamp(dbr), EPICSUtilities.toMetaData(channel, dbr), EPICSUtilities.toStatus(dbr), dbr);
				if (ctx!=null) channel.destroy();
				return p;
			}
			if (ctx!=null) channel.destroy();
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(channel.getName(), "Timeout ("+(int)(timeout/1000.0)+"s) while performing get.", null));
		}
	}
	
	/**
	 * Plug type string.
	 */
	public static final String TYPE = "EPICS";

	/**
	 * Default authority.
	 */
	public static final String DEFAULT_AUTHORITY = "DEFAULT";

	/**
	 * Property name for JCA context type flag.  
	 * If <code>false</code> or not defined then by default CAJ instance of JCA context is used.
	 * If value set to <code>true</code> in System properties or in configuration properties, then JNI (thread safe) 
	 * instance of JCA context is used.
	 * Property defined in System properties take precedence before property in defined in configuration.
	 */
	public static final String PROPERTY_USE_JNI = "EPICS.use_jni";
		
	/**
	 * Parameter name for expert monitor creation. 
	 * Value is of type Integer and provides mask value for EPICS monitor creation.
	 */
	public static final String PARAMETER_MONITOR_MASK = "EPICS.monitor.mask";
	
	/**
	 * Property name for verbosity of INFO log level. 
	 * Verbosity 0 is minimal, verbosity 1 reports individual connections are made. Default is 1.
	 */
	public static final String PROPERTY_VERBOSITY = "EPICS.verbosity";
	/**
	 * Property name for default pendIO timeout property. 
	 * Value is of type Double and provides the default timeout for pendIO.
	 */
	public static final String PROPERTY_DEFAULT_PENDIO_TIMEOUT = "EPICS.default_pendIO_timeout";
	
	/**
	 * Property name for default monitor property. 
	 * Value is of type Integer and provides mask value for default EPICS monitor creation.
	 */
	public static final String PROPERTY_DEFAULT_MONITOR_MASK = "EPICS.default_monitor_mask";
	
	/**
	 * Property name for JNI flush timer delay.
	 * The default value is 100 ms and it is overridden if provided in the configuration.
	 * Property defined in System properties take precedence before property in defined in configuration.
	 */
	public static final String PROPERTY_JNI_FLUSH_TIMER_DELAY = "EPICS.jni_flush_timer_delay";

	/**
	 * Default verbosity level.
	 */
	public static final int DEFAULT_VERBOSITY = 1;

	/**
	 * Defines if characteristics should be initialized on connect event.
	 */
	private boolean initializeCharacteristicsOnConnect;
	
	/**
	 * Defines if a common <code>Executor</code> from this <code>EPICSPlug</code> should be used instead of
	 * individual <code>Executor<code>s in <code>PropertyProxyImpl</code>s.
	 * 
	 * @see PropertyProxyImpl
	 */
	private boolean useCommonExecutor;
	
	private static final Double DEFAULT_PENDIO_TIMEOUT_VALUE = 1.0;
	
	/*
	 * Timeout for calling PendIO. 
	 * Units are seconds.
	 */
	private double pendIOTimeout = DEFAULT_PENDIO_TIMEOUT_VALUE;
	
	/*
	 * Timeout for various operations.
	 * It is configured trough system property defined by org.epics.css.dal.spi.Plugs.CONNECTION_TIMEOUT.
	 * Units are seconds.
	 */
	private long timeout = 30000;

	/**
	 * Context.
	 */
	private Context context;
	
	/**
	 * Verbosity of INFO log level.
	 */
	private int verbosity = DEFAULT_VERBOSITY;
	
	/**
	 * Flag that indicates if JNI is used.
	 */
	private boolean use_jni = false;
	
	/**
	 * Default monitor mask used for creation of monitors.
	 */
	private int defaultMonitorMask = Monitor.ALARM | Monitor.VALUE;
	
	/** Constant <code>PROPERTY_JCA_ADDR_LIST="addr_list"</code> */
	public static final String PROPERTY_JCA_ADDR_LIST="addr_list";
	/** Constant <code>PROPERTY_JCA_AUTO_ADDR_LIST="auto_addr_list"</code> */
	public static final String PROPERTY_JCA_AUTO_ADDR_LIST="auto_addr_list";
	/** Constant <code>PROPERTY_JCA_NAME_SERVERS="name_servers"</code> */
	public static final String PROPERTY_JCA_NAME_SERVERS="name_servers";
	/** Constant <code>PROPERTY_JCA_CONNECTION_TIMEOUT="connection_timeout"</code> */
	public static final String PROPERTY_JCA_CONNECTION_TIMEOUT="connection_timeout";
	/** Constant <code>PROPERTY_JCA_BEACON_PERIOD="beacon_period"</code> */
	public static final String PROPERTY_JCA_BEACON_PERIOD="beacon_period";
	/** Constant <code>PROPERTY_JCA_REPEATER_PORT="repeater_port"</code> */
	public static final String PROPERTY_JCA_REPEATER_PORT="repeater_port";
	/** Constant <code>PROPERTY_JCA_SERVER_PORT="server_port"</code> */
	public static final String PROPERTY_JCA_SERVER_PORT="server_port";
	/** Constant <code>PROPERTY_JCA_MAX_ARRAY_BYTES="max_array_bytes"</code> */
	public static final String PROPERTY_JCA_MAX_ARRAY_BYTES="max_array_bytes";

	/**
	 * If JNI is used, this flag indicates if <code>flushIO</code> method has been
	 * called and flushIO should be called on context on next run of
	 * <code>jniFlushTimer</code>.
	 */
	//private boolean jniFlushIO = false;
	
	/**
	 * Timer that is used for flushingIO when JNI is used.
	 */
	//private Timer jniFlushTimer;
	
	/**
	 * Delay for <code>jniFlushTimer</code> that is used for flushingIO when JNI
	 * is used.
	 */
	//private long jniFlushTimerDelay = 100;

	private boolean dbrUpdatesCharacteristics=true;
	
	/**
	 * Create EPICS plug instance.
	 * @param configuration
	 * @throws RemoteException 
	 */
	private EPICSConnector(Properties configuration) throws RemoteException {
		super(configuration);
		
		setConnectionCache(newDefaultConnectionCache());
		
		registerDefaultMetaData("DBPM", null, new MetaDataImpl(null, "BPM", -15.0, 15.0, -15.0, 15.0, -15.0, 15.0, -15.0, 15.0, null, null, "%5.3f", "mm", null, null, DataType.DOUBLE, Double.class, true, false, null));
		registerDefaultMetaData("PBEND", null, new MetaDataImpl(null, "Main dipole manget", 0.0, 790.0, 0.0, 790.0, 0.0, 790.0, 0.0, 790.0, null, null, "%5.3f", "A", null, null, DataType.DOUBLE, Double.class, true, true, null));
		registerDefaultMetaData("PQ", null, new MetaDataImpl(null, "Quadrupoles", 0.0, 300.0, 0.0, 300.0, 0.0, 300.0, 0.0, 300.0, null, null, "%5.3f", "A", null, null, DataType.DOUBLE, Double.class, true, true, null));
		registerDefaultMetaData("PCH", null, new MetaDataImpl(null, "Horizontal correctors", -2.0, 2.0, -2.0, 2.0, -2.0, 2.0, -2.0, 2.0, null, null, "%5.3f", "A", null, null, DataType.DOUBLE, Double.class, true, true, null));
		registerDefaultMetaData("PCV", null, new MetaDataImpl(null, "Vertical correctors", -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, null, null, "%5.3f", "A", null, null, DataType.DOUBLE, Double.class, true, true, null));
		
		initialize();
	}
	
	/**
	 * Create new EPICS plug instance.
	 *
	 * @param configuration a {@link java.util.Properties} object
	 * @throws org.scictrl.csshell.RemoteException if initialization of remote context fails
	 * @return a {@link org.scictrl.csshell.epics.EPICSConnector} object
	 */
	public static synchronized EPICSConnector newInstance(Properties configuration) throws RemoteException {
		return (EPICSConnector)getInstance(configuration);
	}
	
	/** {@inheritDoc} */
	public static synchronized AbstractConnector<?> getInstance(Properties configuration) throws RemoteException {
		return new EPICSConnector(configuration);
	}

	
	/* (non-Javadoc)
	 * @see org.epics.css.dal.proxy.AbstractPlug#releaseInstance()
	 */
	/**
	 * <p>shutdown.</p>
	 *
	 * @throws java.lang.Exception if any.
	 */
	public synchronized void shutdown() throws Exception {
		if (!isAlive()) {
			return;
		}

		super.shutdown();
		
		if (context!=null) {
				context.destroy();
				context=null;
		}
	}

	/**
	 * Initialize EPICS plug.
	 * @throws RemoteException if initialization of remote context fails
	 */
	private void initialize() throws RemoteException {
		
		if (System.getProperties().containsKey(PROPERTY_DEFAULT_MONITOR_MASK)) {
			defaultMonitorMask = Integer.valueOf(System.getProperty(PROPERTY_DEFAULT_MONITOR_MASK, (Integer.valueOf(defaultMonitorMask)).toString()));
		} else {
			defaultMonitorMask = Integer.valueOf(getConfiguration().getProperty(PROPERTY_DEFAULT_MONITOR_MASK, (Integer.valueOf(defaultMonitorMask)).toString()));
		}
		
		if (System.getProperties().containsKey(PROPERTY_USE_JNI)) {
			use_jni = Boolean.valueOf(System.getProperty(PROPERTY_USE_JNI, "false"));
		} else {
			use_jni = Boolean.valueOf(getConfiguration().getProperty(PROPERTY_USE_JNI, "false"));
		}
		
		//if (!use_jni) {
			context = createJCAContext();
		/*} else {
			context = createThreadSafeContext();
			
			if (System.getProperties().containsKey(PROPERTY_JNI_FLUSH_TIMER_DELAY)) {
				jniFlushTimerDelay = new Long(System.getProperty(PROPERTY_JNI_FLUSH_TIMER_DELAY, (new Long(jniFlushTimerDelay)).toString()));
			} else {
				jniFlushTimerDelay = new Long(getConfiguration().getProperty(PROPERTY_JNI_FLUSH_TIMER_DELAY, (new Long(jniFlushTimerDelay)).toString()));
			}
			
			jniFlushTimer = new Timer();
			jniFlushTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (jniFlushIO) {
						jniFlushIO = false;
						try {
							getContext().flushIO();
						} catch (Throwable th) {
							Logger.getLogger(this.getClass()).warn("Flush IO error.", th);
						}
					}
					
				}
			}, jniFlushTimerDelay, jniFlushTimerDelay);
		}*/
		
		if (System.getProperties().containsKey(PROPERTY_DEFAULT_PENDIO_TIMEOUT)) {
			pendIOTimeout = Double.valueOf(System.getProperty(PROPERTY_DEFAULT_PENDIO_TIMEOUT, DEFAULT_PENDIO_TIMEOUT_VALUE.toString()));
		} else {
			pendIOTimeout = Double.valueOf(getConfiguration().getProperty(PROPERTY_DEFAULT_PENDIO_TIMEOUT, DEFAULT_PENDIO_TIMEOUT_VALUE.toString()));
		}
		
		if (System.getProperties().containsKey(ConnectorUtilities.CONNECTION_TIMEOUT)) {
			timeout = Long.parseLong(System.getProperty(ConnectorUtilities.CONNECTION_TIMEOUT, Long.toString(ConnectorUtilities.DEFAULT_CONNECTION_TIMEOUT)));
		} else {
			timeout = Long.parseLong(getConfiguration().getProperty(ConnectorUtilities.CONNECTION_TIMEOUT, Long.toString(ConnectorUtilities.DEFAULT_CONNECTION_TIMEOUT)));
		}
		
		if (System.getProperties().containsKey(PROPERTY_VERBOSITY)) {
			verbosity = Integer.parseInt(System.getProperty(PROPERTY_VERBOSITY, Integer.toString(DEFAULT_VERBOSITY)));
		} else {
			verbosity = Integer.parseInt(getConfiguration().getProperty(PROPERTY_VERBOSITY, Integer.toString(DEFAULT_VERBOSITY)));
		}

		getLogger().info("config {jni:"+use_jni+", addr_list:{"+System.getProperty("com.cosylab.epics.caj.CAJContext.addr_list")+"}, timeout:"+timeout+"}");
		
	}

	/**
	 * <p>Getter for the field <code>verbosity</code>.</p>
	 *
	 * @return a int
	 */
	public int getVerbosity() {
		return verbosity;
	}

	boolean isVerbosityAbove0() {
		return verbosity>0;
	}

	/*
	 * @see org.epics.css.dal.proxy.AbstractPlug#getPlugType()
	 */
	/**
	 * <p>getType.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getType() {
		return TYPE;
	}

	/*
	 * @see Context.flushIO(double)
	 */
	/**
	 * <p>flushIO.</p>
	 */
	public void flushIO() {
		/*if (use_jni) {
			jniFlushIO = true;
		} else {*/
			try {
				// CAJ will take care of optimization
				getContext().flushIO();
			} catch (Throwable th) {
				LogManager.getLogger(this.getClass()).warn("Flush IO error: "+EPICSUtilities.toShortErrorReport(th), th);
			}
		//}
	}

	/*
	 * @see Context.pendIO(double)
	 */
	/**
	 * <p>pendIO.</p>
	 *
	 * @throws gov.aps.jca.CAException if any.
	 * @throws gov.aps.jca.TimeoutException if any.
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public void pendIO() throws CAException, TimeoutException, RemoteException {
		getContext().pendIO(pendIOTimeout);
	}

	/*
	 * @see Context
	 */
	/**
	 * <p>Getter for the field <code>context</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.Context} object
	 */
	public synchronized Context getContext() {
		if (context==null) {
			throw new IllegalStateException("Connection to EPICS has beeen already destroyed.");
		}
		return context;
	}
	
	private CAJContext createJCAContext() throws RemoteException {
		try {
			DefaultConfiguration edconf = new DefaultConfiguration("event_dispatcher");
			edconf.setAttribute("class", QueuedEventDispatcher.class.getName());

			
			DefaultConfiguration config = new DefaultConfiguration("EPICSPlugConfig");
		    config.setAttribute("class", JCALibrary.CHANNEL_ACCESS_JAVA);
			config.addChild(edconf);
			
			copyAttribute(config,PROPERTY_JCA_ADDR_LIST);
			copyAttribute(config,PROPERTY_JCA_AUTO_ADDR_LIST);
			copyAttribute(config,PROPERTY_JCA_BEACON_PERIOD);
			copyAttribute(config,PROPERTY_JCA_CONNECTION_TIMEOUT);
			copyAttribute(config,PROPERTY_JCA_NAME_SERVERS);
			copyAttribute(config,PROPERTY_JCA_REPEATER_PORT);
			copyAttribute(config,PROPERTY_JCA_SERVER_PORT);
			
			config.setAttribute(PROPERTY_JCA_MAX_ARRAY_BYTES, getConfiguration().getProperty(PROPERTY_JCA_MAX_ARRAY_BYTES, "12000000"));
		    
			// create context
		    CAJContext c= (CAJContext)JCALibrary.getInstance().createContext(config);
		    
			// force explicit initialization
			c.initialize();

			// register all context listeners
			c.addContextExceptionListener(this);
			c.addContextMessageListener(this);
			
			c.getLogger().setLevel(Level.WARNING);
			Logger.getLogger("com.cosylab.epics.caj.impl.CABeaconHandler").setLevel(Level.WARNING);
			
			return c;

		} catch (Throwable th) {
			// rethrow to abort EPICS plug instance creation
			throw new RemoteException(this,"Failed to initilze EPICS plug: "+EPICSUtilities.toShortErrorReport(th), th);
		}
	}
	
	private void copyAttribute(DefaultConfiguration config, String prop) {
		if (getConfiguration().containsKey(prop)) {
			config.setAttribute(prop, getConfiguration().getProperty(prop));
		}
	}

/*	private ThreadSafeContext createThreadSafeContext() throws RemoteException {
		try {
			DefaultConfiguration edconf = new DefaultConfiguration("event_dispatcher");
			edconf.setAttribute("class", QueuedEventDispatcher.class.getName());

			
			DefaultConfiguration config = new DefaultConfiguration("EPICSPlugConfig");
			config.setAttribute("class", JCALibrary.JNI_THREAD_SAFE);
			config.addChild(edconf);
		    
			// create context
		    ThreadSafeContext c= (ThreadSafeContext)JCALibrary.getInstance().createContext(config);
		    
			// force explicit initialization
			c.initialize();

			// register all context listeners
			c.addContextExceptionListener(this);
			c.addContextMessageListener(this);
			
			return c;

		} catch (Throwable th) {
			// rethrow to abort EPICS plug instance creation
			throw new RemoteException(this,"Failed to initilze EPICS plug: "+EPICSUtilities.toShortErrorReport(th), th);
		}
	}*/

	/**
	 * Get timeout parameter (in milliseconds).
	 * It is configured trough system property defined by org.epics.css.dal.spi.Plugs.CONNECTION_TIMEOUT.
	 *
	 * @return timeout (in milliseconds)
	 */
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * Gets the default monitor mask.
	 *
	 * @return the default monitor mask
	 */
	public int getDefaultMonitorMask() {
		return defaultMonitorMask;
	}
	
	/**
	 * Gets the {@link #initializeCharacteristicsOnConnect} property.
	 *
	 * @return <code>true</code> if characteristics should be initialized on connect and <code>false</code> otherwise.
	 */
	public boolean isInitializeCharacteristicsOnConnect() {
		return initializeCharacteristicsOnConnect;
	}
	
	/**
	 * <p>isDbrUpdatesCharacteristics.</p>
	 *
	 * @return a boolean
	 */
	public boolean isDbrUpdatesCharacteristics() {
		return dbrUpdatesCharacteristics;
	}
	
	/**
	 * Gets {@link #useCommonExecutor} property.
	 *
	 * @return <code>true</code> if common executor should be used and <code>false</code> otherwise.
	 */
	public boolean isUseCommonExecutor() {
		return useCommonExecutor;
	}
	
	/* (non-Javadoc)
	 * @see gov.aps.jca.event.ContextExceptionListener#contextException(gov.aps.jca.event.ContextExceptionEvent)
	 */
	/** {@inheritDoc} */
	public void contextException(ContextExceptionEvent ev) {
		//
	}

	/* (non-Javadoc)
	 * @see gov.aps.jca.event.ContextExceptionListener#contextVirtualCircuitException(gov.aps.jca.event.ContextVirtualCircuitExceptionEvent)
	 */
	/** {@inheritDoc} */
	public void contextVirtualCircuitException(ContextVirtualCircuitExceptionEvent ev) {
		//
	}

	/* (non-Javadoc)
	 * @see gov.aps.jca.event.ContextMessageListener#contextMessage(gov.aps.jca.event.ContextMessageEvent)
	 */
	/** {@inheritDoc} */
	public void contextMessage(ContextMessageEvent ev) {
		//
	}
	
	/** {@inheritDoc} */
	@Override
	public MetaData getMetaData(String name, DataType type) throws Exception {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		
		final int CTRL_OFFSET = 28;
		final DBRType ctrlType = DBRType.forValue(EPICSUtilities.toDBRType(type).getValue() + CTRL_OFFSET);
		
		OneShotGet get= new OneShotGet(name, getContext(), ctrlType);
		
		return get.getFutureMetaData(timeout);
	}
	
	/**
	 * <p>getMetaDataAsync.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link org.scictrl.csshell.DataType} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 * @throws java.lang.Exception if any.
	 */
	public void getMetaDataAsync(String name, DataType type, PropertyChangeListener l) throws Exception {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		
		final int CTRL_OFFSET = 28;
		final DBRType ctrlType = DBRType.forValue(EPICSUtilities.toDBRType(type).getValue() + CTRL_OFFSET);
		
		OneShotGet get= new OneShotGet(name, getContext(), ctrlType);
		get.requestMetaData(l);
	}
	
	/** {@inheritDoc} */
	@Override
	public void getMetaDataAsync(Channel channel, PropertyChangeListener l) throws Exception {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		
		final int CTRL_OFFSET = 28;
		final DBRType ctrlType = DBRType.forValue(channel.getFieldType().getValue() + CTRL_OFFSET);
		
		OneShotGet get= new OneShotGet(channel, ctrlType);
		get.requestMetaData(l);
	}

	/**
	 * <p>getOneShot.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @return a {@link gov.aps.jca.dbr.DBR} object
	 * @throws java.lang.Exception if any.
	 */
	public DBR getOneShot(String name, DBRType type) throws Exception {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		
		OneShotGet get= new OneShotGet(name, getContext(), type);
		
		return get.getFutureDBR(timeout);
		
	}
	
	/**
	 * Makes one time get request and tries to get as many data as possible.
	 * Data type is taken from channel.
	 *
	 * @param name PV name
	 * @return the Poop object with everything
	 * @throws java.lang.Exception if any
	 */
	public Poop<?,DBR> getOneShot(String name) throws Exception {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		
		OneShotGet get= new OneShotGet(name, getContext(), null);
		
		return get.getFuturePoop(timeout);
		
	}

	/** {@inheritDoc} */
	@Override
	public Object getValue(String name, DataType type) throws RemoteException {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		try {
			DBRType dtype = EPICSUtilities.toDBRType(type);
			
			DBR val= getOneShot(name, dtype);
			
			return EPICSUtilities.toJavaValue(val, type.getJavaClass(), dtype);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(name, "Get failed.", e),e);
		}
	}

	
	/** {@inheritDoc} */
	@Override
	public EPICSConnection<?> newConnection(String name, DataType dataType) throws RemoteException {
		if (!isAlive()) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		if (isVerbosityAbove0()) getLogger().info("Connecting '"+name+"' ("+dataType+")");
		
		ConnectionCache<EPICSConnection<?>> cc= getConnectionCache();
		
		if (cc!=null) {
			EPICSConnection<?> ec= cc.get(name);
			if (ec!=null) {
				if (ec.isDestroyed()) {
					cc.remove(name);
				} else {
					return ec;
				}
			}
		}
		
		try {
			EPICSConnection<Object> ec = new EPICSConnection<Object>(name,dataType,this);
			if (cc!=null) {
				cc.add(ec);
			}
			return ec;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RemoteException(this,ConnectorUtilities.formatConnectionMessage(name, "Connection failed.", e),e);
		}
	}

}


