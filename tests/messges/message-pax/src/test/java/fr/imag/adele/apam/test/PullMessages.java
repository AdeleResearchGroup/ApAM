package fr.imag.adele.apam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.message.M1;
import fr.imag.adele.apam.test.message.consumer.impl.C1ImplData;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(PerMethod.class)
public class PullMessages extends ExtensionAbstract {
//	

    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst() {
        apam.waitForIt(100);

        Implementation consumerImpl = CST.apamResolver.findImplByName(null,
                "C1Impl-Simple");

        assertNotNull(consumerImpl);

        Instance consumerInst1 = consumerImpl.createInstance(null, null);

        C1ImplData c1Data = (C1ImplData) consumerInst1.getServiceObject();
        
        Queue<M1> queue = c1Data.getQueue();
       
        assertNotNull(queue);
        
        assertEquals(consumerInst1.getWires().size(),1);
        
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