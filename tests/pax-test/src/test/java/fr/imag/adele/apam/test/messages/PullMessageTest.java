/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.test.messages;

import static org.ops4j.pax.exam.CoreOptions.bundle;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.util.PathUtils;

import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class PullMessageTest extends ExtensionAbstract {

    
    @Override
    @Configuration
    public Option[] apamConfig() {
        List<Option> optionHerited = config();
        
        optionHerited.add(bundle("file:/" + PathUtils.getBaseDir() +"/bundle/wireadmin.jar"));
        
        return optionHerited.toArray(new Option[0]);
    }
    
    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst() {
       /* Implementation consumerImpl = CST.apamResolver.findImplByName(null,
                "C1Impl-Simple");

        assertNotNull(consumerImpl);

        Instance consumerInst1 = consumerImpl.createInstance(null, null);
*/
       /* C1ImplData c1Data = (C1ImplData) consumerInst1.getServiceObject();
        
        Queue<M1> queue = c1Data.getQueue();
       
        assertNotNull(queue);
        
        assertEquals(consumerInst1.getWires().size(),1);
        
        Instance producerInst = (Instance) (consumerInst1.getWires().toArray(new Wire[0])[0]).getDestination();
        
        assertNotNull(producerInst);
    
        MyProducer p = (MyProducer) producerInst.getServiceObject();
        
        M1 m = p.produceM1(null);

        assertEquals(queue.poll(),m);*/
        
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