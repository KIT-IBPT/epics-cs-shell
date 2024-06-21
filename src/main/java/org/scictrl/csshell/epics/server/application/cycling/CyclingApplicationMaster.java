/**
 * 
 */
package org.scictrl.csshell.epics.server.application.cycling;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.processor.EnumValueProcessor;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;

/**
 * <p>CyclingApplicationMaster class.</p>
 *
 * @author igor@scictrl.com
 */
public class CyclingApplicationMaster extends AbstractApplication {
	
	/**
	 * Enum definition of cycling master status.
	 */
	public static enum Status {
		
		/**
		 * Cycling master is idle and ready for operation.
		 */
		READY,
		/**
		 * Cycling master is busy cycling.
		 */
		CYCLING,
		/**
		 * Cycling master has been aborted by user.
		 */
		ABORT,
		/**
		 * Cycling master stopped because of connection fail.
		 */
		CONN_FAIL,
		/**
		 * Cycling master stopped because of general failure.
		 */
		ERROR,
		/**
		 * Cycling master reports mixed state of subordinate cyclers.
		 */
		MISC;

		/**
		 * Labels fro states.
		 */
		public static String[] LABELS= {"Ready","Cycling","Abort","Connect Fail","Error","Misc."};
		
	}

	private static final String DEVICE = "Device";
	private static final String PROGRESS = "Progress";
	private static final String STATUS = "Status";
	private static final String STATUS_DESC = "Status:Desc";
	private static final String CYCLE = "Cycle";
	private static final String ABORT = "Abort";
	

	/**
	 * Constructor.
	 */
	public CyclingApplicationMaster() {
	}
	
	/**
	 * <p>getCyclingParameters.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 */
	public final CyclingParameters getCyclingParameters(String name) {
		String s= getStore().getString(name);
		if (s!=null) {
			return new CyclingParameters(s);
		}
		return null;
	}

	/**
	 * <p>storeCyclingParameters.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param param a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 */
	public final void storeCyclingParameters(String name, CyclingParameters param) {
		getStore().setProperty(name, param.toString());
	}

	private String[] devices;

	private String[] delegates;
	private String[] progressNames;
	private String[] statusNames;
	private String[] cycleNames;
	private String[] abortNames;
	
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		delegates = config.getStringArray("delegates");
		devices= new String[delegates.length];
		progressNames= new String[delegates.length];
		statusNames= new String[delegates.length];
		cycleNames= new String[delegates.length];
		abortNames= new String[delegates.length];
		
		
		
		
		addRecord(DEVICE, MemoryValueProcessor.newProcessor(fullRecordName(DEVICE), DBRType.STRING, 1, "The Power Supply which is cycled by this process.", "", false, false).getRecord());
		addRecord(PROGRESS, MemoryValueProcessor.newProcessor(fullRecordName(PROGRESS), DBRType.DOUBLE, 1, "Progress meeter", new double[]{0,0}, false, false).getRecord());
		addRecord(STATUS, EnumValueProcessor.newProcessor(fullRecordName(STATUS), "Status of cycling process", (short)0, false, Status.LABELS).getRecord());
		addRecord(CYCLE, MemoryValueProcessor.newProcessor(fullRecordName(CYCLE), DBRType.BYTE, 1, "Starts the cycling process.", new byte[]{0}, false, false).getRecord());
		addRecord(ABORT, MemoryValueProcessor.newProcessor(fullRecordName(ABORT), DBRType.BYTE, 1, "Aborts the cycling process.", new byte[]{0}, false, false).getRecord());
		addRecord(STATUS_DESC, MemoryValueProcessor.newProcessor(fullRecordName(STATUS_DESC), DBRType.STRING, 1, "Last status description.", new String[]{"OK"}, false, false).getRecord());

	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		StringBuilder sb= new StringBuilder();
		sb.append("[ ");
				
		for (int i = 0; i < delegates.length; i++) {
			devices[i]= database.getValueAsString(delegates[i]+nameDelimiter+DEVICE);
			sb.append(devices[i]);
			if (i<delegates.length-1) {
				sb.append(", ");
			}
			
			progressNames[i]= delegates[i]+nameDelimiter+PROGRESS;
			statusNames[i]= delegates[i]+nameDelimiter+STATUS;
			cycleNames[i]= delegates[i]+nameDelimiter+CYCLE;
			abortNames[i]= delegates[i]+nameDelimiter+ABORT;
			
		}
		
		sb.append(" ]");

		connectLinks(PROGRESS, progressNames);
		connectLinks(STATUS, statusNames);
		connectLinks(CYCLE, cycleNames);
		connectLinks(ABORT, abortNames);
		
		getRecord(DEVICE).setValueAsString(sb.toString());
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		if (name==PROGRESS) {
			ValueLinks vl= getLinks(PROGRESS);
			if (vl.isInvalid()) {
				getRecord(PROGRESS).updateAlarm(Severity.INVALID_ALARM,gov.aps.jca.dbr.Status.LINK_ALARM,true);
			} else if (vl.isReady()) {
				double[] d= vl.consumeAsDoubles();
				
				double p=0.0;
				for (double e : d) {
					p+=e;
				}
				p=p/d.length;
				getRecord(PROGRESS).setValue(new double[]{p});
			}			
		} else if (name==STATUS) {
			ValueLinks vl= getLinks(STATUS);
			if (vl.isInvalid()) {
				getRecord(STATUS).updateAlarm(Severity.INVALID_ALARM,gov.aps.jca.dbr.Status.LINK_ALARM,true);
			} else if (vl.isReady()) {
				double[] d= vl.consumeAsDoubles();
				double p=Status.READY.ordinal();
				for (double e : d) {
					if (e!=p) {
						if (e==Status.CYCLING.ordinal()||p==Status.CYCLING.ordinal()) {
							p=Status.CYCLING.ordinal();
						} else if (e==Status.ERROR.ordinal()||p==Status.ERROR.ordinal()) {
							p=Status.ERROR.ordinal();
						} else if (e==Status.CONN_FAIL.ordinal()||p==Status.CONN_FAIL.ordinal()) {
							p=Status.CONN_FAIL.ordinal();
						} else if (e==Status.ABORT.ordinal()||p==Status.ABORT.ordinal()) {
							p=Status.ABORT.ordinal();
						} else {
							p=Status.MISC.ordinal();
						}
					};
				}
				getRecord(STATUS).setValue(new short[]{(short)p});
			}
		}
			
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected void notifyRecordChange(String name, boolean alarmOnly) {
		
	}

	/** {@inheritDoc} */
	@Override
	protected void notifyRecordWrite(String name) {
		
		if (name==CYCLE) {
			
			int i= getRecord(CYCLE).getValueAsInt();
			ValueLinks vl= getLinks(CYCLE);
			
			try {
				vl.setValueToAll(new byte[]{(byte)i});
			} catch (Exception e) {
				log4error("Failed to delegate CYCLE", e);
				updateLinkError(true, "Error: "+e.toString());
				updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.STATE_ALARM);
			}
			
		} else if (name==ABORT) {
			
			int i= getRecord(ABORT).getValueAsInt();
			ValueLinks vl= getLinks(ABORT);
			
			try {
				vl.setValueToAll(new byte[]{(byte)i});
			} catch (Exception e) {
				log4error("Failed to delegate ABORT", e);
			}
			
		}
		
	}

}
