package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Application;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.application.FeedbackLoopApplication;
import org.scictrl.csshell.epics.server.processor.LinkedValueProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

/**
 * <p>FeedbackLoopApplicationTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class FeedbackLoopApplicationTest extends AbstractConfiguredServerTest {
	
	class Collector implements PropertyChangeListener {
		public List<PropertyChangeEvent> events= new ArrayList<PropertyChangeEvent>();
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			events.add(evt);
			//System.out.println("Event "+count()+" "+evt.getPropertyName()+" "+evt.getNewValue());
		}
		public int count() {
			return events.size();
		}
		public PropertyChangeEvent last() {
			return events.get(events.size()-1);
		}
		public void clear() {
			events.clear();
		}
	}

	/**
	 * <p>Constructor for FeedbackLoopApplicationTest.</p>
	 */
	public FeedbackLoopApplicationTest() {
		pvCount+=10;
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testTimeWindow() {

		// get the records
		Record input= server.getDatabase().getRecord("A:TEST:01:Input");
		Record output= server.getDatabase().getRecord("A:TEST:01:Output");
		Record outputSet= server.getDatabase().getRecord("A:TEST:FL:OutputSet");
		Record inSync= server.getDatabase().getRecord("A:TEST:FL:InSync");
		
		//get the application
		FeedbackLoopApplication app= (FeedbackLoopApplication) inSync.getApplication();
		
		assertEquals(1,  server.getDatabase().applicationCount());

		String[] an= server.getDatabase().getApplicationNames();
		
		assertNotNull(an);
		assertEquals(1, an.length);
		assertEquals(app.getName(), an[0]);
		
		Application a= server.getDatabase().getApplication(an[0]);
		
		assertNotNull(a);
		assertEquals(app.getName(), a.getName());
		assertEquals(app, a);
		
		
		// initial values
		input.setValue(0.0);
		output.setValue(0.0);
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// start collecting events
		Collector inSyncCol= new Collector();
		inSync.addPropertyChangeListener(Record.PROPERTY_VALUE,inSyncCol);

		// initial values check
		assertEquals(0.0, input.getValueAsDouble(),0.001);
		assertEquals(0.0, output.getValueAsDouble(),0.001);
		assertEquals(1.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(0, inSync.getValueAsInt());
		
		// enable the feedback loop
		assertEquals(false, server.getDatabase().getRecord("A:TEST:FL:Enabled").getValueAsBoolean());
		server.getDatabase().getRecord("A:TEST:FL:Enabled").setValue(1);
		assertEquals(1, inSyncCol.count());
		
		// check if enabled is OK
		assertEquals(true, server.getDatabase().getRecord("A:TEST:FL:Enabled").getValueAsBoolean());
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		
		// set output back to 0
		output.setValue(0.0);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(1, inSyncCol.count());

		// feedback loop should kick-in and correct again
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// we make output not setable, so feedback will fail
		((LinkedValueProcessor)app.getRecord(FeedbackLoopApplication.OUTPUT).getProcessor()).setFixed(true);
		
		// set input above breakpoint
		input.setValue(2.0);
		assertEquals(2.0, input.getValueAsDouble(),0.001);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// output request has been updated
		assertEquals(2.0, outputSet.getValueAsDouble(),0.001);

		// but value has not changed
		assertEquals(2.0, output.getValueAsDouble(),0.001);
		
		// within 3 seconds, sync should not change
		assertEquals(1, inSync.getValueAsInt());
		assertEquals(1, inSyncCol.count());
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// after the time window sync should be updated
		assertEquals(0, inSync.getValueAsInt());
		assertTrue(inSyncCol.count()>1);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testFeedback() {

		// get the records
		Record input= server.getDatabase().getRecord("A:TEST:01:Input");
		Record output= server.getDatabase().getRecord("A:TEST:01:Output");
		Record outputSet= server.getDatabase().getRecord("A:TEST:FL:OutputSet");
		Record inSync= server.getDatabase().getRecord("A:TEST:FL:InSync");
		
		//get the application
		FeedbackLoopApplication app= (FeedbackLoopApplication) inSync.getApplication();
		
		// initial values
		input.setValue(0.0);
		output.setValue(0.0);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// start collecting events
		Collector inSyncCol= new Collector();
		inSync.addPropertyChangeListener(Record.PROPERTY_VALUE,inSyncCol);

		// initial values check
		assertEquals(0.0, input.getValueAsDouble(),0.001);
		assertEquals(0.0, output.getValueAsDouble(),0.001);
		assertEquals(1.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(0, inSync.getValueAsInt());
		
		// enable the feedback loop
		assertEquals(false, app.getRecord(FeedbackLoopApplication.ENABLED).getValueAsBoolean());
		server.getDatabase().getRecord("A:TEST:FL:Enabled").setValue(1);
		assertEquals(1, inSyncCol.count());
		
		// check if enabled is OK
		assertEquals(true, app.getRecord(FeedbackLoopApplication.ENABLED).getValueAsBoolean());
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		
		// set input above breakpoint
		input.setValue(2.0);
		assertEquals(2.0, input.getValueAsDouble(),0.001);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// output request has been updated
		assertEquals(2.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(2.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		assertEquals(1, inSyncCol.count());

		// set input below breakpoint
		input.setValue(0.0);
		assertEquals(0.0, input.getValueAsDouble(),0.001);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// output request has been updated
		assertEquals(1.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		assertEquals(1, inSyncCol.count());
		
		// disable the feedback loop
		app.getRecord(FeedbackLoopApplication.ENABLED).setValue(0);
		
		// check if disable is OK
		assertEquals(false, app.getRecord(FeedbackLoopApplication.ENABLED).getValueAsBoolean());
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		assertEquals(1, inSyncCol.count());
		
		// set input above breakpoint
		input.setValue(2.0);
		assertEquals(2.0, input.getValueAsDouble(),0.001);
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// output request has been updated
		assertEquals(2.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(0, inSync.getValueAsInt());
		assertEquals(2, inSyncCol.count());

		// set input below breakpoint
		input.setValue(0.0);
		assertEquals(0.0, input.getValueAsDouble(),0.001);
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// output request has been updated
		assertEquals(1.0, outputSet.getValueAsDouble(),0.001);
		assertEquals(1.0, output.getValueAsDouble(),0.001);
		assertEquals(1, inSync.getValueAsInt());
		assertEquals(3, inSyncCol.count());

	}
}
