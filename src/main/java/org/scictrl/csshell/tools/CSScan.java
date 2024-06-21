/**
 * 
 */
package org.scictrl.csshell.tools;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

/**
 * <p>CSScan class.</p>
 *
 * @author igor@scictrl.com
 */
public class CSScan {

	static boolean live=true;
	static String pv;

	/**
	 * Constructor.
	 */
	private CSScan() {
	}
	
	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		try {
			
			if (args==null || (args.length==1 && args[0].equals("-h")) || args.length!=4) {
				
				System.out.println("Command line parameters: PV step-in-sec step-value limit-value");
				System.exit(0);
				
			}
			
			pv= args[0];
			long time= Math.abs(Long.parseLong(args[1]));
			double step= Double.parseDouble(args[2]); 
			double lim= Double.parseDouble(args[3]); 
			boolean up=true;
			
			Configurator.initialize(new DefaultConfiguration());
			Configurator.setLevel(LogManager.getRootLogger(),Level.OFF);
			
			EPICSConnector ec= EPICSConnector.newInstance(null);
			@SuppressWarnings("unchecked")
			EPICSConnection<Double> c = (EPICSConnection<Double>) ec.newConnection(pv,DataType.DOUBLE);
			
			c.waitTillConnected();
			
			System.out.println("Connected to "+pv);

			double d= c.getValue();
			
			System.out.println("Start "+d+" step "+step+" limit "+lim+" timer "+time);
			System.out.println("Press <enter> to stop");
			
			if (d<lim) {
				step=  Math.abs(step);
				up= true;
			}
			if (d>lim) {
				step= -Math.abs(step);
				up= false;
			}

			live=true;
			
			new Thread () {
				@Override
				public void run() {
					try {
						System.in.read();
					} catch (IOException e) {
					}
					live=false;
					synchronized (pv) {
						pv.notify();
					}
				}
			}.start();
			
			while (live) {
				
				d= c.getValue();
				
				d= d+step;
				
				if (up) {
					if (d<lim) {
						c.setValue(d);
						System.out.println("Up "+d);
					} else {
						System.out.println("Limit reached");
						System.exit(0);
					}
				} else {
					if (d>lim) {
						c.setValue(d);
						System.out.println("Down "+d);
					} else {
						System.out.println("Limit reached");
						System.exit(0);
					}
				}
				synchronized(pv) {
					try {
						pv.wait(time*1000);
					} catch (Exception e) {}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done");
		System.exit(0);
	}

}
