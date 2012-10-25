package fr.imag.adele.apam.test;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import apam.test.dependency.Dependency;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ComponentBroker;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.test.impl.S1Impl;
import fr.imag.adele.apam.test.impl.device.GenericSwitch;

@RunWith(JUnit4TestRunner.class)
public class PaxTest {

	@Inject
	protected BundleContext context;

	OSGiHelper OSGihelper;

	// ApamResolver apamResolver;
	// ComponentBroker apamBroker;

	private static final int CONST_WAIT_TIME = 2000;

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
						.version("1.2.17"),
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S4")
						.version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S5")
						.version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S3")
						.version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("TestAttrSpec").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("TestDependency").version("0.0.1-SNAPSHOT")
		// mavenBundle().groupId("fr.imag.adele.apam")
		// .artifactId("S3Impl").version("0.0.1-SNAPSHOT"),
		// mavenBundle().groupId("fr.imag.adele.apam.pax")
		// .artifactId("apam-pax-samples-impl-s1").version("1.6.0")
		));

		Option[] r = OptionUtils.combine(platform, bundles);

		// Option[] debug =
		// options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));

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
	public void CheckingConstraints() {

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
	 * @TODO Implementing this test by using the API of APAM instead of
	 *       requiring the Dependency.class application to be installed
	 */
	@Test
	@Ignore
	public void DynamicInjectingMultipleDependencies() {

		waitForIt(CONST_WAIT_TIME);

		Implementation s3Impl = CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency = (Dependency) s3Inst.getServiceObject();

		dependency.p1();
		dependency.p2();
		dependency.p3();

		Assert.assertTrue(dependency.getS3_1set().contains(
				dependency.getS3Inst().getServiceObject()));
		Assert.assertTrue(dependency.getS3_1set().containsAll(
				Arrays.asList(dependency.getS3_2array())));
		;
	}

	/**
	 * @TODO Implementing this test by using the API of APAM instead of
	 *       requiring the Dependency.class application to be installed
	 */
	@Test
	@Ignore
	public void DynamicWireDeletionMultipleDependency() {

		waitForIt(CONST_WAIT_TIME);

		Implementation s3Impl = CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency = (Dependency) s3Inst.getServiceObject();

		dependency.p1();
		dependency.p2();
		dependency.p3();
		dependency.p4();

		Assert.assertTrue(!dependency.getS3_1set().contains(
				dependency.getRmInst().getServiceObject()));
		Assert.assertTrue(dependency.getS3_1set().containsAll(
				Arrays.asList(dependency.getS3_2array())));

	}

	@Test
	@Ignore
	public void MultipleConstraints() throws InvalidSyntaxException {

		waitForIt(CONST_WAIT_TIME);

		final Set<Filter> constraintsAglomerated = new HashSet<Filter>() {
			{
				add(FrameworkUtil
						.createFilter("(&(OS=Windows)(location=bedroom))"));
			}
		};

		final Set<Filter> constraintsSingle = new HashSet<Filter>() {
			{
				add(FrameworkUtil.createFilter("(OS=Windows)"));
				add(FrameworkUtil.createFilter("(location=bedroom)"));
			}
		};

		Implementation s3Impl = CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency = (Dependency) s3Inst.getServiceObject();

		Instance instanceS3_1 = CST.componentBroker.getInstService(dependency
				.getS3ImplWindowsBedroomTry1());
		Instance instanceS3_2 = CST.componentBroker.getInstService(dependency
				.getS3ImplWindowsBedroomTry2());

		Assert.assertTrue(instanceS3_1.match(constraintsAglomerated));
		Assert.assertTrue(instanceS3_2.match(constraintsSingle));

	}

	@Test
	@Ignore
	public void CheckIfConstrainstWereTakenIntoConsideration() {

		waitForIt(CONST_WAIT_TIME);

		// Implementation s3Impl = CST.apamResolver.findImplByName(null,
		// "Dependency");

		final Set<String> constraints = new HashSet<String>() {
			{
				add("(OS*>Android)");
				add("(&(location=living)(MyBool=true))");
			}
		};

		final List<String> preferences = new ArrayList<String>() {
			{
				add("(OS*&gt;Linux, IOS, Windows)");
				add("(OS*&gt;Linux, IOS)");
				add("(OS*&gt;IOS)");
			}
		};

		Apam apam = (Apam) OSGihelper.getServiceObject(Apam.class.getName(),
				null);

		// CST.apamResolver.findComponentByName(null, "Dependency");

		Implementation s3Impl = CST.componentBroker.getImpl("Dependency");
		Instance inst = s3Impl.createInstance(null, null);

		Dependency dependency = (Dependency) inst.getServiceObject();

		Instance instance = CST.apamResolver.resolveImpl(null, s3Impl,
				constraints, preferences);

		System.out.println("instance name:" + instance.getName());

	}

}
// Apam apam = (Apam) help.getServiceObject(Apam.class.getName(), null);
// CST.componentBroker.getInstService(s3bis) ;
// Instance s3Inst=s3Impl.createInstance(null, null);
// Implementation s3Impl =
// CST.apamResolver.findImplByName(null,"apam.test.dependency.S3Impl");

// contraintes multiple

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