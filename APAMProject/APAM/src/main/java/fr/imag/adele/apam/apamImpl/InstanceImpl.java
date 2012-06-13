package fr.imag.adele.apam.apamImpl;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;

//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.apform.ApformInstance;
//import fr.imag.adele.apam.util.Attributes;
//import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.core.InstanceDeclaration;

//import fr.imag.adele.sam.Instance;

public class InstanceImpl extends ConcurrentHashMap<String, Object> implements Instance {

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
    private boolean           sharable         = true;
    private boolean           used             = false;
    private InstanceDeclaration declaration;

    private final Set<Wire>   wires            = new HashSet<Wire>(); // the currently used instances
    private final Set<Wire>   invWires         = new HashSet<Wire>();

    // WARNING to be used only for empty root composite.
    public InstanceImpl() {
    }

    @Override
    public boolean equals(Object o) {
        return (this == o);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    //    private void instConstructor(Implementation impl, Composite instCompo, Map<String, Object> initialproperties,
    //            ApformInstance apfInst) {
    //        assert (apfInst != null);
    //        assert (instCompo != null);
    //
    //        if (impl.getShared().equals(CST.V_FALSE))
    //            sharable = false;
    //        apformInst = apfInst;
    //        declaration = apfInst.getDeclaration();
    //        myImpl = impl;
    //        myComposite = instCompo;
    //        myComposite.addContainInst(this);
    //        put(CST.A_INSTNAME, apfInst.getDeclaration().getName());
    //        put(CST.A_COMPOSITE, myComposite.getName());
    //        ((InstanceBrokerImpl) CST.InstBroker).addInst(this);
    //    }

    public InstanceImpl(Implementation impl, Composite instCompo, Map<String, Object> initialproperties,
            ApformInstance apformInst) {
        // Create the implementation and initialize

        if (impl.getShared().equals(CST.V_FALSE))
            sharable = false;
        this.apformInst = apformInst;
        declaration = apformInst.getDeclaration();
        myImpl = impl;
        myComposite = instCompo;
        myComposite.addContainInst(this);
        apformInst.setInst(this);
        put(CST.A_INSTNAME, apformInst.getDeclaration().getName());
        put(CST.A_COMPOSITE, myComposite.getName());
        ((InstanceBrokerImpl) CST.InstBroker).addInst(this);

        //        instConstructor(impl, instCompo, initialproperties, apformInst);
        putAll(apformInst.getDeclaration().getProperties());
        put(CST.A_SHARED, getShared());

        // not for composite instances, since getServiceObject is the main instance (allready started)
        if ((!(this instanceof Composite)) && (apformInst.getServiceObject() instanceof ApamComponent)) {
            ((ApamComponent) apformInst.getServiceObject()).apamStart(this);
        }
        //calls Dynaman, for own ....
        if (instCompo == CompositeImpl.getRootAllComposites()) { //it is an external composite
            ApamManagers.notifyExternal(this) ;           
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

    // public String getName() {
    // return name ;
    // }

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
     * 
     */
    //    @Override
    //    public Set<Instance> getWireTypeDests(String destType) {
    //        Set<Instance> dests = new HashSet<Instance>();
    //        Class dest = Class.forName(destType);
    //        for (Wire wire : wires) {
    //            if (wire.getDestination().getApformInst().getServiceObject() instanceof dest)
    //                dests.add(wire.getDestination());
    //        }
    //        return dests;
    //    }

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

        // useless when called by Apam. Needed if called by an external program.
        //        if (!Wire.checkNewWire(this, to, depName))
        //            return false;

        // creation
        Wire wire = new Wire(this, to, depName);
        wires.add(wire);
        ((InstanceImpl) to).invWires.add(wire);

        apformInst.setWire(to, depName);

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
        ((SpecificationImpl) getSpec()).addRequires(to.getSpec());
        return true;
    }

    @Override
    public void removeWire(Wire wire) {
        wires.remove(wire);
        ((ImplementationImpl) getImpl()).removeUses(wire.getDestination().getImpl());
    }

    public void removeInvWire(Wire wire) {
        invWires.remove(wire);
        if (invWires.isEmpty()) { // This instance ins no longer used. Delete it
            remove();
        }
    }

    /**
     * remove from ASM It deletes the wires, which deletes the isolated used instances, and transitively. It deleted the
     * invWires, which removes the associated real dependency :
     */

    public void remove() {
        for (Wire wire : invWires) {
            wire.remove();
        }
        for (Wire wire : wires) {
            wire.remove();
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
    //    public String getScope() {
    //        // Check if the composite type overloads the implementation scope
    //        return ((CompositeTypeImpl) myComposite.getCompType()).getScopeInComposite(this);
    //    }

    @Override
    public String getShared() {
        String shared = (String) get(CST.A_SHARED);
        if (shared == null) {
            shared = getImpl().getShared();
        }
        if (shared == null)
            shared = CST.V_TRUE;
        // shared.toUpperCase();
        return shared;
    }

    @Override
    public boolean match(Filter goal) {
        if (goal == null)
            return true;
        try {
            return ((FilterImpl) goal).matchCase(getAllProperties());
        } catch (Exception e) {
        }
        return false;
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
        return (used) ? sharable : true;
    }

    /**
     * Here we assume that attributes are valid and do not overlap.
     */
    @Override
    public Map<String, Object> getAllProperties() {
        Map<String, Object> allProps = new HashMap<String, Object>();
        allProps.putAll(this);
        allProps.putAll(getImpl().getAllProperties());
        return allProps;
    }

    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

}
