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

import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

/**
 * This class handles resolution for dynamic dependencies.
 * 
 * Dynamic dependencies are automatically resolved in a background thread, independently of access to injected
 * fields.
 * 
 * Currently this is done in following cases:
 * 
 * 1) For dependencies marked as multiple (in order to keep up to date automatically the list of targets)
 * 2) For dependencies that define pushed message consumer (and so never use injected fields)
 * 
 * @author vega
 *
 */
public class DynamicResolutionRequest {

	/**
	 * The APAM resolver
	 */
	private final ApamResolver resolver;

	/**
	 * The source of the resolution
	 */
	private final Instance source;
	
	/**
	 * The relation to resolve
	 */
	private final Relation relation;
	
    /**
     * whether this request is currently scheduled for resolution
     */
    private boolean isScheduled;
	
	public DynamicResolutionRequest(ApamResolver resolver, Instance source, Relation relation) {
		this.resolver		= resolver;
		this.source			= source;
		this.relation		= relation;
		this.isScheduled	= false;
	}
	
	/**
	 * The source of the relation
	 */
	public Instance getSource() {
		return source;
	}
	
	/**
	 * Test whether the specified instance is a possible candidate to resolve this request. This
	 * is just used as a hint to trigger a background resolution.
	 *
	 * 
	 * TODO Should we also trigger resolution in the case that a new instantiable implementation
	 * is available? this may create a lot of instances automatically.
	 */
	public boolean isSatisfiedBy(Instance instance) {

		Set<Component> dests = source.getLinkDests(relation.getIdentifier());

		/*
		 * If the candidate is already a result, ignore it
		 */
		if (dests.contains(instance))
			return false;

		/*
		 * If this is relation is already resolved, ignore any triggering event
		 */
		if (! relation.isMultiple() && ! dests.isEmpty())
			return false;

		
		/*
		 * verify the candidate instance is a valid target of the relation
		 */
		Implementation implementation	= instance.getImpl();
		boolean valid 					= false;
		
		if (relation.getTarget() instanceof ImplementationReference<?>)
			valid = implementation.getDeclaration().getReference().equals(relation.getTarget());

		if (relation.getTarget() instanceof SpecificationReference)
			valid = implementation.getSpec().getDeclaration().getReference().equals(relation.getTarget());

		if (relation.getTarget() instanceof ResourceReference)
			valid = implementation.getDeclaration().getProvidedResources().contains(relation.getTarget()) ||
					implementation.getSpec().getDeclaration().getProvidedResources().contains(relation.getTarget());
		
		return valid;

	}

	/**
	 * The dynamic request that is being resolved in the current thread, if any 
	 */
	static private ThreadLocal<DynamicResolutionRequest> current	= new ThreadLocal<DynamicResolutionRequest>();
	

    /**
	 * Perform a recalculation of this relation
	 */
    public synchronized void resolve() {
    	
    	/*
		 * Avoid performing several resolutions for the same relation in
		 * parallel. Usually this is not useful as the current resolution will
		 * find all solutions, but in some circumstances we may lost a
		 * triggering event.
		 */
    	if (isScheduled)
    		return;
    	
    	/*
		 * Invoke resolver to try to find a solution to the dynamic relation.
		 * 
		 * IMPORTANT Notice that resolution is performed in the context of the
		 * thread that triggered the recalculation event. If resolution fails,
		 * the resolver must simply ignore the failure, otherwise this will
		 * block or kill an unrelated thread. This is ensured by the dynamic
		 * manager.
		 * 
		 * We need to evaluate if it is safer to resolve dynamic dependencies in
		 * a background thread, but this may introduce some race conditions
		 */
		try {
			beginResolve();
			resolver.resolveLink(source, relation);
		}
		catch (Throwable ignoredError) {
		}
		finally {
			endResolve();
		}
    }

    /**
     * Start of resolution
     */
	private void beginResolve() {
   		isScheduled = true;	
		current.set(this);
	}
	
	/**
	 * End of resolution
	 */
	private void endResolve() {
		current.set(null);
		isScheduled = false;
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
	public static DynamicResolutionRequest current() {
		return current.get();
	}
	
}
