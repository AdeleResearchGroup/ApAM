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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;

/**
 * This is the base class that is used to represent the pending requests that need to
 * be resolved.
 * 
 * There are two cases :
 * 
 * 1) Blocked requests, in which a thread is waiting for resolution to happen
 * 2) Dynamic requests, in which resolution is performed asynchronously 
 * 
 * Most of the code is common for both cases, however blocked request are short-lived
 * objects that exist only for the duration of the unsatisfied condition, while dynamic
 * request are long-standing objects that must be asynchronously updated.
 * 
 * TODO perhaps we should have different classes to represent these two kinds of requests
 * and move the common code into an abstract superclass.
 * 
 */
public class PendingRequest extends Apform2Apam.PendingThread {

	/**
	 * The source of the relation
	 */
	protected final Component source;

	/**
	 * The relation to resolve
	 */
	protected final RelationDefinition relation;

	/**
	 * The composite in which context the resolution will be performed
	 */
	protected final Composite context;

	/**
	 * The resolver
	 */
	protected final ApamResolver resolver;

	/**
	 * The result of the resolution
	 */
	private Resolved<?> resolution;

	/**
	 * Whether this request has been disposed, this happen for instance when the
	 * source component is removed
	 */
	private boolean isDisposed = false;
	
	/**
	 * Whether the thread that created this request is blocked waiting for resolution
	 */
	private boolean isBlocked = false;

	/**
	 * The stack of the blocked requests
	 */
	private List<StackTraceElement> stack;
	

	/**
	 * Builds a new pending request reification
	 */
	public PendingRequest(ApamResolver resolver, Component source, RelationDefinition relDef) {
		
		super(source.toString());
		
		this.resolver = resolver;

		this.source = source;
		this.context = (source instanceof Instance) ? ((Instance) source).getComposite() : CompositeImpl.getRootAllComposites();
		this.relation = relDef;

		this.resolution = null;
	}

	
	@Override
	public String getCondition() {
		return "resolution of "+relation.toString();
	}

	/**
	 * The context in which the resolution is requested
	 */
	public Composite getContext() {
		return context;
	}

	/**
	 * The source of the relation
	 */
	public Component getSource() {
		return source;
	}

	/**
	 * The relation that needs resolution
	 */
	public RelationDefinition getRelation() {
		return relation;
	}

	/**
	 * Pending requests may be generated for exactly the same source and target in different threads, so
	 * we force reference equality.
	 * 
	 * TODO For dynamic request it makes sense to have semantic equality based on the source and the relation,
	 * one more reason to consider splitting this class 
	 */
	@Override
	public final boolean equals(Object object) {
		return this == object;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}
	
	/**
	 * The result of the last resolution of this request
	 */
	public synchronized Resolved<?> getResolution() {
		return resolution;
	}

	/**
	 * Whether this dynamic request is already being resolved by the another thread
	 */
	private boolean isResolving = false;

	/**
	 * Resolves the request when context changes may have satisfied the request. 
	 */
	public void resolve() {
		
		/*
		 * If a thread is blocked waiting for resolution of this request, we simply notify it to let it retry 
		 * resolution if needed.
		 * 
		 * TODO There is an important invariant that a blocked request is owned by a single thread. This is no clear
		 * in this class, as it is also used for dynamic requests that are "pending" to be recalculated, but that are
		 * not blocking a thread. We should consider separating the two different classes to represent the different
		 * concepts.
		 */
		synchronized (this) {
			if (this.isBlocked) {
				this.notifyAll();
				return;
			}
		}


		/*
		 * Otherwise, this is dynamic request an we resolve it in the context of the current thread. If the request is
		 * already being resolved by another thread, we wait to avoid concurrent invocations of the resolver.
		 * 
		 * 
		 * NOTE is the responsibility of the caller to invoke this method in the appropriate thread to implement the
		 * intended dynamic update policy. Notice that this method may block or throw exceptions (as a side effect of
		 * resolution) that may impact the calling thread.
		 */
		
		synchronized (this) {
			try {
				while (this.isResolving && ! this.isDisposed) {
					this.wait();
				}
			} catch (InterruptedException e) {
			}
			
			this.isResolving = true;
		}

		
		
		/*
		 * IMPORTANT NOTE Notice that we must invoke the resolution outside any synchronized block to avoid
		 * potential deadlocks
		 */
		
		Resolved<?> result = null;
		
		try {
			 result= resolver.resolveLink(source, relation);
		}
		catch (Exception resolutionException) {
			/*
			 * TODO currently if the relation is marked as dynamic/eager and fail exception, we simply ignore
			 * all exceptions if they are thrown in asynchronous resolutions. We need to better specify the
			 * expected behavior.
			 */
		}
		
		/*
		 * register the result of the resolution
		 */
		synchronized (this) {
			this.resolution		= result;
			this.isResolving	= false;
			
			this.notifyAll();
		}

	}

