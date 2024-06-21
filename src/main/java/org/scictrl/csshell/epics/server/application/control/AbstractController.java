/**
 * 
 */
package org.scictrl.csshell.epics.server.application.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.application.control.Optimizer.State;

/**
 * <p>Abstract AbstractController class.</p>
 *
 * @author igor@scictrl.com
 */
public abstract class AbstractController {
	
	/**
	 * <p>sortByInp.</p>
	 *
	 * @param points a {@link java.util.List} object
	 * @return a {@link java.util.List} object
	 */
	public static final List<ProbePoint> sortByInp(List<ProbePoint> points) {
		Collections.sort(points, new Comparator<ProbePoint>() {
			@Override
			public int compare(ProbePoint o1, ProbePoint o2) {
				return (int)Math.signum(o1.inp-o2.inp);
			}
		});
		return points;
	}

	/**
	 * Algorithm seed values.
	 */
	protected List<ProbePoint> seeds;
	/**
	 * Probe points.
	 */
	protected List<ProbePoint> points;
	
	/**
	 * Input minimum allowed value.
	 */
	protected double inputMin;
	/**
	 * Input maximum allowed value.
	 */
	protected double inputMax;
	/**
	 * Input value precision.
	 */
	protected double inputPrecision;

	private boolean aborted=false;

	private ThreePointOptimizer optimizer;
	private ProbePoint best;
	private double outputPrecision;
	private boolean cacheMeasurements=false;
	private State state;
	private int maxSteps=100;
	private int measurementCount=0;

	private Logger log= LogManager.getLogger(this.getClass());
	
	private Map<Double, ProbePoint> measurements= new HashMap<Double, ProbePoint>(128);
	private int steps;
	
	/**
	 * <p>Constructor for AbstractController.</p>
	 */
	public AbstractController() {
		seeds= new ArrayList<ProbePoint>(16);
		points= new ArrayList<ProbePoint>(16);
	}
	
	/**
	 * Returns measurements count.
	 *
	 * @return measurements count
	 */
	public int getMeasurementCount() {
		return measurementCount;
	}
	
	/**
	 * Returns number of executed steps.
	 *
	 * @return number of executed steps
	 */
	public int getSteps() {
		return steps;
	}
	
	/**
	 * Returns max iteration steps, if reached, algorithm fails.
	 *
	 * @return max iteration steps
	 */
	public int getMaxSteps() {
		return maxSteps;
	}
	
	/**
	 * Set max iteration steps, if reached, algorithm fails.
	 *
	 * @param maxSteps max iteration steps
	 */
	public void setMaxSteps(int maxSteps) {
		this.maxSteps = maxSteps;
	}
	
	/**
	 * <p>Setter for the field <code>cacheMeasurements</code>.</p>
	 *
	 * @param cacheMeasurements a boolean
	 */
	public void setCacheMeasurements(boolean cacheMeasurements) {
		this.cacheMeasurements = cacheMeasurements;
	}
	
	/**
	 * <p>isCacheMeasurements.</p>
	 *
	 * @return a boolean
	 */
	public boolean isCacheMeasurements() {
		return cacheMeasurements;
	}
	
	/**
	 * <p>initialize.</p>
	 *
	 * @param min a double
	 * @param max a double
	 * @param inputPrecision a double
	 * @param outputPrecision a double
	 */
	public void initialize(double min, double max, double inputPrecision, double outputPrecision) {
		this.inputMin=min;
		this.inputMax=max;
		this.inputPrecision=inputPrecision;
		this.outputPrecision= outputPrecision;
		
		log.debug("Initialized min:{} max:{} inpPrec:{} outPrec:{}",min,max,inputPrecision,outputPrecision);
	}
	
	/**
	 * <p>Setter for the field <code>seeds</code>.</p>
	 *
	 * @param seeds a {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} object
	 */
	public void setSeeds(ProbePoint... seeds) {
		
		for (int i = 0; i < seeds.length; i++) {
			if (seeds[i]==null) {
				throw new NullPointerException("Seed '+i+' is null");
			}
			this.seeds.add(seeds[i]);
		}
		
		log.debug("Seeds: "+StringUtils.join(seeds, ','));
	}
	
