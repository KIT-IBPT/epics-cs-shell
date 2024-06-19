/**
 * 
 */
package org.scictrl.csshell;


/**
 * <p>Connection Status descriptor.</p>
 *
 * @author igor@scictrl.com
 */
public class Status {
	
	/**
	 * State
	 */
	public enum State {
		
		/**
		 * There is no status information.
		 */
		UNDEFINED	(     0, "There is no status information."),
		/**
		 * Connection was never established.
		 */
		INITIAL		(1 << 0, "Connection was never established."),
		/**
		 * Connection is established.
		 */
		CONNECTED	(1 << 1, "Connection is established."),
		/**
		 * Connection failed while establishing
		 */
		FAILED		(1 << 2, "Connection failed while establishing"),
		/**
		 * Connection currently not available, but was connected erlier
		 */
		LOST		(1 << 3, "Connection currently not available, but was connected erlier."),
		/**
		 * Connection was closed and destroyed
		 */
		CLOSED		(1 << 4, "Connection was closed and destroyed."),
		/**
		 * Remote value has reached warning threshold
		 */
		WARNING		(1 << 5, "Remote value has reached warning threshold."),
		/**
		 * Remote value has reached alarm threshold
		 */
		ALARM		(1 << 6, "Remote value has reached alarm threshold."),
		/**
		 * We might not have valid remote value
		 */
		INVALID		(1 << 7, "We might not have valid remote value."),
		/**
		 * Normal situation, no alarms or erors
		 */
		NORMAL		(1 << 8, "Normal situation, no alarms or erors."),
		;
		
		private int code;
		private String description;

		private State(int code, String description) {
			this.code=code;
			this.description=description;
		}
		
		/**
		 * Returns status code.
		 * @return status code
		 */
		public int getCode() {
			return code;
		}
		
		/**
		 * Returns description
		 * @return description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Flips off bits agains status code
		 * @param i bits to flip
		 * @return result
		 */
		public int unset(int i) {
			return (i | code) - code;		
		}
		
		/**
		 * Sets bits
		 * @param i bits to set
		 * @return result
		 */
		public int set(int i) {
			return i | code;		
		}

		/**
		 * Returns <code>true</code> if bit is set
		 * @param i bit
		 * @return <code>true</code> if bit is set
		 */
		public boolean isSet(int i) {
			return (code & i) == code;
		}
	}
	
	/**
	 * <p>fromStates.</p>
	 *
	 * @param states a {@link org.scictrl.csshell.Status.State} object
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public static final Status fromStates(State... states) {
		int code=0;
		for (State state : states) {
			code= state.set(code);
		}
		return new Status(code);
	}
	
	/** Constant <code>INITIAL</code> */
	public static final Status INITIAL = fromStates(State.INITIAL);
	

	private int code;
	
	private Status(int code) {
		this.code=code;
	}
	
	/**
	 * <p>Getter for the field <code>code</code>.</p>
	 *
	 * @return a int
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * <p>set.</p>
	 *
	 * @param state a {@link org.scictrl.csshell.Status.State} object
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public Status set(State state) {
		return new Status(state.set(code));
	}
	
	/**
	 * <p>unset.</p>
	 *
	 * @param state a {@link org.scictrl.csshell.Status.State} object
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public Status unset(State state) {
		return new Status(state.unset(code));
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		
		State[] st= State.values();
		
		for (int i = 0; i < st.length; i++) {
			if (st[i].isSet(code)) {
				if (sb.length()>0) {
					sb.append(",");
				}
				sb.append(st[i].toString());
			}
		}
		
		return sb.toString();
	}

	/**
	 * <p>isSet.</p>
	 *
	 * @param state a {@link org.scictrl.csshell.Status.State} object
	 * @return a boolean
	 */
	public boolean isSet(State state) {
		return state.isSet(code);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Status) {
			return ((Status)obj).code==code;
		}
		return true;
	}
}
