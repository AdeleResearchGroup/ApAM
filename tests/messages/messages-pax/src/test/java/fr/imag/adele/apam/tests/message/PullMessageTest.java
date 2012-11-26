package fr.imag.adele.apam.tests.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.options;

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
    public Option[] apamConfig() {
        Option[] optionHerited = config();
        Option[] wireAdmin =  options(
                //wireAdmin
                bundle("file:/" + PathUtils.getBaseDir() +"/bundle/wireadmin.jar")
               );
        Option[] r = OptionUtils.combine(optionHerited, wireAdmin);
        return r;
    }
    
    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst() {
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
    
        MyProducer p = (MyProducer) producerInst.getServiceObject();
        
        M1 m = p.produceM1(null);

        assertEquals(queue.poll(),m);
        
    }

    /**
     * A producer and a deployed pull consumer
     * verify wiring
     */
//    @Test
    public void pullConsumerAlreadyDeployed() {
        apam.waitForIt(100);

    }

    /**
     * A pull consumer and a deployed producer
     * verify wiring
     */
//    @Test
    public void producerAlreadyDeployed1() {
        apam.waitForIt(100);

    }

    /**
     * 
     */
//    @Test
    public void testDeployAndRunProducer() {
        apam.waitForIt(100);

    }

}