package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>PowerSupply class.</p>
 *
 * @author igor@scictrl.com
 */
public class PowerSupply extends AbstractApplication {

	private static final String CURRENT_READBACK = "Current:Readback";
	private static final String CURRENT_SETPOINT_RATE = "Current:Setpoint:Rate";
	private static final String CURRENT_SETPOINT_GET = "Current:Setpoint:Get";
	private static final String CURRENT_SETPOINT = "Current:Setpoint";
	private static final String CURRENT_SETPOINT_RATE_GET = "Current:Setpoint:Rate:Get";
	double precision= 0.01;
	double output= 0.0;
	
	/**
	 * <p>Constructor for PowerSupply.</p>
	 */
	public PowerSupply() {
	}


	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		addRecordOfMemoryValueProcessor(CURRENT_SETPOINT, "Setpoint",null,null,"A",(short)4,0.0);
		addRecordOfMemoryValueProcessor(CURRENT_SETPOINT_GET, "Setpoint get",null,null,"A",(short)4,0.0);
		addRecordOfMemoryValueProcessor(CURRENT_READBACK, "Readback",null,null,"A",(short)4, 0.0);
		addRecordOfMemoryValueProcessor(CURRENT_SETPOINT_RATE, "Change Rate", DBRType.DOUBLE, 1.0);
		addRecordOfMemoryValueProcessor(CURRENT_SETPOINT_RATE_GET, "Change Rate", DBRType.DOUBLE, 1.0);
		addRecordOfMemoryValueProcessor("Current:Sync", "Setpoint Sync", DBRType.DOUBLE, 0.0);
		addRecordOfMemoryValueProcessor("Cmd:On", "Command On", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Cmd:Off", "Command Off", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Cmd:Reset", "Command Reset", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:On", "On", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit0", "On", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit1", "Error", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit2", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit3", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit4", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit5", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit6", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit7", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit8", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit9", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit10", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit11", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit12", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit13", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit14", "N/A", DBRType.BYTE, 0.0);
		addRecordOfMemoryValueProcessor("Status:Bit15", "N/A", DBRType.BYTE, 0.0);
	
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		database.schedule(new Runnable() {
			
			@Override
			public void run() {
				
				double setpoint= getRecord(CURRENT_SETPOINT).getValueAsDouble();
				if (Math.abs(setpoint-output)>precision) {
					
					double rate= getRecord(CURRENT_SETPOINT_RATE).getValueAsDouble();
					if (Math.abs(setpoint-output)>rate) {
						output= output + Math.signum(setpoint-output) * rate;
					} else {
						output= setpoint;
					}
				}
				
				double out= output+(Math.random()-0.5)*2*precision;
				
				getRecord(CURRENT_READBACK).setValue(out);
			}
		}, 1000, 1000);
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (CURRENT_SETPOINT==name) {
			getRecord(CURRENT_SETPOINT_GET).setValue(getRecord(CURRENT_SETPOINT).getValue());
		} else if (CURRENT_SETPOINT_RATE==name) {
			getRecord(CURRENT_SETPOINT_RATE_GET).setValue(getRecord(CURRENT_SETPOINT_RATE).getValue());
		}
	}
	
	
}