	/**
	 * Block the current thread until a component satisfying the request is available.
	 * 
	 */
	public void block() {
		
		/*
		 * If it is a retry of this blocked request, we do not block again.
		 * 
		 * IMPORTANT This can only happen in case of a unsuccessful retry. In that case we simply 
		 * return and the failure handler will simply ignore the result. 
		 * 
		 * The stack should look like this :
		 * 
		 *     +--------------------------+
		 *     + this.block()             + <- Unsuccessful retry
		 *     +--------------------------+
		 *     + FailureManager.resolve() +
		 *     +--------------------------+
		 *     + Resolver.resolveLink()   +
		 *     +--------------------------+
		 *     + this.retry()             +
		 *     +--------------------------+
		 *     + this.block()             + <- Blocked request
		 *     +--------------------------+
		 *     + FailureManager.resolve() +
		 *     +--------------------------+
		 *     
		 *     
		 * Control will go back to the previous invocation of this method in the stack, that will
		 * block again until a new event signal the need for another retry.
		 * 
		 */
		if (this.isRetry()) {
			return;
		}
		
		/*
		 * Mark this request as blocked
		 */
		synchronized (this) {
			isBlocked = true;
			stack = getCurrentStack();
		}

		/*
		 * wait for some event that signals a change in the environment that may unblock this resolution,
		 * and try to resolve again
		 */

		while (!isResolved()) {

			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			} 
			
			retry();
		}

