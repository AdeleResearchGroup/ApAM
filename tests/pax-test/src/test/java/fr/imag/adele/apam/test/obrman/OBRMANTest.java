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
package fr.imag.adele.apam.test.obrman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.app1.spec.App1Spec;
import fr.imag.adele.apam.app2.spec.App2Spec;
import fr.imag.adele.apam.test.testcases.RelationTest;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;
import fr.imag.adele.apam.util.CoreMetadataParser;
import fr.imag.adele.apam.util.CoreParser;

/**
 * Test Suite
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class OBRMANTest extends ExtensionAbstract{

	static OBRMANHelper obrmanhelper;
  
	@Before
	public void constructor(){
		obrmanhelper=new OBRMANHelper(context);
	}
	
	@Override
	public List<Option> config() {
		List<Option> obrmanconfig=super.config(null,true);
//		obrmanconfig.add(packApamObrMan());
		return obrmanconfig;
	}
	
    /**
     * Done some initializations.
     */
    
    @Test 
    public void testRootModel() {
	
    	obrmanhelper.waitForIt(1000);
    	auxListInstances();
        int sizebefore = obrmanhelper.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE)
                .size();
        try {
        	obrmanhelper.setObrManInitialConfig("wrongfilelocation", null, 1);
            fail("wrongfilelocation");
        } catch (IOException e) {
            assertEquals(sizebefore,
            		obrmanhelper.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE).size());
        }
    }

    /**
     * Simple Test : Create a compositetype with obrman model and instantiate it
     * then call the application service This composite must contains only the
     * spec and the main impl of the composite
     */
    @Test
    public void simpleComposite() {
    	obrmanhelper.waitForIt(1000);
        CompositeType app2CompoType = null;
        try {
            String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/public.repository/"+obrmanhelper.getMavenVersion()+"!/app-store.xml" };
            obrmanhelper.setObrManInitialConfig("rootAPPS", repos, 1);
            app2CompoType = obrmanhelper.createCompositeType("APP2",
                    "APP2_MAIN", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }

      

        App2Spec app2Spec = obrmanhelper.createInstance(app2CompoType, App2Spec.class);

        System.out
                .println("\n==================Start call test=================== \n");

        app2Spec.call("Call Main APP2 from Test");

        System.out
                .println("\n=================End call test====================\n");
    }


    /**
     * APP1 declare two repositories in ObrMan model The composite APP1 deploy
     * and instantiate the composite APP2 The composite APP2 will be inside the
     * composite APP1
     * 
     */
    @Test
    public void embeddedComposite() {
        apam.waitForIt(1000);
        CompositeType app1CompoType = null;
        
        try {
            String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/public.repository/"+obrmanhelper.getMavenVersion()+"!/app-store.xml" };
            obrmanhelper.setObrManInitialConfig("rootAPPS", repos, 1);
            app1CompoType = obrmanhelper.createCompositeType("APP1",
                    "APP1_MAIN", null);
            
            for(String s : repos)
                System.out.println("repo : "+s);

        } catch (IOException e) {

            fail(e.getMessage());
        }

        

        App1Spec app1Spec = apam.createInstance(app1CompoType, App1Spec.class);

        System.out
                .println("\n==================Start call test=================== \n");

        app1Spec.call("Call Main APP1 from Test");

        System.out
                .println("\n=================End call test====================\n");
    }

    /**
     * APP1 declare one repository and APP2 composite in ObrMan model create the
     * composite APP2 and call it create the composite APP1 which will call the
     * composite APP2 APP1 and APP2 will be on the same level of root composite.
     */
    @Test
    public void movedCompositev1() {
        apam.waitForIt(1000);

        simpleComposite();

        CompositeType app1CompoType = null;
        try {
            app1CompoType = obrmanhelper.createCompositeType("APP1.2",
                    "APP1_MAIN", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        CompositeType root = (CompositeType) app1CompoType.getInCompositeType()
                .toArray()[0];

        assertEquals(2, root.getEmbedded().size()); // the root compositeType
                                                    // contains two composites

        App1Spec app1Spec = obrmanhelper.createInstance(app1CompoType, App1Spec.class);

        System.out
                .println("\n==================Start call test=================== \n");

        app1Spec.call("Call Main APP1 from Test");

        System.out
                .println("\n=================End call test====================\n");

        assertEquals(1, app1CompoType.getEmbedded().size()); // app1 contains
                                                             // app2

        assertEquals(1, root.getEmbedded().size()); // the root compositeType
                                                    // contains two composites

    }

    /**
     * APP1 declare one repository and APP2 composite in ObrMan model Try to
     * create APP1 composite, but APP2 composite is missing
     */
    @Test
    public void missingAPP2Composite() {
    	obrmanhelper.waitForIt(1000);
        try {
        	obrmanhelper.createCompositeType("APP1.2", "APP1_MAIN", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void obrmanInstanciationWhenBundleInstalledNotStarted_tct005() {
    	obrmanhelper.waitForIt(1000);

    	Implementation implementation = waitForImplByName(null,
		"Obrman-Test-S3Impl");

	Assert.assertNotNull("Obrman-Test-S3Impl cannot be resolved (cannot be found using obrman)",implementation);

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());
	
	Assert.assertNotNull("Instance of Obrman-Test-S3Impl is null",instance);    	
	Bundle bundle= implementation.getApformComponent().getBundle();
	
	try {
	    bundle.stop();
	} catch (Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	}
	
    	implementation = waitForImplByName(null,
		"Obrman-Test-S3Impl");
    	
	Assert.assertNotNull("Obrman-Test-S3Impl cannot be resolved as bundle is not started",implementation);

	instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());
	
	Assert.assertNotNull("Instance of Obrman-Test-S3Impl is null",instance);    	

    	
    }
    
    @Test
    public void RelationLinkResolveExternal_tct003() {
	Implementation implementation = waitForImplByName(null,
		"S07-implementation-14ter");

	Instance instance = implementation.createInstance(null,
		Collections.<String, String> emptyMap());

	// Test should success on external bundle resolution
	auxListInstances();
	org.junit.Assert
		.assertFalse(
			"No exception should be raised as the dependency can be resolved externally",
			RelationTest.testResolutionExceptionCase14(instance, 3));
	Assert.assertEquals("Only one relation should have been created : ", 1,
		instance.getRawLinks().size());

    }
    

}