/**
 * 
 */
package org.scictrl.csshell.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.scictrl.csshell.epics.server.PersistencyStore;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;

/**
 * <p>PersistencyStoreTest class.</p>
 *
 * @author igor@scictrl.com
 */
public class PersistencyStoreTest {
	
	/**
	 * <p>testEquals.</p>
	 *
	 * @param exp an array of {@link double} objects
	 * @param act an array of {@link double} objects
	 */
	public static void testEquals(double[] exp, double[] act) {
		assertNotNull(exp);
		assertNotNull(act);
		Assert.assertEquals(exp.length, act.length);

		for (int i = 0; i < act.length; i++) {
			Assert.assertEquals(exp[i],act[i],0.000001);
		}
	}

	/**
	 * Test.
	 */
	@Test
	public void testStore( ) {
		
		String fileName= "./src/test/config/AppServer/persistancyTest.xml";
		
		File file= new File(fileName); 
		
		if (file.exists()) {
			file.delete();
		}
		
		Record recD= new Record("TEST:01:D", DBRType.DOUBLE, 1);
		Record recDd= new Record("TEST:01:Dd", DBRType.DOUBLE, 10);
		Record recS= new Record("TEST:01:S", DBRType.STRING, 1);
		Record recC= new Record("TEST:01:C", DBRType.BYTE, 16);
		
		recD.setProcessor(new MemoryValueProcessor());
		recDd.setProcessor(new MemoryValueProcessor());
		recS.setProcessor(new MemoryValueProcessor());
		recC.setProcessor(new MemoryValueProcessor());
		
		HierarchicalConfiguration hc= new HierarchicalConfiguration();
		
		recD.getProcessor().configure(recD, hc);
		recDd.getProcessor().configure(recDd, hc);
		recS.getProcessor().configure(recS, hc);
		recC.getProcessor().configure(recC, hc);

		Object valD=recD.getValue();
		Object valDd=recD.getValue();
		Object valS=recS.getValueAsString();
		Object valC=recC.getValueAsString();
		
		//System.out.println(recD.getValue());
		//System.out.println(recDd.getValue());
		//System.out.println(recS.getValue());
		//System.out.println(recC.getValue());
		
		try {
			PersistencyStore store= new PersistencyStore(file, null);
		
			// Test with initial zeros
			
			store.registerValue(recD);
			store.registerValue(recDd);
			store.registerValue(recS);
			store.registerValue(recC);
			
			store.saveAll();
			
			store.deregister(recD);
			store.deregister(recDd);
			store.deregister(recS);
			store.deregister(recC);
			
			store.registerValue(recD);
			store.registerValue(recDd);
			store.registerValue(recS);
			store.registerValue(recC);
			
			assertEquals(valD, recD.getValue());
			testEquals((double[])valDd, (double[])recDd.getValue());
			assertEquals(valS, recS.getValueAsString());
			assertEquals(valC, recC.getValueAsString());
			
			store.deregister(recD);
			store.deregister(recDd);
			store.deregister(recS);
			store.deregister(recC);
			
			// test with selected value
			
			valD= 1.2345678;
			valDd= new double[]{1.2345678,1.2345678,1.2345678,1.2345678,1.2345678,1.2345678,1.2345678,1.2345678,1.2345678,1.2345678};
			valS="A>B/C:D,E;F";
			valC="A>B/C:D,E;F";
			
			recD.setValue(valDd);
			recDd.setValue(valDd);
			recS.setValue(valS);
			recC.setValue(valC);

			store.registerValue(recD);
			store.registerValue(recDd);
			store.registerValue(recS);
			store.registerValue(recC);
			
			store.saveAll();
			
			store.deregister(recD);
			store.deregister(recDd);
			store.deregister(recS);
			store.deregister(recC);
			
			store.registerValue(recD);
			store.registerValue(recDd);
			store.registerValue(recS);
			store.registerValue(recC);
			
			assertEquals(valD, recD.getValueAsDouble());
			testEquals((double[])valDd, (double[])recDd.getValue());
			assertEquals(valS, recS.getValueAsString());
			assertEquals(valS, recS.getValueAsString());

		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
	
	/**
	 * Test.
	 */
	@Test
	public void testRead( ) {
		
		String fileName= "./src/test/config/AppServer/persistancyTest1.xml";
		
		File file= new File(fileName);
		
		String[] names={"F:RF:LLRF:01:GunWG:Reflected:Power:Out","F:RF:LLRF:01:VM:Power:Out:Smpl"};
		
		
		try {
			PersistencyStore store= new PersistencyStore(file, null);
		
			for (String prop : names) {
				
				Record recD= new Record(prop, DBRType.DOUBLE, 1000);
				recD.setProcessor(new MemoryValueProcessor());
				HierarchicalConfiguration hc= new HierarchicalConfiguration();
				recD.getProcessor().configure(recD, hc);
				
				//System.out.println(recD.getValue());
				
				store.registerValue(recD);
				//System.out.println(recD.getValue());
			}

		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}
}
