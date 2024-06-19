/**
 * 
 */
package org.scictrl.csshell.epics.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scictrl.csshell.epics.server.processor.ListManagementProcessor;
import org.scictrl.csshell.epics.server.processor.ManagementProcessor;
import org.scictrl.csshell.epics.server.processor.PingManagementProcessor;
import org.scictrl.csshell.epics.server.processor.ShutdownManagementProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>ConfigurationManager class.</p>
 *
 * @author igor@scictrl.com
 */
public final class ConfigurationManager {

	/** Constant <code>NULL_STRING=""</code> */
	public static final String NULL_STRING="";
	/** Constant <code>ZERO_DOUBLE=0.0</code> */
	public static final double ZERO_DOUBLE=0.0;
	
	/**
	 * <p>getSeverity.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @param prefix a {@link java.lang.String} object
	 * @param def a {@link gov.aps.jca.dbr.Severity} object
	 * @return a {@link gov.aps.jca.dbr.Severity} object
	 */
	public static final Severity getSeverity(final HierarchicalConfiguration config, final String prefix, final Severity def) {
		StringBuilder p= new StringBuilder(48);
		if (prefix!=null) {
			p.append(prefix);
			if (prefix.charAt(prefix.length()-1)!='.') {
				p.append('.');
			}
		}
		p.append("severity");
		String s= config.getString(p.toString(), def.getName());
		return Severity.forName(s);
	}
	
	/**
	 * <p>getStatus.</p>
	 *
	 * @param config a {@link org.apache.commons.configuration.HierarchicalConfiguration} object
	 * @param prefix a {@link java.lang.String} object
	 * @param def a {@link gov.aps.jca.dbr.Status} object
	 * @return a {@link gov.aps.jca.dbr.Status} object
	 */
	public static final Status getStatus(final HierarchicalConfiguration config, final String prefix, final Status def) {
		StringBuilder p= new StringBuilder(48);
		if (prefix!=null) {
			p.append(prefix);
			if (prefix.charAt(prefix.length()-1)!='.') {
				p.append('.');
			}
		}
		p.append("status");
		String s= config.getString(p.toString(), def.getName());
		return Status.forName(s);
	}

	/**
	 * Visitor for hierarchical configuration, used during configuration parsing.
	 */
	public static final class ConfigurationVisitor {
		
		private List<String> namePaths;
		private List<Map<String, String>> substitutions;
		private String namePath;
		private List<Record> records;
		private Document alarmDoc;
		private boolean alarmConf;
		private String name;
		private List<Application> applications;
		private Map<String, String> subsMerge;
		private Map<String, SubnodeConfiguration> templates = new HashMap<String, SubnodeConfiguration>(8);
		private Map<String, List<String>> alarmPaths= new HashMap<String, List<String>>();
		
		/**
		 * Constructor.
		 * 
		 * @param name server configuration name
		 * @param alarmDoc alarm configuration
		 */
		public ConfigurationVisitor(String name, Document alarmDoc) {
			
			this.name=name;
			this.alarmDoc=alarmDoc;
			alarmConf=alarmDoc!=null;
			
			namePaths=new ArrayList<String>(8);
			substitutions=new ArrayList<Map<String,String>>(8);
			records=new ArrayList<Record>(256);
			applications= new ArrayList<Application>(8);
		}
		
		/**
		 * Adds application.
		 * 
		 * @param app an application
		 */
		void add(Application app) {
			applications.add(app);
		}
		
		/**
		 * Adds record
		 * @param r a record
		 */
		void add(Record r) {
			records.add(r);
			log.info("Loaded '"+r+"'.");
		}

		/**
		 * Returns name path
		 * @return name path
		 */
		String getNamePath() {
			return namePath==null ? NULL_STRING : namePath;
		}
		
		private void removePath(String path) {
			if (path==null || path.length()==0) {
				return;
			}
			namePaths.remove(path);
			updateNamePath();
		}

		private void addPath(String path) {
			if (path==null || path.length()==0) {
				return;
			}
			namePaths.add(path);
			updateNamePath();
		}

