/**
 * 
 */
package org.scictrl.csshell.epics.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor;
import org.w3c.dom.Document;

import com.cosylab.epics.caj.cas.util.DefaultServerImpl;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ProcessVariable;
import gov.aps.jca.cas.ProcessVariableAttachCallback;
import gov.aps.jca.cas.ProcessVariableEventCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCallback;
import gov.aps.jca.cas.ProcessVariableExistanceCompletion;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;
import tools.BootstrapLoader;

/**
 * <p>Server class.</p>
 *
 * @author igor@scictrl.com
 */
public class Server extends DefaultServerImpl implements Runnable {

	/** Constant <code>APPSERVER="AppServer"</code> */
	public static final String APPSERVER = "AppServer";

	/**
	 * System property name, which is first checked, when server starts. 
	 * Provides name of file with server initialization properties, which defines which serve to run 
	 * and which configuration to load.    
	 */
	public static final String APPSERVER_INIT_FILE = "AppServer.init";

	/**
	 * System property name usually provided trough init file, which define which input file with runtime 
	 * configuration to load into app server. This property is mandatory, if not provided, server will not start.
	 */
	public static final String APPSERVER_INPUT = "AppServer.input";
	
	/**
	 * System property name which defines XML file name into which CSS Beast alarm server configuration is exported.#
	 * IF this property is missing, then alarm export will be disabled.
	 */
	public static final String APPSERVER_ALARM_EXPORT = "AppServer.alarmExport";
	/** Constant <code>APPSERVER_ALARM_CONFIG_NAME="AppServer.alarmConfigName"</code> */
	public static final String APPSERVER_ALARM_CONFIG_NAME = "AppServer.alarmConfigName";
	/** Constant <code>APPSERVER_CONFIG_NAME="AppServer.configName"</code> */
	public static final String APPSERVER_CONFIG_NAME = "AppServer.configName";
	/** Constant <code>APPSERVER_PASSPHRASE="AppServer.passphrase"</code> */
	public static final String APPSERVER_PASSPHRASE = "AppServer.passphrase";
	
	/**
	 * System property name which instructs app server only to export alarm configuration and
	 * then exit. This functionality is used when only conversion of alarm configuration formats is required.
	 */
	public static final String APPSERVER_EXPORT_ONLY = "AppServer.exportOnly";
	/** Constant <code>APPSERVER_PERSISTENCY_FILE="AppServer.persistencyFile"</code> */
	public static final String APPSERVER_PERSISTENCY_FILE = "AppServer.persistencyFile";

	/** Constant <code>DEFAULT_ALARM_CONFIG_NAME="ANKA Machine"</code> */
	public static final String DEFAULT_ALARM_CONFIG_NAME = "ANKA Machine";
	/** Constant <code>DEFAULT_CONFIG_NAME="default"</code> */
	public static final String DEFAULT_CONFIG_NAME = "default";
	/** Constant <code>DEFAULT_PASSPHRASE="please"</code> */
	public static final String DEFAULT_PASSPHRASE = "please";
	/** Constant <code>DEFAULT_PERSISTENCY_FILE="var/persistency.xml"</code> */
	public static final String DEFAULT_PERSISTENCY_FILE = "var/persistency.xml";
	
	/**
	 * Default value taken for system AppServer.input (APPSERVER_INIT_FILE {@link Server#APPSERVER_INIT_FILE}). 
	 * If no AppServer.init parameter was provided, then this file name will be searched for and loaded.
	 */
	public static final String DEFAULT_INIT_FILE = "AppServer.properties";

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		BootstrapLoader.checkLogging();
		final Logger log= LogManager.getLogger(Server.class);
		
		if (log.isDebugEnabled()) {
			Properties p= System.getProperties();
			StringWriter sw= new StringWriter();
			p.list(new PrintWriter(sw));
			log.debug("PROPERTIES: "+sw.toString());
			
			p= new Properties();
			p.putAll(System.getenv());
			sw= new StringWriter();
			p.list(new PrintWriter(sw));
			log.debug("ENV: "+sw.toString());
		}
		
