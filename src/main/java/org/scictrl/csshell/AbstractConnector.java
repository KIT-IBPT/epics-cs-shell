package org.scictrl.csshell;

import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.aps.jca.Channel;


/**
 * This is abstract plug class which helps plug implementators write own
 * plug. This plug has implemented support for following features: proxy
 * object sharing.
 *
 * @author igor@scictrl.com
 */
public abstract class AbstractConnector<C extends Connection<?,?,?>> 
{
	
	/**
	 * Default implementation of connection cache interface. Uses weak references.
	 * @author igor@scictrl.com
	 *
	 */
	public class DefaultConnectionCache implements ConnectionCache<C> {
		Map<String, WeakReference<C>> cache= new HashMap<String, WeakReference<C>>(16);
		
		
		/* (non-Javadoc)
		 * @see org.scictrl.csshell.ConnectionCache#add(C)
		 */
		@Override
		public synchronized C add(C ch) {
			WeakReference<C> wr= new WeakReference<C>(ch);
			WeakReference<C> wr1= cache.put(ch.getName(),wr);
			C ch1= wr1 != null ? wr1.get() : null;
			if (wr1!=null) {
				wr1.clear();
			}
			return ch1;
		}
		
		/* (non-Javadoc)
		 * @see org.scictrl.csshell.ConnectionCache#remove(C)
		 */
		@Override
		public synchronized C remove(String name) {
			WeakReference<C> wr= cache.remove(name);
			C ch= wr != null ? wr.get() : null;
			if (wr!=null) {
				wr.clear();
			}
			return ch;
		}
		
		/* (non-Javadoc)
		 * @see org.scictrl.csshell.ConnectionCache#get(java.lang.String)
		 */
		@Override
		public synchronized C get(String name) {
			WeakReference<C> wr= cache.get(name);
			C ch= wr != null ? wr.get() : null;
			
			if (ch==null) {
				cache.remove(name);
				if (wr!=null) {
					wr.clear();
				}
			}
			
			return ch;
		}
		
		@Override
		public void cleanup() {
			String [] names= cache.keySet().toArray(new String[cache.size()]);
			for (String name : names) {
				WeakReference<C> wr= cache.get(name);
				C ch = wr.get();
				if (ch==null || ch.isDestroyed()) {
					cache.remove(name);
					wr.clear();
				}
			}
		}
		
	}
	
	/**
	 * Wrapper class of <code>Runnable</code> to <code>TimerTask</code>.
	 */
	class ScheduledTask extends TimerTask {
		private Runnable r;

		public ScheduledTask(Runnable r) {
			this.r = r;
		}

		@Override
		public void run() {
			try {
				if (isAlive()) {
					getExecutor().execute(r);
				}
			} catch (Throwable th) {
				LogManager.getLogger(this.getClass().getName()).warn("Sheduled task had unhandled error.", th);
			}
		}
	}
	
	private static final MetaData UNINITIALIZED= MetaDataImpl.createUninitializedMetaData();

	
	private Properties configuration;
	private Logger logger;
	/**
	 * Debug flag.
	 */
	protected boolean debug = false;
	private boolean alive=true;
	private LinkedHashMap<String, MetaData> metaDataCache = new LinkedHashMap<String, MetaData>(10);
	/**
	 * Connection cache.
	 */
	protected ConnectionCache<C> connectionCache;
	/**
	 * Timer instance (used for on-time monitors).
	 */
	private Timer timer;
	/**
	 * Defines the number of core threads to be used with <code>ThreadPoolExecutor</code> from this
	 * <code>EPICSPlug</code> or <code>PropertyProxyImpl</code>.
	 * 
	 * @see PropertyProxyImpl
	 */
	private int coreThreads;
	/**
	 * <code>ThreadPoolExecutor</code> used by this <code>EPICSPlug</code> if {@link #useCommonExecutor}
	 * is selected.
	 */
	private ThreadPoolExecutor executor;
	/**
	 * Defines the maximum number of threads to be used with <code>ThreadPoolExecutor</code> from this
	 * <code>EPICSPlug</code> or <code>PropertyProxyImpl</code>.
	 * 
	 * @see PropertyProxyImpl
	 */
	private int maxThreads;


