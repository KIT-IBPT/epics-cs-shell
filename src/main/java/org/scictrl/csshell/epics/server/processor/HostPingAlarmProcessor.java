/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * This class listens to alarm events from input links (PVs) and
 * converts them to alarm state of this record in following way:
 *
 * <ul>
 * <li>
 * Sets record alarm state to highest alarm state of input links.
 * </li>
 * <li>
 * Sets record value to 0 if there is no alarm state and 1 if there is alarm state in input links.
 * </li>
 * <li>
 * Sets record listens to machine state and blocks alarms when machine state does not match mask.
 * </li>
 * </ul>
 *
 * @author igor@scictrl.com
 */
public class HostPingAlarmProcessor extends DefaultAlarmProcessor {

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		try {
			
			String h= "flute-motcon-maxnet28.anka-flute.kit.edu";
			InetAddress ad= InetAddress.getByName(h);
			
			boolean b= ad.isReachable(10000);
			
			System.out.println(b);
			
			Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 "+h);
		    int returnVal = p1.waitFor();
		    b = (returnVal==0);
			System.out.println(b);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static final long DEFAULT_PING_TRIGGER = 1000;
	private static final long DEFAULT_TIMEOUT = 60000;
	private String hostName;
	private int timeout;
	private boolean external=false;

	/**
	 * <p>Constructor for HostPingAlarmProcessor.</p>
	 */
	public HostPingAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		
		super.configure(record, config);

		hostName= config.getString("host");

		timeout= config.getInt("timeout", (int)DEFAULT_TIMEOUT);
		external= config.getBoolean("external", false);

		if (trigger<1) {
			trigger=DEFAULT_PING_TRIGGER;
		}
		
		try {
			update(false,Severity.NO_ALARM,Status.NO_ALARM,false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();
		
		long start= System.currentTimeMillis();
		InetAddress host;
		try {
			host= InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			log.debug("Failed to resolve host for '"+hostName+"'!", e);
			try {
				update(true,Severity.INVALID_ALARM,Status.LINK_ALARM,true);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			//System.out.println("Ping "+hostName+" unresolved "+(System.currentTimeMillis()-start));
			return;
		}

		boolean reach=isReachable(host, timeout);
		
		log.debug("Ping "+hostName+" "+reach+" "+(System.currentTimeMillis()-start));
		
		if (reach) {
			try {
				update(false,Severity.NO_ALARM,Status.NO_ALARM,true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				update(true,Severity.MAJOR_ALARM,Status.LINK_ALARM,true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean isReachable(InetAddress host, int timeout) {
		
		if (external) {
			try {
				Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 "+host.getHostAddress());
			    int r = p.waitFor();
			    return r==0;
			} catch (Exception e) {
				log.debug("Failed to ping '"+hostName+"': "+e.toString());
			}
			return false;
		}
		
		try {
			boolean b = host.isReachable(timeout);
			return b;
		} catch (IOException e) {
			log.debug("Failed to ping '"+hostName+"': "+e.toString());
		}

		return false;
	}
	

}