		try {
			
			//BasicConfigurator.configure();
			
			String init= System.getProperty(APPSERVER_INIT_FILE, DEFAULT_INIT_FILE);
			
			File f= BootstrapLoader.getInstance().getApplicationConfigFile(APPSERVER, init);
			
			Reader fr= new FileReader(f);
			
			Properties prop= new Properties();
			
			prop.load(fr);
			
			fr.close();
			
			log.info("Initialized from "+init);

			// this way system defined property will override the one from file.
			prop.putAll(System.getProperties());
			
			boolean exportOnly= Boolean.parseBoolean(prop.getProperty(APPSERVER_EXPORT_ONLY, Boolean.FALSE.toString()));
			
			
			String input= prop.getProperty(APPSERVER_INPUT);
			
			if (input==null) {
				throw new IOException("No value provided for property "+APPSERVER_INPUT+"!");
			}
			
			File fInput= new File(input);
			
			if (!fInput.exists()) {
				fInput= BootstrapLoader.getInstance().getApplicationConfigFile(APPSERVER, input);
				if (!fInput.exists()) {
					throw new IOException("Input file '"+input+"' does not exist!");
				}
			}

			String configName= prop.getProperty(APPSERVER_CONFIG_NAME,DEFAULT_CONFIG_NAME);

			// If no alarm export file is defined, then alarm configuration is not exported
			String alarmExport= prop.getProperty(APPSERVER_ALARM_EXPORT);
			boolean alarmOn=alarmExport!=null;
			
			File fAlarmExport=null;
			Document alarmDoc=null;
			String alarmConfigName= prop.getProperty(APPSERVER_ALARM_CONFIG_NAME,DEFAULT_ALARM_CONFIG_NAME);
			
			if (alarmOn) {
				if (alarmExport.startsWith("/")) {
					fAlarmExport= new File(alarmExport);
				} else {
					fAlarmExport= BootstrapLoader.getInstance().getApplicationConfigFile(APPSERVER, alarmExport);
				}

				DocumentBuilderFactory dbf= DocumentBuilderFactory.newInstance();
				DocumentBuilder db= dbf.newDocumentBuilder();
				alarmDoc = db.newDocument();
			}
			
			ConfigurationVisitor visitor= new ConfigurationVisitor(configName, alarmDoc);
					
			Record[] r= ConfigurationManager.loadConfig(fInput.toString(), visitor);

			log.info("Alarm paths: "+Arrays.toString(visitor.getAlarmPaths()));
			
			if (alarmOn && alarmDoc.getDocumentElement()!= null ) {
				alarmDoc.getDocumentElement().setAttribute("name", alarmConfigName);
				
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer t = tf.newTransformer();
				DOMSource source = new DOMSource(alarmDoc);
				StreamResult result = new StreamResult(new File(fAlarmExport.toString()));
				t.transform(source, result); 
	
				log.info("Alarm export: "+fAlarmExport.toString());
			}
			
			if (exportOnly) {
				log.info("Alarm export only requested, closing.");
				return;
			}
			
			final Server server= new Server(prop);
			server.setInputData(visitor);
			server.getDatabase().addAll(r);
			
			server.activate();
			
			log.info("Server is active.");
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					
					if (server.isActive()) {
						log.info("Shutdown initiated from shutdown hook.");
						server.destroy();
					}
				}
			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
			
			log.fatal("Server initialization failed!", e);
			
