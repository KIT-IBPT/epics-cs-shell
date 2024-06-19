/**
 * 
 */
package tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

/**
 * This class tries to initialize two parameters: bundle.conf and bundle.home.
 * The initialization process has three main steps to determine this folder:
 *
 * <ul>
 * <li>
 * Checks if bundle.conf and bundle.home are provided trough system properties.
 * If they are, search is over.
 * </li>
 * <li>
 * Tries to locate file bundle.properties on classpath.
 * If successful, parameters are loaded from the file or location of bundle.properties is used instead.
 * </li>
 * <li>
 * If nothing of above works, then current folder is used.
 * </li>
 * </ul>
 *
 * @author igor@scictrl.com
 */
public class BootstrapLoader {

	/**
	 * Configuration folder of this bundle/installation.
	 * Usually should be under bundle.home.
	 */
	public static final String BUNDLE_CONF = "bundle.conf";
	/**
	 * Home (installation) folder of this bundle/installation.
	 */
	public static final String BUNDLE_HOME = "bundle.home";
	/**
	 * Logging folder of this bundle/installation.
	 */
	public static final String BUNDLE_LOG = "bundle.log";
	/**
	 * Logging file of this bundle/installation.
	 */
	public static final String BUNDLE_LOG_FILE = "bundle.log.file";
	/**
	 * File name, which could contain bundle_home and bundle_conf.
	 */
	public static final String BUNDLE_CONF_FILE_NAME = "bundle.properties";

	private static BootstrapLoader loader;


	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		Configurator.initialize(new DefaultConfiguration());

		BootstrapLoader b= BootstrapLoader.getInstance();
		
