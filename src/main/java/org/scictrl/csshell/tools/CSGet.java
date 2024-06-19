/**
 * 
 */
package org.scictrl.csshell.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.scictrl.csshell.DataType;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.dbr.DBR;

/**
 * <p>CSGet class.</p>
 *
 * @author igor@scictrl.com
 */
public class CSGet {

	/**
	 * <p>Constructor for CSGet.</p>
	 */
	public CSGet() {
	}

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		try {
			
			BufferedReader r= new BufferedReader(new InputStreamReader(System.in));
			String c;
			String n;
			
			if (args.length!=1) {
				System.out.println("Provide PV name!");
				n=r.readLine();
			} else {
				n= args[0];
			}
			
			System.out.println("Connecting to '"+n+"'");

			EPICSConnector ec= EPICSConnector.newInstance(null);
			@SuppressWarnings("unchecked")
			EPICSConnection<Double> pv = (EPICSConnection<Double>) ec.newConnection(n,DataType.DOUBLE);
			
			pv.waitTillConnected();
			
			System.out.println("Connected, <enter>: get, <value>: set, q: quit");
			
			c= r.readLine();
			
			while (c.length()==0 || !c.startsWith("q")) {
				
				if (c.length()==0) {
					
					if (!pv.isReady()) {
						System.out.println("Not ready!");
					} else {
						Poop<Double, DBR> p= pv.getPoop();
						System.out.print(p.getValue());
						System.out.print(" ");
						System.out.print(p.getStatus());
						System.out.println();
					}
				} else {
					
					if (!pv.isReady()) {
						System.out.println("Not ready!");
					} else {
						double d= Double.parseDouble(c);
						
						if (d!=Double.NaN) {
							pv.setValue(d);
						}
					}
				}
				
				c= r.readLine();
				
			}
			 
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
