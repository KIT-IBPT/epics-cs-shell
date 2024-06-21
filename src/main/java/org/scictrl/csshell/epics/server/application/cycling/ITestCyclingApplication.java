/**
 * 
 */
package org.scictrl.csshell.epics.server.application.cycling;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.server.Database;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.Severity;

/**
 * Implementation of cycling application, which uses setPV and getPV to implement cycling
 *
 * @author igor@scictrl.com
 */
public class ITestCyclingApplication extends AbstractCyclingApplication {

	private class Operator {
		
		boolean aborted=false;
		private EPICSConnection<Double> conFinal;
		private EPICSConnection<Double> conMax;
		private EPICSConnection<Double> conMin;
		private EPICSConnection<Long> conStart2;
		private EPICSConnection<Long> conStart3;
		private EPICSConnection<Double> conStep;
		private EPICSConnection<Long> conSync;
		public CyclingParameters parameters;
		public double max;
		public double min;
		public double fin;
		// time bwtween values in start2 it is used once, in start3 it is used 2 times.
		protected double stepRate;
		
		@SuppressWarnings("unchecked")
		public Operator(CyclingParameters cyclingParameters, String pvFinal, String pvMax, String pvMin, String pvStart2, String pvStart3, String pvStep, String pvSync, Database db) throws RemoteException {
			
			parameters=cyclingParameters;
			
			if (!debugLocal) {
				conFinal= (EPICSConnection<Double>)db.getConnector().newConnection(pvFinal, DataType.DOUBLE); 
				conMax= (EPICSConnection<Double>)db.getConnector().newConnection(pvMax, DataType.DOUBLE); 
				conMin= (EPICSConnection<Double>)db.getConnector().newConnection(pvMin, DataType.DOUBLE); 
				conStart2= (EPICSConnection<Long>)db.getConnector().newConnection(pvStart2, DataType.LONG); 
				conStart3= (EPICSConnection<Long>)db.getConnector().newConnection(pvStart3, DataType.LONG); 
				conStep= (EPICSConnection<Double>)db.getConnector().newConnection(pvStep, DataType.DOUBLE); 
				conSync= (EPICSConnection<Long>)db.getConnector().newConnection(pvSync, DataType.DOUBLE); 
				
				conFinal.waitTillConnected();
				conMax.waitTillConnected();
				conMin.waitTillConnected();
				conStart2.waitTillConnected();
				conStart3.waitTillConnected();
				conStep.waitTillConnected();
				conSync.waitTillConnected();
	
				check(conFinal);
				check(conMax);
				check(conMin);
				check(conStep);
				check(conStart2);
				check(conStart3);
				check(conSync);
			}
			
		}
		
		private void check(EPICSConnection<?> con) throws RemoteException {
			if (con.getStatus().isSet(org.scictrl.csshell.Status.State.FAILED) ) {
				throw new RemoteException(con, "["+con.getName()+"] Connection failed !");
			}
		}
		
		public void abortAndRelease() {
			if (!aborted && !debugLocal) {
				aborted=true;
				//conFinal.destroy();
				//conMax.destroy();
				//conMin.destroy();
				//conStart2.destroy();
				//conStart3.destroy();
				//conStep.destroy();
				//conSync.destroy();
			}
		}
		
		public boolean isAborted() {
			return aborted;
		}

		public void start2() throws RemoteException {
			if (debugLocal) {
				return;
			}
			conStep.setValue(stepRate);
			conMax.setValue(max);
			conMin.setValue(min);
			conStart2.setValue(1L);
		}
		
		public void start3() throws RemoteException {
			if (debugLocal) {
				return;
			}
			conStep.setValue(stepRate);
			conMax.setValue(max);
			conMin.setValue(min);
			conFinal.setValue(fin);
			conStart3.setValue(1L);
		}
		
		public void sync() throws RemoteException {
			if (debugLocal) {
				return;
			}
			conSync.setValue(1L);
		}
		
		/** 
		 * Reduce max and min by decreement procantage
		 * 
		 */
		public void decreaseLimits(double decrement) {
			max=max*(1.0-decrement);
			min=min*(1.0-decrement);
		}
	}
	
	private boolean debugLocal=false;
	
	private String pvSet;
	private String pvSync;
	private String pvFinal;
	private String pvMax;
	private String pvMin;
	private String pvStart2;
	private String pvStart3;
	private String pvStep;

	private Operator cycler=null;

	/**
	 * Constructor.
	 */
	public ITestCyclingApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {

		configureDevice(config);
		
		pvSet   = getDevice()+":Current:Setpoint";
		pvSync   = getDevice()+":Current:Sync";
		pvFinal = getDevice()+":Current:Cycle:Final";
		pvMax   = getDevice()+":Current:Cycle:Max";
		pvMin   = getDevice()+":Current:Cycle:Min";
		pvStart2 = getDevice()+":Current:Cycle:Start2";
		pvStart3 = getDevice()+":Current:Cycle:Start3";
		pvStep  = getDevice()+":Current:Cycle:StepTime";
		
		
		super.configure(name, config);
		
		Record r= getRecord(CyclingParameters.STEPS_PER_RAMP);
		r.setMinMax(1, 1);
		r.setValue(1);
		
		r= getRecord(CyclingParameters.WAIT_BETWEEN_STEPS);
		if (r.getValueAsDouble()<10) {
			r.setValue(10.0);
		}
		r.setMinMax(10.0, 10.0);
		
		
	}

	/** {@inheritDoc} */
	@Override
	protected MetaData restoreMetaData() {
		return restoreMetaData(pvSet);
	}

