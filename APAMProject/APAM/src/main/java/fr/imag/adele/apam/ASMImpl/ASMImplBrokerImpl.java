package fr.imag.adele.apam.ASMImpl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.LocalMachine;
import fr.imag.adele.am.Machine;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.eventing.EventingEngine;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.deployment.DeploymentUnit;
import fr.imag.adele.sam.event.EventProperty;

public class ASMImplBrokerImpl implements ASMImplBroker {

    // private Logger logger = Logger.getLogger(ASMImplBrokerImpl.class);

    private final Set<ASMImpl> implems = new HashSet<ASMImpl>();

    // private SamImplEventHandler eventHandler;

    // public ASMImplBrokerImpl() {
    // try {
    // eventHandler = CST.implEventHandler ;
    // Machine machine = LocalMachine.localMachine;
    // EventingEngine eventingEngine = machine.getEventingEngine();
    // eventHandler = new SamImplEventHandler();
    // eventingEngine.subscribe(eventHandler, EventProperty.TOPIC_IMPLEMENTATION);
    // } catch (Exception e) {
    // }
    // }

    public void stopSubscribe(AMEventingHandler handler) {
        try {
            Machine machine = LocalMachine.localMachine;
            EventingEngine eventingEngine = machine.getEventingEngine();
            eventingEngine.unsubscribe(handler, EventProperty.TOPIC_IMPLEMENTATION);
        } catch (Exception e) {
        }
    }

    // Not in the interface. No control
    public void addImpl(ASMImpl impl) {
        if (impl != null)
            implems.add(impl);
    }

    // Not in the interface. No control
    @Override
    public void removeImpl(ASMImpl impl) {
        if (impl != null)
            implems.remove(impl);
    }

    @Override
    public ASMImpl getImpl(String implName) {
        if (implName == null)
            return null;
        for (ASMImpl impl : implems) {
            if (implName.equals(impl.getName()))
                return impl;
        }
        return null;
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(implems);
        // return new HashSet<ASMImpl> (implems) ;
    }

    @Override
    public Set<ASMImpl> getImpls(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getImpls();
        Set<ASMImpl> ret = new HashSet<ASMImpl>();
        for (ASMImpl impl : implems) {
            if (goal.match((AttributesImpl) impl.getProperties()))
                ret.add(impl);
        }
        return ret;
    }

    private ASMImpl addImpl0(CompositeType compo, Implementation samImpl, String specName,
            Attributes properties) {
        if (samImpl == null) {
            System.err.println("ERROR : missing sam Implementaion in addImpl");
            return null;
        }
        //       if (compo == null && )
        String implName = samImpl.getName();
        try {
            //specification control
            Specification samSpec = samImpl.getSpecification();
            ASMImpl asmImpl = null;
            ASMSpecImpl spec = null;
            if (samSpec != null) { // may be null !
                spec = (ASMSpecImpl) CST.ASMSpecBroker.getSpec(samSpec);
            }
            if (spec == null) { // No ASM spec related to the sam spec.
                spec = (ASMSpecImpl) CST.ASMSpecBroker.getSpec(samSpec.getInterfaceNames());
                if (spec != null) { // has been created without the SAM spec.
                                    // Add it now.
                    spec.setSamSpec(samSpec);
                } else { // create the spec
                    spec = new ASMSpecImpl(specName, samSpec, properties);
                }
            }
            if (specName != null)
                spec.setName(specName);

            //if allready existing do not duplicate
            asmImpl = getImpl(implName);
            if (asmImpl != null) { // do not create twice
                ((ASMSpecImpl) asmImpl.getSpec()).setName(specName);
                return asmImpl;
            }

            // create a primitive or composite implementation
            if ((samImpl.getProperty(CST.PROPERTY_COMPOSITE) != null) &&
                    ((Boolean) samImpl.getProperty(CST.PROPERTY_COMPOSITE) == true)) {
                // Allow specifying properties to the composite instance
                asmImpl = CompositeTypeImpl.createCompositeType(compo, samImpl);
            } else {
                asmImpl = new ASMImplImpl(compo, spec, samImpl, properties);
            }

            return asmImpl;

        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public ASMImpl
            addImpl(CompositeType compo, String samImplName, String specName, Attributes properties) {
        if (samImplName == null) {
            System.out.println("ERROR : parameter Sam Implementation " + samImplName
                    + " or composite : " + compo + " missing. In addimpl.");
            return null;
        }
        Implementation samImpl;
        try {
            samImpl = CST.SAMImplBroker.getImplementation(samImplName);
            if (samImpl == null) {
                System.out.println("ERROR : Sam Implementation " + samImplName + " cannot be found");
                return null;
            }
            return addImpl0(compo, samImpl, specName, properties);

        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ASMImpl createImpl(CompositeType compo, String implName, URL url, String specName,
            Attributes properties) {

        if (url == null)
            return null;

        //        String implNameExpected = null;
        Implementation samImpl;
        ASMImpl asmImpl = null;
        try {
            asmImpl = getImpl(implName);
            if (asmImpl != null) { // do not create twice
                ((ASMSpecImpl) asmImpl.getSpec()).setName(specName);
                return asmImpl;
            }

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

            CST.implEventHandler.addExpected(implName);
            du.activate();
            samImpl = CST.implEventHandler.getImplementation(implName);
            // TODO comment savoir si une instance a été créée dans la foulée,
            // et sous quel nom ?
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        asmImpl = addImpl0(compo, samImpl, specName, properties);
        return asmImpl;
    }

    @Override
    public ASMImpl getImpl(Implementation samImpl) {
        for (ASMImpl implem : implems) {
            if (implem.getSamImpl() == samImpl) {
                return implem;
            }
        }
        return null;
    }

    @Override
    public Set<ASMImpl> getImpls(ASMSpec spec) {
        Set<ASMImpl> impls = new HashSet<ASMImpl>();
        for (ASMImpl impl : implems) {
            if (impl.getSpec() == spec)
                impls.add(impl);
        }
        return impls;
    }

}
