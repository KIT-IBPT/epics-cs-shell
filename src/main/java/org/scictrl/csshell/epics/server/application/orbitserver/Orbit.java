package org.scictrl.csshell.epics.server.application.orbitserver;

import java.util.Arrays;

import org.scictrl.csshell.Timestamp;
import org.scictrl.csshell.Tools;

/**
 * <p>Orbit class stores particle orbit.</p>
 *
 * @author igor@scictrl.com
 */
public final class Orbit {
	
	/**
	 * Enum defining orientation constants.
	 */
	public enum O { 
		/**
		 * Horizontal orientation enum.
		 */
		HORIZONTAL,
		/**
		 * Vertical orientation enum.
		 */
		VERTICAL;
		
		/**
		 * Horizontal orientation int.
		 */
		public static final int _HORIZONTAL = 0;
		/**
		 * Vertical orientation int.
		 */
		public static final int _VERTICAL = 1;
		/**
		 * Horizontal orientation int.
		 */
		public static final int _H= _HORIZONTAL;
		/**
		 * Vertical orientation int.
		 */
		public static final int _V= _VERTICAL;
		/**
		 * Horizontal orientation enum.
		 */
		public static final O H= O.HORIZONTAL;
		/**
		 * Vertical orientation enum.
		 */
		public static final O V= O.VERTICAL;
	}
	
	/**
	 * Enum defining statistics field constants.
	 */
	public enum Stat {
		/**
		 * Average value field.
		 */
		AVG,
		/**
		 * RMS, root-mean-square value field.
		 */
		RMS,
		/**
		 * STD, standard deviation value field.
		 */
		STD,
		/**
		 * Maximum value field.
		 */
		MAX;
		
		/**
		 * Average value field, as array index.
		 */
		public static final int _AVG=0;
		/**
		 * RMS, root-mean-square value field, as array index.
		 */
		public static final int _RMS=1;
		/**
		 * Maximum value field, as array index.
		 */
		public static final int _STD=2;
		/**
		 * STD, standard deviation value field, as array index.
		 */
		public static final int _MAX=3;
	}
	
	final double[][] positions= new double[2][];
	final double[][] stat= new double[2][4];
	
	@SuppressWarnings("unused")
	final private Orbit ref;
	
	final private boolean relative;
	final private int size;
	private Timestamp time;
	
	/**
	 * <p>Constructor for Orbit.</p>
	 *
	 * @param x an array of {@link double} objects
	 * @param y an array of {@link double} objects
	 * @param ref a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 */
	public Orbit(double[] x, double[] y, Orbit ref) {
		this.size=x.length;

		if (this.size!=y.length || this.size!=ref.size) {
			throw new IllegalArgumentException("Array sizes does not match!"); 
		}

		this.time= new Timestamp();
		this.ref=ref;
		this.relative=ref!=null;
		
		positions[O._H]= new double[size];
		positions[O._V]= new double[size];
		
		for (int i=0; i<size; i++) {
			positions[O._H][i]= (relative) ? x[i]-ref.positions[O._H][i] : x[i];
			positions[O._V][i]= (relative) ? y[i]-ref.positions[O._V][i] : y[i];
		}
		init();
	}
	/**
	 * <p>Constructor for Orbit.</p>
	 *
	 * @param master a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param ref a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 */
	public Orbit(Orbit master, Orbit ref) {
		this.size=master.size;

		if (this.size!=ref.size) {
			throw new IllegalArgumentException("Array sizes does not match!"); 
		}

		this.time= master.time;
		this.ref=ref;
		this.relative=ref!=null;


		positions[O._H]= new double[size];
		positions[O._V]= new double[size];
		
		if (relative) {
			for (int i = 0; i < size; i++) {
				positions[O._H][i]= master.positions[O._H][i]-ref.positions[O._H][i];
				positions[O._V][i]= master.positions[O._V][i]-ref.positions[O._V][i];
			}
		} else {
			System.arraycopy(master.positions[O._H], 0, positions[O._H], 0, positions[O._H].length);
			System.arraycopy(master.positions[O._V], 0, positions[O._V], 0, positions[O._V].length);
		}
		
		init();
	}
	
	/**
	 * <p>Constructor for Orbit.</p>
	 *
	 * @param x an array of {@link double} objects
	 * @param y an array of {@link double} objects
	 */
	public Orbit(double[] x, double[] y) {
		
		this.size=x.length;
		
		if (this.size!=y.length) {
			throw new IllegalArgumentException("Array sizes does not match!"); 
		}

		this.time= new Timestamp();
		this.ref=null;
		this.relative=false;
		positions[O._H]= x;
		positions[O._V]= y;
		
		init();
	}
	private void init() {
		
		double x,y;
		int i=0;

		if (size<2) {
			Arrays.fill(stat[O._H], Double.NaN);
			Arrays.fill(stat[O._V], Double.NaN);
			return; 
		} else {
			Arrays.fill(stat[O._H], 0.0);
			Arrays.fill(stat[O._V], 0.0);
		}
		
		for (;i<size;i++) {
			x = positions[O._H][i];
			y = positions[O._V][i];

			if (Math.abs(x)>Math.abs(stat[O._H][Stat._MAX])) stat[O._H][Stat._MAX]= x;
			if (Math.abs(y)>Math.abs(stat[O._V][Stat._MAX])) stat[O._V][Stat._MAX]= y;
			
			stat[O._H][Stat._AVG]+=x;
			stat[O._V][Stat._AVG]+=y;
			
			stat[O._H][Stat._RMS]+=(x*x);
			stat[O._V][Stat._RMS]+=(y*y);
		}
		
		if (size>1) {
			stat[O._H][Stat._AVG]/=size;
			stat[O._H][Stat._RMS]/=size;
			stat[O._H][Stat._STD]=Math.sqrt(Math.abs(stat[O._H][Stat._RMS]-(stat[O._H][Stat._AVG]*stat[O._H][Stat._AVG])));
			stat[O._H][Stat._RMS]=Math.sqrt(stat[O._H][Stat._RMS]);
			
			stat[O._V][Stat._AVG]/=size;
			stat[O._V][Stat._RMS]/=size;
			stat[O._V][Stat._STD]=Math.sqrt(Math.abs(stat[O._V][Stat._RMS]-(stat[O._V][Stat._AVG]*stat[O._V][Stat._AVG])));
			stat[O._V][Stat._RMS]=Math.sqrt(stat[O._V][Stat._RMS]);
		}
	}
	