	private boolean readOnly=false;


	/**
	 * Property name for core threads property: {@link #coreThreads}
	 * <p>
	 * The number of core threads must be non-negative.
	 * </p>
	 */
	public static final String PROPERTY_CORE_THREADS = "CSSHELL.property.core_threads";
	/**
	 * Property name for max threads property: {@link #maxThreads}
	 * <p>
	 * The number of core threads must be non-negative and greater than the number of core threads.
	 * </p>
	 */
	public static final String PROPERTY_MAX_THREADS = "CSSHELL.property.max_threads";

	/**
	 * Property name for setting whole connector in read-only mode by disabling write functionality.
	 * Default value is <code>false</code>.
	 */
	public static final String PROPERTY_READ_ONLY = "CSSHELL.property.read_only";

	/**
	 * Creates new plug instance.
	 *
	 * @param configuration a {@link java.util.Properties} object
	 */
	protected AbstractConnector(Properties configuration)
	{
		super();
		if (configuration == null) {
			this.configuration = new Properties();
		} else {
			this.configuration = (Properties)configuration.clone();
		}
		
		getLogger().info("'"+getType()+"' started.");
		
		coreThreads = 2;
		if (System.getProperties().containsKey(PROPERTY_CORE_THREADS)) {
			coreThreads = Integer.valueOf(System.getProperty(PROPERTY_CORE_THREADS, "2"));
		} else {
			coreThreads = Integer.valueOf(getConfiguration().getProperty(PROPERTY_CORE_THREADS, "2"));
		}
		
		maxThreads = 10;
		if (System.getProperties().containsKey(PROPERTY_MAX_THREADS)) {
			maxThreads = Integer.valueOf(System.getProperty(PROPERTY_MAX_THREADS, "10"));
		} else {
			maxThreads = Integer.valueOf(getConfiguration().getProperty(PROPERTY_MAX_THREADS, "10"));
		}
		
		// checks for coreThreads and maxThreads values
		if (maxThreads == 0) { 
			if (coreThreads != 0) {
				StringBuilder sb= new StringBuilder(128);
				sb.append("> EPICSPlug number of core threads can not be "+coreThreads+". It was changed to ");
				coreThreads = 0;
				sb.append(coreThreads+".");
				getLogger().warn(sb.toString());
			}
		}
		else {
			if (coreThreads < 1) {
				StringBuilder sb= new StringBuilder(128);
				sb.append("> EPICSPlug number of core threads can not be "+coreThreads+". It was changed to ");
				coreThreads = 1;
				sb.append(coreThreads+".");
				getLogger().warn(sb.toString());
			}
			if (maxThreads < 0 || maxThreads < coreThreads) {
				StringBuilder sb= new StringBuilder(128);
				sb.append("> EPICSPlug maximum number of threads can not be "+maxThreads+". It was changed to ");
				maxThreads = coreThreads;
				sb.append(maxThreads+".");
				getLogger().warn(sb.toString());
			}
		}
		
		if (System.getProperties().containsKey(PROPERTY_READ_ONLY)) {
			readOnly= Boolean.valueOf(System.getProperty(PROPERTY_READ_ONLY, Boolean.FALSE.toString()));
		} else {
			readOnly= Boolean.valueOf(getConfiguration().getProperty(PROPERTY_READ_ONLY, Boolean.FALSE.toString()));
		}
		if (readOnly) {
			getLogger().warn("READ-ONLY CONNECTOR - local changes only, values are not sent to the remote connections!");
		}

	}

	/**
	 * Creates new instance of default connection cache.
	 * Default connector cache uses weak reference, therefore not used connection are automatically removed from
	 * the cache. Also deleted connection are removed, when spotted.
	 *
	 * @return new instance of default connection cache
	 */
	protected ConnectionCache<C> newDefaultConnectionCache() {
		return new DefaultConnectionCache();
	}

