package fr.imag.adele.apam.test.testcases;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.ResolutionException;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.pax.test.implS7.S07CustomException;
import fr.imag.adele.apam.pax.test.implS7.S07Implem14;
import fr.imag.adele.apam.pax.test.implS7.S07Implem16;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter01;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter02;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter03;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter04;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter06;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter07;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter08;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter09;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter10;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter11;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter12;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter13;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter15;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter17;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter18;
import fr.imag.adele.apam.pax.test.implS7.S07ImplementationImporter19;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class RelationTest extends ExtensionAbstract {

    @Inject
    Apam apamDep;

    @Override
    public List<Option> config() {
	Map<String, String> mapOfRequiredArtifacts= new HashMap<String, String>();
	mapOfRequiredArtifacts.put("apam-pax-samples-impl-s7", "fr.imag.adele.apam.tests.services");
	mapOfRequiredArtifacts.put("apam-pax-samples-spec-s7ext", "fr.imag.adele.apam.tests.services");
	
	List<Option> addon = super.config(mapOfRequiredArtifacts,false);
	return addon;
    }

    private static void AssertCorrectSourceTargetTypes(Component component,
	    Class expectedSource, Class expectedTarget) {

	ComponentImpl ci = null;

	if (component instanceof Implementation
		&& expectedSource == Specification.class) {
	    ci = (ComponentImpl) ((Implementation) component).getSpec();
	}
	if (component instanceof Instance && expectedSource == Instance.class) {
	    ci = (ComponentImpl) component;
	}

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    expectedSource, expectedTarget);

	}

    }


    private static void validateSourceTargetTypes(Component source,
	    Component target, Class expectedSource, Class expectedTarget) {

	// Source
	Assert.assertTrue(
		String.format("Source is not of the type %s",
			expectedSource.getSimpleName()),
		expectedSource.isInstance(source));
	// Target
	Assert.assertTrue(
		String.format("Target is not of the type %s",
			expectedTarget.getSimpleName()),
		expectedTarget.isInstance(target));

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

	String messageTemplate = "Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";

	ComponentImpl ci = (ComponentImpl) implementation;

	Assert.assertTrue(
		String.format(
			"Only one link should have been created, but %s links were found",
			ci.getLocalLinks().size()),
		ci.getLocalLinks().size() == 1);

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    Implementation.class, Implementation.class);

	}

	Assert.assertTrue(
		String.format(messageTemplate, "Implementation",
			"Implementation", instance.getServiceObject()),
		dependency.getInjected().getProperty("implementation-property") != null
			&& dependency.getInjected().getProperty(
				"instance-property") == null);
    }

    @Test
    public void RelationSourceImplementationTargetSpecification_tc098() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-02");

	Instance instance = implementation.createInstance(null, null);

	S07ImplementationImporter02 dependency = (S07ImplementationImporter02) instance
		.getServiceObject();

	auxListProperties(dependency.getInjected());

	auxListInstances();

	ComponentImpl ci = (ComponentImpl) implementation;

	Assert.assertTrue(
		String.format(
			"Only one link should have been created, but %s links were found",
			ci.getLocalLinks().size()),
		ci.getLocalLinks().size() == 1);

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    Implementation.class, Specification.class);

	}

	String messageTemplate = "Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";

	Assert.assertTrue(
		String.format(messageTemplate, "Implementation",
			"Specification", instance.getServiceObject()),
		dependency.getInjected().getProperty("instance-property") == null
			&& dependency.getInjected().getProperty(
				"implementation-property") == null
			&& dependency.getInjected().getProperty(
				"specification-property") != null);

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

	ComponentImpl ci = (ComponentImpl) implementation;

	Assert.assertTrue(
		String.format(
			"Only one link should have been created, but %s links were found",
			ci.getLocalLinks().size()),
		ci.getLocalLinks().size() == 1);

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    Implementation.class, Instance.class);

	}

	String messageTemplate = "Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";

	Assert.assertTrue(
		String.format(messageTemplate, "Implementation", "Instance",
			instance.getServiceObject()),
		instanceInjectedReference
			.getProperty("implementation-property") != null
			&& instanceInjectedReference
				.getProperty("specification-property") != null);

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
    public void RelationSourceImplementationTargetInstanceCreationEager_tc101() {

	// Now by default resolve = exists, creating dependency
	// target of the wire in implem-05 (it will exist so link can be
	// created)
	Implementation target = CST.apamResolver.findImplByName(null,
		"S07-implementation-04");
	target.createInstance(null, null);

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-05");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	auxListInstances();

	ComponentImpl ci = (ComponentImpl) implementation;

	Assert.assertTrue(
		"Using an relation with creation='eager' should instantiate imediatly the dependency, which didnt happened",
		ci.getLocalLinks().size() > 0);

    }

    @Test
    public void RelationSourceImplementationTargetInstanceCreationLazy_tc102() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-06");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter06 dependency = (S07ImplementationImporter06) instance
		.getServiceObject();

	ComponentImpl ci = (ComponentImpl) implementation;

	Assert.assertTrue(
		"In relation, testing creation='lazy'. There should not exist a link before the dependency call",
		ci.getLocalLinks().size() == 0);

	auxListInstances();

	// Instance instanceInjectedReference =
	// auxListInstanceReferencedBy(dependency
	// .getInjected());

	// Force field injection
	dependency.getInjected();

	Assert.assertTrue(
		"Using an relation with creation='lazy' should instantiate after the dependency is called, which didnt happened",
		ci.getLocalLinks().size() == 1);

    }

    @Test
    public void RelationSourceSpecificationTargetInstance_tc103() {

	Implementation target = CST.apamResolver.findImplByName(null,
		"S07-DependencyImpl");
	target.createInstance(null, null);

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-07");

	Instance instance = implementation.createInstance(null, null);

	S07ImplementationImporter07 dependency = (S07ImplementationImporter07) instance
		.getServiceObject();

	// Force field injection
	dependency.getInjected();

	ComponentImpl ci = (ComponentImpl) implementation.getSpec();

	Assert.assertTrue(String.format(
		"One link should have been created, but %s links were found",
		ci.getLocalLinks().size()), ci.getLocalLinks().size() == 1);

	AssertCorrectSourceTargetTypes(implementation, Specification.class,
		Instance.class);

    }

    @Test
    public void RelationSourceSpecificationTargetImplementation_tc104() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-08");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter08 dependency = (S07ImplementationImporter08) instance
		.getServiceObject();

	// Force field injection
	dependency.getInjected();

	ComponentImpl ci = (ComponentImpl) implementation.getSpec();

	Assert.assertTrue(String.format(
		"One link should have been created, but %s links were found",
		ci.getLocalLinks().size()), ci.getLocalLinks().size() == 1);

	AssertCorrectSourceTargetTypes(implementation, Specification.class,
		Implementation.class);

    }

    @Test
    public void RelationSourceSpecificationTargetSpecification_tc105() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-09");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter09 dependency = (S07ImplementationImporter09) instance
		.getServiceObject();

	// Force field injection
	dependency.getInjected();

	ComponentImpl ci = (ComponentImpl) implementation.getSpec();

	Assert.assertTrue(String.format(
		"One link should have been created, but %s links were found",
		ci.getLocalLinks().size()), ci.getLocalLinks().size() == 1);

	AssertCorrectSourceTargetTypes(implementation, Specification.class,
		Specification.class);

    }

    @Test
    public void RelationSourceSpecificationTargetImplementationFailWait_tc106() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-10");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter10 dependency = (S07ImplementationImporter10) instance
		.getServiceObject();

	ThreadWrapper wrapper = new ThreadWrapper(dependency);
	wrapper.setDaemon(true);
	wrapper.start();

	auxListInstances();

	apam.waitForIt(3000);

	Assert.assertTrue(
		"The dependency is the type of fail='wait', but the component was not put in wait state",
		wrapper.isAlive());

    }

    // Require by the test
    // RelationSourceSpecificationTargetImplementationFailWait_tc106
    class ThreadWrapper extends Thread {

	final S07ImplementationImporter10 group;

	public ThreadWrapper(S07ImplementationImporter10 group) {
	    this.group = group;
	}

	@Override
	public void run() {
	    System.out.println("Element injected:" + group.getInjected());
	}

    }

    @Test
    public void RelationSourceSpecificationTargetImplementationFailExceptionGeneric_tc107() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-11");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter11 dependency = (S07ImplementationImporter11) instance
		.getServiceObject();

	Exception raised = null;

	try {
	    // Force field injection
	    dependency.getInjected();
	} catch (Exception e) {
	    // If arrive at this block, the test has passed
	    raised = e;
	}

	Assert.assertTrue(
		"Using tag <relation/> with fail='exception' didnt not raise an exception as expected.",
		raised != null);

	String messageTemplate = "Using tag <relation/> with fail='exception' didnt not raise an exception of the type expected. Type raised was %s instead of %s";

	Assert.assertTrue(String.format(messageTemplate, raised.getClass()
		.getCanonicalName(), ResolutionException.class
		.getCanonicalName()), ResolutionException.class
		.isInstance(raised));

    }

    @Test
    public void RelationSourceSpecificationTargetImplementationFailExceptionCustom_tc108() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-12");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	S07ImplementationImporter12 dependency = (S07ImplementationImporter12) instance
		.getServiceObject();

	Exception raised = null;

	try {
	    // Force field injection
	    dependency.getInjected();
	} catch (Exception e) {
	    // If arrive at this block, the test has passed
	    raised = e;
	}

	Assert.assertTrue(
		"Using tag <relation/> with fail='exception' didnt not raise an exception as expected.",
		raised != null);

	String messageTemplate = "Using tag <relation/> with exception='%s' did not raise this specific exception. It raised %s instead";

	Assert.assertTrue(String.format(messageTemplate,
		S07CustomException.class.getCanonicalName(), raised.getClass()
			.getCanonicalName()), S07CustomException.class
		.isInstance(raised));

    }

    @Test
    public void RelationSourceInstanceTargetInstance_tc109() {
	
	Implementation target = CST.apamResolver.findImplByName(null,
		"S07-DependencyImpl");
	target.createInstance(null, null);	

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-13");

	Instance instance = implementation.createInstance(null,
		null);

	S07ImplementationImporter13 dependency = (S07ImplementationImporter13) instance
		.getServiceObject();

	// Force field injection
	dependency.getInjected();

	ComponentImpl ci = (ComponentImpl) instance;

	auxListInstances();

	Assert.assertTrue(String.format(
		"One link should have been created, but %s links were found",
		ci.getLocalLinks().size()), ci.getLocalLinks().size() == 1);

	AssertCorrectSourceTargetTypes(instance, Instance.class, Instance.class);

    }

    @Test
    public void RelationLinkResolveExist_tct001() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-14");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	org.junit.Assert
		.assertTrue(
			"An exception should be raised as the dependency cannot be resolved as no instance running",
			testResolutionExceptionCase14(instance, 2));
	Assert.assertEquals(
		"No relations should have been created (no instance of dependency existing)",
		0, instance.getRawLinks().size());

	Implementation implementationdep = CST.apamResolver.findImplByName(
		null, "S07-DependencyImpl-02");

	Instance instancedep = implementationdep.createInstance(null,
		Collections.<String, String> emptyMap());
	auxListInstances();

	org.junit.Assert
		.assertFalse(
			"No exception should be raised as the dependency can be resolved",
			testResolutionExceptionCase14(instance, 2));
	Assert.assertEquals("One relation should have been created", 1,
		instance.getRawLinks().size());

    }

    public static boolean testResolutionExceptionCase14(Instance inst,
	    int methodNumber) {
	// Force field injection (a bit akward with polymorphism)
	S07Implem14 implem = (S07Implem14) inst.getServiceObject();
	try {
	    switch (methodNumber) {
	    case 2:
		if (implem.getInjected02() == null)
		    return true;
		break;
	    case 3:
		if (implem.getInjected03() == null)
		    return true;
		break;
	    }

	} catch (ResolutionException exc) {
	    exc.printStackTrace();
	    return true;
	} catch (Exception exc) {
	    exc.printStackTrace();
	    return true;
	}
	return false;
    }

    @Test
    public void RelationLinkResolveInternal_tct002() {
	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-14bis");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	org.junit.Assert
		.assertFalse(
			"No exception should be raised as the dependency should be instanciated",
			testResolutionExceptionCase14(instance, 2));
	Assert.assertEquals("One relation should have been created", 1,
		instance.getRawLinks().size());

	Instance instance2 = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	Implementation implementationdep = CST.apamResolver.findImplByName(
		null, "S07-DependencyImpl-02");

	Assert.assertFalse(
		"No exception should be raised as the dependency is already instanciated",
		testResolutionExceptionCase14(instance2, 2));
	auxListInstances();
	Assert.assertEquals("Only one relation should have been created : ", 1,
		instance2.getRawLinks().size());
	Assert.assertEquals(
		"Only one instance of dependency should have been instanciated",
		1, implementationdep.getInsts().size());

	// Test should fail on external bundle resolution
	testResolutionExceptionCase14(instance2, 3);
	auxListInstances();
	org.junit.Assert
		.assertTrue(
			"An exception should be raised as the dependency cannot be resolved as no instance running",
			testResolutionExceptionCase14(instance2, 3));
	Assert.assertEquals("Only one relation should have been created : ", 1,
		instance.getRawLinks().size());
    }

    @Test
    public void RelationConstraintForSourceInstanceTargetInstance_tc113() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-DependencyImpl");

	Implementation implementationTarget = CST.apamResolver.findImplByName(
		null, "S07-implementation-15");

	Instance instanceInvalid01 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "do-not-take-this-instance");
		    }
		});
	Instance instanceInvalid02 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "neither-this");
		    }
		});
	Instance instanceValid = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "the-chosen-one");
		    }
		});
	Instance instanceInvalid04 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "ignore-this");
		    }
		});

	Instance instanceTarget = implementationTarget.createInstance(null,
		null);

	S07ImplementationImporter15 implem = (S07ImplementationImporter15) instanceTarget
		.getServiceObject();

	Instance instanceInjected = auxListInstanceReferencedBy(implem
		.getInjected());

	Assert.assertTrue(
		"Using <relation/> element in metadata to inject a dependency, the constraint filter was not respect as expected.",
		instanceInjected == instanceValid);

    }

    @Test
    public void RelationPreferencesForSourceInstanceTargetInstance_tc114() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-DependencyImpl");

	Implementation implementationTarget = CST.apamResolver.findImplByName(
		null, "S07-implementation-17");

	Instance instanceInvalid01 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "do-not-take-this-instance");
		    }
		});
	Instance instanceInvalid02 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "neither-this");
		    }
		});
	Instance instanceValid01 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "the-chosen-one");
		    }
		});
	Instance instanceValid02 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "the-chosen-one");
		    }
		});
	Instance instanceInvalid03 = implementation.createInstance(null,
		new HashMap<String, String>() {
		    {
			put("criteria", "ignore-this");
		    }
		});

	Instance instanceTarget = implementationTarget.createInstance(null,
		null);

	S07ImplementationImporter17 implem = (S07ImplementationImporter17) instanceTarget
		.getServiceObject();

	Instance instanceInjected = auxListInstanceReferencedBy(implem
		.getInjected());

	Assert.assertTrue(
		"Using <relation/> element in metadata to inject a dependency, the preference filter was not respect as expected.",
		instanceInjected == instanceValid01
			|| instanceInjected == instanceValid02);

    }



    private boolean testResolutionExceptionCase16(Instance inst) {
	// Force field injection (a bit akward with polymorphism)
	S07Implem16 implem = (S07Implem16) inst.getServiceObject();
	try {
	    if (implem.getInjected02() == null)
		return true;
	} catch (ResolutionException exc) {
	    exc.printStackTrace();
	    return true;
	} catch (Exception exc) {
	    exc.printStackTrace();
	    return true;
	}
	return false;
    }

    @Test
    public void RelationLinkCreationManual_tct004() {
	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-16");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	Implementation implementationdep = CST.apamResolver.findImplByName(
		null, "S07-DependencyImpl-02");
	Instance instancedep = implementationdep.createInstance(null,
		Collections.<String, String> emptyMap());

	org.junit.Assert
		.assertTrue(
			"An exception should be raised as the dependency cannot be resolved automatically (creation=manual)",
			testResolutionExceptionCase16(instance));
	Assert.assertEquals(
		"No relations should have been created (no instance of dependency existing)",
		0, instance.getRawLinks().size());

	instance.createLink(instancedep, instance.getRelation("testexist02"),
		false, false);

	testResolutionExceptionCase16(instance);
	auxListInstances();
	org.junit.Assert
		.assertFalse(
			"No exception should be raised as the dependency has been resolved manually",
			testResolutionExceptionCase16(instance));
	Assert.assertEquals("Only one relation should have been created : ", 1,
		instance.getRawLinks().size());

    }

    @Test
    public void RelationSourceInstanceTargetImplementation_tc119() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-18");

	Instance instance = implementation.createInstance(null, new HashMap() {
	    {
		put("instance-property", "ok");
	    }
	});// Collections.<String, String> emptyMap()

	S07ImplementationImporter18 dependency = (S07ImplementationImporter18) instance
		.getServiceObject();

	auxListProperties(dependency.getInjected());

	auxListInstances();

	ComponentImpl ci = (ComponentImpl) instance;

	Assert.assertTrue(
		String.format(
			"Only one link should have been created, but %s links were found",
			ci.getLocalLinks().size()),
		ci.getLocalLinks().size() == 1);

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    Instance.class, Implementation.class);

	}

	String messageTemplate = "Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";

	Assert.assertTrue(
		String.format(messageTemplate, "Instance", "Implementation",
			instance.getServiceObject()),
		dependency.getInjected().getProperty("instance-property") == null
			&& dependency.getInjected().getProperty(
				"implementation-property") != null
			&& dependency.getInjected().getProperty(
				"specification-property") != null);

    }

    @Test
    public void RelationSourceInstanceTargetSpecification_tc121() {

	Implementation implementation = CST.apamResolver.findImplByName(null,
		"S07-implementation-19");

	Instance instance = implementation.createInstance(null, new HashMap() {
	    {
		put("instance-property", "ok");
	    }
	});// Collections.<String, String> emptyMap()

	S07ImplementationImporter19 dependency = (S07ImplementationImporter19) instance
		.getServiceObject();

	auxListProperties(dependency.getInjected());

	auxListInstances();

	ComponentImpl ci = (ComponentImpl) instance;

	Assert.assertTrue(
		String.format(
			"Only one link should have been created, but %s links were found",
			ci.getLocalLinks().size()),
		ci.getLocalLinks().size() == 1);

	for (Link link : ci.getLocalLinks()) {

	    validateSourceTargetTypes(link.getSource(), link.getDestination(),
		    Instance.class, Specification.class);

	}

	String messageTemplate = "Declaring a relation from source %s to target %s, instantiated object of the type %s which is not the targetKind expected.";

	Assert.assertTrue(
		String.format(messageTemplate, "Instance", "Specification",
			instance.getServiceObject()),
		dependency.getInjected().getProperty("instance-property") == null
			&& dependency.getInjected().getProperty(
				"implementation-property") == null
			&& dependency.getInjected().getProperty(
				"specification-property") != null);

    }

}
