/**
 * 
 */
package org.scictrl.csshell.tools;

import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * <p>CSShellProbe class.</p>
 *
 * @author igor@scictrl.com
 */
public class CSShellProbe {

	private JFrame frame;

	/**
	 * <p>Constructor for CSShellProbe.</p>
	 */
	public CSShellProbe() {
	}

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		try {
			CSShellProbe probe= new CSShellProbe();
			probe.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void show() {
		getFrame().setVisible(true);
	}
	
	private JFrame getFrame() {
		if (frame == null) {
			frame = new JFrame();
			frame.setTitle("CS Shell Probe");
			
			JPanel jp= new JPanel();
			jp.setLayout(new GridBagLayout());
			
			//GridBagConstraints g= new GridBagConstraints();
			
			
		}

		return frame;
	}

}
