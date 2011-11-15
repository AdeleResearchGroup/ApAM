package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
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
import fr.imag.adele.apam.apformAPI.Apform2Apam;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;
import fr.imag.adele.apam.apformAPI.Apform;

public class Apform2ApamImpl implements Apform2Apam {
//    static Set<String>  expectedDeployedImpls = new HashSet<String>();

    static final CompositeType rootType      = CompositeTypeImpl.getRootCompositeType();
    static final Composite     rootInst      = CompositeImpl.getRootAllComposites();
    static Set<Implementation>        unusedImplems = CompositeTypeImpl.getRootCompositeType().getImpls();

    @Override
    public void newInstance(String instanceName, ApformInstance client) {
        if (CST.ASMInstBroker.getInst(instanceName) != null) {
            System.err.println("Instance already existing: " + instanceName);
            return;
        }
        Instance inst = CST.ASMInstBroker.addInst(Apform2ApamImpl.rootInst, client, null);
        inst.setProperties(client.getProperties());
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

    @Override
    public void newImplementation(String implemName, ApformImplementation client) {
        if (Apform.getUnusedImplem(implemName) != null) {
            System.err.println("Implementation already existing: " + implemName);
            return;
        }
        Implementation impl = ((ImplementationBrokerImpl) CST.ASMImplBroker).addImpl(Apform2ApamImpl.rootType, client, null);
        impl.setProperties(client.getProperties());
        synchronized (Apform.expectedImpls) {
            if (Apform.expectedImpls.contains(implemName)) { // it is expected
                Apform.expectedImpls.remove(implemName);
                Apform.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
            }
        }
    }

    @Override
    public void newSpecification(String specName, ApformSpecification client) {
        if (CST.ASMSpecBroker.getSpec(specName) != null) {
            System.err.println("Specification already existing: " + specName);
            return;
        }
        Specification spec = CST.ASMSpecBroker.addSpec(specName, client, null);
        spec.setProperties(client.getProperties());
    }

    @Override
    public void vanishInstance(String instanceName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void vanishImplementation(String implementationName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void vanishSpecification(String specificationName) {
        // TODO Auto-generated method stub

    }

}
