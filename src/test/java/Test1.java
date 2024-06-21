

import org.scictrl.csshell.DataType;
import org.scictrl.csshell.epics.EPICSConnector;

/**
 * <p>Test1 class.</p>
 *
 * @author igor@scictrl.com
 */
public class Test1 {

	/**
	 * <p>main.</p>
	 *
	 * @param args args
	 */
	public static void main(String[] args) {
		
		try {
			
			System.out.println(EPICSConnector.getInstance(null).getValue("A:TEST:Test1", DataType.DOUBLE));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private Test1() {
	}
}
