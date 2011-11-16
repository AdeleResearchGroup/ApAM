package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;

//import fr.imag.adele.am.exception.ConnectionException;
//import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
//import fr.imag.adele.apam.ASMImpl.SamImplEventHandler;
import fr.imag.adele.apam.apamImpl.ImplementationBrokerImpl;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;

public class Apform2Apam {
//    static Set<String>  expectedDeployedImpls = new HashSet<String>();

    static final CompositeType rootType      = CompositeTypeImpl.getRootCompositeType();
    static final Composite     rootInst      = CompositeImpl.getRootAllComposites();
    static Set<Implementation> unusedImplems = CompositeTypeImpl.getRootCompositeType().getImpls();

    /**
     * A new instance, represented by object "client" just appeared in the platform.
     */
    public static void newInstance(String instanceName, ApformInstance client) {
        if (CST.InstBroker.getInst(instanceName) != null) {
            System.err.println("Instance already existing: " + instanceName);
            return;
        }
        Instance inst = CST.InstBroker.addInst(Apform2Apam.rootInst, client, client.getProperties());
        // inst.putAll(client.getProperties());
    }

//        synchronized (ApformImpl.expectedImpls) {
//            if (ApformImpl.expectedImpls.contains(instanceName)) { // it is expected
//                ApformImpl.expectedImpls.remove(instanceName);
//                ApformImpl.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
//            }
//        }

//        synchronized (Apform.expectedMngImpls) {
//            if (Apform.expectedMngImpls.containsKey(instanceName)) {
//                if (Apform.expectedMngImpls.keySet().contains(instanceName)) {
//                    for (DynamicManager manager : Apform.expectedMngImpls.get(instanceName)) {
//                        manager.appeared(inst);
//                    }
//                    Apform.expectedMngImpls.remove(instanceName);
//                }
//            }
//        }
//
//        synchronized (Apform.expectedMngInterfaces) {
//            if (inst.getImpl().getSpec() != null) {
//                for (String interf : inst.getImpl().getSpec().getInterfaces()) {
//                    if (Apform.expectedMngInterfaces.get(interf) != null) {
//                        for (DynamicManager manager : Apform.expectedMngInterfaces.get(interf)) {
//                            manager.appeared(inst);
//                        }
//                    }
//                    Apform.expectedMngInterfaces.remove(interf);
//                }
//            }
//        }
//    }

    /**
     * A new implementation, represented by object "client" just appeared in the platform.
     * 
     * @param implemName : the symbolic name.
     * @param client
     */
    public static void newImplementation(String implemName, ApformImplementation client) {
        if (Apform.getUnusedImplem(implemName) != null) {
            System.err.println("Implementation already existing: " + implemName);
            return;
        }
        Implementation impl = ((ImplementationBrokerImpl) CST.ImplBroker).addImpl(Apform2Apam.rootType, client,
                client.getProperties());
        // impl.setProperties(client.getProperties());
        synchronized (Apform.expectedImpls) {
            if (Apform.expectedImpls.contains(implemName)) { // it is expected
                Apform.expectedImpls.remove(implemName);
                Apform.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
            }
        }
    }

    /**
     * A new specification, represented by object "client" just appeared in the platform.
     * 
     * @param specName
     * @param client
     */
    public static void newSpecification(String specName, ApformSpecification client) {
        if (CST.SpecBroker.getSpec(specName) != null) {
            System.err.println("Specification already existing: " + specName);
            return;
        }
        Specification spec = CST.SpecBroker.addSpec(specName, client, client.getProperties());
        // spec.setProperties(client.getProperties());
    }

    /**
     * The instance called "instance name" just disappeared from the platform.
     * 
     * @param instanceName
     */
    public static void vanishInstance(String instanceName) {
        // TODO Auto-generated method stub

    }

    /**
     * * The implementation called "implementation name" just disappeared from the platform.
     * 
     * @param implementationName
     */
    public static void vanishImplementation(String implementationName) {
        // TODO Auto-generated method stub

    }

    /**
     * 
     * @param specificationName
     */
    public static void vanishSpecification(String specificationName) {
        // TODO Auto-generated method stub

    }

}
