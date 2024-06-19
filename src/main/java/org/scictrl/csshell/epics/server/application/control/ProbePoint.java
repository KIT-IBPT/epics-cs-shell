package org.scictrl.csshell.epics.server.application.control;

import org.scictrl.csshell.Tools;

/**
 * <p>ProbePoint is used by optimization algorithm for probing best control points.</p>
 *
 * @author igor@scictrl.com
 */
public final class ProbePoint implements Cloneable {

	/**
	 * Input value.
	 */
	public double inp;
	/**
	 * Output result.
	 */
	public double out;
	/**
	 * Normalised output.
	 */
	public double outNorm;
	/**
	 * Is valid point.
	 */
	public boolean valid;
	/**
	 * Raw data.
	 */
	public Object data;
	
	/**
	 * <p>Constructor for ProbePoint.</p>
	 */
	public ProbePoint() {
	}
	
	/**
	 * <p>Constructor for ProbePoint.</p>
	 *
	 * @param inp a double
	 */
	public ProbePoint(double inp) {
		this(inp,Double.NaN,false);
	}

	/**
	 * <p>Constructor for ProbePoint.</p>
	 *
	 * @param inp a double
	 * @param out a double
	 * @param valid a boolean
	 */
	public ProbePoint(double inp, double out, boolean valid) {
		this.inp=inp;
		this.out=out;
		this.valid=valid;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		sb.append("{");
		sb.append(Tools.format4D(inp));
		sb.append(" ");
		if (Double.isNaN(out)) {
			sb.append("/");
		} else {
			sb.append(Tools.format4D(out));
		}
		sb.append(" ");
		sb.append(valid);
		sb.append("}");
		return sb.toString();
	}
	
	/** {@inheritDoc} */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}
