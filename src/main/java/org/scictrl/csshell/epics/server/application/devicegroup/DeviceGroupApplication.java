/**
 * 
 */
package org.scictrl.csshell.epics.server.application.devicegroup;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>DeviceGroupApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class DeviceGroupApplication extends AbstractApplication {

	
	private static final String SET_LINKS = "SetLinks";
	private static final String GET_LINKS = "GetLinks";
	private static final String SAME_AS_DEFAULT = "SameAsDefault";
	private static final String SAME_AS_SAVED = "SameAsSaved";
	private static final String APPLY_DEFAULT = "ApplyDefault";
	private static final String APPLY_SAVED = "ApplySaved";
	private static final String SAVED_AS_DEFAULT = "SavedAsDefault";
	private static final String SAVE = "Save";
	private static final String SAVED_PREFIX = "SAV:";
	private static final String DEFAULT_PREFIX = "DEF:";
	private String getSuffix;
	private String setSuffix;
	private String propertySuffix;
	private String groupName;
	private String[] devices;
	private String pvDefault;
	private String pvSaved;
	private String pvGet;
	private String pvSet;

	/**
	 * <p>Constructor for DeviceGroupApplication.</p>
	 */
	public DeviceGroupApplication() {
	}

	/** {@inheritDoc} */
	@Override
	protected PropertiesConfiguration createNewStore() {
		String app= getClass().getSimpleName();
		return getStore(app, groupName+".properties");
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		
		groupName= config.getString("groupName","Group");
		getSuffix= config.getString("getSuffix",":Setpoint:Get");
		setSuffix= config.getString("setSuffix",":Setpoint");
		propertySuffix= config.getString("deviceProperty",":Current");
		
		String descDef="Default value.";
		String descSaved="Saved value.";

		devices= config.getStringArray("devices");
		
		addRecordOfMemoryValueProcessor("GroupName", "", DBRType.STRING, groupName);
		addRecordOfMemoryValueProcessor("Count", "", DBRType.INT, devices.length);
		addRecordOfMemoryValueProcessor(SAVE, "", DBRType.BYTE, (byte)0);
		addRecordOfMemoryValueProcessor(SAVED_AS_DEFAULT, "", DBRType.BYTE, (byte)0);
		addRecordOfMemoryValueProcessor(APPLY_SAVED, "", DBRType.BYTE, (byte)0);
		addRecordOfMemoryValueProcessor(APPLY_DEFAULT, "", DBRType.BYTE, (byte)0);
		addRecordOfMemoryValueProcessor(SAME_AS_DEFAULT, "", DBRType.BYTE, (byte)0);
		addRecordOfMemoryValueProcessor(SAME_AS_SAVED, "", DBRType.BYTE, (byte)0);
		
		
		pvDefault=propertySuffix+nameDelimiter+groupName+":Default";
		pvSaved=propertySuffix+nameDelimiter+groupName+":Saved";
		pvGet=propertySuffix+getSuffix;
		pvSet=propertySuffix+setSuffix;
		
		String[] linksGet= new String[devices.length]; 
		String[] linksSet= new String[devices.length]; 
		
		for (int i=0; i<devices.length;i++) {
			String pv= devices[i];
			addRecord(DEFAULT_PREFIX+pv, MemoryValueProcessor.newProcessor(pv+pvDefault, DBRType.DOUBLE, 1, descDef, getStore().getDouble(DEFAULT_PREFIX+pv,getStore().getDouble(pv+pvDefault,0.0)), false, -Double.MAX_VALUE, Double.MAX_VALUE, "", (short) 3).getRecord());
			addRecord(SAVED_PREFIX+pv, MemoryValueProcessor.newProcessor(pv+pvSaved, DBRType.DOUBLE, 1, descSaved, getStore().getDouble(SAVED_PREFIX+pv,getStore().getDouble(pv+pvSaved,0.0)), false, -Double.MAX_VALUE, Double.MAX_VALUE, "", (short) 3).getRecord());
			linksGet[i]=pv+pvGet;
			linksSet[i]=pv+pvSet;
		}
		
		
		connectLinks(GET_LINKS, linksGet);
		connectLinks(SET_LINKS, linksSet);
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		if (name==GET_LINKS) {
			ValueLinks vl= getLinks(name);
			//Status sta= vl.getLastStatus();
			Severity sev= vl.getLastSeverity();
			if (vl.isReady() && sev!=Severity.NO_ALARM) {
				updateSame();
			}
		} else if (name==SET_LINKS) {
			
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (name.startsWith(DEFAULT_PREFIX)) {
			getStore().setProperty(name, getRecord(name).getValueAsDouble());
		} else if (name.startsWith(SAVED_PREFIX)) {
			getStore().setProperty(name, getRecord(name).getValueAsDouble());
		} else if (name==SAVE) {
			commandSave();
		} else if (name==SAVED_AS_DEFAULT) {
			commandSavedAsDefault();
		} else if (name==APPLY_SAVED) {
			commandApplySaved();
		} else if (name==APPLY_DEFAULT) {
			commandApplyDefault();
		}
	}
	
	private void commandApplySaved() {
		suspendAutoStore(true);
		ValueLinks vl= getLinks(SET_LINKS);
		double[] val= new double[devices.length];
		for (int i=0; i<devices.length; i++) {
			val[i]=getRecord(SAVED_PREFIX+devices[i]).getValueAsDouble();
		}
		try {
			vl.setValueToAll(val);
			updateLinkError(Severity.NO_ALARM, Status.NO_ALARM, "Set successfull");
		} catch (Exception e) {
			e.printStackTrace();
			updateLinkError(Severity.MAJOR_ALARM, Status.LINK_ALARM, "Failed to set: "+e.toString());
		}
		updateSame();
		suspendAutoStore(false);
	}

	private void commandApplyDefault() {
		suspendAutoStore(true);
		ValueLinks vl= getLinks(SET_LINKS);
		double[] val= new double[devices.length];
		for (int i=0; i<devices.length; i++) {
			val[i]=getRecord(DEFAULT_PREFIX+devices[i]).getValueAsDouble();
		}
		try {
			vl.setValueToAll(val);
			updateLinkError(Severity.NO_ALARM, Status.NO_ALARM, "Set successfull");
		} catch (Exception e) {
			e.printStackTrace();
			updateLinkError(Severity.MAJOR_ALARM, Status.LINK_ALARM, "Failed to set: "+e.toString());
		}
		updateSame();
		suspendAutoStore(false);
	}

	private void commandSavedAsDefault() {
		suspendAutoStore(true);
		for (String pv : devices) {
			double v= getRecord(SAVED_PREFIX+pv).getValueAsDouble();
			getRecord(DEFAULT_PREFIX+pv).setValue(v);
		}
		updateSameAsDefault();
		suspendAutoStore(false);
	}

	private void commandSave() {
		suspendAutoStore(true);
		ValueLinks vl= getLinks(GET_LINKS);
		double[] val= vl.consumeAsDoubles();
		for (int i=0; i<devices.length; i++) {
			getRecord(SAVED_PREFIX+devices[i]).setValue(val[i]);
		}
		updateSameAsSaved();
		suspendAutoStore(false);
	}

	private void suspendAutoStore(boolean b) {
		getStore().setAutoSave(!b);
		if (!b) {
			try {
				getStore().save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateSame() {
		ValueLinks get= getLinks(GET_LINKS);
		double[] val= get.consumeAsDoubles();
		boolean sameD=true;
		boolean sameS=true;
		for (int i = 0; i < devices.length; i++) {
			String sD= DEFAULT_PREFIX+devices[i];
			String sS= SAVED_PREFIX+devices[i];
			if (getStore().containsKey(sD)) {
				double def= getStore().getDouble(sD);
				if (Math.abs(def-val[i])>0.00001) {
					sameD=false;
				}
			}
			if (getStore().containsKey(sS)) {
				double def= getStore().getDouble(sS);
				if (Math.abs(def-val[i])>0.00001) {
					sameS=false;
				}
			}
			if (!sameD&&!sameS) {
				break;
			}
		}
		getRecord(SAME_AS_DEFAULT).setValue(sameD);
		getRecord(SAME_AS_SAVED).setValue(sameS);
	}
	private void updateSameAsDefault() {
		ValueLinks get= getLinks(GET_LINKS);
		double[] val= get.consumeAsDoubles();
		boolean sameD=true;
		for (int i = 0; i < devices.length; i++) {
			String sD= DEFAULT_PREFIX+devices[i];
			if (getStore().containsKey(sD)) {
				double def= getStore().getDouble(sD);
				if (Math.abs(def-val[i])>0.00001) {
					sameD=false;
				}
			}
			if (!sameD) {
				break;
			}
		}
		getRecord(SAME_AS_DEFAULT).setValue(sameD);
	}
	private void updateSameAsSaved() {
		ValueLinks get= getLinks(GET_LINKS);
		double[] val= get.consumeAsDoubles();
		boolean sameS=true;
		for (int i = 0; i < devices.length; i++) {
			String sS= SAVED_PREFIX+devices[i];
			if (getStore().containsKey(sS)) {
				double def= getStore().getDouble(sS);
				if (Math.abs(def-val[i])>0.00001) {
					sameS=false;
				}
			}
			if (!sameS) {
				break;
			}
		}
		getRecord(SAME_AS_SAVED).setValue(sameS);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		// TODO Auto-generated method stub
		super.activate();
		
		ValueLinks vl= getLinks(SET_LINKS);
		
		for (int i = 0; i < devices.length; i++) {
			Record rd= getRecord(DEFAULT_PREFIX+devices[i]);
			Record rs= getRecord(SAVED_PREFIX+devices[i]);
			vl.copyMetaData(rd, i);
			vl.copyMetaData(rs, i);
		}
	}

}
