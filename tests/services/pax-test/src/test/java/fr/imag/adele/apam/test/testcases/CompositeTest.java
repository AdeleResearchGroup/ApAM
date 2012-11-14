package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.pax.test.impl.device.GenericSwitch;
import fr.imag.adele.apam.test.support.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends ExtensionAbstract {

	@Test
	public void CompositeTypeInstantiation() {
		
		CompositeType ct=(CompositeType)CST.apamResolver.findImplByName(null, "S1Impl-composite");
		
		Instance instApp = ct.createInstance(null, new HashMap<String, String>());

		Assert.assertTrue("Failed to create the instance of CompositeType",
				instApp != null);
	}

	@Test
	public void CompositeTypeRetrieveServiceObject() {
		
		CompositeType composite = CST.apam.createCompositeType("philipsSwitch",
				"eletronic-device-compotype", "", "eletronic-device",
				new HashSet<ManagerModel>(), new HashMap<String, String>());
		
		Assert.assertTrue(composite != null);

		Instance instance = composite.createInstance(null, null);

		Assert.assertTrue("Failed to create instance of the compotype",
				instance != null);

		GenericSwitch serviceObject = (GenericSwitch) instance
				.getServiceObject();

		Assert.assertTrue("Failed to retrieve service as object ",
				serviceObject != null);

	}

	@Test
	public void CompositeIsolatedTest() {
		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Composite c = (Composite) CST.apamResolver.findComponentByName(null,
				"S1Impl-composite");

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		// s1Impl.createInstance(c, new HashMap<String, String>());
		//
		// s1Impl.createInstance(instCompo, initialproperties)
	}

}
