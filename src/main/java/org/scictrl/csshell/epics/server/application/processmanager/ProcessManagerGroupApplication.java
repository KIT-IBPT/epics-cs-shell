/**
 * 
 */
package org.scictrl.csshell.epics.server.application.processmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.application.AbstractApplication;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ProcessManagerGroupApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class ProcessManagerGroupApplication extends AbstractApplication {

//	public static final String START = "Cmd:Start";
//	public static final String STOP = "Cmd:Stop";
//	public static final String UPGRADE = "Cmd:Upgrade";
//	public static final String RESTART = "Cmd:Restart";
//	public static final String KILL = "Cmd:Kill";
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
//	public static final String PID = "Info:PID";
	/** Constant <code>SERVICE="Info:Service"</code> */
	public static final String SERVICE = "Info:Service";
//	public static final String COMMAND = "Info:Command";
	/** Constant <code>DESCRIPTION="Info:Description"</code> */
	public static final String DESCRIPTION = "Info:Description";
	/** Constant <code>CPU="CPU"</code> */
	public static final String CPU = "CPU";
	/** Constant <code>MEM="MEM"</code> */
	public static final String MEM = "MEM";
	
	
	//private static final String STR_OK = "OK";
	
	/**
	 * Process state
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
		 * Mixed state of underlaying processes.
		 */
		MIXED,
		/**
		 * Undefined.
		 */
		UNDEFINED;
		
		/**
		 * Labels with stated
		 * @return labels with states
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
		 * Returns ordinal value of state.
		 * @return ordinal value of state
		 */
		public short value() {
			return (short)ordinal();
		}
	}
	

	private String description;
	private String[] managers;
	private String service;
	private String host;
	private String completion="";
	private boolean error=false;
	
	/**
	 * <p>Constructor for ProcessManagerGroupApplication.</p>
	 */
	public ProcessManagerGroupApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		
		managers= config.getStringArray("managers");
		if (managers==null || managers.length==0) {
			throw new NullPointerException("managers is not set!");
		}
		
		description= config.getString("description");
		if (description==null) {
			description="ProcMan Group";
		}
		
		service="";
		host="";
		
		addRecordOfMemoryValueProcessor(STATE, "Process state", State.labels(), State.STOPPED.value());
		addRecordOfMemoryValueProcessor(RUNNING, "Is process up and running.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(ERROR, "Is process signaling error.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(COMPLETION, "Completion log of last command", DBRType.STRING, completion);
		addRecordOfMemoryValueProcessor(SERVICE, "Service name", DBRType.STRING, service);
		addRecordOfMemoryValueProcessor(DESCRIPTION, "Descriptive name", DBRType.STRING, description);
		addRecordOfMemoryValueProcessor(HOST, "Host name", DBRType.STRING, host);
		addRecordOfMemoryValueProcessor(CPU, "CPU in % of process", 0.0,100.0, "%", (short)1, 0.0);
		addRecordOfMemoryValueProcessor(MEM, "MEM in % of process", 0.0,100.0, "%", (short)1, 0.0);
		
		String[] statePVs= new String[managers.length];
		String[] runningPVs= new String[managers.length];
		String[] errorPVs= new String[managers.length];
		String[] errorSumPVs= new String[managers.length];
		String[] completionPVs= new String[managers.length];
		String[] servicePVs= new String[managers.length];
		String[] hostPVs= new String[managers.length];
		String[] cpuPVs= new String[managers.length];
		String[] memPVs= new String[managers.length];
		
		for (int i = 0; i < managers.length; i++) {
			statePVs[i]= managers[i]+":"+STATE;
			runningPVs[i]= managers[i]+":"+RUNNING;
			errorPVs[i]= managers[i]+":"+ERROR;
			errorSumPVs[i]= managers[i]+":"+ERROR_SUM;
			completionPVs[i]= managers[i]+":"+COMPLETION;
			servicePVs[i]= managers[i]+":"+SERVICE;
			hostPVs[i]= managers[i]+":"+HOST;
			cpuPVs[i]= managers[i]+":"+CPU;
			memPVs[i]= managers[i]+":"+MEM;
		}
		
		connectLinks(STATE, statePVs);
		connectLinks(RUNNING, runningPVs);
		connectLinks(ERROR, errorPVs);
		connectLinks(ERROR_SUM, errorSumPVs);
		connectLinks(COMPLETION, completionPVs);
		connectLinks(SERVICE, servicePVs);
		connectLinks(HOST, hostPVs);
		connectLinks(CPU, cpuPVs);
		connectLinks(MEM, memPVs);
	}
	
	
	/**
	 * <p>Setter for the field <code>error</code>.</p>
	 *
	 * @param error a boolean
	 * @param completion a {@link java.lang.String} object
	 */
	public void setError(boolean error, String completion) {
		Record rc= getRecord(COMPLETION);
		rc.updateNoAlarm();
		rc.setValueAsString(completion);
		
		rc= getRecord(ERROR);
		rc.setValue(error);
		rc.updateNoAlarm();
		if (error) {
			updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.COMM_ALARM);
		} else {
			updateErrorSum(Severity.NO_ALARM,gov.aps.jca.dbr.Status.NO_ALARM);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		
		ValueLinks vl= getLinks(name);
		if (!vl.isInvalid() && vl.isReady()) {
			ValueHolder[] vh= vl.consume();
			if (name==STATE) {
				State st=null;
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						st=State.UNDEFINED;
						break;
					};
					State s= State.values()[(int) val.longValue()];
					if (st==null) {
						st=s;
					} else if (s!=st) {
						st= State.MIXED;
						break;
					}
				}
				Record r= getRecord(STATE);
				r.updateNoAlarm();
				r.setValue(st.value());
			} else if (name==RUNNING) {
				boolean running=true;
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						running=false;
						break;
					};
					running= running && val.longValue()!=0;
					if (!running) {
						break;
					}
				}
				Record r= getRecord(RUNNING);
				r.updateNoAlarm();
				r.setValue(running);
			} else if (name==ERROR) {
				boolean er=false;
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						er=true;
						completion="Link error to "+val.name;
						break;
					};
					er= er || val.longValue()!=0;
					if (er) {
						break;
					}
				}
				error=er;
				setError(error, completion);
			} else if (name==ERROR_SUM) {
				boolean er=false;
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						er=true;
						completion="Link error to "+val.name;
						break;
					};
					er= er || val.longValue()!=0;
					if (er) {
						completion="Error Sum on delegates";
						break;
					}
				}
				error=er;
				setError(error, completion);
			} else if (name==COMPLETION) {
				StringBuilder sb= new StringBuilder();
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						if (sb.length()>0) {
							sb.append("; ");
						}
						sb.append("Link error to "+val.name);
						break;
					};
					if (sb.length()>0) {
						sb.append("; ");
					}
					sb.append(val.name);
					sb.append(":'");
					sb.append(val.value);
					sb.append("'");
				}
				completion=sb.toString();
				setError(error, completion);
			} else if (name==SERVICE) {
				StringBuilder sb= new StringBuilder();
				for (ValueHolder val : vh) {
					if (val.isAlarm()) {
						if (sb.length()>0) {
							sb.append(", ");
						}
						sb.append("(Link error to "+val.name+")");
						
					} else {
						if (sb.length()>0) {
							sb.append(", ");
						}
						sb.append(val.value);
					}
				}
				Record r= getRecord(SERVICE);
				r.updateNoAlarm();
				r.setValueAsString(sb.toString());
			} else if (name==HOST) {
				Set<String> s= new HashSet<String>();
				for (ValueHolder val : vh) {
					Object o= val.value;
					if (o!=null) {
						String ss=o.toString().trim();
						if (ss.length()>0) {
							s.add(ss);
						}
					}
				}
				List<String> l= new ArrayList<String>(s);
				Collections.sort(l);
				StringBuilder sb= new StringBuilder();
				for (String n : l) {
					if (sb.length()>0) {
						sb.append(", ");
					}
					sb.append(n);
				}
				Record r= getRecord(HOST);
				r.updateNoAlarm();
				r.setValueAsString(sb.toString());
			} else if (name==CPU) {
				double cpu=0.0;
				for (ValueHolder val : vh) {
					if (!val.isAlarm()) {
						cpu+=val.doubleValue();
					}
				}
				cpu=cpu/vh.length;
				Record r= getRecord(CPU);
				r.updateNoAlarm();
				r.setValue(cpu);
			} else if (name==MEM) {
				double mem=0.0;
				for (ValueHolder val : vh) {
					if (!val.isAlarm()) {
						mem+=val.doubleValue();
					}
				}
				mem=mem/vh.length;
				Record r= getRecord(MEM);
				r.updateNoAlarm();
				r.setValue(mem);
			}
		} else {
			Record r= getRecord(name);
			r.updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM);
		}
	}

}
