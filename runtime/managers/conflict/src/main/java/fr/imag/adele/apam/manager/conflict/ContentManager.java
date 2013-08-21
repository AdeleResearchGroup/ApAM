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
package fr.imag.adele.apam.manager.conflict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;
import fr.imag.adele.apam.impl.FailedResolutionManager;
import fr.imag.adele.apam.impl.InstanceImpl;
import fr.imag.adele.apam.impl.PendingRequest;


/**
 * This class is responsible for solving conflicts over owned instances of a composite
 * 
 * @author vega
 *
 */
public class ContentManager  {

	@SuppressWarnings("unused")
	private final static Logger	logger = LoggerFactory.getLogger(ContentManager.class);

	/**
	 * The declaration of the composite type
	 * 
	 * NOTE currently declarations are read-only, so we can safely navigate this information
	 * without any thread synchronization
	 */
	private final CompositeDeclaration declaration;
	
	/**
	 * The managed composite 
	 */
	private final Composite composite;
	
	/**
	 * The instances that are owned by this content manager. This is a subset of the 
	 * contained instances of the composite.
	 */
	private final Map<OwnedComponentDeclaration, Set<Instance>> owned;

	/**
	 * The current state of the composite
	 */
	private String state;

	/**
	 * The instance that holds the state of the content manager. This instance is
	 * automatically created inside the composite at start of content manager.
	 */
	private Instance	stateHolder;

	/**
	 * The property used for holding the state
	 */
	private String 		stateProperty;
	
