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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.test.iface.device.Eletronic;
import fr.imag.adele.apam.test.impl.S1Impl;
import fr.imag.adele.apam.test.impl.device.GenericSwitch;

@RunWith(JUnit4TestRunner.class)
public class PaxTest {

	@Inject
	protected BundleContext context;

	OSGiHelper OSGihelper;

	// ApamResolver apamResolver;
	// ComponentBroker apamBroker;

	private static final int CONST_WAIT_TIME = 5000;

	private static void waitForIt(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			assert false;
		}
	}

	@Before
	public void setUp() {

		OSGihelper = new OSGiHelper(context);
		// apamResolver = CST.apamResolver;
		// apamBroker = CST.componentBroker;

	}

	@Configuration
	public static Option[] apamConfig() {

		Option[] platform = options(felix(),
				systemProperty("org.osgi.service.http.port").value("8080"));

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo").version("1.8.0"),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").version("0.2.0"),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").version("4.2.0"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.bundlerepository")
						.version("1.6.6"),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-mvn").version("1.3.5"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("APAMBundle").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("OBRMAN").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.version("1.6.6"),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-log4j12")
						.version("1.6.6"),
				mavenBundle().groupId("log4j").artifactId("log4j")
						.version("1.2.17")
		// mavenBundle().groupId("fr.imag.adele.apam").artifactId("S4")
		// .version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam").artifactId("S5")
		// .version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam").artifactId("S3")
		// .version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam")
		// .artifactId("TestAttrSpec").version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam")
		// .artifactId("TestDependency").version("0.0.1-SNAPSHOT")
		// mavenBundle().groupId("fr.imag.adele.apam")
		// .artifactId("S3Impl").version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam.pax")
		// .artifactId("apam-pax-samples-impl-s1").version("1.6.0")
		));

		Option[] r = OptionUtils.combine(platform, bundles);

		Option[] debug = options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));

		// r = OptionUtils.combine(r, debug);

		// Option[] log =
		// options(vmOption("-Dlog4j.file=./am.log4j.properties"));
		// r = OptionUtils.combine(r, log);
		return r;
	}

	@After
	public void tearDown() {
		OSGihelper.dispose();
	}

	/**
	 * Creates an implementation and verifies if an correct instance of such
	 * implementation was added in APAM
	 * 
	 * @TODO Change this code to test in case of
	 *       fr.imag.adele.apam.core.CompositeDeclaration
	 */
	@Test
	public void AtomicInstanceCreationWithoutInjection() {

		waitForIt(CONST_WAIT_TIME);

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
	 * Verify if the constraints were used to inject the dependencies in the
	 * component
	 */
	@Test
	public void CheckingConstraintsImplementation() {

		waitForIt(CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		Instance philipsSwitch = CST.componentBroker.getInstService(s1
				.getSimpleDevice110v());

		Assert.assertTrue(philipsSwitch.match("(manufacturer=philips)"));
		Assert.assertTrue(philipsSwitch.match("(voltage=110)"));
		Assert.assertTrue(philipsSwitch
				.match("(&amp;(voltage=110)(manufacturer=philips))"));

	}

	/**
	 * Verify if the constraints were used to inject the dependencies in the
	 * component by initial properties
	 * 
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void CheckingConstraintsInstanceFilteringByInitialProperty()
			throws InvalidSyntaxException {

		waitForIt(CONST_WAIT_TIME);

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "95");
					}
				});

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "100");
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

		Implementation boschImpl = CST.apamResolver.findImplByName(null,
				"BoschSwitch");
		final Instance boschInst = boschImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "110");
					}
				});

		Implementation philipsImpl = CST.apamResolver.findImplByName(null,
				"philipsSwitch");
		final Instance philipsInst = philipsImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "117");
					}
				});

		Set<Instance> validInstances = new HashSet<Instance>() {
			{
				add(boschInst);
				add(siemensInst);
				add(lgInst);
				add(samsungInst);
			}
		};

		waitForIt(CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		// Set<Instance>
		// instances=CST.componentBroker.getInsts(FrameworkUtil.createFilter("(voltageCurrent=-*)"));
		// System.out.println("instances -----------------:"+instances.size());

		Instance s1Inst = s1Impl.createInstance(null, null);
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		for (Eletronic e : s1.getEletronicInstancesConstraintsInstance()) {
			Instance p = CST.componentBroker.getInstService(e);
			System.out.println("---- Voltage:"
					+ p.getProperty("currentVoltage") + " / Name:"
					+ p.getName());

			boolean found = false;

			for (Instance l : validInstances)
				if (l.getName().equals(p.getName())) {
					found = true;
					break;
				}

			// Check if all valid instances were injected
			Assert.assertTrue(found);

		}

		// check if there is no other instance injected
		Assert.assertTrue(s1.getEletronicInstancesConstraintsInstance().size() == validInstances
				.size());

	}

	/**
	 * Verify if the constraints were used to inject the dependencies in the
	 * component by set property
	 * 
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void CheckingConstraintsInstanceFilteringBySetProperty()
			throws InvalidSyntaxException {

		waitForIt(CONST_WAIT_TIME);

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null, null);

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null, null);

		Implementation siemensImpl = CST.apamResolver.findImplByName(null,
				"SiemensSwitch");
		final Instance siemensInst = siemensImpl.createInstance(null, null);

		Implementation boschImpl = CST.apamResolver.findImplByName(null,
				"BoschSwitch");
		final Instance boschInst = boschImpl.createInstance(null, null);

		Implementation philipsImpl = CST.apamResolver.findImplByName(null,
				"philipsSwitch");
		final Instance philipsInst = philipsImpl.createInstance(null, null);

		samsungInst.setProperty("currentVoltage", "95");
		lgInst.setProperty("currentVoltage", "100");
		siemensInst.setProperty("currentVoltage", "105");
		boschInst.setProperty("currentVoltage", "110");
		philipsInst.setProperty("currentVoltage", "117");

		Set<Instance> validInstances = new HashSet<Instance>() {
			{
				add(boschInst);
				add(siemensInst);
				add(lgInst);
				add(samsungInst);
			}
		};

		waitForIt(CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		for (Eletronic e : s1.getEletronicInstancesConstraintsInstance()) {
			Instance p = CST.componentBroker.getInstService(e);
			System.out.println("---- Voltage:"
					+ p.getProperty("currentVoltage") + " / Name:"
					+ p.getName());

			boolean found = false;

			for (Instance l : validInstances)
				if (l.getName().equals(p.getName())) {
					found = true;
					break;
				}

			// Check if all valid instances were injected
			Assert.assertTrue(found);

		}

		// check if there is no other instance injected
		Assert.assertTrue(s1.getEletronicInstancesConstraintsInstance().size() == validInstances
				.size());

	}

	/**
	 * Keeping a set of a given type, verify if the number of elements in this
	 * set are updated automatically after unplugging (remove wire) the
	 * application that holds this set for the Type Set
	 */
	@Test
	public void InjectionUpdateLinkForSetType() {

		waitForIt(CONST_WAIT_TIME);

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
	 * @TODO Test only if the injection of the instaces are working in the
	 *       native array type
	 */
	@Test
	public void InjectionUpdateLinkForArrayType() {

		waitForIt(CONST_WAIT_TIME);

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
	
	/**
	 * 
	 */
	@Test
	public void InheritedPropertyCannotBeChanged(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null, null);
		
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");
		
		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();
	
		//this should be updated correctly
		samsungInst.setProperty("currentVoltage", "999");
		//this should stay with the old value
		samsungInst.setProperty("made", "deutschland");
		
		//this property should be updated since its not inherited
		Assert.assertTrue(samsungInst.getProperty("currentVoltage").equals("999")) ;
		
		//this should stay the same, since its a property defined in the Samsung Switch component.
		Assert.assertTrue(samsungInst.getProperty("made").equals("china")) ;
		
	}
	
	@Test
	@Ignore
	public void InheritedPropertyChanged(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null, null);
		
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.test.impl.S1Impl");
		
		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();
		
		Component k=(Component)samsungInst;
		
		System.out.println("### Declaration Inst");
		
		for(String key:s1Inst.getSpec().getAllProperties().keySet()){
			Object value=s1Inst.getSpec().getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		System.out.println("### Declaration Impl");
		
		for(String key:s1Inst.getImpl().getSpec().getAllProperties().keySet()){
			Object value=s1Inst.getImpl().getSpec().getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		System.out.println("### Declaration Spec");
		
		for(String key:s1Inst.getSpec().getAllProperties().keySet()){
			Object value=s1Inst.getSpec().getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		System.out.println("### Spec");
		
		for(String key:s1Inst.getSpec().getAllProperties().keySet()){
			Object value=s1Inst.getSpec().getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		System.out.println("### Implem");
		
		for(String key:s1Inst.getImpl().getAllProperties().keySet()){
			Object value=s1Inst.getImpl().getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		System.out.println("### Instance");
		
		for(String key:s1Inst.getAllProperties().keySet()){
			Object value=s1Inst.getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		
		System.out.println("----Before");
		
		for(String key:k.getAllProperties().keySet()){
			Object value=k.getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		
		//this should stay with the old value
		samsungInst.setProperty("voltage", "300");
		//this should be updated correctly
		samsungInst.setProperty("currentVoltage", "666");
		
		System.out.println("----After");
		
		for(String key:k.getAllProperties().keySet()){
			Object value=k.getAllProperties().get(key);
			System.out.println("------"+key+":"+value);
		}
		
		//manufacturer
		
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