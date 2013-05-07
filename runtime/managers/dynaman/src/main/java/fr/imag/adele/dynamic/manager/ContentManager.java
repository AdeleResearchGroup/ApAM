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
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;
import fr.imag.adele.apam.impl.InstanceImpl;
import fr.imag.adele.apam.util.Visible;


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
	private final List<FutureInstance> dynamicContains;

	/**
	 * The list of dynamic dependencies that must be updated without waiting for
	 * lazy resolution
	 */
	private final List<DynamicResolutionRequest> dynamicDependencies;
	
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
	public ContentManager(DynamicManagerImplementation manager, Composite composite) throws InvalidConfiguration {
		
		this.composite		= composite;
		this.declaration	= composite.getCompType().getCompoDeclaration();
		
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
			if (composite.getMainInst().getImpl().equals(implementation)) {
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
		 * Initialize the list of dynamic dependencies
		 */
		dynamicDependencies	= new ArrayList<DynamicResolutionRequest>();

		/*
		 * Initialize the list of dynamic contains
		 */
		dynamicContains	= new ArrayList<FutureInstance>();
		
		for (InstanceDeclaration instanceDeclaration : declaration.getInstanceDeclarations()) {
			dynamicContains.add(new FutureInstance(this.composite,instanceDeclaration));
		}
		
		/*
		 * Initialize ownership information
		 */
		owned 			= new HashMap<OwnedComponentDeclaration, Set<Instance>>();
		granted			= new HashMap<OwnedComponentDeclaration, GrantDeclaration>();
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			
			/*
			 * No instances are initially owned
			 */
			owned.put(ownedDeclaration, new HashSet<Instance>());
			
			/*
			 * Compute the current grant based on the initial state
			 */
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				if (state != null && grant.getStates().contains(state))
					granted.put(ownedDeclaration, grant);
			}
		}
		
		/*
		 * Initialize the list of waiting resolutions
		 */
		waitingResolutions	= new ArrayList<PendingRequest>();
		pendingGrants		= new HashMap<GrantDeclaration, List<PendingRequest>>();
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				pendingGrants.put(grant, new ArrayList<PendingRequest>());
			}
		}
		
		/*
		 * Trigger an initial update of dynamic containment and instances
		 */
		for (Instance contained : composite.getContainInsts()) {
			updateDynamicDependencies(contained);
		}
		
		updateContainementTriggers();
	}
	
	/**
	 * The composite managed by this content manager
	 */
	public Composite getComposite() {
		return composite;
	}

	/**
	 * Updates the list of dynamic dependencies when a new instance is added to the composite
	 */
	private void updateDynamicDependencies(Instance instance) {
		
		assert instance.getComposite().equals(getComposite());
		
		for (Relation relation : instance.getRelations()) {

			if (relation.isDynamic()) {
				DynamicResolutionRequest dynamicRequest = new DynamicResolutionRequest(CST.apamResolver,instance,relation);
				dynamicDependencies.add(dynamicRequest);

				/*
				 * Force initial resolution of eager relation
				 */
				if (relation.isEager())
					dynamicRequest.resolve();
			}			
		}
	}

	/**
	 * Verifies if the triggering conditions of pending dynamic contained instances are satisfied
	 */
	private synchronized void updateContainementTriggers() {
		
		/*
		 * Iterate over all pending dynamic instances 
		 * 
		 * IMPORTANT Notice that this method is synchronized so we do not have concurrent accesses over
		 * the list of pending instances. However, we must carefully handle the case of nested invocations
		 * of this method (because instantiation of a dynamic instance may trigger other pending instances)
		 */
		
		List<FutureInstance> processed = new ArrayList<FutureInstance>(); 
		
		while(! dynamicContains.isEmpty()) {

			/*
			 * Take the first pending instance from the list, notice that we remove it
			 * so that we are sure that there is only a single invocation of this method
			 * handling a given pending instance 
			 */
			FutureInstance pendingInstance = dynamicContains.remove(0);
			
			/*
			 * Evaluate triggering conditions and instantiate if satisfied
			 * 
			 */
			pendingInstance.checkInstatiation();
			processed.add(pendingInstance);
		}

		/*
		 * Put back in the list all the processed requests that were not triggered
		 */
		while (! processed.isEmpty()) {
			FutureInstance processedInstance = processed.remove(0);
			if (! processedInstance.isInstantiated())
				dynamicContains.add(processedInstance);
		}
		
	}
	
	/**
	 * Handle state changes in the composite
	 */
	private  void stateChanged(String newState) {
		
		/*
		 * Change state
		 */
		this.state	= newState;
		
		/*
		 * Reevaluate grants
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			
			/*
			 * If the current grant is still valid there is nothing to do
			 */
			GrantDeclaration previuos = granted.get(ownedDeclaration);
			if (previuos != null && previuos.getStates().contains(this.state))
				continue;
			
			/*
			 * Check if another grant is activated
			 */
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				if (grant.getStates().contains(this.state))
					preempt(ownedDeclaration,grant);
			}
			
		}
	}

	
	/**
	 * Preempt access to the owned instances from their current clients, and give it to the
	 * specified grant
	 */
	private void preempt(OwnedComponentDeclaration ownedDeclaration, GrantDeclaration newGrant) {
		
		/*
		 * change current grant
		 */
		granted.put(ownedDeclaration, newGrant);

		/*
		 * preempt all owned instances
		 */
		for (Instance ownedInstance : owned.get(ownedDeclaration)) {
			preempt(ownedInstance, newGrant);
		}
		
		
	}
	
	/**
	 * Preempt access to the specified instance from their current clients and give it
	 * to the specified grant
	 * 
	 * TODO IMPORTANT BUG If the old clients are concurrently resolved again (after the
	 * revoke but before the new resolve) they can get access to the owned instance. 
	 * 
	 * We should find a way to make this method atomic, this requires synchronizing this
	 * content manager and the Apam resolver.
	 */
	private void preempt(Instance ownedInstance,  GrantDeclaration grant) {

		/*
		 * If there is no active grant in the current state, nothing to do
		 */
		if (grant == null)
			return;
		
		/*
		 * If there is no pending request waiting for the grant, nothing to do
		 */
		if (pendingGrants.get(grant).isEmpty())
			return;
		
		/*
		 * revoke all non granted wires
		 */
		for (Link incoming : ownedInstance.getInvLinks()) {
			
			ComponentReference<?> sourceImplementation	= ((Instance)incoming.getSource()).getImpl().getDeclaration().getReference();
			ComponentReference<?> sourceSpecification	= ((Instance)incoming.getSource()).getSpec().getDeclaration().getReference();
			String sourceRelation						= incoming.getName();
			
			ComponentReference<?> grantSource			= grant.getRelation().getDeclaringComponent();
			String grantRelation						= grant.getRelation().getIdentifier();
			
			boolean matchSource 						= grantSource.equals(sourceImplementation) || grantSource.equals(sourceSpecification);
			boolean matchRelation						= grantRelation.equals(sourceRelation);

			if (!matchSource || !matchRelation)
				incoming.remove();
		}

		/*
		 * try to resolve pending requests of this grant
		 */
		for (PendingRequest request : pendingGrants.get(grant)) {
			if (request.isSatisfiedBy(ownedInstance))
				request.resolve();
		}

	}
	
	/**
	 * Whether the specified instance is owned by this composite
	 */
	public boolean owns(Instance instance) {
	
		if (!instance.getComposite().equals(getComposite()))
			return false;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			if (owned.get(ownedDeclaration).contains(instance))
				return true;
		}
		
		return false;
		
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
			
			String propertyValue 	= instance.getProperty(ownedDeclaration.getProperty().getIdentifier());
			boolean matchProperty	= propertyValue != null && ownedDeclaration.getValues().contains(propertyValue);
			
			if (matchType && matchProperty)
				return true;
		}
		
		return false;
	}
	
	/**
	 * The list of potential ownership's conflict between this content manager and the one specified in the parameter
	 */
	public Set<OwnedComponentDeclaration> getConflictingDeclarations(ContentManager that) {
		
		Set<OwnedComponentDeclaration> conflicts = new HashSet<OwnedComponentDeclaration>();
		
		for (OwnedComponentDeclaration thisDeclaration : declaration.getOwnedComponents()) {
			
			ComponentReference<?> thisSpecification = thisDeclaration.getProperty().getDeclaringComponent();
			String thisProperty						= thisDeclaration.getProperty().getIdentifier();
			Set<String> theseValues					= new HashSet<String>(thisDeclaration.getValues());
			
			/*
			 * Ownership declarations are conflicting if they refer to the same specification, with different
			 * properties or with the same values for the same property
			 */

			boolean hasConflict = false;
			for (OwnedComponentDeclaration	thatDeclaration : that.declaration.getOwnedComponents()) {
				
				ComponentReference<?> thatSpecification = thatDeclaration.getProperty().getDeclaringComponent();
				String thatProperty						= thatDeclaration.getProperty().getIdentifier();
				Set<String> thoseValues					= new HashSet<String>(thatDeclaration.getValues());
				
				if (!thisSpecification.equals(thatSpecification))
					continue;
				 
				if( !thisProperty.equals(thatProperty) ||  !Collections.disjoint(theseValues,thoseValues)) 
					hasConflict = true;;
				
			}
			
			if (hasConflict)
				conflicts.add(thisDeclaration);
			
		}
		
		return conflicts;
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
			
			String propertyValue 	= instance.getProperty(ownedDeclaration.getProperty().getIdentifier());
			boolean matchProperty	= propertyValue != null && ownedDeclaration.getValues().contains(propertyValue);
			
			if (!matchProperty)
				continue;

			/*
			 * get ownership
			 */
			((InstanceImpl)instance).setOwner(getComposite());
			owned.get(ownedDeclaration).add(instance);
			
			
			/*
			 * preempt previous users of the instance and give access to granted waiting requests
			 */
			preempt(instance, granted.get(ownedDeclaration));
			
		}			
	}

	/**
	 * Revokes ownership of the specified instance
	 * 
	 */
	public void revokeOwnership(Instance instance) {
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			owned.get(ownedDeclaration).remove(instance);
		}

		((InstanceImpl) instance).setOwner(CompositeImpl.getRootAllComposites());
	}


	/**
	 * Try to resolve all the pending requests that are potentially satisfied by a given component
	 */
	private void resolveRequestsWaitingFor(Component candidate) {
		for (PendingRequest request : waitingResolutions) {
			if (request.isSatisfiedBy(candidate))
				request.resolve();
		}
	}
	
	/**
	 * Try to resolve all the dynamic requests that are potentially satisfied by a given instance
	 */
	private void resolveDynamicRequests(Instance candidate) {
		for (DynamicResolutionRequest request : dynamicDependencies) {
			if (request.isSatisfiedBy(candidate))
				request.resolve();
		}
	}

	
	/**
	 * Updates the contents of this composite when a new implementation is added in APAM
	 */
	public synchronized void implementationAdded(Implementation implementation) {
		
		/*
		 * verify if the new implementation satisfies any pending resolutions in
		 * this composite
		 */
		resolveRequestsWaitingFor(implementation);
	}
	
	
	/**
	 * Updates the contents of this composite when a new instance is added in APAM
	 */
	public synchronized void instanceAdded(Instance instance) {
		
		/*
		 * verify if the new instance satisfies any pending resolutions in this composite
		 */
		resolveRequestsWaitingFor(instance);
		resolveDynamicRequests(instance);

		/*
		 * verify if a newly contained instance has dynamic dependencies or satisfies a trigger
		 */
		if ( instance.getComposite().equals(getComposite())) {
			updateDynamicDependencies(instance);
			updateContainementTriggers();
		}
	}

	/**
	 * Verifies all the effects of a property change in a contained instance
	 * 
	 */
	public synchronized void propertyChanged(Instance instance, String property) {

		/*
		 * For modified contained instances
		 */
		if ( instance.getComposite().equals(getComposite())) {
			
			/*
			 * update triggers
			 */
			updateContainementTriggers();
			
			/*
			 * Force recalculation of dependencies that may have been invalidated by
			 * the property change
			 * 
			 */
			for (Link incoming : instance.getInvLinks()) {
				if (incoming.hasConstraints())
					incoming.remove();
			}

			/*
			 * Verify if property change triggers a state change
			 */
			if (stateHolder != null && stateHolder.equals(instance) && stateProperty.equals(property))
				stateChanged(stateHolder.getProperty(stateProperty));

		}

		/*
		 * verify if the modified instance satisfies any pending resolutions and dynamic
		 * dependencies in this composite
		 */
		if (instance.isSharable() && Visible.checkInstVisible(getComposite(),instance)) {
			resolveRequestsWaitingFor(instance);
	        resolveDynamicRequests(instance);
		}

	}


	/**
	 * Updates the contents of this composite when a contained instance is removed from APAM
	 * 
	 */
	public synchronized void instanceRemoved(Instance instance) {
		
		assert instance.getComposite().equals(getComposite());

		/*
		 * update state
		 */
		if (instance == stateHolder)
			stateHolder = null;
		
		/*
		 * update list of owned instances
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			if (owned.get(ownedDeclaration).contains(instance))
				owned.get(ownedDeclaration).remove(instance);
		}
		
		/*
		 * update list of dynamic dependencies
		 */
		List<DynamicResolutionRequest> removedDynamicRequests = new ArrayList<DynamicResolutionRequest>();
		for (DynamicResolutionRequest dynamicRelation : dynamicDependencies) {
			if (dynamicRelation.getSource().equals(instance))
				removedDynamicRequests.add(dynamicRelation);
		}
		
		dynamicDependencies.removeAll(removedDynamicRequests);
		
		/*
		 * update list of waiting requests
		 */
		List<PendingRequest> removedWaitingResolutions = new ArrayList<PendingRequest>();
		for (PendingRequest pendingRequest : waitingResolutions) {
			if (pendingRequest.getSource().equals(instance))
				removedWaitingResolutions.add(pendingRequest);
		}
		
		waitingResolutions.removeAll(removedWaitingResolutions);
		
	}
	
	/**
	 * Updates the contents of this composite when a wire is removed from APAM.
	 * 
	 * If the target of the wire is a non sharable instance, the released instance can
	 * potentially be used by a pending requests.
	 * 
	 */
	public synchronized void linkRemoved(Link link) {
		Instance instance = (Instance)link.getDestination();
		if (instance.isSharable() && Visible.checkInstVisible(getComposite(),instance))
			resolveRequestsWaitingFor(instance);
	}
	
	/**
	 * Add a new pending request in the content of the composite
	 */
	public synchronized void addPendingRequest(PendingRequest request) {
		
		/*
		 * add to the list of pending requests
		 */
		waitingResolutions.add(request);
		
		/*
		 * Verify if the request corresponds to a grant for an owned instance
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {

			GrantDeclaration currentGrant = granted.get(ownedDeclaration);

			for (GrantDeclaration grant :ownedDeclaration.getGrants()) {
				
				ComponentReference<?> sourceImplementation	= request.getSource().getImpl().getDeclaration().getReference();
				ComponentReference<?> sourceSpecification	= request.getSource().getSpec().getDeclaration().getReference();
				String sourceRelation						= request.getRelation().getIdentifier();
				
				ComponentReference<?> grantSource			= grant.getRelation().getDeclaringComponent();
				String grantRelation						= grant.getRelation().getIdentifier();
				
				boolean matchSource 						= grantSource.equals(sourceImplementation) || grantSource.equals(sourceSpecification);
				boolean matchRelation						= grantRelation.equals(sourceRelation);

				/*
				 * add request to list of pending grants and try to preempt the owned instances
				 */
				if (matchSource && matchRelation) {
					pendingGrants.get(grant).add(request);
					if (currentGrant != null && currentGrant.equals(grant))
						preempt(ownedDeclaration, grant);
				}
			}
		}
	}
	
	/**
	 * Remove a pending request from the content of the composite
	 */
	public synchronized void removePendingRequest(PendingRequest request) {

		waitingResolutions.remove(request);

		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant :ownedDeclaration.getGrants()) {
				pendingGrants.get(grant).remove(request);
			}
		}
		
	}

	
	/**
	 * The composite is removed
	 */
	public void dispose() {
	}


}
