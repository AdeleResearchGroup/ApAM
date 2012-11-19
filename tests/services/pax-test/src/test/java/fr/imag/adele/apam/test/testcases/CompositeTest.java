package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.pax.test.iface.S2;
import fr.imag.adele.apam.pax.test.impl.S2InnerImpl;
import fr.imag.adele.apam.pax.test.impl.S2MiddleImpl;
import fr.imag.adele.apam.pax.test.impl.S2OutterImpl;
import fr.imag.adele.apam.pax.test.impl.device.GenericSwitch;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends ExtensionAbstract {

	@Test
	public void CompositeTypeInstantiation_01() {

		CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-composite-1");

		Assert.assertTrue("Failed to create the instance of CompositeType",
				ct != null);

		Instance instApp = ct.createInstance(null,
				new HashMap<String, String>());

		Assert.assertTrue("Failed to create the instance of CompositeType",
				instApp != null);
	}
	
	@Test
	public void FetchImplThatHasComposite_02() {

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

	@Test
	public void CompositeTypeRetrieveServiceObject_03() {

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
	public void CascadeDependencyInstantiation_04(){
		
		Implementation ct1 = (Implementation) CST.apamResolver.findImplByName(
				null, "fr.imag.adele.apam.pax.test.impl.S2InnerImpl");
		
		Assert.assertTrue(ct1!=null);
		
		auxListInstances("before instantiation-");
		
		Instance instance=ct1.createInstance(null, new HashMap<String, String>());

		String messageDefault="Considering A->B meaning A depends on B. In the relation A-B->C, A is considered the inner most, and C the outter most. %s";
		
		try{
		
			//Means that the inner was injected 
			Assert.assertTrue(String.format(messageDefault, "The inner most instance was not created"),instance.getServiceObject()!=null);
			
			S2InnerImpl innerObject=(S2InnerImpl)instance.getServiceObject();
			
			Assert.assertTrue(String.format(messageDefault, "The middle instance was not created"),innerObject.getMiddle()!=null);
						
			S2MiddleImpl middleObject=(S2MiddleImpl)innerObject.getMiddle();
			
			Assert.assertTrue(String.format(messageDefault, "The outter most instance was not created"),middleObject.getOutter()!=null);
			
			S2OutterImpl outterObject=(S2OutterImpl)middleObject.getOutter();
			
			auxListInstances("after instantiation-");
			
		}catch(ClassCastException castException){
			Assert.fail("Enclosed implementation do not correspond to the right implementation. AImpl->BImpl->CImpl but the wrong implementation was injected");
		}
		
	}
	
	@Test
	@Ignore
	public void Metacomposite() {
		CompositeType ct1 = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-meta-composite");
		
		Assert.assertTrue(ct1!=null);
		
		auxListInstances("before-");
		
		Instance instance=ct1.createInstance(null, new HashMap<String, String>());
		
		//(S1Impl)instance.getServiceObject()
		
		auxListInstances("after-");

		Assert.assertTrue(instance!=null);
		
	}

}
