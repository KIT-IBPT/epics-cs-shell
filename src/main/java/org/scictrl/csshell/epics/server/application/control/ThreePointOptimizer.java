package org.scictrl.csshell.epics.server.application.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ThreePointOptimizer implements Optimizer {

	/**
	 * Determinant of 3x3 matrix, where a, b, c are columns.
	 *
	 * @param a a first column in 3x3 matrix
	 * @param b a second column in 3x3 matrix
	 * @param c a third column in 3x3 matrix
	 * @return a double
	 */
	public static final double determinant(double[] a, double[] b, double[] c) {
		return a[0]*b[1]*c[2]+a[2]*b[0]*c[1]+a[1]*b[2]*c[0]-a[2]*b[1]*c[0]-a[1]*b[0]*c[2]-a[0]*b[2]*c[1];
	}
	
	
	
	private State state; 
	private double min;
	private double max;
	private double inpPrec;
	private double outPrec;
	private ProbePoint[] points;
	private ProbePoint best;
	
	private Logger log= LogManager.getLogger(this.getClass());

	/** {@inheritDoc} */
	@Override
	public void initialize(double inputMin, double inputMax, double inputPrecision, double outputPrecision) {
		min=inputMin;
		max=inputMax;
		inpPrec=inputPrecision;
		outPrec=outputPrecision;
		state=State.INITIAL;
	}
	
	/**
	 * <p>Getter for the field <code>min</code>.</p>
	 *
	 * @return a double
	 */
	public double getMin() {
		return min;
	}
	
	/**
	 * <p>Getter for the field <code>max</code>.</p>
	 *
	 * @return a double
	 */
	public double getMax() {
		return max;
	}

	/** {@inheritDoc} */
	@Override
	public State getState() {
		return state;
	}

	/** {@inheritDoc} */
	@Override
	public ProbePoint[] getInputs() {
		return points;
	}

	private State seed(ProbePoint[] p) {
		state=State.SEEDING;
		
		if (p==null || p.length==0) {

			// there are no previous points, we need to guess
			
			double d= (max-min)/4;
			
			points= new ProbePoint[3];
			points[0]= new ProbePoint(min+d);
			points[1]= new ProbePoint(min+d*2.0);
			points[2]= new ProbePoint(min+d*3.0);

		} else {
			
			List<ProbePoint> valid= new ArrayList<ProbePoint>(p.length);
			
			for (int i = 0; i < p.length; i++) {
				if (p[i].valid) {
					valid.add(p[i]);
				}
			}
			
			if (valid.size()==1) {
				// there is only one valid point, we zoom on that point.
				
				double d= (max-min)/(p.length+4);
				
				if(p.length>1) {
					d= (p[p.length-1].inp-p[0].inp)/p.length*0.5;
				}
				
				ProbePoint val= valid.get(0);
				
				points= new ProbePoint[3];
				points[0]= new ProbePoint(val.inp-d);
				points[1]= new ProbePoint(val.inp);
				points[2]= new ProbePoint(val.inp+d);
				
			} else if (valid.size()==2) {
				// two points valid, we seek one in between
				
				points= new ProbePoint[3];
				points[0]= new ProbePoint(valid.get(0).inp);
				points[1]= new ProbePoint((valid.get(0).inp+valid.get(1).inp)/2.0);
				points[2]= new ProbePoint(valid.get(1).inp);
				
			} else if (valid.size()==3) {
				// three points valid, we seek in between
				
				double d= Math.abs(valid.get(0).inp-valid.get(2).inp)/3.0;
				points= new ProbePoint[3];
				if (valid.get(0).inp<valid.get(2).inp) {
					points[0]= new ProbePoint(valid.get(0).inp-d);
					points[1]= new ProbePoint(valid.get(0).inp);
					points[2]= new ProbePoint(valid.get(1).inp);
				} else {
					points[0]= new ProbePoint(valid.get(1).inp);
					points[1]= new ProbePoint(valid.get(2).inp);
					points[2]= new ProbePoint(valid.get(2).inp+d);
				}
				
			} else {
			
				int n= p.length*2;
				
				if (n>50) {
					state=State.ERROR;
					log.error("Seeding faile at "+p.length+" seeds.");
					return state;
				}
				
				points= new ProbePoint[n];
				double d= (max-min)/(n+1);
				
				for (int i = 0; i < points.length; i++) {
					points[i]= new ProbePoint(min+d+i*d ,0.0 ,false);
				}
			}			
		}
		
		StringBuilder sb= new StringBuilder();
		sb.append("New seeds: ");
		for (ProbePoint pp : points) {
			sb.append(pp.inp);
			sb.append(" ");
		}
		log.debug(sb.toString());
		
		fixBounds();
		
		return state;
	}
	
	/** {@inheritDoc} */
	@Override
	public State nextStep(ProbePoint[] p) {
		
		if (state==State.ERROR || state==State.DONE) {
			return state;
		}

		if (p==null) {
			return seed(null);
		}
		
		List<ProbePoint> pp= new ArrayList<ProbePoint>(p.length);
		for (int i = 0; i < p.length; i++) {
			if (p[i].valid) {
				pp.add(p[i]);
			}
		}
		
		if (pp.size()<3) {
			log.debug("Valid "+pp.size()+" points out of "+p.length+", reseeding.");
			return seed(p);
		}
		
		AbstractController.sortByInp(pp);
		
		if (pp.get(1).outNorm>pp.get(0).outNorm && pp.get(1).outNorm>pp.get(2).outNorm) {
			log.debug("Local peak at "+pp.get(1).inp+", reseeding.");
			if (pp.get(2).outNorm>pp.get(0).outNorm) {
				pp.remove(2);
			} else {
				pp.remove(0);
			}
			return seed(p);
		}
		
		state=State.STEPPING;

		//points= nextStepSimple(pp);
		points= nextStepQuadratic(pp);
		
		log.info("Next points "+Arrays.toString(points));

		fixBounds();
		
		log.debug("Best {} deltas 1-0 {} 1-2 {} prec {}",best.inp,(best.inp-points[0].inp),(best.inp-points[2].inp),inpPrec);
		if (
				(Math.abs(best.inp-points[0].inp)<inpPrec && Math.abs(best.inp-points[2].inp)<inpPrec) ||
				Math.abs(best.out)<outPrec) {
			state=State.DONE;
		}
		
		
		return state;
	}
	
	private void fixBounds() {
		if (points[0].inp<min) {
			points[0].inp=min;
		}
		if (points[0].inp>max) {
			points[0].inp=max;
		}
		if (points[2].inp>max) {
			points[2].inp=max;
		}
		if (points[2].inp<min) {
			points[2].inp=min;
		}
	}

	/**
	 * <p>nextStepSimple.</p>
	 *
	 * @param pp a {@link java.util.List} object
	 * @return an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 */
	public ProbePoint[] nextStepSimple(List<ProbePoint> pp) {
		
		double d= pp.get(2).inp-pp.get(0).inp;
		best=null;

		if (
				(pp.get(1).out<pp.get(0).out && pp.get(1).out<pp.get(2).out) || 
				(pp.get(0).out==pp.get(1).out && pp.get(0).out==pp.get(2).out)) {
			
			// best point is in the middle, we narrow down on it
			best= pp.get(1);
			d=d*0.5;
			log.debug("Best "+best.inp+","+best.out+" step in");
			
		} else if (pp.get(0).out<=pp.get(1).out && pp.get(0).out<=pp.get(2).out) {
			
			// best point is left to the left point
			best= pp.get(0);
			log.debug("Best "+best.inp+","+best.out+" step left");
			
		} else if (pp.get(2).out<=pp.get(1).out && pp.get(2).out<=pp.get(0).out) {

			// best point is right to the right point
			best= pp.get(2);
			log.debug("Best "+best.inp+","+best.out+" step right");
			
		} else {
			
			best= pp.get(1);
			d=d*0.75;
			log.debug("Best "+best.inp+","+best.out+" step in by default");
			
		}
		
		points= new ProbePoint[3];
		points[1]= new ProbePoint(best.inp,0.0,false);
		points[0]= new ProbePoint(best.inp-d/2.0,0.0,false);
		points[2]= new ProbePoint(best.inp+d/2.0,0.0,false);

		return points;
	}

	/**
	 * Uses Cramer rule to predict minimum of quadratic function and then focuses on that point.
	 *
	 * @param pp a {@link java.util.List} object
	 * @return an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 */
	public ProbePoint[] nextStepQuadratic(List<ProbePoint> pp) {

		// liner equation: a*x+b*y+c*z=d
		// where:
		// a - inp^2
		// b - inp
		// c - 1.0
		// d - out
		// x - coeficient for squer
		// y - linear coeficient
		// z - constant coeficient
		// quadratic equation: x * b^2 + x * b + z * 1 = d
		// or : x * inp^2 + x * inp + z  = out
		// minimum is at: inp = - y / (2*x)
		
		double[] b= new double[]{pp.get(0).inp,pp.get(1).inp,pp.get(2).inp};
		double[] a= new double[]{b[0]*b[0],b[1]*b[1],b[2]*b[2]};
		double[] c= new double[]{1.0,1.0,1.0};
		double[] d= new double[]{pp.get(0).outNorm,pp.get(1).outNorm,pp.get(2).outNorm};
		
		double det= determinant(a, b, c);
		double detx= determinant(d, b, c);
		double dety= determinant(a, d, c);
		//double detz= determinant(a, b, d);
		
		double x= detx/det; 
		double y= dety/det; 
		//double z= detz/det;
		
		double minb= - y / x / 2.0;
		
		List<ProbePoint> mins= new ArrayList<ProbePoint>(4);
		mins.addAll(pp);
		mins.add(new ProbePoint(minb));

		Collections.sort(mins, new Comparator<ProbePoint>() {
			@Override
			public int compare(ProbePoint o1, ProbePoint o2) {
				double p1= Math.abs(o1.inp-minb);
				double p2= Math.abs(o2.inp-minb);
				return (int)Math.signum(p1-p2);
			}
		});
		
		mins.remove(3);
		
		Collections.sort(pp, new Comparator<ProbePoint>() {
			@Override
			public int compare(ProbePoint o1, ProbePoint o2) {
				double p1= Math.abs(o1.inp-minb);
				double p2= Math.abs(o2.inp-minb);
				return (int)Math.signum(p1-p2);
			}
		});

		best= pp.get(0);
		
		if (best.data==null) {
			log.warn("Best point has no data: "+best);
		}

		AbstractController.sortByInp(mins);

		return mins.toArray(new ProbePoint[3]);
	}

	/** {@inheritDoc} */
	@Override
	public ProbePoint getBest() {
		return best;
	}
	
}
