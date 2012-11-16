package fr.imag.adele.apam.tests.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.util.Arrays;
import java.util.Queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.util.PathUtils;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.test.message.M1;
import fr.imag.adele.apam.test.message.consumer.C1ImplData;
import fr.imag.adele.apam.test.message.producer.MyProducer;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class PullMessageTest extends ExtensionAbstract {

    
    
    @Configuration()
    public static Option[] apamConfig1() {
        
        
        return options(
                systemProperty("org.osgi.service.http.port").value("8080"),
                cleanCaches(),
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir() + "/log/logback.xml"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
                        .value("NONE"),
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
                        .artifactId("apam-bundle").version("0.0.1-SNAPSHOT"),
                mavenBundle().groupId("fr.imag.adele.apam")
                        .artifactId("obrman").version("0.0.1-SNAPSHOT"),
                mavenBundle("org.slf4j", "slf4j-api").version("1.6.6"),
                mavenBundle("ch.qos.logback", "logback-core").version("1.0.7"),
                mavenBundle("ch.qos.logback", "logback-classic").version(
                        "1.0.7"),
                junitBundles(),
                bundle("file:/" + PathUtils.getBaseDir() +"/bundle/wireadmin.jar"),
                
                mavenBundle("fr.imag.adele.apam.tests", "apam-helpers")
                        .version("0.0.1-SNAPSHOT"),
                when(Boolean.getBoolean("isDebugEnabled"))
                        .useOptions(
                                vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
                                systemTimeout(0)));
        
//        Option[] optionHerited = apamConfig();
//        Option[] wireAdmin =  options(
//                //wireAdmin
//               );
//        Option[] r = OptionUtils.combine(optionHerited, wireAdmin);
//        return r;
    }
    
    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst() {
        apam.printBundleList();
        System.out.println(">>>> file://" + PathUtils.getBaseDir() +"/bundle/wireadmin.jar");
        Implementation consumerImpl = CST.apamResolver.findImplByName(null,
                "C1Impl-Simple");

        assertNotNull(consumerImpl);

        Instance consumerInst1 = consumerImpl.createInstance(null, null);

        C1ImplData c1Data = (C1ImplData) consumerInst1.getServiceObject();
        
        Queue<M1> queue = c1Data.getQueue();
       
        assertNotNull(queue);
        
        assertEquals(consumerInst1.getWires().size(),1);
        
        Instance producerInst = (Instance) (consumerInst1.getWires().toArray(new Wire[0])[0]).getDestination();
        
        assertNotNull(producerInst);
        apam.printBundleList();
        MyProducer p = (MyProducer) producerInst.getServiceObject();
        
        Message<M1> m = p.produceM1(null);

        
        System.out.println("Send " + m.getData());
        
        apam.waitForIt(2000);
        
        System.out.println("Received " + queue.poll());
        
        apam.waitForIt(2000);
        
        System.out.println("Received " + queue.poll());
        
//        assertEquals(queue.poll(),m.getData());
        
    }

    /**
     * A producer and a deployed pull consumer
     * verify wiring
     */
    @Test
    public void pullConsumerAlreadyDeployed() {
        apam.waitForIt(100);

    }

    /**
     * A pull consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed1() {
        apam.waitForIt(100);

    }

    /**
     * 
     */
    @Test
    public void testDeployAndRunProducer() {
        apam.waitForIt(100);

    }

}