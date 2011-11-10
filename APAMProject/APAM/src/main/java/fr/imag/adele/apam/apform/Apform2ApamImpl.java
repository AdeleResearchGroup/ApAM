package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeImpl;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.ASMImpl.ASMImplBrokerImpl;
import fr.imag.adele.apam.ASMImpl.SamImplEventHandler;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.apformAPI.Apform2Apam;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;

public class Apform2ApamImpl implements Apform2Apam {
//    static Set<String>  expectedDeployedImpls = new HashSet<String>();

    static final CompositeType rootType      = CompositeTypeImpl.getRootCompositeType();
    static final Composite     rootInst      = CompositeImpl.getRootAllComposites();
    static Set<ASMImpl>        unusedImplems = CompositeTypeImpl.getRootCompositeType().getImpls();

    @Override
    public void newInstance(String instanceName, ApformInstance client) {
        if (ApformImpl.getUnusedInst(instanceName) != null) {
            System.err.println("Implementation already existing: " + instanceName);
            return;
        }
        ASMInst inst = CST.ASMInstBroker.addInst(Apform2ApamImpl.rootInst, client, null);
        inst.setProperties(client.getProperties());

//        synchronized (ApformImpl.expectedImpls) {
//            if (ApformImpl.expectedImpls.contains(instanceName)) { // it is expected
//                ApformImpl.expectedImpls.remove(instanceName);
//                ApformImpl.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
//            }
//        }
        synchronized (ApformImpl.expectedMngImpls) {
            if (ApformImpl.expectedMngImpls.containsKey(instanceName)) {
                if (ApformImpl.expectedMngImpls.keySet().contains(instanceName)) {
                    for (DynamicManager manager : ApformImpl.expectedMngImpls.get(instanceName)) {
                        manager.appeared(inst);
                    }
                    ApformImpl.expectedMngImpls.remove(instanceName);
                }
            }
        }

        synchronized (ApformImpl.expectedMngInterfaces) {
            for (String interf : inst.getImpl().getSpec().getInterfaces()) {
                if (ApformImpl.expectedMngInterfaces.get(interf) != null) {
                    for (DynamicManager manager : ApformImpl.expectedMngInterfaces.get(interf)) {
                        manager.appeared(inst);
                    }
                }
                ApformImpl.expectedMngInterfaces.remove(interf);
            }
        }
    }

    @Override
    public void newImplementation(String implemName, ApformImplementation client) {
        if (ApformImpl.getUnusedImplem(implemName) != null) {
            System.err.println("Implementation already existing: " + implemName);
            return;
        }
        ASMImpl impl = ((ASMImplBrokerImpl) CST.ASMImplBroker).addImpl(Apform2ApamImpl.rootType, client, null);
        impl.setProperties(client.getProperties());
        synchronized (ApformImpl.expectedImpls) {
            if (ApformImpl.expectedImpls.contains(implemName)) { // it is expected
                ApformImpl.expectedImpls.remove(implemName);
                ApformImpl.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
            }
        }
    }

    @Override
    public void newSpecification(String specName, ApformSpecification client) {
        if (CST.ASMSpecBroker.getSpec(specName) != null) {
            System.err.println("Specification already existing: " + specName);
            return;
        }
        ASMSpec spec = CST.ASMSpecBroker.addSpec(specName, client, null);
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
