/**
 * 
 */
package org.scictrl.csshell.epics.casperr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.scictrl.csshell.Poop;

/**
 * <p>PVCache class.</p>
 *
 * @author igor@scictrl.com
 */
public class PVCache {

	/** Constant <code>PROPERTY_RECORD_ADDED="recordAdded"</code> */
	public static final String PROPERTY_RECORD_ADDED = "recordAdded";
	/** Constant <code>PROPERTY_RECORD_UPDATED="recordUpdated"</code> */
	public static final String PROPERTY_RECORD_UPDATED = "recordUpdated";

	class PVRecord {
		
		final private String name;
		final private Date lastRequested;
		private Date lastObserved;
		private Poop<?, ?> poop;
		
		public PVRecord(String name) {
			this.name= name;
			this.lastObserved= new Date(0);
			this.lastRequested= new Date();
		}
		
		public PVRecord(String name, long lastRequested, long lastObserved) {
			this.name= name;
			this.lastObserved= new Date(lastObserved);
			this.lastRequested= new Date(lastRequested);
		}
		
		public String getName() {
			return name;
		}
		
		public Date getLastRequested() {
			return lastRequested;
		}
		
		public void setLastObserved(Date lastObserved) {
			this.lastObserved = lastObserved;
		}
		
		public Date getLastObserved() {
			return lastObserved;
		}
		
		public Poop<?, ?> getPoop() {
			return poop;
		}
		
	}
	
	private PropertyChangeSupport support = new PropertyChangeSupport(this);
	private Map<String, PVRecord> records = new HashMap<String, PVCache.PVRecord>();
	private List<String> names= new ArrayList<String>();
	
	private XMLConfiguration store; 

	/**
	 * <p>Constructor for PVCache.</p>
	 *
	 * @param file a {@link java.io.File} object
	 */
	public PVCache(File file) {
		//Create a configuration from an XML.
		try {
			store = new XMLConfiguration(file);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		load();
	}
	
	
	/**
	 * Adds listener to event in this data (PVRecord) model.
	 *
	 * @param name name of event
	 * @param l listener
	 */
	public void addPropertyChangeListener(String name, PropertyChangeListener l) {
		support.addPropertyChangeListener(name, l);
	}
	
	/**
	 * Adds record to cache, if does not exist in cache yet.
	 *
	 * @param rec the record to be added
	 * @return returns <code>true</code> if record was added, otherwise <code>false</code>
	 */
	public boolean addRecord(PVRecord rec) {
		if (records.containsKey(rec.getName())) {
			return false;
		}
		synchronized (records) {
			if (records.containsKey(rec.getName())) {
				return false;
			}
			records.put(rec.getName(), rec);
			names.add(rec.getName());
			Collections.sort(names);
		}
		if (store!=null) {
			Node n= new Node("record");
			n.addChild(new Node("name", rec.getName()));
			n.addChild(new Node("last_observed", rec.getLastObserved().getTime()));
			n.addChild(new Node("last_requested", rec.getLastRequested().getTime()));
			store.getRoot().addChild(n);
			try {
				store.save();
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
		}
		support.firePropertyChange(PROPERTY_RECORD_ADDED, null, rec);
		return true;
	}
	
	/**
	 * Adds record to cache, if does not exist in cache yet.
	 *
	 * @return returns <code>true</code> if record was added, otherwise <code>false</code>
	 * @param name a {@link java.lang.String} object
	 */
	public boolean addRecord(String name) {
		if (records.containsKey(name)) {
			return false;
		}
		return addRecord(new PVRecord(name));
	}

	private void load() {
		if (store==null) {
			return;
		}
		//Iterate through button groups.
		List<ConfigurationNode> nodes = store.getRootNode().getChildren();
		for(int i = 0; i < nodes.size(); i++)
		{
			String name = store.getString("record("+i+").name");
			long lasto = store.getLong("record("+i+").last_observed");
			long lastr = store.getLong("record("+i+").last_requested");

			records.put(name, new PVRecord(name,lastr,lasto));
			names.add(name);
		}
		
		Collections.sort(names);
	}
	
	
	/**
	 * <p>Getter for the field <code>names</code>.</p>
	 *
	 * @return an array of {@link java.lang.String} objects
	 */
	public String[] getNames() {
		String [] n;
		synchronized (records) {
			n= names.toArray(new String[names.size()]);
		}
		return n;
	}
	
	/**
	 * <p>getRecord.</p>
	 *
	 * @param pv a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.casperr.PVCache.PVRecord} object
	 */
	public PVRecord getRecord(String pv) {
		return records.get(pv);
	}
	
	/**
	 * <p>updateRecord.</p>
	 *
	 * @param pv a {@link java.lang.String} object
	 * @param poop a {@link org.scictrl.csshell.Poop} object
	 * @return a boolean
	 */
	public boolean updateRecord(String pv, Poop<?, ?> poop) {
		PVRecord r= getRecord(pv);
		
		if (r== null) {
			return false;
		}
		
		r.poop=poop;
		
		support.firePropertyChange(PROPERTY_RECORD_UPDATED, null, r);
		
		return true;
	}

}