		/*
		 * Mark the request as resolved
		 */
		synchronized (this) {
			isBlocked = false;
			stack = null;
		}
				
	}
	
	
	/**
	 * Whether this request was resolved by the last resolution retry
	 */
	private synchronized boolean isResolved() {
		return resolution != null || isDisposed;
	}
	
	/**
	 * The stack trace for blocked requests
	 */
	public List<StackTraceElement> getStack() {
		return stack;
	}
	
	public synchronized void dispose() {
		isDisposed = true;
		this.notifyAll();
	}
	
	/**
	 * Decides whether the specified component could potentially resolve this request.
	 * 
	 * This is used as a hint to avoid unnecessarily retrying a resolution that is not concerned with an
	 * event.
	 * 
	 * TODO Currently we avoid forcing a resolution in cases where instantiating an implementation could
	 * satisfy the pending request, we should better specify the expected behavior.
	 */
	public boolean isSatisfiedBy(Component candidate) {

		/*
		 * Check if the candidate kind matches the target kind of the relation.
		 * 
		 * TODO Consider the special case for instantiable implementations that can satisfy an instance
		 * relation
		 */
		boolean matchKind = relation.getTargetKind().equals(candidate.getKind());

		if (!matchKind) {
			return false;
		}

		/*
		 * Check if the candidate matches the target of the relation
		 */
		boolean matchTarget = false;

		if (relation.getTarget() instanceof ComponentReference<?>) {
			Component target = CST.componentBroker.getComponent(relation.getTarget().getName());
			matchTarget = (target != null) && (target.isAncestorOf(candidate) || target.equals(candidate));
		}

		if (relation.getTarget() instanceof ResourceReference) {
			matchTarget = candidate.getProvidedResources().contains(relation.getTarget());
		}

		if (!matchTarget) {
			return false;
		}

		/*
		 * Check visibility, including potential promotions
		 */
		if (source instanceof Instance) {

			boolean promotion = false;
			for (RelationDefinition compoDep : ((Instance) source).getComposite().getCompType().getLocalRelations()) {
				if (relation.matchRelation((Instance) source, compoDep)) {
					promotion = true;
				}
			}

			if (!promotion && !source.canSee(candidate))
				return false;

		} else if (!source.canSee(candidate)) {
			return false;
		}

		/*
		 * Special validations for target instances
		 */
		if (relation.getTargetKind().equals(ComponentKind.INSTANCE)) {

			boolean valid = ((candidate instanceof Instance) && ((Instance) candidate).isSharable());
			if (!valid) {
				return false;
			}

		}

		/*
		 * If this request has blocked the creating thread we should retry the
		 * resolution to unblock it.
		 * 
		 */

		synchronized (this) {
			if (this.isBlocked) {
				return true;
			}
		}

		/*
		 * Otherwise it is an asynchronous dynamic request and we verify if this request has not been
		 * already resolved (possibly in another thread) to avoid invoking the resolver unnecessarily. 
		 */
		
		Set<Link> resolutions = ((ComponentImpl) source).getExistingLinks(relation.getName());

		/*
		 * For single-valued relations we just verify there is some resolution
		 */
		if (!relation.isMultiple()) {
			return resolutions.isEmpty();
		}

		/*
		 * For multi-valued relations we check if the candidate has already been added
		 */

		for (Link resolution : resolutions) {
			if (resolution.getDestination().equals(candidate)) {
				return false;
			}
		}

		return true;
	}
	

	/**
	 * The request that is blocked in the current thread.
	 * 
	 * Notice that several request may be be blocked in the same thread, nested in the stack, so this
	 * variable only references the one at the top of the stack.
	 */
	private static ThreadLocal<PendingRequest> blockedRequest = new ThreadLocal<PendingRequest>();

	/**
	 * Whether the current thread is performing a reevaluation for this request.
	 * 
	 * NOTE notice that in this case we use semantic equality between requests, as the resolver is not
	 * aware that is retrying a previously blocked request it will create a new request.
	 * 
	 * TODO this should be better handled by the failure resolution manager 
	 */
	private boolean isRetry() {
		
		PendingRequest current = blockedRequest.get();
		
		return	current != null &&
				current.relation.getName().equals(this.relation.getName()) &&
				current.source.equals(this.source) &&
				current.context.equals(this.context);
	}
	
	/**
	 * Invoke the resolver again for this request. 
	 * 
	 * IMPORTANT Notice that this method is only used for blocked requests, and can only  be invoked in the
	 * context of the blocked thread owning this request.
	 * 
	 * TODO There is an important invariant that a blocked request is owned by a single thread. This is no clear
	 * in this class, as it is also used for dynamic requests that are "pending" to be recalculated, but that are
	 * not blocking a thread. We should consider separating the two different classes to represent the different
	 * concepts.
	 * 
	 * Notice that we catch all exceptions that the resolution may be thrown as a side effect, and we simply
	 * consider that the reevaluation did not succeed. 
	 * 
	 * Notice also that resolution is performed without synchronization on this request object, and all context
	 * is kept confined to the stack or thread local fields. This is because resolution may block as a side 
	 * effect and event processing concerning this request must be able to proceed, simply signaling that a new
	 * retry must be tempted, if the current one is unsuccessful.
	 */
	private void retry() {
		
		PendingRequest previous 	= beginResolution();
		Resolved<?> resolverResult 	= null;
		try {
			resolverResult = resolver.resolveLink(source, relation);
		} 
		catch (Exception ignoredFailure) {
		}
		finally {
			endResolution(previous,resolverResult);
		}
	}

	private synchronized PendingRequest beginResolution() {
		
		PendingRequest previous = blockedRequest.get();
		blockedRequest.set(this);
		resolution 		= null;
		
		return previous;
	}

	private synchronized void endResolution(PendingRequest previous, Resolved<?> resolverResult) {
		blockedRequest.set(previous);
		resolution 		= resolverResult;
	}
	
	/**
	 * The stack of the request executing in the context of the current thread.
	 * 
	 */
	private static List<StackTraceElement> getCurrentStack() {

		List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(new Throwable().getStackTrace()));

		/*
		 * Remove all internal APAM implementation frameworks from the top of the stack, to increase 
		 * the readability of the stack trace
		 */
		Iterator<StackTraceElement> frames = stack.iterator();
		while (frames.hasNext()) {
			if (frames.next().getClassName().startsWith(PendingRequest.class.getPackage().getName())) {
				frames.remove();
				continue;
			}

			break;
		}
		return stack;
	}
	
}
