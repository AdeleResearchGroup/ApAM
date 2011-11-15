package fr.imag.adele.apam.mainApam;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.apamImpl.CST;
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
        Instance a3 = appli3.createInst(null /* composite */, null/* properties */);
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

    }

    @SuppressWarnings("unused")
    private void start() {
        new Thread(this, "APAM test").start();
    }

    public void stop() {

    }
}
