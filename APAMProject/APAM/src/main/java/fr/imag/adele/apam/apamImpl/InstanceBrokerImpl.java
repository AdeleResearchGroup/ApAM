package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.LocalMachine;
//import fr.imag.adele.am.Machine;
//import fr.imag.adele.am.eventing.AMEventingHandler;
//import fr.imag.adele.am.eventing.EventingEngine;
//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.InstanceBroker;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
//import fr.imag.adele.apam.ASMImpl.SamInstEventHandler;
import fr.imag.adele.apam.apform.ApformInstance;
//import fr.imag.adele.apam.util.Attributes;
//import fr.imag.adele.apam.util.AttributesImpl;
//import fr.imag.adele.sam.Instance;
//import fr.imag.adele.sam.event.EventProperty;

public class InstanceBrokerImpl implements InstanceBroker {

    private static final ImplementationBroker implBroker        = CST.ImplBroker;

    private final Set<Instance>               instances         = new HashSet<Instance>();
    private final Set<Instance>               sharableInstances = new HashSet<Instance>();

    // EVENTS
    //    private SamInstEventHandler        instEventHandler;

    public InstanceBrokerImpl() {
        //        try {
        //            Machine machine = LocalMachine.localMachine;
        //            EventingEngine eventingEngine = machine.getEventingEngine();
        //            instEventHandler = new SamInstEventHandler();
        //            eventingEngine.subscribe(instEventHandler, EventProperty.TOPIC_INSTANCE);
        //        } catch (Exception e) {
        //        }
    }

    //    public void stopSubscribe(AMEventingHandler handler) {
    //        try {
    //            Machine machine = LocalMachine.localMachine;
    //            EventingEngine eventingEngine = machine.getEventingEngine();
    //            eventingEngine.unsubscribe(handler, EventProperty.TOPIC_INSTANCE);
    //        } catch (Exception e) {
    //        }
    //    }

    @Override
    public Instance getInst(String instName) {
        if (instName == null)
            return null;
        for (Instance inst : instances) {
            if (inst.getName().equals(instName)) {
                return inst;
            }
        }
        return null;
    }

    // End EVENTS

    @Override
    public Set<Instance> getSharableInsts() {
        return Collections.unmodifiableSet(sharableInstances);
    }

    @Override
    public Set<Instance> getInsts() {
        return Collections.unmodifiableSet(instances);
    }

    @Override
    public Set<Instance> getInsts(Specification spec, Filter goal) throws InvalidSyntaxException {
        if (spec == null)
            return null;
        Set<Instance> ret = new HashSet<Instance>();
        if (goal == null) {
            for (Instance inst : instances) {
                if (inst.getSpec() == spec)
                    ret.add(inst);
            }
        } else {
            for (Instance inst : instances) {
                if ((inst.getSpec() == spec) && inst.match(goal))
                    ret.add(inst);
            }
        }
        return ret;
    }

    @Override
    public Set<Instance> getInsts(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (inst.match(goal))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public Instance addInst(Composite instComposite, ApformInstance apfInst, Map properties) {
        assert (apfInst != null);

        Implementation impl = null;
        Instance inst;
        inst = CST.InstBroker.getInst(apfInst.getDeclaration().getName());
        if (inst != null) { // allready existing ! May have been created by
            // DYNAMAN, without all parameters
            System.err.println("Instance already existing: " + inst);
            return inst;
        }

        String implementationName = apfInst.getDeclaration().getImplementation().getName();
        impl = CST.ImplBroker.getImpl(implementationName);
        if (impl == null) { // create the implem also
            System.err.println("Implementation is not existing in addInst: " + implementationName);
            //            impl = ASMInstBrokerImpl.implBroker.addImpl(instComposite.getCompType(),
            //                        apfInst.getImplemName(), properties);
        }

        // Normally composite implementations are visible but they can not be instantiated.
        // The only way to create an instance of a composite should be using APAM.
        if (impl instanceof CompositeType) {
            System.err.println("Error, trying to activate composite instance " + impl
                    + " without using the APAM API");
            return null;
        }

        inst = new InstanceImpl(impl, instComposite, null, apfInst);
        return inst;
    }

    // adds both in the broker and in its implem
    public void addInst(Instance inst) {
        if ((inst != null) && !instances.contains(inst)) {
            instances.add(inst);
            ApamManagers.notifyAddedInApam(inst);
            ((ImplementationImpl) inst.getImpl()).addInst(inst);
            if (inst.isSharable())
                sharableInstances.add(inst);
        }
    }

    @Override
    public void removeInst(Instance inst) {
        if (inst == null)
            return;
        if (instances.contains(inst)) {
            instances.remove(inst);
            ApamManagers.notifyRemovedFromApam(inst);
            sharableInstances.remove(inst);
            ((InstanceImpl) inst).remove(); // wires and sam attributes
            ((ImplementationImpl) inst.getImpl()).removeInst(inst);
        }
    }

}
