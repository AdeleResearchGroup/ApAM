/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

public class InstanceImpl extends ComponentImpl implements Instance {

    /**
     * This class represents the Apam root instance.
     * 
     * This is an APAM concept without mapping at the execution platform level,
     * we build an special apform object to represent it.
     * 
     */
    private static class SystemRootInstance extends
	    BaseApformComponent<Instance, InstanceDeclaration> implements
	    ApformInstance {

	public SystemRootInstance(Implementation rootImplementation, String name) {
	    super(new InstanceDeclaration(rootImplementation
		    .getImplDeclaration().getReference(), name, null));
	}

	@Override
	public Object getServiceObject() {
	    throw new UnsupportedOperationException(
		    "method not available in root instance");
	}

	@Override
	public boolean remLink(Component destInst, String depName) {
	    throw new UnsupportedOperationException(
		    "method not available in root instance");
	}

	@Override
	public void setApamComponent(Component apamComponent) {
	    throw new UnsupportedOperationException(
		    "method not available in root instance");
	}

	@Override
	public boolean setLink(Component destInst, String depName) {
	    throw new UnsupportedOperationException(
		    "method not available in root instance");
	}

	@Override
	public void setProperty(String attr, String value) {
	    throw new UnsupportedOperationException(
		    "method not available in root instance");
	}

    }

    private static Logger logger = LoggerFactory.getLogger(InstanceImpl.class);

    private static final long serialVersionUID = 1L;
    private Implementation myImpl;

    private Composite myComposite;

    /**
     * Builds a new Apam instance to represent the specified platform instance
     * in the Apam model.
     */
    protected InstanceImpl(Composite composite, ApformInstance apformInst)
	    throws InvalidConfiguration {

	super(apformInst);

	if (composite == null) {
	    throw new InvalidConfiguration(
		    "Null parent while creating instance");
	}

	Implementation implementation = CST.componentBroker.getImpl(apformInst
		.getDeclaration().getImplementation().getName());

	if (implementation == null) {
	    throw new InvalidConfiguration(
		    "Null implementation while creating instance");
	}

	/*
	 * reference the implementation and the enclosing composite
	 */
	myImpl = implementation;
	myComposite = composite;

    }

    /**
     * This is an special constructor only used for the root instance of the
     * system
     */
    protected InstanceImpl(Implementation rootImplementation, String name)
	    throws InvalidConfiguration {
	super(new SystemRootInstance(rootImplementation, name));

	myImpl = rootImplementation;
	// WARNING: this is a trick to have a dummy root instance, both an
	// instance and its composite.
	myComposite = (Composite) this;

	/*
	 * NOTE the root instance is automatically registered in Apam in a
	 * specific way that allows bootstraping the system
	 */
	if (rootImplementation == CompositeTypeImpl.getRootCompositeType()) {
	    ((ImplementationImpl) getImpl()).addInst(this);
	}
    }

    @Override
    public final ApformInstance getApformInst() {
	return (ApformInstance) getApformComponent();
    }

    @Override
    public Composite getAppliComposite() {
	return myComposite.getAppliComposite();
    }

    @Override
    public final Composite getComposite() {
	return myComposite;
    }

    @Override
    public Component getGroup() {
	return myImpl;
    }

    @Override
    public Implementation getImpl() {
	return myImpl;
    }

    @Override
    public ComponentKind getKind() {
	return ComponentKind.INSTANCE;
    }

    @Override
    public Set<Component> getMembers() {
	return Collections.emptySet();
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
	// return (!hasInvWires() || isShared());
	return (invlinks.isEmpty() || isShared());
    }

    @Override
    public final boolean isUsed() {
	return !((CompositeImpl) getComposite()).isSystemRoot();
    }

    @Override
    public void register(Map<String, String> initialproperties)
	    throws InvalidConfiguration {

	/*
	 * Opposite references from implementation and enclosing composite
	 */
	((ImplementationImpl) getImpl()).addInst(this);
	((CompositeImpl) getComposite()).addContainInst(this);

	/*
	 * Terminates the initialization, and computes properties
	 */
	finishInitialize(initialproperties);

	/*
	 * Add to broker
	 */
	((ComponentBrokerImpl) CST.componentBroker).add(this);

	/*
	 * Bind to the underlying execution platform instance
	 */
	getApformInst().setApamComponent(this);

	/*
	 * Notify managers
	 */
	ApamManagers.notifyAddedInApam(this);
    }

    /**
     * Change the owner (enclosing composite) of this instance.
     * 
     */
    public void setOwner(Composite owner) {

	CompositeImpl oldOwner = (CompositeImpl) getComposite();
	if (owner == oldOwner) {
	    return;
	}

	oldOwner.removeInst(this);
	this.myComposite = owner;
	((CompositeImpl) owner).addContainInst(this);

	/*
	 * Force recalculation of dependencies that may have been invalidated by
	 * the ownership change
	 */
	for (Link incoming : this.getInvLinks()) {
	    if (!incoming.isPromotion() && !incoming.getSource().canSee(this)) {
		incoming.remove();
	    }
	}

	/*
	 * Remove outgoing wires (definitions or visibilities may have changed)
	 */
	for (Link outgoing : this.getLocalLinks()) {
	    if (!this.canSee(outgoing.getDestination())) {
		outgoing.remove();
	    }
	}

	// recalculer les declarations d'attribut et de relships contextuelles
	// ((ComponentImpl)this).finishInitialize(getAllPropertiesString()) ;

    }

    /**
     * remove from ASM It deletes the wires, which deletes the isolated used
     * instances, and transitively. It deleted the invWires, which removes the
     * associated real relation :
     */
    @Override
    public void unregister() {
	logger.debug("unregister instance " + this);

	/*
	 * Remove from broker, and from its composites. After that, it is
	 * invisible.
	 */
	((ImplementationImpl) getImpl()).removeInst(this);
	((CompositeImpl) getComposite()).removeInst(this);

	/*
	 * Remove all incoming and outgoing wires (this deletes the associated
	 * references at the execution platform level)
	 */
	for (Link wire : invlinks) {
	    ((LinkImpl) wire).remove();
	}

	/*
	 * Unbind from the underlying execution platform instance
	 */
	getApformInst().setApamComponent(null);

	/*
	 * Do no remove the outgoing wires, in case a Thread is still here. If
	 * so, the relation will be resolved again ! Should only remove the
	 * invWire ! But weird: wired only in a direction ...
	 */

	for (Link wire : links) {
	    ((LinkImpl) wire).remove();
	}

	// /*
	// * Notify managers
	// */
	// ApamManagers.notifyRemovedFromApam(this);
	//
	//

    }

    // /**
    // * returns the wires leading to that instance
    // *
    // */
    // public Set<Link> getInvWires() {
    // Set<Link> wires = new HashSet<Link> () ;
    // for (Link link : invlinks) {
    // if (link.isWire())
    // wires.add(link) ;
    // }
    // return wires ;
    // }
    //
    // // returns the wires leading to that instance
    // public Set<Link> getWires() {
    // Set<Link> wires = new HashSet<Link> () ;
    // for (Link link : links) {
    // if (link.isWire())
    // wires.add(link) ;
    // }
    // return wires ;
    // }
    //
    //
    // /**
    // * returns the wires from that instance
    // *
    // */
    // public boolean hasWires() {
    // for (Link link : links) {
    // if (link.isWire())
    // return true ;
    // }
    // return false ;
    // }
    //
    // public boolean hasInvWires() {
    // for (Link link : invlinks) {
    // if (link.isWire())
    // return true ;
    // }
    // return false ;
    // }

}