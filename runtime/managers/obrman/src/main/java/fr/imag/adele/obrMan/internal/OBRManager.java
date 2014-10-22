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

package fr.imag.adele.obrMan.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DeploymentManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;

/**
 * This manager handles OBR request for a specific composite.
 * 
 * Resolution in this context is based on repositories specified in the OBR manager
 * model associated with the context in APAM
 * 
 * 
 * @author vega
 *
 */
public class OBRManager {

	/**
	 * The context this manager handles
	 */
	private final CompositeType context;

	/**
	 * The global obr manager
	 */
	private final OBRMan obrMan;

	/**
	 * The model associated with this composite
	 */
	private final Model model;


	public OBRManager(OBRMan obrman, CompositeType context, Model model) {
		this.obrMan 	= obrman;
		this.context	= context;
		this.model		= model;
	}
	
	public Model getModel() {
		return model;
	}

	public String getName() {
		return context.getName();
	}

	public Resolved<?> resolve(RelToResolve relation) {
		
		/*
		 * Try to optimize the case of finding a component by name, without constraints.
		 * 
		 */
		ComponentReference<?> reference = relation.getTarget().as(ComponentReference.class);
		if (reference != null && reference.getKind() == relation.getTargetKind()) {
			DeployableComponent component = getComponent(reference);
			return component != null ? new Resolved<Component>(component.install(this)) : null;
		}
		
		/*
		 * This is the case of relation resolution, based on searching for components providing
		 * the specified resources
		 */
		
		Set<DeployableComponent> candidates = getProviders(relation);

		if (candidates.isEmpty())
			return null;
		
		/*
		 * When the target kind is INSTANCE we must consider a special case, as the calculated candidate
		 * providers are implementations.
		 * 
		 * We must check if there are deployable instances of these implementations declared in the 
		 * repository, as these are the real candidates.
		 * 
		 */
		boolean instantiableCandidates = (relation.getTargetKind() == ComponentKind.INSTANCE);
		
		if (instantiableCandidates) {
			
			/*
			 * Select a single revision of each implementation
			 * 
			 * TODO Because the revision is based uniquely based on the version number, it is not possible to use preferences
			 * to select the preferred version. A better alternative would be to use preferences as a ranking, and use version
			 * number to break ties.
			 */
			selectRevisions(candidates);

			StringBuffer providerInstancesQuery = new StringBuffer();
			
			providerInstancesQuery.append("(|");
			for (DeployableComponent candidate : candidates) {
				if (candidate.getReference().getKind() == ComponentKind.IMPLEMENTATION)
					providerInstancesQuery.append("(").append(CST.IMPLNAME).append("=").append(candidate.getReference().getName()).append(")");
			}
			providerInstancesQuery.append(")");
			
			ApamFilter query = ApamFilter.newInstance(providerInstancesQuery.toString());
			
			/*
			 * TODO We can not be sure that the candidate instance corresponds to the selected revision of the implementation. We
			 * are just assuming that instances can be created without considering versions, this may not be true if the implementation
			 * declaration has changed in a not backward compatible way 
			 */
			Set<DeployableComponent> providerInstances = new HashSet<DeployableComponent>();
			for (DeployableComponent component : obrMan.getComponents(this)) {
				if (component.getReference().getKind() == ComponentKind.INSTANCE && 
					component.satisfies(query) && component.satisfies(relation))
					providerInstances.add(component);
			}
			
			if (! providerInstances.isEmpty()) {
				candidates = providerInstances;
				instantiableCandidates = false;
			}
				
		}

		/*
		 * Select a single revision of each candidate
		 * 
		 * TODO Because the revision is based uniquely based on the version number, it is not possible to use preferences
		 * to select the preferred version. A better alternative would be to use preferences as a ranking, and use version
		 * number to break ties.
		 */
		selectRevisions(candidates);

		/*
		 * Select best candidate according to preferences
		 * 
		 * TODO currently if the target kind is INSTANCE and there are both implementation and instance preferences, only one
		 * set is evaluated. A better alternative would be to use both criteria for ranking.
		 */
		if (relation.hasPreferences() && !relation.isMultiple()) {
			
			int bestRanking 			= 0;
			DeployableComponent best 	= null;
			
			for (DeployableComponent candidate : candidates) {
				
				int ranking = candidate.ranking(relation); 
				if (ranking >= bestRanking) {
					best		= candidate;
					bestRanking = ranking;
				}
			}
			
			candidates = Collections.singleton(best);
		}
		
		/*
		 * Deploy and install candidates
		 * 
		 * TODO currently we only install a single candidate, irrespective of the multiplicity of the relationship.
		 * This is a trade-off between comprehensiveness and laziness (in the worst case scenario, when there is no
		 * constraints, we would install all possible providers) we should have an explicit strategy.
		 */
		
		Component selected = candidates.iterator().next().install(this);
		
		return !instantiableCandidates ? new Resolved<Component>(selected) : new Resolved<Implementation>((Implementation)selected,true);
	}

