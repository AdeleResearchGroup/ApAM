package fr.imag.adele.apam.test;

import static fr.imag.adele.apam.test.ApAMHelper.waitForIt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

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

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.app1.spec.App1Spec;
import fr.imag.adele.apam.app2.spec.App2Spec;
//http://junit.sourceforge.net/javadoc/org/junit/Assert.html
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertNull;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class OBRMANTest {
//	
    @Inject
    protected BundleContext context;

    protected OSGiHelper    osgi;

    protected IPOJOHelper   ipojo;

    private ApAMHelper      apam;

    /**
     * Done some initializations.
     */
    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        apam = new ApAMHelper(context, osgi);
        // initialise the annoted mock object
//        MockitoAnnotations.initMocks(this);
    }

    @Configuration
    public static Option[] apamConfig() {
        
        Option[] platform = options(felix(), systemProperty(
                "org.osgi.service.http.port").value("8080"));
       
        Option[] bundles = options(provision(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo")
                        .versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers")
                        .versionAsInProject(),
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.bundlerepository")
                        .version(
                                "1.6.6"),
                mavenBundle().groupId("org.ops4j.pax.url").artifactId("pax-url-mvn").version("1.3.5"),
                mavenBundle().groupId("fr.imag.adele.apam").artifactId("APAMBundle").version("0.0.1-SNAPSHOT"),
                mavenBundle().groupId("fr.imag.adele.apam").artifactId("OBRMAN").version("0.0.1-SNAPSHOT"),

                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").version("1.6.6"),
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-log4j12").version("1.6.6"),
                mavenBundle().groupId("log4j").artifactId("log4j").version("1.2.17")

                ));

        Option[] r = OptionUtils.combine(platform, bundles);

        Option[] debug = options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));
        r = OptionUtils.combine(r, debug);

        // Option[] log = options(vmOption("-Dlog4j.file=./am.log4j.properties"));
        // r = OptionUtils.combine(r, log);
        return r;
    }

    @After
    public void tearDown() {
        osgi.dispose();
        ipojo.dispose();
    }

    /**
     * Simple Test : Create a compositetype with obrman model and instantiate it then call the application service
     * This composite must contains only the spec and the main impl of the composite
     */
    // @Test
    public void simpleComposite() {
        waitForIt(100);

        apam.setObrManInitialConfig("root");
        
        CompositeType app2CompoType = apam.runApplication("APP2", "APP2_MAIN", null);

        assertNotNull(app2CompoType);

        assertNotNull(app2CompoType.getMainImpl().getApformImpl());

        Composite instanceApp2 = (Composite) app2CompoType.createInstance(null, null);

        App2Spec app2Spec = (App2Spec) instanceApp2.getServiceObject();

        System.out.println("\n==================Start call test=================== \n");

        app2Spec.call("Call Main APP2 from Test");

        System.out.println("\n=================End call test====================\n");

        assertNotNull(app2CompoType);

    }

    /**
     * APP1 declare two repositories in ObrMan model
     * The composite APP1 deploy and instantiate the composite APP2
     * The composite APP2 will be inside the composite APP1
     * 
     */
    @Test
    public void embeddedComposite() {
        waitForIt(100);

        apam.setObrManInitialConfig("root");
        
        CompositeType app1CompoType = apam.runApplication("APP1", "APP1_MAIN", null);

        assertNotNull(app1CompoType);

        assertNotNull(app1CompoType.getMainImpl().getApformImpl());

        Composite instanceApp1 = (Composite) app1CompoType.createInstance(null, null);

        App1Spec app1Spec = (App1Spec) instanceApp1.getServiceObject();

        System.out.println("\n==================Start call test=================== \n");

        app1Spec.call("Call Main APP1 from Test");

        System.out.println("\n=================End call test====================\n");
    }

    /**
     * APP1 declare one repository and APP2 composite in ObrMan model
     * create the composite APP2 and call it
     * create the composite APP1 which will call the composite APP2
     * APP1 and APP2 will be on the same level of root composite.
     */
    // @Test
    public void friendCompositev1() {
        waitForIt(100);

        simpleComposite();

        CompositeType app1CompoType = apam.runApplication("APP1.2", "APP1_MAIN", null);

        assertNotNull(app1CompoType);

        assertNotNull(app1CompoType.getMainImpl().getApformImpl());

        Composite instanceApp1 = (Composite) app1CompoType.createInstance(null, null);

        App1Spec app1Spec = (App1Spec) instanceApp1.getServiceObject();

        System.out.println("\n==================Start call test=================== \n");

        app1Spec.call("Call Main APP1 from Test");

        System.out.println("\n=================End call test====================\n");

        assertEquals(app1CompoType.getEmbedded().size(), 0);

    }

    /**
     * APP1 declare one repository and APP2 composite in ObrMan model
     * Try to create APP1 composite, but APP2 composite is missing
     */
    // @Test
    public void missingAPP2Composite() {
        waitForIt(100);

        CompositeType app1CompoType = apam.runApplication("APP1.2", "APP1_MAIN", null);

        // Est ce que c'est normal que app1CompoType != null
        assertNull(app1CompoType.getMainImpl());

    }

