package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformSpecification;
//import fr.imag.adele.apam.util.Attributes;
//import fr.imag.adele.apam.util.AttributesImpl;
//import fr.imag.adele.sam.ApformImplementation;
//import fr.imag.adele.sam.ApformSpecification;
import fr.imag.adele.sam.deployment.DeploymentUnit;

public class ImplementationBrokerImpl implements ImplementationBroker {

    // private Logger logger = Logger.getLogger(ASMImplBrokerImpl.class);

    private final Set<Implementation> implems = new HashSet<Implementation>();

    // Not in the interface. No control
    public void addImpl(Implementation impl) {
        if (impl != null)
            implems.add(impl);
    }

    // Not in the interface. No control
    @Override
    public void removeImpl(Implementation impl) {
        if (impl != null)
            implems.remove(impl);
    }

    @Override
    public Implementation getImpl(String implName) {
        if (implName == null)
            return null;
        for (Implementation impl : implems) {
            if (implName.equals(impl.getName()))
                return impl;
        }
        return null;
    }

    @Override
    public Set<Implementation> getImpls() {
        return Collections.unmodifiableSet(implems);
        // return new HashSet<ASMImpl> (implems) ;
    }

    @Override
    public Set<Implementation> getImpls(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getImpls();
        Set<Implementation> ret = new HashSet<Implementation>();
        for (Implementation impl : implems) {
            if (impl.match(goal))
                ret.add(impl);
        }
        return ret;
    }

//    @Override
    public Implementation addImpl(CompositeType compo, ApformImplementation apfImpl, Map<String, Object> properties) {
        if ((apfImpl == null) || (compo == null)) {
            System.err.println("ERROR : missing apf Implementaion or composite in addImpl");
            return null;
        }

        String implName = apfImpl.getName();
        String specName = (String) apfImpl.getProperty(CST.A_APAMSPECNAME);

        // if allready existing do not duplicate
        Implementation asmImpl = getImpl(implName);
        if (asmImpl != null) { // do not create twice
            System.err.println("Implementation already existing (in addImpl) " + implName);
            if (specName != null)
                ((SpecificationImpl) asmImpl.getSpec()).setName(specName);
            return asmImpl;
        }

        // specification control. Spec usually does not exist in Apform, but we need to create one anyway.
        ApformSpecification apfSpec = apfImpl.getSpecification();
        SpecificationImpl spec = null;
        if (apfSpec != null) { // may be null !
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(apfSpec);
        }
        if ((spec == null) && (specName != null)) // No ASM spec related to the apf spec.
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(specName);
        if (spec == null)
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(apfImpl.getInterfaceNames());
        if (spec == null) {
            if (specName == null) { // create an arbitrary name, and give the impl interface.
                // TODO warning, it is an approximation, impl may have more interfaces than its spec
                specName = implName + "_spec";
            }
            spec = new SpecificationImpl(specName, apfSpec, apfImpl.getInterfaceNames(), properties);
        }

//        if (specName != null)
//            spec.setName(specName);

        // create a primitive or composite implementation
        if ((apfImpl.getProperty(CST.A_COMPOSITE) != null) &&
                    ((Boolean) apfImpl.getProperty(CST.A_COMPOSITE) == true)) {
            // Allow specifying properties to the composite instance
            asmImpl = CompositeTypeImpl.createCompositeType(compo, apfImpl);
        } else {
            asmImpl = new ImplementationImpl(compo, spec, apfImpl, properties);
        }

        return asmImpl;
    }

//    @Override
//    public ASMImpl addImpl(CompositeType compo, String apfImplName, Attributes properties) {
//        if (apfImplName == null) {
//            System.out.println("ERROR : parameter ApformImplementation " + apfImplName
//                    + " or composite : " + compo + " missing. In addimpl.");
//            return null;
//        }
//        ApformImplementation apfImpl;
//        ASMImpl impl = ApformImpl.getUnusedImplem(apfImplName);
//        if (impl == null) {
//            System.out.println("ERROR : Sam ApformImplementation " + apfImplName + " cannot be found");
//            return null;
//        }
//        // TODO probably BUG
//        apfImpl = impl.getApformImpl();
//        return addImpl(compo, apfImpl, properties);
//    }

    @Override
    public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, Object> properties) {

        if (url == null)
            return null;

        // String implNameExpected = null;
        ApformImplementation apfImpl;
        Implementation asmImpl = null;
        asmImpl = getImpl(implName);
        if (asmImpl != null) { // do not create twice
            return asmImpl;
        }
        try {
            DeploymentUnit du = CST.SAMDUBroker.install(url, "bundle");
            Set<String> implementationsNames = du.getImplementationsName();
            boolean found = false;
            for (String implInBundle : implementationsNames) {
                if (implInBundle.equals(implName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.err.println("Error : Bundle at URL " + url + " does not contain implementation " + implName);
                return null;
            }
            du.activate();
        } catch (Exception e) {
            System.err.println("deployment failed :" + implName + " at URL " + url);
        }
        asmImpl = Apform.getWaitImplementation(implName);
        ApamResolver.deployedImpl(compo, asmImpl, true);
        // comment savoir si une instance a été créée dans la foulée,
        // et sous quel nom ?

//        asmImpl = addImpl0(compo, apfImpl, properties);
        return asmImpl;
    }

    @Override
    public Implementation getImpl(ApformImplementation apfImpl) {
        String apfName = apfImpl.getName();
        // Warning : for a composite main implem, both the composite type and the main implem refer to the same apf
        // implem
        for (Implementation implem : implems) {
            if ((implem.getApformImpl() == apfImpl) && implem.getName().equals(apfName)) {
                return implem;
            }
        }
        return null;
    }

    @Override
    public Set<Implementation> getImpls(Specification spec) {
        Set<Implementation> impls = new HashSet<Implementation>();
        for (Implementation impl : implems) {
            if (impl.getSpec() == spec)
                impls.add(impl);
        }
        return impls;
    }

}
