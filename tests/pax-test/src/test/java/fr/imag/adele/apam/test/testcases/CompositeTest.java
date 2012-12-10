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
import fr.imag.adele.apam.pax.test.impl.deviceSwitch.GenericSwitch;
import fr.imag.adele.apam.pax.test.implS2.S2InnerImpl;
import fr.imag.adele.apam.pax.test.implS2.S2MiddleImpl;
import fr.imag.adele.apam.pax.test.implS2.S2OutterImpl;
import fr.imag.adele.apam.pax.test.implS3.S3GroupAImpl;
import fr.imag.adele.apam.pax.test.implS3.S3GroupBImpl;
import fr.imag.adele.apam.tests.helpers.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class CompositeTest extends ExtensionAbstract {

	@Test
	public void CompositeTypeInstantiation_tc028() {

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
	public void FetchImplThatHasComposite_tc029() {

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
	public void CompositeTypeRetrieveServiceObject_tc030() {

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
	public void CascadeDependencyInstantiation_tc031(){
		
		Implementation ct1 = (Implementation) CST.apamResolver.findImplByName(
				null, "fr.imag.adele.apam.pax.test.implS2.S2InnerImpl");
		
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
	public void ComponentMngtLocalWithInstance_tc032() {

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
	public void ComponentMngtLocalWithImplementation_tc033() {

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
		
		String message=String.format(messageTemplate, "But A marked with <export implementation=false />" +
				" allowed its instance to be used by another composite");
//Normal !		
		Assert.assertTrue(message,ga.getElement() == gb.getElement());

	}

	@Test
	public void CompositeContentMngtImportNothingInstance_tc034() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-import-nothing-instance");

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
		
		String messageTemplate = "Composite that do not allow anything to be imported (<import instance='false' />) should never use other composite instance. %s";

		String message=String.format(messageTemplate, "Although, an instance from composite B was injected in composite A even if A is marked with import instance='false'");
		
		Assert.assertTrue(message,ga.getElement() != gb.getElement());

	}
	
	@Test
	public void CompositeContentMngtImportNothingImplementation_tc035() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-import-nothing-implementation");

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
		
		String messageTemplate = "Composite that do not allow anything to be imported (<import implementation='false' />) should never import other composite instance. %s";

		String message=String.format(messageTemplate, "Although, an instance from composite B was injected in composite A even if A is marked with <import implementation='false' />");
		//The fact the implem is not visible does not mean we cannot resolve : it can be deployed again, 
		//and it is possible to see its instances anyway !.
		Assert.assertTrue(message,ga.getElement() == gb.getElement());

	}
	
	@Test
	public void CompositeContentMngtExportApplicationEverythingGlobalNothingInstance_tc038() {
		
		CompositeType appCompositeType = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		Composite appComposite=(Composite)appCompositeType.createInstance(null, null);
		
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-export-application-everything-global-nothing");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_a = (Composite) cta.createInstance(appComposite, null);
		Composite composite_b = (Composite) ctb.createInstance(appComposite, null);

		Instance instanceApp1=ia.createInstance(composite_a, null);
		
		Instance instanceApp2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceApp2.getServiceObject();

		System.out.println("instanceApp1 is in appli " + instanceApp1.getAppliComposite()) ;
		System.out.println("instanceApp2 is in appli " + instanceApp2.getAppliComposite()) ;
		
		ga1.getElement();
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String message= "A composite that share its dependencies into application level, should be allowED to inject its instances into other composites that are into the same application.";
//They are not in the same application !		
		Assert.assertTrue(message,ga1.getElement() == ga2.getElement());

	}
	
	@Test
	public void CompositeContentMngtExportApplicationNothingGlobalEverythingInstance_tc048() {
		
		CompositeType appCompositeType = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		Composite superparent=(Composite)appCompositeType.createInstance(null, null);
		
		Composite appCompositeA=(Composite)appCompositeType.createInstance(superparent, null);
		
		CompositeType appCompositeTypeC = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-c");
		
		Composite appCompositeC=(Composite)appCompositeTypeC.createInstance(superparent, null);
		
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-export-application-nothing-global-everything");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_a = (Composite) cta.createInstance(appCompositeA, null);
		Composite composite_b = (Composite) ctb.createInstance(appCompositeC, null);

		Instance instanceApp1=ia.createInstance(composite_a, null);
		
		Instance instanceApp2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceApp2.getServiceObject();

		ga1.getElement();
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String message= "Consider composite A, instantiated into a composite SA, and B, into a composite SB. If A declares that export nothing to all app but everything to global, the global take over, and the instances of A should be visible in B";
	// Test faux ?? : ce sont deux appli differentes	
		Assert.assertTrue(message,ga1.getElement() == ga2.getElement());

	}
	
	@Test
	public void CompositeContentMngtExportApplicationNothingGlobalEverythingInstance_tc049() {
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-export-application-nothing-global-everything");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_a = (Composite) cta.createInstance(null, null);
		Composite composite_b = (Composite) ctb.createInstance(null, null);

		Instance instanceApp1=ia.createInstance(composite_a, null);
		
		Instance instanceApp2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceApp2.getServiceObject();

		ga1.getElement();
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String message= "Consider composite A, instantiated into the ROOT composite, and B, into the ROOT composite. If A declares that export nothing to all app but everything to global, the global take over, and the instances of A should be visible in B";
		
		Assert.assertTrue(message,ga1.getElement() == ga2.getElement());

	}	
	
	@Test
	public void CompositeContentMngtExportGlobalEverythingInstance_tc050() {
		
		CompositeType appCompositeType = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a");
		
		Composite appCompositeA=(Composite)appCompositeType.createInstance(null, null);
		
		CompositeType appCompositeTypeC = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-c");
		
		Composite appCompositeC=(Composite)appCompositeTypeC.createInstance(null, null);
		
		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-export-global-everything");

		CompositeType ctb = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-b");
		
		Implementation ia=CST.apamResolver.findImplByName(null, "group-a");
		
		Composite composite_a = (Composite) cta.createInstance(appCompositeA, null);
		Composite composite_b = (Composite) ctb.createInstance(appCompositeC, null);

		Instance instanceApp1=ia.createInstance(composite_a, null);
		
		Instance instanceApp2=ia.createInstance(composite_b, null);

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		S3GroupAImpl ga2 = (S3GroupAImpl) instanceApp2.getServiceObject();

		ga1.getElement();
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		auxListInstances("bbbbbbbbbbbbbbbbbbbbbbbb");
		
		ga2.getElement();

		auxListInstances("aaaaaaaaaaaaaaaaaaaaaaaa");
		
		String message= "Consider composite A, instantiated into a composite SA, and B, into a composite SB. If A declares that export everything globally, its instances should be visible/injected in B";
		
		Assert.assertTrue(message,ga1.getElement() == ga2.getElement());

	}	
	
}