	/**
	 * Returns connector type string.
	 *
	 * @return distinguishing type name
	 */
	public abstract String getType();

	
	/**
	 * <p>This method <b>MUST</b> be implemented by plug implementation if plug want to support
	 * default behavior with shared plug instance</p>.
	 *
	 * <p>This method is used by default DAL factories to access plug instances. Implementation may
	 * choose one of the following strategies how this method is implemented:
	 * </p>
	 *
	 * <ul>
	 *   <li><b>Singleton plug:</b> This method always returns the same instance, thus singleton.
	 *   This means that this particular DAL implementation will always use same plug.</li>
	 *   <li><b>Multiple plugs:</b> Implementation may decide to return different plug instance.
	 *   In this case it may be wiser to declare in AbstractFactorySupport this plug to be
	 *   non-shared.</li>
	 * </ul>
	 *
	 * <p> Which strategy will be used could be hard-coded in AbstractFactorySupport or dynamically
	 * decided from application context configuration with AbstractFactory.SHARE_PLUG property.</p>
	 *
	 * @param configuration Properties with configuration, whcih is provided by application context,
	 * which initiated plug construction.
	 * @return new or reused plug instance, depends on plug implementation strategy
	 * @throws org.scictrl.csshell.RemoteException if construction fails.
	 */
	public static AbstractConnector<?> getInstance(Properties configuration)
		throws RemoteException
	{
		throw new RemoteException(null,
		    "This method MUST be implemented by this plug class, if plug instance is shared.");
	}
	
	/**
	 * This method is used all destroy connector instance and releases all connection and resources.
	 * After this call connector is no longer alive.
	 *
	 * @throws java.lang.Exception if shutdown fails.
	 */
	public synchronized void shutdown() throws Exception {
		if (!alive) {
			return;
		}
		alive=false;
		if (executor!=null) {
			executor.shutdown();
	        try {
	            if (!executor.awaitTermination(1, TimeUnit.SECONDS))
	            	executor.shutdownNow();
	        } catch (InterruptedException ie) {  }
		}
	}
	
	/**
	 * <p>isAlive.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * Return active configuration of this plug.
	 *
	 * @return Returns the configuration.
	 */
	public Properties getConfiguration()
	{
		return configuration;
	}
	
	/* (non-Javadoc)
	 * @see org.epics.css.dal.context.Identifiable#isDebug()
	 */
	/**
	 * <p>isDebug.</p>
	 *
	 * @return a boolean
	 */
	public boolean isDebug()
	{
		return debug;
	}

	/**
	 * Returns the Logger used by this plug. This logger logs all exceptions and
	 * printouts within the DAL implementation.
	 *
	 * @return a {@link org.apache.logging.log4j.Logger} object
	 */
	public Logger getLogger() {
		if (logger == null) {
			logger = ConnectorUtilities.getConnectorLogger(getType());
		}
		return logger;
	}
	
	/**
	 * Timer lazy initialization pattern.
	 * @return timer instance.
	 */
	private synchronized Timer getTimer()
	{
		if (timer == null)
			timer = new Timer("SimulatorPlugTimer");

		return timer;
	}
	
	/**
	 * Schedule task for execution.
	 *
	 * @param r ask to be scheduled.
	 * @param delay delay in milliseconds before task is to be executed.
	 * @param rate reschedule perion, if <code>0</code> periodic rescheduling is disabled.
	 * @return <code>TimerTask</code> instance, used to cancel the task scheduling.
	 */
	public TimerTask schedule(Runnable r, long delay, long rate) {
		if (!alive) {
			throw new IllegalStateException("This connector is not alive andy more!");
		}
		ScheduledTask t = new ScheduledTask(r);
		
		if (rate > 0) {
			getTimer().scheduleAtFixedRate(t, delay, rate);
		} else {
			getTimer().schedule(t, delay);
		}
		return t;
	}
	
