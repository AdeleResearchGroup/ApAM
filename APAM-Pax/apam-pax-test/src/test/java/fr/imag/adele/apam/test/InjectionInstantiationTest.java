package fr.imag.adele.apam.test;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.test.iface.device.Eletronic;
import fr.imag.adele.apam.test.impl.S1Impl;
import fr.imag.adele.apam.test.impl.device.GenericSwitch;

@RunWith(JUnit4TestRunner.class)
public class InjectionInstantiationTest extends ExtensionAbstract {

	/**
	 * Creates an implementation and verifies if an correct instance of such
	 * implementation was added in APAM
	 * 
	 * @TODO Change this code to test in case of
	 *       fr.imag.adele.apam.core.CompositeDeclaration
	 */
	@Test
	public void AtomicInstanceCreationWithoutInjection() {

		waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		// save the initial number of instances present in APAM
		int counterInstanceBefore = CST.componentBroker.getInsts().size();

		Instance inst = s1Impl.createInstance(null, null);

		ImplementationDeclaration initialImplDecl = inst.getImpl()
				.getImplDeclaration();

		boolean found = false;

		// save the number of instances present in APAM after the creation of
		// our own instance
		int counterInstanceAfter = CST.componentBroker.getInsts().size();

		for (Instance i : CST.componentBroker.getInsts()) {

			ImplementationDeclaration apamImplDecl = i.getImpl()
					.getImplDeclaration();

			if (apamImplDecl instanceof AtomicImplementationDeclaration
					&& initialImplDecl instanceof AtomicImplementationDeclaration) {
				AtomicImplementationDeclaration atomicInitialInstance = (AtomicImplementationDeclaration) apamImplDecl;
				AtomicImplementationDeclaration atomicApamInstance = (AtomicImplementationDeclaration) initialImplDecl;

				if (atomicInitialInstance.getClassName().equals(
						atomicApamInstance.getClassName())) {
					found = true;
					break;
				}
			}
		}

		// Checks if a new instance was added into APAM
		Assert.assertTrue((counterInstanceBefore + 1) == counterInstanceAfter);
		// Check if its a correct type
		Assert.assertTrue(found);

	}



	/**
	 * Keeping a set of a given type, verify if the number of elements in this
	 * set are updated automatically after unplugging (remove wire) the
	 * application that holds this set for the Type Set
	 */
	@Test
	public void InjectionUpdateLinkForSetType() {

		waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		int initialSize = s1.getEletronicInstancesInSet().size();

		for (Wire wire : s1Inst.getWires()) {

			s1Inst.removeWire(wire);

		}

		Implementation sansungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");

		Instance sansungInst = (Instance) sansungImpl
				.createInstance(null, null);

		GenericSwitch samsungSwitch = (GenericSwitch) sansungInst
				.getServiceObject();

		int finalSize = s1.getEletronicInstancesInSet().size();

		// Make sure that one instance was added
		Assert.assertTrue((finalSize - initialSize) == 1);

	}

	/**
	 * Keeping a set of a given type, verify if the number of elements in this
	 * set are updated automatically after unplugging (remove wire) the
	 * application that holds this set for the native array type
	 * 
	 * @TODO Test only if the injection of the instances are working in the
	 *       native array type
	 */
	@Test
	public void InjectionUpdateLinkForArrayType() {

		waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		int initialSize = s1.getEletronicInstancesInArray().length;

		for (Wire wire : s1Inst.getWires()) {

			s1Inst.removeWire(wire);

		}

		Implementation sansungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");

		Instance sansungInst = (Instance) sansungImpl
				.createInstance(null, null);

		GenericSwitch samsungSwitch = (GenericSwitch) sansungInst
				.getServiceObject();

		int finalSize = s1.getEletronicInstancesInArray().length;

		// Make sure that one instance was added
		Assert.assertTrue((finalSize - initialSize) == 1);

	}
	
}
// Apam apam = (Apam) help.getServiceObject(Apam.class.getName(), null);
// CST.componentBroker.getInstService(s3bis) ;
// Instance s3Inst=s3Impl.createInstance(null, null);
// Implementation s3Impl =
// CST.apamResolver.findImplByName(null,"apam.test.dependency.S3Impl");

// contraintes implementations
// contraintes instances

// heritage de contraintes
// contraintes générique

// preferences

// instantiable

// shared

// singleton

// resolution interface
// resolution message
// resolution Spec
// resolution Implem
// resolution instance

// fail
// exception
// override exception
// override hidden
// wait