package fr.imag.adele.apam.test;

import static fr.imag.adele.apam.test.helpers.ApAMHelper.waitForIt;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.IOException;

import static junit.framework.Assert.*;

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
//http://junit.sourceforge.net/javadoc/org/junit/Assert.html
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.assertNull;
import fr.imag.adele.apam.test.helpers.ApAMHelper;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class MessageTests {
//	
    @Inject
    protected BundleContext context;


    private ApAMHelper      apam;

    /**
     * Done some initializations.
     */
    @Before
    public void setUp() {
       
        apam = new ApAMHelper(context);
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
        // r = OptionUtils.combine(r, debug);

        // Option[] log = options(vmOption("-Dlog4j.file=./am.log4j.properties"));
        // r = OptionUtils.combine(r, log);
        return r;
    }

    @After
    public void tearDown() {
        apam.dispose();
    }

    
    
    @Test
    public void testRootModel(){
        waitForIt(100);
        int sizebefaore = apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE).size();
        try {
            apam.setObrManInitialConfig("wrongfilelocation",null,1);
            fail("wrongfilelocation");
        } catch (IOException e) {
            assertEquals(sizebefaore, apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE).size());
        }       
    }
    
    /**
     * Deploy a producer first , then instantiate
     * This should deploy a consumer, instantiate and wire it to the producer  
     */
    @Test
    public void testRunProducerFirst(){
        waitForIt(100);
        
    }
    

    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst(){
        waitForIt(100);
        
    }
    
    /**
     * Deploy a pull consumer first , then instantiate
     * This should do nothing 
     */
    @Test
    public void testRunPushConsumerFirst(){
        waitForIt(100);
        
    }
    
    
    /**
     * A producer and a deployed push consumer
     * verify wiring
     */
    @Test
    public void pushConsumerAlreadyDeployed(){
        waitForIt(100);
        
    }
    
    
    /**
     * A producer and a deployed pull consumer
     * verify wiring
     */
    @Test
    public void pullConsumerAlreadyDeployed(){
        waitForIt(100);
        
    }
    
    /**
     * A pull consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed1(){
        waitForIt(100);
        
    }
    
    /**
     * A push consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed2(){
        waitForIt(100);
        
    }
    
    
    
    
    /**
     * 
     */
    @Test
    public void testDeployAndRunProducer(){
        waitForIt(100);
        
    }
 
}