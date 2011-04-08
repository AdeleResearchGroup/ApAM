package fr.imag.adele.apam.samAPIImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
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
    private static ASMInstBroker     myBroker = ASM.ASMInstBroker;

    // private String name;
    private ASMImpl                  myImpl;
    private Composite                myComposite;
    private Instance                 samInst;
    private ApamDependencyHandler    depHandler;

    private final Map<ASMInst, Wire> wires    = new HashMap<ASMInst, Wire>(); // the currently used instances
    private final Map<ASMInst, Wire> invWires = new HashMap<ASMInst, Wire>();

    public ASMInstImpl(Composite compo, ASMImpl impl, Attributes initialproperties, Instance samInst) {
        myImpl = impl;
        myComposite = compo;
        if (samInst == null) {
            System.out.println("erreur : sam instance cannot be null on ASM instance constructor");
            return;
        }
        this.samInst = samInst;
        // name = samInst.getName();

        ((ASMInstBrokerImpl) ASM.ASMInstBroker).addInst(this);
        // Check if it is an APAM instance
        try {
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
    public Set<ASMInst> getWires() {
        return wires.keySet();
    }

    public Set<Filter> getWireConstraint(ASMInst to) {
        if (to == null)
            return null;
        if (wires.get(to) == null)
            return null;
        return wires.get(to).getConstraints();
    }

    @Override
    public boolean setWire(ASMInst to, String depName, Filter filter) {
        Set<Filter> constraints = new HashSet<Filter>();
        constraints.add(filter);
        return setWire(to, depName, constraints);
    }

    @Override
    public boolean setWire(ASMInst to, String depName, Set<Filter> constraints) {
        if ((to == null) || (depName == null))
            return false;

        if (wires.get(to) != null)
            return true;
        if (!Wire.checkNewWire(this, to))
            return false;
        Wire wire = new Wire(this, to, depName, constraints);
        wires.put(to, wire);
        ((ASMInstImpl) to).setInvWire(this, wire);
        if (depHandler != null) {
            depHandler.setWire(to, depName);
        }
        return true;
    }

    // Not in the interface
    private void setInvWire(ASMInst from, Wire wire) {
        invWires.put(from, wire);
    }

    /**
     * The removed wire is not considered as lost; the client may still be active. The state is not changed.
     * 
     * @param to
     */
    @Override
    public void removeWire(ASMInst to) {
        if (to == null)
            return;
        removeWire(to, null);
    }

    private void removeWire(ASMInst to, ASMInst newTo) {
        if ((to == null) || (newTo == null))
            return;
        Wire wire = wires.get(to);
        if (wire == null)
            return;
        wires.remove(to);
        ((ASMInstImpl) to).removeInvWire(this);
        if (depHandler != null) {
            if (newTo == null) {
                depHandler.remWire(to, wire.getDepName());
            } else {
                depHandler.substWire(to, newTo, wire.getDepName());
            }
        }
    }

    private void removeInvWire(ASMInst from) {
        if (invWires.get(from) == null)
            return;
        invWires.remove(from);
        if (invWires.isEmpty()) { // This instance ins no longer used. Delete it
                                  // => remove all its wires
            for (ASMInst dest : wires.keySet()) {
                removeWire(dest, null);
            }
            try {
                ASMInstImpl.myBroker.removeInst(this);
                samInst.delete();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * remove from ASM It deletes the wires, which deletes the isolated used instances, and transitively. It deleted the
     * invWires, which removes the associated real dependency :
     */
    @Override
    public void remove() {
        // The fact the instance is no longer used deletes it. It is done in
        // removeWire.
        for (ASMInst client : invWires.keySet()) {
            ((ASMInstImpl) client).removeWire(this, null);
        }
    }

    @Override
    public void substWire(ASMInst oldTo, ASMInst newTo, String depName) {
        if ((oldTo == null) || (newTo == null))
            return;
        new Wire(this, newTo, depName, wires.get(oldTo).getConstraints());
        removeWire(oldTo, newTo);
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
    public Set<ASMInst> getClients() {
        return invWires.keySet();
    }

    @Override
    public Wire getWire(ASMInst destInst) {
        if (destInst == null)
            return null;
        return wires.get(destInst);
    }
}
