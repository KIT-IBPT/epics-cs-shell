package org.scictrl.csshell.epics.server.application;

import java.util.Arrays;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.Severity;

/**
 * <p>PSSwitchApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class PSSwitchApplication extends AbstractApplication {
	
	private static final String CMD_OFF = "Cmd:Off";
	private static final String CMD_ON =  "Cmd:On";
	
	private static final String SUF_CMD_ON =    ":Cmd:On";
	private static final String SUF_CMD_OFF =   ":Cmd:Off";
	private static final String SUF_STATUS_ON = ":Status:On";
	
	private static final String LINK_ON = "link.on";
	private static final String LINK_OF = "link.of";
	private static final String LINK_ST = "link.st";

	private String[] psPVs;
	private boolean[] select;
	private ValueLinks linkOn;
	private ValueLinks linkOf;
	private ValueLinks linkSt;

	/**
	 * <p>Constructor for PSSwitchApplication.</p>
	 */
	public PSSwitchApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		psPVs = config.getStringArray("psPVs");
		
		if (psPVs==null || psPVs.length==0 || psPVs[0]==null) {
			throw new IllegalArgumentException("Parameter 'forwardsPVs' is missing!");
		}

		select= new boolean[psPVs.length];
		Arrays.fill(select, true);
		
		String[] pvOn = new String[psPVs.length]; 
		String[] pvOf = new String[psPVs.length]; 
		String[] pvSt = new String[psPVs.length];
		
		for (int i = 0; i < pvSt.length; i++) {
			pvOn[i] = psPVs[i]+SUF_CMD_ON;
			pvOf[i] = psPVs[i]+SUF_CMD_OFF;
			pvSt[i] = psPVs[i]+SUF_STATUS_ON;
		}
		
		linkOn = connectLinks(LINK_ON, pvOn);
		linkOf = connectLinks(LINK_OF, pvOf);
		linkSt = connectLinks(LINK_ST, pvSt);
		
		
		addRecordOfCommandProcessor(CMD_ON, "Sets all On", 1000L);
		addRecordOfCommandProcessor(CMD_OFF, "Sets all Off", 1000L);
		
	}
	
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name==CMD_ON) {
			setOn();
		} else if (name==CMD_OFF) {
			setOff();
		}
		
	}
	
	private void updateSelect() {
		try {
			ValueHolder[] vh=  linkSt.getValue();
			for (int i=0; i < vh.length; i++) {
				ValueHolder v= vh[i];
				if (v!=null && !v.failed && v.severity!=null && v.severity.isLessThan(Severity.INVALID_ALARM)) {
					long l= v.longValue();
					select[i] = l!=0L;
				} else {
					select[i] = false;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Remote set failed", e);
		}
	}
	
	private void setOn() {
		try {
			linkOn.setValueToAll(1L, select);
		} catch (Exception e) {
			throw new IllegalStateException("Remote set failed", e);
		}
	}
	
	private void setOff() {
		updateSelect();
		try {
			linkOf.setValueToAll(1L, select);
		} catch (Exception e) {
			throw new IllegalStateException("Remote set failed", e);
		}
	}
	

}