		private void updateNamePath() {
			
			StringBuilder sb= new StringBuilder(8+namePaths.size()*8);
			for (String s : namePaths) {
				sb.append(s);
			}
			namePath=sb.toString();
		}
		
		/**
		 * Returns collected records.
		 * @return collected records
		 */
		public Record[] records() {
			return records.toArray(new Record[records.size()]);
		}
		
		/**
		 * Returns collected applications.
		 * @return collected applications
		 */
		public Application[] applications() {
			return applications.toArray(new Application[applications.size()]);
		}

		/**
		 * Server configuration name.
		 * @return server configuration name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Alarm configuration.
		 * @return alarm configuration
		 */
		public Document getAlarmDoc() {
			return alarmDoc;
		}

		/**
		 * Inserts CSS Alarm Beast configuration DOM nodes into DOM document corresponding to provided parameter.
		 * The parameters belong to single PV tag with information about it's position in alarm configuration.
		 * 
		 * @param config Beast configuration
		 * @param components array with component names defining path to PV tag
		 * @param pv the name attribute of PV tag
		 * @param description the value of description sub-tag of PV tag
		 * @param enabled the value of enabled sub-tag of PV tag
		 * @param latching the value of latching sub-tag of PV tag
		 * @param displayTitle the display title value for nested tags  
		 * @param displayDetails the display details value for nested tags
		 */
		
		void addAlarmConfig(String[] components, String pv, String description, boolean enabled, boolean latching, String displayTitle, String displayDetails, int delay) {
			if (!alarmConf) {
				return;
			}
			
			if (pv==null) {
				throw new NullPointerException("Alarm PV is null!");
			}
			if (components==null || components.length==0) {
				throw new IllegalArgumentException("Alarm config for '"+pv+"' has no component hirarchy, PV can not be added directly to the root!");
			}
			
			if (alarmDoc.getDocumentElement()==null) {
				alarmDoc.appendChild(alarmDoc.createElement("config"));
			}
			
			Element confEl= alarmDoc.getDocumentElement();
			Element nodeCo=confEl;
	
			for (int i = 0; i < components.length; i++) {
				NodeList nl = nodeCo.getChildNodes();
				Element el=null;
				
				for (int j = 0; j < nl.getLength(); j++) {
					el=(Element)nl.item(j);
					if ("component".equals(el.getNodeName()) && components[i].equals(el.getAttribute("name"))) {
						nodeCo=el;
						break;
					}
				}
				if (!"component".equals(nodeCo.getNodeName()) || !components[i].equals(nodeCo.getAttribute("name"))) {
					el= alarmDoc.createElement("component");
					el.setAttribute("name", components[i]);
					nodeCo.appendChild(el);
					nodeCo=el;
				}
			}
			
			Element nodePV= alarmDoc.createElement("pv");
			nodePV.setAttribute("name", pv);
			nodeCo.appendChild(nodePV);
			
			Element node = alarmDoc.createElement("description");
			node.setTextContent(description);
			nodePV.appendChild(node);
			
			node= alarmDoc.createElement("latching");
			node.setTextContent(Boolean.toString(latching));
			nodePV.appendChild(node);
			
			node= alarmDoc.createElement("enabled");
			node.setTextContent(Boolean.toString(enabled));
			nodePV.appendChild(node);

			if (delay>-1) {
				node= alarmDoc.createElement("delay");
				node.setTextContent(Integer.toString(delay));
				nodePV.appendChild(node);
			}

			if (displayDetails!=null || displayTitle!=null) {
				if (displayDetails==null) {
					displayDetails=NULL_STRING;
				}
				if (displayTitle==null) {
					displayTitle=NULL_STRING;
				}
				Element nodeDisp= alarmDoc.createElement("display");
				node= alarmDoc.createElement("title");
				node.setTextContent(displayTitle);
				nodeDisp.appendChild(node);
				node= alarmDoc.createElement("details");
				node.setTextContent(displayDetails);
				nodeDisp.appendChild(node);
				nodePV.appendChild(nodeDisp);
			}
			
			
		}

