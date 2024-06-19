/**
 * 
 */
package org.scictrl.csshell.server.processor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.TimeValueProcessor;
import org.scictrl.csshell.server.test.AbstractConfiguredServerTest;

/**
 * <p>TimeValueProcessorTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class TimeValueProcessorTest extends AbstractConfiguredServerTest {

	/**
	 * Constructor.
	 */
	public TimeValueProcessorTest() {
		pvCount+=5;
	}

	/**
	 * Test.
	 */
	@Test
	public void test() {
		
		Record recClk= server.getDatabase().getRecord("A:TEST:01:Clock");
		Record recUnix= server.getDatabase().getRecord("A:TEST:01:Clock:Unix");
		Record recISO= server.getDatabase().getRecord("A:TEST:01:Clock:ISO");
		Record recDT= server.getDatabase().getRecord("A:TEST:01:Clock:DateTime");
		Record recH= server.getDatabase().getRecord("A:TEST:01:Clock:Hours");

		TimeValueProcessor procUnix= (TimeValueProcessor) recUnix.getProcessor();
		TimeValueProcessor procClk= (TimeValueProcessor) recClk.getProcessor();
		TimeValueProcessor procISO= (TimeValueProcessor) recISO.getProcessor();
		TimeValueProcessor procDT= (TimeValueProcessor) recDT.getProcessor();
		TimeValueProcessor procH= (TimeValueProcessor) recH.getProcessor();


		long t=0;
		Object r= procUnix.convert(t);
		assertNotNull(r);
		assertEquals(int[].class, r.getClass());
		assertEquals(1, ((int[])r).length);
		assertEquals(0, ((int[])r)[0]);
		
		t=System.currentTimeMillis();
		r= procUnix.convert(t);
		assertNotNull(r);
		assertEquals(int[].class, r.getClass());
		assertEquals(1, ((int[])r).length);
		assertEquals(t/1000, ((int[])r)[0]);
		
		t=0;
		r= procClk.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("1970-01-01T01:00:00.000+0100", ((String[])r)[0]);

		t=System.currentTimeMillis();
		r= procClk.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date(t)), ((String[])r)[0]);
		
		t=0;
		r= procISO.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("1970-01-01T01:00:00.000+0100", ((String[])r)[0]);

		t=System.currentTimeMillis();
		r= procISO.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date(t)), ((String[])r)[0]);

		t=0;
		r= procDT.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("1970-01-01 01:00:00", ((String[])r)[0]);

		t=System.currentTimeMillis();
		r= procDT.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(t)), ((String[])r)[0]);
		
		t=0;
		r= procH.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("0:00:00", ((String[])r)[0]);

		t=((99*60+59)*60+59)*1000+999;
		r= procH.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("99:59:59", ((String[])r)[0]);

		t=((0*60+1)*60+1)*1000+1;
		r= procH.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("0:01:01", ((String[])r)[0]);

		t=((1*60+1)*60+1)*1000+1;
		r= procH.convert(t);
		assertNotNull(r);
		assertEquals(String[].class, r.getClass());
		assertEquals(1, ((String[])r).length);
		assertEquals("1:01:01", ((String[])r)[0]);
	}

}
