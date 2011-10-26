package fr.imag.adele.apam.ASMImpl;

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
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.event.EventProperty;

public class ASMInstBrokerImpl implements ASMInstBroker {

    private static final ASMImplBroker implBroker = CST.ASMImplBroker;

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
            if (inst.getName().equals(instName)) {
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
    public ASMInst addSamInst(Composite instComposite, Instance samInst, Attributes properties) {
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
            impl = CST.ASMImplBroker.getImpl(samInst.getImplementation());
            if (impl == null) { // create the implem also
                impl = ASMInstBrokerImpl.implBroker.addImpl(instComposite.getCompType(),
                        samInst.getImplementation().getName(), properties);
            }

            // Normally composite implementations are visible by SAM, but they can not be instantiated.
            // Their iPojo instances (although allowed) are not visible in the OSGi registry or by SAM.
            // The only way to create an instance of a composite should be using APAM.
            if (impl instanceof CompositeType) {
                System.err.println("Error, trying to activate composite instance " + impl
                        + " without using the APAM API");
                return null;
            }

            inst = new ASMInstImpl(impl, instComposite, null, samInst, false);
            return inst;
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    // adds both in the broker and in its implem
    public void addInst(ASMInst inst) {
        if ((inst != null) && !instances.contains(inst)) {
            instances.add(inst);
            ((ASMImplImpl) inst.getImpl()).addInst(inst);
        }
    }

    @Override
    public ASMInst getInst(Instance samInst) {
        if (samInst == null)
            return null;
        String samName = samInst.getName();
        // Warning : for a composite both the composite and the main instance refer to the same sam instance
        for (ASMInst inst : instances) {
            if ((inst.getSAMInst() == samInst) && !inst.getName().equals(samName)) {
                System.err.println("error in name " + samName);
            }
            if (inst.getName().equals(samName))
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
            ((ASMInstImpl) inst).remove(); // wires and sam attributes
            ((ASMImplImpl) inst.getImpl()).removeInst(inst);
        }
    }

}
