/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.util.Arrays;

import org.apache.commons.configuration.HierarchicalConfiguration;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_String;

/**
 * <p>OrbitCheckApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class OrbitCheckApplication extends AbstractApplication {
	
	private String bpmName;
	@SuppressWarnings("unused")
	private String bpmPV;
	private String bpmNamesPV;
	private String bbaRefPV;
	private int index;

	/**
	 * <p>Constructor for OrbitCheckApplication.</p>
	 */
	public OrbitCheckApplication() {
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		bpmName= config.getString("bpmName");
		if (bpmName==null) {
			throw new IllegalArgumentException("Property 'bpmName' is not defined");
		}
		bpmPV= config.getString("bpmPV");
		if (bpmName==null) {
			throw new IllegalArgumentException("Property 'bpmPV' is not defined");
		}
		
		bpmNamesPV= config.getString("bpmNamesPV");
		bbaRefPV= config.getString("bbaRefPV");

		index=-1;
		
		connectLinks("bpm", bpmNamesPV);
		connectLinks("bbaRef", bbaRefPV);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		try {
			DBR dbr= database.getConnector().getOneShot(bpmNamesPV, DBRType.STRING);
			if (dbr.isSTRING() && dbr.getCount()>=0) {
				DBR_String ds= (DBR_String)dbr;
				String[] s= ds.getStringValue();
				for (int i = 0; i < s.length; i++) {
					if (bpmName.equals(s[i])) {
						index=i;
						break;
					}
				}
				if (index<0) {
					log4error("Failed to resolve index of '"+bpmName+"' within names: "+Arrays.toString(s));
				}
			} else {
				if (index<0) {
					log4error("Failed to resolve index of '"+bpmName+"', bad names PV.");
				}
			}
		} catch (Exception e) {
			log4error("Failed to resolve index of '"+bpmName+"'", e);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		
	}
	
}
