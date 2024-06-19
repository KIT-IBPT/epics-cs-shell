/**
 * 
 */
package org.scictrl.csshell.directory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * <p>Directory class.</p>
 *
 * @author igor@scictrl.com
 */
public class Directory {
	
	
	private static final String DEFAULT_FILE_NAME = "csshell-directory.xml";

	
	

	private Map<String, String> aliases;
	private Map<String, Record> records;
	private Set<String> deprecated; 
	
	
	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		
		try {
			
			Directory dir= new Directory();
			
			Record r= dir.lookupRecord("Lama");
			
			System.out.println("Lama is "+r.getAlias()+"!");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * <p>Constructor for Directory.</p>
	 */
	public Directory() {
		aliases= new HashMap<String, String>();
		records= new HashMap<String, Record>();
		deprecated= new HashSet<String>();
		
		try {
			loadConfiguration(DEFAULT_FILE_NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void loadConfiguration(String file) throws Exception {
		
		XMLConfiguration conf= new XMLConfiguration(file);
		
		//SubnodeConfiguration sconf = conf.configurationAt("directory");
		SubnodeConfiguration sconf = conf.configurationAt("records");
		
		int i=0;
		
		String nameKey = "record("+i+").name";
		String aliasKey = "record("+i+").alias";
		String connKey = "record("+i+").connectionType";
		
		while (sconf.containsKey(nameKey)) {
			
			String name= sconf.getString(nameKey);
			String alias= sconf.getString(aliasKey);
			String connectionType= sconf.getString(connKey);
			
			if (Record.EPICS_CONNECTION_TYPE.equals(connectionType.toUpperCase())) {
				connectionType=Record.EPICS_CONNECTION_TYPE;
			}
			
			Record r= new Record(name, alias, connectionType);
			
			addRecord(r);
			
			i++;
			
			nameKey = "record("+i+").name";
			aliasKey = "record("+i+").alias";
			connKey = "record("+i+").connectionType";

		}
		
		String[] dep= conf.getStringArray("deprecated");
		
		if (dep!=null) {
			for (int j = 0; j < dep.length; j++) {
				deprecated.add(dep[j].trim());
			}
		}
		
	}


	/**
	 * <p>addRecord.</p>
	 *
	 * @param r a {@link org.scictrl.csshell.directory.Record} object
	 */
	public synchronized void addRecord(Record r) {
		
		if (r.getAlias()!=null) {
			aliases.put(r.getAlias(), r.getName());
			aliases.put(r.getName(),r.getAlias());
		}
		
		records.put(r.getName(), r);
		
	}


	/**
	 * If record provides name and alias, then this method will convert one to the other.
	 *
	 * @param alias the name or alias to be converted
	 * @return valid name or alias
	 */
	public String resolveAlias(String alias) {
		String name= aliases.get(alias);
		return name;
	}
	
	/**
	 * <p>lookupRecord.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.directory.Record} object
	 */
	public Record lookupRecord(String name) {
		
		Record r = records.get(name);
		
		if (r==null) {
			String n= resolveAlias(name);
			if (n!=null) {
				r=records.get(n);
			}
		}
		
		return r;
	}
	
	/**
	 * <p>isDeprecated.</p>
	 *
	 * @param name a {@link java.lang.String} object
	 * @return a boolean
	 */
	public boolean isDeprecated(String name) {
		return deprecated.contains(name);
	}
	
	/**
	 * <p>size.</p>
	 *
	 * @return a int
	 */
	public int size() {
		return records.size();
	}
	
	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder(512);
		
		sb.append("Directory::{ records=");
		sb.append(records.size());
		sb.append(", aliases=");
		sb.append(aliases.size());
		sb.append(", deprecated=");
		sb.append(deprecated.size());
		sb.append("}");
		
		return sb.toString();
	}

}
