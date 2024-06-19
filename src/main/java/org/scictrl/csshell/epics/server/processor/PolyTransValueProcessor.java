package org.scictrl.csshell.epics.server.processor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.scictrl.csshell.DataType;
import org.scictrl.csshell.MetaData;
import org.scictrl.csshell.MetaDataImpl;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;

/**
 * Value processor, which connects to provided remote names and uses polynomial transformation.
 * By default first value from array of linked values is used.
 *
 *
 * @deprecated Functionality of polynomial transformation has been moved to LinkedValueProcessor and is further mentained there in superior form.
 * 
 * @author igor@scictrl.com
 */
@Deprecated 
public class PolyTransValueProcessor extends MemoryValueProcessor implements PropertyChangeListener {

	/**
	 * Creates processor, configures it and returns it embedded within the returned record.
	 *
	 * @param name name of the returned record
	 * @param description the description of the record
	 * @param link the link of the returned processor
	 * @return new record with embedded and configured processor
	 */
	public static final PolyTransValueProcessor newProcessor(String name, String description, String link) {
		
		Record r= new Record(name, DBRType.DOUBLE, 1);

		PolyTransValueProcessor lvp= new PolyTransValueProcessor();
		lvp.configure(r,new HierarchicalConfiguration());
		lvp.configure(link, Record.PROPERTY_VALUE);
		
		r.setProcessor(lvp);
		
		return lvp;
	}

	
	private void configure(String name, String type) {
		this.input= new ValueLinks(record.getName(), new String[]{name}, this, type);
		record.updateAlarm(Severity.INVALID_ALARM, Status.UDF_ALARM, false);
	}

	/**
	 * Input value remote link.
	 */
	protected ValueLinks input;
	private PolynomialTransformation transform;

	/**
	 * <p>Constructor for PolyTransValueProcessor.</p>
	 */
	public PolyTransValueProcessor() {
		type=DBRType.DOUBLE;
	}
	
	/** {@inheritDoc} */
	@Override
	public void configure(Record record, HierarchicalConfiguration config) {
		super.configure(record, config);
		
		record.setPersistent(true);
		
		String name= config.getString("input.link");
		
		String type= Record.toPropertyName(config.getString("input.type",Record.PROPERTY_VALUE));
		
		transform= new PolynomialTransformation();
		List<?> l=config.configurationsAt("transform"); 
		if (l.size()==1) {
			transform.configure((HierarchicalConfiguration) l.get(0));
		}
		
		if (name!=null) {
			configure(name, type);
		}
		
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		
		if (input!=null) {
			input.activate(getRecord().getDatabase());
			MetaData data= input.getMetaData(0);
			
			MetaData trans= new MetaDataImpl(
					data.getName(),
					getRecord().getDescription(),
					transform.transformX(data.getMinimum()),
					transform.transformX(data.getMaximum()),
					transform.transformX(data.getDisplayMin()),
					transform.transformX(data.getDisplayMax()),
					transform.transformX(data.getWarnMin()),
					transform.transformX(data.getWarnMax()),
					transform.transformX(data.getAlarmMin()),
					transform.transformX(data.getAlarmMax()),
					null,
					null,
					data.getFormat(),
					getRecord().getUnits(),
					1,
					(int) getRecord().getPrecision(),
					DataType.DOUBLE,
					null,
					true,
					false,
					null);
			
			getRecord().copyFields(trans);
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		if (evt.getSource()==input) {

			if (input==null) {
				return;
			}
			if (!input.isReady()) {
				return;
			}
			if (input.isInvalid()||input.isLastSeverityInvalid()) {
				record.updateAlarm(Severity.INVALID_ALARM,Status.LINK_ALARM,true);
				return;
			}
			ValueHolder[] vh= input.consume();
			
			if (vh.length!=1 || vh[0]==null) {
				return;
			}
			
			double d= transform.transformX(vh[0].doubleValue());
			_setValue(d,vh[0].severity,vh[0].status, true);
		}
		
	}

}
