package fr.imag.adele.apam.apform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeImpl;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.ASMImpl.SamInstEventHandler;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class ApformImpl {

    static Set<ASMImpl>                     unusedImplems      = CompositeTypeImpl.getRootCompositeType().getImpls();
    static Set<ASMInst>                     unusedInsts        = CompositeImpl.getRootAllComposites().getContainInsts();

    // The managers are waiting for the apparition of an instance of the ASMImpl or implementing the interface
    // In both case, no ASMInst is created.
    static Map<String, Set<DynamicManager>> expectedImpls      = new HashMap<String, Set<DynamicManager>>();
    static Map<String, Set<DynamicManager>> expectedInterfaces = new HashMap<String, Set<DynamicManager>>();

    // registers the managers that are interested in services that disappear.
    static Set<DynamicManager>              listenLost         = new HashSet<DynamicManager>();

    public static ASMImpl getUnusedImplem(String name) {
        ASMImpl impl = CST.ASMImplBroker.getImpl(name);
        if (impl == null)
            return null;
        return (ApformImpl.unusedImplems.contains(impl)) ? impl : null;
    }

    /**
     * A bundle is under deployment, in which is located the implementation to wait.
     * The method waits until the implementation arrives and is notified by Apam-iPOJO.
     * 
     * @param expectedImpl the symbolic name of that implementation
     * @return
     */
    public ASMImpl getWaitImplementation(CompositeType compoType, String expectedImpl, Attributes properties) {
        if (expectedImpl == null)
            return null;
        // if allready here
        ASMImpl impl = CST.ASMImplBroker.getImpl(expectedImpl);
        if (impl != null)
            return impl;

        // not yet here. Wait for it.
        synchronized (ApformImpl.expectedImpls) {

            try {
                while (ApformImplementationImpl.expectedImpls.contains(expectedImpl)) {
                    this.wait();
                }
                // The expected impl arrived. It is in unUsed.
                impl = CST.ASMImplBroker.getImpl(expectedImpl);
                if (impl == null) // should never occur
                    System.out.println("wake up but imlementation is not present " + expectedImpl);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (properties != null)
                impl.setProperties(properties.getProperties());
            if (compoType != null)
                ApformImpl.moveImpl(impl, compoType);
            ApformImplementationImpl.apfImplems.remove(impl);
        }
        return impl;
    }

    /**
     * Provided an unused implementation, remove it from the unused list, and adds it in the provided composite.
     * 
     * @param impl
     * @param compoType
     */
    public static void moveImpl(ASMImpl impl, CompositeType compoType) {
        if (compoType == null) {
            System.err.println("compoType must not be null in moveImpl");
            return;
        }
        ApformImpl.unusedImplems.remove(impl);
        compoType.addImpl(impl);
    }

    /**
     * 
     * @param expected
     */
    public static synchronized void addExpected(String expected) {
        ApformImplementationImpl.expectedImpls.add(expected);
    }

    public static synchronized void addExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;
        Set<DynamicManager> mans = ApformImpl.expectedImpls.get(samImplName);
        if (mans == null) {
            mans = new HashSet<DynamicManager>();
            mans.add(manager);
            ApformImpl.expectedImpls.put(samImplName, mans);
        } else {
            mans.add(manager);
        }
    }

    public static synchronized void removeExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;

        Set<DynamicManager> mans = ApformImpl.expectedImpls.get(samImplName);
        if (mans != null) {
            mans.remove(manager);
        }
    }

    public static synchronized void addExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;

        Set<DynamicManager> mans = ApformImpl.expectedInterfaces.get(interf);
        if (mans == null) {
            mans = new HashSet<DynamicManager>();
            mans.add(manager);
            ApformImpl.expectedInterfaces.put(interf, mans);
        } else {
            mans.add(manager);
        }
    }

    public static synchronized void removeExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;
        Set<DynamicManager> mans = ApformImpl.expectedInterfaces.get(interf);
        if (mans != null) {
            mans.remove(manager);
        }
    }

    public static synchronized void addLost(DynamicManager manager) {
        if (manager == null)
            return;
        ApformImpl.listenLost.add(manager);
    }

    public static synchronized void removeLost(DynamicManager manager) {
        if (manager == null)
            return;
        ApformImpl.listenLost.remove(manager);
    }

}
