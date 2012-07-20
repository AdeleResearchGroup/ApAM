package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public class ImplementationImpl extends PropertiesImpl implements Implementation {

    private static final long           serialVersionUID = 1L;

    // all relationship use and their reverse
    protected final Set<Implementation> uses             = Collections
    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());
    protected final Set<Implementation> invUses           = Collections
    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    // composite in which it is contained
    protected final Set<CompositeType>  inComposites      = Collections
    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    private final Object                id                = new Object();                 // only for hashCode
    protected ImplementationDeclaration declaration;

    protected String                    name;
    protected Specification             mySpec;
    protected ApformImplementation      apfImpl           = null;
    protected boolean                   used              = false;

    // the instances
    protected Set<Instance>             instances        = Collections
    .newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

	Logger logger =  LoggerFactory.getLogger(ImplementationImpl.class);

    // the sharable instances
    //    protected Set<Instance>             sharableInstances = new HashSet<Instance>();      // the sharable instances

    @Override
    public boolean equals(Object o) {
        return (this == o);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Instantiate a new service implementation.
     */
    // used ONLY when creating a composite type
    protected ImplementationImpl() {
    }

    public ImplementationImpl(CompositeType compo, String specName, ApformImplementation apfImpl, Map<String, Object> props) {
        assert ((apfImpl  != null) || (specName != null));
        assert (compo != null);

        // specification control. Spec usually does not exist in Apform, but we need to create one anyway.
        SpecificationImpl spec = null;
        ApformSpecification apfSpec = null;
        if (apfImpl != null) {
            apfSpec = apfImpl.getSpecification();
            if (apfSpec != null) { // may be null !
                spec = (SpecificationImpl) CST.SpecBroker.getSpec(apfSpec);
            }
        } else {
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(specName);
        }
        if ((spec == null) && (specName != null)) // No ASM spec related to the apf spec.
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(specName);
        if (spec == null)
            spec = (SpecificationImpl) CST.SpecBroker.getSpec(apfImpl.getDeclaration().getProvidedResources());
        if (spec == null) {
            if (specName == null) { // create an arbitrary name, and give the impl interface.
                // TODO warning, it is an approximation, impl may have more interfaces than its spec
                specName = apfImpl.getDeclaration().getName() + "_spec";
            }
            spec = new SpecificationImpl(specName, apfSpec, apfImpl.getDeclaration().getProvidedResources(), props);
        }

        name = apfImpl.getDeclaration().getName(); // warning, for composites, it is a different name. Overloaded in createCompositeType
        put(CST.A_IMPLNAME, name);
        mySpec = spec;
        spec.addImpl(this);
        ((ImplementationBrokerImpl) CST.ImplBroker).addImpl(this);
        this.apfImpl = apfImpl;
        initializeNewImpl(compo, props);
    }


    public void initializeNewImpl(CompositeType compoType, Map<String, Object> props) {
        declaration = apfImpl.getDeclaration();
        compoType.addImpl(this);
        if (props != null) {
            setAllProperties(props);
        }
        // put(CST.A_COMPOSITETYPE, compoType.getName());
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * From an implementation, create an instance. Creates both the apform and APAM instances.
     * 
     */
    protected Instance createInst(Composite instCompo, Map<String, Object> initialproperties) {
        if ((get(CST.A_INSTANTIABLE) != null) && get(CST.A_INSTANTIABLE).equals(CST.V_FALSE)) {
        	logger.debug("Implementation " + this + " is not instantiable");
            return null;
        }
        ApformInstance apfInst = apfImpl.createInstance(initialproperties);
        InstanceImpl inst = InstanceImpl.newInstanceImpl(this, instCompo, initialproperties, apfInst);
        return inst;
    }

    /**
     * From an implementation, create an instance. Creates both the apform and APAM instances.
     * Can be called from the API. Must check if instCompo can instantiate.
     */
    @Override
    public Instance createInstance(Composite instCompo, Map<String, Object> initialproperties) {
        if ((instCompo != null) && !Util.checkImplVisible(instCompo.getCompType(), this)) {
            logger.error("cannot instantiate " + this + ". It is not visible from composite " + instCompo);
            return null;
        }
        return createInst(instCompo, initialproperties);
    }

    @Override
    public boolean match(Filter goal) {
        if (goal == null)
            return true;
        try {
            return ((ApamFilter) goal).matchCase(getAllProperties());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    // WARNING : no control ! Only called by the instance Broker.
    public void addInst(Instance inst) {
        if (inst != null) {
            instances.add(inst);
            //            if (inst.isSharable())
            //                sharableInstances.add(inst);
        }
    }

    @Override
    public Specification getSpec() {
        return mySpec;
    }

    @Override
    public Instance getInst(String targetName) {
        if (targetName == null)
            return null;
        for (Instance inst : instances) {
            if (inst.getName().equals(targetName))
                return inst;
        }
        return null;
    }

    /**
     * returns the first instance only.
     */
    @Override
    public Instance getInst() {
        if (instances.size() == 0)
            return null;
        return (Instance) instances.toArray()[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.Implementation#getInstances()
     */
    @Override
    public Set<Instance> getInsts() {
        return Collections.unmodifiableSet(instances);
        // return new HashSet <ASMInst> (instances) ;
    }

    //    @Override
    //    public Set<Instance> getSharableInsts() {
    //        return Collections.unmodifiableSet(sharableInstances);
    //        // return new HashSet <ASMInst> (instances) ;
    //    }

    @Override
    public Set<Instance> getInsts(Filter query) throws InvalidSyntaxException {
        if (query == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (inst.match(query))
                ret.add(inst);
        }
        return ret;
    }

    //    @Override
    //    public Set<Instance> getSharableInsts(Filter query) throws InvalidSyntaxException {
    //        if (query == null)
    //            return getSharableInsts();
    //        Set<Instance> ret = new HashSet<Instance>();
    //        for (Instance inst : sharableInstances) {
    //            if (inst.match(query))
    //                ret.add(inst);
    //        }
    //        return ret;
    //    }

    @Override
    public Set<Instance> getInsts(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(instances);
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            for (Filter filter : constraints) {
                if (inst.match(filter)) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

    //    @Override
    //    public Set<Instance> getSharableInsts(Set<Filter> constraints) {
    //        if ((constraints == null) || constraints.isEmpty())
    //            return Collections.unmodifiableSet(sharableInstances);
    //        Set<Instance> ret = new HashSet<Instance>();
    //        for (Instance inst : sharableInstances) {
    //            for (Filter filter : constraints) {
    //                if (inst.match(filter)) {
    //                    ret.add(inst);
    //                }
    //            }
    //        }
    //        return ret;
    //    }

    @Override
    public Instance getInst(Set<Filter> constraints, List<Filter> preferences) {
        Set<Instance> insts = null;
        if ((preferences != null) && !preferences.isEmpty()) {
            insts = getInsts(constraints);
        } else
            insts = instances;
        if ((constraints == null) || constraints.isEmpty())
            return ((Instance) insts.toArray()[0]);

        return getPreferedInst(insts, preferences);
    }

    //    @Override
    //    public Instance getSharableInst(Set<Filter> constraints, List<Filter> preferences) {
    //        Set<Instance> insts = null;
    //        if ((preferences != null) && !preferences.isEmpty()) {
    //            insts = getSharableInsts(constraints);
    //        } else
    //            insts = sharableInstances;
    //
    //        if (insts.isEmpty())
    //            return null;
    //
    //        if ((constraints == null) || constraints.isEmpty())
    //            return ((Instance) insts.toArray()[0]);
    //
    //        return getPreferedInst(insts, preferences);
    //    }

    @Override
    public Instance getPreferedInst(Set<Instance> candidates, List<Filter> preferences) {
        if ((preferences == null) || preferences.isEmpty()) {
            return (Instance) candidates.toArray()[0];
        }
        Instance winner = null;
        int maxMatch = -1;
        for (Instance inst : candidates) {
            int match = 0;
            for (Filter filter : preferences) {
                if (!inst.match(filter))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = inst;
            }
        }
        logger.debug("   Selected : " + winner);
        return winner;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * returns the visibility.
     * WARNING : an implem can pertain to various composite types that can overload (reduce) the visibility.
     * Is returned only the intrinsic visibility.
     */
    //    @Override
    //    public String getVisible() {
    //        String visible = (String) get(CST.A_VISIBLE);
    //        if (visible == null)
    //            visible = CST.V_GLOBAL;
    //        return visible;
    //    }

    //    @Override
    //    public String getShared() {
    //        String shared = (String) get(CST.A_SHARED);
    //        if (shared == null)
    //            shared = CST.V_TRUE;
    //        return shared;
    //    }

    // relation uses control
    public void addUses(Implementation dest) {
        if (uses.contains(dest))
            return;
        uses.add(dest);
        ((ImplementationImpl) dest).addInvUses(this);
        ((SpecificationImpl) getSpec()).addRequires(dest.getSpec());
    }

    public void removeUses(Implementation dest) {
        for (Instance inst : instances) {
            for (Instance instDest : inst.getWireDests())
                if (instDest.getImpl() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        uses.remove(dest);
        ((ImplementationImpl) dest).removeInvUses(this);
        ((SpecificationImpl) getSpec()).removeRequires(dest.getSpec());
    }

    private void addInvUses(Implementation orig) {
        invUses.add(orig);
    }

    private void removeInvUses(Implementation orig) {
        invUses.remove(orig);
    }

    public void addInComposites(CompositeType compo) {
        inComposites.add(compo);
    }

    public void removeInComposites(CompositeType compo) {
        inComposites.remove(compo);
    }

    @Override
    public Set<Implementation> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    @Override
    public Set<Implementation> getInvUses() {
        return Collections.unmodifiableSet(invUses);
    }

    //
    public void remove() {
        for (Instance inst : instances) {
            ((InstanceBrokerImpl)CST.InstBroker).removeInst(inst,false);
        }
    }

    public void removeInst(Instance inst) {
        instances.remove(inst);
        //        if (inst.isSharable())
        //            sharableInstances.remove(inst);
    }

    @Override
    public ApformImplementation getApformImpl() {
        return apfImpl;
    }

    @Override
    public boolean isInstantiable() {
        String instantiable = (String) get(CST.A_INSTANTIABLE);
        return (instantiable == null) ? true : instantiable.equals(CST.V_TRUE);
    }

    @Override
    public Set<CompositeType> getInCompositeType() {
        return Collections.unmodifiableSet(inComposites);
    }

    @Override
    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }


    @Override
    public ImplementationDeclaration getImplDeclaration() {
        return declaration;
    }

    /**
     * only here for future optimization.
     * shared is applied on all the instances
     */
    @Override
    public boolean isSharable() {
        if (get(CST.A_SHARED) == null)
            return true;
        return get(CST.A_SHARED).equals(CST.V_TRUE);
    }
}
