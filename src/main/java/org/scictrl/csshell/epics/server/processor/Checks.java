/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Database;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.Status;

/**
 * <p>Checks class.</p>
 *
 * @author igor@scictrl.com
 * 
 * @param <A> use either {@link Check} or {@link LinkCheck}, any other class will cause errors
 */
public final class Checks<A> {

	/**
	 * Check condition enumeration.
	 */
	public static enum Condition {
		/** Check returns always true. */
		ALWAYS,
		/** Check never returns true. */
		NEVER,
		/** Check returns true if tested value is within precision of the test value. */
		EQUAL,
		/** Check returns true if tested value is outside precision of the test value. */
		NONEQUAL,
		/** Check returns true if tested value is numerically exactly same the test value. */
		SAME,
		/** Check returns true if tested value is numerically different than the test value. */
		DIFFERENT,
		/** Check returns true if tested value is greater than the test value. */
		GREATER,
		/** Check returns true if tested value is less than the test value. */
		LESS,
		/** Check returns true if tested value is greater than or equal to the test value. */
		GREATEREQ,
		/** Check returns true if tested value is less than or equal to the test value. */
		LESSEQ;
		
		/**
		 * Returns Condition object from configuration string.
		 * @param s configuration string
		 * @return Condition object from configuration string
		 */
		static public Condition fromString(String s) {
			String ss= s.toUpperCase();
			return valueOf(ss);
		}
	};

	/**
	 * Simple configurable check, that test value against some criteria.
	 * 
	 */
	public static final class Check {
		/**
		 * Static check based on ALWAYS.
		 */
		public static final Check ALWAYS= new Check(Condition.ALWAYS,0.0,0.0,null);
		/**
		 * Static check based on NEVER.
		 */
		public static final Check NEVER= new Check(Condition.NEVER,0.0,0.0,null);
		
		/**
		 * Extracts check parameters from XML based Configuration. The {@link Condition} keywords must be 
		 * defined as attributes to a tag. Tag must be provided as Configuration, attributes must have a defined value.
		 * <br/>
		 * Example: <code>&lt;check equal=1.0 precision=0.0001 /&gt;</code>, element name can be arbitrary
		 * @param c configuration
		 * @return new Check
		 */
		public static Check fromConfiguration(HierarchicalConfiguration c) {
			return fromConfiguration(c, null, null);
		}
		
		/**
		 * Extracts check parameters from XML based Configuration. The {@link Condition} keywords must be 
		 * defined as attributes to a tag. Tag must be provided as Configuration, attributes must have a defined value.
		 * <br/>
		 * Example: <code>&lt;check equal=1.0 precision=0.0001 /&gt;</code>, element name can be arbitrary
		 * @param c configuration
		 * @param value test value
		 * @param precision value precision
		 * @return new Check object
		 */
		public static Check fromConfiguration(HierarchicalConfiguration c, Double value, Double precision) {

			String link= c.getString("");
			
			String always=c.getString("[@always]");
			String never=c.getString("[@never]");
			String equal=c.getString("[@equal]");
			String nonequal=c.getString("[@nonequal]");
			String same=c.getString("[@same]");
			String different=c.getString("[@different]");
			String greater=c.getString("[@greater]");
			String less=c.getString("[@less]");
			String greatereq=c.getString("[@greatereq]");
			String lesseq=c.getString("[@lesseq]");
			String prec=c.getString("[@precision]","0.0000001");
			
			double p= precision!=null ? precision : Double.parseDouble(prec);
			
			if (always!=null) {
				return ALWAYS;
			}
			if (never!=null) {
				return NEVER;
			}
			if (equal!=null) {
				return new Check(Condition.EQUAL, value!=null ? value : Double.parseDouble(equal), p, link);
			} 
			if (nonequal!=null) {
				return new Check(Condition.NONEQUAL, value!=null ? value : Double.parseDouble(nonequal), p, link);
			} 
			if (same!=null) {
				return new Check(Condition.SAME, value!=null ? value : Double.parseDouble(same), 0.0, link);
			} 
			if (different!=null) {
				return new Check(Condition.DIFFERENT, value!=null ? value : Double.parseDouble(different), p, link);
			} 
			if (greater!=null) {
				return new Check(Condition.GREATER, value!=null ? value : Double.parseDouble(greater), 0.0, link);
			} 
			if (less!=null) {
				return new Check(Condition.LESS, value!=null ? value : Double.parseDouble(less), 0.0, link);
			} 
			if (greatereq!=null) {
				return new Check(Condition.GREATEREQ, value!=null ? value : Double.parseDouble(greatereq), p, link);
			} 
			if (lesseq!=null) {
				return new Check(Condition.LESSEQ, value!=null ? value : Double.parseDouble(lesseq), p, link);
			} 
		
			return NEVER;

		}
		
