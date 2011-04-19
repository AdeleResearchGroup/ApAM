package fr.imag.adele.apam.samAPIImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.samAPIImpl.SamInstEventHandler.NewApamInstance;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Instance;

public class ASMInstImpl extends AttributesImpl implements ASMInst {

    /** The logger. */
    // private static Logger logger = Logger.getLogger(ASMInstImpl.class);
    // private static ASMInstBroker myBroker = ASM.ASMInstBroker;

    // private String name;
    private ASMImpl               myImpl;
    private Composite             myComposite;
    private Instance              samInst;
    private ApamDependencyHandler depHandler;

    public ApamDependencyHandler getDepHandler() {
        return depHandler;
    }

    private final Set<Wire> wires    = new HashSet<Wire>(); // the currently used instances
    private final Set<Wire> invWires = new HashSet<Wire>();

    public ASMInstImpl(Composite compo, ASMImpl impl, Attributes initialproperties, Instance samInst) {
        myImpl = impl;
        myComposite = compo;
        if (samInst == null) {
            System.err.println("ERROR : sam instance cannot be null on ASM instance constructor");
            return;
        }
        this.samInst = samInst;

        ((ASMInstBrokerImpl) ASM.ASMInstBroker).addInst(this);
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
                handler = (ApamDependencyHandler) samInst.getProperty(ASM.APAMDEPENDENCYHANDLER);
                implName = (String) samInst.getProperty(ASM.APAMIMPLNAME);
                specName = (String) samInst.getProperty(ASM.APAMSPECNAME);
            }
            if (handler != null) { // it is an Apam instance
                depHandler = handler;
                handler.SetIdentifier(this);
                if (implName != null)
                    impl.setASMName(implName);
                if (specName != null)
                    ((ASMSpecImpl) impl.getSpec()).setASMName(specName);
            }

            setProperties(Util.mergeProperties(initialproperties, samInst.getProperties()));
            this.setProperty(Attributes.APAMAPPLI, compo.getApplication().getName());
            this.setProperty(Attributes.APAMCOMPO, compo.getName());

            compo.addInst(this);
        } catch (ConnectionException e1) {
            e1.printStackTrace();
        }
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
    public boolean createWire(ASMInst to, String depName) {
        if ((to == null) || (depName == null))
            return false;

        for (Wire wire : wires) { // check if it allready exists
            if ((wire.getSource() == to) && wire.getDepName().equals(depName))
                return true;
        }

        if (!Wire.checkNewWire(this, to))
            return false;
        Wire wire = new Wire(this, to, depName);
        wires.add(wire);
        ((ASMInstImpl) to).invWires.add(wire);
        if (depHandler != null) {
            depHandler.setWire(to, depName);
        }
        return true;
    }

    @Override
    public void removeWire(Wire wire) {
        wires.remove(wire);
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
            ASM.ASMInstBroker.removeInst(this);
            samInst.delete();
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

    }

    @Override
    public ASMSpec getSpec() {
        return myImpl.getSpec();
    }

    @Override
    public String getASMName() {
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
    public String getClonable() {
        return (String) getProperty(Attributes.CLONABLE);
    }

    @Override
    public String getShared() {
        return (String) getProperty(Attributes.SHARED);
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

    //
    // public Set<ASMInst> getClients() {
    // Set<ASMInst> clients = new HashSet<ASMInst>();
    // for (Wire wire : invWires.keySet()) {
    // clients.add(wire.getDestination());
    // }
    // return clients;
    // }

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

}