	/**
	 * The active grant in the current state 
	 */
	private final Map<OwnedComponentDeclaration, GrantDeclaration> granted;

	
	/**
	 * Initializes the content manager
	 */
	public ContentManager(ConflictManager manager, Composite composite) {
		
		this.composite		= composite;
		this.declaration	= composite.getCompType().getCompoDeclaration();
		
		/*
		 * Initialize state information
		 */
		
		this.stateHolder		= null;
		this.stateProperty		= null;
		this.state				= null;
		
		/*
		 * Initialize ownership information
		 */
		owned 			= new HashMap<OwnedComponentDeclaration, Set<Instance>>();
		granted			= new HashMap<OwnedComponentDeclaration, GrantDeclaration>();
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			owned.put(ownedDeclaration, new HashSet<Instance>());
		
		}
		
	}
	
	/**
	 * The composite managed by this content manager
	 */
	public Composite getComposite() {
		return composite;
	}
	
	
	/**
	 * Whether the specified instance is currently owned by this composite
	 */
	public boolean owns(Instance instance) {
	
		if (!instance.getComposite().equals(getComposite()))
			return false;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			if (getOwned(ownedDeclaration).contains(instance))
				return true;
		}
		
		return false;
		
	}	

	/**
	 * The declaration of the owned instances
	 */
	public Set<OwnedComponentDeclaration> getOwned() {
		return declaration.getOwnedComponents();
	}

	/**
	 * Get a thread safe (stack contained) copy of the current list of instances owned
	 * for the specified ownership declaration
	 */
	public Collection<Instance> getOwned(OwnedComponentDeclaration ownedDeclaration) {
		synchronized (owned) {
			return new ArrayList<Instance>(owned.get(ownedDeclaration));
		}
	}


	/**
	 * Whether this composite requests ownership of the specified instance
	 */
	public boolean shouldOwn(Instance instance) {
		
		/*
		 * Iterate over all ownership declarations and verify if the instance matches the specified
		 * condition
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {


			boolean matchType = false;	
			
			if (ownedDeclaration.getComponent() instanceof SpecificationReference)
				matchType = instance.getSpec().getDeclaration().getReference().equals(ownedDeclaration.getComponent());
			
			if (ownedDeclaration.getComponent() instanceof ImplementationReference<?>)
				matchType = instance.getImpl().getDeclaration().getReference().equals(ownedDeclaration.getComponent());
			
			String property			= ownedDeclaration.getProperty() != null ? ownedDeclaration.getProperty().getIdentifier() : null;
			String propertyValue 	= property != null ? instance.getProperty(property) : null;
			boolean matchProperty	= property == null || (propertyValue != null && ownedDeclaration.getValues().contains(propertyValue));
			
			if (matchType && matchProperty)
				return true;
		}
		
		return false;
	}

	/**
	 * Accord ownership of the specified instance to this composite 
	 */
	public void accordOwnership(Instance instance) {
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {

			/*
			 * find matching declaration
			 */
			boolean matchType = false;	
			
			if (ownedDeclaration.getComponent() instanceof SpecificationReference)
				matchType = instance.getSpec().getDeclaration().getReference().equals(ownedDeclaration.getComponent());
			
			if (ownedDeclaration.getComponent() instanceof ImplementationReference<?>)
				matchType = instance.getImpl().getDeclaration().getReference().equals(ownedDeclaration.getComponent());
			
			if (!matchType)
				continue;
			
			String property			= ownedDeclaration.getProperty() != null ? ownedDeclaration.getProperty().getIdentifier() : null;
			String propertyValue 	= property != null ? instance.getProperty(property) : null;
			boolean matchProperty	= property == null || (propertyValue != null && ownedDeclaration.getValues().contains(propertyValue));
			
			if (!matchProperty)
				continue;

			/*
			 * get ownership
			 */
			((InstanceImpl)instance).setOwner(getComposite());
			
			/* 
			 * register owned instance
			 */
			synchronized (owned) {
				owned.get(ownedDeclaration).add(instance);
			}
			
			/*
			 * preempt previous users of the instance and give access to currently granted
			 * waiting requests
			 */
			preempt(ownedDeclaration,instance);
			
		}			
	}
	
	/**
	 * Revokes ownership of the specified instance
	 * 
	 */
	public void revokeOwnership(Instance instance) {
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			synchronized (owned) {
				owned.get(ownedDeclaration).remove(instance);
			}
		}

		((InstanceImpl) instance).setOwner(CompositeImpl.getRootAllComposites());
	}

	/**
	 * Updates the contents of this composite when a contained instance is removed from APAM
	 * 
	 */
	public void removedInstance(Instance instance) {
		
		assert instance.getComposite().equals(getComposite());

		/*
		 * update state
		 */
		synchronized (this) {
			if (instance == stateHolder)
				stateHolder = null;
		}
		
		/*
		 * update list of owned instances
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			synchronized (owned) {
				owned.get(ownedDeclaration).remove(instance);
			}
		}
	}

	/**
	 * Verifies all the effects of a property change in the contained instances
	 * 
	 */
	public void propertyChanged(Instance instance, String property) {

		assert instance.getComposite().equals(getComposite());
		
		boolean stateChanged 	= false;
		String newState			= null;
		
		synchronized (this) {
			if (stateHolder != null && stateHolder.equals(instance) && stateProperty.equals(property)) {
				stateChanged = true;
				newState	 = stateHolder.getProperty(stateProperty);
			}
		}
		
		if (stateChanged)
			stateChanged(newState);
		
	}
	
	/**
	 * Handle state changes in the composite
	 */
	private  void stateChanged(String newState) {
		
		/*
		 * Change state
		 */
		synchronized (this) {
			this.state	= newState;
		}
		
		/*
		 * Reevaluate grants
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			
			/*
			 * If the current grant is still valid there is nothing to do
			 */
			synchronized (granted) {
				GrantDeclaration previuos = granted.get(ownedDeclaration);
				if (previuos != null && previuos.getStates().contains(newState))
					continue;
			}
			

			/*
			 * Set new grant, according to new state
			 */
			
			GrantDeclaration newGrant = null;
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				if (grant.getStates().contains(newState)) {
					newGrant = grant;
				}
			}
			
			setCurrentGrant(ownedDeclaration,newGrant);
		}
	}


	/**
	 * Get the current grant for the specified ownership declaration
	 */
	private GrantDeclaration getCurrentGrant(OwnedComponentDeclaration ownedDeclaration) {
		synchronized (granted) {
			return granted.get(ownedDeclaration);
		}
	}
	
	/**
	 * Preempt access to the owned instances from their current clients, and give it to the
	 * requests satisfying the new grant
	 */
	private void setCurrentGrant(OwnedComponentDeclaration ownedDeclaration, GrantDeclaration newGrant) {
		
		/*
		 * change current grant
		 */
		synchronized (granted) {
			if (newGrant != null)
				granted.put(ownedDeclaration, newGrant);
			else
				granted.remove(ownedDeclaration);
		}

		/*
		 * preempt current users of all owned instances
		 */
		for (Instance ownedInstance : getOwned(ownedDeclaration)) {
			preempt(ownedDeclaration,ownedInstance);
			
			/*
			 * Wake pending request that could be satisfied by the new grant
			 */
			FailedResolutionManager failureManager = (FailedResolutionManager) ApamManagers.getManager("FailedResolutionManager");
			for (PendingRequest request : failureManager.getWaitingResolutions()) {
				if (request.isSatisfiedBy(ownedInstance))
					if (newGrant == null || match(newGrant,request))
						request.resolve();
			}

		}
		
	}


	/**
	 * Preempt access to the specified instance from their current clients
	 * 
	 */
	private void preempt(OwnedComponentDeclaration ownedDeclaration, Instance ownedInstance) {

		assert this.owns(ownedInstance);
		
		GrantDeclaration grant = getCurrentGrant(ownedDeclaration);
		for (Link incoming : ownedInstance.getInvLinks()) {
			if ( grant == null || !match(grant,incoming))
				incoming.remove();
		}

	}

	/**
	 * verifies if the requested resolution is granted access to the specified owned instances
	 */
	public boolean isGranted(OwnedComponentDeclaration ownedDeclaration, Component source, Relation relation) {
		GrantDeclaration grant = getCurrentGrant(ownedDeclaration);
		return grant == null || match(grant,source,relation);
	}


	/**
	 * The list of potential ownership's conflict between this content manager and the one specified in the parameter
	 * 
	 */
	public Set<OwnedComponentDeclaration> getConflictingDeclarations(ContentManager that) {
		
		Set<OwnedComponentDeclaration> conflicts = new HashSet<OwnedComponentDeclaration>();
		
		for (OwnedComponentDeclaration thisDeclaration : declaration.getOwnedComponents()) {
			
			ComponentReference<?> thisSpecification = thisDeclaration.getComponent();
			String thisProperty						= thisDeclaration.getProperty() != null ? thisDeclaration.getProperty().getIdentifier() : null;
			Set<String> theseValues					= new HashSet<String>(thisDeclaration.getValues());
			
			/*
			 * Ownership declarations are conflicting if they refer to the same specification, with different
			 * properties or with the same values for the same property
			 */

			boolean hasConflict = false;
			for (OwnedComponentDeclaration	thatDeclaration : that.declaration.getOwnedComponents()) {
				
				ComponentReference<?> thatSpecification = thatDeclaration.getComponent();
				String thatProperty						= thatDeclaration.getProperty() != null ? thatDeclaration.getProperty().getIdentifier() : null;
				Set<String> thoseValues					= new HashSet<String>(thatDeclaration.getValues());
				
				if (!thisSpecification.equals(thatSpecification))
					continue;
				 
				if( thisProperty == null || thatProperty == null || !thisProperty.equals(thatProperty) ||  !Collections.disjoint(theseValues,thoseValues)) 
					hasConflict = true;
				
			}
			
			if (hasConflict)
				conflicts.add(thisDeclaration);
			
		}
		
		return conflicts;
	}
	
	
	
	
	/**
	 * Activates the content manager
	 */
	public synchronized void start() throws InvalidConfiguration  {
		
		/*
		 * Initialize state information
		 */
		
		this.stateHolder		= null;
		this.stateProperty		= null;
		this.state				= null;
		
		if (declaration.getStateProperty() != null) {
			
			PropertyDefinition.Reference propertyReference = declaration.getStateProperty();

			/*
			 * Get the component that defines the property, notice that this may deploy the implementation if not yet installed
			 */
			Component implementation = CST.apamResolver.findComponentByName(composite.getMainInst(),propertyReference.getDeclaringComponent().getName());
			
			/*
			 * In case the implementation providing the state is not available signal an error. 
			 * 
			 */
			if (implementation == null || ! (implementation instanceof Implementation)) {
				throw new InvalidConfiguration("Invalid state declaration, implementation can not be found "+propertyReference.getDeclaringComponent().getName());
			}

			/*
			 * Eagerly instantiate an instance to hold the state.
			 *
			 * In case the main instance can be used to hold state we avoid creating additional objects.
			 */
			if (composite.getMainInst() != null && composite.getMainInst().getImpl().equals(implementation)) {
				this.stateHolder 	= composite.getMainInst();
			}
			else
				this.stateHolder	= ((Implementation)implementation).createInstance(composite, null);
			
			/*
			 * Get the property used to handle the state
			 */
			PropertyDefinition propertyDefinition = implementation.getDeclaration().getPropertyDefinition(propertyReference);
			
			/*
			 * In case the property providing the state is not defined signal an error. 
			 * 
			 */
			if (propertyDefinition == null ) {
				throw new InvalidConfiguration("Invalid state declaration, property not defined "+propertyReference.getIdentifier());
			}
			
			this.stateProperty	= propertyDefinition.getName();
			
			/*
			 * compute the initial state of the composite
			 */
			this.state	= this.stateHolder.getProperty(this.stateProperty);

		}
		
		/*
		 * Initialize ownership information
		 */

		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			
			/*
			 * Compute the current grant based on the initial state
			 */
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				if (state != null && grant.getStates().contains(state))
					granted.put(ownedDeclaration, grant);
			}
		}
		
	}
	
	/**
	 * The composite is removed
	 */
	public synchronized void dispose() {
		
		stateHolder	= null;
		state 		= null;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			owned.get(ownedDeclaration).clear();
		}
		
	}

	
	/**
	 * verifies if a link matches the specified grant
	 */
	private static boolean match(GrantDeclaration grant, Link link) {
		return link.getName().equals(grant.getRelation().getIdentifier()) && match(grant,link.getSource());
	}

	/**
	 * verifies if a component matches the specified grant source
	 */
	private static boolean match(GrantDeclaration grant, Component source) {
		ComponentReference<?> grantSource = grant.getRelation().getDeclaringComponent();
		while (source != null) {
			
			if (source.getDeclaration().getReference().equals(grantSource))
				return true;
			
			source = source.getGroup();
		}
		
		return false;
	}
	
	/**
	 * verifies if the requested resolution matches the specified grant
	 */
	private static boolean match(GrantDeclaration grant, Component source, Relation relation) {
		return relation.getName().equals(grant.getRelation().getIdentifier()) && match(grant,source);
	}

	
	/**
	 * verifies if the requested resolution matches the specified grant
	 */
	private static boolean match(GrantDeclaration grant, PendingRequest request) {
		return match(grant,request.getSource(), request.getRelation());
	}


}