		private Condition condition;
		private double testValue;
		private double precision;
		private String link;
		
		/**
		 * Constructor.
		 * 
		 * @param operation condition
		 * @param testValue test value
		 * @param precision value precision
		 */
		public Check(Condition operation, double testValue, double precision) {
			this(operation, testValue, precision, null);
		}

		/**
		 * Constructor.
		 * 
		 * @param operation condition
		 * @param testValue test value
		 * @param precision value precision
		 * @param link remote link that provides value
		 */
		public Check(Condition operation, double testValue, double precision, String link) {
			this.condition=operation;
			this.testValue=testValue;
			this.precision=precision;
			this.link=link;
		}

		/**
		 * Constructor.
		 * 
		 * @param operation condition
		 * @param testValue test value
		 * @param precision value precision
		 */
		public Check(String operation, String testValue, String precision) {
			this(operation, testValue, precision, null);
		}
		
		/**
		 * Constructor.
		 * 
		 * @param operation condition
		 * @param testValue test value
		 * @param precision value precision
		 * @param link remote link that provides value
		 */
		public Check(String operation, String testValue, String precision, String link) {
			this(Condition.fromString(operation), Double.parseDouble(testValue), Double.parseDouble(precision), link);
		}

		
		/**
		 * Returns <code>true</code> if tested value is meeting the configured criteria.
		 * @param value value to be tested
		 * @return <code>true</code> if tested value is meeting the configured criteria.
		 */
		public boolean check(double value) {
			
			if (condition==Condition.ALWAYS) {
				return true;
			}
			if (condition==Condition.NEVER) {
				return false;
			}
			if (condition==Condition.EQUAL) {
				return Math.abs(value-testValue)<=precision;
			}
			if (condition==Condition.NONEQUAL) {
				return Math.abs(value-testValue)>precision;
			}
			if (condition==Condition.SAME) {
				return value==testValue;
			}
			if (condition==Condition.DIFFERENT) {
				return value!=testValue;
			}
			if (condition==Condition.GREATER) {
				return value>testValue;
			}
			if (condition==Condition.LESS) {
				return value<testValue;
			}
			if (condition==Condition.GREATEREQ) {
				return value>=testValue || Math.abs(value-testValue)<=precision;
			}
			if (condition==Condition.LESSEQ) {
				return value<=testValue || Math.abs(value-testValue)<=precision;
			}
			return false;
		}

		/**
		 * Returns test condition.
		 * @return test condition
		 */
		public Condition getCondition() {
			return condition;
		}
		
		/**
		 * Returns test value.
		 * @return test value
		 */
		public double getTestValue() {
			return testValue;
		}
		
		/**
		 * Returns value precision.
		 * @return value precision
		 */
		public double getPrecision() {
			return precision;
		}

		/**
		 * Remote link, if available.
		 * @return remote link, if available
		 */
		public String getLink() {
			return link;
		}

		/**
		 * Returns <code>true</code> if condition ALWAYS.
		 * @return <code>true</code> if condition ALWAYS
		 */
		public boolean isAlways() {
			return condition==Condition.ALWAYS;
		}

