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
package fr.imag.adele.dynamic.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;
import fr.imag.adele.apam.impl.InstanceImpl;


/**
 * This class is responsible for the content management of a composite.
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
	 * The list of contained instances that must be dynamically created when the
	 * specified triggering condition is satisfied
	 */
	private final List<FutureInstance> futureInstances;

	/**
	 * The list of dynamic dependencies that must be updated without waiting for
	 * lazy resolution
	 */
	private final List<PendingRequest> dynamicDependencies;
	
	/**
	 * The list of waiting resolutions in this composite
	 */
	private final List<PendingRequest> waitingResolutions;

	
	/**
	 * The current state of the content manager
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
	 * The list of pending resolutions waiting for a grant. This is a subset
	 * of the waiting resolutions indexed by the associated grant.
	 */
	private final Map<GrantDeclaration, List<PendingRequest>> pendingGrants;
	
	
	
	/**
	 * Initializes the content manager
	 */
	public ContentManager(DynamicManagerImplementation manager, Composite composite) {
		
		this.composite		= composite;
		this.declaration	= composite.getCompType().getCompoDeclaration();
		
		/*
		 * Initialize state information
		 */
		
		this.stateHolder		= null;
		this.stateProperty		= null;
		this.state				= null;
		

		/*
		 * Initialize the list of dynamic dependencies
		 */
		dynamicDependencies	= new ArrayList<PendingRequest>();

		/*
		 * Initialize the list of waiting resolutions
		 */
		waitingResolutions	= new ArrayList<PendingRequest>();
		
		/*
		 * Initialize the list of dynamic contains
		 */
		futureInstances	= new ArrayList<FutureInstance>();
		
		/*
		 * Initialize ownership information
		 */
		owned 			= new HashMap<OwnedComponentDeclaration, Set<Instance>>();
		granted			= new HashMap<OwnedComponentDeclaration, GrantDeclaration>();
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			owned.put(ownedDeclaration, new HashSet<Instance>());
		
		}
		
		pendingGrants	= new HashMap<GrantDeclaration, List<PendingRequest>>();
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				pendingGrants.put(grant, new ArrayList<PendingRequest>());
			}
		}
		
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
		 * Initialize the list of dynamic contains
		 */
		for (InstanceDeclaration instanceDeclaration : declaration.getInstanceDeclarations()) {
			futureInstances.add(new FutureInstance(this.composite,instanceDeclaration));
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
		
		/*
		 * Trigger an initial update of dynamic containment 
		 */
		updateContainementTriggers();

	}
	
	/**
	 * The composite is removed
	 */
	public synchronized void dispose() {
		
		stateHolder	= null;
		state 		= null;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			owned.get(ownedDeclaration).clear();
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				pendingGrants.get(grant).clear();
			}
		}
		
		futureInstances.clear();
		dynamicDependencies.clear();
		
		waitingResolutions.clear();
	}
	
	/**
	 * The composite managed by this content manager
	 */
	public Composite getComposite() {
		return composite;
	}

	/**
	 * Accord ownership of the specified instance to this composite 
	 */
	public void grantOwnership(Instance instance) {
		
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
	 * Updates the contents of this composite when an instance changes ownership.
	 * 
	 * If the instance is contained in this composite, verify containment triggers
	 */
	public void ownershipChanged(Instance instance) {
		
		if (instance.getComposite().equals(getComposite())) {
			updateContainementTriggers();
		}
	}
	

	/**
	 * Add a new pending request in the content of the composite
	 */
	public void addPendingRequest(PendingRequest request) {
		
		/*
		 * add to the list of pending requests
		 */
		synchronized (waitingResolutions) {
			waitingResolutions.add(request);
		}
		
		/*
		 * If the target of the request is an instance, verify if the request corresponds
		 * to a grant for an owned instance
		 */
		if (! request.getRelation().getTargetKind().equals(ComponentKind.INSTANCE))
			return;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {

			GrantDeclaration currentGrant	= getCurrentGrant(ownedDeclaration);
			boolean reschedule 				= false;
			/*
			 * add request to list of matched pending grants 
			 * 
			 */
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				
				if (match(grant,request.getRelation()) && match(grant,request.getSource())) {
					synchronized (pendingGrants) {
						pendingGrants.get(grant).add(request);
					}
					
					reschedule = reschedule || (currentGrant != null && currentGrant.equals(grant));
				}
				
			}
			
			/*
			 * force rescheduling of owned instances to give a chance to the new request
			 */
			if (reschedule && currentGrant != null)
				setCurrentGrant(ownedDeclaration, currentGrant);
		}
	}
	
	/**
	 * Remove a pending request from the content of the composite
	 */
	public void removePendingRequest(PendingRequest request) {

		synchronized (waitingResolutions) {
			waitingResolutions.remove(request);
		}
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant :ownedDeclaration.getGrants()) {
				synchronized (pendingGrants) {
					pendingGrants.get(grant).remove(request);
				}
			}
		}
		
	}
	
	/**
	 * Updates the contents of this composite when a new component is added in APAM
	 */
	public void addedComponent(Component component) {
		
		/*
		 * Try to satisfy pending requests
		 */
		resolveRequests(getWaitingRequests(),component);
		resolveRequests(getDynamicRequests(),component);

		/*
		 * If a new instance is created in this composite, verify if it triggers
		 * a containment
		 */
		if ( (component instanceof Instance) && ((Instance)component).getComposite().equals(getComposite()) )
			updateContainementTriggers();
		
	}
	
	/**
	 * Updates the list of dynamic dependencies when a new component is managed by this composite
	 */
	public void updateDynamicDependencies(Component component) {
		
		for (Relation relation : component.getRelations()) {

			if (component.getKind().equals(relation.getSourceKind()) && relation.isDynamic()) {
				
				PendingRequest request = new PendingRequest((ApamResolverImpl)CST.apamResolver,component,relation);
				addDynamicRequest(request);
				
				if (relation.getCreation() == CreationPolicy.EAGER)
					request.resolve();
			}			
		}
	}
	
	/**
	 * Updates the contents of this composite when a component is removed from APAM
	 * 
	 */
	public void removedComponent(Component component) {
		
		/*
		 * Remove from the list of pending requests all requests originating from the
		 * removed component
		 */
		for (PendingRequest request : getWaitingRequests()) {
			if (request.getSource().equals(component))
				removePendingRequest(request);
		}
		
		for (PendingRequest request : getDynamicRequests()) {
			if (request.getSource().equals(component))
				removeDynamicRequest(request);
		}
		
		/*
		 * For instances update ownership information
		 */
		if ( !(component instanceof Instance))
			return;
		
		
		Instance instance = (Instance) component;
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
	 * Updates the contents of this composite when a wire is added to APAM.
	 */ 
	public void addedLink(Link link) {
	}
	
	/**
	 * Updates the contents of this composite when a wire is removed from APAM.
	 * 
	 * If the target of the wire is a non sharable instance, the released instance can
	 * potentially be used by a pending requests.
	 * 
	 */
	public void removedLink(Link link) {
		
		if (link.getDestination() instanceof Instance) {
			Instance instance = (Instance)link.getDestination();
			
			if (instance.isSharable())
				resolveRequests(getWaitingRequests(),instance);

		}
		
	}
	
	/**
	 * Verifies all the effects of a property change in this composite
	 * 
	 */
	public void propertyChanged(Component component, String property) {

		resolveRequests(getWaitingRequests(),component);
		resolveRequests(getDynamicRequests(),component);

		/*
		 * Verify if a contained instance has changed the state
		 */
		
		if (! (component instanceof Instance))
			return;
		
		Instance instance = (Instance) component;
		
		if ( ! instance.getComposite().equals(getComposite()) )
			return;
		
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
		
		updateContainementTriggers();
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
			 * Revoke previous grant
			 */
			synchronized (granted) {
				
				/*
				 * If the current grant is still valid there is nothing to do
				 */
				GrantDeclaration previuos = granted.get(ownedDeclaration);
				if (previuos != null && previuos.getStates().contains(this.state))
					continue;
				
				granted.remove(ownedDeclaration);

			}
			

			/*
			 * Set new grant according to new state
			 */
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				if (grant.getStates().contains(this.state))
					setCurrentGrant(ownedDeclaration,grant);
			}
			
			/*
			 * If there is no active grant, we remove all incoming links to owned
			 * instances. The owned instances will be then be bound on first-come
			 * first-served base 
			 */
			GrantDeclaration currentGrant = getCurrentGrant(ownedDeclaration);
			if ( currentGrant == null && !ownedDeclaration.getGrants().isEmpty()) {
				for (Instance ownedInstance : getOwned(ownedDeclaration)) {
					for (Link incoming : ownedInstance.getInvLinks()) {
						incoming.remove();
					}
				}
			}
			
		}
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
	 * Whether this composite requests ownership of the specified instance
	 */
	public boolean requestOwnership(Instance instance) {
		
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
	 * Get a thread safe (stack contained) copy of the current list of instances owned
	 * for the specified ownership declaration
	 */
	private Collection<Instance> getOwned(OwnedComponentDeclaration ownedDeclaration) {
		synchronized (owned) {
			return new ArrayList<Instance>(owned.get(ownedDeclaration));
		}
	}
	
	/**
	 * Whether the specified instance is owned by this composite
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
	 * Preempt access to the owned instances from their current clients, and give it to the
	 * requests satisfying the new grant
	 */
	private void setCurrentGrant(OwnedComponentDeclaration ownedDeclaration, GrantDeclaration newGrant) {
		
		/*
		 * change current grant
		 */
		synchronized (granted) {
			granted.put(ownedDeclaration, newGrant);
		}

		/*
		 * preempt all owned instances
		 */
		for (Instance ownedInstance : getOwned(ownedDeclaration)) {
			preempt(ownedDeclaration,ownedInstance);
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
	 * Get a thread safe (stack contained) copy of the current list of pending
	 * requests of a given grant
	 */
	private List<PendingRequest> getPendingRequest(GrantDeclaration grant) {
		synchronized (pendingGrants) {
			return new ArrayList<PendingRequest>(pendingGrants.get(grant));
		}
	}

	/**
	 * verifies if a component matches the specified grant source
	 */
	private static boolean match(GrantDeclaration grant, Component candidate) {
		ComponentReference<?> grantSource = grant.getRelation().getDeclaringComponent();
		while (candidate != null) {
			
			if (candidate.getDeclaration().getReference().equals(grantSource))
				return true;
			
			candidate = candidate.getGroup();
		}
		
		return false;
	}
	
	/**
	 * verifies if a relation matches the specified grant
	 */
	private static boolean match(GrantDeclaration grant, Relation relation) {
		return relation.getName().equals(grant.getRelation().getIdentifier());
	}

	/**
	 * verifies if a link matches the specified grant
	 */
	private static boolean match(GrantDeclaration grant, Link link) {
		return link.getName().equals(grant.getRelation().getIdentifier()) && match(grant,link.getSource());
	}
	
	
	/**
	 * Preempt access to the specified instance from their current clients and give it
	 * to the request currently granted
	 * 
	 * TODO IMPORTANT BUG If the old clients are concurrently resolved again (after the
	 * revoke but before the new resolve) they can get access to the owned instance. 
	 * 
	 * We should find a way to make this method atomic, this requires synchronizing this
	 * content manager and the Apam resolver.
	 */
	private void preempt(OwnedComponentDeclaration ownedDeclaration, Instance ownedInstance) {

		assert this.owns(ownedInstance);
		
		/*
		 * If there is no active grant in the current state, nothing to do
		 */
		GrantDeclaration grant = getCurrentGrant(ownedDeclaration);
		if (grant == null)
			return;

		/*
		 * revoke all non granted wires
		 */
		for (Link incoming : ownedInstance.getInvLinks()) {
			if ( !match(grant,incoming))
				incoming.remove();
		}

		/*
		 * try to resolve pending requests of this grant
		 */
		List<PendingRequest> grantedRequests = getPendingRequest(grant);
		for (PendingRequest request : grantedRequests) {
			if (request.isSatisfiedBy(ownedInstance))
				request.resolve();
		}

	}
	
	/**
	 * Get a thread-safe (stack confined) copy of the waiting requests
	 */
	private List<PendingRequest> getWaitingRequests() {
		synchronized (waitingResolutions) {
			return new ArrayList<PendingRequest>(waitingResolutions);
		}
	}
	
	/**
	 * Get a thread-safe (stack confined) copy of the dynamic requests
	 */
	private List<PendingRequest> getDynamicRequests() {
		synchronized (dynamicDependencies) {
			return new ArrayList<PendingRequest>(dynamicDependencies);
		}
	}
	
	

	/**
	 * Add a new dynamic request in the content of the composite
	 */
	private void addDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			dynamicDependencies.add(request);
		}
	}
	
	/**
	 * Remove a dynamic request from the content of the composite
	 */
	public void removeDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			dynamicDependencies.remove(request);
		}
	}
	
	
	/**
	 * Try to resolve all the requests that are potentially satisfied by a given component
	 */
	private static void resolveRequests(List<PendingRequest> requests, Component candidate) {
		for (PendingRequest request : requests) {
			if (request.isSatisfiedBy(candidate))
				request.resolve();
		}
	}
	

	/**
	 * Verifies if the triggering conditions of pending dynamic contained instances are satisfied
	 */
	private void updateContainementTriggers() {
		
		/*
		 * Iterate over all pending dynamic instances 
		 * 
		 * IMPORTANT Notice that this iteration is synchronized so we do not have concurrent accesses over
		 * the list of future instances. However, we must carefully handle the case of nested invocations
		 * of this method (because instantiation of a dynamic instance may trigger other pending instances)
		 */
		
		synchronized (futureInstances) {
			
			List<FutureInstance> processed = new ArrayList<FutureInstance>(); 
			while(! futureInstances.isEmpty()) {
		
				/*
				 * Take the first pending instance from the list, notice that we remove it
				 * so that we are sure that there is only a single invocation of this method
				 * handling a given pending instance 
				 */
				FutureInstance futureInstance = futureInstances.remove(0);
				
				/*
				 * Evaluate triggering conditions and instantiate if satisfied
				 * 
				 */
				futureInstance.checkInstatiation();
				processed.add(futureInstance);
			}

			/*
			 * Put back in the list all the processed requests that were not triggered
			 */
			while (! processed.isEmpty()) {
				FutureInstance processedInstance = processed.remove(0);
				if (! processedInstance.isInstantiated())
					futureInstances.add(processedInstance);
			}
		}
		
	}
	



}
