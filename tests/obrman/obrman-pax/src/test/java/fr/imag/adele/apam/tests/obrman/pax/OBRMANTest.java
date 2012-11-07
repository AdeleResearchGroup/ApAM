package fr.imag.adele.apam.tests.obrman.pax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.app1.spec.App1Spec;
import fr.imag.adele.apam.app2.spec.App2Spec;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * Test Suite
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OBRMANTest extends ExtensionAbstract{

  
    /**
     * Done some initializations.
     */
    
    @Test 
    public void testRootModel() {
        apam.waitForIt(1000);
        int sizebefaore = apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE)
                .size();
        try {
            apam.setObrManInitialConfig("wrongfilelocation", null, 1);
            fail("wrongfilelocation");
        } catch (IOException e) {
            assertEquals(sizebefaore,
                    apam.getCompositeRepos(CST.ROOT_COMPOSITE_TYPE).size());
        }
    }

    /**
     * Simple Test : Create a compositetype with obrman model and instantiate it
     * then call the application service This composite must contains only the
     * spec and the main impl of the composite
     */
    @Test
    public void simpleComposite() {
        apam.waitForIt(1000);
        try {
            String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/APPS/0.0.1-SNAPSHOT!/APPS-repo.xml" };
            apam.setObrManInitialConfig("rootAPPS", repos, 1);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        CompositeType app2CompoType = apam.createCompositeType("APP2",
                "APP2_MAIN", null);

        App2Spec app2Spec = apam.createInstance(app2CompoType, App2Spec.class);

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

        try {
            String[] repos = { "jar:mvn:fr.imag.adele.apam.tests.obrman.repositories/APPS/0.0.1-SNAPSHOT!/APPS-repo.xml" };
            apam.setObrManInitialConfig("rootAPPS", repos, 1);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        CompositeType app1CompoType = apam.createCompositeType("APP1",
                "APP1_MAIN", null);

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

        CompositeType app1CompoType = apam.createCompositeType("APP1.2",
                "APP1_MAIN", null);

        CompositeType root = (CompositeType) app1CompoType.getInCompositeType()
                .toArray()[0];

        assertEquals(2, root.getEmbedded().size()); // the root compositeType
                                                    // contains two composites

        App1Spec app1Spec = apam.createInstance(app1CompoType, App1Spec.class);

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
        apam.waitForIt(1000);
        apam.createCompositeType("APP1.2", "APP1_MAIN", null);
    }

}