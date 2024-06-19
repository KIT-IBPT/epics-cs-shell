/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

/**
 * 
 */
package org.scictrl.csshell.jcamon;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.scictrl.csshell.Connection;
import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.dbr.DBR;

/**
 * <p>JCAMon class.</p>
 *
 * @author igor@scictrl.com
 */
public class JCAMon implements Runnable {

	EPICSConnector ctx;
	EPICSConnection<?> prop;
	SimpleDateFormat f;
	final static String HELP="*** JCA Channel Monitor *** \nUsage: List PV names separated by space.\nNothing to do, done.";
	String plug = "Simulator";
	
	PropertyChangeListener listener= new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			//Poop<?, ?> poop= (Poop<?, ?>)evt.getNewValue();
			//System.out.println(f.format(poop.getTimestamp().getMilliseconds())+' '+poop.getValue()+' '+poop.getStatus());
		}
	};
	private EPICSConnection<?>[] channels;
	private Timer timer;
	private String delim;
	

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setLevel(LogManager.getRootLogger(), Level.ERROR);
		Configurator.setLevel(ConnectorUtilities.getLogger(), Level.ERROR);
		Configurator.setLevel(ConnectorUtilities.getConnectorLogger("EPICS"), Level.ERROR);
		
		
		if (args==null || args.length==0) {
			printHelp();
			return;
		}
		
		try {
			@SuppressWarnings("unused")
			JCAMon ex= new JCAMon(args);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Constructor for JCAMon.</p>
	 *
	 * @throws org.scictrl.csshell.RemoteException remote fail
	 * @param names an array of {@link java.lang.String} objects
	 */
	public JCAMon(String[] names) throws RemoteException {
		super();
		
		String p= System.getProperty("jcamon.dform", "yyyy-MM-dd'T'HH:mm:ss.SSS");
		p=StringEscapeUtils.unescapeJava(p);
		p=StringEscapeUtils.unescapeHtml(p);
		f= new SimpleDateFormat(p);
		

		this.delim= StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeJava(System.getProperty("jcamon.delim", " ")));
		
		ctx= EPICSConnector.newInstance(System.getProperties());
		
		this.channels= new EPICSConnection[names.length];
		
		System.out.println("# Monitoring started");
		System.out.print("# ");
		System.out.println(f.format(new Date()));
		System.out.println("#");


		for (int i = 0; i < names.length; i++) {
			channels[i]=ctx.newConnection(names[i], null);
			channels[i].addPropertyChangeListener(Connection.PROPERTY_POOP, listener);
		}
		
		System.out.print("#Timestamp");
		for (int i = 0; i < channels.length; i++) {
			System.out.print(delim);
			System.out.print(names[i]);
			channels[i].waitTillConnected();
		}
		System.out.println();
		
		
		this.timer= new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				JCAMon.this.run();
			}
		}, 1000, 1000);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	/**
	 * <p>run.</p>
	 */
	public void run() {
		
		System.out.print(f.format(new Date()));
		for (int i = 0; i < channels.length; i++) {
			System.out.print(delim);
			if (channels[i].isReady()) {
				try {
					Poop<?, DBR> poop= channels[i].getLastPoop();
					//Poop<?, DBR> poop = channels[i].getPoop();
					if (poop!=null) {
						if (poop.isStatusOK()) {
							System.out.print(poop.getString());
						} else {
							System.out.print(poop.getStatus());
						}
					} else {
						System.out.print("NaN");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.print(channels[i].getStatus());
			}
		}
		System.out.println();
	}
	
	/**
	 * <p>printHelp.</p>
	 */
	public static void printHelp() {
		System.out.println(HELP);
	}
	
}
