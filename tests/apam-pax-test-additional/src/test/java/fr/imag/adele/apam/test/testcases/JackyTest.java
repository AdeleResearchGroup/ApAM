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


import fr.imag.adele.apam.*;
import fr.imag.adele.apam.mainApam.perfWire;
import fr.imag.adele.apam.test.MainApamSpec;
import fr.imag.adele.apam.test.PerfWireSpec;
import fr.imag.adele.apam.test.s1.S1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

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
        mapOfRequiredArtifacts.put("TestAttrSpec",
                "fr.imag.adele.apam");
        mapOfRequiredArtifacts.put("MainApam",
                "fr.imag.adele.apam");
        mapOfRequiredArtifacts.put("S1",
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

    private perfWire initperfWireInstance() {
        Implementation implW = CST.apamResolver.findImplByName(null,"W");

        Instance instW = implW.createInstance(null,null);
        Assert.assertNotNull("W instance not created", instW);

        perfWire serviceObject = (perfWire) instW.getServiceObject();
        Assert.assertNotNull("perfWire service object created successfully", serviceObject);

        return serviceObject;
    }


    @Test
    public void testReaction_tct041() {
        waitForApam();

        System.out.println("=========== start testReaction test");

        perfWire perfWire = initperfWireInstance();

        System.out.println("creating 2 instances");
        Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
        Instance s1 = implS1.createInstance(null, null);
        Instance s2 = implS1.createInstance(null, null);


        S1 testReaction = perfWire.getFieldTestReaction();
        Assert.assertNotNull("S1 field testReaction not resoved", testReaction);

        Instance test = CST.componentBroker.getInstService(testReaction) ;
        System.out.println("connected to " + test.getName());
        if (test == s1)
            s2.setProperty("debit", 100) ;
        else
            s1.setProperty("debit", 100) ;

        Instance thisInstance = perfWire.getFieldThisInstance();
        Assert.assertNotNull("thisInstance field testReaction not resoved", thisInstance);

        thisInstance.setProperty("need", 20) ;
        test = CST.componentBroker.getInstService(testReaction) ;
        System.out.println("connected to " + test.getName());
    }

    @Test
    public void testPerfLink () {

        System.out.println("=========== start testPerfLink test");
        waitForApam();
        perfWire perfWire = initperfWireInstance();


        Implementation impl = CST.apamResolver.findImplByName(null, "S2Impl");

        long overHead = 0;
        long fin;
        long duree;
        long deb;
        int nb = 1000;
        int nbInst = 0;

        System.out.println("creating 2 instances");
        Implementation implS1 = CST.apamResolver.findImplByName(null, "S1ImplEmpty");
        implS1.createInstance(null, null);
        nbInst++;
        implS1.createInstance(null, null);
        nbInst++;

        Instance test = null;

        String s;
        Link l;
        deb = System.nanoTime();
        for (int i = 0; i < nb; i++) {
            test = CST.componentBroker.getInstService(perfWire.getFieldSimpleDep());
            test.setProperty("debit", 2);
            s = perfWire.getFieldSimpleDep().getName();
            test.setProperty("debit", 10);
        }
        fin = System.nanoTime();
        duree = (fin - deb - overHead);
        System.out.println("Nombre d'instances " + nbInst + " : duree de " + nb + " appels avec contrainte et changement de dep : " + duree / 1000000 + " milli secondes");


    }



}