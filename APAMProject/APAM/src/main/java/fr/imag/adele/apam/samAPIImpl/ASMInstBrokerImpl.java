package fr.imag.adele.apam.samAPIImpl;

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
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.event.EventProperty;

public class ASMInstBrokerImpl implements ASMInstBroker {

    private static final ASMImplBroker implBroker = ASM.ASMImplBroker;

    private final Set<ASMInst>         instances  = new HashSet<ASMInst>();

    // EVENTS
    private SamInstEventHandler        instEventHandler;

    public ASMInstBrokerImpl() {
        try {
            Machine machine = LocalMachine.localMachine;
            EventingEngine eventingEngine = machine.getEventingEngine();
            instEventHandler = new SamInstEventHandler();
            eventingEngine.subscribe(instEventHandler, EventProperty.TOPIC_INSTANCE);
        } catch (Exception e) {
        }
    }

    public void stopSubscribe(AMEventingHandler handler) {
        try {
            Machine machine = LocalMachine.localMachine;
            EventingEngine eventingEngine = machine.getEventingEngine();
            eventingEngine.unsubscribe(handler, EventProperty.TOPIC_INSTANCE);
        } catch (Exception e) {
        }
    }

    @Override
    public ASMInst getInst(String instName) {
        if (instName == null)
            return null;
        for (ASMInst inst : instances) {
            if (inst.getASMName().equals(instName)) {
                return inst;
            }
        }
        return null;
    }

    // End EVENTS

    @Override
    public Set<ASMInst> getInsts() {
        return Collections.unmodifiableSet(instances);
    }

    @Override
    public Set<ASMInst> getInsts(ASMSpec spec, Filter goal) throws InvalidSyntaxException {
        if (spec == null)
            return null;
        Set<ASMInst> ret = new HashSet<ASMInst>();
        if (goal == null) {
            for (ASMInst inst : instances) {
                if (inst.getSpec() == spec)
                    ret.add(inst);
            }
        } else {
            for (ASMInst inst : instances) {
                if ((inst.getSpec() == spec) && goal.match((AttributesImpl) inst.getProperties()))
                    ret.add(inst);
            }
        }
        return ret;
    }

    @Override
    public Set<ASMInst> getInsts(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getInsts();
        Set<ASMInst> ret = new HashSet<ASMInst>();
        for (ASMInst inst : instances) {
            if (goal.match((AttributesImpl) inst.getProperties()))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public ASMInst addInst(Composite compo, Instance samInst, String implName, String specName, Attributes properties) {
        if (samInst == null) {
            System.out.println("No instance provided for add Instance");
            return null;
        }
        ASMImpl impl = null;
        ASMInst inst;
        try {
            inst = getInst(samInst);
            if (inst != null) { // allready existing ! May have been created by
                // DYNAMAN, without all parameters
                return inst;
            }
            impl = ASM.ASMImplBroker.getImpl(samInst.getImplementation());
            if (impl == null) { // create the implem also
                if (compo == null) {
                    System.out.println("No implementation for the instance, and composite not provided");
                    return null;
                }
                impl = ASMInstBrokerImpl.implBroker.addImpl(compo, implName, samInst.getImplementation().getName(),
                        specName, properties);
            }
            if (compo == null)
                compo = impl.getComposite();
            return new ASMInstImpl(compo, impl, null, samInst);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Warning : no control
    public void addInst(ASMInst inst) {
        if (inst != null)
            instances.add(inst);
    }

    @Override
    public ASMInst getInst(Instance samInst) {
        if (samInst == null)
            return null;
        for (ASMInst inst : instances) {
            if (inst.getSAMInst() == samInst)
                return inst;
        }
        return null;
    }

    @Override
    public void removeInst(ASMInst inst) {
        if (inst == null)
            return;
        if (instances.contains(inst)) {
            instances.remove(inst);
            inst.remove();
        }
    }

    @Override
    public Set<ASMInst> getShareds(ASMSpec spec, Application appli, Composite compo) {
        if (spec == null)
            return null;
        Set<ASMInst> ret = new HashSet<ASMInst>();
        for (ASMInst inst : instances) {
            if (inst.getSpec() == spec) {
                if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
                    ret.add(inst);
                else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
                        .getApplication() == appli))) {
                    ret.add(inst);
                } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

    @Override
    public ASMInst getShared(ASMImpl impl, Application appli, Composite compo) {
        if (impl == null)
            return null;
        for (ASMInst inst : instances) {
            if (inst.getImpl() == impl) {
                if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
                    return inst;
                else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
                        .getApplication() == appli))) {
                    return inst;
                } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
                    return inst;
                }
            }
        }
        return null;
    }

    @Override
    public ASMInst getShared(ASMSpec spec, Application appli, Composite compo) {
        if (spec == null)
            return null;
        for (ASMInst inst : instances) {
            if (inst.getSpec() == spec) {
                if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
                    return inst;
                else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
                        .getApplication() == appli))) {
                    return inst;
                } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
                    return inst;
                }
            }
        }
        return null;
    }

    @Override
    public Set<ASMInst> getShareds(ASMImpl impl, Application appli, Composite compo) {
        if (impl == null)
            return null;
        Set<ASMInst> ret = new HashSet<ASMInst>();
        for (ASMInst inst : instances) {
            if (inst.getImpl() == impl) {
                if (inst.getProperty(Attributes.SHARED).equals(Attributes.SHARABLE))
                    ret.add(inst);
                else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.APPLI) && (inst.getComposite()
                        .getApplication() == appli))) {
                    ret.add(inst);
                } else if ((inst.getProperty(Attributes.SHARED).equals(Attributes.LOCAL) && (inst.getComposite() == compo))) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

}
