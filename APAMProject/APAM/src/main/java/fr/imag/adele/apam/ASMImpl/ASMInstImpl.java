package fr.imag.adele.apam.ASMImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apform.ApformImpl;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;

//import fr.imag.adele.sam.Instance;

public class ASMInstImpl extends AttributesImpl implements ASMInst {

    /** The logger. */
    // private static Logger logger = Logger.getLogger(ASMInstImpl.class);
    // private static ASMInstBroker myBroker = ASM.ASMInstBroker;

    private ASMImpl          myImpl;
    private Composite        myComposite;
    protected ApformInstance apformInst;
    private boolean          sharable = true;
//    private ApamDependencyHandler depHandler;

//    public ApamDependencyHandler getDepHandler() {
//        return depHandler;
//    }

    private final Set<Wire>  wires    = new HashSet<Wire>(); // the currently used instances
    private final Set<Wire>  invWires = new HashSet<Wire>();

    // WARNING to be used only for creating composites.
    public ASMInstImpl() {
    }

    protected void instConstructor(ASMImpl impl, Composite instCompo, Attributes initialproperties,
            ApformInstance samInst) {
        if (samInst == null) {
            new Exception("ERROR : sam instance cannot be null on ASM instance constructor").printStackTrace();
            return;
        }
        if (instCompo == null) {
            new Exception("no composite in instance contructor" + samInst);
        }
        if (impl.getShared().equals(CST.V_FALSE))
            sharable = false;
        apformInst = samInst;
        myImpl = impl;
        myComposite = instCompo;
        myComposite.addContainInst(this);
        this.setProperty(Attributes.APAMCOMPO, myComposite.getName());
        ((ASMInstBrokerImpl) CST.ASMInstBroker).addInst(this);
    }

    public ASMInstImpl(ASMImpl impl, Composite instCompo, Attributes initialproperties, ApformInstance apformInst,
            boolean composite) {
        // Create the implementation and initialize
        instConstructor(impl, instCompo, initialproperties, apformInst);

        // Compute the handler for apam components
//        try {
//            ApamDependencyHandler handler = SamInstEventHandler.getHandlerInstance(apformInst.getName());
//
//            // The Sam event arrived first : it stored the info in the attributes
//            if (handler == null) {
//                handler = (ApamDependencyHandler) apformInst.getProperty(CST.A_DEPHANDLER);
//            }
//
//            if (handler != null) { // it is an Apam instance
//                depHandler = handler;
//                handler.SetIdentifier(this);
//            }

//            setProperties(Util.mergeProperties(this, initialproperties, apformInst.getProperties()));
        setProperties(apformInst.getProperties());
        setProperty(CST.A_SHARED, getShared());
        sharable = (getShared().equals(CST.V_TRUE));

        if ((instCompo != null) && (apformInst.getServiceObject() instanceof ApamComponent))
            ((ApamComponent) apformInst.getServiceObject()).apamStart(this);
//        } catch (ConnectionException e1) {
//            e1.printStackTrace();
//        }

    }

    @Override
    public String toString() {
        return apformInst.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.ApformInstance#getImplementation()
     */
    @Override
    public ASMImpl getImpl() {
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
    public Set<ASMInst> getWireDests(String depName) {
        Set<ASMInst> dests = new HashSet<ASMInst>();
        for (Wire wire : wires) {
            if (wire.getDepName().equals(depName))
                dests.add(wire.getDestination());
        }
        return dests;
    }

    /**
     * returns the connections towards the service instances actually used. return only APAM wires. for SAM wires the
     * sam instance
     */
    @Override
    public Set<ASMInst> getWireDests() {
        Set<ASMInst> dests = new HashSet<ASMInst>();
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
    public boolean createWire(ASMInst to, String depName) {
        if ((to == null) || (depName == null))
            return false;

        for (Wire wire : wires) { // check if it already exists
            if ((wire.getDestination() == to) && wire.getDepName().equals(depName))
                return true;
        }

        if (!Wire.checkNewWire(this, to, depName))
            return false;
        Wire wire = new Wire(this, to, depName);
        wires.add(wire);

        ((ASMInstImpl) to).invWires.add(wire);
        // if (apformI != null) {
        apformInst.setWire(to, depName);
        // }

        // if the instance was in the unUsed pull, move it to the from composite.
        if (ApformImpl.getUnusedInst(to) != null) {
            ApformImpl.setUsedInst(to);
            getComposite().addContainInst(to);
        }

        // Other relationships to instantiate
        ((ASMImplImpl) getImpl()).addUses(to.getImpl());
        ((ASMSpecImpl) getSpec()).addRequires(to.getSpec());
        return true;
    }

    @Override
    public void removeWire(Wire wire) {
        wires.remove(wire);
        ((ASMImplImpl) getImpl()).removeUses(wire.getDestination().getImpl());
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
//        try {
        // CST.ASMInstBroker.removeInst(this);
        // ((ASMImplImpl) getImpl()).removeInst(this);
        // Should we delete the Sam instance,
        // TODO
        // samInst.delete();
        // or only remove the Apam attributes, such that SAMMAN knows which objects are APAM?
        // samInst.removeProperty(Attributes.APAMAPPLI);
        // apformInst.removeProperty(Attributes.APAMCOMPO);
//        } catch (ConnectionException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public ASMSpec getSpec() {
        return myImpl.getSpec();
    }

    @Override
    public String getName() {
        return apformInst.getName();
    }

    @Override
    public Composite getComposite() {
        return myComposite;
    }

    @Override
    public ApformInstance getApformInst() {
        return apformInst;
    }

    @Override
    public String getScope() {
        // Check if the composite type overloads the implementation scope
        return ((CompositeTypeImpl) myComposite.getCompType()).getScopeInComposite(this);
    }

    @Override
    public String getShared() {
        String shared = (String) getProperty(CST.A_SHARED);
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
            return false;
        try {
            return goal.match((AttributesImpl) getProperties());
        } catch (Exception e) {
        }
        return false;
    }

//    @Override
//    public ApamDependencyHandler getDependencyHandler() {
//        return depHandler;
//    }
//
//    @Override
//    public void setDependencyHandler(ApamDependencyHandler handler) {
//        if (handler == null)
//            return;
//        depHandler = handler;
//    }

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
    public Wire getWire(ASMInst destInst) {
        if (destInst == null)
            return null;
        for (Wire wire : invWires) {
            if (wire.getDestination() == destInst)
                return wire;
        }
        return null;
    }

    @Override
    public Wire getWire(ASMInst destInst, String depName) {
        if (destInst == null)
            return null;
        for (Wire wire : invWires) {
            if ((wire.getDestination() == destInst) && (wire.getDepName().equals(depName)))
                return wire;
        }
        return null;
    }

    @Override
    public Set<Wire> getWires(ASMInst destInst) {
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
    public Composite getRootComposite() {
        return myComposite.getRootComposite();
    }

    @Override
    public boolean isSharable() {
        return sharable;
    }
}
