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
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import apam.test.dependency.Dependency;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;

@RunWith(JUnit4TestRunner.class)
public class PaxTest {

	@Inject
	protected BundleContext context;
	
	OSGiHelper OSGihelper;
	
	private static final int CONST_WAIT_TIME=1000;

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
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S3")
						.version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("TestAttrSpec").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("TestDependency").version("0.0.1-SNAPSHOT")

		));

		Option[] r = OptionUtils.combine(platform, bundles);

		//Option[] debug = options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));

		//r = OptionUtils.combine(r, debug);

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

		Implementation s3Impl = CST.apamResolver.findImplByName(null, "S3Impl");

		// save the initial number of instances present in APAM
		int counterInstanceBefore = CST.componentBroker.getInsts().size();

		Instance inst = s3Impl.createInstance(null, null);

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
	 * @TODO Implementing this test by using the API of APAM instead of requiring the Dependency.class application to be installed
	 */
	@Test
	public void CheckingConstraints(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation s3Impl=CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency=(Dependency)s3Inst.getServiceObject();
		
		dependency.p1();
		dependency.p2();
		
		Assert.assertTrue(dependency.getS3Inst().match("(OS*>Android)" ));
		Assert.assertTrue(dependency.getS3Inst().match("(&amp;(location=living)(MyBool=true))"));
		
	}
	
	/**
	 * @TODO Implementing this test by using the API of APAM instead of requiring the Dependency.class application to be installed
	 */
	@Test
	public void CheckingConstraintsWereInjected(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation s3Impl=CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency=(Dependency)s3Inst.getServiceObject();
		
		dependency.p1();
		dependency.p2();
		
		Assert.assertTrue(dependency.getS3_1set().size() != 0) ;
		Assert.assertTrue(dependency.getS3_1set().containsAll (Arrays.asList(dependency.getS3_2array()))) ;
	}
	
	/**
	 * @TODO Implementing this test by using the API of APAM instead of requiring the Dependency.class application to be installed
	 */
	@Test
	public void DynamicInjectingMultipleDependencies(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation s3Impl=CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency=(Dependency)s3Inst.getServiceObject();
		
		dependency.p1();
		dependency.p2();
		dependency.p3();
		
		Assert.assertTrue(dependency.getS3_1set().contains(s3Inst.getServiceObject())) ;
		Assert.assertTrue(dependency.getS3_1set().containsAll (Arrays.asList(dependency.getS3_2array()))) ;;
	}
	
	/**
	 * @TODO Implementing this test by using the API of APAM instead of requiring the Dependency.class application to be installed
	 */
	@Test
	public void DynamicWireDeletionMultipleDependency(){
		
		waitForIt(CONST_WAIT_TIME);
		
		Implementation s3Impl=CST.componentBroker.getImpl("Dependency");
		Instance s3Inst = s3Impl.createInstance(null, null);

		Dependency dependency=(Dependency)s3Inst.getServiceObject();
		
		dependency.p1();
		dependency.p2();
		dependency.p3();
		dependency.p4();
		
		Assert.assertTrue(!dependency.getS3_1set().contains(dependency.getRmInst().getServiceObject()));
		Assert.assertTrue(dependency.getS3_1set().containsAll (Arrays.asList(dependency.getS3_2array()))) ;
		
	}
	
	@Test
	@Ignore
	public void CheckIfConstrainstWereTakenIntoConsideration() {

		waitForIt(CONST_WAIT_TIME);

		//Implementation s3Impl = CST.apamResolver.findImplByName(null, "Dependency");

		final Set<String> constraints = new HashSet<String>() {
			{
				add("(OS*>Android)");
				add("(&(location=living)(MyBool=true))");
			}
		};

		final List<String> preferences = new ArrayList<String>(){
			{
				add("(OS*&gt;Linux, IOS, Windows)");
				add("(OS*&gt;Linux, IOS)");
				add("(OS*&gt;IOS)");
			}
		};

		
		Apam apam = (Apam) OSGihelper.getServiceObject(Apam.class.getName(), null);
		
		//CST.apamResolver.findComponentByName(null, "Dependency");
		
		Implementation s3Impl=CST.componentBroker.getImpl("Dependency");
		Instance inst = s3Impl.createInstance(null, null);

		Dependency dependency=(Dependency)inst.getServiceObject();
		
		//apam.getComposite("Dependency").getComposite()
		//CST.apamResolver.
		Instance instance=CST.apamResolver.resolveImpl(null, s3Impl, constraints, preferences);

		System.out.println("instance name:"+instance.getName());
		
		//Instance inst = s3Impl.createInstance(null, null);

		//Instance s3Inst = CST.componentBroker.getInstService(s3bis);

		// fr.imag.adele.apam.Component compo =
		// CST.apamResolver.findComponentByName(targetType, componentName);
		// if (compo instanceof Implementation)
		// ((Implementation)compo).createInstance(target,null);
		// if (compo instanceof Specification) {
		// Implementation impl = CST.apamResolver.resolveSpecByName(targetType,
		// componentName, null, null) ;
		// if (impl != null)
		// impl.createInstance(null, null);
		// }
		//
		// Composite instanceApp = (Composite) CompoType.createInstance(null,
		// null);

		// CST.componentBroker.getSpec("lo")

		// Checking constraints
		// s3Inst = CST.componentBroker.getInstService(s3bis) ;

//		Assert.assertTrue(s3Inst.match("(OS*>Android)")
//				&& s3Inst.match("(&(location=living)(MyBool=true))"));

	}

	public void testRootModel() {

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

	}

}
// Apam apam = (Apam) help.getServiceObject(Apam.class.getName(), null);
// CST.componentBroker.getInstService(s3bis) ;
// Instance s3Inst=s3Impl.createInstance(null, null);
// Implementation s3Impl =
// CST.apamResolver.findImplByName(null,"apam.test.dependency.S3Impl");