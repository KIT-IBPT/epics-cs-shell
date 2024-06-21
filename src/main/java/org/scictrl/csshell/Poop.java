/**
 * 
 */
package org.scictrl.csshell;

import java.lang.reflect.Array;

import org.scictrl.csshell.Status.State;

/**
 * Poop is object, which combines deliverables from remotely connected object in one package.
 * Contains value, timestamp, metadata, connection status at the time of creation and reference to vector,
 * an object of actual communication layer, which delivered those values.
 *
 * @author igor@scictrl.com
 * 
 * @param <T> data type
 * @param <V> vector delivering value, such as DBR
 */
public final class Poop <T,V> {
	
	/**
	 * <p>createTimestampPoop.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Poop} object
	 */
	public static final Poop<Object,Object> createTimestampPoop() {
		return new Poop<Object, Object>(null, new Timestamp(), null, null, null);
	}
	
	
	private final T value;
	private final Timestamp timestamp;
	private final MetaData metaData;
	private final Status status;
	private final V vector;
	
	/**
	 * Creates new Poop object.
	 *
	 * @param value the value of reote connection
	 * @param timestamp the timestamp, if <code>null</code>, then current local time is used
	 * @param metaData if available metadata (limits, description, etc)
	 * @param status the status of remote connection at the time of value retrieval
	 * @param vector the object from communication layer, which delivered the value
	 */
	public Poop(T value, Timestamp timestamp, MetaData metaData, Status status, V vector) {
		this.value = value;
		this.timestamp = (timestamp==null || timestamp.getMilliseconds()==0) ? new Timestamp() : timestamp;
		this.metaData = metaData;
		this.status=status;
		this.vector=vector;
	}
	
	/**
	 * <p>Getter for the field <code>metaData</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.MetaData} object
	 */
	public MetaData getMetaData() {
		return metaData;
	}
	
	/**
	 * <p>Getter for the field <code>status</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Status} object
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * <p>Getter for the field <code>timestamp</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.Timestamp} object
	 */
	public Timestamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * <p>Getter for the field <code>value</code>.</p>
	 *
	 * @return a T object
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * <p>Getter for the field <code>vector</code>.</p>
	 *
	 * @return a V object
	 */
	public V getVector() {
		return vector;
	}
	
	/**
	 * <p>getString.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getString() {
		
		if (value == null) {
			return null;
		}
		
		if (value instanceof String) {
			return (String)value;
		}
		
		Class<?> c= value.getClass();
		
		if (c.isArray()) {
			
			StringBuilder sb= new StringBuilder(256);
			
			sb.append('[');
			int len= Array.getLength(value);
			
			if (len>0) {
				sb.append(Array.get(value, 0));
			}
			for (int i = 1; i < len; i++) {
				sb.append(',');
				sb.append(Array.get(value, i));
			}
			sb.append(']');
			
			return sb.toString();
		}
		
		
		return String.valueOf(value);
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(128);
		sb.append("Poop:{");
		if (metaData!=null) {
			sb.append(metaData.getName());
		} else {
			sb.append("?");
		}
		sb.append(",");
		sb.append(getString());
		sb.append("}");
		return sb.toString();
	}
	
	/**
	 * <p>isStatusOK.</p>
	 *
	 * @return a boolean
	 */
	public boolean isStatusOK() {
		return status.isSet(State.NORMAL);
	}
}
