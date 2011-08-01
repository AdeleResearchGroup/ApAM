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
import fr.imag.adele.apam.ApplicationImpl;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
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
            if (impl.getASMName() == null) {
                if (impl.getSamImpl().getName().equals(implName))
                    return impl;
            } else {
                if (implName.equals(impl.getASMName()))
                    return impl;
            }
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

    private ASMImpl addImpl0(Composite compo, String implName, Implementation samImpl, String specName,
            Attributes properties) {
        if ((samImpl == null) || (compo == null)) {
            System.err.println("ERROR : missing Implementaion or composite in addImpl");
            return null;
        }
        try {
            Specification samSpec;
            ASMImpl asmImpl = null;
            asmImpl = getImpl(implName);
            if (asmImpl != null) { // do not create twice
                ((ASMImplImpl) asmImpl).setASMName(implName);
                ((ASMSpecImpl) asmImpl.getSpec()).setASMName(specName);
                return asmImpl;
            }
            ASMSpecImpl spec = null;
            samSpec = samImpl.getSpecification();
            if (samSpec != null) { // may be null !
                spec = (ASMSpecImpl) CST.ASMSpecBroker.getSpec(samSpec);
            }
            if (spec == null) { // No ASM spec related to the sam spec.
                spec = (ASMSpecImpl) CST.ASMSpecBroker.getSpec(samSpec.getInterfaceNames());
                if (spec != null) { // has been created without the SAM spec.
                                    // Add it now.
                    spec.setSamSpec(samSpec);
                } else { // create the spec
                    spec = new ASMSpecImpl(compo, specName, samSpec, properties);
                }
            }
            if (specName != null)
                spec.setASMName(specName);

            asmImpl = new ASMImplImpl(compo, implName, spec, samImpl, properties);

            //          Application appli = asmImpl.getComposite().getApplication();
            //            if ((asmImpl.getSpec() == appli.getMainSpec()) && (appli.getMainImpl() == null))
            //                ((ApplicationImpl) appli).setMainImpl(asmImpl);

            return asmImpl;

        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public ASMImpl
            addImpl(Composite compo, String implName, String samImplName, String specName, Attributes properties) {
        if ((samImplName == null) || (compo == null)) {
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
            return addImpl0(compo, samImplName, samImpl, specName, properties);

        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ASMImpl createImpl(Composite compo, String implName, URL url, String specName,
            Attributes properties) {

        if ((url == null) || (compo == null))
            return null;

        String implNameExpected = null;
        Implementation samImpl;
        ASMImpl asmImpl = null;
        try {
            asmImpl = getImpl(implName);
            if (asmImpl != null) { // do not create twice
                ((ASMImplImpl) asmImpl).setASMName(implName);
                ((ASMSpecImpl) asmImpl.getSpec()).setASMName(specName);
                return asmImpl;
            }

            DeploymentUnit du = CST.SAMDUBroker.install(url, "bundle");
            Set<String> implementationsNames = du.getImplementationsName();
            implNameExpected = (String) implementationsNames.toArray()[0];

            CST.implEventHandler.addExpected(implNameExpected);
            du.activate();
            samImpl = CST.implEventHandler.getImplementation(implNameExpected);
            // TODO comment savoir si une instance a été créée dans la foulée,
            // et sous quel nom ?
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }

        asmImpl = addImpl0(compo, implName, samImpl, specName, properties);
        // in case it is the main implementation
        //        Application appli = asmImpl.getComposite().getApplication();
        //        if ((asmImpl.getSpec() == appli.getMainSpec()) && (appli.getMainImpl() == null))
        //            ((ApplicationImpl) appli).setMainImpl(asmImpl);
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
    public ASMImpl getImplSamName(String samName) {
        if (samName == null)
            return null;
        for (ASMImpl impl : implems) {
            if (impl.getSamImpl().getName().equals(samName))
                return impl;
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

    // @Override
    // public Set<ASMImpl> getShareds(ASMSpec spec, Application appli, Composite compo) {
    // Set<ASMImpl> ret = new HashSet<ASMImpl>();
    // for (ASMImpl inst : implems) {
    // if (inst.getSpec() == spec) {
    // if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
    // ret.add(inst);
    // else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
    // .getApplication() == appli))) {
    // ret.add(inst);
    // } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
    // ret.add(inst);
    // }
    // }
    // }
    // return ret;
    // }
    //
    // @Override
    // public ASMImpl getShared(ASMSpec spec, Application appli, Composite compo) {
    // for (ASMImpl inst : implems) {
    // if (inst.getSpec() == spec) {
    // if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
    // return inst;
    // else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
    // .getApplication() == appli))) {
    // return inst;
    // } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
    // return inst;
    // }
    // }
    // }
    // return null;
    // }

}
