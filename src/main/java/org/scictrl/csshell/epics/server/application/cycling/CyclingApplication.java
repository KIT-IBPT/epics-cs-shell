/**
 * 
 */
package org.scictrl.csshell.epics.server.application.cycling;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.MetaData;

import gov.aps.jca.dbr.Severity;

/**
 * Implementation of cycling application, which uses setPV and getPV to implement cycling
 *
 * @author igor@scictrl.com
 */
public class CyclingApplication extends AbstractCyclingApplication {
	
	/**
	 * PV name for remote get.
	 */
	protected String getPV;
	/**
	 * PV name for remote set.
	 */
	protected String setPV;
	/**
	 * Ramping thread.
	 */
	protected RampingThread cycler;


	/**
	 * Constructor.
	 */
	public CyclingApplication() {
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {

		configureDevice(config);
		
		getPV= config.getString("get_pv",getDevice()+":Current:Setpoint:Get");
		setPV= config.getString("set_pv",getDevice()+":Current:Setpoint");
		
		super.configure(name, config);
		
	}

	/** {@inheritDoc} */
	@Override
	protected MetaData restoreMetaData() {
		return restoreMetaData(setPV);
	}

	/** {@inheritDoc} */
	@Override
	protected void getAsyncMetadata() throws Exception {
		getAsyncMetadata(setPV);
	}

	/** {@inheritDoc} */
	@Override
	protected void doCycleTop() {
		// TODO Auto-generated method stub
		
	}

	/** {@inheritDoc} */
	@Override
	protected void doCycle() {
			
		if (cycler!=null) {
			return;
		}

		try {

			cycler= new RampingThread(getParameters(), getPV, setPV, getMetaData(),database, linear);
			
			setStatus(Status.CYCLING);
			
			cycler.start(new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName()==RampingThread.PROGRESS) {
						updateLinkError(false, "");
						updateErrorSum(Severity.NO_ALARM, gov.aps.jca.dbr.Status.NO_ALARM);
						setProgress((Double)evt.getNewValue());
					} else if (evt.getPropertyName()==RampingThread.ERROR) {
						setStatus(Status.ERROR);
						if (cycler!=null) {
							getRecord(STATUS_DESC).setValueAsString(cycler.getMessage());
							cycler.release();
							cycler=null;
						}
					} else if (evt.getPropertyName()==RampingThread.CONNECTION_FAIL) {
						setStatus(Status.CONN_FAIL);
						updateLinkError(true, "Connection failed!");
						updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.STATE_ALARM);
						if (cycler!=null) {
							getRecord(STATUS_DESC).setValueAsString(cycler.getMessage());
							cycler.release();
							cycler=null;
						}
					} else if (evt.getPropertyName()==RampingThread.ABORT) {
						setStatus(Status.ABORT);
						if (cycler!=null) {
							getRecord(STATUS_DESC).setValueAsString(cycler.getMessage());
							cycler.release();
							cycler=null;
						}
					} else if (evt.getPropertyName()==RampingThread.END) {
						setStatus(Status.READY);
						updateLastTimeCycled();
						if (cycler!=null) {
							getRecord(STATUS_DESC).setValueAsString(cycler.getMessage());
							cycler.release();
							cycler=null;
						}
					} else if (evt.getPropertyName()==DEVICE_FINAL_VALUE) {
						getRecord(DEVICE_FINAL_VALUE).setValue(evt.getNewValue());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			setStatus(Status.ERROR);
			updateErrorSum(Severity.MAJOR_ALARM, gov.aps.jca.dbr.Status.STATE_ALARM);
			
			if (cycler!=null) {
				cycler.release();
				cycler=null;
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected void doAbort() {
		if (cycler!=null) {
			cycler.abort();
		}
	}

}
