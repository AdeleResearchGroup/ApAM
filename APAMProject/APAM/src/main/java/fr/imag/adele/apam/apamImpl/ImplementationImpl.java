package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
//import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
//import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
//import fr.imag.adele.apam.util.Util;
//import fr.imag.adele.sam.Implementation;
//import fr.imag.adele.sam.Instance;
//import fr.imag.adele.sam.broker.ImplementationBroker;

public class ImplementationImpl extends AttributesImpl implements Implementation {

    protected final Set<Implementation>       uses              = new HashSet<Implementation>();      // all relations uses
    protected final Set<Implementation>       invUses           = new HashSet<Implementation>();      // all reverse relations uses

    protected final Set<CompositeType> inComposites      = new HashSet<CompositeType>(); // composite it is contained
                                                                                         // in.

    protected String                   name;
    protected Specification                  mySpec;
    protected ApformImplementation     apfImpl           = null;
    protected boolean                  used              = false;

    protected Set<Instance>             instances         = new HashSet<Instance>();      // the instances of that impl.
    protected Set<Instance>             sharableInstances = new HashSet<Instance>();      // the sharable instances of

    protected Set<DependencyModel>     dependencyModel;                                 // The dependencies of that

    /**
     * Instantiate a new service implementation.
     * 
     * @param instance the ASM instance
     * 
     * 
     */

    // used ONLY when creating a composite type
    protected ImplementationImpl() {

    }

    public ImplementationImpl(CompositeType compo, SpecificationImpl spec, ApformImplementation impl, Attributes props) {
        if (impl == null) {
            new Exception("Sam ApformImplementation cannot be null when creating an imple").printStackTrace();
        }
        if (compo == null) { // compo is null for the root composite AND its main implem.
            // done in create composite type when compo is null
            new Exception("compo is null").printStackTrace();
        }

        name = impl.getName(); // warning, for composites, it is a different name. Overloaded in createCOmpositeType
        mySpec = spec;
        spec.addImpl(this);
        ((ImplementationBrokerImpl) CST.ImplBroker).addImpl(this);
        apfImpl = impl;
        initializeNewImpl(compo, props);

    }

    // warning : for setting composite name, which is different from sam name.
    public void setName(String name) {
        this.name = name;
    }

    public void initializeNewImpl(CompositeType compo, Attributes props) {
        // myComposites.add(compo);
        compo.addImpl(this);
        if (props != null) {
            this.setProperties(props.getProperties());
        }
        setProperty(Attributes.APAMCOMPO, compo.getName());
        dependencyModel = (Set<DependencyModel>) getProperty(CST.A_DEPENDENCIES);
        // initialize properties. A fusion of SAM and APAM values
//        try {
//            this.setProperties(Util.mergeProperties(this, props, getSamImpl().getProperties()));
//        } catch (ConnectionException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public String toString() {
        return name;
        // String ret;
        // if (name == null)
        // ret = " (" + samImpl.getName() + ") ";
        // else
        // ret = name + " (" + samImpl.getName() + ") ";
        // return ret;
    }

    /**
     * From an implementation, create an instance. Creates both the SAM and ASM instances with the same properties.
     * 
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @throws ConnectionException
     */
    @Override
    public Instance createInst(Composite instCompo, Attributes initialproperties) {
        if ((getProperty(CST.A_INSTANTIABLE) != null) && getProperty(CST.A_INSTANTIABLE).equals(CST.V_FALSE)) {
            System.out.println("Implementation " + this + " is not instantiable");
            return null;
        }
        ApformInstance apfInst = apfImpl.createInstance(initialproperties);
        // if it is a composite. Not sure it is ever caller here.
        boolean composite = ((getProperty(CST.A_COMPOSITE) != null) && getProperty(CST.A_COMPOSITE).equals(CST.V_TRUE));
        InstanceImpl inst = new InstanceImpl(this, instCompo, initialproperties, apfInst, composite);
        return inst;
    }