	/**
	 * <p>Getter for the field <code>positions</code>.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return an array of {@link double} objects
	 */
	public double[] getPositions(O ori) {
		return positions[ori.ordinal()];
	}
	/**
	 * <p>getPosH.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] getPosH() {
		return positions[O._H];
	}
	/**
	 * <p>getPosV.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] getPosV() {
		return positions[O._V];
	}
	/**
	 * <p>getStatistics.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return an array of {@link double} objects
	 */
	public double[] getStatistics(O ori) {
		return stat[ori.ordinal()];
	}
	/**
	 * <p>getStatH.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] getStatH() {
		return stat[O._H];
	}
	/**
	 * <p>getStatV.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] getStatV() {
		return stat[O._V];
	}
	/**
	 * <p>getAvg.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return a double
	 */
	public double getAvg(O ori) {
		return stat[ori.ordinal()][Stat._AVG];
	}
	/**
	 * <p>getAvgH.</p>
	 *
	 * @return a double
	 */
	public double getAvgH() {
		return stat[O._H][Stat._AVG];
	}
	/**
	 * <p>getAvgV.</p>
	 *
	 * @return a double
	 */
	public double getAvgV() {
		return stat[O._V][Stat._AVG];
	}
	/**
	 * <p>getRms.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return a double
	 */
	public double getRms(O ori) {
		return stat[ori.ordinal()][Stat._RMS];
	}
	/**
	 * <p>getRmsH.</p>
	 *
	 * @return a double
	 */
	public double getRmsH() {
		return stat[O._H][Stat._RMS];
	}
	/**
	 * <p>getRmsV.</p>
	 *
	 * @return a double
	 */
	public double getRmsV() {
		return stat[O._V][Stat._RMS];
	}
	/**
	 * <p>getStd.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return a double
	 */
	public double getStd(O ori) {
		return stat[ori.ordinal()][Stat._STD];
	}
	/**
	 * <p>getStdH.</p>
	 *
	 * @return a double
	 */
	public double getStdH() {
		return stat[O._H][Stat._STD];
	}
	/**
	 * <p>getStdV.</p>
	 *
	 * @return a double
	 */
	public double getStdV() {
		return stat[O._V][Stat._STD];
	}
	/**
	 * <p>getMax.</p>
	 *
	 * @param ori a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O} object
	 * @return a double
	 */
	public double getMax(O ori) {
		return stat[ori.ordinal()][Stat._MAX];
	}
	/**
	 * <p>getMaxH.</p>
	 *
	 * @return a double
	 */
	public double getMaxH() {
		return stat[O._H][Stat._MAX];
	}
	/**
	 * <p>getMaxV.</p>
	 *
	 * @return a double
	 */
	public double getMaxV() {
		return stat[O._V][Stat._MAX];
	}
	
	/**
	 * <p>toStringStatistics.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String toStringStatistics() {
		
		StringBuilder sb= new StringBuilder(128);
		sb.append(time);
		sb.append(" a:");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._AVG]));
		sb.append(",");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._AVG]));
		sb.append(" r:");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._RMS]));
		sb.append(",");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._RMS]));
		sb.append(" s:");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._STD]));
		sb.append(",");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._STD]));
		
		return sb.toString();
	}
	
	/**
	 * <p>toStringStatFancy.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String toStringStatFancy() {
		
		StringBuilder sb= new StringBuilder(128);
		sb.append("HOR [ AVG: ");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._AVG]));
		sb.append(" RMS: ");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._RMS]));
		sb.append(" STD: ");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._STD]));
		sb.append(" MAX: ");
		sb.append(Tools.FORMAT_F3.format(stat[0][Stat._MAX]));
		sb.append("] VER [ avg: ");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._AVG]));
		sb.append(" RMS: ");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._RMS]));
		sb.append(" STD: ");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._STD]));
		sb.append(" MAX: ");
		sb.append(Tools.FORMAT_F3.format(stat[1][Stat._MAX]));
		sb.append(" ]");

		return sb.toString();
	}

	/**
	 * <p>Getter for the field <code>time</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Timestamp} object
	 */
	public Timestamp getTime() {
		return time;
	}
	
	/**
	 * <p>isRelative.</p>
	 *
	 * @return a boolean
	 */
	public boolean isRelative() {
		return relative;
	}
	
}
