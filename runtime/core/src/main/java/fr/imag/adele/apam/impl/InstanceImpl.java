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
import fr.imag.adele.apam.declarations.references.components.VersionedReference;

public class InstanceImpl extends ComponentImpl implements Instance {

	/**
	 * This class represents the Apam root instance.
	 * 
	 * This is an APAM concept without mapping at the execution platform level, we build an special
	 * apform object to represent it.
	 * 
	 */
	private static class SystemRootInstance extends BaseApformComponent<Instance, InstanceDeclaration> implements ApformInstance {

		public SystemRootInstance(Implementation rootImplementation, String name) {
			super(new InstanceDeclaration(VersionedReference.any(rootImplementation.getImplDeclaration().getReference()),name));
		}

		@Override
		public Object getServiceObject() {
			throw new UnsupportedOperationException("method not available in root instance");
		}

		@Override
		public boolean remLink(Component destInst, String depName) {
			throw new UnsupportedOperationException("method not available in root instance");
		}

		@Override
		public void setApamComponent(Component apamComponent) {
			throw new UnsupportedOperationException("method not available in root instance");
		}

		@Override
		public boolean setLink(Component destInst, String depName) {
			throw new UnsupportedOperationException("method not available in root instance");
		}

		@Override
		public void setProperty(String attr, String value) {
			throw new UnsupportedOperationException("method not available in root instance");
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
	protected InstanceImpl(Composite composite, ApformInstance apformInst) throws InvalidConfiguration {

		super(apformInst);

		if (composite == null) {
			throw new InvalidConfiguration("Null parent while creating instance");
		}

		Implementation implementation = CST.componentBroker.getImpl(apformInst.getDeclaration().getImplementation().getName());

		if (implementation == null) {
			throw new InvalidConfiguration("Null implementation while creating instance");
		}

		/*
		 * reference the implementation and the enclosing composite
		 */
		myImpl = implementation;
		myComposite = composite;

	}

	/**
	 * This is an special constructor only used for the root instance of the system
	 */
	protected InstanceImpl(Implementation rootImplementation, String name) throws InvalidConfiguration {
		super(new SystemRootInstance(rootImplementation, name));

		/*
		 * NOTE the root instance is automatically registered in the Apam model in a ad-hoc way that allows
		 * bootstraping the system
		 */
		myImpl 		= rootImplementation;
		((ImplementationImpl) rootImplementation).addInst(this);

		put(CST.NAME,name);
		
		/*
		 * The top level of the hierarchy is closed by a self-loop
		 */
		myComposite = (Composite) this;

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
	public void register(Map<String, String> initialproperties) throws InvalidConfiguration {

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
		 * Bind to the underlying execution platform instance
		 */
		getApformInst().setApamComponent(this);

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
	 * Change the owner (enclosing composite) of this instance.
	 * 
	 */
	public void setOwner(Composite owner) {

		CompositeImpl previousOwner = (CompositeImpl) getComposite();
		if (owner == previousOwner) {
			return;
		}

		previousOwner.removeInst(this);
		this.myComposite = owner;
		((CompositeImpl) owner).addContainInst(this);

		/*
		 *  Compute again the properties and relation definition, 
		 *  and removes the properties and links now invalid
		 */
		((ComponentImpl)this).finishInitialize(getAllPropertiesString()) ;

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
		try {
			getApformInst().setApamComponent(null);
		} catch (InvalidConfiguration ignored) {
			logger.error("error unregistering instance "+ this, ignored.getCause());
		}

		/*
		 * Do no remove the outgoing wires, in case a Thread is still here. If
		 * so, the relation will be resolved again ! Should only remove the
		 * invWire ! But weird: wired only in a direction ...
		 */

		for (Link wire : links) {
			((LinkImpl) wire).remove();
		}

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