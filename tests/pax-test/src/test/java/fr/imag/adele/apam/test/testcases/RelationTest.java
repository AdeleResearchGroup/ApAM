package fr.imag.adele.apam.test.testcases;

import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter01;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter02;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter03;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter04;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter05;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class RelationTest extends ExtensionAbstract {

	@Test
	public void RelationSourceImplementationTargetImplementation_tc097() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-01");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter01 dependency = (S07ImplementationImporter01) instance
				.getServiceObject();

		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
		
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

		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
		
		Assert.assertTrue(
				String.format(messageTemplate,"Implementation","Instance",instance.getServiceObject()),
				instanceInjectedReference.getProperty("instance-property") != null && 
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

		Assert.assertTrue(
				"Declaring a relation information on the apam specification tag, and just using the id in the apam implementation didnt work, so the dependency was not resolved",
				dependency.getInjected() != null);
	}

	@Test
	public void RelationSourceSpecificationTargetImplementation_tc101() {

		Implementation implementation = CST.apamResolver.findImplByName(null,
				"S07-implementation-05");

		Instance instance = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		S07ImplementationImporter05 dependency = (S07ImplementationImporter05) instance
				.getServiceObject();

		Instance instanceInjectedReference = auxListInstanceReferencedBy(dependency
				.getInjected());

		auxListProperties(implementation);
		auxListInstances();
		
		String messageTemplate="Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";
		
		Assert.assertTrue(
				String.format(messageTemplate,"Implementation","Instance",instance.getServiceObject()),
				instanceInjectedReference.getProperty("instance-property") != null && 
				instanceInjectedReference.getProperty("implementation-property") != null &&
				instanceInjectedReference.getProperty("specification-property") != null);
	}
	
}
