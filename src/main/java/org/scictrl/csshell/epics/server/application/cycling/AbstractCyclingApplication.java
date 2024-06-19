/**
 * 
 */
package org.scictrl.csshell.epics.server.application.cycling;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>Abstract AbstractCyclingApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public abstract class AbstractCyclingApplication extends AbstractApplication {
	
	private static SimpleDateFormat FORMAT= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Cycling status.
	 */
	public static enum Status {
		
		/**
		 * Cycler is ready.
		 */
		READY,
		/**
		 * Cyclign in progress.
		 */
		CYCLING,
		/**
		 * Cyclign has been aborted.
		 */
		ABORT,
		/**
		 * Connection fail.
		 */
		CONN_FAIL,
		/**
		 * General error.
		 */
		ERROR;

		/**
		 * State labels.
		 */
		public static String[] LABELS= {"Ready","Cycling","Abort","Connect Fail","Error"};
		
	}

	/** Constant <code>DURATION="Duration"</code> */
	public static final String DURATION 	= "Duration";
	/** Constant <code>DEVICE="Device"</code> */
	public static final String DEVICE 		= "Device";
	/** Constant <code>PROGRESS="Progress"</code> */
	public static final String PROGRESS 	= "Progress";
	/** Constant <code>CYCLE="Cycle"</code> */
	public static final String CYCLE 		= "Cycle";
	/** Constant <code>CYCLE_TOP="CycleTop"</code> */
	public static final String CYCLE_TOP 	= "CycleTop";
	/** Constant <code>ABORT="Abort"</code> */
	public static final String ABORT 		= "Abort";
	
	/** Constant <code>STATUS="Status"</code> */
	public static final String STATUS 			= "Status";
	/** Constant <code>STATUS_DESC="Status:Desc"</code> */
	public static final String STATUS_DESC 		= "Status:Desc";
	/** Constant <code>STATUS_LAST="Status:Last"</code> */
	public static final String STATUS_LAST 		= "Status:Last";
	/** Constant <code>STATUS_LAST_STR="Status:Last:Str"</code> */
	public static final String STATUS_LAST_STR 	= "Status:Last:Str";
	
	/** Constant <code>DEVICE_MAX_LIMIT="Device:MaxLimit"</code> */
	public static final String DEVICE_MAX_LIMIT 	= "Device:MaxLimit";
	/** Constant <code>DEVICE_MIN_LIMIT="Device:MinLimit"</code> */
	public static final String DEVICE_MIN_LIMIT 	= "Device:MinLimit";
	/** Constant <code>DEVICE_FINAL_VALUE="Device:FinalValue"</code> */
	public static final String DEVICE_FINAL_VALUE 	= "Device:FinalValue";
	
	
	
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
		updateDuration(param);
	}

	private String device;
	
	/**
	 * If it is linear based.
	 */
	protected boolean linear=false;

	private CyclingParameters parameters;

	private MetaData metaData;
	private PropertyChangeListener metadataCallback;
	
	
	/**
	 * <p>Getter for the field <code>metaData</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * <p>Getter for the field <code>device</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getDevice() {
		return device;
	}
	
	/**
	 * <p>Getter for the field <code>parameters</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 */
	public CyclingParameters getParameters() {
		return parameters;
	}

	/**
	 * <p>configureDevice.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 */
	protected void configureDevice(HierarchicalConfiguration config) {
		if (device==null) {
			device= config.getString("device");
			if (device == null) {
				log4error("Device name is missing!");
				throw new IllegalStateException("Device name is missing!");
			}
		}
		
		linear= config.getBoolean("linear", false);
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		configureDevice(config);
		
		parameters= getCyclingParameters(device);
		if (parameters==null) {
			parameters= getCyclingParameters("default");
		}
		if (parameters==null) {
			parameters= new CyclingParameters(1,10,1.0,10.0,0.0,0.0,0.0,0.0,true,false,0.05);
		}

		
		addRecordOfMemoryValueProcessor(DEVICE, "The Power Supply which is cycled by this process.", DBRType.STRING, device);
		addRecordOfMemoryValueProcessor(PROGRESS, "Progress meeter", 0.0, 100.0, "%", (short)2, 0.0);
		addRecordOfMemoryValueProcessor(STATUS, "Status of cycling process",Status.LABELS, (short)0);
		addRecordOfMemoryValueProcessor(CYCLE, "Starts the cycling process.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CYCLE_TOP, "Starts the top-cycling process.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(ABORT, "Aborts the cycling process.", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS_DESC, "Last status description.", DBRType.STRING, "OK");
		
		addRecordOfMemoryValueProcessor(STATUS_LAST, "Last time cycled", DBRType.INT, 0);
		addRecordOfMemoryValueProcessor(STATUS_LAST_STR, "Last time cycled", DBRType.STRING, "");

		addRecordOfMemoryValueProcessor(CyclingParameters.NO_CYCLES, "Number of cycles.", 0, 20, "No.", parameters.getNoCycles());
		addRecordOfMemoryValueProcessor(CyclingParameters.STEPS_PER_RAMP, "Steps per ramp up or down.", 0, 3000, "No.", parameters.getStepsPerRamp());
		addRecordOfMemoryValueProcessor(CyclingParameters.WAIT_BETWEEN_STEPS, "Wait time in seconds between steps.", 0.0, 600.0, "s", (short)1 , parameters.getWaitBetweenSteps());
		addRecordOfMemoryValueProcessor(CyclingParameters.WAIT_AT_LIMITS, "Wait time in seconds at limits.", 0.0, 60.0, "s", (short)1, parameters.getWaitAtLimits());
		addRecordOfMemoryValueProcessor(CyclingParameters.FINAL_VALUE, "Final value after last cycle.", 0.0, 100.0, "A", (short)2, parameters.getFinalValue());
		addRecordOfMemoryValueProcessor(CyclingParameters.CYCLE_DECREMENT, "Decrement of effective limits in % each cycle.", 0.0, 100.0, "%", (short)2, parameters.getCycleDecrement());
		addRecordOfMemoryValueProcessor(CyclingParameters.USE_DEVICE_LIMITS, "If limits from device should be used.", DBRType.BYTE, parameters.isUseDeviceLimits()?(byte)1:(byte)0);
		addRecordOfMemoryValueProcessor(CyclingParameters.MAX_LIMIT, "Cycling max value limit", 0.0, 100.0, "A", (short)2, parameters.getMaxLimit());
		addRecordOfMemoryValueProcessor(CyclingParameters.MIN_LIMIT, "Cycling min value limit", 0.0, 100.0, "A", (short)2, parameters.getMinLimit());
		addRecordOfMemoryValueProcessor(CyclingParameters.STARTING_AS_FINAL, "Use startign value as final.", DBRType.BYTE, parameters.isStartingAsFinal()?(byte)1:(byte)0);
		addRecordOfMemoryValueProcessor(CyclingParameters.TOP_SCALE, "Ration of min-max scale to be used for top cycling.", 0.0, 1.0, "%", (short)2, parameters.getTopScale());
		addRecordOfMemoryValueProcessor(DURATION,"Estimated time of full cycle.",0.0,100.0,"min",(short)1,0.0);

		try {
			metaData= restoreMetaData();
		} catch (Exception e) {
			log4error("Failed to restore metadata", e);
		}

		addRecord(DEVICE_MAX_LIMIT, MemoryValueProcessor.newProcessor(fullRecordName(DEVICE_MAX_LIMIT), DBRType.DOUBLE, 1, "Cycling max value limit from device", new double[]{metaData!=null?metaData.getMaximum():0.0}, false, 0.0, 100.0, "A", (short)2).getRecord());
		addRecord(DEVICE_MIN_LIMIT, MemoryValueProcessor.newProcessor(fullRecordName(DEVICE_MIN_LIMIT), DBRType.DOUBLE, 1, "Cycling min value limit from device", new double[]{metaData!=null?metaData.getMinimum():0.0}, false, 0.0, 100.0, "A", (short)2).getRecord());
		addRecord(DEVICE_FINAL_VALUE, MemoryValueProcessor.newProcessor(fullRecordName(DEVICE_FINAL_VALUE), DBRType.DOUBLE, 1, "Final value from device before cycling", new double[]{0.0}, false, 0.0, 100.0, "A", (short)2).getRecord());
		
		if (metaData!=null) {
			getRecord(CyclingParameters.FINAL_VALUE).copyFields(metaData);
			getRecord(CyclingParameters.MAX_LIMIT).copyFields(metaData);
			getRecord(CyclingParameters.MIN_LIMIT).copyFields(metaData);
		}
	
		updateDuration(parameters);
	}
	
	/**
	 * Implement this to provide inital metadata for the cycler.
	 * If possible use helper method {@link #restoreMetaData(String)}.
	 *
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	protected abstract MetaData restoreMetaData();
	
	/**
	 * Restores metadata from local persistency store, which is identified by PV name.
	 *
	 * @param pvName the name of PV that provides the metadata.
	 * @return metadata from local presistancy store
	 */
	protected MetaData restoreMetaData(String pvName) {
		return restore(getStore(), pvName);
	}

	/**
	 * <p>updateLastTimeCycled.</p>
	 */
	protected void updateLastTimeCycled() {
		
		long t= System.currentTimeMillis();
		String s= FORMAT.format(new Date(t));
		
		getRecord(STATUS_LAST).setValue(t);
		getRecord(STATUS_LAST_STR).setValue(s);
		
	}
	

	/**
	 * <p>updateDuration.</p>
	 *
	 * @param p a {@link org.scictrl.csshell.epics.server.application.cycling.CyclingParameters} object
	 */
	protected void updateDuration(CyclingParameters p) {
		double slope= p.getWaitBetweenSteps()*p.getStepsPerRamp();
		double cycle= 2.0 * (slope + p.getWaitAtLimits());
		double d= p.getNoCycles() * cycle + slope;
		
		getRecord(DURATION).setValue(d/60);
	}
	
	private void updateMetaData(MetaData metaData) {
		if (metaData!=null && metaData.isValid()) {
			getRecord(CyclingParameters.FINAL_VALUE).copyFields(metaData);
			getRecord(CyclingParameters.MAX_LIMIT).copyFields(metaData);
			getRecord(CyclingParameters.MIN_LIMIT).copyFields(metaData);
			getRecord(DEVICE_MIN_LIMIT).setValue(metaData.getMinimum());
			getRecord(DEVICE_MAX_LIMIT).setValue(metaData.getMaximum());
			store(getStore(),metaData);
			this.metaData=metaData;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		
		if (parameters.isUseDeviceLimits()) {
			try {
				getAsyncMetadata();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		super.activate();
	}

	/**
	 * This method is called from activate and initiate asynchonous get for metadata.
	 * If possible
	 *
	 * @throws java.lang.Exception if fails
	 */
	protected abstract void getAsyncMetadata() throws Exception;
	
	
	/**
	 * Defautl implementation of {@link #getAsyncMetadata()}, which uses PV to get remote metadata asynchronously.
	 *
	 * @param pvName a {@link java.lang.String} object
	 * @throws java.lang.Exception remote exception
	 */
	protected void getAsyncMetadata(String pvName) throws Exception {
		database.getConnector().getMetaDataAsync(pvName, DataType.DOUBLE, metadataCallback=new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				MetaData md= (MetaData) evt.getNewValue();
				if (md.isValid() && md.getMaximum()>md.getMinimum() && (md.getMaximum()-md.getMinimum() > 0.01 )) {
					updateMetaData((MetaData) evt.getNewValue());
				}
			}
		});
	}

	/** {@inheritDoc} */
	@Override
	protected void notifyRecordChange(String name, boolean alarmOnly) {
		
		if (name==CyclingParameters.FINAL_VALUE) {
			parameters= parameters.withParameter(CyclingParameters.FINAL_VALUE,getRecord(CyclingParameters.FINAL_VALUE).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.NO_CYCLES) {
			parameters= parameters.withParameter(CyclingParameters.NO_CYCLES,getRecord(CyclingParameters.NO_CYCLES).getValueAsInt());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.STEPS_PER_RAMP) {
			parameters= parameters.withParameter(CyclingParameters.STEPS_PER_RAMP,getRecord(CyclingParameters.STEPS_PER_RAMP).getValueAsInt());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.WAIT_AT_LIMITS) {
			parameters= parameters.withParameter(CyclingParameters.WAIT_AT_LIMITS,getRecord(CyclingParameters.WAIT_AT_LIMITS).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.WAIT_BETWEEN_STEPS) {
			parameters= parameters.withParameter(CyclingParameters.WAIT_BETWEEN_STEPS,getRecord(CyclingParameters.WAIT_BETWEEN_STEPS).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.CYCLE_DECREMENT) {
			parameters= parameters.withParameter(CyclingParameters.CYCLE_DECREMENT,getRecord(CyclingParameters.CYCLE_DECREMENT).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.USE_DEVICE_LIMITS) {
			parameters= parameters.withParameter(CyclingParameters.USE_DEVICE_LIMITS,getRecord(CyclingParameters.USE_DEVICE_LIMITS).getValueAsInt());
			if (parameters.isUseDeviceLimits() && metadataCallback==null) {
				try {
					getAsyncMetadata();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.MAX_LIMIT) {
			parameters= parameters.withParameter(CyclingParameters.MAX_LIMIT,getRecord(CyclingParameters.MAX_LIMIT).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.MIN_LIMIT) {
			parameters= parameters.withParameter(CyclingParameters.MIN_LIMIT,getRecord(CyclingParameters.MIN_LIMIT).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.STARTING_AS_FINAL) {
			int b=getRecord(CyclingParameters.STARTING_AS_FINAL).getValueAsInt(); 
			parameters= parameters.withParameter(CyclingParameters.STARTING_AS_FINAL,b);
			storeCyclingParameters(device, parameters);
		} else if (name==CyclingParameters.TOP_SCALE) {
			parameters= parameters.withParameter(CyclingParameters.TOP_SCALE,getRecord(CyclingParameters.TOP_SCALE).getValueAsDouble());
			storeCyclingParameters(device, parameters);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	protected void notifyRecordWrite(String name) {
		
		if (name==CYCLE) {
			
			doCycle();
			
		} if (name==CYCLE_TOP) {
				
			doCycleTop();
				
		} else if (name==ABORT) {
			
			doAbort();
			
		}
		
	}

	/**
	 * <p>doAbort.</p>
	 */
	protected abstract void doAbort();

	/**
	 * <p>doCycle.</p>
	 */
	protected abstract void doCycle();
	
	/**
	 * <p>doCycleTop.</p>
	 */
	protected abstract void doCycleTop();

	/**
	 * <p>setProgress.</p>
	 *
	 * @param p a double
	 */
	protected void setProgress(double p) {
		Record r= getRecord(PROGRESS);
		r.setValue(new double[]{p*100.0});
	}

	/**
	 * <p>setStatus.</p>
	 *
	 * @param status a {@link org.scictrl.csshell.epics.server.application.cycling.AbstractCyclingApplication.Status} object
	 */
	protected void setStatus(Status status) {
		Record r= getRecord(STATUS);
		r.setValue(new short[]{(short)status.ordinal()});
	}
	
	
}
