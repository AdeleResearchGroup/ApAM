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

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.ResolutionException;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.RelationDeclaration;

/**
 * This class handles failure when a dependency is missing and the resolution
 * process fails to find a solution.
 * 
 * @author vega
 * 
 */

public class FailedResolutionManager implements RelationManager, DynamicManager, PropertyManager {


	/**
	 * The list of waiting resolutions
	 */
	private final List<PendingRequest> waitingResolutions;

	/**
	 * A reference to the APAM machine
	 */
	private Apam apam;

	public FailedResolutionManager() {
		waitingResolutions = new ArrayList<PendingRequest>();
	}

	@Override
	public String getName() {
		return "FailedResolutionManager";
	}

	public Apam getApam() {
		return apam;
	}

	/**
	 * A filter for pending request, that can be used to request a resolution for a subset
	 * of the waiting request satisfying a condition.
	 * 
	 * This may be useful to avoid retrying all the request when a condition is met, but only
	 * the requests that are impacted by the event.
	 */
	public static interface Scope {
		
		public boolean concerns(PendingRequest request);
	}
	
	/**
	 * Try to resolve all the requests that are potentially satisfied by a given
	 * component. This may happen when a new component is created, or some property
	 * has changed that may satisfy a pending request.
	 * 
	 * Only consider request sat
	 */
	public void resolveWaitingRequests(Scope scope, Component candidate) {
		for (PendingRequest request : getWaitingRequests()) {
			if (scope.concerns(request) && request.isSatisfiedBy(candidate)) {
				request.resolve();
			}
		}
	}

	private static final Scope GLOBAL = new Scope() {
		public boolean concerns(PendingRequest request) {
			return true;
		}
	};

	public final void resolveWaitingRequests(Component candidate) {
		resolveWaitingRequests(GLOBAL, candidate);
	}

	/**
	 * This is an INTERNAL manager that will be invoked by the core. 
	 * 
	 * So in this method we signal that we are not part of the external handlers to
	 * invoke for this resolution request.
	 * 
	 */
	@Override
	public boolean beginResolving(RelToResolve relToResolve) {
		return false;
	}

	/**
	 * If this manager is invoked it means that the specified relationship could
	 * not be resolved, so we have to apply the specified dynamic policy.
	 */
	@Override
	public Resolved<?> resolve(RelToResolve relToResolve) {

		/*
		 * If no policy is specified for the relationship just ignore it.
		 */
		if (relToResolve.getMissingPolicy() == null) {
			return null;
		}

		/*
		 * In case of retry of a waiting or eager request we simply return to
		 * avoid blocking or killing the unrelated thread that triggered the
		 * recalculation
		 */
		if (PendingRequest.isRetry()) {
			return null;
		}

		/*
		 * Apply failure policies
		 */
		switch (relToResolve.getMissingPolicy()) {
		case OPTIONAL: {
			return null;
		}

		case EXCEPTION: {
			throwMissingException(relToResolve.getLinkSource(), relToResolve);
		}

		case WAIT: {

			/*
			 * schedule request
			 */
			PendingRequest request = new PendingRequest(CST.apamResolver, relToResolve.getLinkSource(), relToResolve.getRelationDefinition());

			addWaitingRequests(request);
			request.block();
			removeWaitingRequests(request);

			return request.getResolution();
		}
		}

		return null;
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
		this.apam = null;

		ApamManagers.removeDynamicManager(this);
		ApamManagers.removePropertyManager(this);

		/*
		 * Try to dispose all waiting requests
		 */
		for (PendingRequest request : getWaitingRequests()) {
			request.dispose();
		}
	}

	@Override
	public void addedComponent(Component component) {
		resolveWaitingRequests(component);
	}

	@Override
	public void removedComponent(Component component) {
		for (PendingRequest request : getWaitingRequests()) {
			if (request.getSource().equals(component)) {
				request.dispose();
			}
		}

	}

	private void propertyChanged(Component component, String property) {
		resolveWaitingRequests(component);
	}