    // WARNING : no control ! Only called by the instance Broker.
    public void addInst(Instance inst) {
        if (inst != null) {
            instances.add(inst);
            if (inst.isSharable())
                sharableInstances.add(inst);
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

    @Override
    public Set<Instance> getSharableInsts() {
        return Collections.unmodifiableSet(sharableInstances);
        // return new HashSet <ASMInst> (instances) ;
    }

    @Override
    public Set<Instance> getInsts(Filter query) throws InvalidSyntaxException {
        if (query == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (query.match((AttributesImpl) inst))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public Set<Instance> getSharableInsts(Filter query) throws InvalidSyntaxException {
        if (query == null)
            return getSharableInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : sharableInstances) {
            if (query.match((AttributesImpl) inst))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public Set<Instance> getInsts(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(instances);
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            for (Filter filter : constraints) {
                if (filter.match((AttributesImpl) inst)) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

    @Override
    public Set<Instance> getSharableInsts(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(sharableInstances);
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : sharableInstances) {
            for (Filter filter : constraints) {
                if (filter.match((AttributesImpl) inst)) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

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

    @Override
    public Instance getSharableInst(Set<Filter> constraints, List<Filter> preferences) {
        Set<Instance> insts = null;
        if ((preferences != null) && !preferences.isEmpty()) {
            insts = getSharableInsts(constraints);
        } else
            insts = sharableInstances;
        if ((constraints == null) || constraints.isEmpty())
            return ((Instance) insts.toArray()[0]);

        return getPreferedInst(insts, preferences);
    }

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
                if (!filter.match((AttributesImpl) inst))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = inst;
            }
        }
        System.out.println("   Selected : " + winner);
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
    @Override
    public String getVisible() {
        String visible = (String) getProperty(CST.A_VISIBLE);
        if (visible == null)
            visible = CST.V_GLOBAL;
//        String compoVisible;
//        for (CompositeType compoDest : getInCompositeType()) {
//            // Check if the composite type overloads the implementation scope
//            compoVisible = ((CompositeTypeImpl) compoDest).getVisibleInCompoType(this);
//            visible = Util.getEffectiveScope(visible, compoVisible);
//        }
        return visible;
    }

    @Override
    public String getShared() {
        String shared = (String) getProperty(CST.A_SHARED);
        if (shared == null)
            shared = CST.V_TRUE;
        return shared;
    }

    // @Override
    // public void setScope(String scope) {
    // if ((scope.equals(CST.V_TRUE) || (scope.equals(CST.V_FALSE))))
    // setProperty(CST.A_SCOPE, scope);
    // }

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
    @Override
    public void remove() {
        for (Instance inst : instances) {
            ((InstanceImpl) inst).remove();
        }
        CST.ImplBroker.removeImpl(this);
        ((SpecificationImpl) getSpec()).removeImpl(this);
        // remove the APAM specific attributes in SAM
//        if (apfImpl != null) {
//            try {
//                apfImpl.removeProperty(Attributes.APAMCOMPO);
//            } catch (ConnectionException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void removeInst(Instance inst) {
        instances.remove(inst);
        if (inst.isSharable())
            sharableInstances.remove(inst);
    }

    @Override
    public ApformImplementation getApformImpl() {
        return apfImpl;
    }

    @Override
    public boolean isInstantiable() {
        String instantiable = (String) getProperty(CST.A_INSTANTIABLE);
        return (instantiable == null) ? true : instantiable.equals(CST.V_TRUE);
    }

    @Override
    public Set<CompositeType> getInCompositeType() {
        return Collections.unmodifiableSet(inComposites);
    }

    @Override
    public Set<DependencyModel> getDependencies() {
        Set<DependencyModel> deps = (Set<DependencyModel>) getProperty(CST.A_DEPENDENCIES);
        if (deps == null)
            return Collections.EMPTY_SET;
        return deps;
    }

    @Override
    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
