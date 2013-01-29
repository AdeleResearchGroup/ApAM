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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
public class PushMessages extends ExtensionAbstract {
//	

    /**
     * Deploy a producer first , then instantiate
     * This should deploy a consumer, instantiate and wire it to the producer
     */
    @Test
    public void testRunProducerFirst() {
        apam.waitForIt(100);

    }

    /**
     * Deploy a pull consumer first , then instantiate
     * This should deploy a producer, instantiate and wire it to the consumer
     */
    @Test
    public void testRunPullConsumerFirst() {
        apam.waitForIt(100);

    }

    /**
     * Deploy a pull consumer first , then instantiate
     * This should do nothing
     */
    @Test
    public void testRunPushConsumerFirst() {
        apam.waitForIt(100);

    }

    /**
     * A producer and a deployed push consumer
     * verify wiring
     */
    @Test
    public void pushConsumerAlreadyDeployed() {
        apam.waitForIt(100);

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
     * A push consumer and a deployed producer
     * verify wiring
     */
    @Test
    public void producerAlreadyDeployed2() {
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