	/**
	 * Look for a specific component in the component repository.
	 * 
	 * Notice that a component is searched by the specified name and kind of the reference. However,
	 * several versions may exist , in this case we choose the latest version.
	 * 
	 * TODO Should we consider preferences when selecting the best revision ? A possible implementation
	 * would be to order according to preferences, and use version number to break ties.
	 * 
	 */
	private DeployableComponent getComponent(ComponentReference<?> searched) {

		boolean ignoreKind			= searched.getKind() == ComponentKind.COMPONENT;
		DeployableComponent result	= null;
			
		for (DeployableComponent component : obrMan.getComponents(this)) {
			if	( (component.getReference().equals(searched)) && 
				  (ignoreKind || component.getReference().getKind() == searched.getKind()) ) {
				
				result = (result == null || component.isPreferedVersionThan(result)) ? component : result;
			}
		}
		
		return result;
			
	}

	/**
	 * Look for components that provide the resource specified in the relation
	 * 
	 */
	private Set<DeployableComponent> getProviders(RelToResolve relation) {

		
		/*
		 * Depending on the kind requested resource, we calculate the filter that must be satisfied
		 * by the providing components.
		 * 
		 * For java resources and specifications the metadata contains all the information to find the
		 * provider components. For specific component references we search by name. 
		 */
		String requirementQuery = null;
		
		ResourceReference requestedResource = relation.getTarget().as(ResourceReference.class);
		if (requestedResource != null) {
			
			if (requestedResource instanceof InterfaceReference) {
				requirementQuery = "("+CST.PROVIDE_INTERFACES+"*>" + requestedResource.getJavaType() + ")";
			}
			
			else if (requestedResource instanceof MessageReference) {
				requirementQuery = "("+CST.PROVIDE_MESSAGES+"*>" + requestedResource.getJavaType() + ")";
			}
			
		}
		
		ComponentReference<?> requestedComponent = relation.getTarget().as(ComponentReference.class);
		if (requestedComponent != null) {
			
			if (requestedComponent.getKind() == ComponentKind.SPECIFICATION) {
				requirementQuery = "("+CST.PROVIDE_SPECIFICATION+"*>" + requestedComponent.getName() + ")";
			}

			else if (requestedComponent.getKind() == ComponentKind.IMPLEMENTATION) {
				requirementQuery = "("+CST.IMPLNAME+ "=" + requestedComponent.getName() + ")";
			}

			else if (requestedComponent.getKind() == ComponentKind.COMPONENT) {
				requirementQuery ="("+CST.NAME+ "=" + requestedComponent.getName() + ")";
			}

		}
		
		if (requirementQuery == null)
			return Collections.emptySet();

		
		/*
		 * We need to do an special case when the target kind is INSTANCE, as the metadata of instances doesn't contain
		 * the provided resources, so we search for corresponding implementations.
		 * 
		 * The caller should decide if it look for instances in the repository or instantiate the found implementations.
		 */
		ComponentKind searchedKind 		= relation.getTargetKind();
		if (searchedKind == ComponentKind.INSTANCE)
			searchedKind = ComponentKind.IMPLEMENTATION;
		
		ApamFilter requirement = ApamFilter.newInstance(requirementQuery);
		
		Set<DeployableComponent> candidates = new HashSet<DeployableComponent>();
		for (DeployableComponent component : obrMan.getComponents(this)) {
			
			/*
			 * Ignore all components that do not provide the required resource
			 */
			if (component.getReference().getKind() != searchedKind || ! component.satisfies(requirement))
				continue;
			
			/*
			 * Evaluate constraints specified in the relationship
			 */
			if (component.satisfies(relation))
				candidates.add(component);
			
		}
		
		/*
		 * keep a single version of each candidate
		 */
		return candidates;

	}
	

	/**
	 * Verify if this manager deployed the specified component, and return a newer version that can be used 
	 * to update it.
	 * 
	 */
	public DeploymentManager.Unit getDeploymentUnit(Component deployed) {

		DeployableComponent result	= null;
		
		for (DeployableComponent component : obrMan.getComponents(this)) {
			if	(component.isRepositoryVersionOf(deployed)) {
				result = (result == null || component.isPreferedVersionThan(result)) ? component : result;
			}
		}
		
		return result != null ? new DeploymentUnit(this,result,deployed) : null;

	}

	/**
	 * This represents a component that has been deployed in the system by obrman, and that can be
	 * updated from the repository
	 */
	private static class DeploymentUnit implements DeploymentManager.Unit {

		private final Component 			component;
		private final DeployableComponent	repositoryVersion;
		private final OBRManager 			context;
		
		public DeploymentUnit(OBRManager context, DeployableComponent repositoryVersion, Component component) {

			this.context			= context;
			this.component			= component;
			this.repositoryVersion	= repositoryVersion;
		}
		
		
		@Override
		public Set<String> getComponents() {
			return repositoryVersion.getDeploymentUnitComponents();
		}

		@Override
		public void update() throws Exception {
			repositoryVersion.update(context,component);
			
		}
		
	}
	
	
	/**
	 *  Utility method to keep a single revision of each component in the given set.
	 * 
	 */
	private static void selectRevisions(Set<DeployableComponent> components) {
		
		Map<ComponentReference<?>,DeployableComponent> retained = new HashMap<ComponentReference<?>,DeployableComponent>();
		
		for (DeployableComponent component : components) {
			
			DeployableComponent best = retained.get(component.getReference());
			
			if (best == null || component.isPreferedVersionThan(best)) 
				retained.put(component.getReference(),component);
		}
		
		components.retainAll(retained.values());
	}
}
