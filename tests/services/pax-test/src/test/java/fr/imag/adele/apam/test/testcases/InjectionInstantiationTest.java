package fr.imag.adele.apam.test.testcases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.impl.S1Impl;
import fr.imag.adele.apam.pax.test.impl.device.GenericSwitch;
import fr.imag.adele.apam.pax.test.impl.device.HouseMeterSwitch;
import fr.imag.adele.apam.test.support.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
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

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

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

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

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

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

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

	/**
	 * Test is singleton activated in the implementation really ensures that no
	 * more than one instance is created in apam
	 */
	@Test
	public void SingletonNotSharedInstance() {
		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterSingletonNotShared");

		Instance inst1 = impl.createInstance(null,
				new HashMap<String, String>());
		Instance inst2 = null;

		try {
			inst2 = impl.createInstance(null, new HashMap<String, String>());
		} catch (Exception e) {
			// Nothing to do
		}

		System.out.println("-----" + inst1);
		System.out.println("-----" + inst2);

		Assert.assertTrue(
				"In case of a singleton not shared instance, after calling createInstance an exception should be raised",
				inst1 != null);
		Assert.assertTrue(
				"In case of a singleton not shared instance, after calling createInstance an exception should be raised",
				inst2 == null);
	}

	/**
	 * Test is singleton activated in the implementation really ensures that no
	 * more than one instance is created in apam
	 */
	@Test
	public void SingletonSharedInstance() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterSingletonShared");

		CompositeType root = (CompositeType) impl.getInCompositeType()
				.toArray()[0];

		System.out.println("IMPL:" + impl);

		Composite rootComposite = null;

		if (root.getInst() instanceof Composite) {
			rootComposite = (Composite) root.getInst();
		}

		impl.createInstance(null, null);

		Instance inst1 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());
		Instance inst2 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());
		Instance inst3 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());

		final String message = "In case of a singleton and shared instance, all instances should be the same";

		Assert.assertTrue(message, inst1 == inst2);
		Assert.assertTrue(message, inst2 == inst3);

	}

	@Test
	public void NotSingletonNotSharedInstance() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterNotSingletonNotShared");

		Instance inst1 = impl.createInstance(null, null);
		Instance inst2 = impl.createInstance(null, null);
		Instance inst3 = impl.createInstance(null, null);

		System.out.println("1 ----- " + inst1 + "/"
				+ (HouseMeterSwitch) inst1.getServiceObject());
		System.out.println("2 ----- " + inst2 + "/"
				+ (HouseMeterSwitch) inst2.getServiceObject());
		System.out.println("3 ----- " + inst3 + "/"
				+ (HouseMeterSwitch) inst3.getServiceObject());

		HouseMeterSwitch houseMeter1 = (HouseMeterSwitch) inst1
				.getServiceObject();
		HouseMeterSwitch houseMeter2 = (HouseMeterSwitch) inst2
				.getServiceObject();
		HouseMeterSwitch houseMeter3 = (HouseMeterSwitch) inst3
				.getServiceObject();

		Assert.assertTrue(
				"In case of a not singleton and not shared instance, all instances should be different",
				houseMeter1 != houseMeter2);
		Assert.assertTrue(
				"In case of a not singleton and not shared instance, all instances should be different",
				houseMeter2 != houseMeter3);
		Assert.assertTrue(
				"In case of a not singleton and not shared instance, all instances should be different",
				houseMeter1 != houseMeter3);

	}

	@Test
	public void NotSingletonSharedInstance() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterNotSingletonShared");

		CompositeType root = (CompositeType) impl.getInCompositeType()
				.toArray()[0];

		Composite rootComposite = null;

		if (root.getInst() instanceof Composite) {
			rootComposite = (Composite) root.getInst();
		}

		impl.createInstance(null, null);

		boolean success = true;
		try {
			impl.createInstance(null, null);
			impl.createInstance(null, null);
		} catch (Exception e) {
			success = false;
		}

		Assert.assertTrue(
				"In case of a not singleton and shared instance, several instances of the same implementation can be created",
				success);

		Instance inst1 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());
		Instance inst2 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());
		Instance inst3 = CST.apamResolver.resolveImpl(rootComposite, impl,
				new HashSet<String>(), new ArrayList<String>());

		final String message = "In case of a not singleton and shared instance, a new instance is never created, always recycled";

		Assert.assertTrue(message, inst1 == inst2);
		Assert.assertTrue(message, inst2 == inst3);

	}

	@Test
	public void NotInstantiableInstance() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterNotInstantiable");

		boolean failed = false;

		try {
			Instance inst1 = impl.createInstance(null, null);
		} catch (Exception e) {
			// nothing to do
			failed = true;
		}

		Assert.assertTrue(
				"Not Instantiable instance shall not be instantiated by API or any other means",
				failed);

	}

	@Test
	public void InstantiableInstance() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation impl = CST.apamResolver.findImplByName(null,
				"HouseMeterInstantiable");

		boolean failed = false;

		try {
			Instance inst1 = impl.createInstance(null, null);
		} catch (Exception e) {
			// nothing to do
			failed = true;
		}

		Assert.assertFalse(
				"Instantiable instance shall be instantiated by API or any other means",
				failed);

	}

	@Test
	public void PreferenceInjectionAttribute() throws InvalidSyntaxException {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "100");
					}
				});

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "500");
					}
				});

		Implementation siemensImpl = CST.apamResolver.findImplByName(null,
				"SiemensSwitch");
		final Instance siemensInst = siemensImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "105");
					}
				});

		System.out.println("Instances before injection request");
		auxListInstances("\t");

		// Creates S1 instance (class that requires the injection)
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		Eletronic samsungSwitch = (Eletronic) samsungInst.getServiceObject();
		Eletronic lgSwitch = (Eletronic) lgInst.getServiceObject();
		Eletronic siemensSwitch = (Eletronic) siemensInst.getServiceObject();

		System.out.println("Instances after injection request");
		auxListInstances("\t");
		
		Instance injectedInstance=CST.componentBroker.getInstService(s1.getDevicePreference110v());
		
		Assert.assertTrue(
				String.format(
						"The instance injected should be the prefered one (currentVoltage=500), since there exist an instance in which the preference is valid. The instance %s (currentVoltage:%s) was injected instead of %s (currentVoltage:%s)",
						injectedInstance.getName(),injectedInstance.getAllProperties().get("currentVoltage"),
						samsungInst.getName(),samsungInst.getAllProperties().get("currentVoltage")),
				s1.getDevicePreference110v() == samsungSwitch);

	}

	@Test
	public void ConstraintInjectionWhenEmptyPreferenceTagExistsAttribute()
			throws InvalidSyntaxException {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "100");
					}
				});

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "500");
					}
				});

		Implementation siemensImpl = CST.apamResolver.findImplByName(null,
				"SiemensSwitch");
		final Instance siemensInst = siemensImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "105");
					}
				});

		System.out.println("Instances before injection request");
		auxListInstances("\t");
		// Creates S1 instance (class that requires the injection)
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		Eletronic samsungSwitch = (Eletronic) samsungInst.getServiceObject();
		Eletronic lgSwitch = (Eletronic) lgInst.getServiceObject();
		Eletronic siemensSwitch = (Eletronic) siemensInst.getServiceObject();

		System.out.println("Instances after injection request");
		auxListInstances("\t");
		
		Instance injectedInstance=CST.componentBroker.getInstService(s1.getDeviceConstraint110v());
		
		Assert.assertTrue(
				String.format(
						"The instance injected should obey the contraints (currentVoltage=500) given in the xml, this does not happens when there is a <preference> tag with nothing declared inside. The instance %s (currentVoltage:%s) was injected instead of %s (currentVoltage:%s)",
						injectedInstance.getName(),injectedInstance.getAllProperties().get("currentVoltage"),
						samsungInst.getName(),samsungInst.getAllProperties().get("currentVoltage")),
				s1.getDeviceConstraint110v() == samsungSwitch);

	}
	
	@Test
	public void FindImplByName(){
		
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		
		auxListInstances("before-");
		
		Implementation impl=CST.apamResolver.findImplByName(null,"fr.imag.adele.apam.pax.test.impl.S1Impl");
		apam.waitForIt(Constants.CONST_WAIT_TIME);
		auxListInstances("after-");
		
		Assert.assertTrue("Should be possible to find an implementation by using its name.",impl!=null);

	}	
	
	@Test
	public void CompositeTypeInstantiation() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		// CompositeType appli3 = apam.createCompositeType(null, "TestS1Bis",
		// null, "S1", null /* models */, null /* properties */);

		CompositeType app = CST.apam.createCompositeType(null,
				"eletronic-device-compotype", null, "eletronic-device",
				new HashSet<ManagerModel>(), new HashMap<String, String>());

		Assert.assertTrue(String.format("Failed to create the CompositeType"),
				app != null);

		Instance instApp = app
				.createInstance(null /* composite */, null/* properties */);

		Assert.assertTrue(
				String.format("Failed to create the instance of CompositeType"),
				instApp != null);
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