package org.scictrl.csshell.epics.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.Connection;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.Status.State;
import org.scictrl.csshell.epics.EPICSConnection;
import org.scictrl.csshell.epics.EPICSUtilities;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ValueLinks class.</p>
 *
 * @author igor@scictrl.com
 */
public class ValueLinks {
	
	/** Constant <code>SUMMARY="Summary"</code> */
	public static final String SUMMARY="Summary";
	
	/**
	 * Holds value.
	 */
	public final static class ValueHolder {
		
		/**
		 * Computes summary value holder.
		 * @param vh value holders
		 * @return summary holder
		 */
		public static ValueHolder toSummaryBit(ValueHolder[] vh) {
			return toSummaryBit(vh, null);
		}

		/**
		 * Computes summary value holder.
		 * @param vh value holders
		 * @param disable disabled values
		 * @return summary holder
		 */
		public static ValueHolder toSummaryBit(ValueHolder[] vh, boolean[] disable) {
			
			Severity sev= Severity.NO_ALARM;
			Status sta= Status.NO_ALARM;
			DBRType t= null;

			for (int i = 0; i < vh.length; i++) {
				if (disable!=null && disable.length>i && !disable[i]) {
					if (sev.isLessThan(vh[i].severity)) {
						sev= vh[i].severity;
						sta= vh[i].status;
					}
					if (t==null) {
						t=vh[i].type;
					} else if (!t.equals(vh[i].type) && !DBRType.UNKNOWN.equals(t)) {
						t=DBRType.UNKNOWN;
					}
				}
			}
			
			ValueHolder v= new ValueHolder(SUMMARY, null, sev, sta, t, System.currentTimeMillis());
			return v;
		}

		/**
		 * Returns value holders
		 * @param result array with result
		 * @param vh value holders
		 * @param defaultValue default value, if not valid
		 * @return array with result
		 */
		public static boolean[] getValid(boolean[] result, ValueHolder[] vh, boolean defaultValue) {
			
			for (int i = 0; i < vh.length || i < result.length; i++) {
				ValueHolder v= vh[i];
				
				if (v!=null && v.status== Status.NO_ALARM && v.severity==Severity.NO_ALARM) {
					result[i] = v.longValue()>0;
				} else {
					result[i] = defaultValue;
				}
			}

			return result;
		}

		
		/**
		 * PV name.
		 */
		public final String name;
		/**
		 * Value.
		 */
		public final Object value;
		/**
		 * Severity.
		 */
		public final Severity severity;
		/**
		 * Status.
		 */
		public final Status status;
		/**
		 * Value type.
		 */
		public final DBRType type;
		/**
		 * <code>true</code> if update has failed
		 */
		public final boolean failed;
		/**
		 * Timestamp of last update
		 */
		public final long timestamp;
		
		/**
		 * Constructor
		 * @param name PV name
		 * @param value value
		 * @param severity severity
		 * @param status status
		 * @param type value type
		 * @param timestamp update timestamp
		 */
		public ValueHolder(String name, Object value, Severity severity,
				Status status, DBRType type, long timestamp) {
			super();
			this.name = name;
			this.value = value;
			this.severity = severity;
			this.status = status;
			this.type=type;
			this.failed=false;
			this.timestamp=timestamp;
		}
		
		/**
		 * Constructor
		 * @param name PV name
		 */
		public ValueHolder(String name) {
			super();
			this.name = name;
			this.value = null;
			this.severity = Severity.INVALID_ALARM;
			this.status = Status.LINK_ALARM;
			this.type=null;
			this.failed=true;
			this.timestamp= System.currentTimeMillis();
		}

		/**
		 * Returns <code>true</code> if connection has alarm
		 * @return <code>true</code> if connection has alarm
		 */
		public boolean isAlarm() {
			return severity!=Severity.NO_ALARM || status!=Status.NO_ALARM;
		}
		
		/**
		 * Returns value as long.
		 * @return value as long
		 */
		public long longValue() {
			
			if (value instanceof Number) {
				return ((Number)value).longValue();
			}
			
			if (Array.getLength(value)>0) {
				return Array.getLong(value, 0);
			}
			
			return 0L;
			
		}

		/**
		 * Returns value as double.
		 * @return value as double
		 */
		public double doubleValue() {
			
			if (value instanceof Number) {
				return ((Number)value).doubleValue();
			}
			
			if (Array.getLength(value)>0) {
				return Array.getDouble(value, 0);
			}
			
			return 0.0;
			
		}

