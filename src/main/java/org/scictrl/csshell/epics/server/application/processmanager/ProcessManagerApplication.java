/**
 * 
 */
package org.scictrl.csshell.epics.server.application.processmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.application.processmanager.PSPool.PSProc;
import org.scictrl.csshell.epics.server.processor.CommandValueProcessor;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;

/**
 * <p>ProcessManagerApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class ProcessManagerApplication extends AbstractApplication implements PropertyChangeListener {

	/** Constant <code>START="Cmd:Start"</code> */
	public static final String START = "Cmd:Start";
	/** Constant <code>STOP="Cmd:Stop"</code> */
	public static final String STOP = "Cmd:Stop";
	//public static final String UPGRADE = "Cmd:Upgrade";
	//public static final String RESTART = "Cmd:Restart";
	/** Constant <code>KILL="Cmd:Kill"</code> */
	public static final String KILL = "Cmd:Kill";
	/** Constant <code>ERROR="Status:Error"</code> */
	public static final String ERROR = "Status:Error";
	/** Constant <code>RUNNING="Status:Running"</code> */
	public static final String RUNNING = "Status:Running";
	/** Constant <code>ERROR_MESSAGE="Status:ErrorMessage"</code> */
	public static final String ERROR_MESSAGE = "Status:ErrorMessage";
	/** Constant <code>STATE="Status:State"</code> */
	public static final String STATE = "Status:State";
	/** Constant <code>COMPLETION="Status:Completion"</code> */
	public static final String COMPLETION = "Status:Completion";
	/** Constant <code>HOST="Info:Host"</code> */
	public static final String HOST = "Info:Host";
	/** Constant <code>LOCATION="Info:Location"</code> */
	public static final String LOCATION = "Info:Location";
	/** Constant <code>USER="Info:User"</code> */
	public static final String USER = "Info:User";
	/** Constant <code>PID="Info:PID"</code> */
	public static final String PID = "Info:PID";
	/** Constant <code>SERVICE="Info:Service"</code> */
	public static final String SERVICE = "Info:Service";
	/** Constant <code>COMMAND="Info:Command"</code> */
	public static final String COMMAND = "Info:Command";
	/** Constant <code>DESCRIPTION="Info:Description"</code> */
	public static final String DESCRIPTION = "Info:Description";
	//public static final String SUMMARY = "Info:Summary";
	/** Constant <code>RELAY="Info:Relay"</code> */
	public static final String RELAY = "Info:Relay";
	/** Constant <code>CPU="CPU"</code> */
	public static final String CPU = "CPU";
	/** Constant <code>MEM="MEM"</code> */
	public static final String MEM = "MEM";
	
	
	private static final String STR_OK = "OK";
	
	/**
	 * State of process.
	 */
	public static enum State {
		/**
		 * Starting
		 */
		STARTING,
		/**
		 * Running.
		 */
		RUNNING,
		/**
		 * Stopping.
		 */
		STOPPING,
		/**
		 * Stopped
		 */
		STOPPED,
		/**
		 * Restarting.
		 */
		RESTARTING,
		/**
		 * Upgrading
		 */
		UPGRADING,
		/**
		 * Process is being killed.
		 */
		KILLING;
		
		/**
		 * Returns labels;
		 * @return labels
		 */
		public static String[] labels() {
			State[] val= State.values();
			String[] s= new String[val.length];
			for (int i = 0; i < s.length; i++) {
				s[i]=val[i].name();
			}
			return s;
		}

		/**
		 * State value, ordinal position.
		 * @return ordinal position
		 */
		public short value() {
			return (short)ordinal();
		}
	}
	
	class StreamBufferer extends Thread {
		private InputStream is;
		private StringBuilder sb;

		public StreamBufferer(InputStream is) {
			this.is=is;
			this.sb=new StringBuilder();
		}
		
		@Override
		public void run() {
			byte[] buff = new byte[1024];
			try {
				while(is.available() > 0)
				{
					int i = is.read(buff, 0, 1024);
					if(i < 0)
						break;
					sb.append(new String(buff, 0, i));
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

	
	private class CmdExecutor implements Runnable {
		final String cmd;
		final CommandValueProcessor proc;
		private StreamBufferer errorOut;
		private StreamBufferer stdOut;
		private boolean timeout=false;
		private boolean done=false;
		private long time;
		
		public CmdExecutor(String cmd, CommandValueProcessor proc) {
			this.cmd=cmd;
			this.proc=proc;
		}
		
		void relayCommand() throws JSchException, IOException {
			
			time= System.currentTimeMillis();
			
			//Set up the connection parameters and connect.
			JSch jsch = new JSch();
			String key=System.getProperty("user.home")+"/"+privateKey;
			jsch.addIdentity(key);
			Session session = jsch.getSession(user, host, 22);
			//session.setPassword("ankacoop");
			//session.setPassword(parameters.getPassword());
			session.setConfig(config);
			//session.setX11Host("127.0.0.1");
			//session.setX11Port(6000);
			
			session.connect();
			
			Channel channel;
			
			//Set up the command execution channel and connect.
			channel = session.openChannel("exec");
			ChannelExec exec=(ChannelExec)channel; 
			exec.setCommand(cmd);
			errorOut = new StreamBufferer(exec.getErrStream());
			errorOut.start();
			
			stdOut= new StreamBufferer(channel.getInputStream());
			stdOut.start();
			
			//BufferedWriter out = new BufferedWriter(new OutputStreamWriter(channel.getOutputStream()));

			channel.connect();
			
			long start=System.currentTimeMillis();
			
			while (!channel.isClosed()) {
			//if (debug) System.out.println("in "+inB.toString());
				int w= 100;
				try {
					Thread.sleep(w);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (System.currentTimeMillis()-start>proc.getTimeout()) {
					timeout=true;
					channel.disconnect();
				}
			}
					
			time= System.currentTimeMillis()-time;

			int exitCode= channel.getExitStatus();

			done=exitCode==0;
			
			exec.getErrStream().close();
			channel.getInputStream().close();
			channel.disconnect();
			session.disconnect();
			
		}
		
		public void localCommand() throws IOException, InterruptedException {
			
			time= System.currentTimeMillis();

			String[] args= cmd.split("\\s");
			ProcessBuilder pb= new ProcessBuilder(args);
				//pb.directory(null);
				//pb.environment().clear();
				//Map<String, String> m= pb.environment();
				
				/*List<String> l= new ArrayList<String>();
				for (String s : m.keySet()) {
					if (s.startsWith("AppServer.")) {
						l.add(s);
					}
				}*/
				
				/*for (String s : l) {
					m.remove(s);
				}*/
				
				/*Properties pr= new Properties();
				pr.putAll(pb.environment());
				StringWriter sw= new StringWriter();
				pr.list(new PrintWriter(sw));
				log.info("ENV: "+sw.toString());*/

			Process p= pb.start();
				
			@SuppressWarnings("unused")
			boolean done= p.waitFor(proc.getTimeout(), TimeUnit.MILLISECONDS);
				
			time= System.currentTimeMillis()-time;
			
			errorOut = new StreamBufferer(p.getErrorStream());
			stdOut = new StreamBufferer(p.getInputStream());
				
			proc.setValue(0);

			int exitCode= p.exitValue();

			timeout=time>proc.getTimeout();
			
			p.getErrorStream().close();
			p.getInputStream().close();
			p.getOutputStream().close();
			
			
			// Write proper error message if exitCode is not 0 or timeout happened
			if((exitCode != 0)&&(timeout))
				getRecord(ERROR_MESSAGE).setValue("Error Code: " + exitCode);
		}

		@Override
		public void run() {
			
			log.debug("["+service+"] calling '"+cmd+"'.");
			try {
				
				if (relayCommand) {
					relayCommand();
				} else {
					localCommand();
				}
				
				proc.setValue(0);
				
				if (!done && 
						(!(getRecord(STATE).getValueAsInt()==State.STARTING.ordinal() && running) 
								|| !(getRecord(STATE).getValueAsInt()==State.STOPPING.ordinal() && !running))) {
					String s=null;
					if (timeout) {
						s= "["+service+"] '"+cmd+"' TIMEOUT in '"+time+"' ms, err:'"+errorOut.toString()+"', std:'"+stdOut.toString()+"'.";
					} else {
						s= "["+service+"] '"+cmd+"' FAILED in '"+time+"' ms, err:'"+errorOut.toString()+"', std:'"+stdOut.toString()+"'.";
					}
					log.error(s);
					setError(true,s);
				} else {
					log.info("["+service+"] '"+cmd+"' DONE in '"+time+"' ms, err:'"+errorOut.toString()+"', std:'"+stdOut.toString()+"'.");
					setError(false,STR_OK);
				}
				
			} catch (Exception e) {
				proc.setValue(0);
				String s= "["+service+"] Failed: "+e.toString();
				log.error(s,e);
				setError(true,s);
			}
			/*State st= running? State.RUNNING : State.STOPPED;
			log.debug("STATE after exec "+st);
			getRecord(STATE).setValue(st.ordinal());*/
		}
	}
	

	private String processPattern;
	private PSPool ps;
	private String processCmdStart;
	private String processCmdStop;
	@SuppressWarnings("unused")
	private String processCmdRestart;
	private String processCmdKill;
	private Set<PSProc> processes;
	private ExecutorService executor;
	private String service;
	private String host;
	private String user;
	private String location;
	private boolean running;
	private String description;
	private boolean relayCommand;
	private String privateKey;
	
	final private Properties config = new Properties();

	
	/**
	 * <p>Constructor for ProcessManagerApplication.</p>
	 */
	public ProcessManagerApplication() {
		
		//Strict host key checking will not be used.
		config.put("StrictHostKeyChecking", "no");

		processes= new LinkedHashSet<PSProc>();
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		
		processPattern= config.getString("process_pattern");
		if (processPattern==null) {
			throw new NullPointerException("process_pattern is not set!");
		}
		processCmdStart= config.getString("start");
		if (processCmdStart==null) {
			throw new NullPointerException("start command is not set!");
		}
		processCmdStop= config.getString("stop");
		if (processCmdStop==null) {
			throw new NullPointerException("stop command is not set!");
		}
		service= config.getString("service");
		if (service==null) {
			service= getName();
			int l= service.lastIndexOf(':');
			if (l>-1) {
				service=service.substring(l+1);
			}
			if (service.charAt(service.length()-3)=='-') {
				service=service.substring(0, service.length()-3);
			}
		}
		description= config.getString("description",service);
		privateKey= config.getString("privateKey",".ssh/id_rsa");

		processCmdRestart= config.getString("restart");
		/*if (processCmdRestart==null) {
			throw new NullPointerException("restart command is not set!");
		}*/
		processCmdKill= config.getString("kill");
		if (processCmdKill==null) {
			processCmdKill="kill -9 ";
		}
		
		try {
			location= InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("["+service+"] Could not resolve host name: "+e.toString(), e);
		}
		
		host = config.getString("host");
		// MODIFIED RBS
		if (host==null || host.equals("")) {
			host=location;
			relayCommand=false;
		} else {
			relayCommand=true;
		}
	
		user= config.getString("user",System.getProperty("user.name"));

		addRecordOfCommandProcessor(START, "Start the process", 30000);
		addRecordOfCommandProcessor(STOP, "Stop the process", 30000);
		//addRecordOfCommandProcessor(RESTART, "Restart the process", 30000);
		addRecordOfCommandProcessor(KILL, "Kill the process", 30000);
		addRecordOfMemoryValueProcessor(STATE, "Process state", State.labels(), State.STOPPED.value());
		addRecordOfMemoryValueProcessor(RUNNING, "Is process up and running.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(ERROR, "Is process signaling error.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(PID, "PID of process", DBRType.INT, 0);
		addRecordOfMemoryValueProcessor(COMMAND, "Command of process", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(USER, "User name of the process", DBRType.STRING, user);
		addRecordOfMemoryValueProcessor(COMPLETION, "Completion log of last command", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(SERVICE, "Service name", DBRType.STRING, service);
		addRecordOfMemoryValueProcessor(DESCRIPTION, "Descriptive name", DBRType.STRING, description);
		addRecordOfMemoryValueProcessor(HOST, "Host name of process", DBRType.STRING, host);
		addRecordOfMemoryValueProcessor(LOCATION, "Host name of manager", DBRType.STRING, location);
		addRecordOfMemoryValueProcessor(ERROR_MESSAGE, "Error output when a command fails.", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(RELAY, "Commands relayed to remote proceess host.", DBRType.BYTE, relayCommand);
		addRecordOfMemoryValueProcessor(CPU, "CPU in % of process", 0.0,100.0, "%", (short)1, 0.0);
		addRecordOfMemoryValueProcessor(MEM, "MEM in % of process", 0.0,100.0, "%", (short)1, 0.0);
		
		
		// ADDED RBS
		//ps= PSPool.getInstance();
		if( relayCommand) {
			ps= PSPool.getInstance(host,user);
		} else {
			ps= PSPool.getInstance(null,user);
		}
		
		/*if (relayCommand) {
			ps= PSPool.getInstance(host);
		} else { 
			ps= PSPool.getInstance();
		}*/
		
		ps.addProcessMonitor(processPattern, this);
		ps.start();
		
		updateProcesses();
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name == START) {
			log.debug("["+service+"] STATE on Start "+State.STARTING);
			getRecord(STATE).setValue(State.STARTING.ordinal());
			execute(name,processCmdStart);
			
		} else if (name == STOP) {
			log.debug("["+service+"] STATE on Stop "+State.STOPPING);
			getRecord(STATE).setValue(State.STOPPING.ordinal());
			execute(name,processCmdStop);
		} else if (name == KILL) {
			if(getRecord(PID).getValueAsInt() > 0) {
				log.debug("["+service+"] STATE on Kill "+State.KILLING);
				getRecord(STATE).setValue(State.KILLING.ordinal());
				execute(name,processCmdKill + getRecord(PID).getValueAsInt());
			}
		}
	}
	
	private void execute(String name, String processCmd) {
		Record r= getRecord(name);
		CommandValueProcessor cmp= (CommandValueProcessor) r.getProcessor();
		
		CmdExecutor cmd= new CmdExecutor(processCmd, cmp);
		
		getExecutor().execute(cmd);
	}

	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Object o= evt.getNewValue();
		
		if (o !=null && o instanceof org.scictrl.csshell.epics.server.application.processmanager.PSPool.PSProc) {
			org.scictrl.csshell.epics.server.application.processmanager.PSPool.PSProc p= (org.scictrl.csshell.epics.server.application.processmanager.PSPool.PSProc)o;

			//log.debug("["+service+"] "+evt.getPropertyName()+": '"+p+"' current: "+Arrays.toString(processes.toArray()));
			
			if (evt.getPropertyName() == PSPool.PROCESS_ADDED) {
				processes.add(p);
				updateProcesses();
			} else if (evt.getPropertyName() == PSPool.PROCESS_REMOVED) {
				processes.remove(p);
				updateProcesses();
			} else {
				updateProcesses();
			}
			
		}
	}

	private void updateProcesses() {
		int l= processes.size();
		int[] pid= new int[l];
		double cpu=0.0;
		double mem=0.0;
		String[] command= new String[l];
		
		Iterator<PSProc> it= processes.iterator();
		
		for (int i = 0; i < l; i++) {
			PSProc p= it.next();
			pid[i]=p.pid;
			command[i]=p.command;
			cpu+=p.cpu;
			mem+=p.mem;
		}
		
		this.running=l>0;
		
		if (l>0) {
			getRecord(COMMAND).setValue(command);
			getRecord(PID).setValue(pid);
		} else {
			getRecord(COMMAND).setValue("");
			getRecord(PID).setValue(0);
		}
		getRecord(CPU).setValue(cpu);
		getRecord(MEM).setValue(mem);
		
		getRecord(RUNNING).setValue(running);
		State st= running? State.RUNNING : State.STOPPED;
		log.debug("["+service+"] STATE on update "+st);
		getRecord(STATE).setValue(st.ordinal());
	}

	private Executor getExecutor()
	{
		if(executor == null)
		{
			executor = Executors.newCachedThreadPool(); 
					//new ThreadPoolExecutor(1, 4, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		}
		return executor;
	}
	
	/**
	 * <p>setError.</p>
	 *
	 * @param error a boolean
	 * @param completion a {@link java.lang.String} object
	 */
	public void setError(boolean error, String completion) {
		getRecord(COMPLETION).setValueAsString(completion);
		getRecord(ERROR).setValue(error);
		if (error) {
			updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.COMM_ALARM);
		} else {
			updateErrorSum(Severity.NO_ALARM,gov.aps.jca.dbr.Status.NO_ALARM);
		}
	}

}
