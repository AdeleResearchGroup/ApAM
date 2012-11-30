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
import fr.imag.adele.apam.pax.test.impl.S2InnerImpl;
import fr.imag.adele.apam.pax.test.impl.S2MiddleImpl;
import fr.imag.adele.apam.pax.test.impl.S2OutterImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupAImpl;
import fr.imag.adele.apam.pax.test.impl.S3GroupBImpl;
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
	public void ComponentMngtLocalWithInstance() {

		final String messageTemplate = "Two composites A and B, each of them have their own mainimpl as IA and IB. " +
				"Both IA and IB have an attribute that depends on the specification X. " +
				"If an X instance is created into A and this instance is marked as local, this instance cannot be used by other composite. %s";
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-local-instance");

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

	@Test
	public void CompositeContentMngtFriendNothingImplementation() {

		CompositeType ctroot = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-friend-nothing-implementation");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_root = (Composite) ctroot.createInstance(null, null);
		Composite composite_a = (Composite) cta.createInstance(composite_root, null);
		Composite composite_b = (Composite) ctb.createInstance(composite_root, null);

		Instance instanceFriend1=ia.createInstance(composite_a, null);
		
		Instance instanceFriend2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceFriend1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceFriend2.getServiceObject();

		ga1.getElement();
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String messageTemplate = "An instance belonging to a composite that do not export any implementation to friends (<friend implementation=\"false\" />), should not be visible (used/injected) by another composite, even if they are friends. %s";

		String message=String.format(messageTemplate, "Although the instance was injeted externally into their friends.");
		
		Assert.assertTrue(message,ga1.getElement() != ga2.getElement());

	}
	
	@Test
	public void CompositeContentMngtFriendNothingInstance() {

		CompositeType ctroot = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-friend-nothing-instance");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_root = (Composite) ctroot.createInstance(null, null);
		Composite composite_a = (Composite) cta.createInstance(composite_root, null);
		Composite composite_b = (Composite) ctb.createInstance(composite_root, null);

		Instance instanceFriend1=ia.createInstance(composite_a, null);
		
		Instance instanceFriend2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceFriend1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceFriend2.getServiceObject();

		ga1.getElement();
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String messageTemplate = "An instance belonging to a composite that do not export any instance to friends (<friend instance=\"false\" />), should not be visible (used/injected) by another composite, even if they are friends. %s";

		String message=String.format(messageTemplate, "Although the instance was injeted externally into their friends.");
		
		Assert.assertTrue(message,ga1.getElement() != ga2.getElement());

	}	
	
	@Test
	public void CompositeContentMngtApplicationNothingInstance() {
		
		CompositeType appCompositeType = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		Composite appComposite=(Composite)appCompositeType.createInstance(null, null);
		
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-application-nothing-instance");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_a = (Composite) cta.createInstance(appComposite, null);
		Composite composite_b = (Composite) ctb.createInstance(appComposite, null);

		Instance instanceApp1=ia.createInstance(composite_a, null);
		
		Instance instanceApp2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceApp2.getServiceObject();

		ga1.getElement();
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String message= "A composite that share its dependencies into application level, should be allow to inject its instances into other composites that are into the same application.";
		
		Assert.assertTrue(message,ga1.getElement() == ga2.getElement());

	}
	
}
