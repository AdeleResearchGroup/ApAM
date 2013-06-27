package fr.imag.adele.apam.test.testcases;

import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter01;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter02;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter03;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter04;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter05;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter06;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter07;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter08;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class RelationTest extends ExtensionAbstract {

	
	private static void AssertCorrectSourceTargetTypes(Component component, Class expectedSource,Class expectedTarget){
		
		ComponentImpl ci=null;
		
		if(component instanceof Implementation && expectedSource==Specification.class){
			ci=(ComponentImpl)((Implementation)component).getSpec();
		}
		
		for(Link link:ci.getLocalLinks()){
			
			validateSourceTargetTypes(link.getSource(),link.getDestination(),expectedSource,expectedTarget);
			
		}
		
	}
	
	private static void validateSourceTargetTypes(Component source,Component target,Class expectedSource,Class expectedTarget){
		
		//Source
		Assert.assertTrue(String.format("Source is not of the type %s",expectedSource.getSimpleName()),expectedSource.isInstance(source) );
		//Target
		Assert.assertTrue(String.format("Target is not of the type %s",expectedTarget.getSimpleName()),expectedTarget.isInstance(target) );
		
	}
	
	@Test
	public void RelationSourceImplementationTargetImplementation_tc097() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-01");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter01 dependency = (S07ImplementationImporter01) instance
				.getServiceObject();
		
		dependency.getInjected();
		
		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
		
		ComponentImpl ci=(ComponentImpl)implementation;
		
		Assert.assertTrue(String.format("Only one link should have been created, but %s links were found",ci.getLocalLinks().size()),ci.getLocalLinks().size()==1);
		
		for(Link link:ci.getLocalLinks()){
			
			validateSourceTargetTypes(link.getSource(),link.getDestination(),Implementation.class,Implementation.class);
			
		}
		
		Assert.assertTrue(
				String.format(messageTemplate,"Implementation","Implementation",instance.getServiceObject()),
				dependency.getInjected().getProperty("implementation-property") != null
						&& dependency.getInjected().getProperty(
								"instance-property") == null);
	}

	@Test
	public void RelationSourceImplementationTargetSpecification_tc098() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-02");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter02 dependency = (S07ImplementationImporter02) instance
				.getServiceObject();

		auxListProperties(dependency.getInjected());

		auxListInstances();

		ComponentImpl ci=(ComponentImpl)implementation;
		
		Assert.assertTrue(String.format("Only one link should have been created, but %s links were found",ci.getLocalLinks().size()),ci.getLocalLinks().size()==1);
		
		for(Link link:ci.getLocalLinks()){
			
			validateSourceTargetTypes(link.getSource(),link.getDestination(),Implementation.class,Specification.class);
			
		}
		
		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
				
		Assert.assertTrue(String.format(messageTemplate,"Implementation","Specification",instance.getServiceObject()),
						  dependency.getInjected().getProperty("instance-property") == null && 
						  dependency.getInjected().getProperty("implementation-property") == null && 
						  dependency.getInjected().getProperty("specification-property") != null);

	}

	@Test
	public void RelationSourceImplementationTargetInstance_tc099() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-03");
		
		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter03 dependency = (S07ImplementationImporter03) instance
				.getServiceObject();

		Instance instanceInjectedReference = auxListInstanceReferencedBy(dependency
				.getInjected());

		auxListProperties(instanceInjectedReference);

		auxListInstances();

		ComponentImpl ci=(ComponentImpl)implementation;
		
		Assert.assertTrue(String.format("Only one link should have been created, but %s links were found",ci.getLocalLinks().size()),ci.getLocalLinks().size()==1);
		
		for(Link link:ci.getLocalLinks()){
			
			validateSourceTargetTypes(link.getSource(),link.getDestination(),Implementation.class,Instance.class);
			
		}
		
		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
		
		Assert.assertTrue(
				String.format(messageTemplate,"Implementation","Instance",instance.getServiceObject()), 
				instanceInjectedReference.getProperty("implementation-property") != null &&
				instanceInjectedReference.getProperty("specification-property") != null);

	}

	@Test
	public void RelationSourceImplementationTargetImplementationOverride_tc100() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-04");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter04 dependency = (S07ImplementationImporter04) instance
				.getServiceObject();

		Assert.assertTrue("Declaring a relation information on the apam specification tag, and just using the id in the apam implementation didnt work, so the dependency was not resolved",dependency.getInjected()!=null);

		auxListProperties(dependency.getInjected());
		
		auxListInstances();
		
	}

	@Test
	public void RelationSourceImplementationTargetInstanceCreationEager_tc101() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-05");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter05 dependency = (S07ImplementationImporter05) instance
				.getServiceObject();

		auxListInstances();
		
//		Instance instanceInjectedReference = auxListInstanceReferencedBy(dependency
//				.getInjected());

		ComponentImpl ci=(ComponentImpl)implementation;
		
		Assert.assertTrue("Using an relation with creation='eager' should instantiate imediatly the dependency, which didnt happened",ci.getLocalLinks().size()>0);
		
	}
	
	@Test
	public void RelationSourceImplementationTargetInstanceCreationLazy_tc102() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-06");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter06 dependency = (S07ImplementationImporter06) instance
				.getServiceObject();

		ComponentImpl ci=(ComponentImpl)implementation;
		
		Assert.assertTrue("In relation, testing creation='lazy'. There should not exist a link before the dependency call",ci.getLocalLinks().size()==0);
		
		auxListInstances();
		
//		Instance instanceInjectedReference = auxListInstanceReferencedBy(dependency
//				.getInjected());
		
		//Force field injection
		dependency.getInjected();
		
		Assert.assertTrue("Using an relation with creation='lazy' should instantiate after the dependency is called, which didnt happened",ci.getLocalLinks().size()==1);
		
	}

	@Test
	public void RelationSourceSpecificationTargetInstance_tc103() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-07");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter07 dependency = (S07ImplementationImporter07) instance
				.getServiceObject();
		
		dependency.getInjected();
		
		ComponentImpl ci=(ComponentImpl)implementation.getSpec();
		
		Assert.assertTrue(String.format("One link should have been created, but %s links were found",ci.getLocalLinks().size()),ci.getLocalLinks().size()==1);
		
		AssertCorrectSourceTargetTypes(implementation,Specification.class,Instance.class);
		
	}
	
	@Test
	public void RelationSourceSpecificationTargetImplementation_tc104() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-08");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter08 dependency = (S07ImplementationImporter08) instance
				.getServiceObject();
		
		dependency.getInjected();
		
		ComponentImpl ci=(ComponentImpl)implementation.getSpec();
		
		Assert.assertTrue(String.format("One link should have been created, but %s links were found",ci.getLocalLinks().size()),ci.getLocalLinks().size()==1);
		
		AssertCorrectSourceTargetTypes(implementation,Specification.class,Implementation.class);
		
	}
	
}
