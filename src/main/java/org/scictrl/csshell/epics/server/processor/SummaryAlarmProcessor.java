/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

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
public class SummaryAlarmProcessor extends DefaultAlarmProcessor {

	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;
	private String[] path;
	private boolean pathRecursive;
	private ValueLinks inputAD;

	@SuppressWarnings("unused")
	private String nameAD;
	private String alarmDisablePv;
	private int alarmDisableReplace;
	
	/**
	 * <p>Constructor for SummaryAlarmProcessor.</p>
	 */
	public SummaryAlarmProcessor() {
		super();
		type=DBRType.BYTE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		
		super.configure(record, config);

		disableSupressTime();
		
		// if PV suffix is declared, then it is used to disable summary of alarms with disabled bit in array
		// proposed value: ":AlarmDisable"
		alarmDisablePv = config.getString("alarmDisable.pv");
		// how many elements delimited by : has to be replaced with pv string
		alarmDisableReplace = config.getInt("alarmDisable.replace",0);
		
		if (alarmDisablePv==null || alarmDisablePv.length()<2) {
			alarmDisablePv=null;
		}
				
		String[] names= config.getStringArray("input.links");
		
		String type= Record.toPropertyName(config.getString("input.type",Record.PROPERTY_VALUE));
		
		if (names!=null && names.length>0) {
			
			this.input= new ValueLinks(record.getName(), names, this, type);
			
			if (alarmDisablePv!=null) {
				String[] namesAD= new String[names.length];
				for (int i = 0; i < namesAD.length; i++) {
					if (alarmDisableReplace>0) {
						String s[] = names[i].split(":");
						if (s!=null && (s.length>alarmDisableReplace+1)) {
							StringBuilder sb= new StringBuilder(names[i].length());
							for (int j = 0; j < s.length-alarmDisableReplace; j++) {
								sb.append(s[j]);
								sb.append(':');
							}
							if (alarmDisablePv.charAt(0)==':') {
								sb.append(alarmDisablePv.substring(1));
							} else {
								sb.append(alarmDisablePv);
							}
							namesAD[i]=sb.toString();
						} else {
							namesAD[i]=names[i]+alarmDisablePv;
						}
					} else {
						namesAD[i]=names[i]+alarmDisablePv;
					}
				}
				this.inputAD= new ValueLinks(nameAD=record.getName()+"_AD", namesAD, this, type);
			}
			
			update(true, Severity.INVALID_ALARM, Status.UDF_ALARM,false);
			
		} else if (config.containsKey("input.alarmPath")) {
			path= config.getStringArray("input.alarmPath");
			pathRecursive= config.getBoolean("input.recursive", false);
			update(true, Severity.INVALID_ALARM, Status.UDF_ALARM,false);
		} else {
			_setValue(false,Severity.INVALID_ALARM,Status.UDF_ALARM,false,true);
		}

	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		super.propertyChange(evt);
		
		if (input==evt.getSource() || inputAD==evt.getSource()) {
			if (input.isInvalid()) {
				update(true,Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			if (!input.isReady()) {
				return;
			}
			
			
			ValueHolder[] vh= input.consume();
			
			boolean[] alarmDisable= new boolean[vh.length];
			if (inputAD!=null) {
				ValueHolder[] vhAD= inputAD.consume();
				ValueHolder.getValid(alarmDisable, vhAD, false);
			} else {
				Arrays.fill(alarmDisable, false);
			}
								
			ValueHolder vhsum=ValueHolder.toSummaryBit(vh,alarmDisable);
			
			update(vhsum.isAlarm(),vhsum.severity,vhsum.status,true);
			return;
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (path!=null) {
			
			ConfigurationVisitor cv= getRecord().getDatabase().getServer().getInputData();
			String[] paths= cv.getAlarmPaths();
			
			List<String> p= new ArrayList<String>();
			
			StringBuilder sb= new StringBuilder(128);
			sb.append(path[0]);
			for (int i = 1; i < path.length; i++) {
				sb.append(',');
				sb.append(path[i]);
			}
			String ph= sb.toString();

			
			for (String p1 : paths) {
				if (pathRecursive) {
					if (p1.length()>=ph.length() && p1.startsWith(ph) && (p1.length()==ph.length() || p1.charAt(ph.length())==',')) {
						p.add(p1);
					} 
				} else if (p1.equals(ph)) {
					p.add(p1);
					break;
				}
			}
			
			List<String> pvs= new ArrayList<String>();
			
			for (String p1 : p) {
				pvs.addAll(cv.getPVsForAlarmPath(p1));
			}
			
			if (pvs.size()>0) {
				input= new ValueLinks(record.getName(),pvs.toArray(new String[pvs.size()]),this,Record.PROPERTY_VALUE);
			} else {
				log.warn("Summary alarm '{}' has found not links for search path '{}'.",getName(),Arrays.toString(path));
			}
		}
		
		if (input!=null) {
			input.activate(getRecord().getDatabase());
		}
		if (inputAD!=null) {
			inputAD.activate(getRecord().getDatabase());
		}
	}


}
