package org.scictrl.csshell;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;



/**
 * Helper class which helps manage different plug implementations.<p>This
 * class expects specifc keywords in configuration properties, which define
 * which plugs are present and which instances can be used for particular
 * plug.</p>
 *
 * @author igor@scictrl.com
 */
public final class ConnectorUtilities
{
	
	/*
	 * 
	 * STATIC DECLARATIONS, COMMON TO ALL PLUGS OBJECTS
	 * 
	 */
	
	/** 
	 * Keyword used in System properties to configure DAL logging. 
	 * Valie values are:
	 * <ul>
	 * <li>true - DAL will call basic log4j configuration, which enables logging to console.</li>
	 * <li>false - DAL will not configure logging to console.</li>
	 * </ul>
	 * 
	 * By default true is assumed and logging will be configured.
	 * 
	 * <p>
	 * Regardless of this setting an application can configure own appenders for DAL logging.
	 * See {@link ConnectorUtilities#getLogger()} for details.
	 * </p>  
	 *  
	 * @see #getLogger()
	 */
	public static final String CSSHELL_LOGGING = "csshell.logging";

	/**
	 * Optional system configuration property which tells to plug implementations
	 * what is expected timeout for remote operations in milliseconds.
	 */
	public static final String CONNECTION_TIMEOUT = "csshell.connectionTimeout";
	
	/**
	 * Optional system configuration property, which defines the timeout for
	 * establishing or initializing the connections in milliseconds.
	 */
	public static final String INITIAL_CONNECTION_TIMEOUT = "csshell.initialConnectionTimeout";

	/**
	 * DAL default connection timeout value in milliseconds. Used if CONNECTION_TIMEOUT is not defined.
	 */
	public static final long DEFAULT_CONNECTION_TIMEOUT = 30000;
	
	/**
	 * DAL default initial connection timeout value in milliseconds. Used if INITIAL_CONNECTION_TIMEOUT is not defined.
	 */
	public static final long DEFAULT_INITIAL_CONNECTION_TIMEOUT = 30000;

	/**
	 * Convenience method which tries to get connection timeout first from provided properties,
	 * then from system properties and if both fails returns provided default value.
	 *
	 * @param p properties, may be null
	 * @param def default fallback values
	 * @return connection timeout propety value
	 */
	public static final long getConnectionTimeout(Properties p, long def)
	{
		String s = null;

		if (p != null) {
			s = p.getProperty(CONNECTION_TIMEOUT);

			if (s != null) {
				try {
					return Long.parseLong(s);
				} catch (Exception e) {
					LogManager.getLogger(ConnectorUtilities.class).warn("System defined property "+CONNECTION_TIMEOUT+" could not be parsed as long.", e);
				}
			}
		}

		return Long.getLong(CONNECTION_TIMEOUT, def);
	}
	/**
	 * Convenience method which tries to get connection timeout first from provided properties,
	 * then from system properties and if both fails returns DAL default value.
	 *
	 * @param p properties, may be null
	 * @return connection timeout propety value
	 */
	public static final long getConnectionTimeout(Properties p)
	{
		return getConnectionTimeout(p, DEFAULT_CONNECTION_TIMEOUT);
	}
	
	/**
	 * Convenience method which tries to get initial connection timeout first from provided properties,
	 * then from system properties and if both fails returns provided default value.
	 *
	 * @param p properties, may be null
	 * @param def default fallback values
	 * @return connection timeout property value
	 */
	public static final long getInitialConnectionTimeout(Properties p, long def)
	{
		String s = null;

		if (p != null) {
			s = p.getProperty(INITIAL_CONNECTION_TIMEOUT);

			if (s != null) {
				try {
					return Long.parseLong(s);
				} catch (Exception e) {
					LogManager.getLogger(ConnectorUtilities.class).warn("System defined property "+INITIAL_CONNECTION_TIMEOUT+" could not be parsed as long.", e);
				}
			}
		}

		return Long.getLong(INITIAL_CONNECTION_TIMEOUT, def);
	}
	/**
	 * Convenience method which tries to get initial connection timeout first from provided properties,
	 * then from system properties and if both fails returns DAL default value.
	 *
	 * @param p properties, may be null
	 * @return initial connection timeout property value
	 */
	public static final long getInitialConnectionTimeout(Properties p)
	{
		return getInitialConnectionTimeout(p, DEFAULT_INITIAL_CONNECTION_TIMEOUT);
	}
	
	/**
	 * Return logger, which is parent for all DAL plug loggers.
	 *
	 * <p>
	 * DAL Plug loggers collect and distribute messages, which are intended for general plublic.
	 * E.g. application which is not interested in internal structure, but wants to display progress when some channel
	 * was connected or some user initiated action failed.
	 * </p>
	 *
	 * <p>
	 * Parent DAL logger name is 'DAL'. Names of plug loggers are 'DAL.PLUG_NAME', for example 'DAL.EPICS'.
	 * </p>
	 *
	 * <p>
	 * Default configuration of appenders is controlled with System parameter {@link org.scictrl.csshell.ConnectorUtilities#CSSHELL_LOGGING}.
	 * </p>
	 *
	 * @return parent logger for all DAL plug loggers.
	 * @see #CSSHELL_LOGGING
	 * @see #getLogger()
	 */
	public static final Logger getLogger() {
		if (logger == null) {
			logger = LogManager.getLogger("CSSHELL");
			
			boolean log= Boolean.parseBoolean(System.getProperty(CSSHELL_LOGGING, Boolean.FALSE.toString()));
			
			if (log) {
				Configurator.initialize(new DefaultConfiguration());
			} else {
				// supresses log4j warning about nonconfigured logging  
				NullAppender na= NullAppender.createAppender("NULL");
				na.start();
				((org.apache.logging.log4j.core.Logger)logger).addAppender(na);
			}			
		}
		return logger;
	}
	
	/**
	 * Returns logger for particular DAL plug, this logger shuld be used for general messages about plug activity.
	 *
	 * @return logger for DAL plug
	 * @param type a {@link java.lang.String} object
	 */
	public static final Logger getConnectorLogger(String type) {
		getLogger();
		return LogManager.getLogger("CSSHELL."+type);
	}

	
	/**
	 * <p>formatConnectionMessage.</p>
	 *
	 * @param conn a {@link org.scictrl.csshell.Connection} object
	 * @param message a {@link java.lang.String} object
	 * @param t a {@link java.lang.Throwable} object
	 * @return a {@link java.lang.String} object
	 */
	public static final String formatConnectionMessage(Connection<?,?,?> conn, String message, Throwable t) {
		return formatConnectionMessage(conn.getName(), message, t);
	}
	
	/**
	 * <p>formatConnectionMessage.</p>
	 *
	 * @param conn a {@link java.lang.String} object
	 * @param message a {@link java.lang.String} object
	 * @param t a {@link java.lang.Throwable} object
	 * @return a {@link java.lang.String} object
	 */
	public static final String formatConnectionMessage(String conn, String message, Throwable t) {
		StringBuilder sb= new StringBuilder(256);
		sb.append("[");
		sb.append(conn);
		sb.append("] ");
		sb.append(message);
		if (t!=null) {
			sb.append(" (");
			sb.append(t.getClass().getSimpleName());
			sb.append(": ");
			sb.append(t.getMessage());
			sb.append(")");
		}
		return sb.toString();
	}
	
	
	/*
	 * 
	 * INSTANCE SPECIFIC ONLY DECLARATIONS 
	 * 
	 */
	
	private static Logger logger;
	
	private ConnectorUtilities() {
	}
	
}

/* __oOo__ */