	/**
	 * <p>getSate.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.control.Optimizer.State} object
	 */
	public State getSate() {
		if (state!=null) {
			return state;
		}
		if (optimizer!=null) {
			return optimizer.getState();
		}
		return State.INITIAL;
	}
	
	/**
	 * <p>abort.</p>
	 */
	public void abort() {
		aborted=true;
	}
	
	/**
	 * <p>isAborted.</p>
	 *
	 * @return a boolean
	 */
	public boolean isAborted() {
		return aborted;
	}

	/**
	 * <p>start.</p>
	 */
	public void start() {
		aborted=false;
		optimizer= new ThreePointOptimizer();
		optimizer.initialize(inputMin, inputMax, inputPrecision, outputPrecision);
		
		points.addAll(seeds);
		
		steps=1;
		measurementCount=0;
		do {
			
			log.debug("Interation start no {}",steps);
			
			notifyStepStart();
			
			step();
			
			if (isAborted()) {
				return;
			}
			
			if (steps>maxSteps) {
				log.error("Maximum of steps "+maxSteps+" reached, exiting.");
				state=State.ERROR;
			}
			
			notifyStepEnd();

			steps++;
		} while (getSate()!=State.DONE && getSate()!=State.ERROR);
	}

	/**
	 * Implementation class might override this method to be notified on step end.
	 */
	protected void notifyStepEnd() {
		
	}

	/**
	 * Implementation class might override this method to be notified on step start.
	 */
	protected void notifyStepStart() {
		
	}

	private void step() {

		if (isAborted()) {
			return;
		}

		ProbePoint[] p= points.toArray(new ProbePoint[points.size()]);

		intTakeMeasurements(p);
		
		log.info("Measurements: "+Arrays.toString(p));
		
		normalize(p);
		
		if (isAborted()) {
			return;
		}
		
		@SuppressWarnings("unused")
		State st= optimizer.nextStep(p);
		
		setPoints(optimizer.getInputs());
		setBest(optimizer.getBest());

	}
	
	private boolean intTakeMeasurements(ProbePoint[] p) {
		
		List<ProbePoint> pp= new ArrayList<ProbePoint>(p.length);
		
		if (cacheMeasurements) {
			for (ProbePoint point : p) {
				ProbePoint out= measurements.get(point.inp);
				if (out!=null) {
					point.out=out.out;
					point.valid=true;
					point.data=out.data;
				} else {
					pp.add(point);
				}
			}
		} else {
			for (ProbePoint point : p) {
				pp.add(point);
			}
		}
		
		this.measurementCount+=pp.size();
		
		Boolean b= takeMeasurements(pp.toArray(new ProbePoint[pp.size()]));
		
		for (ProbePoint point : pp) {
			if (point.valid) {
				try {
					measurements.put(point.inp, (ProbePoint)point.clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
		
		return b;
		
	}

	/**
	 * <p>Setter for the field <code>points</code>.</p>
	 *
	 * @param inputs an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 */
	public void setPoints(ProbePoint[] inputs) {
		points.clear();
		for (ProbePoint p : inputs) {
			if (p==null) {
				throw new NullPointerException("Point is null");
			}
			points.add(p);
		}
		sortByInp(points);
	}

	/**
	 * <p>Setter for the field <code>best</code>.</p>
	 *
	 * @param best a {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} object
	 */
	public void setBest(ProbePoint best) {
		this.best = best;
	}
	
	/**
	 * <p>Getter for the field <code>best</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} object
	 */
	public ProbePoint getBest() {
		return best;
	}
	
	/**
	 * <p>takeMeasurements.</p>
	 *
	 * @param points an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 * @return a boolean
	 */
	protected abstract boolean takeMeasurements(ProbePoint[] points);

	/**
	 * <p>normalize.</p>
	 *
	 * @param points an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 */
	protected void normalize(ProbePoint[] points) {
		for (ProbePoint p : points) {
			p.outNorm= p.out*p.out;
		}
	}
	
	/**
	 * <p>Getter for the field <code>measurements</code>.</p>
	 *
	 * @return a {@link java.util.Map} object
	 */
	public Map<Double, ProbePoint> getMeasurements() {
		return measurements;
	}

}