	/**
	 * <p>getMetaData.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link org.scictrl.csshell.DataType} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 * @throws java.lang.Exception if any.
	 */
	public abstract MetaData getMetaData(String name, DataType type) throws Exception;
	
	/**
	 * <p>getMetaDataAsync.</p>
	 *
	 * @param channel a {@link gov.aps.jca.Channel} object
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 * @throws java.lang.Exception if any.
	 */
	public abstract void getMetaDataAsync(Channel channel, PropertyChangeListener l) throws Exception;

	/**
	 * Gets {@link #coreThreads} property.
	 *
	 * @return the number of core threads.
	 */
	public int getCoreThreads() {
		return coreThreads;
	}

	/**
	 * In order to use this method the {@link #PROPERTY_MAX_THREADS}
	 * must be greater than 0.
	 *
	 * @return a <code>ThreadPoolExecutor</code>
	 * @throws java.lang.IllegalStateException if useCommonExecutor property is set to <code>false</code>
	 * or maximum number of threads is equal to 0.
	 */
	public synchronized ThreadPoolExecutor getExecutor() {
		if (!alive) {
			throw new IllegalStateException("This connector is not alive any more!");
		}
		if (executor==null) {
			synchronized (this) {
				if (maxThreads == 0) throw new IllegalStateException("Maximum number of threads must be greater than 0.");
				if (executor==null) {
					executor= new ThreadPoolExecutor(coreThreads,maxThreads,Long.MAX_VALUE, TimeUnit.NANOSECONDS,
			                new LinkedBlockingQueue<Runnable>());
					executor.prestartAllCoreThreads();
				}				
			}
		}
		return executor;
	}

	/**
	 * Gets {@link #maxThreads} property.
	 *
	 * @return the maximum number of threads.
	 */
	public int getMaxThreads() {
		return maxThreads;
	}
	
	/**
	 * <p>newConnection.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @return a C object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public abstract C newConnection(String name, DataType dataType) throws RemoteException;

	
	/**
	 * <p>getDefaultMetaData.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData getDefaultMetaData(String name, DataType dataType) {
		
		for (String key: metaDataCache.keySet()) {
			
			if (name.contains(key)) {
				return metaDataCache.get(key);
			}
			
		}
		
		String key= dataType.getClass().getName()+"#"+dataType.toString();
		if (metaDataCache.containsKey(key)) {
			return metaDataCache.get(key);
		}
		
		return UNINITIALIZED;
		
	}

	/**
	 * <p>registerDefaultMetaData.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param dataType a {@link org.scictrl.csshell.DataType} object
	 * @param data a {@link org.scictrl.csshell.MetaData} object
	 */
	protected void registerDefaultMetaData(String name, DataType dataType, MetaData data) {
	
		if (name != null) {
			metaDataCache.put(name, data);
		}
		
		if (dataType!=null) {
			String key= dataType.getClass().getName()+"#"+dataType.toString();
			metaDataCache.put(key, data);
		}
		
	}
	
	/**
	 * <p>getValue.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link org.scictrl.csshell.DataType} object
	 * @return a {@link java.lang.Object} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 */
	public abstract Object getValue(String name, DataType type) throws RemoteException;

	/**
	 * Returns connection cache, if it was set ( {@link #setConnectionCache(ConnectionCache)} ).
	 * If no connection cache was set, then it is not used.
	 *
	 * @return connection cache in use, or null
	 */
	public ConnectionCache<C> getConnectionCache() {
		return connectionCache;
	}
	
	/**
	 * Sets connection cache to the connector instance. If not set, then it is not used.
	 * By default connection cache is not set or used.
	 *
	 * @param connectionCache connection cache to be used
	 */
	public void setConnectionCache(ConnectionCache<C> connectionCache) {
		this.connectionCache = connectionCache;
	}
	
	/**
	 * Returns <code>true</code> when this connector is not setting values to underlying remote connections.
	 *
	 * @return <code>true</code> when this connector is not setting values
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
}


/* __oOo__ */
