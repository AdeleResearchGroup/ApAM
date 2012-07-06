package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.Dictionary;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.apform.ApformInstance;
//import fr.imag.adele.apam.util.Attributes;
//import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.core.InstanceDeclaration;

//import fr.imag.adele.sam.Instance;

public class InstanceImpl extends PropertiesImpl implements Instance {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /** The logger. */
    // private static Logger logger = Logger.getLogger(ASMInstImpl.class);
    // private static ASMInstBroker myBroker = ASM.ASMInstBroker;

    private final Object      id               = new Object();
    private Implementation    myImpl;
    private Composite         myComposite;
    protected ApformInstance  apformInst;
    //    private boolean           sharable         = true;
    private boolean           used             = false;
    private InstanceDeclaration declaration;

    private final Set<Wire>     wires            = Collections.newSetFromMap(new ConcurrentHashMap<Wire, Boolean>()); // the
    // currently
    // used
    // instances
    private final Set<Wire>     invWires         = Collections.newSetFromMap(new ConcurrentHashMap<Wire, Boolean>());

    // WARNING to be used only for empty root composite.
    protected InstanceImpl() {
    }

    @Override
    public boolean equals(Object o) {
        return (this == o);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    //    @Override
    //    public Object put(String attr, Object value) {
    //        //Util.validAttr(this, attr, value);
    //        return super.put(attr, value);
    //    }

    public void setAttr(String attr, Object value) {
        Util.validAttr(this, attr, value);
        put(attr, value);
    }

    /**
     * Instance creation. Should be the only way to create an instance, because the constructor *does not chain to the
     * broker*.
     * The constructor should be used *only* for creating composites,
     * because the new composite must be fully initialized before to be visible (through the broker) by others.
     * 
     * @param impl
     * @param instCompo
     * @param initialproperties
     * @param apformInst
     * @return
     */
    public static InstanceImpl newInstanceImpl(Implementation impl, Composite instCompo,
            Map<String, Object> initialproperties, ApformInstance apformInst) {

        InstanceImpl inst = new InstanceImpl (impl,instCompo,initialproperties,apformInst) ;

        ((InstanceBrokerImpl) CST.InstBroker).addInst( inst);
        return inst ;
    }

    /**
     * Should be used *only* by Composite impl constructor.
     * 
     * @param impl
     * @param instCompo
     * @param initialproperties
     * @param apformInst
     */
    protected InstanceImpl(Implementation impl, Composite instCompo, Map<String, Object> initialproperties,
            ApformInstance apformInst) {

        this.apformInst = apformInst;
        declaration = apformInst.getDeclaration();
        myImpl = impl;
        myComposite = instCompo;
        myComposite.addContainInst(this);
        apformInst.setInst(this);

        put(CST.A_INSTNAME, apformInst.getDeclaration().getName());
        // allready checked
        putAll(apformInst.getDeclaration().getProperties());
        if (initialproperties != null) {
            setAllProperties(initialproperties);
        }

        //calls Dynaman, for own ....
        if (instCompo == CompositeImpl.getRootAllComposites()) { // it is a root composite
            put(CST.A_COMPOSITE, "rootComposite");
            ApamManagers.notifyExternal(this) ;           
        } else {
            put(CST.A_COMPOSITE, myComposite.getName());
        }

        // not for composite instances, since getServiceObject is the main instance (allready started)
        if ((!(this instanceof Composite)) && (apformInst.getServiceObject() instanceof ApamComponent)) {
            ((ApamComponent) apformInst.getServiceObject()).apamStart(this);
        }
    }

    @Override
    public String toString() {
        return apformInst.getDeclaration().getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.ApformInstance#getImplementation()
     */
    @Override
    public Implementation getImpl() {
        return myImpl;
    }

    @Override
    public Object getServiceObject() {
        return apformInst.getServiceObject();
    }

    /**
     * returns the connections towards the service instances actually used. return only APAM wires. for SAM wires the
     * sam instance
     */
    @Override
    public Set<Instance> getWireDests(String depName) {
        Set<Instance> dests = new HashSet<Instance>();
        for (Wire wire : wires) {
            if (wire.getDepName().equals(depName))
                dests.add(wire.getDestination());
        }
        return dests;
    }


    /**
     */
    @Override
    public Set<Instance> getWireDests() {
        Set<Instance> dests = new HashSet<Instance>();
        for (Wire wire : wires) {
            dests.add(wire.getDestination());
        }
        return dests;
    }

    @Override
    public Set<Wire> getWires() {
        return Collections.unmodifiableSet(wires);
    }

    @Override
    public Set<Wire> getWires(String dependencyName) {
        Set<Wire> dests = new HashSet<Wire>();
        for (Wire wire : wires) {
            if (wire.getDepName().equals(dependencyName))
                dests.add(wire);
        }
        return dests;
    }

    @Override
    public boolean createWire(Instance to, String depName) {
        if ((to == null) || (depName == null))
            return false;

        for (Wire wire : wires) { // check if it already exists
            if ((wire.getDestination() == to) && wire.getDepName().equals(depName))
                return true;
        }

        // creation
        if (apformInst.setWire(to, depName)) {
            Wire wire = new WireImpl(this, to, depName);
            wires.add(wire);
            ((InstanceImpl) to).invWires.add(wire);
        } else {
            System.err.println("INTERNAL ERROR: wire from " + this + " to " + to
                    + " could not be created in the real instance.");
            return false;
        }

        // if the instance was in the unUsed pull, move it to the from composite.
        if (!to.isUsed()) {
            Apform.setUsedInst(to);
        }
        if (!isUsed())
            Apform.setUsedInst(this);
        if (to instanceof Composite) {
            Apform.setUsedInst(((Composite) to).getMainInst());
        }
        if (this instanceof Composite) {
            Apform.setUsedInst(((Composite) this).getMainInst());
        }

        // Other relationships to instantiate
        ((ImplementationImpl) getImpl()).addUses(to.getImpl());
        if ((SpecificationImpl) getSpec() != null)
            ((SpecificationImpl) getSpec()).addRequires(to.getSpec());
        return true;
    }

    @Override
    public void removeWire(Wire wire) {
        if (apformInst.remWire(wire.getDestination(), wire.getDepName())) {
            wires.remove(wire);
            ((ImplementationImpl) getImpl()).removeUses(wire.getDestination().getImpl());
        } else {
            System.err.println("INTERNAL ERROR: wire from " + this + " to " + wire.getDestination()
                    + " could not be removed in the real instance.");
        }
    }

    public void removeInvWire(Wire wire) {
        invWires.remove(wire);
        if (invWires.isEmpty()) { // This instance ins no longer used.
            setUsed(false);
        }
    }

    /**
     * remove from ASM It deletes the wires, which deletes the isolated used instances, and transitively. It deleted the
     * invWires, which removes the associated real dependency :
     */

    public void remove() {
        for (Wire wire : invWires) {
            ((WireImpl) wire).remove();
        }
        for (Wire wire : wires) {
            ((WireImpl) wire).remove();
        }
    }

    @Override
    public Specification getSpec() {
        return myImpl.getSpec();
    }

    @Override
    public String getName() {
        return apformInst.getDeclaration().getName();
    }

    @Override
    public Composite getComposite() {
        return myComposite;
    }

    @Override
    public ApformInstance getApformInst() {
        return apformInst;
    }

    //    @Override
    //    public boolean isSharable() {
    //        if (getProperty(CST.A_SHARED) == null)
    //            return true;
    //        return getProperty(CST.A_SHARED).equals(CST.V_TRUE);
    //    }

    //    @Override
    //    public String getShared() {
    //        String shared = (String) get(CST.A_SHARED);
    //        if (shared == null) {
    //            shared = getImpl().getShared();
    //        }
    //        if (shared == null)
    //            shared = CST.V_TRUE;
    //        // shared.toUpperCase();
    //        return shared;
    //    }

    @Override
    public boolean match(String filter) {
        return match(ApamFilter.newInstance(filter));
    }

    @Override
    public boolean match(Filter goal) {
        if (goal == null)
            return true;
        try {
            return ((ApamFilter) goal).matchCase(getAllProperties());
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public boolean match(Set<Filter> goals) {
        if ((goals == null) || goals.isEmpty())
            return true;
        Map props = getAllProperties() ;
        try {
            for (Filter f : goals) {
                if (!((ApamFilter) f).matchCase(props)) {
                    return false ;
                }
            }
            return true;
        } catch (Exception e) {
            return false ;
        }
    }


    @Override
    public Set<Wire> getInvWires() {
        return Collections.unmodifiableSet(invWires);
    }

    @Override
    public Set<Wire> getInvWires(String depName) {
        Set<Wire> w = new HashSet<Wire>();
        for (Wire wire : invWires) {
            if ((wire.getDestination() == this) && (wire.getDepName().equals(depName)))
                w.add(wire);
        }
        return w;
    }

    @Override
    public Wire getInvWire(Instance destInst) {
        if (destInst == null)
            return null;
        for (Wire wire : invWires) {
            if (wire.getDestination() == destInst)
                return wire;
        }
        return null;
    }

    @Override
    public Wire getInvWire(Instance destInst, String depName) {
        if (destInst == null)
            return null;
        for (Wire wire : invWires) {
            if ((wire.getDestination() == destInst) && (wire.getDepName().equals(depName)))
                return wire;
        }
        return null;
    }

    @Override
    public Set<Wire> getInvWires(Instance destInst) {
        if (destInst == null)
            return null;
        Set<Wire> w = new HashSet<Wire>();
        for (Wire wire : invWires) {
            if (wire.getDestination() == destInst)
                w.add(wire);
        }
        return w;
    }

    @Override
    public Set<Wire> getWires(Specification spec) {
        if (spec == null)
            return null;
        Set<Wire> w = new HashSet<Wire>();
        for (Wire wire : invWires) {
            if (wire.getDestination().getSpec() == spec)
                w.add(wire);
        }
        return w;
    }

    @Override
    public Composite getAppliComposite() {
        return myComposite.getAppliComposite();
    }

    @Override
    public final boolean isUsed() {
        return used;
    }

    public final void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public boolean isSharable() {
        return (getInvWires().isEmpty()
                || (getProperty(CST.A_SHARED) == null) || (getProperty(CST.A_SHARED).equals(CST.V_TRUE)));
    }

    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

}
