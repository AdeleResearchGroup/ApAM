package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;
import fr.imag.adele.apam.util.Attributes;

public class ApformImplementationImpl implements ApformImplementation {

    static Set<String>  expectedImpls = new HashSet<String>();

    static Set<ASMImpl> apfImplems    = CompositeTypeImpl.getRootCompositeType().getImpls();

//    public static ApformImplementation getApfImplementation(String impl) {
//        return ApformImplementationImpl.apfImplems.
//    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getInterfaceNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getProperty(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApformInstance createInstance(Attributes initialproperties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApformSpecification getSpecification() {
        // TODO Auto-generated method stub
        return null;
    }

}
