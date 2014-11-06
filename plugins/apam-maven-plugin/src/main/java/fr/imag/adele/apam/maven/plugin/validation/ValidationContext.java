package fr.imag.adele.apam.maven.plugin.validation;

import java.util.ArrayList;
import java.util.List;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.repository.ComponentIndex;
import fr.imag.adele.apam.declarations.repository.Repository;

/**
 * This class keeps a set of components referenced during the validation. 
 * 
 * Unlike a normal component repository, components in the context are effective (all group's features
 * are automatically inherited) for avoiding navigation up the group hierarchy.
 * 
 * The set of loaded components is held in a cache, so that we consistently return the same declaration,
 * specially when the version ranges are not precise.
 *  
 * @author vega
 *
 */
public class ValidationContext implements Repository {

   /**
    * The repository chain to lookup for components
    */
	private final Repository repository;
	
	
	/**
	 * The index of loaded original (without inheritance) components
	 */
	private final ComponentIndex originalCache;

	/**
	 * The index of loaded effective (with inheritance) components
	 */
	private final ComponentIndex effectiveCache;
	
	/**
	 * The list of loaded declarations (both original and effective)
	 * 
	 * NOTE this list keeps strong references to the loaded declarations, so that
	 * the cached indexes are never garbage collected
	 */
	private List<ComponentDeclaration> loaded;

	
	public ValidationContext(Repository repository) {
		
		this.repository		= repository;
		
		this.loaded			= new ArrayList<ComponentDeclaration>();
		this.originalCache	= new ComponentIndex();
		this.effectiveCache	= new ComponentIndex();
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference) {
		return getComponent(reference,false);
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> reference) {
		return getComponent(reference,false);
	}
	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one.
	 * 
	 * Optionally, it is possible to request an effective declaration, that inherit all features from its group
	 */
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference, boolean effective) {
		return reference != null ? getComponent(Versioned.any(reference),effective) : null;
	}
	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one among the specified range.
	 * 
	 * Optionally, it is possible to request an effective declaration, that inherit all features from its group
	 */
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> reference, boolean effective) {
		
		if (reference == null)
			return null;
		
		/*
		 * First check the loaded components
		 */
		C cached = effective ? effectiveCache.getComponent(reference) : originalCache.getComponent(reference);
		if (cached != null)
			return cached;
		
		/*
		 * Otherwise we try to load the component from repository
		 * 
		 */
		C component = repository.getComponent(reference);
		if (component == null)
			return null;

		/*
		 * update cache
		 */
		originalCache.put(component);
		loaded.add(component);

		
		/*
		 *  Load the parent group, this may fail if the group or one of its ancestors is not
		 *  in the repository
		 */
		ComponentDeclaration group = getComponent(component.getGroupVersioned(),true);
		if (effective && component.getGroup() != null && group == null)
			return null;
			
		/*
		 *  Compute the effective declaration.
		 */
		@SuppressWarnings("unchecked")
		C effectiveDeclaration =  (C) component.getEffectiveDeclaration(group);
		if (effective && effectiveDeclaration == null)
			return null;

		
		/*
		 * update cache
		 */
		effectiveCache.put(effectiveDeclaration);
		loaded.add(effectiveDeclaration);
		
		return effective ? effectiveDeclaration : component;
	}
	
	/**
	 * Determines if the candidate is an ancestor (or equals) to the component
	 * 
	 * TODO This method should be directly available in class {@link ComponentDeclaration}, this requires to migrate
	 * part of the validation and repository API directly to the declarations package.
	 */
	public boolean isAncestor(ComponentDeclaration component, ComponentReference<?> candidate, boolean orEquals) {
		ComponentDeclaration ancestor = orEquals ? component : getComponent(component.getGroupVersioned());
		while (ancestor != null) {
			
			if (ancestor.getReference().equals(candidate))
				return true;
			
			ancestor = getComponent(ancestor.getGroupVersioned());
		}
		
		return false;
	}

	/**
	 * Determines if a component is a candidate to satisfy a relation
	 * 
	 * TODO This method should be directly available in class {@link RelationDeclaration}, this requires to migrate
	 * part of the validation and repository API directly to the declarations package.
	 */
	protected boolean isCandidateTarget(RelationDeclaration relation, ComponentDeclaration candidate) {

		ComponentReference<?> targetComponent	= relation.getTarget().as(ComponentReference.class);
		ResourceReference targetResource 		= relation.getTarget().as(ResourceReference.class);

		if (targetComponent != null) {
			return isAncestor(candidate,targetComponent,true);
		}
		else if (targetResource != null) {
			return candidate.getProvidedResources(ResourceReference.class).contains(targetResource);
		}				
		else {
			return false;
		}
	}
	
	/**
	 * Reinitializes the context
	 */
	public void reset() {
		originalCache.clear();
		effectiveCache.clear();
		
		loaded.clear();
	}

}
