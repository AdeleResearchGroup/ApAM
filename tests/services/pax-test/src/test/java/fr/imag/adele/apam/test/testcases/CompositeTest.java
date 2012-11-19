package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.pax.test.impl.device.GenericSwitch;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends ExtensionAbstract {

	@Test
	public void CompositeTypeInstantiation() {

		CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-composite");

		Assert.assertTrue("Failed to create the instance of CompositeType",
				ct != null);

		Instance instApp = ct.createInstance(null,
				new HashMap<String, String>());

		Assert.assertTrue("Failed to create the instance of CompositeType",
				instApp != null);
	}

	@Test
	public void CompositeTypeRetrieveServiceObject() {

		CompositeType composite = CST.apam.createCompositeType(null,
				"eletronic-device-compotype", null, "eletronic-device",
				new HashSet<ManagerModel>(), new HashMap<String, String>());

		Assert.assertTrue(
				"Should be possible to create a composite through API using createCompositeType method",
				composite != null);

		Instance instance = composite.createInstance(null, null);

		Assert.assertTrue("Failed to create instance of the compotype",
				instance != null);

		GenericSwitch serviceObject = (GenericSwitch) instance
				.getServiceObject();

		Assert.assertTrue("Failed to retrieve service as object ",
				serviceObject != null);

	}

	@Test
	public void FetchImplThatHasComposite() {

		CompositeType ct1 = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-composite-1");
		
		apam.waitForIt(2000);
		
		CompositeType ct2 = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-composite-2");

		String general="From two composites based on the same impl, both should be fetchable/instantiable from apam. %s";
		
		Assert.assertTrue(String.format(general,"The first one failed to be fetched."),ct1 != null);
		Assert.assertTrue(String.format(general,"The second one failed to be fetched."),ct2 != null);

		Instance ip1 = ct1.createInstance(null, new HashMap<String, String>());
		Instance ip2 = ct2.createInstance(null, new HashMap<String, String>());

		Assert.assertTrue(String.format(general,"The first one failed to instantiate."),ip1 != null);
		Assert.assertTrue(String.format(general,"The second one failed to instantiate."),ip2 != null);
		
		System.err.println("-------------");
		auxListInstances("\t");

	}

}
