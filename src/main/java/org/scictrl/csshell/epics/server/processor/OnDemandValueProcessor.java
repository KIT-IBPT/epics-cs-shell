/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.EPICSUtilities;
import org.scictrl.csshell.epics.server.Record;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.TimeStamp;

/**
 * <p>OnDemandValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class OnDemandValueProcessor extends AbstractValueProcessor {
	
	/**
	 * An interface which is asked each time new value is needed. Single 
	 * provider can be used for number of processors if unique key value is 
	 * set to each processor.
	 *  
	 * @author igor@scictrl.com
	 *
	 */
	public static interface ValueProvider {
		/**
		 * Returns new value associated with the provided key.
		 * @param key a key object to recognize the value if there is more then one processor asking
		 * @return the new value
		 */
		public Object getValue(Object key);
	}
	
	/**
	 * <p>newProcessor.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @param type a {@link gov.aps.jca.dbr.DBRType} object
	 * @param count a int
	 * @param description a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.processor.OnDemandValueProcessor} object
	 */
	public static final OnDemandValueProcessor newProcessor(String name, DBRType type, int count, String description) {
		
		Record r= new Record(name, type, count);
		
		OnDemandValueProcessor p= new OnDemandValueProcessor();
		p.configure(r,new HierarchicalConfiguration());
		r.setProcessor(p);
		
		return p;
	}

	
	private Object value;
	private ValueProvider provider;
	private Object key;
	private TimeStamp timestamp;
	
	
	/**
	 * Sets new provider object. Provider is asked for new value when necessary.
	 *
	 * @param provider the on-demand value provided
	 */
	public void setProvider(ValueProvider provider) {
		this.provider = provider;
	}
	
	/**
	 * The key which is associated with this processor and help provider distinguish between different processors.
	 *
	 * @param key the unique key
	 */
	public void setKey(Object key) {
		this.key = key;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Does absolutly nothing, vbalue is oabtained trough provider.
	 * @see org.scictrl.csshell.epics.server.ValueProcessor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		// 
	}

	/**
	 * {@inheritDoc}
	 *
	 * Each time EPICS server is asked for value this method is called.
	 * If this processor has value in cache, the value from cache is returned.
	 * If cached value is null (or it was never set or processor was reset), then
	 * the provider is asked for new value.
	 * @see org.scictrl.csshell.epics.server.ValueProcessor#getValue()
	 */
	@Override
	public Object getValue() {
		if (value==null) {
			Object o= provider.getValue(key);
			value= EPICSUtilities.convertToDBRValue(o, type);
			timestamp= new TimeStamp();
		}
		return value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Timestamp of last time provider was asked for value.
	 * @see org.scictrl.csshell.epics.server.ValueProcessor#getTimestamp()
	 */
	@Override
	public TimeStamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Resets cached value to null. So when next time getValue request will come, a new value
	 * will be requested from provider.
	 */
	public void reset() {
		value=null;
		timestamp= new TimeStamp();
		record.fireValueChange();
	}
	

}
