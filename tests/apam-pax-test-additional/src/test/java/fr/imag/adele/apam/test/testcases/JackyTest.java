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

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import fr.imag.adele.apam.test.MainApamSpec;
import fr.imag.adele.apam.test.PerfWireSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * Test Suite
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JackyTest extends ExtensionAbstract {

	@Override
	public List<Option> config() {

        Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();
        mapOfRequiredArtifacts.put("obrman",
                "fr.imag.adele.apam");
        mapOfRequiredArtifacts.put("MainApamSpec",
                "fr.imag.adele.apam");

		List<Option> obrmanconfig = super.config(mapOfRequiredArtifacts, true);

        obrmanconfig.add(frameworkProperty(Apam.EXPECTED_MANAGERS).value("OBRMAN"));
        obrmanconfig.add(frameworkProperty(Apam.CONFIGURATION_MANAGERS).value("OBRMAN:file:./conf/test.obrman.config"));

		return obrmanconfig;
	}

	@Before
	@Override
	public void setUp() {
		super.setUp();
        System.out.println("********** setting up");

		waitForInstByName(null, "OBRMAN-Instance");
        System.out.println("*********** OBRMAN is there");

	}

    private MainApamSpec initmainApamInstance() {
        Implementation implM = CST.apamResolver.findImplByName(null,"M");
        Assert.assertNotNull("M implementation not found", implM);

        Instance instM = implM.createInstance(null,null);
        Assert.assertNotNull("M instance not created", instM);

        MainApamSpec serviceObject = (MainApamSpec) instM.getServiceObject();
        Assert.assertNotNull("MainApam service object created successfully", serviceObject);

        return serviceObject;
    }

    private PerfWireSpec initperfWireInstance() {
        Implementation implW = CST.apamResolver.findImplByName(null,"W");

        Instance instW = implW.createInstance(null,null);
        Assert.assertNotNull("W instance not created", instW);

        PerfWireSpec serviceObject = (PerfWireSpec) instW.getServiceObject();
        Assert.assertNotNull("perfWire service object created successfully", serviceObject);

        return serviceObject;
    }


	@Test
	public void testReactionSimple_tct041() {
        waitForApam();

        System.out.println("=========== start testReaction test Simple");
//        initperfWireInstance();
//
//        System.out.println("creating instance");
//        Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
//        implS1.createInstance(null, null);


//        Instance test = CST.componentBroker.getInstService(testSimple) ;
//        System.out.println("connected to " + test.getName());

	}	
}