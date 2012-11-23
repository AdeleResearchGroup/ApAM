package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
import fr.imag.adele.apam.pax.test.device.DeadsManSwitch;
import fr.imag.adele.apam.pax.test.impl.S2InnerImpl;
import fr.imag.adele.apam.pax.test.impl.S2MiddleImpl;
import fr.imag.adele.apam.pax.test.impl.S2OutterImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupAImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupBImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupCImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupDImpl;
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
	public void CompositeWithEagerDependency_05() {
		CompositeType ct1 = (CompositeType) CST.apamResolver.findImplByName(
				null, "S2Impl-composite-eager");
		
		String message="During this test, we enforce the resolution of the dependency by signaling dependency as eager='true'. %s";
		
		Assert.assertTrue(String.format(message, "Although, the test failed to retrieve the composite"),ct1!=null);
		
		auxListInstances("instances existing before the test-");
		
		Instance instance=ct1.createInstance(null, new HashMap<String, String>());
		
		Assert.assertTrue(String.format(message, "Although, the test failed to instantiate the composite"),instance!=null);
		
		//Force injection (for debuggin purposes)
		//S2Impl im=(S2Impl)instance.getServiceObject();
		//im.getDeadMansSwitch();
		
		List<Instance> pool=auxLookForInstanceOf(DeadsManSwitch.class.getCanonicalName());
		
		auxListInstances("intances existing after the test-");

		Assert.assertTrue(String.format(message, "Although, there exist no instance of dependence required(DeadsManSwitch.class), which means that it was not injected."),pool.size()==1);
		
	}
	
	@Test
	public void ComponentMngtLocalWithInstance() {

		final String messageTemplate = "Two composites A and B, each of them have their own mainimpl as IA and IB. " +
				"Both IA and IB have an attribute that depends on the specification X. " +
				"If an X instance is created into A and this instance is marked as local, this instance cannot be used by other composite. %s";
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");

		Composite composite_a = (Composite) cta.createInstance(null, null);
		Composite composite_b = (Composite) ctb.createInstance(null, null);

		Instance a = composite_a.getMainInst();

		Instance b = composite_b.getMainInst();

		S3GroupAImpl ga = (S3GroupAImpl) a.getServiceObject();

		S3GroupBImpl gb = (S3GroupBImpl) b.getServiceObject();

		//Force instantiation one given specification inside the composite A
		ga.getElement();

		//Force instantiation of the same specification as before in composite B
		gb.getElement();

		auxListInstances("---");
		
		String message=String.format(messageTemplate, "But A marked with '<local instance='true'>' allowed its instance to be used by another composite");
		
		Assert.assertTrue(message,ga.getElement() != gb.getElement());

	}
	
	@Test
	public void ComponentMngtLocalWithImplementation() {

		final String messageTemplate = "Two composites A and B, each of them have their own mainimpl as IA and IB. " +
				"Both IA and IB have an attribute that depends on the specification X. " +
				"If an X instance is created into A and this instance is marked as local, this instance cannot be used by other composite. %s";
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-local-implementation");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");

		Composite composite_a = (Composite) cta.createInstance(null, null);
		Composite composite_b = (Composite) ctb.createInstance(null, null);

		Instance a = composite_a.getMainInst();

		Instance b = composite_b.getMainInst();

		S3GroupAImpl ga = (S3GroupAImpl) a.getServiceObject();

		S3GroupBImpl gb = (S3GroupBImpl) b.getServiceObject();

		//Force instantiation one given specification inside the composite A
		ga.getElement();

		//Force instantiation of the same specification as before in composite B
		gb.getElement();

		auxListInstances("---");
		
		String message=String.format(messageTemplate, "But A marked with '<local implementation='true'>' allowed its instance to be used by another composite");
		
		Assert.assertTrue(message,ga.getElement() != gb.getElement());

	}

	@Test
	public void CompositeContentMngtBorrowNothingInstance() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-borrow-nothing-instance");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");

		Composite composite_a = (Composite) cta.createInstance(null, null);
		Composite composite_b = (Composite) ctb.createInstance(null, null);

		Instance a = composite_a.getMainInst();

		Instance b = composite_b.getMainInst();

		S3GroupAImpl ga = (S3GroupAImpl) a.getServiceObject();

		S3GroupBImpl gb = (S3GroupBImpl) b.getServiceObject();

		gb.getElement();
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String messageTemplate = "Composite that do not allow anything to be borrowed (<borrow instance='false' />) never should borrow other composite instance. %s";

		String message=String.format(messageTemplate, "Although, an instance from composite B was injected in composite A even if A is marked with borrow instance='false'");
		
		Assert.assertTrue(message,ga.getElement() != gb.getElement());

	}
	
	@Test
	public void CompositeContentMngtBorrowNothingImplementation() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-borrow-nothing-implementation");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");

		Composite composite_a = (Composite) cta.createInstance(null, null);
		Composite composite_b = (Composite) ctb.createInstance(null, null);

		Instance a = composite_a.getMainInst();

		Instance b = composite_b.getMainInst();

		S3GroupAImpl ga = (S3GroupAImpl) a.getServiceObject();

		S3GroupBImpl gb = (S3GroupBImpl) b.getServiceObject();

		gb.getElement();
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String messageTemplate = "Composite that do not allow anything to be borrowed (<borrow instance='false' />) never should borrow other composite instance. %s";

		String message=String.format(messageTemplate, "Although, an instance from composite B was injected in composite A even if A is marked with borrow instance='false'");
		
		Assert.assertTrue(message,ga.getElement() != gb.getElement());

	}
	
}
