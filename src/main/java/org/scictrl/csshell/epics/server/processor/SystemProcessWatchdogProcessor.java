/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>SystemProcessWatchdogProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class SystemProcessWatchdogProcessor extends DefaultAlarmProcessor {

	class StreamBufferer extends Thread {
		@SuppressWarnings("unused")
		private InputStream is;
		private BufferedReader re;
		private StringBuilder sb;

		public StreamBufferer(InputStream is) {
			this.is=is;
			this.re=new BufferedReader(new InputStreamReader(is));
			this.sb=new StringBuilder();
		}
		
		@Override
		public void run() {
			String in=null;
			try {
				while ((in=re.readLine())!=null) {
					sb.append(in);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public String toString() {
			return sb.toString();
		}
		
		
	}
	private static final String CMD_LIN= "ps aux";
	private static final String CMD_WIN= "cmd.exe /C tasklist.exe";

	
	private Severity severityHit;
	private Status statusHit;
	private Severity severityMiss;
	private Status statusMiss;
	private String processPattern;
	private int processCount=1;
	private String command;
	
	
	private Logger log = LogManager.getLogger(this.getClass());

	/**
	 * <p>Constructor for SystemProcessWatchdogProcessor.</p>
	 */
	public SystemProcessWatchdogProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		String severity= config.getString("valueHit.severity", Severity.NO_ALARM.getName());
		String status= config.getString("valueHit.status", Status.NO_ALARM.getName());
		
		severityHit = Severity.forName(severity);
		statusHit= Status.forName(status);
		
		severity= config.getString("valueMiss.severity", Severity.NO_ALARM.getName());
		status= config.getString("valueMiss.status", Status.NO_ALARM.getName());
		
		severityMiss = Severity.forName(severity);
		statusMiss= Status.forName(status);

		processPattern= config.getString("processPattern");
		processCount= config.getInt("processCount",1);
		
		if (processPattern==null) {
			throw new NullPointerException("No process pattern!");
		}

		this.fixed=false;
		
		_setValue(false,null,null,false,true);
	
	}

	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		super.propertyChange(evt);
		
	}
	
	private String getCommand() {
		if (command==null) {
			String os= System.getProperty("os.name");
			if (os.toLowerCase().contains("win")) {
				command=CMD_WIN;
			} else {
				command=CMD_LIN;
			}
			log.info("OS:'"+os+"' CMD:'"+command+"'");
		}
		return command;
	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		super.process();

		updateAlarmStatus();
		
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void updateAlarmStatus() {
		super.updateAlarmStatus();
		
		Runtime r= Runtime.getRuntime();
		try {
			
			Process p= r.exec(getCommand());
			
			StreamBufferer sb= new StreamBufferer(p.getInputStream());
			StreamBufferer sbe= new StreamBufferer(p.getErrorStream());

			sb.start();
			sbe.start();

			p.waitFor();

			//System.out.println("OUTPUT "+sb.toString());
			//System.out.println("ERROR "+sbe.toString());

			String output= sb.toString();

			int idx=0;
			int pos=0;
			int hits= 0;

			while ((idx=output.indexOf(processPattern,pos))>-1) {
				hits++;
				pos=idx+processPattern.length();
				log.debug("Process pattern found "+hits+" times, needed "+processCount+".");
				if (processCount==hits) {
					log.debug("Process pattern positive.");
					updateHit(true);
					break;
				}
			}
			if (idx<0) {
				updateHit(false);
			}
			
		} catch (Exception e) {
			log.error("Command failed: "+e.toString(), e);
			//e.printStackTrace();
			update(true,Severity.INVALID_ALARM, Status.READ_ALARM, true);
		}

	}


	private void updateHit(boolean b) {
		if (b!=getValueAsBoolean()) {
			log.info("Process pattern positive: "+b+".");
		}
		
		if (b) {
			update(b,severityHit, statusHit, true);
		} else {
			update(b,severityMiss, statusMiss, true);
		}
	}
	
}
