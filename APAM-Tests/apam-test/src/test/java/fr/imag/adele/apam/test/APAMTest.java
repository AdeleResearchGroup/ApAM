package fr.imag.adele.apam.test;

import static junit.framework.Assert.*;
//http://junit.sourceforge.net/javadoc/org/junit/Assert.html
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;
/**
 *  Test Suite 
 * 
 */
@RunWith(JUnit4TestRunner.class)
public  class APAMTest {
//	
//	//Mock object
//	@Mock LogService logservice;
	
	@Inject
	protected BundleContext context;
	
	protected  Apam apam;
	 
    protected OSGiHelper osgi;
    
    protected IPOJOHelper ipojo;
    

	/**
     * Done some initializations.
     */
    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        
        //initialise the annoted mock object
//        MockitoAnnotations.initMocks(this);
    }

	@Configuration
	public static Option[] apamConfig() {
		 Option[] platform = options(felix(),systemProperty( "org.osgi.service.http.port" ).value( "8080" ));

	        Option[] bundles = options(provision(
	                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
	                mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject(),
	                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(), 
	                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.bundlerepository").version("1.6.6"),
	                mavenBundle().groupId("fr.imag.adele.apam").artifactId("APAMBundle").version("0.0.1-SNAPSHOT"),
	                mavenBundle().groupId("fr.imag.adele.apam").artifactId("OBRMAN").version("0.0.1-SNAPSHOT"),
	                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").version("1.6.1"),
					mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").version("1.6.1")
	                )); 
	   

	        Option[] r = OptionUtils.combine(platform, bundles);

	        return r;
	}
 
	@After
	public void tearDown(){
		 osgi.dispose();
	     ipojo.dispose();
	}

	/**
	 * Test test;
	 */
	@Test
	public void compoByURL_Name(){
		 apam = (Apam) osgi.getServiceObject(Apam.class.getName(), null);
		 System.out.println("Starting compoByURL_Name");

		 try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertNotNull(apam);

		System.out.println("Starting tests and apam ready");
	        // examples of the different ways to create and start an application in apam.
	        // The easiest way is as done for starting this class: "implements Runnable, ApamComponent".
	        // Once loaded in OSGi, it starts in a root composite automatically created for it.

		System.out.println("providing an URL leading to the bundle to start. It must contain the main implementation S2Simple");
	        File bundle = new File("https://repository-apam.forge.cloudbees.com/snapshot/fr/imag/adele/apam/S2Impl/0.0.1-SNAPSHOT/S2Impl-0.0.1-20120810.041326-9.jar");
	        URL theUrl = null;
	        try {
	            theUrl = bundle.toURI().toURL();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		 	CompositeType appliTestURL = apam.createCompositeType(null,  "TestURL", "S2Simple", /* models */null, theUrl, /*specName*/null, null);
		 	assertNotNull(appliTestURL);
	        
		 	System.out.println("testing findImplByName in ASM");
		 	Implementation implem = CST.apamResolver.findImplByName(null,"S1toS2Final");
		 	assertNotNull(implem);
		 	
		 	System.out.println("testing findImplByName in OBR");
		 	Implementation implem2 = CST.apamResolver.findImplByName(null,"S2Final");
		 	assertNotNull(implem2);

		 	System.out.println("testing a root createCompositeType by name existing in ASM. will call S2Final.");
		 	CompositeType appliTest00 = apam.createCompositeType(null,  "Test00", "S1toS2Final", null,null);
		 	assertNotNull(appliTest00);
		 	
//	        CompositeType appliTest00 = apam.createCompositeType(null, "Test00", "S2Simple", null /* models */, theUrl,
//	                null /* specName */, null /* properties */);
		 	
		 	System.out.println("testing create instance root");
	        Instance a = appliTest00.createInstance(null /* composite */, null/* properties */);
	        assertNotNull(a);
	        
	        System.out.println("testing call to S2Simple");
	        S2 s02 = (S2) a.getServiceObject();
	        s02.callS2("createAppli by API by name. Should call S2Final.");

	        System.out.println("=====================================\nend test S1toS2Final \n\n");
	}
	
	@Test
	public void thingToTest(){
		 apam = (Apam) osgi.getServiceObject(Apam.class.getName(), null);
		 System.out.println("Starting thingToTest");

		 try {
			Thread.currentThread().sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotNull(apam);


	        System.out.println("=====================================\nA composite inside another composite\n\n");

	        // testing initial properties
	        Map<String, String> props = new HashMap<String, String>();
	        props.put("testMain", "valeurTestMain"); // not declared
	        props.put("scope", "5"); // redefined
	        props.put("impl-name", "5"); // final
	        props.put("location", "living"); // ok
	        props.put("location", "anywhere"); // value not defined

		 	//testing a root createCompositeType by name existing in ASM. will call S2Final.");
		 	CompositeType appliTest00 = apam.createCompositeType(null,  "Test00", "S1toS2Final", null,null);
		 	assertNotNull(appliTest00);

		 	//testing create instance root");
	        Instance a = appliTest00.createInstance(null /* composite */, null/* properties */);
	        assertNotNull(a);
	        Instance test00_instance0 = appliTest00.createInstance((Composite) a /* composite */, props/* properties */);
	        System.err.println("composite in composite same type !! " + test00_instance0 + "\n\n\n");

	        // Creation of an application TestS1 which main implementation is called "S1Simple".");
	        // The system will try to find an implementation called "S1Simple" (found by OBR in this example)
	        CompositeType appli3 = apam
	        .createCompositeType(null, "TestS1", "S1Simple", null /* models */, null /* properties */);
	        // fails since S1Simple does not exist");

	        System.err.println("Composite type TestS1 inside composite type Test00");
	        appli3 = apam.createCompositeType("Test00", "TestS1", "S1Impl", null /* models */, null /* properties */);

	        System.err.println("Root Composite types have no spec. This declarations is false (no definition), but done.");
	        appli3.setProperty("location", "no spec for application root");

	        System.err.println(" Testing attributes on main implem declared: location = {living, kitchen, bedroom}");
	        Implementation impl = appli3.getMainImpl();
	        impl.setProperty("location", "living"); // good
	        impl.setProperty("location", "Living"); // error: Value is case sensitive
	        impl.setProperty("location", "anywhere"); // error: false value
	        impl.setProperty("testMain", "valeurTestMain"); // error: not defined
	        impl.setProperty("scope", "5"); // error: redefined
	        impl.setProperty("impl-name", "5"); // error: final attribute

	        System.out.println("\n\n=====================================\n"
	                + " Alternatively, a specification (S1) can be provided instead; it is resolved to find the main implem.\n");

	        // The system will look for an atomic implementations of "S1" as the main implementation
	        appli3 = apam.createCompositeType(null, "TestS1Bis", "S1", null /* models */, null /* properties */);

	        // Create an instance of that composite type
	        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);

	        // Calling that application from an external program (this instance).
	        S1 s11 = (S1) test00_instance0.getServiceObject();
	        s11.callS1("createAppli-1");

	        // setting visibilities
	        // composite a3 should not be shared
	        test00_instance0.setProperty(CST.A_SHARED, CST.V_FALSE);

	        System.out.println("\n\n===================================== Testing promotions\n"
	                + " creating composite on S1 containing an S2 composite \n");

	        props.clear();
	        props.put(CST.A_LOCALIMPLEM, CST.V_TRUE);
	        props.put(CST.A_LOCALINSTANCE, CST.V_TRUE);
	        props.put(CST.A_BORROWIMPLEM, CST.V_FALSE);
	        props.put(CST.A_BORROWINSTANCE, CST.V_FALSE);

	        CompositeType mainCompo = apam.createCompositeType("Test00", "TestS1Promotions", "S1Main", null /* models */,
	                props);

	        System.err.println("TestS1Promotions inside Test00 and black box");
	        mainCompo.createInstance((Composite) test00_instance0, props);

	        System.out.println("\n\n\n========================== deuxieme exec ===");
	        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);

	        // Calling that application from an external program (this instance).
	        s11 = (S1) test00_instance0.getServiceObject();
	        s11.callS1("createAppli-2");


	        System.out.println("\n\n\n== troisieme exec ===");
	        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
	        // Calling that application from an external program (this instance).
	        s11 = (S1) test00_instance0.getServiceObject();
	        s11.callS1("createAppli-3");
	}

	

	
}