	/** {@inheritDoc} */
	@Override
	protected void getAsyncMetadata() throws Exception {
		getAsyncMetadata(pvSet);
	}
	

	/** {@inheritDoc} */
	@Override
	protected void doCycle() {
		
		_doCycle(false);
		
	}
	
	/** {@inheritDoc} */
	@Override
	protected void doCycleTop() {
		
		_doCycle(true);
		
	}

	private void _doCycle(boolean top) {
		
		if (cycler!=null) {
			return;
		}
		
		try {
			
			cycler= new Operator(getParameters(), pvFinal, pvMax, pvMin, pvStart2, pvStart3, pvStep, pvSync, database);

			int no= cycler.parameters.getNoCycles(); 
			if (no == 0) {
				reportError("No cycles to perform.");
				return;
			}

			cycler.max = cycler.parameters.getMaxLimit();	
			cycler.min = cycler.parameters.getMinLimit();
			
			if (cycler.parameters.isUseDeviceLimits()) {
				if (getMetaData()!=null) {
					cycler.max= getMetaData().getMaximum();
					cycler.min= getMetaData().getMinimum();
				}
			}
	
			if (cycler.max<cycler.min || cycler.max==cycler.min || cycler.max-cycler.min<0.1) {
				reportError("Min ("+cycler.min+") and max ("+cycler.max+") not usable.");
				return;
			}

			cycler.fin= cycler.parameters.getFinalValue();

			if (cycler.parameters.isStartingAsFinal()) {
				cycler.fin= (double) database.getConnector().getValue(pvSet, DataType.DOUBLE);
			}
			
			if (top) {
				
				double scale= cycler.parameters.getTopScale();
				
				double topAmp= (cycler.max-cycler.min)*scale;
				double topMin=cycler.fin-topAmp; 
				double topMax=cycler.fin+topAmp; 
				
				if (cycler.max>topMax) {
					cycler.max=topMax;
				}
				
				if (cycler.min<topMin) {
					cycler.min=topMin;
				}
				
				double dec= 1.0/(double)no;
				
				cycler.parameters=cycler.parameters.withParameter(CyclingParameters.CYCLE_DECREMENT, dec);
			}

			setStatus(Status.CYCLING);
			
			reportProgress(0.0);

			Thread t= new Thread("Cycler-"+System.currentTimeMillis()) {
				@Override
				public synchronized void run() {
					
					try {
						// up/down the slope and wait at the end and back again
						cycler.stepRate= 
								cycler.parameters.getWaitBetweenSteps()*
								(cycler.parameters.getStepsPerRamp()+1)+
								cycler.parameters.getWaitAtLimits();
						
						//System.out.println("STEPTIME "+cycler.stepRate);
						
						long startTime=System.currentTimeMillis();
						// each cycle has slope up, 2 slopes down, a slope back.
						long endTime=(long)(startTime+cycler.stepRate*1000.0*(no*2.0+1.0));

						//System.out.println("END "+endTime);
						//long t= endTime-startTime;
						//System.out.println("DURATION "+t+" "+(double)t/60000);

						for (int i = 0; i < no; i++) {
							
							//System.out.println("CYCLENO "+i);
							
							if (cycler.isAborted()) {
								return;
							}
							
							long cycleTime=0;
	
							if (i==no-1) {
								cycler.start3();
								
								cycleTime=(long)(System.currentTimeMillis()+cycler.stepRate*1000.0*3.0);
	
							} else {
								cycler.start2();
								
								cycleTime=(long)(System.currentTimeMillis()+cycler.stepRate*1000.0*2.0);
							}
							
							if (cycler.parameters.getCycleDecrement()>0.0 && cycler.parameters.getCycleDecrement()<100.0) {
								cycler.decreaseLimits(cycler.parameters.getCycleDecrement()/100.0);
							}
							
							while (System.currentTimeMillis()<cycleTime) {
								//System.out.println("CYCLETIME "+System.currentTimeMillis()+" "+cycleTime);
								this.wait(1000);
								if (cycler.isAborted()) {
									return;
								}
								double d= (double)(System.currentTimeMillis()-startTime)/(endTime-startTime);
								//System.out.println("PROGRESS "+d);
								setProgress(d>1.0?1.0:d);
							}
						}
						
						setProgress(1.0);
						setStatus(Status.READY);
						updateLastTimeCycled();
						
					} catch (Exception e) {
						log4error("Cycling failed!", e);
						setStatus(Status.ERROR);
						updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.STATE_ALARM);
						
					} finally {
						if (cycler!=null) {
							try {
								cycler.sync();
							} catch (RemoteException e) {
								log4error("Sync failed!", e);
							}
							cycler.abortAndRelease();
							cycler=null;
						}
					}
				}
			};
			
			t.start();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(Status.ERROR);
			updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.STATE_ALARM);
			
			if (cycler!=null) {
				cycler.abortAndRelease();
				cycler=null;
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doAbort() {
		setStatus(Status.ABORT);
		if (cycler!=null) {
			cycler.abortAndRelease();
			cycler=null;
		}
	}
	
	private void reportProgress(double p) {
		updateLinkError(false, "");
		updateErrorSum(Severity.NO_ALARM, gov.aps.jca.dbr.Status.NO_ALARM);
		setProgress(p);
	}
	
	private void reportError(String m) {
		setStatus(Status.ERROR);
		if (cycler!=null) {
			getRecord(STATUS_DESC).setValueAsString(m);
			cycler.abortAndRelease();
			cycler=null;
		}

	}


}
