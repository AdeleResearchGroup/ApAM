package fr.imag.adele.apam.test.testcases;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.app1.spec.App1Spec;
import fr.imag.adele.apam.app2.spec.App2Spec;
import fr.imag.adele.apam.test.support.ApAMHelper;
import fr.imag.adele.apam.test.support.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class OBRMANTest extends ExtensionAbstract {
	//
	@Inject
	protected BundleContext context;

	private ApAMHelper apam;

	/**
	 * Done some initializations.
	 */
	@Before
	public void setUp() {
		apam = new ApAMHelper(context);
	}

	@Configuration
	public static Option[] apamConfig() {

		Option[] platform = options(felix(),
				systemProperty("org.osgi.service.http.port").value("8080"));

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo")
						.version("1.8.0"),
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
		apam.dispose();
	}

	@Test
	public void testRootModel() {
		waitForIt(1000);
		int sizebefaore = apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE)
				.size();
		try {
			apam.setObrManInitialConfig("wrongfilelocation", null, 1);
			fail("wrongfilelocation");
		} catch (IOException e) {
			assertEquals(sizebefaore,
					apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE).size());
		}
	}

	/**
	 * Simple Test : Create a compositetype with obrman model and instantiate it
	 * then call the application service This composite must contains only the
	 * spec and the main impl of the composite
	 */
	@Test
	public void simpleComposite() {
		waitForIt(1000);
		try {
			String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/APPS/0.0.1-SNAPSHOT!/APPS-repo.xml" };
			apam.setObrManInitialConfig("rootAPPS", repos, 1);
		} catch (IOException e) {
			fail(e.getMessage());
		}

		CompositeType app2CompoType = apam.createCompositeType("APP2",
				"APP2_MAIN", null);

		App2Spec app2Spec = apam.createInstance(app2CompoType, App2Spec.class);

		System.out
				.println("\n==================Start call test=================== \n");

		app2Spec.call("Call Main APP2 from Test");

		System.out
				.println("\n=================End call test====================\n");
	}

	
//	<parent>
//	<artifactId>OBRMAN-Tests</artifactId>
//	<groupId>fr.imag.adele.apam.tests</groupId>
//	<version>0.0.1-SNAPSHOT</version>
//</parent>
//
//
//<groupId>fr.imag.adele.apam.tests.obrman</groupId>
//<artifactId>run-obrman-tests</artifactId>
//
//	
//	---------------
//	<parent>
//	<artifactId>APAMProject.root</artifactId>
//	<groupId>fr.imag.adele.apam</groupId>
//	<version>0.0.1-SNAPSHOT</version>
//	<relativePath>../../pom.xml</relativePath>
//</parent>
//
//<packaging>bundle</packaging>
//<groupId>fr.imag.adele.apam.pax</groupId>
//<artifactId>apam-pax-test</artifactId>
//<name>apam-pax-test</name>
	
	/**
	 * APP1 declare two repositories in ObrMan model The composite APP1 deploy
	 * and instantiate the composite APP2 The composite APP2 will be inside the
	 * composite APP1
	 * 
	 */
	@Test
	public void embeddedComposite() {
		waitForIt(1000);

		try {
			String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/APPS/0.0.1-SNAPSHOT!/APPS-repo.xml" };
			apam.setObrManInitialConfig("rootAPPS", repos, 1);
		} catch (IOException e) {
			fail(e.getMessage());
		}

		CompositeType app1CompoType = apam.createCompositeType("APP1",
				"APP1_MAIN", null);

		App1Spec app1Spec = apam.createInstance(app1CompoType, App1Spec.class);

		System.out
				.println("\n==================Start call test=================== \n");

		app1Spec.call("Call Main APP1 from Test");

		System.out
				.println("\n=================End call test====================\n");
	}

	/**
	 * APP1 declare one repository and APP2 composite in ObrMan model create the
	 * composite APP2 and call it create the composite APP1 which will call the
	 * composite APP2 APP1 and APP2 will be on the same level of root composite.
	 */
	@Test
	public void movedCompositev1() {
		waitForIt(1000);

		simpleComposite();

		CompositeType app1CompoType = apam.createCompositeType("APP1.2",
				"APP1_MAIN", null);

		CompositeType root = (CompositeType) app1CompoType.getInCompositeType()
				.toArray()[0];

		assertEquals(2, root.getEmbedded().size()); // the root compositeType
													// contains two composites

		App1Spec app1Spec = apam.createInstance(app1CompoType, App1Spec.class);

		System.out
				.println("\n==================Start call test=================== \n");

		app1Spec.call("Call Main APP1 from Test");

		System.out
				.println("\n=================End call test====================\n");

		assertEquals(1, app1CompoType.getEmbedded().size()); // app1 contains
																// app2

		assertEquals(1, root.getEmbedded().size()); // the root compositeType
													// contains two composites

	}

	/**
	 * APP1 declare one repository and APP2 composite in ObrMan model Try to
	 * create APP1 composite, but APP2 composite is missing
	 */
	@Test
	public void missingAPP2Composite() {
		waitForIt(1000);
		apam.createCompositeType("APP1.2", "APP1_MAIN", null);
	}

}