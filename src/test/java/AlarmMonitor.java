/**
 * 
 */


import java.lang.reflect.Array;

import org.scictrl.csshell.DataType;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * <p>AlarmMonitor class.</p>
 *
 * @author igor@scictrl.com
 */
public class AlarmMonitor {

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		try {
			
			EPICSConnector c= EPICSConnector.newInstance(null);
			
			//EPICSConnection<Long> con= (EPICSConnection<Long>) c.newConnection("A:AL:PSCheck:SR-B:Check", DataType.LONG);
			@SuppressWarnings("unchecked")
			EPICSConnection<Object> con= (EPICSConnection<Object>) c.newConnection("A:SR:PS:B-01:Status:Alarm", DataType.LONG);
			System.out.println("Sleep");
			Thread.sleep(1000);
			System.out.println("Monitor");
			@SuppressWarnings("unused")
			Monitor mon= con.getChannel().addMonitor(Monitor.ALARM, new MonitorListener() {
				@Override
				public void monitorChanged(MonitorEvent ev) {
					System.out.println("Source: "+ev.getSource());
					System.out.println("Status: "+ev.getStatus());
					System.out.println("DBR   : "+ev.getDBR());
					System.out.println("Count : "+ev.getDBR().getCount());
					System.out.println("Val[0]: "+Array.get(ev.getDBR().getValue(),0));
					System.out.println("Class : "+ev.getDBR().getClass());
					System.out.println("Type  : "+ev.getDBR().getType());
					
					DBR_TIME_Int dbr= (DBR_TIME_Int) ev.getDBR();
					
					System.out.println("Severi: "+dbr.getSeverity());
					System.out.println("Status: "+dbr.getStatus());
					System.out.println("Timest: "+dbr.getTimeStamp());
					
					
				}
			});
			
			
			System.out.println("Wait");
			Thread.sleep(60000);
			
		} catch (Exception e) {
			System.out.println("Error "+e);
			e.printStackTrace();
		}
		
		System.out.println("Exit");
	}
	
	private AlarmMonitor() {
	}

}
