/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>WaveformSumApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class WaveformSumApplication extends AbstractApplication {
	
	/** Input link PV. */
	protected static final String INPUT = 	 "Input";
	/** Sample value PV.  */
	protected static final String SAMPLE = 	 "Sample";
	/** Sample index PV. */
	protected static final String INDEX = 	 "Index";
	/** Max value PV. */
	protected static final String MAX =		 "Max";
	/** Min value PV, */
	protected static final String MIN = 	 "Min";
	/** Waveform integral PV. */
	protected static final String INTEGRAL = "Integral";

	/** Input PV. */
	protected String psInput;
	/** Sample index. */
	protected int idx=0;
	/** Input link. */
	protected Record input;
	/** Sample value link. */
	protected Record sample;
	/** Max value link. */
	protected Record max;
	/** Min value link. */
	protected Record min;
	/** Waveform integral link. */
	protected Record integral;
	/** Waveform array step between samples. */
	protected double sampleStep;
	/** Units for samples, Y. */
	private String unitsSample;
	/** Units for steps, X. */
	private String unitsStep;
	/** Units for integral. */
	private String unitsIntegral; 
	
	/**
	 * <p>Constructor for WaveformSumApplication.</p>
	 */
	public WaveformSumApplication() {
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		psInput=config.getString(INPUT.toLowerCase());
		idx= config.getInt(INDEX.toLowerCase(),0);
		sampleStep=config.getDouble("sampleStep",1.0);
		unitsSample=config.getString("units.sample","Y");
		unitsStep=config.getString("units.step","X");
		unitsIntegral=config.getString("units.integral",unitsSample+"×"+unitsStep);
		
		addRecordOfOnLinkValueProcessor(INPUT, "Input value", DBRType.DOUBLE, psInput);
		addRecordOfMemoryValueProcessor(INDEX,"Index of sample",0,4096,"",idx);
		addRecordOfMemoryValueProcessor(SAMPLE,"Waveform sample",null,null,unitsSample,(short)2,0.0);
		addRecordOfMemoryValueProcessor(MAX,"Waveform max",null,null,unitsSample,(short)2,0.0);
		addRecordOfMemoryValueProcessor(MIN,"Waveform min",null,null,unitsSample,(short)2,0.0);
		addRecordOfMemoryValueProcessor(INTEGRAL,"Waveform integral",null,null,unitsIntegral,(short)2,0.0);
		
		Record r= getRecord(INDEX);
		r.setPersistent(true);
		idx= r.getValueAsInt();

		input= getRecord(INPUT);
		sample= getRecord(SAMPLE);
		max = getRecord(MAX);
		min= getRecord(MIN);
		integral= getRecord(INTEGRAL);
		
		update();
	}
	
	/**
	 * Implementation must extract array data from record. Any data manipulation should be applied here.
	 * @param input
	 * @return
	 */
	protected double[] extract(Record input) {
		double[] d= input.getValueAsDoubleArray();
		return d;
	}
	
	protected void update() {
		
		int i= idx;
		Severity sev= input.getAlarmSeverity();
		Status sta= input.getAlarmStatus();
		
		if (sev==Severity.NO_ALARM  && sta==Status.NO_ALARM) {
			
			double[] d= extract(input);
			
			if (i>-1 && i<d.length && !Double.isNaN(d[i])) {
				
				sample.updateNoAlarm();
				sample.setValue(d[i]);
				
				double mn= Double.MAX_VALUE;
				double mx= -Double.MAX_VALUE;
				double intg= 0.0;
				
				for (int j = 0; j < d.length; j++) {
					double dd = d[j]; 
					if (dd<mn) {
						mn=dd;
					}
					if (dd>mx) {
						mx=dd;
					}
					intg+=dd;
				}
				
				intg=intg*sampleStep;
				
				min.updateNoAlarm();
				min.setValue(mn);
				max.updateNoAlarm();
				max.setValue(mx);
				integral.setValue(intg);
				
			} else {
				sample.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM);
			}
			
		} else {
			sample.updateAlarm(sev, sta);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);
		
		if (name == INPUT) {
			update();
		} else if (name==INDEX) {
			idx=getRecord(INDEX).getValueAsInt();
			update();
		}

	}

}
