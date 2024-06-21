import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSConnector;

/**
 * <p>Test class.</p>
 *
 * @author igor@scictrl.com
 */
public class Test {

	/**
	 * <p>main.</p>
	 *
	 * @param args args
	 */
	public static void main(String[] args) {
		
		String ch= "A:SR:OperationStatus:01:Mode";
		
		
		try {
			EPICSConnector c= EPICSConnector.newInstance(null);
			
			EPICSConnection<?> conn= c.newConnection(ch, null);
			
			System.out.println(conn.getStatus());
			
			Thread.sleep(1000);
			
			System.out.println(conn.getStatus());
			
			
			EPICSConnection<?> conn1= c.newConnection(ch, null);
			
			System.out.println(conn1.getStatus());
			
			Thread.sleep(1000);
			
			System.out.println(conn1.getStatus());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private Test() {
	}

}
