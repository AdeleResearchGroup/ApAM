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

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.util.Visible;

/**
 * This class is used to represent the pending requests that are waiting for resolution.
 * 
 * 
 */
public class PendingRequest {

	/**
	 * The source of the dependency
	 */
	protected final Instance source;
	
	/**
	 * The dependency to resolve
	 */
	protected final Relation relation;

	
	/**
	 * The resolver
	 */
	protected final ApamResolverImpl resolver;
	
	/**
	 * The result of the resolution
	 */
	private Resolved<?> resolution;

	/**
	 * Builds a new pending request reification
	 */
	protected PendingRequest(ApamResolverImpl resolver, Instance source, Relation relation) {
		this.resolver		= resolver;
		this.source			= source;
		this.relation		= relation;
		this.resolution		= null;
	}
	
	public Instance getSource() {
		return source;
	}
	
	/**
	 * The dependency that needs resolution
	 */
	public Relation getRelation() {
		return relation;
	}
	
	/**
	 * The context in which the resolution is requested
	 */
	public Composite getContext() {
		// ??? 
		return source.getComposite();
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
				while (!isResolved())
					this.wait();
				
				
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
		if (!isResolved())
			return;

		/*
		 * try to resolve
		 */
		synchronized (this) {
			try {
				beginResolve();
				resolution = resolver.resolveLink(source, relation);
				this.notifyAll();
			} finally {
				endResolve();
			}
		}
	}

	
	private static ThreadLocal<PendingRequest> current = new ThreadLocal<PendingRequest>();

	private void beginResolve() {
		current.set(this);
	}
	
	private void endResolve() {
		current.set(null);
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
	 */
	public boolean isSatisfiedBy(Component candidate) {
	
		if (! source.canSee(candidate))
			return false;
		
		Implementation implementation = null;
		
		if (candidate instanceof Implementation)
			implementation = (Implementation) candidate;
		
		if (candidate instanceof Instance)
			implementation	= ((Instance) candidate).getImpl();
		
		if (implementation == null)
			return false;
		
		/*
		 * Validate the implementation matches the requested specification
		 */
		boolean valid = false;
		
		if (relation.getTarget() instanceof ImplementationReference<?>)
			valid = implementation.getDeclaration().getReference().equals(relation.getTarget());

		if (relation.getTarget() instanceof SpecificationReference)
			valid = implementation.getSpec().getDeclaration().getReference().equals(relation.getTarget());

		if (relation.getTarget() instanceof ResourceReference)
			valid = implementation.getProvidedResources().contains(relation.getTarget());
		
		return valid;
		
	}


}
