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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class implements the manager in charge of dynamically updating the
 * architecture, in response to execution context evolution.
 * 
 * @author vega
 * 
 */

public class DynaMan implements DynamicManager, PropertyManager {

	private final static Logger logger = LoggerFactory.getLogger(DynaMan.class);

	/**
	 * A reference to the APAM machine
	 */
	@SuppressWarnings("unused")
	private Apam apam;

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
	 * The dynamic manager handle all request concerned with dynamic management
	 * of links
	 */
	public DynaMan() {
		dynamicDependencies = new ArrayList<PendingRequest>();
		futureInstances = new ArrayList<FutureInstance>();
	}

	/**
	 * Add a new dynamic request
	 */
	private void addDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			dynamicDependencies.add(request);
		}
	}

	/**
	 * Reduce the priority of a request
	 */
	private void reducePriorityDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			if (dynamicDependencies.remove(request))
				dynamicDependencies.add(request);
		}
	}

	/**
	 * Updates the list of dynamic dependencies when a new component is managed
	 */
	private void addDynamicRequests(Component component) {

		for (RelationDefinition relDef : component.getRelations()) {

			if (component.getKind().equals(relDef.getSourceKind()) && relDef.isDynamic()) {

				PendingRequest request = new PendingRequest(CST.apamResolver, component, relDef);
				addDynamicRequest(request);

				if (relDef.getCreation() == CreationPolicy.EAGER) {
					request.resolve();
				}
			}
		}
	}

	@Override
	public void addedComponent(Component component) {

		/*
		 * Add the dynamic dependencies and future instances of the newly-added
		 * component
		 */
		addDynamicRequests(component);
		if (component instanceof Composite) {
			addFutureInstances((Composite) component);
		}

		/*
		 * Verify if the new component satisfies some pending requests or the
		 * triggering condition of future instances
		 */
		resolveDynamicRequests(component);
		resolveFutureInstances(component);
	}

	@Override
	public void addedLink(Link link) {
	}

	/**
	 * Add a new future Instance
	 */
	private void addFutureInstance(FutureInstance request) {
		synchronized (futureInstances) {
			futureInstances.add(request);
		}
	}

	/**
	 * Updates the list of future instances when a new composite is managed
	 */
	private void addFutureInstances(Composite composite) {
		for (InstanceDeclaration instanceDeclaration : composite.getCompType().getCompoDeclaration().getInstanceDeclarations()) {
			try {

				/*
				 * Create the future instance request and evaluate triggering
				 * conditions in the initial configuration
				 */
				FutureInstance request = new FutureInstance(composite, instanceDeclaration);
				request.checkInstatiation();

				if (!request.isInstantiated()) {
					addFutureInstance(request);
				}

			} catch (InvalidConfiguration error) {
				logger.error("Error managing dynmaic instances for composite " + composite.getName(), error);
			}
		}
	}

	@Override
	public void attributeAdded(Component component, String attr, String newValue) {
		propertyChanged(component, attr);
	}

	@Override
	public void attributeChanged(Component component, String attr, String newValue, String oldValue) {
		propertyChanged(component, attr);
	}

	@Override
	public void attributeRemoved(Component component, String attr, String oldValue) {
		propertyChanged(component, attr);
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
	 * Get a thread-safe (stack confined) copy of the dynamic requests
	 */
	private List<FutureInstance> getFutureInstances() {
		synchronized (futureInstances) {
			return new ArrayList<FutureInstance>(futureInstances);
		}
	}

	/**
	 * Dynamic manager identifier
	 */
	@Override
	public String getName() {
		return CST.DYNAMAN;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	/**
	 * Dynaman does not have its own model, all the information is in the
	 * component declaration.
	 */
	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

	public void ownershipChanged(Instance instance) {
		/*
		 * Verify if the component in the new context satisfies some pending
		 * requests or the triggering condition of future instances
		 */
		resolveDynamicRequests(instance);
		resolveFutureInstances(instance);
	}

	private void propertyChanged(Component component, String property) {
		/*
		 * Verify if the updated component satisfies some pending requests or
		 * the triggering condition of future instances
		 */
		resolveDynamicRequests(component);
		resolveFutureInstances(component);
	}

	@Override
	public void removedComponent(Component component) {

		/*
		 * Remove from the list of dynamic requests all requests originating
		 * from the removed component
		 */
		for (PendingRequest request : getDynamicRequests()) {
			if (request.getSource().equals(component)) {
				removeDynamicRequest(request);
			}
		}

		/*
		 * Remove from the list of future instances all requests originating
		 * from the removed component
		 */
		for (FutureInstance request : getFutureInstances()) {
			if (request.getOwner().equals(component)) {
				removeFutureInstance(request);
			}
		}

	}

	@Override
	public void removedLink(Link link) {

		/*
		 * If the target of the wire is a non sharable instance, the released
		 * instance can potentially be used by a pending requests.
		 */
		if (link.getDestination() instanceof Instance) {
			Instance candidate = (Instance) link.getDestination();

			if ((!candidate.isShared()) && candidate.isSharable()) {
				resolveDynamicRequests(candidate);
			}
		}
	}

	/**
	 * Remove a dynamic request
	 */
	private void removeDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			dynamicDependencies.remove(request);
		}
	}

	/**
	 * Remove a dynamic request
	 */
	private void removeFutureInstance(FutureInstance request) {
		synchronized (futureInstances) {
			futureInstances.remove(request);
		}
	}

	/**
	 * Try to resolve all the requests that are potentially satisfied by a given
	 * component
	 */
	private void resolveDynamicRequests(Component candidate) {
		for (PendingRequest request : getDynamicRequests()) {
			if (request.isSatisfiedBy(candidate)) {

				request.resolve();
				
				/*
				 * If the target is a non sharable instance, and the resolution was successful, 
				 * we reduce the priority of the request to avoid starvation of other dynamic
				 * request expecting the same target
				 */
				if ( request.getResolution() != null && candidate instanceof Instance) {
					if (!((Instance)candidate).isSharable())
						reducePriorityDynamicRequest(request);
				}

			}	
		}
	}

	/**
	 * Verifies if the triggering conditions of pending future instances are
	 * satisfied
	 */
	private void resolveFutureInstances(Component component) {

		/*
		 * Future instances are triggered when a matching instance is added to
		 * the composite where the future is declared
		 */
		if (!(component instanceof Instance)) {
			return;
		}

		Instance candidate = (Instance) component;

		/*
		 * Iterate over all pending future instances
		 * 
		 * IMPORTANT Notice that this iteration is synchronized so we do not
		 * have concurrent accesses over the list of future instances. However,
		 * we must carefully handle the case of nested invocations of this
		 * method (because instantiation of a dynamic instance may trigger other
		 * pending instances)
		 */

		synchronized (futureInstances) {

			List<FutureInstance> processed = new ArrayList<FutureInstance>();
			while (!futureInstances.isEmpty()) {

				/*
				 * Take the first pending instance from the list, notice that we
				 * remove it so that we are sure that there is only a single
				 * invocation (in case of nested invocations) of this method
				 * handling a given pending instance
				 */
				FutureInstance futureInstance = futureInstances.remove(0);

				/*
				 * Evaluate triggering conditions and instantiate if satisfied
				 */
				if (futureInstance.getOwner().equals(candidate.getComposite())) {
					futureInstance.checkInstatiation();
				}

				processed.add(futureInstance);
			}

			/*
			 * Put back in the list all the processed requests that were not
			 * triggered
			 */
			while (!processed.isEmpty()) {
				FutureInstance processedInstance = processed.remove(0);
				if (!processedInstance.isInstantiated()) {
					futureInstances.add(processedInstance);
				}
			}
		}

	}

	/**
	 * This method is automatically invoked when the manager is validated, so we
	 * can safely assume that APAM is available
	 */
	public synchronized void start(Apam apam) {
		this.apam = apam;

		ApamManagers.addDynamicManager(this);
		ApamManagers.addPropertyManager(this);

	}

	/**
	 * This method is automatically invoked when the manager is invalidated, so
	 * APAM is no longer available
	 */
	public synchronized void stop() {
		ApamManagers.removeDynamicManager(this);
		ApamManagers.removePropertyManager(this);

		this.apam = null;
	}

}
