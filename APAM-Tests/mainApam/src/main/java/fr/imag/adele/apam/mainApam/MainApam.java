package fr.imag.adele.apam.mainApam;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s5.S5;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class MainApam implements Runnable, intTestApam {
    // iPOJO injected
    Apam apam;

    /*
     *  Apam injected
     

    S1 s1;
    S5 s5;
    */
    public void run() {
        System.out.println("Starting mainApam");

        /*
         * s1.calls1 () ;
         */
        // examples of the different ways to create and start an application in apam.

        // Creation of an application which main implementation called S1Impl provides an interface S1.
        // The system will ask all its managers to find implementation S1Impl (found by OBR in this example)
        CompositeType appli3 = apam.createCompositeType("TestS1", "S1Impl",
                null /* models */, null /* properties */);
        ASMInst a3 = appli3.createInst(null /* composite */, null/* properties */);
        // Calling that application from an external program (this instance).
        S1 s11 = (S1) a3.getServiceObject();
        s11.callS1("createAppli-1");

        // setting visibilities
        // composite a3 should not be shared
        a3.setProperty(CST.A_SHARED, CST.V_FALSE);
        Composite s2Compo0 = apam.getComposite("S2Compo-0");
        if (s2Compo0 == null)
            System.err.println("s2Compo-0 is null ??!!");
        s2Compo0.setProperty(CST.A_SHARED, CST.V_FALSE);

        Attributes props = new AttributesImpl();

        /* This properties have been declared in the S2Compo metadata definition
         * 
        Attributes props = new AttributesImpl();
        // properties for S2Compo instances. A different instance for each use.
        props.setProperty(CST.A_LOCALSCOPE, new String[] {".*"});
        props.setProperty(CST.A_INTERNALINST, CST.V_TRUE);
        // for s2Compo type
        props.setProperty(CST.A_LOCALVISIBLE, new String[] {"S.Im.*"});
        props.setProperty(CST.A_INTERNALIMPL, CST.V_TRUE);

        */
        CompositeType s2Compo = apam.getCompositeType("S2Compo");
        if (s2Compo == null)
            System.err.println("s2Compos is null ??!!");

        /* Take properties form metadata
        s2Compo.setProperties(props.getProperties());
        */
        System.out.println("\n\n\n== deuxieme exec ===");
        a3 = appli3.createInst(null /* composite */, null/* properties */);
        // Calling that application from an external program (this instance).
        s11 = (S1) a3.getServiceObject();
        s11.callS1("createAppli-2");

        // properties for S2Compo instances. A different instance for each use.
        props.setProperty(CST.A_LOCALSCOPE, new String[] { ".*" });
        props.setProperty(CST.A_INTERNALINST, CST.V_TRUE);
        // for s2Compo type
        props.setProperty(CST.A_LOCALVISIBLE, new String[] { "S*Impl*" });
        props.setProperty(CST.A_INTERNALIMPL, CST.V_TRUE);

        // CompositeType s2Compo = apam.getCompositeType("S2Compo");
        if (s2Compo == null)
            System.err.println("s2Compos is null ??!!");
        s2Compo.setProperties(props.getProperties());

        System.out.println("\n\n\n== troisieme exec ===");
        a3 = appli3.createInst(null /* composite */, null/* properties */);
        // Calling that application from an external program (this instance).
        s11 = (S1) a3.getServiceObject();
        s11.callS1("createAppli-3");

//        System.out.println("\n\napam.startAppli(\"S5CompEx\")");
//        System.out
//                .println("Creation from the name of an existing composite (will be found by managers : can be in Apam, SAM, OBR and so on). ");
//        // Creation from the name of an existing composite (will be found by managers : can be in Apam, SAM, OBR and so
//        // on).
//        Composite s5compo = apam.startAppli("S5CompEx");
//        // this composite implements interface S5 and waits to be called.
//        // a composite instance of type S5CompEx is created and s5 is resolved.
//        S5 s55 = (S5) s5compo.getServiceObject();
//        s55.callS5("called by s55. from S1 to S5");
//        // s5.callS5("called by resolved s5. from S1 to S5");
//
//        System.out.println("\n\napam.startAppli(\"AppliTest\")");
//        System.out.println("If main implementation start automatically (found in OBR in this exemple).");
//        // If main implementation start automatically (found in OBR in this exemple).
//        Composite appliTest = apam.startAppli("AppliTest");
//
//        try {
//            // Alternatively, directly from the bundle containing the composite
//            System.out.println("\n\napam.startAppli(compositeUrl, \"AppliTest\")");
//            System.out.println("Alternatively, directly from the bundle containing the composite");
//            URL compositeUrl = new File("F:/APAM/fr.imag.adele.apam.test.dependency/target/test.dependency-1.0.0.jar")
//                    .toURI().toURL();
//            apam.startAppli(compositeUrl, "AppliTest");
//
//            System.out.println("\n\napam.createCompositeType(\"monAppliADeployer\", \"DependencyTest\", ..");
//            System.out
//                    .println("creation of a composite type from an URL leading to a bundle containing the DependencyTest service implementation");
//            // creation of a composite type from an URL leading to a bundle containing the DependencyTest service
//            // implementation.
//            URL mainImplemUrl = new File("F:/APAM/fr.imag.adele.apam.test.dependency/target/test.dependency-1.0.0.jar")
//                    .toURI().toURL();
//            CompositeType appli1 = apam.createCompositeType("monAppliADeployer", "DependencyTest", /* models */null,
//                    mainImplemUrl, null /* specName */, null /* properties */);
//            apam.startAppli(appli1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // System.out.println("1 er appli deploy done");
//
//        System.out.println("\n\napam.createCompositeType(\"monAppliADeployer2\", \"DependencyTest\",");
//        System.out
//                .println("creation and start of the same application giving only its main implementation service name.");
//        // Creation and start of the same application giving only its main implementation service name.
//        // The system will ask all its managers to find an implementation called DependencyTest. found either in Apam,
//        // sam, OBR etc.
//        CompositeType appli2 = apam.createCompositeType("monAppliADeployer2", "DependencyTest",
//                 null /* models */, null /* properties */);
//        apam.startAppli(appli2);
//        // Note that appli1 and appli2 are different composites sharing the same main implementation.
//
//        // impossible car il y a un start iPOJO. On n'a pas le controle.
//        /*
//        System.out.println("\n\napam.createCompositeType(\"TestAuto\", \"MainApam\", ");
//        System.out
//                .println("It is the running instance that creates an application with itself as main implem and instance.");
//        //Auto creation.
//        //It is the running instance that creates an application with itself as main implem and instance.
//        // an iPOJO "bug" requires that the main class implements an interface (even if empty).
//        CompositeType appli4 = apam.createCompositeType("TestAuto", "MainApam", null, null);
//        System.out.println("application TestAuto created");
//        //Now this instance being an Apam instance, Apam will resolve s1 when used.
//        if (s1 == null) {
//            System.out.println("s1 is null");
//        }
//        apam.startAppli(appli4);
//        if (s1 == null) {
//            System.out.println("s1 is still null");
//        }
//        s1.callS1(" appel depuis main Apam");
//        */
    }

    @SuppressWarnings("unused")
    private void start() {
        new Thread(this, "APAM test").start();
    }

    public void stop() {

    }
}
