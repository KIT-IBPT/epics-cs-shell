/**
 * 
 */
package org.scictrl.csshell.epics.server.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import tools.BootstrapLoader;

/**
 * <p>WigglerRampApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class WigglerRampApplication extends AbstractApplication {
	
	private static final String STATUS_ABORTING = "Status:Aborting";
	private static final String FIELD_TABLE_RELOAD_OFF = "Field:Table:ReloadOff";
	private static final String FIELD_TABLE_RELOAD = "Field:Table:Reload";
	private static final String FIELD_TABLE_FILE = "Field:Table:File";
	private static final String RATES_GET = "ratesGet";
	private static final String RATES = "rates";
	private static final String CMD_ABORT = "Cmd:Abort";
	private static final String STATUS_RAMPING = "Status:Ramping";
	private static final String FIELD_SETPOINT = "Field:Setpoint";
	private static final String FIELD_READBACK = "Field:Readback";
	private static final String READBACKS = "readbacks";
	private static final String SETPOINTS_GET = "setpointsGet";
	private static final String SETPOINTS = "setpoints";

	class RampingStep {
		/**
		 * @param startValues
		 * @param endValues
		 * @param startField
		 * @param endField
		 * @param rate
		 */
		public RampingStep(double startField, double endField, double rate, double[] startValues, double[] endValues				) {
			super();
			this.startValues = startValues;
			this.endValues = endValues;
			this.startField = startField;
			this.endField = endField;
			this.rate = rate;
		}
		double[] startValues;
		double[] endValues;
		double startField;
		double endField;
		double rate;
		double time=-1;
		
		public boolean isInRange(int i, double d) {
			return startValues[i]<=d && endValues[i]>=d;
		}
		public double intrapolateField(int i, double d) {
			return startField+(endField-startField)*(d-startValues[i])/(endValues[i]-startValues[i]);
		}
		public boolean isInRange(double field) {
			return startField<=endField ? startField<=field && endField>=field : startField>=field && endField<=field;
		}
		public RampingStep intrapolateEnd(double field) {
			double[] vals= new double[endValues.length];
			
			double x= (field-startField)/(endField-startField);
			
			for (int i = 0; i < vals.length; i++) {
				vals[i]= startValues[i]+(endValues[i]-startValues[i])*x;
			}
			
			return new RampingStep(startField, field, rate, startValues, vals);
		}
		public RampingStep intrapolateStart(double field) {
			double[] vals= new double[startValues.length];
			
			double x= (endField-field)/(endField-startField);
			
			for (int i = 0; i < vals.length; i++) {
				vals[i]= endValues[i]-(endValues[i]-startValues[i])*x;
			}
			
			return new RampingStep(field, endField, rate, vals, endValues);
		}
		public RampingStep intrapolate(double field1, double field2) {
			double[] vals1= new double[endValues.length];
			double[] vals2= new double[startValues.length];
			
			double x1= (field1-startField)/(endField-startField);
			double x2= (endField-field2)/(endField-startField);
			
			for (int i = 0; i < vals1.length; i++) {
				vals1[i]= startValues[i]+(endValues[i]-startValues[i])*x1;
				vals2[i]= endValues[i]-(endValues[i]-startValues[i])*x2;
			}
			
			return new RampingStep(field1, field2, rate, vals1, vals2);
		}
		public double getTime() {
			if (time<0) {
				double max=0;
				
				for (int i = 0; i < startValues.length; i++) {
					if (Math.abs(endValues[i]-startValues[i])>max) {
						max= Math.abs(endValues[i]-startValues[i]);
					}
				}
				
				time= max/rate;
			}
			return time;
		}
		public double[] normalizeRates() {
			double[] rates= new double[startValues.length];
			
			double t= getTime();
			
			for (int i = 0; i < rates.length; i++) {
				rates[i]=Math.abs(endValues[i]-startValues[i])/t;
			}
			
			return rates;
		}
		public RampingStep invert() {
			return new RampingStep(endField, startField, rate, endValues, startValues);
		}
		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder();
			sb.append("RampingStep={");
			sb.append(startField);
			sb.append("->");
			sb.append(endField);
			sb.append(",");
			sb.append(rate);
			sb.append("}");
			return sb.toString();
		}
	}
	
	class RampingSequence {
		List<RampingStep> steps= new ArrayList<WigglerRampApplication.RampingStep>();
		
		public RampingSequence() {
		}
		
		public void addStep(double field, double rate, double[] values) {
			double field0;
			double[] values0;
			
			if (steps.size()>0) {
				RampingStep st= steps.get(steps.size()-1);
				field0= st.endField;
				values0= st.endValues;
				if (st.startField==st.endField) {
					steps.remove(st);
				}
			} else {
				field0=0;
				values0= new double[values.length];
				Arrays.fill(values0, 0);
			}
			
			RampingStep rs= new RampingStep(field0, field, rate, values0, values);
			
			steps.add(rs);
		}

		public double convertToField(double[] values) {

			double[] fields= new double[values.length];
			double sum=0.0;
			
			for (int i = 0; i < fields.length; i++) {
				fields[i]= convertToField(i,values[i]);
				sum+=fields[i];
			}

			return sum/values.length;
		}

		public double[] convertToCurrents(double field) {
			Iterator<RampingStep> it= steps.iterator();
			
			for (;it.hasNext();) {
				RampingStep rs=it.next();
				
				if (rs.isInRange(field)) {
					RampingStep curr= rs.intrapolateStart(field);
					return curr.startValues;
				}
			}
			
			return null;
		}

		private double convertToField(int i, double d) {
			for (RampingStep rs : steps) {
				if (rs.isInRange(i,d)) {
					return rs.intrapolateField(i,d);
				}
			}
			return 0;
		}

		public RampingSequence constructSequence(double startField, double endField) {
			RampingSequence ramp= new RampingSequence();
			
			Iterator<RampingStep> it=null;
			
			if(startField>endField) {
				it= invert().steps.iterator();
			} else {
				it= steps.iterator();
			}
			
			boolean done=false;
			for (;it.hasNext()&&!done;) {
				RampingStep rs=it.next();
				
				if (rs.isInRange(startField)&&rs.isInRange(endField)) {
					ramp.addStep(rs.intrapolate(startField, endField));
					done=true;
				} else if (rs.isInRange(startField)) {
					RampingStep s= rs.intrapolateStart(startField);
					ramp.addStep(s);
				} else if (rs.isInRange(endField)) {
					RampingStep s= rs.intrapolateEnd(endField);
					ramp.addStep(s);
					done=true;
				} else if (ramp.steps.size()>0) {
					ramp.addStep(rs);
				}
			}
			
			if (!done) {
				throw new IllegalArgumentException("Provided field of both fields '"+startField+"', '"+endField+"' are out of range!");
			}
			
			return ramp;
		}

		private void addStep(RampingStep rs) {
			steps.add(rs);
		}

		public RampingSequence invert() {
			RampingSequence ramp= new RampingSequence();
			
			for (RampingStep st : steps) {
				ramp.steps.add(0,st.invert());
			}
			
			return ramp;
		}

		public RampingSequence optimize() {
			RampingSequence ramp= new RampingSequence();
			
			RampingStep step=null;
			for (RampingStep st : steps) {
				if (step==null) {
					step=st;
				} else {
					if (step.rate==st.rate) {
						step= new RampingStep(step.startField, st.endField, step.rate, step.startValues, st.endValues);
					} else {
						ramp.addStep(step);
						step=st;
					}
				}
			}
			if (step!=null) {
				ramp.addStep(step);
			}
			
			return ramp;
		}
		
		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder();
			sb.append("Step # | Field [t] | I1..In [A] | dI/dt [A/s]\n");
			int i=0;
			for (RampingStep rs : steps) {
				sb.append(i);
				sb.append(" | ");
				sb.append(df3.format(rs.startField));
				sb.append(" | ");
				for (double d : rs.startValues) {
					sb.append(df3.format(d));
					sb.append(" | ");
				}
				sb.append(df3.format(rs.rate));
				sb.append("\n");
				sb.append(i++);
				sb.append(" | ");
				sb.append(df3.format(rs.endField));
				sb.append(" | ");
				for (double d : rs.endValues) {
					sb.append(df3.format(d));
					sb.append(" | ");
				}
				sb.append(df3.format(rs.rate));
				sb.append("\n");
			}
			return sb.toString();
		}
		
	}
	
	class RampingTask implements Runnable {
		
		private RampingSequence ramp;
		private boolean aborted=false;

		public RampingTask(RampingSequence ramp) {
			this.ramp=ramp;
			
			getRecord(STATUS_RAMPING).setValue(1);
		}
		
		public synchronized void abort() {
			aborted=true;
			notify();
		}
		
		@Override
		public synchronized void run() {
			try {
				
				log.debug("Ramping started in '"+ramp.steps.size()+"' steps...");
				int i=0;
				for (RampingStep step : ramp.steps) {
					
					if (aborted) {
						log.debug("Ramping aborted!");
						return;
					}
					
					log.debug("Ramping step '"+i+"' from '"+df3.format(step.startField)+"' to '"+df3.format(step.endField)+"'.");

					double[] rates= step.normalizeRates();
					
					getLinks(RATES).setValueToAll(rates);
					
					double[] val= step.endValues;
					
					if (log.isDebugEnabled()) {
						StringBuilder sb= new StringBuilder();
						sb.append("Ramping step currents [");
						for (double d : val) {
							if (sb.length()>23) {
								sb.append(",");
							}
							sb.append(df3.format(d));
						}
						sb.append("]");
						log.debug(sb.toString());
					}
					
					getLinks(SETPOINTS).setValueToAll(val);
					
					double time=step.getTime()*1.075;

					log.debug("Ramping step will take '"+df1.format(time)+"' seconds.");

					if (time<1) {
						time=1;
					}
					
					this.wait((long)(time*1000));
					

					if (aborted) {
						//val= getLinks(READBACKS).consumeAsDoubles();
						//getLinks(SETPOINTS).setValueToAll(val);
						log.debug("Ramping aborted!");
						return;
					}
					i++;
				}
				
				log.debug("Ramping ended.");

				updateLinkError(false, "");
				
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Ramping failed "+e.toString(),e);
				updateLinkError(true, "Failed to execute: "+e.toString());
			} finally {
				getRecord(STATUS_RAMPING).setValue(0);
				getRecord(STATUS_ABORTING).setValue(0);

			}
		}
	}
	
	private String[] psSetpoints;
	private String[] psSetpointsGet;
	private String[] psReadbacks;
	private String[] psRates;
	private String[] psRatesGet;
	
	private RampingSequence rampTable;
	private boolean changing;
	private RampingTask task;
	
	private DecimalFormat df3= new DecimalFormat("0.000");
	private DecimalFormat df1= new DecimalFormat("0.0");
	private File tableFile;


	/**
	 * <p>Constructor for WigglerRampApplication.</p>
	 */
	public WigglerRampApplication() {
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);
		
		addRecordOfMemoryValueProcessor(FIELD_READBACK,"Current readbacks converted to Tesla",null,null,"T",(short)3, 0.0);
		addRecordOfMemoryValueProcessor(FIELD_SETPOINT, "Current setpoints converted to Tesla",null,null,"T",(short)3, 0.0);
		addRecordOfMemoryValueProcessor(STATUS_RAMPING, "Flag indicating ramping in progress", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS_ABORTING, "Flag indicating abort in progress", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(CMD_ABORT, "Aborts ramping task", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(FIELD_TABLE_FILE, "File name of the ramping table", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(FIELD_TABLE_RELOAD, "Reloads the ramping table file", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(FIELD_TABLE_RELOAD_OFF, "Reloading table file not possible", DBRType.BYTE, 0);
		
		
		psSetpoints=config.getStringArray(SETPOINTS);
		psSetpointsGet= config.getStringArray(SETPOINTS_GET);
		psReadbacks= config.getStringArray(READBACKS);
		psRates= config.getStringArray(RATES);
		psRatesGet= config.getStringArray(RATES_GET);
		
		connectLinks(SETPOINTS, psSetpoints);
		connectLinks(SETPOINTS_GET, psSetpointsGet);
		connectLinks(READBACKS, psReadbacks);
		connectLinks(RATES, psRates);
		connectLinks(RATES_GET, psRatesGet);
		
		String file= config.getString("table.file", "");
		
		if (file.length()>0) {
			
			tableFile= BootstrapLoader.getInstance().getApplicationConfigFile("WigglerRamp", file);

			getRecord(FIELD_TABLE_RELOAD_OFF).setValue(0);

			reloadTableFile();
			
		} else {

			getRecord(FIELD_TABLE_FILE).setValue("Table loaded from internal configuration, reload does not work.");
			getRecord(FIELD_TABLE_RELOAD_OFF).setValue(1);

			rampTable= new RampingSequence();

			log.info("Reading table form configuration");				
			List<?> table= config.configurationsAt("table");
			
			Iterator<?> it= table.iterator();
			while (it.hasNext()) {
				HierarchicalConfiguration row= (HierarchicalConfiguration) it.next();
				
				double field= row.getDouble("field");
				double rate= row.getDouble("rate");
				String[] sval= row.getStringArray("values");
				double[] val=new double[sval.length];
				for (int i = 0; i < val.length; i++) {
					val[i]= Double.parseDouble(sval[i]);
				}
				
				rampTable.addStep(field, rate, val);
				
			}
		}
		
		double min=rampTable.steps.get(0).startField;
		double max=rampTable.steps.get(rampTable.steps.size()-1).endField;				

		log.info("Table loaded, contains '"+rampTable.steps.size()+"' entries from '"+min+"T' till '"+max+"T'.");				

		Record r= getRecord(FIELD_SETPOINT);
		r.setMinMax(min,max);
		r.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
		
		r= getRecord(FIELD_READBACK);
		r.setMinMax(min,max);
		
		if (getRecord(ERROR_SUM).getAlarmSeverity()==Severity.INVALID_ALARM) {
			updateErrorSum(Severity.NO_ALARM, Status.NO_ALARM);
		}
		if (getRecord(LINK_ERROR).getAlarmSeverity()==Severity.INVALID_ALARM) {
			updateLinkError(false, "");
		}
	}
	
	private void reloadTableFile() {
		if (tableFile==null) {
			log.warn("Table can not be loaded, since it is provided by internal server confguration.");
			return;
		}
		if (tableFile.exists()) {
			log.info("Reading table form: "+tableFile.toString());				
			try {
				BufferedReader r= new BufferedReader(new FileReader(tableFile));
				
				RampingSequence rt = new RampingSequence();

				
				while(r.ready()) {
					String l= r.readLine();
					String[] s= l.split("\t");

					double field= Double.parseDouble(s[0]);
					double rate= Double.parseDouble(s[5]);
					double[] val=new double[4];
					for (int i = 0; i < val.length; i++) {
						val[i]= Double.parseDouble(s[i+1]);
					}
					
					rt.addStep(field, rate, val);
				}
				r.close();
				rampTable=rt;
				getRecord(FIELD_TABLE_FILE).setValue("Loaded: "+tableFile.getAbsolutePath());
				if (log.isDebugEnabled()) {
					log.debug("Loaded: "+tableFile.getAbsolutePath()+", new sequence:\n"+rt.toString()+"\nend of sequence.");
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.error("Loading from file '"+tableFile.toString()+"' failed: "+e.toString(),e);				
				getRecord(FIELD_TABLE_FILE).setValue("Load failed: "+e.toString());
			}
			
		} else {
			log.error("Could not load table, file does not exist: "+tableFile.toString());				
			getRecord(FIELD_TABLE_FILE).setValue("Table loaded from internal configuration, reload does not work.");
		}

	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		if (name==SETPOINTS_GET && getLinks(SETPOINTS_GET).isReady()) {
			if (getRecord(FIELD_SETPOINT).getAlarmStatus()==Status.UDF_ALARM) {
				double[] values= getLinks(SETPOINTS_GET).consumeAsDoubles();
				double field= rampTable.convertToField(values);
				changing=true;
				log.info(FIELD_SETPOINT+" initial value set '"+field+"'.");
				getRecord(FIELD_SETPOINT).setValue(field);
				getRecord(FIELD_SETPOINT).updateAlarm(Severity.NO_ALARM, Status.NO_ALARM, true);
				changing=false;
			}
		} else if (name==READBACKS && getLinks(READBACKS).isReady()) {
			double[] values= getLinks(READBACKS).consumeAsDoubles();
			double field= rampTable.convertToField(values);
			getRecord(FIELD_READBACK).setValue(field);
		}
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name==FIELD_SETPOINT) {
			if (changing) {
				log.debug("Ramp request denied");
				return;
			}
			getRecord(FIELD_SETPOINT).updateAlarm(Severity.NO_ALARM, Status.NO_ALARM, true);
			double[] values= getLinks(SETPOINTS_GET).consumeAsDoubles();
			double startField= rampTable.convertToField(values);
			double endField= getRecord(FIELD_SETPOINT).getValueAsDouble();
			
			if (Math.abs(startField-endField)<0.001) {
				log.info("Ramp request denied, change too small, to '"+df3.format(endField)+"' from '"+df3.format(startField)+"'.");
			}
			log.info("Ramp request to '"+df3.format(endField)+"' from '"+df3.format(startField)+"'.");
			
			RampingSequence ramp= rampTable.constructSequence(startField,endField);
			
			for (int i = 1; i < ramp.steps.size(); i++) {
				if (Math.abs(ramp.steps.get(i-1).endField-ramp.steps.get(i).startField)>0.0001) {
					log.error("Step "+(i-1)+" fiels ("+ramp.steps.get(i-1).endField+"T) does not match with step "+i+" field ("+ramp.steps.get(i).startField+"T)!");
					updateErrorSum(Severity.MAJOR_ALARM, Status.CALC_ALARM);
					return;
				}
			}
			
			//ramp= ramp.optimize();
			
			task= new RampingTask(ramp);
			database.schedule(task, 0);
			
		} else if (name==CMD_ABORT) {
			if (task!=null) {
				synchronized (task) {
					if (task!=null) {
						getRecord(STATUS_ABORTING).setValue(1);
						task.abort();
					}
				}
			}
		} else if (name==FIELD_TABLE_RELOAD) {
			if (getRecord(STATUS_RAMPING).getValueAsBoolean()) {
				return;
			}
			reloadTableFile();
		}
	}

}
