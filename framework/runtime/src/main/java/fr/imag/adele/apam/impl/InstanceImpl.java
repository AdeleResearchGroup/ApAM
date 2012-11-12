package fr.imag.adele.apam.impl;


import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.core.InstanceDeclaration;

public class InstanceImpl extends ComponentImpl implements Instance {

    private static Logger     logger           = LoggerFactory.getLogger(InstanceImpl.class);

    private static final long serialVersionUID = 1L;

    private Implementation    myImpl;
    private Composite         myComposite;

    private final Set<Wire>   wires            = Collections.newSetFromMap(new ConcurrentHashMap<Wire, Boolean>());
    private final Set<Wire>   invWires         = Collections.newSetFromMap(new ConcurrentHashMap<Wire, Boolean>());

    /**
     * This class represents the Apam root instance.
     * 
     * This is an APAM concept without mapping at the execution platform level, we build an special
     * apform object to represent it.
     * 
     */
    private static class SystemRootInstance implements ApformInstance {

        private final InstanceDeclaration declaration;

        public SystemRootInstance(Implementation rootImplementation, String name) {
            declaration = new InstanceDeclaration(rootImplementation.getImplDeclaration().getReference(), name, null);
        }

        @Override
        public Bundle getBundle() {
        	return null;
        }
        
        @Override
        public InstanceDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public void setInst(Instance asmInstImpl) {
            throw new UnsupportedOperationException("method not available in root instance");
        }

        @Override
        public void setProperty(String attr, String value) {
            throw new UnsupportedOperationException("method not available in root instance");
        }

        @Override
        public Object getServiceObject() {
            throw new UnsupportedOperationException("method not available in root instance");
        }

        @Override
        public boolean setWire(Instance destInst, String depName) {
            throw new UnsupportedOperationException("method not available in root instance");
        }

        @Override
        public boolean remWire(Instance destInst, String depName) {
            throw new UnsupportedOperationException("method not available in root instance");
        }

        @Override
        public boolean substWire(Instance oldDestInst, Instance newDestInst,
                String depName) {
            throw new UnsupportedOperationException("method not available in root instance");
        }

		@Override
		public Instance getInst() {
	           throw new UnsupportedOperationException("method not available in root instance");
		}

    }

    /**
     * This is an special constructor only used for the root instance of the system
     */
    protected InstanceImpl(Implementation rootImplementation, String name) throws InvalidConfiguration {
        super(new SystemRootInstance(rootImplementation, name));

        myImpl = rootImplementation;
        myComposite = null;

        /*
         * NOTE the root instance is automatically registered in Apam in a specific way that
         * allows bootstraping the system
         * 
         */
        ((ImplementationImpl) getImpl()).addInst(this);

    }

    /**
     * Builds a new Apam instance to represent the specified platform instance in the Apam model.
     */
    protected InstanceImpl(Composite composite, ApformInstance apformInst) throws InvalidConfiguration {

        super(apformInst);

        if (composite == null)
            throw new InvalidConfiguration("Null parent while creating instance");

        Implementation implementation = CST.componentBroker.getImpl(apformInst.getDeclaration().getImplementation()
                .getName());

        if (implementation == null)
            throw new InvalidConfiguration("Null implementation while creating instance");

        assert composite != null && implementation != null;

        /*
         * reference the implementation and the enclosing composite
         */
        myImpl = implementation;
        myComposite = composite;

    }

    @Override
    public void register(Map<String, String> initialproperties) throws InvalidConfiguration {

        /*
         * Bind to the underlying execution platform instance
         */
        getApformInst().setInst(this);

        /*
         * Opposite references from implementation and enclosing composite
         */
        ((ImplementationImpl) getImpl()).addInst(this);
        ((CompositeImpl) getComposite()).addContainInst(this);

        /*
         * Terminates the initialization, and computes properties
         */
        initializeProperties(initialproperties);

        /*
         * Add to broker
         */
        ((ComponentBrokerImpl) CST.componentBroker).add(this);
        
        /*
         * Notify managers
         */
        ApamManagers.notifyAddedInApam(this);
    }

