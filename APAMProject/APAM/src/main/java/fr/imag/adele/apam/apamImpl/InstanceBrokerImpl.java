package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.LocalMachine;
//import fr.imag.adele.am.Machine;
//import fr.imag.adele.am.eventing.AMEventingHandler;
//import fr.imag.adele.am.eventing.EventingEngine;
//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
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
    //    private final Set<Instance> sharableInstances = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());
    private final Set<Instance> instances         = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());
    private static final ImplementationBroker implBroker        = CST.ImplBroker;

    //  private final Set<Instance>               instances         = new HashSet<Instance>();
    //    private final Set<Instance>               sharableInstances = new HashSet<Instance>();

    public InstanceBrokerImpl() {
    }

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

    //    @Override
    //    public Set<Instance> getSharableInsts() {
    //        return Collections.unmodifiableSet(sharableInstances);
    //    }

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
        }

        // Composite implementations can be installed too.
        if (impl instanceof CompositeType) {
            inst = CompositeImpl.newCompositeImpl((CompositeType) impl, instComposite, null, properties, apfInst);
        }
        else {
            inst = InstanceImpl.newInstanceImpl(impl, instComposite, null, apfInst);
        }

        return inst;
    }

    // adds both in the broker and in its implem
    public void addInst(Instance inst) {
        if ((inst != null) && !instances.contains(inst)) {
            instances.add(inst);
            ApamManagers.notifyAddedInApam(inst);
            ((ImplementationImpl) inst.getImpl()).addInst(inst);
            //            if (inst.isSharable())
            //                sharableInstances.add(inst);
        }
    }

    protected void removeInst(Instance inst) {
        if (inst == null)
            return;
        if (instances.contains(inst)) {
            instances.remove(inst);
            ApamManagers.notifyRemovedFromApam(inst);
            //            sharableInstances.remove(inst);
            ((InstanceImpl) inst).remove(); // wires and sam attributes
            ((ImplementationImpl) inst.getImpl()).removeInst(inst);
        }
    }

}
