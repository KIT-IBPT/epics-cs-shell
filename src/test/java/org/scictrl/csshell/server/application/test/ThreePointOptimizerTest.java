package org.scictrl.csshell.server.application.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scictrl.csshell.epics.server.application.control.AbstractController;
import org.scictrl.csshell.epics.server.application.control.Optimizer.State;
import org.scictrl.csshell.epics.server.application.control.ProbePoint;

/**
 * Optimizer test
 *
 * @author igor@scictrl.com
 */
public class ThreePointOptimizerTest {

	/**
	 * Constructor.
	 */
	public ThreePointOptimizerTest() {
	}

	/**
	 * SetUp
	 *
	 * @throws java.lang.Exception failed
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * tearDown
	 *
	 * @throws java.lang.Exception failed
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test.
	 */
	@Test
	public void testIdeal() {
		
		AbstractController ac= new AbstractController() {
			
			@Override
			protected boolean takeMeasurements(ProbePoint[] points) {
				//System.out.println("Measure");
				for (ProbePoint p : points) {
					if (p.inp>10.3549 && p.inp<10.8611) {
						p.out=31.609*p.inp-335.3076;
						p.valid=true;
					} else {
						p.valid=false;
					}
					//System.out.println(p.toString());
				}
				return true;
			}
			
		};
		
		assertEquals(State.INITIAL,ac.getSate());
		
		double min= 9.0;
		double max= 12.0;
		double precision= 0.0001;
		
		ac.initialize(min, max, precision, precision);
		
		assertNull(ac.getBest());
		assertEquals(State.INITIAL,ac.getSate());
		
		ac.start();
		
		ProbePoint p= ac.getBest();
		
		assertNotNull(p);
		assertEquals(10.607978740, p.inp,precision);
		assertEquals(0.0, p.out,precision);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testIdealCached() {
		
		AbstractController ac= new AbstractController() {
			
			@Override
			protected boolean takeMeasurements(ProbePoint[] points) {
				//System.out.println("Measure");
				for (ProbePoint p : points) {
					if (p.inp>10.3549 && p.inp<10.8611) {
						p.out=31.609*p.inp-335.3076;
						p.valid=true;
					} else {
						p.valid=false;
					}
					//System.out.println(p.toString());
				}
				return true;
			}
			
		};
		
		assertEquals(State.INITIAL,ac.getSate());
		
		double min= 9.0;
		double max= 12.0;
		double precision= 0.0001;
		
		ac.setCacheMeasurements(true);
		ac.initialize(min, max, precision, precision);
		
		assertNull(ac.getBest());
		assertEquals(State.INITIAL,ac.getSate());
		
		ac.start();
		
		ProbePoint p= ac.getBest();
		
		assertNotNull(p);
		assertEquals(10.607978740, p.inp,precision);
		assertEquals(0.0, p.out,precision);
		
	}

	/**
	 * Test.
	 */
	@Test
	public void testFuzzy() {
		
		AbstractController ac= new AbstractController() {
			Random rand= new Random();
			@Override
			protected boolean takeMeasurements(ProbePoint[] points) {
				//System.out.println("Measure");
				for (ProbePoint p : points) {
					if (p.inp>10.3549 && p.inp<10.8611) {
						p.out=31.609*p.inp-335.3076;
						p.out=p.out+(rand.nextDouble()-0.5)*p.out*0.1;
						p.valid=true;
					} else {
						p.valid=false;
					}
					//System.out.println(p.toString());
				}
				return true;
			}
			
		};
		
		assertEquals(State.INITIAL,ac.getSate());
		
		double min= 9.0;
		double max= 12.0;
		double precision= 0.0001;
		
		ac.initialize(min, max, precision/10.0, precision/10.0);
		
		assertNull(ac.getBest());
		assertEquals(State.INITIAL,ac.getSate());
		
		ac.start();
		
		ProbePoint p= ac.getBest();
		
		assertNotNull(p);
		assertEquals(0.0, p.out,precision);
		assertEquals(10.607978740, p.inp,precision);
		
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testFuzzyCached() {
		
		AbstractController ac= new AbstractController() {
			Random rand= new Random();
			@Override
			protected boolean takeMeasurements(ProbePoint[] points) {
				//System.out.println("Measure");
				for (ProbePoint p : points) {
					if (p.inp>10.3549 && p.inp<10.8611) {
						p.out=31.609*p.inp-335.3076;
						p.out=p.out+(rand.nextDouble()-0.5)*p.out*0.1;
						p.valid=true;
					} else {
						p.valid=false;
					}
					//System.out.println(p.toString());
				}
				return true;
			}
			
		};
		
		assertEquals(State.INITIAL,ac.getSate());
		
		double min= 9.0;
		double max= 12.0;
		double precision= 0.0001;
		
		ac.setCacheMeasurements(true);
		ac.initialize(min, max, precision/10.0, precision/10.0);
		
		assertNull(ac.getBest());
		assertEquals(State.INITIAL,ac.getSate());
		
		ac.start();
		
		ProbePoint p= ac.getBest();
		
		assertNotNull(p);
		assertEquals(0.0, p.out,precision);
		assertEquals(10.607978740, p.inp,precision);
		
	}

}