    /**
     * remove from ASM It deletes the wires, which deletes the isolated used instances, and transitively. It deleted the
     * invWires, which removes the associated real dependency :
     */
    @Override
    public void unregister() {
    	logger.debug("unregister instance " + this);
    	
        /*
         * Remove from broker, and from its composites.
         * After that, it is invisible.
         */
//        ((ComponentBrokerImpl) CST.componentBroker).remove(this);
        ((ImplementationImpl) getImpl()).removeInst(this);
        ((CompositeImpl) getComposite()).removeInst(this);

        /*
         * Remove all incoming and outgoing wires (this deletes the associated references at the execution platform level) 
         */
        for (Wire wire : invWires) {
            ((WireImpl) wire).remove();
        }
 
       

        /*
         * Unbind from the underlying execution platform instance
         */
        getApformInst().setInst(null);
        
        /*
         * Do no remove the outgoing wires, in case a Thread is still here.
         * If so, the dependency will be resolved again !
         * TODO Should only remove the invWire ! But weird: wired only in a direction ...
         */
//      myImpl = null;
//      myComposite = null;
        
      for (Wire wire : wires) {
          ((WireImpl) wire).remove();
      }

//        /*
//         * Notify managers
//         */
//        ApamManagers.notifyRemovedFromApam(this);
//
//

    }



    /**
     * Change the owner (enclosing composite) of this instance.
     * 
     * This is only allowed for unused instances and is the only dynamic
     * modification of the composite hierarchy allowed so far.
     * 
     * An owner instance must become unused again, before changing of
     * owner.
     * 
     */
    public void setOwner(Composite owner) {

		assert (isUsed() && owner == CompositeImpl.getRootAllComposites()) || ( !isUsed() && owner != null);

        ((CompositeImpl) getComposite()).removeInst(this);
        this.myComposite = owner;
        ((CompositeImpl) owner).addContainInst(this);
    }

    @Override
    public final boolean isUsed() {
        return !((CompositeImpl) getComposite()).isSystemRoot();
    }

    @Override
    public final ApformInstance getApformInst() {
        return (ApformInstance) getApformComponent();
    }

    @Override
    public final Composite getComposite() {
        return myComposite;
    }

    @Override
    public Composite getAppliComposite() {
        return myComposite.getAppliComposite();
    }

    @Override
    public Implementation getImpl() {
        return myImpl;
    }

    @Override
    public Object getServiceObject() {
        return getApformInst().getServiceObject();
    }

    @Override
    public Specification getSpec() {
        return myImpl.getSpec();
    }

    @Override
    public boolean isSharable() {
        return (getInvWires().isEmpty() || isShared());
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
    public Set<Wire> getWires(Specification spec) {
        if (spec == null)
            return null;
        Set<Wire> w = new HashSet<Wire>();
        for (Wire wire : wires) {
            if (wire.getDestination().getSpec() == spec)
                w.add(wire);
        }
        return w;
    }
    
    @Override
    public boolean createWire(Instance to, String depName, boolean hasConstraints) {
        if ((to == null) || (depName == null))
            return false;

        for (Wire wire : wires) { // check if it already exists
            if ((wire.getDestination() == to) && wire.getDepName().equals(depName))
                return true;
        }

        // creation
        if (getApformInst().setWire(to, depName)) {
            Wire wire = new WireImpl(this, to, depName, hasConstraints);
            wires.add(wire);
            ((InstanceImpl) to).invWires.add(wire);
        } else {
            logger.error("INTERNAL ERROR: wire from " + this + " to " + to
                    + " could not be created in the real instance.");
            return false;
        }

        /*
         *  if the instance was in the unused pull, move it to the from composite.
         *  
         */
        if (!to.isUsed()) {
            ((InstanceImpl) to).setOwner(getComposite());
        }

        // Other relationships to instantiate
        ((ImplementationImpl) getImpl()).addUses(to.getImpl());
        if ((SpecificationImpl) getSpec() != null)
            ((SpecificationImpl) getSpec()).addRequires(to.getSpec());
        
        return true;
    }

//    @Override
    public void removeWire(Wire wire) {
        if (getApformInst().remWire(wire.getDestination(), wire.getDepName())) {
            wires.remove(wire);
            ((ImplementationImpl) getImpl()).removeUses(wire.getDestination().getImpl());
        } else {
            logger.error("INTERNAL ERROR: wire from " + this + " to " + wire.getDestination()
                    + " could not be removed in the real instance.");
        }
    }

    public void removeInvWire(Wire wire) {
        invWires.remove(wire);
        if (invWires.isEmpty()) {
            /*
             * This instance is no longer used.
             * TODO should we set unused and change the owner  ?
             * 
             * setUsed(false);
             * setOwner(CompositeImpl.getRootAllComposites());
             * 
             * Currently, it will stay in the same composite. 
             * It may be the target of an "OWN" clause, and must not be changed. In case it will be re-used (local).
             */
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
	public Set<Component> getMembers() {
		return Collections.emptySet();
	}

    @Override
    public Component getGroup() {
        return myImpl;
    }

}
