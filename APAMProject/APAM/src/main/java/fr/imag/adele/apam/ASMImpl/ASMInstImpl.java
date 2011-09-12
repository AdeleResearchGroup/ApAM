package fr.imag.adele.apam.ASMImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeTypeImpl;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.ASMImpl.SamInstEventHandler.NewApamInstance;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Instance;

public class ASMInstImpl extends AttributesImpl implements ASMInst {

    /** The logger. */
    // private static Logger logger = Logger.getLogger(ASMInstImpl.class);
    // private static ASMInstBroker myBroker = ASM.ASMInstBroker;

    private ASMImpl               myImpl;
    private Composite             myComposite;
    private Composite             rootComposite;
    protected Instance            samInst;
    private ApamDependencyHandler depHandler;

    public ApamDependencyHandler getDepHandler() {
        return depHandler;
    }

    private final Set<Wire> wires    = new HashSet<Wire>(); // the currently used instances
    private final Set<Wire> invWires = new HashSet<Wire>();

    public ASMInstImpl(ASMImpl impl, Composite instCompo, Attributes initialproperties, Instance samInst,
            boolean composite) {
        if (samInst == null) {
            new Exception("ERROR : sam instance cannot be null on ASM instance constructor").printStackTrace();
            return;
        }
        this.samInst = samInst;
        myImpl = impl;
        ((ASMImplImpl) impl).addInst(this);
        if (instCompo != null) { //null only if main appli instance.
            myComposite = instCompo;
            rootComposite = instCompo.getRootComposite();
            myComposite.addContainInst(this);
            this.setProperty(Attributes.APAMCOMPO, myComposite.getName());
        } else {
            this.setProperty(Attributes.APAMCOMPO, "root");
        }

        ((ASMInstBrokerImpl) CST.ASMInstBroker).addInst(this);

        //when called by Composite constructor. No associated handler. 
        if (composite)
            return;

        try {
            if (samInst.getServiceObject() instanceof ApamComponent)
                ((ApamComponent) samInst.getServiceObject()).apamStart(this);

            String implName = null;
            String specName = null;
            ApamDependencyHandler handler = null;
            // The apam handler arrived first : it stored the info in the object NewApamInstance
            // ApamDependencyHandler handler = SamInstEventHandler.getHandlerInstance(samInst.getName(), implName,
            // specName);
            NewApamInstance apamInst = SamInstEventHandler.getHandlerInstance(samInst.getName());
            if (apamInst != null) {
                handler = apamInst.handler;
                implName = apamInst.implName;
                specName = apamInst.specName;
            }
            // The event arrived first : it stored the info in the attributes
            if (handler == null) {
                handler = (ApamDependencyHandler) samInst.getProperty(CST.A_DEPHANDLER);
                // implName = (String) samInst.getProperty(CST.A_APAMIMPLNAME);
                specName = (String) samInst.getProperty(CST.A_APAMSPECNAME);
            }
            if (handler != null) { // it is an Apam instance
                depHandler = handler;
                handler.SetIdentifier(this);
                if ((specName != null) && (impl.getSpec()).getName().equals(samInst.getSpecification().getName()))
                    ((ASMSpecImpl) impl.getSpec()).setName(specName);
            }

            setProperties(Util.mergeProperties(this, initialproperties, samInst.getProperties()));

        } catch (ConnectionException e1) {
            e1.printStackTrace();
        }
    }

    // only for main appli instance.
    public void setComposite(Composite instCompo) {
        myComposite = instCompo;
        if (instCompo == null)
            return;
        this.setProperty(Attributes.APAMCOMPO, instCompo.getName());
        instCompo.addContainInst(this);
    }

    @Override
    public String toString() {
        return samInst.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.sam.Instance#getImplementation()
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
        try {
            return samInst.getServiceObject();
        } catch (ConnectionException e) {
            e.printStackTrace();
            return null;
        }
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
    public boolean createWire(ASMInst to, String depName, boolean deployed) {
        if ((to == null) || (depName == null))
            return false;

        for (Wire wire : wires) { // check if it already exists
            if ((wire.getDestination() == to) && wire.getDepName().equals(depName))
                return true;
        }

        if (!Wire.checkNewWire(getComposite(), to))
            return false;
        Wire wire = new Wire(this, to, depName);
        wires.add(wire);
        ((ASMInstImpl) to).invWires.add(wire);
        if (depHandler != null) {
            depHandler.setWire(to, depName);
        }

        //Other relationships to instantiate
        ((ASMImplImpl) getImpl()).addUses(to.getImpl());
        ((ASMSpecImpl) getSpec()).addRequires(to.getSpec());

        // if to has been deployed, and from is inside a composite (it is not a root)
        if (deployed && (getComposite() != null)) {
            getComposite().getCompType().addImpl(to.getImpl());
            if (to instanceof Composite) { //|| (returnedInst.getComposite().getMainInst() == returnedInst)) { //it is a composite
                ((CompositeTypeImpl) getComposite().getCompType()).addEmbedded(((Composite) to).getCompType());
            } else {
                if (to.getComposite().getMainInst() == to) { //to is a composite (or the main instance of a composite)
                    ((CompositeTypeImpl) getComposite().getCompType()).addEmbedded(to.getComposite().getCompType());
                }
            }
        }

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

    @Override
    public void remove() {
        for (Wire wire : invWires) {
            wire.remove();
        }
        for (Wire wire : wires) {
            wire.remove();
        }
        try {
            CST.ASMInstBroker.removeInst(this);
            // Should we delete the Sam instance,
            // TODO
            // samInst.delete();
            // or only remove the Apam attributes, such that SAMMAN knows which objects are APAM?
            samInst.removeProperty(Attributes.APAMAPPLI);
            samInst.removeProperty(Attributes.APAMCOMPO);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

    }

    @Override
    public ASMSpec getSpec() {
        return myImpl.getSpec();
    }

    @Override
    public String getName() {
        return samInst.getName();
    }

    @Override
    public Composite getComposite() {
        return myComposite;
    }

    @Override
    public Instance getSAMInst() {
        return samInst;
    }

    @Override
    public String getScope() {
        String scope = (String) getProperty(CST.A_SCOPE);
        if (scope == null)
            scope = CST.V_GLOBAL;
        //scope.toUpperCase();
        return scope;
    }

    @Override
    public String getShared() {
        String shared = (String) getProperty(CST.A_SHARED);
        if (shared == null)
            shared = CST.V_TRUE;
        //shared.toUpperCase();
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

    @Override
    public ApamDependencyHandler getDependencyHandler() {
        return depHandler;
    }

    @Override
    public void setDependencyHandler(ApamDependencyHandler handler) {
        if (handler == null)
            return;
        depHandler = handler;
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

        return rootComposite;
    }

}
