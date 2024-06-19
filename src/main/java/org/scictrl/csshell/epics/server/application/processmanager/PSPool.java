/**
 * 
 */
package org.scictrl.csshell.epics.server.application.processmanager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
// COMMENTED RBS
//import com.sun.xml.internal.bind.v2.runtime.Location;

/**
 * <p>PSPool class.</p>
 *
 * @author igor@scictrl.com
 */
public class PSPool implements Runnable {
	
	/** Constant <code>PROCESS_ADDED="PROCESS_ADDED"</code> */
	public static final String PROCESS_ADDED = "PROCESS_ADDED";
	/** Constant <code>PROCESS_REMOVED="PROCESS_REMOVED"</code> */
	public static final String PROCESS_REMOVED = "PROCESS_REMOVED";
	/** Constant <code>PROCESS_UPDATED="PROCESS_UPDATED"</code> */
	public static final String PROCESS_UPDATED = "PROCESS_UPDATED";

	private static final PSPool psPooler=new PSPool();
	// MODIFIED RBS
	//private static final Map<String, PSPool> psHost2Pooler=new HashMap<String, PSPool>(); 
	private static final HashMap<String, PSPool> psHost2Pooler=new HashMap<String, PSPool>();
	
	// COMMENTED RBS
	/*public static final synchronized PSPool getInstance() {
		return psPooler;
	}*/
	
	// MODIFIED RBS
	//public static final synchronized PSPool getInstance(String host) {
	/**
	 * <p>getInstance.</p>
	 *
	 * @param host a {@link java.lang.String} object
	 * @param hostuser a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.processmanager.PSPool} object
	 */
	public static final synchronized PSPool getInstance(String host, String hostuser) {
		if (host==null || host.equals(""))
			return psPooler;
		
		PSPool p= psHost2Pooler.get(host);
		if (p!=null) {
			return p;
		}
		
		// MODIFIED RBS
		//p= new PSPool(host, hostuser);
		p= new PSPool(host, hostuser);
		psHost2Pooler.put(host, p);
		p = psHost2Pooler.get(host);
		return p;
	}
	
	/**
	 * Proc record.
	 */
	public static class PSProc {
		/**
		 * Command name.
		 */
		public final String command;
		// ADDED RBS
		//public final String user;
		//
		/**
		 * Process pid.
		 */
		public final int pid;
		/**
		 * process cpu usage
		 */
		public double cpu;
		/**
		 * memory usage.
		 */
		public double mem;
		
		/**
		 * Constructor
		 * @param pid pid
		 * @param command command
		 */
		public PSProc(int pid, String command) {
		//public PSProc(int pid, String command, String user) {
			this.pid=pid;
			this.command=command.trim();
			//this.user=user;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PSProc) {
				PSProc p= (PSProc)obj;
				return this.pid==p.pid && this.command.equals(p.command);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return (pid+command).hashCode();
		}
		
		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder(128);
			sb.append("PROC:{");
			sb.append(pid);
			sb.append(", ");
			sb.append(cpu);
			sb.append("%, ");
			sb.append(mem);
			sb.append("%, \"");
			sb.append(command);
			sb.append("\"}");
			return sb.toString();
		}
		