		Map<String, String> getSubstitutions() {
			if (subsMerge!=null) {
				return subsMerge;
			}
			subsMerge= new HashMap<String, String>();
			for (Map<String,String> prop : substitutions) {
				subsMerge.putAll(prop);
			}
			return subsMerge;
		}

		void addSubstitutions(Map<String,String> subs) {
			substitutions.add(subs);
			subsMerge=null;
		}

		void removeSubstitutions(Map<String,String> subs) {
			substitutions.remove(subs);
			subsMerge=null;
		}

		/**
		 * Adds a substitution template
		 * @param name template name
		 * @param config configuration to be inserted instead template name
		 */
		public void addTemplate(String name, SubnodeConfiguration config) {
			if (templates.containsKey(name)) {
				log.warn("Template with name '"+name+"' already exists, duplicate ignored!");
				return;
			}
			
			templates.put(name, config);
			
		}
		
		/**
		 * Returns template.
		 * @param name template name
		 * @return template configuration
		 */
		public SubnodeConfiguration getTemplate(String name) {
			return templates.get(name);
		}
		
		/**
		 * Adds alarm path
		 * @param path path
		 * @param pv associated PV
		 */
		public void addAlarmPath(String[] path, String pv) {
			
			StringBuilder sb= new StringBuilder(128);
			sb.append(path[0]);
			for (int i = 1; i < path.length; i++) {
				sb.append(',');
				sb.append(path[i]);
			}
			String p= sb.toString();
			
			List<String> l= alarmPaths.get(p);
			if (l==null) {
				l= new ArrayList<String>();
				alarmPaths.put(p, l);
			}
			l.add(pv);
			
		}

		/**
		 * Returns collected alarm paths.
		 * @return collected alarm paths
		 */
		public String[] getAlarmPaths() {
			String[] s= alarmPaths.keySet().toArray(new String[alarmPaths.size()]);
			Arrays.sort(s);
			return s;
		}
		
		/**
		 * Returns collected PVs for alarm path.
		 * @param path a path name
		 * @return collected PVs for alarm path
		 */
		public List<String> getPVsForAlarmPath(String path) {
			return new ArrayList<String>(alarmPaths.get(path));
		}
		
		/**
		 * Substitutes macros in provided string.
		 * @param s string to be substituted
		 * @return substituted string
		 */
		public String replace(String s) {
			StringSubstitutor sub= new StringSubstitutor(getSubstitutions());
			return sub.replace(s);
		}
	}
	
	private static final Logger log= LogManager.getLogger(ConfigurationManager.class);
	
	/**
	 * <p>Constructor for ConfigurationManager.</p>
	 */
	public ConfigurationManager() {
	}
	

	/**
	 * Loads EPICS server configuration from XML File.
	 *
	 * @param file XML file
	 * @param name server name
	 * @return array of record created from XML
	 * @throws org.apache.commons.configuration.ConfigurationException if loading fails
	 * @throws javax.xml.parsers.ParserConfigurationException parsing failed
	 */
	public static final Record[] loadConfig(final String file, final String name) throws ConfigurationException, ParserConfigurationException {
		
		return loadConfig(file, new ConfigurationVisitor(name, null));
		
	}
	
	/**
	 * Loads EPICS server configuration from XML File.
	 *
	 * @param file XML file
	 * @return array of record created from XML
	 * @throws org.apache.commons.configuration.ConfigurationException if loading fails
	 * @throws javax.xml.parsers.ParserConfigurationException parsing failed
	 * @param visitor a {@link org.scictrl.csshell.epics.server.ConfigurationManager.ConfigurationVisitor} object
	 */
	public static final Record[] loadConfig(final String file, final ConfigurationVisitor visitor) throws ConfigurationException, ParserConfigurationException {
		
		Logger log= LogManager.getLogger(ConfigurationManager.class);
		
		log.info("Loading server '"+visitor.name+"' from '"+file);

		Map<String,String> subs= new HashMap<String,String>();
		InetAddress addr=null;
		try {
			addr = InetAddress.getLocalHost();
			subs.put("host",addr.getHostName().toUpperCase());
		} catch (UnknownHostException e) {
			subs.put("host","localhost");
			e.printStackTrace();
		}
		subs.put("server",visitor.name);

		visitor.addSubstitutions(subs);
		
		//Create a configuration from an XML.
		XMLConfiguration config = new XMLConfiguration();
		//config.setDelimiterParsingDisabled(true);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (config.isValidating()) {
            factory.setValidating(true);
            if (config.isSchemaValidation()) {
                factory.setNamespaceAware(true);
                factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage","http://www.w3.org/2001/XMLSchema");
            }
        } 
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        DocumentBuilder db = factory.newDocumentBuilder();
        db.setEntityResolver(config.getEntityResolver());
		
