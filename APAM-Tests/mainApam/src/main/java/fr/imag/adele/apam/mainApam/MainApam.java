package fr.imag.adele.apam.mainApam;

//import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
//import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class MainApam implements Runnable, ApamComponent {
    // injected
    Apam apam;

    public void run() {
        System.out.println("Starting new mainApam");

        // examples of the different ways to create and start an application in apam.
        // The easiest way is as done for starting this class: "implements Runnable, ApamComponent".
        // Once loaded in OSGi, it starts in a root composite automatically created for it.

		System.out.println("providing an URL leading to the bundle to start. It must contain the main implementation S2Simple");
        File bundle = new File("http://repository-apam.forge.cloudbees.com/snapshot/fr/imag/adele/apam/S2Impl/0.0.1-SNAPSHOT/S2Impl-0.0.1-20120810.041326-9.jar");
        URL theUrl = null ;
        try {
        	theUrl = new URL("http://repository-apam.forge.cloudbees.com/snapshot/fr/imag/adele/apam/S2Impl/0.0.1-SNAPSHOT/S2Impl-0.0.1-20120810.041326-9.jar") ;
           // theUrl = bundle.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }
	 	CompositeType appliTestURL = apam.createCompositeType(null,  "TestURL", "S2Simple", /* models */null, theUrl, /*specName*/null, null);
	 	assert(appliTestURL != null);
        
	 	System.out.println("testing findImplByName in OBR");
	 	Implementation implem = CST.apamResolver.findImplByName(null,"S1toS2Final");
	 	assert(implem != null);
	 	
	 	System.out.println("Deploying S1Impl bundle should deploy also the implems and composites. Composite SICompoFinal is created and started.");
	 	System.out.println("shoud have the message \"S1toS2Final is sarted\" ");
	 	
	 	System.out.println("testing findImplByName in ASM and unused");
	 	Implementation implem2 = CST.apamResolver.findImplByName(null,"S2Final");
	 	assert(implem2!= null);

	 	System.out.println("testing findImplByName in ASM  and used");
	 	implem2 = CST.apamResolver.findImplByName(null,"S2Simple");
	 	assert(implem2!= null);

	 
	 	System.out.println("testing a root createCompositeType by name existing in ASM. will call S2Final.");
	 	CompositeType appliTest00 = apam.createCompositeType(null,  "Test00", "S1toS2Final", null,null);
	 	assert(appliTest00!= null);
	 		 	
	 	System.out.println("testing create instance root");
        Instance a = appliTest00.createInstance(null /* composite */, null/* properties */);
        assert(a!= null);
        
        System.out.println("testing call to S1toS2Final");
        S1 s01 = (S1) a.getServiceObject();
        s01.callS1("createAppli by API by name. Should call S2Final.");


        System.out.println("=====================================\nend test url\n\n");

        System.out.println("=====================================\nA composite inside another composite\n\n");

        // testing initial properties
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("testMain", "valeurTestMain"); // not declared
        props.put("scope", "5"); // redefined
        props.put("impl-name", "5"); // final
        //props.put("location", "living"); // ok
        props.put("location", "anywhere"); // value not defined

        Instance test00_instance0 = appliTest00.createInstance((Composite) a /* composite */, props/* properties */);
        System.err.println("composite in composite same type !! " + test00_instance0 + "\n\n\n");

        // Creation of an application TestS1 which main implementation is called "S1Simple".
        // The system will try to find an implementation called "S1Simple" (found by OBR in this example)
        CompositeType appli3 = apam
        .createCompositeType(null, "TestS1", "S1Simple", null /* models */, null /* properties */);
        // fails since S1Simple does not exist

        System.err.println("Composite type TestS1 inside composite type Test00");
        appli3 = apam.createCompositeType("Test00", "TestS1", "S1Impl", null /* models */, null /* properties */);

//        System.err.println("Root Composite types have no spec. This declarations is false (no definition), but done.");
        appli3.setProperty("location", "no spec for application root");

        System.err.println(" Testing attributes on main implem declared: location = {living, kitchen, bedroom}");
        Implementation impl = appli3.getMainImpl();
        impl.setProperty("location", "living"); // good
        impl.setProperty("location", "Living"); // error: Value is case sensitive
        impl.setProperty("location", "anywhere"); // error: false value
        impl.setProperty("testMain", "valeurTestMain"); // error: not defined
        impl.setProperty("scope", "5"); // error: redefined
        impl.setProperty("impl-name", "5"); // error: final attribute

        System.out.println("\n\n=====================================\n"
                + " Alternatively, a specification (S1) can be provided instead; it is resolved to find the main implem.\n");

        // The system will look for an atomic implementations of "S1" as the main implementation
        appli3 = apam.createCompositeType(null, "TestS1Bis", "S1", null /* models */, null /* properties */);

        // Create an instance of that composite type
        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);

        // Calling that application from an external program (this instance).
        S1 s11 = (S1) test00_instance0.getServiceObject();
        s11.callS1("createAppli-1");

        // setting visibilities
        // composite a3 should not be shared
        test00_instance0.setProperty(CST.A_SHARED, CST.V_FALSE);

        System.out.println("\n\n===================================== Testing promotions\n"
                + " creating composite on S1 containing an S2 composite \n");

        props.clear();
        props.put(CST.A_LOCALIMPLEM, CST.V_TRUE);
        props.put(CST.A_LOCALINSTANCE, CST.V_TRUE);
        props.put(CST.A_BORROWIMPLEM, CST.V_FALSE);
        props.put(CST.A_BORROWINSTANCE, CST.V_FALSE);

        CompositeType mainCompo = apam.createCompositeType("Test00", "TestS1Promotions", "S1Main", null /* models */,
                props);

        System.err.println("TestS1Promotions inside Test00 and black box");
        mainCompo.createInstance((Composite) test00_instance0, props);

        System.out.println("\n\n\n========================== deuxieme exec ===");
        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);

        // Calling that application from an external program (this instance).
        s11 = (S1) test00_instance0.getServiceObject();
        s11.callS1("createAppli-2");


        System.out.println("\n\n\n== troisieme exec ===");
        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
        // Calling that application from an external program (this instance).
        s11 = (S1) test00_instance0.getServiceObject();
        s11.callS1("createAppli-3");

    }


    public void apamStart(Instance apamInstance) {
        new Thread(this, "APAM test").start();
    }

    public void apamStop() {

    }

    public void apamRelease() {

    }

    // Composite s2Compo0 = apam.getComposite("S2Compo-0");
    // if (s2Compo0 == null)
    // System.err.println("s2Compo-0 is null ??!!");
    // s2Compo0.put(CST.A_SHARED, CST.V_FALSE);
    //
    // Map<String, Object> props = new HashMap<String, Object>();

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
    // CompositeType s2Compo = apam.getCompositeType("S2Compo");
    // if (s2Compo == null)
    // System.err.println("s2Compos is null ??!!");

    /* Take properties form metadata
            s2Compo.setProperties(props.getProperties());
     */

    /* these properties are provided in the xml
    // properties for S2Compo instances.
    // A different instance for each use.
    props.put(CST.A_LOCALSCOPE, new String[] { ".*" });
    // All instance must pertain to S2Compo instance
    props.put(CST.A_INTERNALINST, CST.V_TRUE);

    // for s2Compo type
    // Implementations matching "S*Impl*" i.e. all, are not visible from outside
    props.put(CST.A_LOCALVISIBLE, new String[] { "S*Impl*" });
    // All implementations must pertain to S2Compo
    props.put(CST.A_INTERNALIMPL, CST.V_TRUE);

    CompositeType s2Compo = apam.getCompositeType("S2Compo");
    if (s2Compo == null)
        System.err.println("s2Compos is null ??!!");
    s2Compo.putAll(props);
     */

}