		/**
		 * Updated with new data
		 * @param proc new data
		 */
		public void update(PSProc proc) {
			this.cpu=proc.cpu;
			this.mem=proc.mem;
		}
	}
	
	/**
	 * Process monitor
	 */
	class ProcessMonitor {
		/**
		 * Process pattern
		 */
		String pattern;
		/**
		 * Listener.
		 */
		PropertyChangeListener monitor;
		/**
		 * Pattern
		 */
		Pattern rg;

		/**
		 * Constructor
		 * @param pattern process pattern
		 * @param monitor listener
		 */
		public ProcessMonitor(String pattern, PropertyChangeListener monitor) {
			this.pattern=pattern;
			this.monitor=monitor;
			rg=Pattern.compile(pattern);
		}
		
		boolean equals(String pattern, PropertyChangeListener monitor) {
			return this.pattern.equals(pattern) && this.monitor==monitor;
		}
		
		boolean match(PSProc process) {
			return rg.matcher(process.command).find();
		}
	}



	private PropertyChangeSupport support;
	private Map<Integer,PSProc> processes;
	private boolean alive=true;
	private Thread thread;
	private ConcurrentLinkedQueue<ProcessMonitor> processMonitors;
	private String host;
	// ADDED RBS
	private String hostuser;

	/**
	 * <p>Constructor for PSPool.</p>
	 */
	public PSPool() {
		
		this.support= new PropertyChangeSupport(this);
		this.processes= new HashMap<Integer,PSProc>();
		this.processMonitors= new ConcurrentLinkedQueue<PSPool.ProcessMonitor>();
		
	}
	
	/**
	 * <p>Constructor for PSPool.</p>
	 *
	 * @param host a {@link java.lang.String} object
	 * @param user a {@link java.lang.String} object
	 */
	public PSPool(String host, String user) {
		this();
		this.host=host;
		this.hostuser=user;
	}

	/**
	 * <p>addPropertyChangeListener.</p>
	 *
	 * @param l a {@link java.beans.PropertyChangeListener} object
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}
	
	/**
	 * <p>addProcessMonitor.</p>
	 *
	 * @param processPattern a {@link java.lang.String} object
	 * @param processMonitor a {@link java.beans.PropertyChangeListener} object
	 */
	public void addProcessMonitor(String processPattern, PropertyChangeListener processMonitor) {
		ProcessMonitor pm= new ProcessMonitor(processPattern,processMonitor);
		processMonitors.add(pm);
	}
	
	/**
	 * <p>removeProcessMonitor.</p>
	 *
	 * @param processPattern a {@link java.lang.String} object
	 * @param processMonitor a {@link java.beans.PropertyChangeListener} object
	 */
	public void removeProcessMonitor(String processPattern, PropertyChangeListener processMonitor) {
		for (ProcessMonitor pm : processMonitors) {
			if (pm.equals(processPattern, processMonitor)) {
				processMonitors.remove(pm);
			}
		}
	}

	
	// COMMENTED RBS
	/*@Override
	public void run() {
		
		try {
			while (this.alive) {
				synchronized (this) {
					wait(1000);
				}

				pool();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}*/
	
	// ADDED RBS
	/** {@inheritDoc} */
	@Override
	public void run() {
		
		try {
			while (this.alive) {
				synchronized (this) {
					wait(1000);
				}
				
				pool(host);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	// COMMENTED RBS
	/*private void pool() throws IOException, InterruptedException {
		
		java.lang.Process p= Runtime.getRuntime().exec("ps aux");
		
		p.waitFor();
		
		BufferedReader re=new BufferedReader(new InputStreamReader(p.getInputStream()));
		List<String> lines=new ArrayList<String>();
		String in=null;
		try {
			while ((in=re.readLine())!=null) {
				lines.add(in);
				//System.out.println("OUTPUT "+in);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println("OUTPUT lines "+lines.size());

		Set<PSProc> newProc= new HashSet<PSPool.PSProc>(lines.size());
		
		for (int i = 1; i < lines.size(); i++) {
			if (!(i==lines.size()-1 && lines.get(i).endsWith("ps aux"))) {
				newProc.add(toProcess(lines.get(i)));
			}
		}
		
		Set<PSProc> toBeRemoved= new HashSet<PSPool.PSProc>(processes.size());
		
		for (PSProc proc : processes.values()) {
			if (!newProc.contains(proc)) {
				toBeRemoved.add(proc);
			}
		}
		
		for (PSProc proc : toBeRemoved) {
			processes.remove(proc.pid);
			fireRemoved(proc);
		}
		
		for (PSProc proc : newProc) {
			PSProc pr= processes.get(proc.pid); 
			if (pr==null) {
				processes.put(proc.pid,proc);
				fireAdded(proc);
			} else if (Math.abs(pr.cpu-proc.cpu)>0.01 || Math.abs(pr.mem-proc.mem)>0.01) {
				pr.update(proc);
				fireUpdated(pr);
			}
		}
		
	}*/
	
	// ADDED RBS
	private void pool(String host) throws IOException, InterruptedException {
		//String user = "";
		String command;
		
		// Fill the user
		
		if (host==null || host.equals("")){
			command = "ps aux";
		} else {
			//command = "ssh " + hostuser + "@" + host + " ps aux";
			// RBS TEMP
			command = "ssh " + hostuser + "@" + host + " ps aux";
		}
		//command = "ssh ankaop@ankasr-epics01 echo 123";
		java.lang.Process p= Runtime.getRuntime().exec(command);
						
		p.waitFor();
		
		// ADDED RBS
		// The external command continue to work in the background and we cannot read the output
		// we need a wait in between this 2 seconds is experimental so it may need to be changed later
		// the reason I used this method is that all kinds of waiting method for the process p cause errors
		java.lang.Process paux= Runtime.getRuntime().exec("sleep 0.5");
		paux.waitFor();
		
		BufferedReader re=new BufferedReader(new InputStreamReader(p.getInputStream()));
		List<String> lines=new ArrayList<String>();
		String in=null;
		try {
			while ((in=re.readLine())!=null) {
				lines.add(in);
				//System.out.println("OUTPUT "+in);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println("OUTPUT lines "+lines.size());

		Set<PSProc> newProc= new HashSet<PSPool.PSProc>(lines.size());
		
		for (int i = 1; i < lines.size(); i++) {
			if (!(i==lines.size()-1 && lines.get(i).endsWith(command))) {
				newProc.add(toProcess(lines.get(i)));
			}
		}
		
		Set<PSProc> toBeRemoved= new HashSet<PSPool.PSProc>(processes.size());
		
		for (PSProc proc : processes.values()) {
			if (!newProc.contains(proc)) {
				toBeRemoved.add(proc);
			}
		}
		
		for (PSProc proc : toBeRemoved) {
			processes.remove(proc.pid);
			fireRemoved(proc);
		}
		
		for (PSProc proc : newProc) {
			PSProc pr= processes.get(proc.pid); 
			if (pr==null) {
				processes.put(proc.pid,proc);
				fireAdded(proc);
			} else if (Math.abs(pr.cpu-proc.cpu)>0.01 || Math.abs(pr.mem-proc.mem)>0.01) {
				pr.update(proc);
				fireUpdated(pr);
			}
		}
		
	}

	
	private void fireRemoved(PSProc proc) {
		PropertyChangeEvent e= new PropertyChangeEvent(this, PROCESS_REMOVED, null, proc);
		support.firePropertyChange(e);

		for (ProcessMonitor pm : processMonitors) {
			if (pm.match(proc)) {
				pm.monitor.propertyChange(e);
			}
		}
	}

	private void fireAdded(PSProc proc) {
		PropertyChangeEvent e= new PropertyChangeEvent(this, PROCESS_ADDED, null, proc);
		support.firePropertyChange(e);
		
		for (ProcessMonitor pm : processMonitors) {
			if (pm.match(proc)) {
				pm.monitor.propertyChange(e);
			}
		}
	}

	private void fireUpdated(PSProc proc) {
		PropertyChangeEvent e= new PropertyChangeEvent(this, PROCESS_UPDATED, null, proc);
		support.firePropertyChange(e);
		
		for (ProcessMonitor pm : processMonitors) {
			if (pm.match(proc)) {
				pm.monitor.propertyChange(e);
			}
		}
	}
	
	private PSProc toProcess(String line) {
		String pid= line.substring(8, 14).trim();
		// ADDED RBS
		//String user= line.substring(0, 8).trim();

		String name= line.substring(64);
		if (!name.startsWith(" ")) {
			int i= name.indexOf(' ');
			if (i<name.length()-2 && i>-1) {
				name=name.substring(i);
			}
		}
		name=name.trim();
		
		String cpuS= line.substring(14, 20).trim();
		double cpu = 0.0;
		try {
			cpu=Double.parseDouble(cpuS);
		} catch (Exception e) {
			LogManager.getLogger(PSPool.class).error(e.toString(), e);
			cpu=0.0;
		}
		
		String memS= line.substring(20, 25).trim();
		double mem = 0.0;
		try {
			mem=Double.parseDouble(memS);
		} catch (Exception e) {
			LogManager.getLogger(PSPool.class).error(e.toString(), e);
			mem=0.0;
		}

		// MODIFIED RBS
		PSProc p= new PSProc(Integer.valueOf(pid),name);
		//PSProc p= new PSProc(Integer.valueOf(pid),name,user);
		p.cpu=cpu;
		p.mem=mem;
		return p;
//R       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
//aop   53404  1.7  0.0 338872  3552 pts/24   Sl+  Apr08 1351:25 /home/ankaop/applications/epics/apps/LLRFcontrolBooster/bin/llrf-control-booster
//aop   53405  0.0  0.0   7776   728 pts/24   S+   Apr08   0:00 tee -a /tmp/llrf-control-booster.log
	}
	
	/**
	 * <p>start.</p>
	 */
	public synchronized void start() {
		if (this.thread!=null) {
			return;
		}
		this.thread=new Thread(this);
		this.thread.start();
	}

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		PSPool ps= new PSPool();
		
		if (args!=null && args.length==1 && args[0]!=null && args[0].trim().length()>0 ) {
			
			String pattern= args[0].trim();
			System.out.println("Listening to processes with pattern '"+pattern+"'");
			ps.addProcessMonitor(pattern, new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					System.out.println(evt.getPropertyName()+" "+evt.getNewValue());
				}
			});

		} else {
			
			ps.addPropertyChangeListener(new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					System.out.println(evt.getPropertyName()+" "+evt.getNewValue());
				}
			});
			
		}
		ps.start();
		
		synchronized (ps) {
			try {
				ps.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
