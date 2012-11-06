package fr.imag.adele.apam.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;
/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class PullMessages extends ExtensionAbstract{
//	
  
    
    @Test
    public void testRootModel(){
        apam.waitForIt(100);
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
        apam.waitForIt(100);
        
    }
    

    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst(){
        apam.waitForIt(100);
        
    }
    
    /**
     * Deploy a pull consumer first , then instantiate
     * This should do nothing 
     */
    @Test
    public void testRunPushConsumerFirst(){
        apam.waitForIt(100);
        
    }
    
    
    /**
     * A producer and a deployed push consumer
     * verify wiring
     */
    @Test
    public void pushConsumerAlreadyDeployed(){
        apam.waitForIt(100);
        
    }
    
    
    /**
     * A producer and a deployed pull consumer
     * verify wiring
     */
    @Test
    public void pullConsumerAlreadyDeployed(){
        apam.waitForIt(100);
        
    }
    
    /**
     * A pull consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed1(){
        apam.waitForIt(100);
        
    }
    
    /**
     * A push consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed2(){
        apam.waitForIt(100);
        
    }
    
    
    
    
    /**
     * 
     */
    @Test
    public void testDeployAndRunProducer(){
        apam.waitForIt(100);
        
    }
 
}