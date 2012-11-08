package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.pax.test.impl.device.GenericSwitch;
import fr.imag.adele.apam.test.support.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends ExtensionAbstract {

	@Test
	public void CompositeTypeInstantiation() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		CompositeType app = CST.apam.createCompositeType(null,
				"eletronic-device-compotype", null, "eletronic-device",
				new HashSet<ManagerModel>(), new HashMap<String, String>());

		Assert.assertTrue("Failed to create the CompositeType",
				app != null);

		Instance instApp = app
				.createInstance(null /* composite */, null/* properties */);

		Assert.assertTrue(
				"Failed to create the instance of CompositeType",
				instApp != null);
	}

	@Test
	public void CompositeTypeRetrieveServiceObject () {
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		CompositeType composite = CST.apam.createCompositeType(null,  "eletronic-device-compotype", null, "eletronic-device", null,null);
		Assert.assertTrue(composite!= null);
		
		Instance instance = composite.createInstance(null,null);
		
		Assert.assertTrue("Failed to create instance of the compotype",instance!= null);
		
		GenericSwitch serviceObject = (GenericSwitch) instance.getServiceObject();
		
		Assert.assertTrue("Failed to retrieve service as object ",serviceObject!= null);
		
//		Instance test00_instance0 = appliTest00.createInstance((Composite) a /* composite */, null/* properties */);
//		Assert.assertTrue(test00_instance0 != null);
//		Assert.assertTrue(((Composite)a).containsInst(test00_instance0)) ;
//		System.out.println("composite in composite same type !! " + test00_instance0 );
//		System.out.println("=========== passed nested composite instance by API\n\n");
		
	}
	
}