		/**
		 * Returns <code>true</code> if condition NEVER
		 * @return <code>true</code> if condition NEVER
		 */
		public boolean isNever() {
			return condition==Condition.NEVER;
		}
		
		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder(128);
			sb.append(this.getClass().getSimpleName());
			sb.append(":{");
			sb.append(condition);
			sb.append(',');
			sb.append(testValue);
			sb.append(',');
			sb.append(precision);
			sb.append(',');
			sb.append(link);
			sb.append('}');
			return sb.toString();
		}
	}
	
	/**
	 * Utilizes checks with link, obtains remote value and does condition test.
	 */
	public static final class LinkCheck {
		ValueLinks link;
		private PropertyChangeListener listener;
		Check check;

		/**
		 * Constructor.
		 * 
		 * @param listener check listener
		 * @param check check to be proessed
		 */
		public LinkCheck(PropertyChangeListener listener, Check check) {
			this.listener=listener;
			this.check=check;
		}

		/**
		 * Activates link.
		 * @param database the database
		 */
		void activate(Database database) {
			link= new ValueLinks(check.getLink(), new String[]{check.getLink()}, listener, Record.PROPERTY_VALUE);
			link.activate(database);
		}

		/**
		 * Does check with against remote value.
		 * @return if check condition passes
		 */
		public boolean check() {
			if (link==null || !link.isReady()) {
				return false;
			}
			double n= link.consumeAsDoubles()[0];
			boolean b= check.check(n);
			return b;
		}

		/**
		 * Returns link status, if grater then provided one.
		 * @param st provided status
		 * @return returned status, if greater than provided
		 */
		public Status status(Status st) {
			if (link==null || !link.isReady()) {
				return Status.UDF_ALARM;
			}
			if (link.isInvalid()) {
				return Status.READ_ALARM; 
			}
			Status s= link.getLastStatus();
			if (s.isGreaterThan(st)) {
				return s;
			}
			return st;
		}
	}

	
	
	/**
	 * <p>checks.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @param tag a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.processor.Checks} object
	 */
	public static final Checks<Check> checks(HierarchicalConfiguration config, String tag) {
		boolean operationAnd= config.getBoolean(tag+"[@and]",true);
		Checks<Check> checks= new Checks<Check>(operationAnd);
		List<?> l= ((HierarchicalConfiguration)config.interpolatedConfiguration()).configurationsAt(tag+".check");
		for (Iterator<?> it = l.iterator(); it.hasNext();) {
			HierarchicalConfiguration c = (HierarchicalConfiguration) it.next();
			Check ch= Check.fromConfiguration(c);
			checks.add(ch);
		}
		return checks;
	}

	/**
	 * <p>linkChecks.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @param tag a {@link java.lang.String} object
	 * @param listener a {@link java.beans.PropertyChangeListener} object
	 * @return a {@link org.scictrl.csshell.epics.server.processor.Checks} object
	 */
	public static final Checks<LinkCheck> linkChecks(HierarchicalConfiguration config, String tag, PropertyChangeListener listener) {
		boolean operationAnd= config.getBoolean(tag+"[@and]",true);
		Checks<LinkCheck> checks= new Checks<LinkCheck>(operationAnd);
		List<?> l= ((HierarchicalConfiguration)config.interpolatedConfiguration()).configurationsAt(tag+".check");
		for (Iterator<?> it = l.iterator(); it.hasNext();) {
			HierarchicalConfiguration c = (HierarchicalConfiguration) it.next();
			Check ch= Check.fromConfiguration(c);
			LinkCheck lch= new LinkCheck(listener,ch);
			checks.add(lch);
		}
		return checks;
	}

	
	private ArrayList<A> checks;
	private boolean operationAnd=false;
	private Status lastStatus;
	
	/**
	 * 
	 */
	private Checks() {
		this.checks= new ArrayList<A>();
	}
	/**
	 * 
	 */
	private Checks(boolean operationAnd) {
		this();
		this.operationAnd=operationAnd;
	}

	/**
	 * <p>add.</p>
	 *
	 * @param ch a A object
	 */
	public void add(A ch) {
		checks.add(ch);
	}
	
	/**
	 * <p>Getter for the field <code>lastStatus</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.Status} object
	 */
	public Status getLastStatus() {
		return lastStatus;
	}
	
	/**
	 * <p>check.</p>
	 *
	 * @param st a {@link gov.aps.jca.dbr.Status} object
	 * @return a boolean
	 */
	@SuppressWarnings("unchecked")
	public boolean check(Status st) {
		boolean b= operationAnd;
		
		for (LinkCheck check : (List<LinkCheck>)checks) {
			if (operationAnd) {
				b= b && check.check();
			} else {
				b= b || check.check();
			}
			st= check.status(st);
		}
		
		lastStatus=st;
		
		return b;

	}
	
	/**
	 * <p>check.</p>
	 *
	 * @param value a double
	 * @return a boolean
	 */
	@SuppressWarnings("unchecked")
	public boolean check(double value) {
		boolean b= operationAnd;
		
		for (Check check : (List<Check>)checks) {
			if (operationAnd) {
				b= b && check.check(value);
			} else {
				b= b || check.check(value);
			}
		}
		
		lastStatus= Status.NO_ALARM;
		
		return b;

	}
	
	
	/**
	 * <p>activate.</p>
	 *
	 * @param database a {@link org.scictrl.csshell.epics.server.Database} object
	 */
	@SuppressWarnings("unchecked")
	public void activate(Database database) {
		for (LinkCheck check : (List<LinkCheck>)checks) {
			check.activate(database);
		}
	}
	
	
}
