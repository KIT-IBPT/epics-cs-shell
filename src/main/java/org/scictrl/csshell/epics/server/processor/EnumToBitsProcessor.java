/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.lang.reflect.Array;

import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

/**
 * <p>EnumToBitsProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class EnumToBitsProcessor extends LinkedValueProcessor {

	/**
	 * Constructor.
	 */
	public EnumToBitsProcessor() {
	}
	
	/** {@inheritDoc} */
	@Override
	protected Object processInput(ValueHolder[] inputValues) {
		
		if (inputValues==null || inputValues.length<1) {
			return 0;
		}
		
		Object o = inputValues[0].value;
		
		int i= Array.getInt(o, 0);
		
		int val = 1 << i;
		
		return val;
		
	}
	
}
