package fr.imag.adele.apam.mainApam;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
//import java.util.HashMap;
//import java.util.Map;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
//import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class MainApam implements Runnable, ApamComponent {
    // injected
    Apam apam;

    /*
     *  Apam injected

    S1 s1;
    S5 s5;
     */
    public void run() {
        System.out.println("Starting mainApam");

        // examples of the different ways to create and start an application in apam.
        // The easiest way is as done for starting this class: "implements Runnable, ApamComponent".
        // Once loaded in OSGi, it starts in a root composite automatically created for it.

        // providing an URL leading to the bundle to start. It must contain the main implementation "S2Simple"
        File bundle = new File("F:/APAM/APAM-Tests/S2Impl/target/S2Impl-0.0.1-SNAPSHOT.jar");
        URL theUrl = null;
        try {
            theUrl = bundle.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }
        CompositeType appli = apam.createCompositeType("Test00", "S2Simple", null /* models */, theUrl,
                null /* specName */, null /* properties */);
        Instance a = appli.createInst(null /* composite */, null/* properties */);
        S2 s02 = (S2) a.getServiceObject();
        s02.callS2("createAppli by URL");

        System.out.println("=====================================\nend test url\n\n");

        System.out.println("=====================================\nA composite inside another composite\n\n");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("testMain", "valeurTestMain");
        props.put("scope", "5");
        props.put("impl-name", "5");

        Instance a3 = appli.createInst((Composite) a /* composite */, props/* properties */);
        return;
        // Creation of an application TestS1 which main implementation called "S1Simple".
        // The system will ask all its managers to find implementation "S1Simple" (found by OBR in this example)
        //        CompositeType appli3 = apam
        //        .createCompositeType("TestS1", "S1Simple", null /* models */, null /* properties */);
        //        // S1Simple does not exist
        //        appli3 = apam.createCompositeType("TestS1", "S1Main", null /* models */, null /* properties */);
        //
        //        System.out.println("\n\n=====================================\n"
        //                + " Alternatively, a specification can be provided instead; it is resolved to find the main implem.\n");
        //        appli3 = apam.createCompositeType("TestS1Bis", "S1", null /* models */, null /* properties */);
        //
        //        // Create an instance of that composite type
        //        a3 = appli3.createInst(null /* composite */, null/* properties */);
        //
        //        // Calling that application from an external program (this instance).
        //        S1 s11 = (S1) a3.getServiceObject();
        //        s11.callS1("createAppli-1");
        //
        //        // setting visibilities
        //        // composite a3 should not be shared
        //        a3.put(CST.A_SHARED, CST.V_FALSE);
        //        //        Composite s2Compo0 = apam.getComposite("S2Compo-0");
        //        //        if (s2Compo0 == null)
        //        //            System.err.println("s2Compo-0 is null ??!!");
        //        //        s2Compo0.put(CST.A_SHARED, CST.V_FALSE);
        //        //
        //        //        Map<String, Object> props = new HashMap<String, Object>();
        //
        //        /* This properties have been declared in the S2Compo metadata definition
        //         * 
        //        Attributes props = new AttributesImpl();
        //        // properties for S2Compo instances. A different instance for each use.
        //        props.setProperty(CST.A_LOCALSCOPE, new String[] {".*"});
        //        props.setProperty(CST.A_INTERNALINST, CST.V_TRUE);
        //        // for s2Compo type
        //        props.setProperty(CST.A_LOCALVISIBLE, new String[] {"S.Im.*"});
        //        props.setProperty(CST.A_INTERNALIMPL, CST.V_TRUE);
        //
        //         */
        //        //        CompositeType s2Compo = apam.getCompositeType("S2Compo");
        //        //        if (s2Compo == null)
        //        //            System.err.println("s2Compos is null ??!!");
        //
        //        /* Take properties form metadata
        //        s2Compo.setProperties(props.getProperties());
        //         */
        //
        //        System.out.println("\n\n\n========================== deuxieme exec ===");
        //        a3 = appli3.createInst(null /* composite */, null/* properties */);
        //
        //        // Calling that application from an external program (this instance).
        //        s11 = (S1) a3.getServiceObject();
        //        s11.callS1("createAppli-2");
        //
        //        /* these properties are provided in the xml
        //                // properties for S2Compo instances.
        //                // A different instance for each use.
        //                props.put(CST.A_LOCALSCOPE, new String[] { ".*" });
        //                // All instance must pertain to S2Compo instance
        //                props.put(CST.A_INTERNALINST, CST.V_TRUE);
        //
        //                // for s2Compo type
        //                // Implementations matching "S*Impl*" i.e. all, are not visible from outside
        //                props.put(CST.A_LOCALVISIBLE, new String[] { "S*Impl*" });
        //                // All implementations must pertain to S2Compo
        //                props.put(CST.A_INTERNALIMPL, CST.V_TRUE);
        //
        //                CompositeType s2Compo = apam.getCompositeType("S2Compo");
        //                if (s2Compo == null)
        //                    System.err.println("s2Compos is null ??!!");
        //                s2Compo.putAll(props);
        //         */
        //
        //        System.out.println("\n\n\n== troisieme exec ===");
        //        a3 = appli3.createInst(null /* composite */, null/* properties */);
        //        // Calling that application from an external program (this instance).
        //        s11 = (S1) a3.getServiceObject();
        //        s11.callS1("createAppli-3");

    }


    public void apamStart(Instance apamInstance) {
        new Thread(this, "APAM test").start();
    }

    public void apamStop() {

    }

    public void apamRelease() {

    }
}
