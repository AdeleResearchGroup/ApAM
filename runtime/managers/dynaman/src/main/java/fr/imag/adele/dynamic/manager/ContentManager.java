package fr.imag.adele.dynamic.manager;

import java.util.ArrayList;
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
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.DependencyInjection;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;
import fr.imag.adele.apam.impl.InstanceImpl;
import fr.imag.adele.apam.util.Util;


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
	 * The managed composite 
	 */
	private final Composite composite;
	
	/**
	 * The declaration of the composite type
	 */
	private final CompositeDeclaration declaration;

	/**
	 * The current state of the content manager
	 */
	private String state;

	/**
	 * The instance that holds the state of the content manager
	 */
	private Instance	stateHolder;

	/**
	 * The property used for holding the state
	 */
	private String 		stateProperty;
	
	/**
	 * The contained instances that are owned by this content manager
	 */
	private Map<OwnedComponentDeclaration, Set<Instance>> owned;

	/**
	 * The current granted dependencies 
	 */
	private Map<OwnedComponentDeclaration, GrantDeclaration> granted;

	/**
	 * The list of pending resolutions in this composite
	 */
	private List<PendingRequest<?>> pendingResolutions;

	/**
	 * The list of pending resolutions waiting for a grant. This is a subset
	 * of the pending resolutions indexed by the associated grant.
	 */
	private Map<GrantDeclaration, List<PendingRequest<?>>> pendingGrants;
	
	
	/**
	 * The list of dynamic dependencies that must be updated without waiting
	 * for lazy resolution
	 */
	private List<DynamicResolutionRequest> dynamicRequests;
	
	/**
	 * Initializes the content manager
	 */
	public ContentManager(DynamicManagerImplementation manager, Composite composite) throws InvalidConfiguration {
		
		this.composite		= composite;
		this.declaration	= composite.getCompType().getCompoDeclaration();
		
		/*
		 * Initialize state
		 */
		
		this.stateHolder		= null;
		this.stateProperty		= null;
		this.state				= null;
		
		if (declaration.getStateProperty() != null) {
			
			PropertyDefinition.Reference propertyReference = declaration.getStateProperty();

			/*
			 * Get the component that defines the property, notice that this may deploy the implementation if not yet installed
			 */
			Component implementation = CST.apamResolver.findComponentByName(composite.getCompType(),propertyReference.getDeclaringComponent().getName());
			
			/*
			 * In case the implementation providing the state is not available signal an error. 
			 * 
			 * TODO We should not add the composite in APAM, but currently there is no way for a manager
			 * to avoid reification
			 */
			if (implementation == null || ! (implementation instanceof Implementation)) {
				throw new InvalidConfiguration("Invalid state declaration, implementation can not be found "+propertyReference.getDeclaringComponent().getName());
			}

			/*
			 * Eagerly instantiate an instance to hold the state
			 *
			 * TODO In case the main instance can be used to hold state we avoid creating additional objects,
			 * we need to evaluate if it is useful or not
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
			 * TODO We should not add the composite in APAM, but currently there is no way for a manager
			 * to avoid reification
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
		 * Initialize the list of pending resolutions (initially empty)
		 */
		pendingResolutions	= new ArrayList<PendingRequest<?>>();
		pendingGrants		= new HashMap<GrantDeclaration, List<PendingRequest<?>>>();
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant : ownedDeclaration.getGrants()) {
				pendingGrants.put(grant, new ArrayList<PendingRequest<?>>());
			}
		}
		
		/*
		 * Initialize list of dynamic dependencies
		 */
		dynamicRequests	= new ArrayList<DynamicResolutionRequest>();
		for (Instance conained : composite.getContainInsts()) {
			verifyDynamicDependencies(conained);
		}
	}
	
	/**
	 * The composite managed by this content manager
	 */
	public Composite getComposite() {
		return composite;
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
		for (Wire incoming : ownedInstance.getInvWires()) {
			boolean granted	= 	incoming.getSource().getImpl().getDeclaration().getDependency(grant.getDependency()) != null ||
								incoming.getSource().getSpec().getDeclaration().getDependency(grant.getDependency()) != null;
			if (! granted)
				incoming.remove();
		}

		/*
		 * try to resolve pending requests of this grant
		 */
		for (PendingRequest<?> request : pendingGrants.get(grant)) {
			if (request.isSatisfiedBy(ownedInstance))
				request.resolve();
		}

	}
	
	/**
	 * Verifies if the specified instance must be owned by this composite
	 */
	public void verifyOwnership(Instance instance) {
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {


			/*
			 * Verify if the instance matches the ownership criteria
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
			 * Get ownership of the instance
			 */
			((InstanceImpl)instance).setOwner(getComposite());
			owned.get(ownedDeclaration).add(instance);
			
			/*
			 * Grant access to the instance to the pending granted requests
			 */
			preempt(instance, granted.get(ownedDeclaration));
			
		}			
	}

	/**
	 * Verifies the specified instance to see if ownership is still valid
	 */
	private void verifyRelinquish(Instance instance) {
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			
			/*
			 * ignore instances not owned
			 */
			if (! owned.get(ownedDeclaration).contains(instance))
				continue;
			
			/*
			 * verify property value matches the required values
			 */
			String propertyValue	= instance.getProperty(ownedDeclaration.getProperty().getIdentifier());
			boolean matchProperty	= propertyValue != null && ownedDeclaration.getValues().contains(propertyValue);
			
			/*
			 * Relinquish instance if no longer matches criteria
			 */
			if (! matchProperty) {
				owned.get(ownedDeclaration).remove(instance);
				((InstanceImpl) instance).setOwner(CompositeImpl.getRootAllComposites());
			}
		}
			
	}


	/**
	 * Try to resolve all the pending requests that are potentially satisfied by a given component
	 */
	private void resolveRequestsWaitingFor(Component candidate) {
		for (PendingRequest<?> request : pendingResolutions) {
			if (request.isSatisfiedBy(candidate))
				request.resolve();
		}
	}
	
	/**
	 * Try to resolve all the dynamic requests that are potentially satisfied by a given instance
	 */
	private void resolveDynamicRequests(Instance candidate) {
		for (DynamicResolutionRequest request : dynamicRequests) {
			if (request.isSatisfiedBy(candidate))
				request.resolve();
		}
	}

	/**
	 * Verify if a contained instance has declared dynamic isntances
	 */
	private void verifyDynamicDependencies(Instance instance) {
		
		for (DependencyDeclaration dependency : Util.computeAllEffectiveDependency(instance)) {

			boolean hasField =  false;
			for (DependencyInjection injection : dependency.getInjections()) {
				if (injection instanceof DependencyInjection.Field) {
					hasField = true;
					break;
				}
			}
			/*
			 * ignore lazy dependencies
			 */
			if (! hasField || dependency.isMultiple() || dependency.isEager())
				dynamicRequests.add(new DynamicResolutionRequest(CST.apamResolver,instance,dependency));
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
		if (Util.checkImplVisible(getComposite().getCompType(),implementation))
			resolveRequestsWaitingFor(implementation);
	}
	
	
	/**
	 * Updates the contents of this composite when a new instance is added in APAM
	 */
	public synchronized void instanceAdded(Instance instance) {
		
		/*
		 * verify if the instance should be owned by this composite
		 * 
		 * TODO We need to verify that there is no conflicting ownership declarations.
		 * In the current implementation the first invoked content manager silently
		 * gets the ownership of the instance. 
		 */
		//if (! instance.isUsed())
			verifyOwnership(instance);
		
		/*
		 * verify if the new instance satisfies any pending resolutions in
		 * this composite
		 */
		if (instance.isSharable() && Util.checkInstVisible(getComposite(),instance)) {
			resolveRequestsWaitingFor(instance);
			resolveDynamicRequests(instance);
		}

		/*
		 * verify if a newly contained instance has dynamic dependencies  
		 */
		if ( instance.getComposite().equals(getComposite()))
			verifyDynamicDependencies(instance);
	}


	/**
	 * Updates the contents of this composite when a contained instance is removed from APAM
	 */
	public synchronized void instanceRemoved(Instance instance) {
		
		assert instance.getComposite().equals(getComposite());

		if (instance == stateHolder)
			stateHolder = null;
		
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			if (owned.get(ownedDeclaration).contains(instance))
				owned.get(ownedDeclaration).remove(instance);
		}
	}
	
	/**
	 * Updates the contents of this composite when a wire is removed from APAM.
	 * 
	 * If the target of the wire is a non sharable instance, the released instance can
	 * potentially be used by a pending requests.
	 * 
	 * TODO Currently there is no manager notification when a wire is removed so this
	 * case is not being considered, change API of dynamic manager. 
	 */
	public synchronized void wireRemoved(Wire wire) {
		Instance instance = wire.getDestination();
		if (instance.isSharable() && Util.checkInstVisible(getComposite(),instance))
			resolveRequestsWaitingFor(instance);
	}
	
	/**
	 * Add a new pending request in the content of the composite
	 */
	public synchronized void addPendingRequest(PendingRequest<?> request) {
		
		/*
		 * add to the list of pending requests
		 */
		pendingResolutions.add(request);
		
		/*
		 * Verify if the request corresponds to a grant for an owned instance, and
		 * index it to accelerate scheduling of grants
		 */
		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant :ownedDeclaration.getGrants()) {
				
				/*
				 * TODO BUG This test may fail if the resolution dependency is defined in the
				 * specification or refined in the instance. Dependency declaration equality
				 * is defined based on the name of the dependency PLUS the defining component.
				 * 
				 * Comparing only the name of the dependency is not correct. Because it is 
				 * possible, and frequent, to have dependencies with the same name in different
				 * implementations or specifications.
				 * 
				 * The right test will be to get the actual implementation of the  source instance
				 * of the resolution and compare it to grant.getDependency().getDeclaringComponent(),
				 * but this information currently is not available in the dependency manager API 
				 */
				if (request.getDependency().equals(grant.getDependency()))
					pendingGrants.get(grant).add(request);
			}
		}
	}
	
	/**
	 * Remove a pending request from the content of the composite
	 */
	public synchronized void removePendingRequest(PendingRequest<?> request) {

		pendingResolutions.remove(request);

		for (OwnedComponentDeclaration ownedDeclaration : declaration.getOwnedComponents()) {
			for (GrantDeclaration grant :ownedDeclaration.getGrants()) {
				pendingGrants.get(grant).remove(request);
			}
		}
		
	}

	
	/**
	 * Verifies all the effects of a property change in a contained instance
	 * 
	 */
	public synchronized void propertyChanged(Instance instance, String property) {

		assert instance.getComposite().equals(getComposite());

		/*
		 * Force recalculation of dependencies that may have been invalidated by
		 * the property change
		 * 
		 */
		for (Wire incoming : instance.getInvWires()) {
			if (incoming.hasConstraints())
				incoming.remove();
		}

		/*
		 * Verify if property change triggers a state change
		 */
		if (stateHolder != null && stateHolder.equals(instance) && stateProperty.equals(property))
			stateChanged(stateHolder.getProperty(stateProperty));
		
		/*
		 * Verify if property change triggers an ownership loss
		 * 
		 */
		//verifyRelinquish(instance);
		
		/*
		 * verify if the modified instance satisfies any pending resolutions in
		 * this composite
		 */
		resolveRequestsWaitingFor(instance);

        resolveDynamicRequests(instance);
	}



	/**
	 * The composite is removed
	 */
	public void dispose() {
	}


}
