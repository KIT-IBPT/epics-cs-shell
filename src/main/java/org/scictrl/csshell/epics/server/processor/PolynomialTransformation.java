package org.scictrl.csshell.epics.server.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * <p>PolynomialTransformation class.</p>
 *
 * @author igor@scictrl.com
 */
public class PolynomialTransformation {

	private Double[] transX=new Double[]{0.0,1.0};

	/**
	 * <p>Constructor for PolynomialTransformation.</p>
	 */
	public PolynomialTransformation() {
	}
	
	/**
	 * <p>configure.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 */
	public void configure(HierarchicalConfiguration config) {
		
		List<Double> coef= new ArrayList<Double>(4);

		for(int i=0; i<10; i++) {
			String par= "x"+i;
			if (config.containsKey(par)) {
				coef.add(config.getDouble(par, 0.0));
			} else {
				break;
			}
		}
		
		transX= coef.toArray(new Double[coef.size()]);
	}

	/**
	 * <p>transformX.</p>
	 *
	 * @param x a double
	 * @return a double
	 */
	public double transformX(final double x) {
		double y=0.0;
		if (transX.length>0) {
			y= transX[0];
		}
		if (transX.length>1) {
			y+= transX[1] * x;
		}
		for (int i = 2; i < transX.length; i++) {
			y+= transX[i] * Math.pow(x, i);
		}
		return y;
	}
}
