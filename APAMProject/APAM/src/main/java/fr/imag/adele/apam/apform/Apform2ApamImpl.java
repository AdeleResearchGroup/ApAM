package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ASMImpl.SamImplEventHandler;
import fr.imag.adele.apam.apformAPI.Apform2Apam;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;

public class Apform2ApamImpl implements Apform2Apam {
    static Set<String> expectedDeployedImpls = new HashSet<String>();

    @Override
    public void newInstance(String instanceName, ApformInstance client) {
        // TODO Auto-generated method stub

    }

    @Override
    public void newImplementation(String implemName, ApformImplementation client) {
        synchronized (ApformImpl.expectedImpls) {
            if (ApformImpl.expectedImpls.contains(implemName)) { // it is expected
                ApformImpl.expectedImpls.remove(implemName);
                ApformImpl.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
            }
        }
    }

    @Override
    public void newSpecification(String specName, ApformSpecification client) {
        // TODO Auto-generated method stub

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