        config.setDocumentBuilder(db);
        
		config.load(file);

		int count = config.getRootNode().getChildrenCount();
		
		for (int i=0;i<count;i++) {
			List<ConfigurationNode> att= config.getRootNode().getChild(i).getAttributes("name");
			if (att.size()>0 && visitor.name.equals(att.get(0).getValue())) {
				loadConfig(visitor,config.configurationAt("server("+i+")"));
			}
		}
		
		log.info("Loaded "+visitor.records.size()+" records");
		return visitor.records();
		
	}

	private static final void loadConfig(final ConfigurationVisitor visitor, final SubnodeConfiguration config) throws ConfigurationException {
		
		Map<String,String> subs=null;
		List<?> l= config.configurationsAt("substitutions");
		if (l.size()>0) {
			SubnodeConfiguration subConf= config.configurationAt("substitutions");
			Iterator<String> it= subConf.getKeys();
			subs=new HashMap<String,String>();
			while (it.hasNext()) {
				String key= it.next();
				String val= visitor.replace((String) subConf.getString(key));
				subs.put(key, val);
			}
			visitor.addSubstitutions(subs);
		}

		
		int count= config.getRootNode().getChildrenCount();
		int g=0,r=0,m=0,a=0;
		for (int i=0;i<count;i++) {
			if ("group".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadGroup(visitor,config.configurationAt("group("+(g++)+")"));
			} else if ("record".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadRecord(visitor,config.configurationAt("record("+(r++)+")"));
			} else if ("management".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadManagement(visitor,config.configurationAt("management("+(m++)+")"));
			} else if ("application".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadApplication(visitor,config.configurationAt("application("+(a++)+")"));
			} 
		}
	}

	private static final void loadRecord(final ConfigurationVisitor visitor, final SubnodeConfiguration config) throws ConfigurationException {
		if (!"record".equalsIgnoreCase(config.getRootNode().getName())) {
			throw new ConfigurationException("Expected a RECORD element, but got instead "+config.getRootNode().getName());
		}
		
		injectSubstitutions(visitor.getSubstitutions(), config);
		
		String name=visitor.getNamePath()+config.getString("name");
		
		log.debug("Loading record '"+name+"'.");
		
		String description= config.getString("description",NULL_STRING);
		
		Record r=null;

		if (config.configurationsAt("processor").size()>0) {
			
			String s= config.getString("type");
			
			/*if (s==null) {
				throw new ConfigurationException("Record '"+name+"' is missing type definition tag!");
			}*/
			
			DBRType type = s!=null ? DBRType.forName(s.toUpperCase()) : DBRType.UNKNOWN;
			
			if (type==null) {
				//throw new ConfigurationException("Record '"+name+"' type definition tag value '"+s+"' is not valid type string!");
				type= DBRType.UNKNOWN;
			}
			
			
			int count= config.getInt("count",1);
			String units= config.getString("units",NULL_STRING); 
			Number upperDispLimit= config.getDouble("upperDispLimit", ZERO_DOUBLE);
			Number lowerDispLimit= config.getDouble("lowerDispLimit", ZERO_DOUBLE);
			Number upperWarningLimit= config.getDouble("upperWarningLimit", ZERO_DOUBLE);
			Number lowerWarningLimit= config.getDouble("lowerWarningLimit", ZERO_DOUBLE);
			Number upperAlarmLimit= config.getDouble("upperAlarmLimit", ZERO_DOUBLE);
			Number lowerAlarmLimit= config.getDouble("lowerAlarmLimit", ZERO_DOUBLE);
			Number upperCtrlLimit= config.getDouble("upperCtrlLimit", ZERO_DOUBLE);
			Number lowerCtrlLimit= config.getDouble("lowerCtrlLimit", ZERO_DOUBLE);
			short precision = config.getShort("precision",(short)ZERO_DOUBLE);
			String[] enumLabels = config.getStringArray("enumLabels");
			

			r= new Record(name,type,count,units,upperDispLimit,lowerDispLimit,upperWarningLimit,lowerWarningLimit,upperAlarmLimit,lowerAlarmLimit,upperCtrlLimit,lowerCtrlLimit,precision,enumLabels,description);
			
			r.updateAlarm(getSeverity(config,"alarm",Severity.NO_ALARM), getStatus(config,"alarm",Status.NO_ALARM));
			
			boolean persistent = config.getBoolean("persistent",false);
			r.setPersistent(persistent);
			
			boolean writable = config.getBoolean("writable",true);
			r.setWritable(writable);

			SubnodeConfiguration subConf= config.configurationAt("processor");
			injectSubstitutions(visitor.getSubstitutions(), subConf);
			
			String cl=subConf.getString("[@instance]");
			
			final String oldPac="com.kriznar";
			final String newPac="org.scictrl";
			
			if (cl.startsWith(oldPac)) {
				cl= newPac+cl.substring(oldPac.length());
			}

			
			try {
				ValueProcessor vp = (ValueProcessor) Class.forName(cl).getDeclaredConstructor().newInstance();
				// We set processor to record, in case processor does something on record that requires processor be set  
				r.setProcessor(vp);
				vp.configure(r,subConf);
				// we set again since this update the record type with processor type and some processors might change type from configuration.
				r.setProcessor(vp);
			} catch (Exception e) {
				throw new ConfigurationException("Value processor '"+cl+"' failed to instantiate: "+e.toString(),e);
			} finally {
				cleanupSubstitutions(visitor.getSubstitutions(), subConf);				
			}
			
			visitor.records.add(r);

			log.info("Loaded '"+r+"'.");
		}
		
		
		if (visitor.alarmConf && config.containsKey("alarmConf.path")) {

			SubnodeConfiguration subConf= config.configurationAt("alarmConf");
			injectSubstitutions(visitor.getSubstitutions(), subConf);
			
			String[] components = subConf.getStringArray("path");
			List<String> l= new ArrayList<String>(components.length*2);
			for (String s : components) {
				s=s.trim();
				if (s.contains(",")) {
					String[] ss= s.split(",");
					for (String st : ss) {
						st=st.trim();
						if(st.length()>0) {
							l.add(st);
						}
					}
				} else {
					if(s.length()>0) {
						l.add(s);
					}
				}
			}
			components= l.toArray(new String[l.size()]);
			
			String pv= name;
			String alDescription= subConf.getString("description",description);
			boolean enabled= subConf.getBoolean("enabled",Boolean.TRUE);
			boolean latching= subConf.getBoolean("latching",Boolean.TRUE);
			String displayTitle= subConf.getString("display.title");
			String displayDetails= subConf.getString("display.details");
			int delay= subConf.getInt("delay",-1);
			
			visitor.addAlarmConfig(components, pv, alDescription, enabled, latching, displayTitle, displayDetails,delay);
			
			if (enabled) {
				visitor.addAlarmPath(config.getStringArray("alarmConf.path"), name);
			}

			log.debug("Alarm conf loaded: pv:'"+pv+"' comps:'"+Arrays.toString(components)+"'.");
			cleanupSubstitutions(visitor.getSubstitutions(), subConf);
		}
		
		cleanupSubstitutions(visitor.getSubstitutions(), config);

	}

	private static final void loadApplication(final ConfigurationVisitor visitor, final SubnodeConfiguration config) throws ConfigurationException {
		if (!"application".equalsIgnoreCase(config.getRootNode().getName())) {
			throw new ConfigurationException("Expected a APPLICATION element, but got instead "+config.getRootNode().getName());
		}
		
		Map<String, String> subs= visitor.getSubstitutions();
		injectSubstitutions(subs, config);
		
		String name=config.getString("name");
		
		if (name==null) {
			throw new ConfigurationException("application name is missing!");
		}

		name = visitor.getNamePath() + name;
		
		name= StringSubstitutor.replace(name, subs);
		
		log.debug("Loading application '"+name+"'.");
		
		String cl=config.getString("[@instance]");
		
		final String oldPac="com.kriznar";
		final String newPac="org.scictrl";
		
		if (cl.startsWith(oldPac)) {
			cl= newPac+cl.substring(oldPac.length());
		}
			
		try {
			log.info("Loading application '"+cl+"' with name '"+name+"'.");
			Application app = (Application) Class.forName(cl).getDeclaredConstructor().newInstance();
			app.configure(name,config);
			
			visitor.add(app);
			for (String pv : app.getRecordNames()) {
				Record r= app.getRecord(pv);
				if (r==null) {
					log.warn("Applications '"+name+"' declared PV '"+pv+"' but is not supporting it.");
				} else {
					visitor.add(r);
				}
			}
			log.info("Loaded '"+app+"'.");
		} catch (Exception e) {
			throw new ConfigurationException("Value processor '"+cl+"' failed to instantiate: "+e.toString(),e);
		} finally {
			cleanupSubstitutions(visitor.getSubstitutions(), config);
		}
	}

	private static final void injectSubstitutions(final Map<String,String> subs, final SubnodeConfiguration config) {
		for (String key : subs.keySet()) {
			if (!config.containsKey(key)) {
				config.setProperty(key, subs.get(key));
			}
		}
	}

	private static final void cleanupSubstitutions(final Map<String,String> subs, final SubnodeConfiguration config) {
		for (String key : subs.keySet()) {
			Object o1=config.getProperty(key);
			String o2=subs.get(key);
			if (o1==o2) {
				config.clearProperty(key);
			} else if (o2.contains(",")) {
				o2=o2.replace("\\,",",");
				if (o2.equals(o1)) {
					config.clearProperty(key);
				}
			}
		}
	}

	private static final void loadManagement(final ConfigurationVisitor visitor, final SubnodeConfiguration config) throws ConfigurationException {
		if (!"management".equalsIgnoreCase(config.getRootNode().getName())) {
			throw new ConfigurationException("Expected a MANAGEMENT element, but got instead "+config.getRootNode().getName());
		}
		
		injectSubstitutions(visitor.getSubstitutions(), config);
		
		String name=config.getString("name","${host}:${server}");
		
		/*if (name.contains("${host}")) {
			InetAddress addr=null;
			try {
				addr = InetAddress.getLocalHost();
				name=name.replace("${host}", addr.getHostName().toUpperCase());
			} catch (UnknownHostException e) {
				name=name.replace("${host}", "localhost");
				e.printStackTrace();
			}
		}
		
		if (name.contains("${server}")) {
			name=name.replace("${server}", visitor.name);
		}*/

		String shutdownName= config.getString("shutdown", ":Shutdown");
		String pingName= config.getString("ping", ":Ping");
		String listName= config.getString("list", ":List");
		
		int count= 1;
		String units= NULL_STRING; 
		String description= "Shutdown management record"; 
		Number upperDispLimit= ZERO_DOUBLE;
		Number lowerDispLimit= ZERO_DOUBLE;
		Number upperWarningLimit= ZERO_DOUBLE;
		Number lowerWarningLimit= ZERO_DOUBLE;
		Number upperAlarmLimit= ZERO_DOUBLE;
		Number lowerAlarmLimit= ZERO_DOUBLE;
		Number upperCtrlLimit= ZERO_DOUBLE;
		Number lowerCtrlLimit= ZERO_DOUBLE;
		short precision = (short)ZERO_DOUBLE;
		String[] enumLabels = new String[0];
		
		Record r= new Record(name+shutdownName,null,count,units,upperDispLimit,lowerDispLimit,upperWarningLimit,lowerWarningLimit,upperAlarmLimit,lowerAlarmLimit,upperCtrlLimit,lowerCtrlLimit,precision,enumLabels,description);
		
		String severity= config.getString("alarm.severity", Severity.NO_ALARM.getName());
		String status= config.getString("alarm.status", Status.NO_ALARM.getName());
		r.updateAlarm(Severity.forName(severity), Status.forName(status));

		ManagementProcessor mp= new ShutdownManagementProcessor();
		mp.configure(r, config);
		r.setProcessor(mp);

		visitor.records.add(r);

		
		description= "Ping management record"; 
				
		r= new Record(name+pingName,null,count,units,upperDispLimit,lowerDispLimit,upperWarningLimit,lowerWarningLimit,upperAlarmLimit,lowerAlarmLimit,upperCtrlLimit,lowerCtrlLimit,precision,enumLabels,description);
		r.updateAlarm(Severity.forName(severity), Status.forName(status));
		
		mp= new PingManagementProcessor();
		mp.configure(r, config);
		r.setProcessor(mp);

		visitor.records.add(r);
		

		description= "List all records management record"; 
		
		r= new Record(name+listName,null,count,units,upperDispLimit,lowerDispLimit,upperWarningLimit,lowerWarningLimit,upperAlarmLimit,lowerAlarmLimit,upperCtrlLimit,lowerCtrlLimit,precision,enumLabels,description);
		r.updateAlarm(Severity.forName(severity), Status.forName(status));
		
		mp= new ListManagementProcessor();
		mp.configure(r, config);
		r.setProcessor(mp);

		visitor.records.add(r);
		
		cleanupSubstitutions(visitor.getSubstitutions(), config);

	}


	private static final void loadGroup(final ConfigurationVisitor visitor, SubnodeConfiguration config) throws ConfigurationException {
		boolean template=config.getBoolean("[@template]",false);
		if (template) {
			visitor.addTemplate(config.getString("[@name]"),config);
			return;
		}

		String path=config.getString("[@path]","");
		visitor.addPath(path);
		Map<String,String> subs=new HashMap<String,String>();
		subs.put("path", visitor.getNamePath());
		subs.put("path1", path);
		
		List<HierarchicalConfiguration> l= config.configurationsAt("substitutions");
		
		for (HierarchicalConfiguration subConf : l) {
			//SubnodeConfiguration subConf= config.configurationAt("substitutions");
			Iterator<String> it= subConf.getKeys();
			while (it.hasNext()) {
				String key= it.next();
				String val= visitor.replace(subConf.getString(key));
				val = val.replace("${path}", visitor.getNamePath());
				val = val.replace("${path1}", path);
				if (val.contains(",")) {
					// if property contains ',' character, then means it was escaped in the config file, if we want to preserve
					// this character further the line, we musk keep the escape
					int i=-2;
					while ((i=val.indexOf(',',i+2))>-1) {
						val= val.substring(0, i) + '\\' +  val.substring(i);
					}
				}
				subs.put(key, val);
			}
		}
		visitor.addSubstitutions(subs);

		String ins= config.getString("insert");
		if (ins!=null) {
			SubnodeConfiguration temp= visitor.getTemplate(ins);
			if (temp!=null) {
				config=temp;
			} else {
				log.warn("Template '"+ins+"' does not exist, nothing will be injected!");
			}
		}
		
		int count= config.getRootNode().getChildrenCount();
		int r=0,g=0,a=0;
		for (int i=0;i<count;i++) {
			if ("record".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadRecord(visitor,config.configurationAt("record("+(r++)+")"));
			} else if ("group".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadGroup(visitor,config.configurationAt("group("+(g++)+")"));
			} else if ("application".equalsIgnoreCase(config.getRootNode().getChild(i).getName())) {
				loadApplication(visitor,config.configurationAt("application("+(a++)+")"));
			}
		}
		
		visitor.removePath(path);
		if (subs!=null) {
			visitor.removeSubstitutions(subs);
		}
	}

	
	
}
