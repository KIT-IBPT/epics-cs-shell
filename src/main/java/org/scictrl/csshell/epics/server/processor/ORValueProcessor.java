/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ORValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class ORValueProcessor extends LinkedValueProcessor {

	/**
	 * <p>Constructor for ORValueProcessor.</p>
	 */
	public ORValueProcessor() {
		type=DBRType.BYTE;
	}

	/** {@inheritDoc} */
	@Override
	protected Object processInput(ValueHolder[] inputValues) {
		
		Severity severity = Severity.NO_ALARM;
		Status status = Status.NO_ALARM;
		
		boolean b=false,failed=false;
		
		for (int i = 0; i < inputValues.length; i++) {
			if (severity.isLessThan(inputValues[i].severity)) {
				severity = inputValues[i].severity;
				status = inputValues[i].status;
			}
			long val= inputValues[i].longValue();
			b= b || val>0;
		}
		
		record.updateAlarm(severity, status, true);

		
		return failed ? false : b;
		
	}
	
	/** {@inheritDoc} */
	@Override
	public void setValue(Object value) {
		if (input!=null && !input.isInvalid()) {
			_setValue(value,null, null,  true, false);
			try {
				input.setValueToAll(value);
			} catch (Exception e) {
				throw new IllegalStateException("Remote set failed", e);
			}
		}
	}

}
