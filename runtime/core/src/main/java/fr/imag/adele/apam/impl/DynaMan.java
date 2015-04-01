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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
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
	 * The task executor. We use a pool of a threads to handle resolutions and instantiations
	 * that may block
	 */
	private final Executor taskExecutor = Executors.newCachedThreadPool();
	
	/**
	 * The task in charge of performing dynamic resolution 
	 */
	private class ResolutionTask implements Runnable {
	
		private final PendingRequest 	request;
		private final Component 		candidate;
		
		public ResolutionTask(PendingRequest request, Component candidate) {
			this.request 	= request;
			this.candidate	= candidate;
		}

		@Override
		public void run() {
			
			/*
			 * perform resolution
			 */
			request.resolve();
			
			
			/*
			 * If the relation is marked as dynamic/eager and optional, we simply ignore unsuccessful 
			 * asynchronous resolutions.
			 */
			if (request.getResolution() == null) {
				return;
			}
			
			/*
			 * If the target is a non sharable instance, and the resolution was successful,  we reduce
			 * the priority of the request to avoid starvation of other dynamic request expecting the
			 * same target
			 */
			if (candidate != null && candidate instanceof Instance && request.getResolution() != null) {
				if (!((Instance)candidate).isSharable())
					reducePriorityDynamicRequest(request);
			}
		}
	}
	
	/**
	 * The task in charge of instantiating declared future instances
	 */
	private class InstantiationTask implements Runnable {
		
		private final FutureInstance 	instantiableFutureInstance;
		
		public InstantiationTask(FutureInstance instantiableFutureInstance) {
			this.instantiableFutureInstance 	= instantiableFutureInstance;
		}

		@Override
		public void run() {
			
			/*
			 * Try to instantiate triggered instance
			 * 
			 */
			instantiableFutureInstance.instantiate();

			/*
			 * Put the request back in the pending list, if it did not succeed 
			 */
			if (!instantiableFutureInstance.isInstantiated()) {
				addFutureInstance(instantiableFutureInstance);
			}
		}
	}
	
	/**
	 * The dynamic manager handle all request concerned with dynamic management
	 * of links
	 */
	public DynaMan() {
		dynamicDependencies = new ArrayList<PendingRequest>();
		futureInstances = new ArrayList<FutureInstance>();
	}

	/**
	 * Dynamic manager identifier
	 */
	@Override
	public String getName() {
		return CST.DYNAMAN;
	}

	/**
	 * Schedule resolution of all the requests that are potentially satisfied by a given component
	 */
	private void resolveDynamicRequests(Component candidate) {
		for (PendingRequest request : getDynamicRequests()) {
			if (request.isSatisfiedBy(candidate)) {
				taskExecutor.execute(new ResolutionTask(request, candidate));
			}	
		}
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
	 * Remove a dynamic request
	 */
	private void removeDynamicRequest(PendingRequest request) {
		synchronized (dynamicDependencies) {
			dynamicDependencies.remove(request);
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

				/*
				 * In casez of eager relationships, schedule initial resolution
				 */
				if (relDef.getCreation() == CreationPolicy.EAGER) {
					taskExecutor.execute(new ResolutionTask(request, null));
				}
			}
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
	 * Add a new future Instance
	 */
	private void addFutureInstance(FutureInstance request) {
		synchronized (futureInstances) {
			futureInstances.add(request);
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
	 * Verifies if the triggering conditions of pending future instances defined in the specified 
	 * composite are satisfied
	 */
	private void resolveFutureInstances(Composite context) {


		/*
		 * Take a snapshot of future instances that can be instantiated in the updated context
		 * 
		 * IMPORTANT  Notice that we remove the request from the list of future instances, so that we
		 * are sure* that there is a single thread handling a given pending instantiation request
		 * 
		 */

		List<FutureInstance> instantiableFutureInstances = new ArrayList<FutureInstance>();
		synchronized (futureInstances) {

			for (FutureInstance futureInstance : futureInstances) {
				if (futureInstance.getOwner().equals(context) && futureInstance.isInstantiable()) {
					instantiableFutureInstances.add(futureInstance);
				}
			}

			futureInstances.removeAll(instantiableFutureInstances);
		}
		
		/*
		 * Schedule instantiation of triggered instances
		 * 
		 */
		for (FutureInstance instantiableFutureInstance : instantiableFutureInstances) {
			taskExecutor.execute(new InstantiationTask(instantiableFutureInstance));
		}

	}
	
	/**
	 * Updates the list of future instances when a new composite is managed
	 */
	private void addFutureInstances(Composite composite) {

		/*
		 * Create the future instance corresponding to the declared start sentences 
		 */
		for (InstanceDeclaration instanceDeclaration : composite.getCompType().getCompoDeclaration().getInstanceDeclarations()) {
			try {
				addFutureInstance( new FutureInstance(composite, instanceDeclaration));
			} catch (InvalidConfiguration error) {
				logger.error("Error managing dynamic instances for composite " + composite.getName(), error);
			}
		}

		/*
		 * Evaluate the the triggering conditions in the initial configuration
		 */
		resolveFutureInstances(composite);
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
	 * Reevaluate all dynamic relationships and instantiations impacted by changes in the
	 * specified component
	 */
	private void propagateComponentChange(Component component) {
		
		resolveDynamicRequests(component);
		if (component instanceof Instance) {
			resolveFutureInstances(((Instance)component).getComposite());
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
		propagateComponentChange(component);
	}

	@Override
	public void addedLink(Link link) {
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

	private void propertyChanged(Component component, String property) {
		/*
		 * Verify if the updated component satisfies some pending requests or
		 * the triggering condition of future instances
		 */
		propagateComponentChange(component);
	}

	public void ownershipChanged(Instance instance) {
		/*
		 * Verify if the component in the new context satisfies some pending
		 * requests or the triggering condition of future instances
		 */
		propagateComponentChange(instance);
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

		/*
		 * Try to dispose all dynamic requests
		 */
		for (PendingRequest request : getDynamicRequests()) {
			request.dispose();
		}
		
		this.apam = null;
	}

}