		/**
		 * Returns value as double array.
		 * @return value as double array
		 */
		public double[] doubleArrayValue() {
			
			if (value instanceof double[]) {
				return (double[])value;
			}
			
			return new double[]{};
			
		}

		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder(128);
			sb.append("VH:{");
			sb.append(name);
			sb.append(',');
			sb.append(value);
			sb.append(',');
			sb.append(severity.getName());
			sb.append(',');
			sb.append(status.getName());
			sb.append('}');
			return sb.toString();
		}
	}
	
	private abstract class ValueListener implements PropertyChangeListener {
		final int index;
		
		public ValueListener(int index) {
			this.index=index;
		}

		public boolean isInvalid() {
			return true; 
		}

		/**
		 * Copy meta data from this link representation to the provided record reference.
		 * @param r the Record object for metadata to be copied to 
		 */
		abstract void copyMetaData(Record r);
		abstract MetaData getMetaData();
		abstract void setValue(Object value) throws RemoteException, Exception;
		abstract ValueHolder getValue() throws RemoteException, Exception;
		abstract void disconnect();
	}

	private class RecordListener extends ValueListener {
		final Record record;
		
		public RecordListener(int index, Record record) {
			super(index);
			this.record=record;
			record.addPropertyChangeListener(propertyType,this);
			if (propertyType==Record.PROPERTY_VALUE) {
				record.addPropertyChangeListener(Record.PROPERTY_ALARM,this);
			}
			propertyChange(null);
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (!active) {
				return;
			}
			values[index]= new ValueHolder(record.getName(), record.getValue(), record.getAlarmSeverity(), record.getAlarmStatus(), record.getType(), EPICSUtilities.toUTC(record.getTimestamp()));
			update();
		}
		
		/**
		 * Copy meta data from this link representation to the provided record reference.
		 * @param r the Record object for metadata to be copied to 
		 */
		public void copyMetaData(final Record r) {
			if (r!=null) {
				r.copyFields(record);
			}
		}
		
		public MetaData getMetaData() {
			return Record.extractMetaData(record);
		}

		@Override
		public void setValue(Object value) throws Exception {
			record.write(value);
		}
		
		@Override
		ValueHolder getValue() throws RemoteException, Exception {
			values[index]= new ValueHolder(record.getName(), record.getValue(), record.getAlarmSeverity(), record.getAlarmStatus(), record.getType(), EPICSUtilities.toUTC(record.getTimestamp()));
			return values[index];
		}
		
		@Override
		public boolean isInvalid() {
			//return record.getAlarmSeverity() != Severity.NO_ALARM || record.getAlarmStatus() != Status.NO_ALARM;
			return record==null;
		}
		
		@Override
		void disconnect() {
			record.removePropertyChangeListener(propertyType,this);
			if (propertyType==Record.PROPERTY_VALUE) {
				record.removePropertyChangeListener(Record.PROPERTY_ALARM,this);
			}
		}
		
		@Override
		public String toString() {
			return record.toString();
		}

	}
	
	private class ConnectionListener extends ValueListener {
		final EPICSConnection<Object> conn;
		
		public ConnectionListener(int index, EPICSConnection<Object> conn) {
			super(index);
			this.conn=conn;
			
			String p=null;
			if (propertyType==Record.PROPERTY_VALUE) {
				p=Connection.PROPERTY_VALUE;
			} else if (propertyType==Record.PROPERTY_ALARM) {
				p=Connection.PROPERTY_ALARM;
			}
			try {
				conn.addPropertyChangeListener(p, this);
				conn.addPropertyChangeListener(Connection.PROPERTY_STATUS, this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		void disconnect() {
			conn.removePropertyChangeListener(propertyType, this);
			conn.removePropertyChangeListener(Connection.PROPERTY_STATUS, this);
			conn.destroy();
		}
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (!active) {
				return;
			}
			log.debug("Update "+conn.getName()+" "+evt.getNewValue()+" ("+conn.getStatus()+")");
			Poop<?, DBR> p= conn.getLastPoop();
			if (p!=null) {
				values[index]= new ValueHolder(conn.getName(), p.getValue(), ((STS)p.getVector()).getSeverity(), ((STS)p.getVector()).getStatus(), conn.getChannel().getFieldType(), p.getTimestamp().getMilliseconds());
				update();
			} else {
				if (!conn.isConnected()) {
					values[index]= new ValueHolder(conn.getName());
					update();
				}
				//System.out.println(conn.getName()+" "+conn.getStatus()+" "+conn.isReady());
			}
		}
		
		public boolean isInvalid() {
			return conn==null || !conn.getStatus().isSet(State.CONNECTED); 
		}
		
		public void copyMetaData(final Record r) {
			if (r!=null && conn!=null && conn.isConnected()) {
				conn.getMetaDataAsync(new PropertyChangeListener() {
					
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getNewValue() instanceof MetaData) {
							MetaData md= (MetaData) evt.getNewValue();
							if (md.isValid()) {
								r.copyFields(md);
							}
						}
					}
				});
			}
		}
		
		public MetaData getMetaData() {
			return conn.getMetaData();
		}

		@Override
		public void setValue(Object value) throws RemoteException {
			conn.setValue(value);
		}
		
		@Override
		ValueHolder getValue() throws RemoteException, Exception {
			Poop<?, DBR> p= conn.getPoop();
			if (p!=null) {
				values[index]= new ValueHolder(conn.getName(), p.getValue(), ((STS)p.getVector()).getSeverity(), ((STS)p.getVector()).getStatus(), conn.getChannel().getFieldType(),p.getTimestamp().getMilliseconds());
			} else {
				if (!conn.isConnected()) {
					values[index]= new ValueHolder(conn.getName());
				}
			}
			return values[index];
		}
		
		@Override
		public String toString() {
			return conn.toString();
		}
	}

	private String[] linkNames;
	private ValueHolder[] values;
	private PropertyChangeListener listener;
	private long lastUpdate;
	private long minUpdateRate=10;
	private boolean updatePending;
	private List<ValueListener> listeners;
	private final String propertyType;
	private boolean ready=false;
	private Logger log= LogManager.getLogger(getClass());
	private String name;
	private Database db;
	
	private boolean consumed=false;
	private Status lastStatus;
	private Severity lastSeverity;
	private String context;
	
	private boolean active=false;

	/**
	 * Creates new link handler. Name is used as a context.
	 *
	 * @param name this name is used as identifier, so if several link handlers are used, they can be distinguished, for example as in {@link org.scictrl.csshell.epics.server.application.AbstractApplication}.
	 * @param linkName name of PV links to be established by this handler.
	 * @param listener update listener
	 * @param propertyType how to listen to links,c an be {@link org.scictrl.csshell.epics.server.Record#PROPERTY_VALUE} or {@link org.scictrl.csshell.epics.server.Record#PROPERTY_ALARM}
	 */
	public ValueLinks(String name, String linkName, PropertyChangeListener listener, String propertyType) {
		this(name, null, new String[]{linkName}, listener, propertyType);
	}

	/**
	 * Creates new link handler.
	 *
	 * @param name this name is used as identifier, so if several link handlers are used, they can be distinguished, for example as in {@link org.scictrl.csshell.epics.server.application.AbstractApplication}.
	 * @param context a context to the link, this is used in logging to distinguish who is using this link. if <code>null</code> then name is used.
	 * @param linkName name of PV links to be established by this handler.
	 * @param listener update listener
	 * @param propertyType how to listen to links,c an be {@link org.scictrl.csshell.epics.server.Record#PROPERTY_VALUE} or {@link org.scictrl.csshell.epics.server.Record#PROPERTY_ALARM}
	 */
	public ValueLinks(String name, String context, String linkName, PropertyChangeListener listener, String propertyType) {
		this(name, context, new String[]{linkName}, listener, propertyType);
	}

	/**
	 * Creates new link handler. Name is used as a context.
	 *
	 * @param name this name is used as identifier, so if several link handlers are used, they can be distinguished, for example as in {@link org.scictrl.csshell.epics.server.application.AbstractApplication}.
	 * @param linkNames name of PV links to be established by this handler.
	 * @param listener update listener
	 * @param propertyType how to listen to links,c an be {@link org.scictrl.csshell.epics.server.Record#PROPERTY_VALUE} or {@link org.scictrl.csshell.epics.server.Record#PROPERTY_ALARM}
	 */
	public ValueLinks(String name, String[] linkNames, PropertyChangeListener listener, String propertyType) {
		this(name,null,linkNames,listener,propertyType);
	}

	/**
	 * Creates new link handler.
	 *
	 * @param name this name is used as identifier, so if several link handlers are used, they can be distinguished, for example as in {@link org.scictrl.csshell.epics.server.application.AbstractApplication}.
	 * @param context a context to the link, this is used in logging to distinguish who is using this link. Can be <code>null</code>.
	 * @param linkNames name of PV links to be established by this handler.
	 * @param listener update listener
	 * @param propertyType how to listen to links,c an be {@link org.scictrl.csshell.epics.server.Record#PROPERTY_VALUE} or {@link org.scictrl.csshell.epics.server.Record#PROPERTY_ALARM}
	 */
	public ValueLinks(String name, String context, String[] linkNames, PropertyChangeListener listener, String propertyType) {
		if (name==null) {
			throw new NullPointerException("Parameter 'name' is null");
		}
		if (linkNames==null) {
			throw new NullPointerException("Parameter 'linkNames' is null");
		}
		if (linkNames.length==0) {
			throw new NullPointerException("Parameter 'linkNames' is zero length");
		}
		for (int i = 0; i < linkNames.length; i++) {
			if (linkNames[i]==null) {
				throw new NullPointerException("Parameter 'linkNames' containt null on index "+i);
			}
		}

		this.name=name;
		this.linkNames=linkNames;
		this.listener=listener;
		this.propertyType=propertyType;
		this.context=context;

		values= new ValueHolder[linkNames.length];
		
	}
	
	/**
	 * Returns the name of this links object.
	 *
	 * @return the name of this links object
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Activates links to database records or remote objects.
	 * But be called from activate method from Record or Application.
	 *
	 * @param db a {@link org.scictrl.csshell.epics.server.Database} object
	 */
	public void activate(Database db) {
		if (this.db!=null) {
			log.warn("["+(context!=null?context+"/":"")+name+"] has been already activated, skipping.");
			return;
		}
		active=true;
		this.db=db;
		db.activationDelay();
		listeners= new ArrayList<ValueListener>(linkNames.length);
		for (int i = 0; i < linkNames.length && active==true; i++) {
			String name= linkNames[i].trim();
			Record r= db.getRecord(name);
			if (r!=null) {
				listeners.add(new RecordListener(i, r));
				log.info("["+(context!=null?context+"/":"")+this.name+"] local connection to '"+name+"'");
			} else {
				
				try {
					long t= System.currentTimeMillis();
					@SuppressWarnings("unchecked")
					EPICSConnection<Object> conn= (EPICSConnection<Object>) db.getConnector().newConnection(name,null);
					t= System.currentTimeMillis()-t;

					if (conn!=null) {
						listeners.add(new ConnectionListener(i, conn));
						log.info("["+(context!=null?context+"/":"")+this.name+"] in "+t+" ms remote connection to "+name);
 					} else {
						log.error("["+(context!=null?context+"/":"")+this.name+"] in "+t+" ms remote connection to "+name+" FAILED for unknown reason!");
 					}

				} catch (RemoteException e) {
					e.printStackTrace();
					log.error("Remote connection "+this.name+" to "+name+" FAILED!",e);
				}
			}
		}
	}
	
	/**
	 * <p>deactivate.</p>
	 */
	public void deactivate() {
		if (!active || listener==null) {
			return;
		}
		active=false;
		for (ValueListener l : listeners) {
			l.disconnect();
		}
		listeners.clear();
	}

	/**
	 * Receives copy of array with received values. Values in array are in same order as link names.
	 * Resets consumed flag to true.
	 *
	 * @return copy of received values up to this point
	 */
	public synchronized ValueHolder[] consume() {
		ValueHolder[] o = new ValueHolder[values.length];
		System.arraycopy(values, 0, o, 0, o.length);
		consumed=true;
		return o;
	}
	
	/**
	 * <p>consumeAsDoubles.</p>
	 *
	 * @return an array of {@link double} objects
	 */
	public double[] consumeAsDoubles() {
		ValueHolder[] vh= consume();
		
		double[] val= new double[vh.length];
		
		for (int i = 0; i < val.length; i++) {
			if (vh[i]==null) {
				return null;
			}
			val[i]= vh[i].doubleValue();
		}
		
		return val;
	}

	/**
	 * <p>consumeAsLongs.</p>
	 *
	 * @return an array of {@link long} objects
	 */
	public long[] consumeAsLongs() {
		ValueHolder[] vh= consume();
		
		long[] val= new long[vh.length];
		
		for (int i = 0; i < val.length; i++) {
			if (vh[i]==null) {
				return null;
			}
			val[i]= vh[i].longValue();
		}
		
		return val;
	}

	/**
	 * <p>consumeAsLong.</p>
	 *
	 * @return a long
	 */
	public long consumeAsLong() {
		ValueHolder[] vh= consume();
		
		if (vh!=null && vh.length>0 && vh[0]!=null) {
			return vh[0].longValue();
		}
		
		return 0L;
	}

	/**
	 * <p>consumeAsBooleanAnd.</p>
	 *
	 * @return a boolean
	 */
	public boolean consumeAsBooleanAnd() {
		ValueHolder[] vh= consume();

		boolean val=true;
		
		for (int i = 0; i < vh.length; i++) {
			if (vh[i]==null) {
				return false;
			}
			val= val && vh[i].longValue()!=0;
		}
		
		return val;
	}

	/**
	 * <p>Getter for the field <code>lastSeverity</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.Severity} object
	 */
	public synchronized Severity getLastSeverity() {
		if (lastSeverity==null) {
			updateStatusAndStatus();
		}
		return lastSeverity;
	}
	
	/**
	 * <p>isLastSeverityInvalid.</p>
	 *
	 * @return a boolean
	 */
	public synchronized boolean isLastSeverityInvalid() {
		if (lastSeverity==null) {
			updateStatusAndStatus();
		}
		return lastSeverity.isGreaterThanOrEqual(Severity.INVALID_ALARM);
	}

	/**
	 * <p>isLastSeverityHigh.</p>
	 *
	 * @return a boolean
	 */
	public synchronized boolean isLastSeverityHigh() {
		if (lastSeverity==null) {
			updateStatusAndStatus();
		}
		return lastSeverity.isGreaterThan(Severity.NO_ALARM);
	}

	/**
	 * <p>Getter for the field <code>lastStatus</code>.</p>
	 *
	 * @return a {@link gov.aps.jca.dbr.Status} object
	 */
	public synchronized Status getLastStatus() {
		if (lastStatus==null) {
			updateStatusAndStatus();
		}
		return lastStatus;
	}
	
	
	private void updateStatusAndStatus() {
		Status status= Status.NO_ALARM;
		Severity severity= Severity.NO_ALARM;
		
		for (int i = 0; i < values.length; i++) {
			if (values[i]==null) {
				lastStatus=Status.UDF_ALARM;
				lastSeverity=Severity.INVALID_ALARM;
				return;
			}
			if (severity.isLessThan(values[i].severity)) {
				status=values[i].status;
				severity=values[i].severity;
			} 
		}
		lastStatus=status;
		lastSeverity=severity;
	}

	/**
	 * Returns <code>true</code> if after consume was called and no new values were delivered.
	 *
	 * @return <code>true</code> if after consume was called and no new values were delivered
	 */
	public synchronized boolean isConsumed() {
		return consumed;
	}

	/**
	 * <p>resetConsumedFlag.</p>
	 */
	public synchronized void resetConsumedFlag() {
		consumed=false;
	}
	
	/**
	 * Return <code>true</code> when all values from all links has been received and there is no
	 * <code>null</code> in array of values.
	 *
	 * @return <code>true</code> when there is no more <code>null</code> among values
	 */
	public boolean isReady() {
		if (!ready) {
			for (int i = 0; i < values.length; i++) {
				if (values[i]==null) return false;
				if (values[i].failed) return false;
			}
			ready=true;
		}
		return true;
	}
	
	/**
	 * <p>getNotConnected.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getNotConnected() {
		ArrayList<String> l= new ArrayList<String>(linkNames.length);
		
		for (int i = 0; i < linkNames.length; i++) {
			if (values[i]==null || values[i].failed) {
				l.add(linkNames[i]);
			}
		}
		
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Returns <code>true</code> if some of the connection failed.
	 *
	 * @return <code>true</code> if some of the connection failed
	 */
	public boolean isInvalid() {
		if (listeners==null) {
			return true;
		}
		for (int i = 0; i < listeners.size(); i++) {
			if (listeners.get(i).isInvalid()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * <p>printDebug.</p>
	 *
	 * @param sb a {@link java.lang.Appendable} object
	 * @throws java.io.IOException if any.
	 */
	public void printDebug(Appendable sb) throws IOException {
		sb.append("ValueLinks["+toString()+"]:");
		if (listeners==null) {
			sb.append("No links");
		} else {
			for (int i = 0; i < listeners.size(); i++) {
				ValueListener vl= listeners.get(i);
				if (vl instanceof ConnectionListener) {
					ConnectionListener cl= (ConnectionListener)vl;
					sb.append("Conn["+cl.toString()+"]:");
					sb.append("Invalid:");
					sb.append(Boolean.toString(cl.isInvalid()));
					sb.append(",Name:");
					sb.append(cl.conn.getName());
					sb.append(",State:");
					sb.append(cl.conn.getStatus().toString());
				} else {
					RecordListener rl= (RecordListener)vl;
					sb.append("Rec["+rl.toString()+"]:");
					sb.append("Invalid:");
					sb.append(Boolean.toString(rl.isInvalid()));
					sb.append(",Name:");
					sb.append(rl.record.getName());
					sb.append(",Status:");
					sb.append(rl.record.getAlarmStatus().toString());
					sb.append(",Severity:");
					sb.append(rl.record.getAlarmSeverity().toString());
				}
			}
		}
	}

	/**
	 * Schedules update, take care that only update is scheduled 
	 */
	private synchronized void update() {
		consumed=false;
		
		lastSeverity=null;
		lastStatus=null;
		
		if (listener!=null) {
			
			if (updatePending) {
				return;
			}
			
			long diff= System.currentTimeMillis()-lastUpdate;
			lastUpdate= System.currentTimeMillis();

			updatePending=true;
			db.schedule(new Runnable() {
				
				@Override
				public void run() {
					fireUpdate();
				}
			}, Math.max(minUpdateRate-diff,10));
			
		}
		
	}

	/**
	 * <p>fireUpdate.</p>
	 */
	protected void fireUpdate() {
		if (listener!=null) {
			updatePending=false;
			PropertyChangeEvent p= new PropertyChangeEvent(this, name, null, values);
			listener.propertyChange(p);
		}
	}

	/**
	 * Copy meta data from this link representation to the provided record reference.
	 * If there is more than one link, first link is used.
	 *
	 * @param r the Record object for metadata to be copied to
	 */
	public void copyMetaData(final Record r) {
		ValueListener vl= listeners.get(0);
		vl.copyMetaData(r);
	}

	/**
	 * Copy meta data from this link representation indicated by index to the provided record reference.
	 *
	 * @param r the Record object for metadata to be copied to
	 * @param link the index of link to provide the data for copy
	 */
	public void copyMetaData(final Record r, int link) {
		ValueListener vl= listeners.get(link);
		vl.copyMetaData(r);
	}

	/**
	 * <p>getMetaData.</p>
	 *
	 * @param link a int
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData getMetaData(int link) {
		ValueListener vl= listeners.get(link);
		return vl.getMetaData();
	}

	/**
	 * <p>setValue.</p>
	 *
	 * @param value a {@link java.lang.Object} object
	 * @throws java.lang.Exception if any.
	 */
	public void setValue(Object value) throws Exception {
		if (listener==null) {
			return;
		}
		ValueListener vl= listeners.get(0);
		vl.setValue(value);
	}
	
	/**
	 * <p>setValueToAll.</p>
	 *
	 * @param value an array of {@link double} objects
	 * @throws java.lang.Exception if any.
	 */
	public void setValueToAll(double[] value) throws Exception {
		if (listener==null) {
			return;
		}
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).setValue(value[i]);
		}
	}
	
	/**
	 * <p>setValueToAll.</p>
	 *
	 * @param value a {@link java.lang.Object} object
	 * @throws java.lang.Exception if any.
	 */
	public void setValueToAll(Object value) throws Exception {
		if (listener==null) {
			return;
		}
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).setValue(value);
		}
	}
	
	/**
	 * <p>setValueToAll.</p>
	 *
	 * @param value a {@link java.lang.Object} object
	 * @throws java.lang.Exception if any.
	 * @param select an array of {@link boolean} objects
	 */
	public void setValueToAll(Object value, boolean[] select) throws Exception {
		if (listener==null) {
			return;
		}
		for (int i = 0; i < listeners.size(); i++) {
			if (select[i]) {
				listeners.get(i).setValue(value);
			}
		}
	}

	/**
	 * <p>getValue.</p>
	 *
	 * @return an array of {@link org.scictrl.csshell.epics.server.ValueLinks.ValueHolder} objects
	 * @throws java.lang.Exception if any.
	 */
	public ValueHolder[] getValue() throws Exception {
		ValueHolder[] vh= new ValueHolder[listeners.size()];
		for (int i = 0; i < listeners.size(); i++) {
			vh[i]= listeners.get(i).getValue();
		}
		return vh;
	}
	
	/**
	 * <p>Getter for the field <code>linkNames</code>.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getLinkNames() {
		return linkNames;
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(1024);
		
		sb.append("ValueLinks[");
		sb.append(name);
		sb.append("] ");
		
		for (ValueListener vl: listeners) {
			sb.append(vl.toString());
			sb.append(" ");
		}
		
		return sb.toString();
	}
}
