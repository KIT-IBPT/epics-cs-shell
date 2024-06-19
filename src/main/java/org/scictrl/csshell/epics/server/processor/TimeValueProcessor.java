/**
 * 
 */
package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * <p>TimeValueProcessor class.</p>
 *
 * @author igor@scictrl.com
 */
public class TimeValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {
	
	private static final long DEFAULT_MONITOR_TRIGGER = 1000;

	
	private enum Out {
		UNIX("UNIX"),FORMAT(""),FIELDS("FIELDS"),DURATION("DURATION");
		
		private String id;

		private Out(String id) {
			this.id= id;
		}
		
		static Out fromFormat(String format) {
			if (format==null) {
				return FORMAT;
			}
			format=format.trim().toUpperCase();
			if (format.equals(UNIX.id)) {
				return UNIX;
			}
			if (format.equals('\''+UNIX.id+'\'')) {
				return UNIX;
			}
			if (format.equals(FIELDS.id)) {
				return FIELDS;
			}
			if (format.equals('\''+FIELDS.id+'\'')) {
				return FIELDS;
			}
			if (format.startsWith(DURATION.id)) {
				return DURATION;
			}
			if (format.startsWith('\''+DURATION.id)) {
				return DURATION;
			}
			return FORMAT;
		}
		
		/**
		 * Creates fromatter from format specification string
		 * @param format format as defined in configuration
		 * @return appropriate formatter
		 */
		public SimpleDateFormat formatter(String format) {
			if (format==null) {
				return new SimpleDateFormat(FORMAT_ISO);
			}
			if (this==DURATION) {
				if (format.startsWith(id)) {
					format=format.substring(id.length());
					if (format.startsWith(":")) {
						format=format.substring(1);
						return new SimpleDateFormat(format);
					}
				}
				return new SimpleDateFormat(FORMAT_MIN);
			}
			return new SimpleDateFormat(format);
		}
	}
	
	
	/** Constant <code>FORMAT_ISO="yyyy-MM-dd'T'HH:mm:ss.SSSZ"</code> */
	public static final String FORMAT_ISO= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	/** Constant <code>FORMAT_MIN="mm:ss"</code> */
	public static final String FORMAT_MIN= "mm:ss";
	private String format;
	private SimpleDateFormat formatter;
	private Out type;
	private ValueLinks link;

	/**
	 * <p>Constructor for TimeValueProcessor.</p>
	 */
	public TimeValueProcessor() {
		
	}
	
	/**
	 * <p>Constructor for TimeValueProcessor.</p>
	 *
	 * @param format a {@link java.lang.String} object
	 */
	public TimeValueProcessor(String format) {
		this.format=format;
		
		this.type= Out.fromFormat(format);
		
		if (type==Out.FORMAT) {
			this.formatter= new SimpleDateFormat(format);
			super.type=DBRType.STRING;
		} if (type==Out.DURATION) {
			this.formatter= new SimpleDateFormat(FORMAT_MIN);
			super.type=DBRType.STRING;
		} else if (type==Out.FIELDS) {
			super.type=DBRType.INT;
		}
		
	}

	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		this.format=config.getString("format", FORMAT_ISO);
		
		this.type= Out.fromFormat(format);
		
		if (type==Out.FORMAT) {
			this.formatter= new SimpleDateFormat(format);
			super.type=DBRType.STRING;
			record.setCount(1);
		} if (type==Out.DURATION) {
			this.formatter= type.formatter(format);
			super.type=DBRType.STRING;
			record.setCount(1);
		} else if (type==Out.FIELDS) {
			super.type=DBRType.INT;
			record.setCount(7);
		} else {
			// this is UNIX
			record.setCount(1);
		}
		
		String l= config.getString("link");
		if (l!=null) {
			link= new ValueLinks(l,getName(),l,this,Record.PROPERTY_VALUE);
			trigger=0;
			process(0,null,null,false);
		} else {
			if (trigger<1) {
				trigger=DEFAULT_MONITOR_TRIGGER;
			}
			process(System.currentTimeMillis(),null,null,false);
		}

	}
	
	/** {@inheritDoc} */
	@Override
	public void process() {
		process(System.currentTimeMillis(),null,null,true);
	}
	
	/**
	 * <p>process.</p>
	 *
	 * @param time a long
	 * @param severity a {@link gov.aps.jca.dbr.Severity} object
	 * @param status a {@link gov.aps.jca.dbr.Status} object
	 * @param notify a boolean
	 * @return a {@link java.lang.Object} object
	 */
	public Object process(long time, Severity severity, Status status, boolean notify) {
		
		Object value= convert(time);
		_setValue(value,severity,status,notify);
		return value;
	}
	
	/**
	 * <p>convert.</p>
	 *
	 * @param time a long
	 * @return a {@link java.lang.Object} object
	 */
	public Object convert(long time) {
		
		Object value;
		
		if (type==Out.FORMAT) {
			value= new String[]{formatter.format(new Date(time))};
		} else if (type==Out.FIELDS) {
			Calendar c= Calendar.getInstance();
			c.setTimeInMillis(time);
			value= new int[]{c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND),c.get(Calendar.MILLISECOND)};
		} else if (type==Out.DURATION) {
			long hours= (long)(time/1000.0/60.0/60.0);
			long minutes= time-(hours*60*60*1000);
			StringBuilder sb= new StringBuilder(16);
			sb.append(hours);
			sb.append(":");
			sb.append(formatter.format(new Date(minutes)));
			value= new String[]{sb.toString()};
		} else {
			value= new int[]{(int)(time/1000L)};
		}
		
		return value;
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (!link.isReady()) {
			record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM);
			return;
		}
		//System.out.println(getName()+" "+link.consume()[0].longValue()+" "+link.getLastSeverity());
		process(link.consume()[0].longValue()*1000, link.getLastSeverity(), link.getLastStatus(), true);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (link!=null) {
			link.activate(record.getDatabase());
		}
	}
}
