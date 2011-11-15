package fr.imag.adele.apam.apformAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import fr.imag.adele.apam.ASMImpl.SamInstEventHandler;
import fr.imag.adele.apam.ASMImpl;
import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
//import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamImpl.ImplementationImpl;
import fr.imag.adele.apam.apamImpl.InstanceImpl;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
//import fr.imag.adele.apam.util.Attributes;
//import fr.imag.adele.apam.util.AttributesImpl;

public class Apform {
    static final CompositeType              rootType              = CompositeTypeImpl.getRootCompositeType();
    static final Composite                  rootInst              = CompositeImpl.getRootAllComposites();

    static Set<ASMImpl>                     unusedImplems         = CompositeTypeImpl.getRootCompositeType().getImpls();
    static Set<ASMInst>                     unusedInsts           = CompositeImpl.getRootAllComposites()
                                                                          .getContainInsts();

    public static Set<String>               expectedImpls         = new HashSet<String>();

    // The managers are waiting for the apparition of an instance of the ASMImpl or implementing the interface
    // In both case, no ASMInst is created.
    static Map<String, Set<DynamicManager>> expectedMngImpls      = new HashMap<String, Set<DynamicManager>>();
    static Map<String, Set<DynamicManager>> expectedMngInterfaces = new HashMap<String, Set<DynamicManager>>();

    // registers the managers that are interested in services that disappear.
    static Set<DynamicManager>              listenLost            = new HashSet<DynamicManager>();

    public static ASMImpl getUnusedImplem(String name) {
        ASMImpl impl = CST.ASMImplBroker.getImpl(name);
        if (impl == null)
            return null;
        return (Apform.unusedImplems.contains(impl)) ? impl : null;
    }

    public static ASMInst getUnusedInst(String name) {
        ASMInst inst = CST.ASMInstBroker.getInst(name);
        if (inst == null)
            return null;
        return (Apform.unusedInsts.contains(inst)) ? inst : null;
    }

    /**
     * The implementation that was unused so far, is now logicaly deployed.
     * Remove it from the unUsed compositeType.
     * 
     * @param impl
     */
    public static void setUsedImpl(ASMImpl impl) {
        if (impl.isUsed())
            return;
        ((ImplementationImpl) impl).setUsed(true);
        ((CompositeTypeImpl) Apform.rootType).removeImpl(impl);
        if (impl instanceof CompositeType) { // it is a composite
            ((CompositeTypeImpl) Apform.rootType).removeEmbedded((CompositeType) impl);
        }
    }

    /**
     * The implementation that was unused so far, is now logicaly deployed.
     * Remove it from the unUsed compositeType.
     * 
     * @param impl
     */
    public static void setUsedInst(ASMInst inst) {
        if (inst.isUsed())
            return;
        ((CompositeImpl) Apform.rootInst).removeInst(inst);
        ((InstanceImpl) inst).setUsed(true);
        if (inst instanceof Composite) { // it is a composite. Should never happen ?
            ((CompositeImpl) Apform.rootInst).removeSon((Composite) inst);
        }
    }

    /**
     * A bundle is under deployment, in which is located the implementation to wait.
     * The method waits until the implementation arrives and is notified by Apam-iPOJO.
     * 
     * @param expectedImpl the symbolic name of that implementation
     * @return
     */
    public static ASMImpl getWaitImplementation(String expectedImpl) {
        if (expectedImpl == null)
            return null;
        // if allready here
        ASMImpl impl = CST.ASMImplBroker.getImpl(expectedImpl);
        if (impl != null)
            return impl;
        Apform.expectedImpls.add(expectedImpl);
        // not yet here. Wait for it.
        synchronized (Apform.expectedImpls) {

            try {
                while (Apform.expectedImpls.contains(expectedImpl)) {
                    Apform.expectedImpls.wait();
                }
                // The expected impl arrived. It is in unUsed.
                impl = CST.ASMImplBroker.getImpl(expectedImpl);
                if (impl == null) // should never occur
                    System.out.println("wake up but imlementation is not present " + expectedImpl);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return impl;
    }

//    /**
//     * 
//     * @param expected
//     */
//    public static void addExpected(String expected) {
//        synchronized (ApformImpl.expectedImpls) {
//            ApformImpl.expectedImpls.add(expected);
//        }
//    }

    public static void addExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;

        synchronized (Apform.expectedMngImpls) {
            Set<DynamicManager> mans = Apform.expectedMngImpls.get(samImplName);
            if (mans == null) {
                mans = new HashSet<DynamicManager>();
                mans.add(manager);
                Apform.expectedMngImpls.put(samImplName, mans);
            } else {
                mans.add(manager);
            }
        }

    }

    public static synchronized void removeExpectedImpl(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null))
            return;

        synchronized (Apform.expectedMngImpls) {
            Set<DynamicManager> mans = Apform.expectedMngImpls.get(samImplName);
            if (mans != null) {
                mans.remove(manager);
            }
        }
    }

    public static synchronized void addExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;

        synchronized (Apform.expectedMngInterfaces) {
            Set<DynamicManager> mans = Apform.expectedMngInterfaces.get(interf);
            if (mans == null) {
                mans = new HashSet<DynamicManager>();
                mans.add(manager);
                Apform.expectedMngInterfaces.put(interf, mans);
            } else {
                mans.add(manager);
            }
        }
    }

    public static synchronized void removeExpectedInterf(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null))
            return;
        synchronized (Apform.expectedMngInterfaces) {
            Set<DynamicManager> mans = Apform.expectedMngInterfaces.get(interf);
            if (mans != null) {
                mans.remove(manager);
            }
        }
    }

    public static synchronized void addLost(DynamicManager manager) {
        if (manager == null)
            return;

        synchronized (Apform.listenLost) {
            Apform.listenLost.add(manager);
        }
    }

    public static synchronized void removeLost(DynamicManager manager) {
        if (manager == null)
            return;
        synchronized (Apform.listenLost) {
            Apform.listenLost.remove(manager);
        }
    }

}
