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

import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.impl.CompositeImpl;

/**
 * This class is used to represent the pending requests that are waiting for resolution.
 * 
 * 
 */
public class PendingRequest {

	/**
	 * The source of the relation
	 */
	protected final Component source;
	
	/**
	 * The relation to resolve
	 */
	protected final Relation relation;

	/**
	 * The composite in which context the resolution will be performed
	 */
	protected final Composite context;
	
	/**
	 * The resolver
	 */
	protected final ApamResolverImpl resolver;
	
	/**
	 * The result of the resolution
	 */
	private Resolved<?> resolution;

	/**
	 * Whether this request is being resolved in some thread
	 */
	private boolean isResolving = false;

	/**
	 * Whether the thread that created this request is blocked
	 */
	private boolean isBlocked = false;
	
	/**
	 * Whether 
	 */
	/**
	 * Builds a new pending request reification
	 */
	protected PendingRequest(ApamResolverImpl resolver, Component source, Relation relation) {
		this.resolver		= resolver;
		
		this.source			= source;
		this.context		= (source instanceof Instance) ? ((Instance)source).getComposite() : CompositeImpl.getRootAllComposites();
		this.relation		= relation;
		
		this.resolution		= null;
	}
	
	public Component getSource() {
		return source;
	}
	
	/**
	 * The relation that needs resolution
	 */
	public Relation getRelation() {
		return relation;
	}
	
	/**
	 * The context in which the resolution is requested
	 */
	public Composite getContext() {
		return context;
	}
	
	/**
	 * Whether this request was resolved by the last resolution retry
	 */
	private boolean isResolved() {
		return resolution != null;
	}
	
	/**
	 * Block the current thread until a component satisfying the request is available.
	 * 
	 * Resolution must be retried by another thread, and when successful it will notify
	 * this object to unblock the waiting thread.
	 * 
	 */
	public void block() {
		synchronized (this) {
			try {
				/*
				 * wait for resolution
				 */
				
				isBlocked = true;
				while (!isResolved())
					this.wait();
				
				isBlocked = false;
				
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * The result of the resolution
	 */
	public synchronized Resolved<?> getResolution() {
		return resolution;
	}

	/**
	 * Tries to resolve the request and wakes up the blocked thread
	 */
	public void resolve() {

		/*
		 * avoid multiple concurrent resolutions
		 */
		synchronized (this) {
			if (isResolving)
				return;
		}

		/*
		 * try to resolve.
		 * 
		 * IMPORTANT resolution is performed outside synchronization, as it may
		 * block in case of deployment. Notice also that the result is temporary
		 * confined to the stack before notifying pending threads.
		 * 
		 */
		
		Resolved<?> resolverResult = null;
		
		try {
			beginResolve();
			resolverResult = resolver.resolveLink(source, relation);
		} finally {
			endResolve(resolverResult);
		}
	}

	private static ThreadLocal<PendingRequest> current = new ThreadLocal<PendingRequest>();

	private synchronized void beginResolve() {
		current.set(this);
		isResolving = true;
	}
	
	private synchronized void endResolve(Resolved<?> resolverResult) {
		isResolving	= false;
		resolution	= resolverResult;
		current.set(null);

		this.notifyAll();
	}
	
	/**
	 * Whether the current thread is performing a resolution retry
	 */
	public static boolean isRetry() {
		return current() != null;
	}
	
	/**
	 * The request that is being resolved by the current thread
	 */
	public static PendingRequest current() {
		return current.get();
	}
		
	/**
	 * Decides whether the specified component could potentially resolve this request.
	 * 
	 * This is used as a hint to avoid unnecessarily retrying a resolution that is not
	 * concerned with an event.
	 * 
	 * TODO Currently we avoid forcing a resolution that will instantiate an implementation
	 * we should specify the expected behavior.
	 */
	public boolean isSatisfiedBy(Component candidate) {

		/*
		 * Check visibility
		 */
		if (!source.canSee(candidate))
			return false;
		
		/*
		 * Check if the candidate kind matches the target kind of the relation. Consider the special
		 * case for instantiable implementations that can satisfy an instance relation
		 */
		boolean matchKind = relation.getTargetKind().equals(candidate.getKind());
				  /*    || (relation.getTargetKind().equals(ComponentKind.INSTANCE) && candidate.getKind().equals(ComponentKind.IMPLEMENTATION)); */
		
		if (!matchKind)
			return false;
		
		/*
		 * Check if the candidate matches the target of the relation
		 */
		boolean matchTarget = false;
		
		if (relation.getTarget() instanceof ComponentReference<?>) {
			Component target = CST.componentBroker.getComponent(relation.getTarget().getName());
			matchTarget = (target != null) && (target.isAncestorOf(candidate) || target.equals(candidate));
		}
		
		if (relation.getTarget() instanceof ResourceReference)
			matchTarget = candidate.getProvidedResources().contains(relation.getTarget());

		if (! matchTarget)
			return false;
		
		/*
		 * Special validations for target instances
		 */
		if (relation.getTargetKind().equals(ComponentKind.INSTANCE)) {
			
			boolean valid = ( (candidate instanceof Instance) && ((Instance) candidate).isSharable());
					/* ||	( (candidate instanceof Implementation) && ((Implementation) candidate).isInstantiable()); */
				
			if (!valid)
				return false;
				
		}
		
		/*
		 * If this request has blocked the creating thread we should retry the resolution to unblock it.
		 * 
		 * Otherwise we verify if this request has not been already resolved by this candidate ( possibly
		 * in another thread) to avoid unnecessary resolves.
		 */
		
		synchronized (this) {
			if (this.isBlocked)
				return true;
		}
		
				
		Set<Link> resolutions = ((ComponentImpl)source).getExistingLinks(relation.getIdentifier());
		
		/*
		 * For single-valued relations we just verify there is some resolution
		 */
		if (! relation.isMultiple() )
			return resolutions.isEmpty();
		
		/*
		 * For multi-valued relations we check if the candidate is already a resolution
		 */
		
		/*
		boolean instantiatedCandidate = (relation.getTargetKind().equals(ComponentKind.INSTANCE) &&
										candidate.getKind().equals(ComponentKind.IMPLEMENTATION));
		*/							
		for (Link resolution : resolutions) {
			
			/*
			if (instantiatedCandidate && (resolution.getDestination() instanceof Instance) && 
				((Instance)resolution.getDestination()).getImpl().equals(candidate) )
				return false;
			*/
			if (resolution.getDestination().getKind().equals(candidate))
				return false;
			
		}
		
		return true;
	}


}