			System.exit(1);
		}
		
		
	}
	
	
	private Database database;
	private boolean active;
	private Thread deamon;
	
	Logger log= LogManager.getLogger(getClass());
	private ServerContext serverContext;
	private Properties configuration;
	private ConfigurationVisitor inputData;
	private boolean embedded= false;
	
	/**
	 * <p>Constructor for Server.</p>
	 *
	 * @throws org.scictrl.csshell.RemoteException if initialization of access to EPICS fails
	 * @throws org.apache.commons.configuration.ConfigurationException configuration fail
	 */
	public Server() throws RemoteException, ConfigurationException {
		this(System.getProperties(),false);
	}
	
	/**
	 * <p>Constructor for Server.</p>
	 *
	 * @param prop a {@link java.util.Properties} object
	 * @throws org.scictrl.csshell.RemoteException if any.
	 * @throws org.apache.commons.configuration.ConfigurationException if any.
	 */
	public Server(Properties prop) throws RemoteException, ConfigurationException {
		this(prop,false);
	}
	
	/**
	 * <p>Constructor for Server.</p>
	 *
	 * @throws org.scictrl.csshell.RemoteException if initialization of access to EPICS fails
	 * @throws org.apache.commons.configuration.ConfigurationException configuration fail
	 * @param embedded a boolean
	 */
	public Server(boolean embedded) throws RemoteException, ConfigurationException {
		this(System.getProperties(), embedded);
	}
	
	/**
	 * <p>Constructor for Server.</p>
	 *
	 * @param prop a {@link java.util.Properties} object
	 * @param embedded a boolean
	 * @throws org.scictrl.csshell.RemoteException if any.
	 * @throws org.apache.commons.configuration.ConfigurationException if any.
	 */
	public Server(Properties prop, boolean embedded) throws RemoteException, ConfigurationException {
		this.embedded=embedded;
		configuration= prop;
		database=new Database(this);
		
	}

	/**
	 * <p>isEmbedded.</p>
	 *
	 * @return a boolean
	 */
	public boolean isEmbedded() {
		return embedded;
	}
	
	/**
	 * <p>Getter for the field <code>inputData</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor} object
	 */
	public ConfigurationVisitor getInputData() {
		return inputData;
	}
	
	/**
	 * <p>Setter for the field <code>inputData</code>.</p>
	 *
	 * @param inputData a {@link org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor} object
	 */
	public void setInputData(ConfigurationVisitor inputData) {
		this.inputData = inputData;
	}
	
	/**
	 * <p>Getter for the field <code>configuration</code>.</p>
	 *
	 * @return a {@link java.util.Properties} object
	 */
	public Properties getConfiguration() {
		return configuration;
	}

	/**
	 * <p>isActive.</p>
	 *
	 * @return a boolean
	 */
	public boolean isActive() {
		return active;
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void run() {
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>activate.</p>
	 *
	 * @throws gov.aps.jca.CAException if any.
	 */
	public void activate() throws CAException {
		
		DefaultConfiguration config = new DefaultConfiguration("EPICSPlugConfig");
	    config.setAttribute("class", JCALibrary.CHANNEL_ACCESS_SERVER_JAVA);
	    
		// create context
	    serverContext = JCALibrary.getInstance().createServerContext(config, this);
	    
		// register all context listeners
	    serverContext.addContextExceptionListener(new ContextExceptionListener() {
			
			@Override
			public void contextVirtualCircuitException(
					ContextVirtualCircuitExceptionEvent ev) {
				System.out.println(ev);
			}
			
			@Override
			public void contextException(ContextExceptionEvent ev) {
				System.out.println(ev);
			}
		});
	    serverContext.addContextMessageListener(new ContextMessageListener() {
			
			@Override
			public void contextMessage(ContextMessageEvent ev) {
				System.out.println(ev);
			}
		});

		
		
		if (deamon==null) {
			deamon= new Thread(this,"AppServerDeamon");
			deamon.setDaemon(true);
			deamon.start();
		}
		
		active=true;
		
		getDatabase().activate();
	}
	
	/**
	 * <p>Getter for the field <code>database</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.Database} object
	 */
	public Database getDatabase() {
		return database;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public ProcessVariableExistanceCompletion processVariableExistanceTest(
			String aliasName, InetSocketAddress clientAddress,
			ProcessVariableExistanceCallback asyncCompletionCallback)
			throws CAException, IllegalArgumentException, IllegalStateException {
		synchronized (pvs)
		{
			if (!active) return ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
			
			@SuppressWarnings("unused")
			String fieldName=null;
			String recordName=null;
			if (aliasName!=null  && aliasName.length()>4) {
				if (aliasName.charAt(aliasName.length()-4)=='.') {
					fieldName=aliasName.substring(aliasName.length()-3);
					recordName=aliasName.substring(0,aliasName.length()-4);
				} else if (aliasName.charAt(aliasName.length()-5)=='.') {
					fieldName=aliasName.substring(aliasName.length()-4);
					recordName=aliasName.substring(0,aliasName.length()-5);
				}
			}
			
			if (pvs.containsKey(aliasName)) {
				//log.info("PV "+aliasName+" requested from "+clientAddress.getHostName());
				return ProcessVariableExistanceCompletion.EXISTS_HERE;
			}
			
			if (database.hasRecord(aliasName)) {
				//log.info("PV "+aliasName+" requested from "+clientAddress.getHostName());
				return ProcessVariableExistanceCompletion.EXISTS_HERE;
			}
			
			//if (aliasName.startsWith("A:TEST")) log.info("PV "+aliasName+" requested from "+clientAddress.getHostName());

			if (recordName!=null && pvs.containsKey(recordName)) {
				log.info("PV "+aliasName+" requested from "+clientAddress.getHostName()+", but we have that only as record!!!");
			}
			if (recordName!=null && database.hasRecord(recordName)) {
				log.info("PV "+aliasName+" requested from "+clientAddress.getHostName()+", but we have that only as record!!!");
			}

			return ProcessVariableExistanceCompletion.DOES_NOT_EXIST_HERE;
		}

	}
	
	/** {@inheritDoc} */
	@Override
	public ProcessVariable processVariableAttach(String aliasName,
			ProcessVariableEventCallback eventCallback,
			ProcessVariableAttachCallback asyncCompletionCallback)
			throws CAStatusException, IllegalArgumentException,
			IllegalStateException {
		
		synchronized (pvs)
		{
			ProcessVariable pv = (ProcessVariable)pvs.get(aliasName);
			
			if (pv==null) {
				if (database.hasRecord(aliasName)) {
					Record r= database.getRecord(aliasName);
					
					pv= new ProcessVariable4Record(r,eventCallback);
					registerProcessVariable(pv);
				}
			}
			
			if (pv != null)
			{
				// set PV if not yet set
				if (pv.getEventCallback() == null)
					pv.setEventCallback(eventCallback);
				
				log.debug("PV "+aliasName+" attached");
				return pv;
			}
			else
				throw new CAStatusException(CAStatus.NOSUPPORT, "PV does not exist");
		}
		
	}
	
	/**
	 * <p>requestShutdown.</p>
	 *
	 * @param passphrase a {@link java.lang.String} object
	 * @return a boolean
	 */
	public boolean requestShutdown(String passphrase) {
		String pass= configuration.getProperty(APPSERVER_PASSPHRASE, DEFAULT_PASSPHRASE);
		
		if (pass.equals(passphrase)) {
			destroy();
			return true;
		}
		return false;
	}
	
	/**
	 * <p>destroy.</p>
	 */
	public synchronized void destroy() {
		if (!active) {
			return;
		}
		
		active=false;
		
		database.getPeristencyStore().saveAll();
		
		database.deactivate();
		
		try {
			serverContext.shutdown();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (CAException e) {
			e.printStackTrace();
		}
		try {
			serverContext.destroy();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (CAException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Number of PV connections made to this server
	 *
	 * @return nuber of PV connections to this server
	 */
	public int pvCount() {
		return pvs.size();
	}
	
	/**
	 * <p>recordCount.</p>
	 *
	 * @return a int
	 */
	public int recordCount() {
		return database.count();
	}
	
}