//    public void thingToTest() {
//        apam = (Apam) osgi.getServiceObject(Apam.class.getName(), null);
//        System.out.println("Starting thingToTest");
//
//        try {
//            Thread.currentThread();
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        assertNotNull(apam);
//
//        System.out.println("=====================================\nA composite inside another composite\n\n");
//
//        // testing initial properties
//        Map<String, String> props = new HashMap<String, String>();
//        props.put("testMain", "valeurTestMain"); // not declared
//        props.put("scope", "5"); // redefined
//        props.put("impl-name", "5"); // final
//        props.put("location", "living"); // ok
//        props.put("location", "anywhere"); // value not defined
//
//        // testing a root createCompositeType by name existing in ASM. will call S2Final.");
//        CompositeType appliTest00 = apam.createCompositeType(null, "Test00", "S1toS2Final", null, null);
//        assertNotNull(appliTest00);
//
//        // testing create instance root");
//        Instance a = appliTest00.createInstance(null /* composite */, null/* properties */);
//        assertNotNull(a);
//        Instance test00_instance0 = appliTest00.createInstance((Composite) a /* composite */, props/* properties */);
//        System.err.println("composite in composite same type !! " + test00_instance0 + "\n\n\n");
//
//        // Creation of an application TestS1 which main implementation is called "S1Simple".");
//        // The system will try to find an implementation called "S1Simple" (found by OBR in this example)
//        CompositeType appli3 = apam
//                .createCompositeType(null, "TestS1", "S1Simple", null /* models */, null /* properties */);
//        // fails since S1Simple does not exist");
//
//        System.err.println("Composite type TestS1 inside composite type Test00");
//        appli3 = apam.createCompositeType("Test00", "TestS1", "S1Impl", null /* models */, null /* properties */);
//
//        System.err.println("Root Composite types have no spec. This declarations is false (no definition), but done.");
//        appli3.setProperty("location", "no spec for application root");
//
//        System.err.println(" Testing attributes on main implem declared: location = {living, kitchen, bedroom}");
//        Implementation impl = appli3.getMainImpl();
//        impl.setProperty("location", "living"); // good
//        impl.setProperty("location", "Living"); // error: Value is case sensitive
//        impl.setProperty("location", "anywhere"); // error: false value
//        impl.setProperty("testMain", "valeurTestMain"); // error: not defined
//        impl.setProperty("scope", "5"); // error: redefined
//        impl.setProperty("impl-name", "5"); // error: final attribute
//
//        System.out
//                .println("\n\n=====================================\n"
//                        + " Alternatively, a specification (S1) can be provided instead; it is resolved to find the main implem.\n");
//
//        // The system will look for an atomic implementations of "S1" as the main implementation
//        appli3 = apam.createCompositeType(null, "TestS1Bis", "S1", null /* models */, null /* properties */);
//
//        // Create an instance of that composite type
//        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
//
//        // Calling that application from an external program (this instance).
//        S1 s11 = (S1) test00_instance0.getServiceObject();
//        s11.callS1("createAppli-1");
//
//        // setting visibilities
//        // composite a3 should not be shared
//        test00_instance0.setProperty(CST.SHARED, CST.V_FALSE);
//
//        System.out.println("\n\n===================================== Testing promotions\n"
//                + " creating composite on S1 containing an S2 composite \n");
//
//        props.clear();
////        props.put(CST.A_LOCALIMPLEM, CST.V_TRUE);
////        props.put(CST.A_LOCALINSTANCE, CST.V_TRUE);
////        props.put(CST.A_BORROWIMPLEM, CST.V_FALSE);
////        props.put(CST.A_BORROWINSTANCE, CST.V_FALSE);
//
//        CompositeType mainCompo = apam.createCompositeType("Test00", "TestS1Promotions", "S1Main", null /* models */,
//                props);
//
//        System.err.println("TestS1Promotions inside Test00 and black box");
//        mainCompo.createInstance((Composite) test00_instance0, props);
//
//        System.out.println("\n\n\n========================== deuxieme exec ===");
//        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
//
//        // Calling that application from an external program (this instance).
//        s11 = (S1) test00_instance0.getServiceObject();
//        s11.callS1("createAppli-2");
//
//        System.out.println("\n\n\n== troisieme exec ===");
//        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
//        // Calling that application from an external program (this instance).
//        s11 = (S1) test00_instance0.getServiceObject();
//        s11.callS1("createAppli-3");
//    }

}