		System.out.println(b.getBundleConfDir());
	}
	
	
	/**
	 * <p>checkLogging.</p>
	 */
	public static final void checkLogging() {
		if (logger!=null) {
			return;
		}
		String file= System.getProperty(BUNDLE_LOG_FILE);
		if (file==null) {
			try {
				System.setProperty(BUNDLE_LOG_FILE, File.createTempFile("bundle", "log").toString() );
			} catch (IOException e) {
				e.printStackTrace();
			}
			LoggerContext.getContext().getConfiguration().getRootLogger().removeAppender("RF");
			logger = LogManager.getLogger(BootstrapLoader.class);
		} else {
			logger = LogManager.getLogger(BootstrapLoader.class);
			logger.info("File logging enabled: "+new File(file).getAbsolutePath());
		}
	}
	
	/**
	 * <p>getInstance.</p>
	 *
	 * @return a {@link tools.BootstrapLoader} object
	 */
	public static final BootstrapLoader getInstance() {
		if (loader == null) {
			loader = new BootstrapLoader(System.getProperties());
		}
		return loader;
	}


	private Properties bootstrapProperties;
	private Properties properties;
	private File bundleConf;
	private static Logger logger;
	private File bundleHome;
	private File bundleFile;
	
	
	/**
	 * Creates instance of bootstrap loader.
	 *
	 * @param bootstrap the bootstrap parameters. System parameters should be used if nothing else is available.
	 */
	public BootstrapLoader(Properties bootstrap) {
		this.bootstrapProperties=new Properties(bootstrap);

		checkLogging();
		/*
		 * Creating logger
		 */

		/* 
		 * try to load bootstrap file
		 */
		InputStream is = findBootstrapConfFile(); 
		
		/*
		 * Create empty properties file used for this bundle.
		 */
		this.properties= new Properties();
		
		/*
		 * If possible load bootstrap properties file.
		 */
		if (is!=null) {
			try {
				properties.load(is);
				logger.info("Loaded: "+this.properties);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Load of bootstrap conf failed, "+e.toString()+".",e);
			}
		}


		/* 
		 * Overload with bootstrap properties, thez should have precedence over loaded from file.
		 */
		if (bootstrap!=null) {
			properties.putAll(bootstrap);
		}
		
		/*
		 * We try to load conf from properties 
		 */
		
		String c= properties.getProperty(BUNDLE_CONF);
		if (c!=null) {
			bundleConf= new File(c);
			if (!bundleConf.isDirectory()) {
				bundleConf= new File(new File("."),c);
				if (!bundleConf.isDirectory()) {
					bundleConf= null;
				}
			}
		}
		
		/* 
		 * Last resort: current working dir.
		 */
		if (bundleConf==null) {
			bundleConf= new File(".");
		}

		/*
		 * Try to calculate absoluth path if defined as relative.
		 */
		if (bundleConf.toString().startsWith(".")) {
			File f=null;
			if (bundleFile!=null) {
				try {
					f= new File(bundleFile.getParentFile(), bundleConf.toString()).getCanonicalFile();
				} catch (IOException e) {
					e.printStackTrace();
					f= new File(bundleFile.getParentFile(), bundleConf.toString()).getAbsoluteFile();
				}
			} 
			if (f==null || !f.exists()) {
				try {
					f= new File(new File("."), bundleConf.toString()).getCanonicalFile();
				} catch (IOException e) {
					e.printStackTrace();
					bundleConf= new File(new File("."), bundleConf.toString()).getAbsoluteFile();
				}
			}
			if (f.exists()) {
				bundleConf=f;
			}
			/*
			 * set back to properties resolved location
			 */
			properties.setProperty(BUNDLE_CONF,bundleConf.getAbsolutePath());
		}
		
		/*
		 * Repeat same procedure for home 
		 */
		
		c= properties.getProperty(BUNDLE_HOME);
		if (c!=null) {
			bundleHome= new File(c);
			if (!bundleHome.isDirectory()) {
				bundleHome= new File(new File("."),c);
				if (!bundleHome.isDirectory()) {
					bundleHome= null;
				}
			}
		}
		if (bundleHome==null) {
			bundleHome= bundleConf;
			properties.setProperty(BUNDLE_HOME,bundleHome.getAbsolutePath());
		}
		if (bundleHome.toString().equals(".")) {
			bundleHome= bundleConf;
			properties.setProperty(BUNDLE_HOME,bundleHome.getAbsolutePath());
		}
		if (bundleHome.toString().startsWith(".")) {
			File f=null;
			if (bundleHome!=null) {
				try {
					f= new File(bundleFile.getParentFile(), bundleHome.toString()).getCanonicalFile();
				} catch (IOException e) {
					e.printStackTrace();
					f= new File(bundleFile.getParentFile(), bundleHome.toString()).getAbsoluteFile();
				}
			} 
			if (f==null || !f.exists()) {
				try {
					f= new File(new File("."), bundleHome.toString()).getCanonicalFile();
				} catch (IOException e) {
					e.printStackTrace();
					bundleHome= new File(new File("."), bundleHome.toString()).getAbsoluteFile();
				}
			}
			if (f.exists()) {
				bundleHome=f;
			}
			
			properties.setProperty(BUNDLE_HOME,bundleHome.getAbsolutePath());
		}
		
		/*
		 * Resolve also all paths in the bootstrap properties.
		 */
		Iterator<Object> keys= properties.keySet().iterator();
		while (keys.hasNext()) {
			String key=keys.next().toString();
			String val = properties.getProperty(key);
			if (val!=null) {
				String val1=val;
				if (val.equals(".")) {
					val1=bundleConf.getAbsolutePath();
				}
				if (val.startsWith("../") || val.startsWith("..\\")) {
					try {
						val1= new File(bundleConf,val).getCanonicalPath();
					} catch (IOException e) {
						e.printStackTrace();
						val1= new File(bundleConf,val).getAbsolutePath();
					}
				}
				if (val.contains("${")) {
					val1= StrSubstitutor.replace(val, properties);
				}
				if (!val.equals(val1)) {
					properties.setProperty(key, val1);
				}
			}
		}
		
		logger.info("Resolved: "+BUNDLE_HOME+"="+properties.getProperty(BUNDLE_HOME)+", "+BUNDLE_CONF+"="+properties.getProperty(BUNDLE_CONF));


		//System.getProperties().putAll(this.properties);
	}
	
	/**
	 * Searches classpath for signs of bundle.properties file.
	 * @return the bundle.properties file strem, if exists
	 */
	private InputStream findBootstrapConfFile() {
		
		/*
		 * Check first if bootstrap parameters provides config location, and check there first
		 */
		String conf= bootstrapProperties.getProperty(BUNDLE_CONF);

		File confF=null;
		
		/*
		 * try to establish first conf folder 
		 */
		if (conf!=null) {
			confF= new File(conf);
			
			logger.info("Bootstrap parameter detected: "+BUNDLE_CONF+"="+conf);
			
		}
		
		if (confF!=null && confF.isDirectory()) {
			/*
			 * Load properties from conf location if exists
			 */
			File f= new File(confF,BUNDLE_CONF_FILE_NAME);
			if (f.exists()) {
				
				logger.info("Bootstrap file found: "+f.toString());
				
				/* 
				 * remeber this as valid conf location
				 */
				bundleFile= f;
				try {
					return new BufferedInputStream(new FileInputStream(f), 262144);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		
		/*
		 * So loading trough CONF bootstrap parameter failed. 
		 * Could be that parameter was there but no file, of no parameter at all.
		 * 
		 * Now we try with classpath option 
		 */
		
		File f= new File(BUNDLE_CONF_FILE_NAME);
		
		if (!f.exists()) {
			/*
			 * If file is not in working dir, ten we request from classpath
			 */
			URL url= this.getClass().getClassLoader().getResource(BUNDLE_CONF_FILE_NAME);
			if (url!=null && url.getProtocol().equals("file")) {
				f= new File(url.getFile());
			}
		}
		
		/*
		 * SO not or it was on class path oin working dir.
		 */
		if (f.exists()) {
			
			logger.info("Bootstrap file found on current path: "+f.getAbsolutePath());
			
			/*
			 * We have found the file, let us remeber the conf folder. 
			 */
			bundleFile= f.getAbsoluteFile();
			
			try {
				return new BufferedInputStream(new FileInputStream(f), 262144);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		/*
		 * Another try to load trough classloader, this time file could be in JAR.
		 */
		InputStream is= this.getClass().getClassLoader().getResourceAsStream(BUNDLE_CONF_FILE_NAME);
		
		if (is==null) {
			is= ClassLoader.getSystemClassLoader().getResourceAsStream(BUNDLE_CONF_FILE_NAME);
		}

		if (is!=null) {
			logger.info("Bootstrap file loaded from classpath.");
			return is;
		}

		logger.error("Bootstrap config file '"+BUNDLE_CONF_FILE_NAME+"' discovery failed.");

		return is;
	}
	
	/**
	 * <p>getBundleConfDir.</p>
	 *
	 * @return a {@link java.io.File} object
	 */
	public File getBundleConfDir() {
		return bundleConf;
	}
	
	/**
	 * <p>getBundleHomeDir.</p>
	 *
	 * @return a {@link java.io.File} object
	 */
	public File getBundleHomeDir() {
		return bundleHome;
	}
	
	/**
	 * <p>getProperty.</p>
	 *
	 * @param key a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * <p>getProperty.</p>
	 *
	 * @param key a {@link java.lang.String} object
	 * @param def a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	public String getProperty(String key, String def) {
		return properties.getProperty(key,def);
	}
	
	/**
	 * <p>getApplicationConfigFile.</p>
	 *
	 * @param appName a {@link java.lang.String} object
	 * @param appConfFile a {@link java.lang.String} object
	 * @return a {@link java.io.File} object
	 */
	public File getApplicationConfigFile(String appName, String appConfFile) {
		if (bundleConf==null) {
			return null;
		}
		
		File f= new File(getApplicationConfigFolder(appName),appConfFile);
		
		return f;
	}

	/**
	 * <p>getApplicationConfigFolder.</p>
	 *
	 * @param appName a {@link java.lang.String} object
	 * @return a {@link java.io.File} object
	 */
	public File getApplicationConfigFolder(String appName) {
		if (bundleConf==null) {
			return null;
		}
		
		String fName= properties.getProperty(appName+"/",appName);

		File f= new File(bundleConf,fName);
		
		return f;
	}

	/**
	 * <p>getApplicationConfigProperties.</p>
	 *
	 * @param appName a {@link java.lang.String} object
	 * @param appConfFile a {@link java.lang.String} object
	 * @return a {@link java.util.Properties} object
	 * @throws java.io.IOException if any.
	 */
	public Properties getApplicationConfigProperties(String appName, String appConfFile) throws IOException {
		File f= getApplicationConfigFile(appName, appConfFile);
		
		Properties p= new Properties();
		if (f!=null && f.exists()) {
			Reader r= new BufferedReader(new FileReader(f));
			try {
				p.load(r);
			} finally {
				if (r!=null) {
					r.close();
				}
			}
		}
		
		return p;
	}
	/**
	 * Substitutes (replaces) occurrences of property macros (example ${property.name}) with values
	 * if they are available in bundle or System properties.
	 *
	 * @param str a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	public String substitute(String str) {
		return StrSubstitutor.replace(str, properties);
		
	}
	
	/**
	 * <p>loadIcon.</p>
	 *
	 * @param icon a {@link java.lang.String} object
	 * @return a {@link javax.swing.ImageIcon} object
	 */
	public ImageIcon loadIcon(String icon) {
		if (icon!=null) {
			File f= new File(icon);
			if (!f.exists()) {
				f= new File(".",icon);
			}
			if (!f.exists()) {
				f= new File(bundleHome,icon);
			}
			if (!f.exists()) {
				f= new File(bundleConf,icon);
			}
			if (f.exists()) {
				return new ImageIcon(f.toString());
			} else {
				URL url= ClassLoader.getSystemClassLoader().getResource(icon);
				if (url!=null) {
					return new ImageIcon(url);
				}
			}
		}
		return null;
	}
}