	@Override
	public void attributeRemoved(Component component, String attr, String oldValue) {
		propertyChanged(component, attr);
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
	public void removedLink(Link link) {

		/*
		 * If the target of the wire is a non sharable instance, the released
		 * instance can potentially be used by a pending requests.
		 */
		if (link.getDestination() instanceof Instance) {
			Instance candidate = (Instance) link.getDestination();

			if ((!candidate.isShared()) && candidate.isSharable()) {
				resolveWaitingRequests(candidate);
			}
		}
	}


	/**
	 * Add a new pending request in the waiting list
	 */
	private void addWaitingRequests(PendingRequest request) {
		synchronized (waitingResolutions) {
			waitingResolutions.add(request);
		}
	}

	/**
	 * Remove a pending request from the waiting list
	 */
	private void removeWaitingRequests(PendingRequest request) {

		synchronized (waitingResolutions) {
			waitingResolutions.remove(request);
		}
	}

	/**
	 * Get a thread-safe (stack confined) copy of the waiting requests
	 */
	public List<PendingRequest> getWaitingRequests() {
		synchronized (waitingResolutions) {
			return new ArrayList<PendingRequest>(waitingResolutions);
		}
	}


	/**
	 * Hack to throw a checked exception from inside the framework without
	 * wrapping it.
	 * 
	 * When the formal type parameter E is replaced by the actual type argument
	 * RuntimeException the erased signature of this method seems to throw an
	 * unchecked exception, however we throw the original exception.
	 * 
	 * Currently at runtime the specified cast is a NOOP, this may not work
	 * depending on the used JVM
	 */
	@SuppressWarnings("unchecked")
	private static <E extends Exception> void doThrow(Exception e) throws E {
		throw (E) e;
	}

	/**
	 * Throws the exception associated with a missing relation
	 */
	private static void throwMissingException(Component source, RelToResolve relToResolve) {
		try {

			/*
			 * If no exception is specified throw ResolutionException
			 */
			String exceptionName = relToResolve.getMissingException();
			if (exceptionName == null) {
				throw new ResolutionException();
			}

			/*
			 * Try to find the component declaring the relation that specified
			 * the Exception to throw
			 * 
			 * TODO BUG : the class should be loaded using the bundle context of
			 * the component where the relation is declared. This can be either
			 * the specification, or the implementation of the source component,
			 * or a composite in the case of contextual dependencies. The
			 * current implementation may not handle every case.
			 * 
			 * The best solution is to modify relationDeclaration to load the
			 * exception class, but this is not possible at compile time, so we
			 * can not change the signature of
			 * relationDeclaration.getMissingException.
			 * 
			 * A possible solution is to move this method to relationDeclaration
			 * and make it work only at runtime, but we need to consider merge
			 * of contextual dependencies and use the correct bundle context.
			 * 
			 * Evaluate changes to relationDeclaration, relation,
			 * CoreMetadataParser and computeEffectiverelation
			 */
			Component declaringComponent = source;
			while (declaringComponent != null) {

				RelationDeclaration declaration = declaringComponent.getDeclaration().getLocalRelation(relToResolve.getName());
				if (declaration != null && declaration.getMissingException() != null && declaration.getMissingException().equals(exceptionName)) {
					break;
				}

				declaringComponent = declaringComponent.getGroup();
			}

			if (declaringComponent == null && source instanceof Instance) {
				declaringComponent = ((Instance) source).getComposite().getCompType();
			}

			if (declaringComponent == null) {
				throw new ResolutionException();
			}

			/*
			 * throw the specified exception
			 */

			Class<?> exceptionClass = declaringComponent.getApformComponent().getBundle().loadClass(exceptionName);
			Exception exception = Exception.class.cast(exceptionClass.newInstance());

			FailedResolutionManager.<RuntimeException> doThrow(exception);

		} catch (ClassNotFoundException e) {
			throw new ResolutionException(e);
		} catch (InstantiationException e) {
			throw new ResolutionException(e);
		} catch (IllegalAccessException e) {
			throw new ResolutionException(e);
		}

	}

}
