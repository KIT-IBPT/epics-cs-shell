/**
 * 
 */
package org.scictrl.csshell.epics.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stores and restores state of Record or PersistentValueProcessor, which are registered.
 *
 * @author igor@scictrl.com
 */
public class PersistencyStore implements Runnable {
	
	class PersistencyProcessor implements PropertyChangeListener {
		
		private PersistentValueProcessor proc;
		private Record rec;

		public PersistencyProcessor(Record rec) {
			this.rec=rec;
			restore();
			this.rec.addPropertyChangeListener(Record.PROPERTY_VALUE, this);
		}
		
		public PersistencyProcessor(PersistentValueProcessor proc) {
			this.proc=proc;
			restore();
			proc.getRecord().addPropertyChangeListener(Record.PROPERTY_VALUE, this);
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			store();
			flushLater();
		}
		
		public void store() {
			if (proc!=null) {
				store(proc);
			} else {
				store(rec);
			}
		}
		
		public void restore() {
			if (proc!=null) {
				restore(proc);
			} else {
				restore(rec);
			}
		}
		
		public void store(PersistentValueProcessor proc) {
			
			String path= proc.getClass().getName()+DIVIDER+proc.getName();
			
			List<HierarchicalConfiguration> l=store.configurationsAt(path);
			
			HierarchicalConfiguration subconf=null;
			
			if (l.size()==0) {
				
				subconf= new XMLConfiguration();
				
				((XMLConfiguration) subconf).setRootElementName(path);
				
			} else {
				subconf= l.get(0);
			}
			
			proc.store(subconf);
			
			if (l.size()==0) {
				store.append(subconf);
			}		
		}

		public void store(Record rec) {
			String path= rec.getName()+DIVIDER+Record.PROPERTY_VALUE;
			String value= rec.getValueAsString();
			value= StringEscapeUtils.escapeXml10(value);
			value=value.replace(",", "\\,");
			store.setProperty(path, value);
		}

		public void restore(Record rec) {
			String path= rec.getName()+DIVIDER+Record.PROPERTY_VALUE;
			String value= store.getString(path);
			if (value!=null) {
				value=StringEscapeUtils.unescapeXml(value);
				value=value.replace("\\,", ",");
				rec.setValueAsString(value);
			}
		}

		public void restore(PersistentValueProcessor proc) {
			
			String path= proc.getClass().getName()+DIVIDER+proc.getName();
			
			List<HierarchicalConfiguration> l=store.configurationsAt(path);
			
			HierarchicalConfiguration subconf=null;
			
			if (l.size()==0) {
				
				return;
				
			}
			
			subconf= l.get(0);
			
			proc.restore(subconf);
			
		}

	}
	
	private static final String DIVIDER="::";
	
	
	Logger log= LogManager.getLogger(getClass());
	
	private File storeFile;
	private XMLConfiguration store;

	@SuppressWarnings("unused")
	private Database database;

	private ThreadPoolExecutor exec;

	private Map<Object,PersistencyProcessor> processors;

	/**
	 * Make instance of store, that operates as a dummy.
	 *
	 * @throws org.apache.commons.configuration.ConfigurationException configuration error
	 */
	public PersistencyStore() throws ConfigurationException {
	}
	
	/**
	 * <p>Constructor for PersistencyStore.</p>
	 *
	 * @param f a {@link java.io.File} object
	 * @param database a {@link org.scictrl.csshell.epics.server.Database} object
	 * @throws org.apache.commons.configuration.ConfigurationException if any.
	 */
	public PersistencyStore(File f, Database database) throws ConfigurationException {
		this.storeFile=f;
		this.database=database;

		if (!storeFile.exists()) {
			File p= storeFile.getParentFile();
			if (p!=null && !p.exists()) {
				p.mkdirs();
			}
		}

		if (storeFile.exists() && storeFile.length()==0) {
			storeFile.delete();
		}
		try {
			store= new XMLConfiguration();
			store.setAttributeSplittingDisabled(true);
			store.setDelimiterParsingDisabled(false);
			store.setFile(storeFile);
			if (storeFile.exists()) {
				store.load();
			}
			log.info("Using persistancy store file '"+storeFile+"'.");
		} catch (Exception e) {
			log.warn("Opening persistancy store file '"+storeFile+"' failed: "+e.toString(), e);
			throw e;
		}
		
		exec= new ThreadPoolExecutor(1,1,1000,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(1), Executors.defaultThreadFactory(),new RejectedExecutionHandler() {
			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// ignored
			}
		});
		
		processors= new HashMap<Object, PersistencyStore.PersistencyProcessor>();
	}
	
	/**
	 * <p>flush.</p>
	 */
	public synchronized void flush() {
		if (processors==null) return;

		try {
			store.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			log.error("Peristancy sore failed to flush to a file '"+storeFile+"'!",e);
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void run() {
		if (processors==null) return;
		flush();
		try {
			this.wait(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void flushLater() {
		if (exec.getQueue().size()<2) {
			exec.execute(this);
		}
		
	}

	/**
	 * Registers provided processor with store/restore service.
	 * First restores any previously stored status to the processor, as defined by {@link org.scictrl.csshell.epics.server.PersistentValueProcessor#restore(org.apache.commons.configuration.Configuration)}.
	 * Then processor is monitored for changes and any change triggers store functionality, as defined by {@link org.scictrl.csshell.epics.server.PersistentValueProcessor#store(org.apache.commons.configuration.Configuration)}.
	 *
	 * @param proc the processor to be stored
	 */
	public void registerProcessor(PersistentValueProcessor proc) {
		if (processors==null) return;
		if (processors.containsKey(proc)) {
			return;
		}
		PersistencyProcessor pp= new PersistencyProcessor(proc);
		processors.put(proc,pp);
		
	}
	
	/**
	 * Registers provided record to have value stored. Provided record is monitored for changes
	 * and any change triggers store functionality.
	 *
	 * @param rec a {@link org.scictrl.csshell.epics.server.Record} object
	 */
	public void registerValue(Record rec) {
		if (processors==null) return;
		if (processors.containsKey(rec)) {
			return;
		}
		processors.put(rec,new PersistencyProcessor(rec));
		
	}
	
	/**
	 * <p>deregister.</p>
	 *
	 * @param rec a {@link org.scictrl.csshell.epics.server.Record} object
	 * @return a boolean
	 */
	public boolean deregister(Record rec) {
		if (processors==null) return false;
		return processors.remove(rec)!=null;
	}
	
	/**
	 * <p>deregister.</p>
	 *
	 * @param proc a {@link org.scictrl.csshell.epics.server.PersistentValueProcessor} object
	 * @return a boolean
	 */
	public boolean deregister(PersistentValueProcessor proc) {
		if (processors==null) return false;
		return processors.remove(proc)!=null;
	}

	/**
	 * <p>saveAll.</p>
	 */
	public void saveAll() {
		if (processors==null) return;
		
		for (PersistencyProcessor pp : processors.values()) {
			pp.store();
		}
		
		flush();
	}

}
