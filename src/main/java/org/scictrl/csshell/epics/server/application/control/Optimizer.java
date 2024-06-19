package org.scictrl.csshell.epics.server.application.control;

/**
 * Interface implementing optimization procedure.
 * Procedure exchanges ProbePoints. Controller makes measurements and for each input in probe provides measured output.
 * Optimizer tries to guess next input point to zoom in on those inputs, that bring outputs closest to 0.
 *
 * @author igor@scictrl.com
 */
public interface Optimizer {
	
	/**
	 * Optimization cycle state.
	 */
	public enum State {
		/**
		 * Initial status, before first inputs have been calculated. 
		 */
		INITIAL,
		/**
		 * Optimization is in progress.
		 */
		SEEDING,
		/**
		 * Optimization is in progress.
		 */
		STEPPING,
		/**
		 * Optimization has reached end.
		 */
		DONE,
		/**
		 * Optimization has been interrupted by error.
		 */
		ERROR};
	
	/**
	 * Sets expected boundaries for inputs. This sets optimizer into {@link org.scictrl.csshell.epics.server.application.control.Optimizer.State#INITIAL} state.
	 *
	 * @param inputMin min allowed input
	 * @param inputMax max allowed input
	 * @param inputPrecision input values precision
	 * @param outputPrecision output values precision, defines how close to 0 algorithm iterates
	 */
	public void initialize(double inputMin, double inputMax, double inputPrecision, double outputPrecision);
	
	/**
	 * operation status.
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.control.Optimizer.State} object
	 */
	public Optimizer.State getState();
	
	/**
	 * Returns array of ProbePoint object with inputs for which optimizer wishes controller to provide outputs.
	 *
	 * @return an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 */
	public ProbePoint[] getInputs();
	
	/**
	 * Called by Controller to provide output values for requested inputs and asks for next step evaluation.
	 * Returned is status of optimizers.
	 *
	 * @param points an array of {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} objects
	 * @return a {@link org.scictrl.csshell.epics.server.application.control.Optimizer.State} object
	 */
	public Optimizer.State nextStep(ProbePoint[] points);
	
	/**
	 * Returns best point so far
	 *
	 * @return a {@link org.scictrl.csshell.epics.server.application.control.ProbePoint} object
	 */
	public ProbePoint getBest();

	/**
	 * Returns min allowed value for inputs.
	 *
	 * @return min allowed value for inputs.
	 */
	public double getMin();
	
	/**
	 * Returns max allowed value for inputs.
	 *
	 * @return max allowed value for inputs.
	 */
	public double getMax();

}
