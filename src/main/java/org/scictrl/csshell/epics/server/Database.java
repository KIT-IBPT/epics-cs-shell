/**
 * 
 */
package org.scictrl.csshell.epics.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.dbr.DBR;
import tools.BootstrapLoader;

/**
 * Database is controller type of object for handling Record and Application objects and managing their lifecycle.
 *
 * @author igor@scictrl.com
 */
public class Database {
	
	/** Constant <code>PROPERTY_RECORDS="records"</code> */
	public static final String PROPERTY_RECORDS = "records";
	/** Constant <code>PROPERTY_APPLICATIONS="applications"</code> */
	public static final String PROPERTY_APPLICATIONS = "applications";
		
	/** Constant <code>CONFIG_ACTIVATION_MIN="Activation.min"</code> */
	public static final String CONFIG_ACTIVATION_MIN = "Activation.min";
	/** Constant <code>CONFIG_ACTIVATION_DELAY="Activation.delay"</code> */
	public static final String CONFIG_ACTIVATION_DELAY = "Activation.delay";

	
	private class ActivationTimer {
		
		private long min;
		private long delay;
		private long last=0L;

		public ActivationTimer(long min, long delay) {
			this.min=min;
			this.delay=delay;
		}
		
		public synchronized void checkAndDelay() {
			if (last!=0L) {
			
				long t= System.currentTimeMillis();
				long d=delay;
				
				if (t-last<min) {
					d= d + (min - (t-last));
				}
				
				if (d>0) {
					
					try {
						this.wait(d);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
			}
			
			last=System.currentTimeMillis();
		}
		
	}
	
	private class ScheduledTask extends TimerTask {
		private Runnable r;

		public ScheduledTask(Runnable r) {
			super();
			this.r = r;
		}
		
		@Override
		public void run() {
			if (!active || getExecutor().isShutdown()) {
				return;
			}
			try {
				getExecutor().execute(r);
			} catch (Throwable th) {
				LogManager.getLogger(this.getClass()).warn("Sheduled task had unhandled error.", th);
			}
		}
		
	}
	
	private class ProcessorTask implements Runnable {
		
		private ValueProcessor processor;

		public ProcessorTask(ValueProcessor processor) {
			this.processor=processor;
		}
		
		@Override
		public void run() {
			try {
				processor.process();
			} catch (Exception e) {
				log.warn("Execution failed for "+processor.getName(), e);
			}
		}
	}

	private Map<String,Record> records;
	private Server server;
	
	private ThreadPoolExecutor executor;
	private Timer timer;
	private EPICSConnector connector;
	
	private Logger log= LogManager.getLogger(getClass());
	private boolean active;
	
	private PropertyChangeSupport support;
	private PersistencyStore peristencyStore;
	private Map<String,Application> applications;
	
	private Object layzLock= "LayzLock";
	private ActivationTimer activationTimer;
	
	/**
	 * Crate new instance with reference to a {@link org.scictrl.csshell.epics.server.Server}.
	 *
	 * @param server the {@link org.scictrl.csshell.epics.server.Server} reference, can not be <code>null</code>
	 * @throws org.scictrl.csshell.RemoteException if creation of {@link org.scictrl.csshell.epics.EPICSConnector} fails
	 * @throws org.apache.commons.configuration.ConfigurationException if creation of {@link org.scictrl.csshell.epics.server.PersistencyStore} fails
	 * @throws java.lang.NullPointerException if parameter 'server' is null
	 */
	public Database(Server server) throws RemoteException, ConfigurationException {
		if (server==null) {
			throw new NullPointerException("Parameter 'server' is null!");
		}
		this.server=server;
		records=new HashMap<String, Record>(256);
		support= new PropertyChangeSupport(this);
		applications= new HashMap<String,Application>();
		
		Properties p= new Properties(server.getConfiguration());
		//p.put(EPICSConnector.PROPERTY_VERBOSITY, "0");
		
		connector= EPICSConnector.newInstance(p);
		
		if (server.isEmbedded()) {
			peristencyStore= new PersistencyStore();
		} else {
			String n= server.getConfiguration().getProperty(Server.APPSERVER_PERSISTENCY_FILE,Server.DEFAULT_PERSISTENCY_FILE);
			
			File f;
			if (n.startsWith("/")) {
				f= new File(n);
			} else {
				f= BootstrapLoader.getInstance().getApplicationConfigFile(Server.APPSERVER, n);
			}
			
			peristencyStore= new PersistencyStore(f,this);
		}
		
		activationTimer= new ActivationTimer(
				Long.parseLong(server.getConfiguration().getProperty(CONFIG_ACTIVATION_MIN, "0")),
				Long.parseLong(server.getConfiguration().getProperty(CONFIG_ACTIVATION_DELAY, "0")));
	}
	
	/**
	 * Adds listener to {@link #PROPERTY_RECORDS} and {@link #PROPERTY_APPLICATIONS} events. They are fired when a record or application has been added.
	 *
	 * @param l a listener to {@link #PROPERTY_RECORDS} or {@link #PROPERTY_APPLICATIONS} events
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}
	
	/**
	 * <p>Getter for the field <code>server</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Server} object
	 */
	public Server getServer() {
		return server;
	}
	
	/**
	 * <p>Getter for the field <code>peristencyStore</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.PersistencyStore} object
	 */
	public PersistencyStore getPeristencyStore() {
		return peristencyStore;
	}
	
	/**
	 * <p>Getter for the field <code>executor</code>.</p>
	 *
	 * @return a {@link java.util.concurrent.ThreadPoolExecutor} object
	 */
	public ThreadPoolExecutor getExecutor() {
		if (executor==null) {
			createExecutor();
		}
		return executor;
	}
	
	
	private void createExecutor() {
		synchronized (layzLock) {
			if (executor==null) {
				executor=new ThreadPoolExecutor(10, 20, 6000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
					int count=0;
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r,"AppServerPool-"+(++count));
					}
				});
			}
		}
	}
	
	/**
	 * Schedule execution of Runnable within thread pool.
	 *
	 * @param r the Runnable to be scheduled
	 * @param delay a long
	 */
	public void schedule(Runnable r, long delay) {
		if (timer==null) {
			createtimer();
		}
		
		if (delay<=0) {
			getExecutor().execute(r);
		} else {
			timer.schedule(new ScheduledTask(r), delay);
		}
	}

	/**
	 * Schedule execution of Runnable within thread pool.
	 *
	 * @param r the Runnable to be scheduled
	 * @param delay a long
	 * @param period a long
	 */
	public void schedule(Runnable r, long delay, long period) {
		if (timer==null) {
			createtimer();
		}
		
		if (delay<=0) {
			getExecutor().execute(r);
		} else {
			timer.schedule(new ScheduledTask(r), delay, period);
		}
	}

	private void createtimer() {
		synchronized (layzLock) {
			if (timer == null) {
				timer = new Timer("EPICS Server Timer");
			}
		}
	}
	
	/**
	 * Adds and registers all {@link org.scictrl.csshell.epics.server.Record} instances in array to this database.
	 * If {@link org.scictrl.csshell.epics.server.Record} instance contains an {@link org.scictrl.csshell.epics.server.Application} reference, then also the {@link org.scictrl.csshell.epics.server.Application} is added and registered.
	 * If this database instance has been activated, then also added {@link org.scictrl.csshell.epics.server.Record} and {@link org.scictrl.csshell.epics.server.Application} is being activated.
	 *
	 * @param records array with {@link org.scictrl.csshell.epics.server.Record} instance to be added
	 */
	public void addAll(Record[] records) {
		
		StringBuilder sb= new StringBuilder(records.length*16);
		
		sb.append("Loaded ");
		sb.append(records.length);
		sb.append(" records: ");
		for (Record record : records) {
			
			if (this.records.containsKey(record.getName())) {
				log.error("AppServer already contains record '"+record.getName()+"', ignoring record with duplicate name!");
				continue;
			}
			
			sb.append(record.getName());
			sb.append(", ");
			record.initialize(this);
			this.records.put(record.getName(), record);
			
			Application app= record.getApplication();
			if (app!=null && !applications.containsKey(app.getName())) {
				app.initialize(this);
				applications.put(app.getName(),app);
				support.firePropertyChange(PROPERTY_APPLICATIONS, null, getApplicationNames());
			}
			
			if (active) {
				activateNow(record);
			}
		}
		
		log.info(sb);
		
		support.firePropertyChange(PROPERTY_RECORDS, null, getNames());
	}
	
	/**
	 * <p>addRecord.</p>
	 *
	 * @param record a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public void addRecord(Record record) {
		
		StringBuilder sb= new StringBuilder(128);
		

		if (this.records.containsKey(record.getName())) {
			log.error("AppServer already contains record '"+record.getName()+"', ignoring record with duplicate name!");
			return;
		}
			
		record.initialize(this);
		this.records.put(record.getName(), record);
		
		Application app= record.getApplication();
		if (app!=null && !applications.containsKey(app.getName())) {
			app.initialize(this);
			applications.put(app.getName(),app);
			support.firePropertyChange(PROPERTY_APPLICATIONS, null, getApplicationNames());
		}
		
		if (active) {
			activateNow(record);
		}

		sb.append("Loaded 1 record: ");
		sb.append(record.getName());
		log.info(sb);
		
		support.firePropertyChange(PROPERTY_RECORDS, null, record.getName());
	}

	/**
	 * <p>getRecord.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public Record getRecord(String name) {
		return records.get(name);
	}
	
	/**
	 * Returns {@link org.scictrl.csshell.epics.server.Application} instance with provided name, if registered, otherwise <code>null</code>.
	 *
	 * @param name the name of the {@link org.scictrl.csshell.epics.server.Application} instance
	 * @return {@link org.scictrl.csshell.epics.server.Application} with matching name or <code>null</code>
	 */
	public Application getApplication(String name) {
		return applications.get(name);
	}
	
	/**
	 * Returns <code>true</code> if an {@link org.scictrl.csshell.epics.server.Application} with provided name is registered.
	 *
	 * @param name the name of the {@link org.scictrl.csshell.epics.server.Application} instance
	 * @return <code>true</code> if an {@link org.scictrl.csshell.epics.server.Application} with provided name is registered
	 */
	public boolean hasApplication(String name) {
		return applications.containsKey(name);
	}
	

	/**
	 * Returns <code>true</code> if an {@link org.scictrl.csshell.epics.server.Record} with provided name is registered.
	 *
	 * @param name the name of the {@link org.scictrl.csshell.epics.server.Record} instance
	 * @return <code>true</code> if an {@link org.scictrl.csshell.epics.server.Record} with provided name is registered
	 */
	public boolean hasRecord(String name) {
		return records.containsKey(name);
	}
	
	/**
	 * Return count of registered {@link org.scictrl.csshell.epics.server.Record} instances.
	 *
	 * @return count of registered {@link org.scictrl.csshell.epics.server.Record} instances
	 */
	public int count() {
		return records.size();
	}
	
	/**
	 * Return count of registered {@link org.scictrl.csshell.epics.server.Application} instances.
	 *
	 * @return count of registered {@link org.scictrl.csshell.epics.server.Application} instances
	 */
	public int applicationCount() {
		return applications.size();
	}

	/**
	 * Calls activate on all registered {@link org.scictrl.csshell.epics.server.Record} and {@link org.scictrl.csshell.epics.server.Application} instances.
	 * Called by the server during initialization.
	 */
	public synchronized void activate() {
		if (active==true) {
			return;
		}
		active=true;
		for (Record r : records.values()) {
			activationTimer.checkAndDelay();
			r.activate();
		}
		for (Application app : applications.values()) {
			activationTimer.checkAndDelay();
			app.activate();
		}
		
		for (Record r : records.values()) {
			if (r.getProcessor().getTrigger()>0) {
				schedule(new ProcessorTask(r.getProcessor()), 1000, r.getProcessor().getTrigger());
			}
		}
	}
	
	/**
	 * <p>activationDelay.</p>
	 */
	public void activationDelay() {
		activationTimer.checkAndDelay();
	}
	
	/**
	 * Called by the server during shutdown, deactivates the database.
	 */
	public synchronized void deactivate() {
		if (active==false) {
			return;
		}
		active=false;
		
		getExecutor().shutdown();
		
		/*for (Record r : records.values()) {
			r.deactivate();
		}
		for (Application app : applications) {
			app.deactivate();
		}*/
	}

	/**
	 * <p>activateNow.</p>
	 *
	 * @param r a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public synchronized void activateNow(Record r) {
		if (!active) {
			return;
		}
		
		if (!records.containsKey(r.getName())) {
			return;
		}
		
		activationTimer.checkAndDelay();
		r.activate();
		
		Application a= r.getApplication();
		
		if (a!=null && applications.containsKey(a.getName())) {
			String[] names= a.getRecordNames();
			boolean allIn=true;
			for (String n : names) {
				allIn= allIn && records.containsKey(a.fullRecordName(n));
				if (!allIn) {
					break;
				}
			}
			if (allIn) {
				activationTimer.checkAndDelay();
				a.activate();
			}
		}
		
		if (r.getProcessor().getTrigger()>0) {
			schedule(new ProcessorTask(r.getProcessor()), 1000, r.getProcessor().getTrigger());
		}
	}

	/**
	 * <p>getNames.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getNames() {
		String[] s= records.keySet().toArray(new String[records.size()]);
		return s;
	}

	/**
	 * <p>getApplicationNames.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getApplicationNames() {
		String[] s= applications.keySet().toArray(new String[applications.size()]);
		return s;
	}

	/**
	 * <p>Getter for the field <code>connector</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.EPICSConnector} object
	 */
	public EPICSConnector getConnector() {
		return connector;
	}
	
	/**
	 * <p>recordsIterator.</p>
	 *
	 * @return a {@link java.util.Iterator} object
	 */
	public Iterator<Record> recordsIterator() {
		return records.values().iterator();
	}
	
	/**
	 * Tries to get value for the PV name. It first try to locate local record in Database,
	 * if that fails, then makes remote EPICS get.
	 *
	 * @param pv a {@link java.lang.String} object
	 * @return a {@link java.lang.Object} object
	 */
	public Object getValue(String pv) {
		Record r= getRecord(pv);
		
		if (r!=null) {
			return r.getValue();
		}
		
		Poop<?, DBR> poop;
		try {
			poop = getConnector().getOneShot(pv);
			return poop.getValue();
		} catch (Exception e) {
			log.error("Failed to obtain value for '"+pv+"': "+e.toString(), e);
		}
		
		return null;
		
	}

	/**
	 * Tries to get value for the PV name. It first try to locate local record in Database,
	 * if that fails, then makes remote EPICS get.
	 *
	 * @param pv a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	public String getValueAsString(String pv) {
		Record r= getRecord(pv);
		
		if (r!=null) {
			return r.getValueAsString();
		}
		
		Poop<?, DBR> poop;
		try {
			poop = getConnector().getOneShot(pv);
			return poop.getString();
		} catch (Exception e) {
			log.error("Failed to obtain value for '"+pv+"': "+e.toString(), e);
		}
		
		return null;
		
	}
	
	/**
	 * <p>isActive.</p>
	 *
	 * @return a boolean
	 */
	public boolean isActive() {
		return active;
	}
	
}
