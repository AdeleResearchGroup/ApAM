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
package fr.imag.adele.apam.test.testcases;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.imag.adele.apam.mainApam.MainApam;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JackyTesting extends ExtensionAbstract {

	@Override
	public List<Option> config() {
		List<Option> obrmanconfig = super.config(null, true);

        obrmanconfig.add(frameworkProperty(Apam.EXPECTED_MANAGERS).value("OBRMAN"));
        obrmanconfig.add(frameworkProperty(Apam.CONFIGURATION_MANAGERS).value("OBRMAN:file:./conf/test.obrman.config"));

		return obrmanconfig;
	}

	@Before
	public void constructor() {

	}

	@Before
	@Override
	public void setUp() {
		super.setUp();

		waitForInstByName(null, "OBRMAN-Instance");

	}


	class InstanceCreator extends Thread {

		String myCompositeName;
		public Instance created;

		public InstanceCreator(String compositeName) {
			myCompositeName = compositeName;
			created = null;
		}

		@Override
		public void run() {
			CompositeType compo = (CompositeType) waitForComponentByName(null,
					myCompositeName);			
			if (compo != null) {
				created = compo.createInstance(null, null);
			}
		}
	}

    private void initmainApamInstance() {
        Implementation implM = CST.apamResolver.findImplByName(null,"M");
        Assert.assertNotNull("M implementation not found", implM);

        Instance instM = implM.createInstance(null,null);
        Assert.assertNotNull("M instance note created", instM);

        MainApam serviceObject = (MainApam) instM.getServiceObject();


    }

    private void initperfWireInstance() {
        Implementation implW = CST.apamResolver.findImplByName(null,"W");
    }


	@Test
	public void testReactionSimple_tct041() {

        System.out.println("=========== start testReaction test Simple");

        System.out.println("creating instance");
        Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
        implS1.createInstance(null, null);


//        Instance test = CST.componentBroker.getInstService(testSimple) ;
//        System.out.println("connected to " + test.getName());

	}	
}