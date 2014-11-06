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
package fr.imag.adele.apam.declarations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;

/**
 * This class represents the declaration of a composite implementation
 * 
 * @author vega
 * 
 */
public class CompositeDeclaration extends ImplementationDeclaration {

	/**
	 * A reference to a composite implementation
	 */
	private static class Reference extends ImplementationReference<CompositeDeclaration> {

		public Reference(String name) {
			super(name);
		}

	}

	/**
	 * The main implementation of the composite
	 */
	private final ComponentReference<?> mainComponent;

	/**
	 * The property that represents the state of the component
	 */
	private PropertyDefinition.Reference stateProperty;
	/**
	 * The visibility policies
	 */
	private final VisibilityDeclaration visibility;

	/**
	 * The list of owned components
	 */
	private final Set<OwnedComponentDeclaration> ownedComponents;

	/**
	 * The list of declared instances in this composite
	 */
	private final List<InstanceDeclaration> instances;

	/**
	 * The list of contextual relation overrides of this composite
	 */
	private final Set<RelationDeclaration> contextualDependencies;

	/**
	 * The list of contextual relation overrides of this composite
	 */
	private final Set<RelationDeclaration> contextualOverrides;


	/**
	 * The list of dependencies promotions of this composite
	 */
	private final List<RelationPromotion> promotions;

	public CompositeDeclaration(String name, Versioned<SpecificationDeclaration> specification, ComponentReference<?> mainComponent) {
		super(name, specification);

		this.mainComponent 		= mainComponent;
		
		this.visibility 		= new VisibilityDeclaration();
		
		this.ownedComponents 	= new HashSet<OwnedComponentDeclaration>();
		this.instances 			= new ArrayList<InstanceDeclaration>();
		
		this.contextualDependencies = new HashSet<RelationDeclaration>();
		this.contextualOverrides 	= new HashSet<RelationDeclaration>();
		
		this.promotions 		= new ArrayList<RelationPromotion>();

	}

	/**
	 * Clone this declaration
	 */
	protected CompositeDeclaration(CompositeDeclaration original) {
		super(original);

		this.mainComponent 		= original.mainComponent;

		this.visibility 		= new VisibilityDeclaration(original.visibility);
		
		this.ownedComponents 	= new HashSet<OwnedComponentDeclaration>();
		for (OwnedComponentDeclaration ownedDeclaration : original.ownedComponents) {
			this.ownedComponents.add(new OwnedComponentDeclaration(ownedDeclaration));
		}
		
		this.instances = new ArrayList<InstanceDeclaration>();
		for (InstanceDeclaration instance : original.instances) {
			this.instances.add(new InstanceDeclaration(instance));
		}
		
		this.contextualDependencies = new HashSet<RelationDeclaration>();
		for (RelationDeclaration relation : original.contextualDependencies) {
			this.contextualDependencies.add(new RelationDeclaration(relation));
		}
		
		this.contextualOverrides = new HashSet<RelationDeclaration>();
		for (RelationDeclaration override : original.contextualOverrides) {
			this.contextualOverrides.add(new RelationDeclaration(override));
		}
		
		this.promotions = new ArrayList<RelationPromotion>(original.promotions);
	}

	/**
	 * Generates the reference to this implementation
	 */
	@Override
	protected ImplementationReference<CompositeDeclaration> generateReference() {
		return new Reference(getName());
	}

	/**
	 * Override the return type to a most specific class in order to avoid
	 * unchecked casting when used
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ImplementationReference<CompositeDeclaration> getReference() {
		return (ImplementationReference<CompositeDeclaration>) super.getReference();
	}
	
	/**
	 * The list of contextual dependencies, these declarations either refine a matching
	 * declaration or are added to components inside this composite.
	 * 
	 * see {@link RelationDeclaration#refinedBy(RelationDeclaration)} for a description of
	 * the allowed refinements
	 */
	public Set<RelationDeclaration> getContextualDependencies() {
		return contextualDependencies;
	}

	/**
	 * The list of override dependencies, these declaration override matching declarations for
	 * components inside this composite
	 * 
	 * see {@link RelationDeclaration#overriddenBy(RelationDeclaration)} for a description of
	 * the allowed overrides
	 */
	public Set<RelationDeclaration> getOverridenDependencies() {
		return contextualOverrides;
	}

	/**
	 * Get the list of declared instances
	 */
	public List<InstanceDeclaration> getInstanceDeclarations() {
		return instances;
	}

	/**
	 * Get the main implementation
	 */
	public ComponentReference<?> getMainComponent() {
		return mainComponent;
	}

	/**
	 * Get the list of owned appearing components
	 */
	public Set<OwnedComponentDeclaration> getOwnedComponents() {
		return ownedComponents;
	}

	/**
	 * The list of contextual promotions
	 */
	public List<RelationPromotion> getPromotions() {
		return promotions;
	}


	/**
	 * The property that specifies the state of the composite
	 */
	public PropertyDefinition.Reference getStateProperty() {
		return stateProperty;

	}

	/**
	 * The visibility rules of the composite
	 */
	public VisibilityDeclaration getVisibility() {
		return visibility;
	}

	/**
	 * Sets the state property
	 */
	public void setStateProperty(PropertyDefinition.Reference stateProperty) {
		this.stateProperty = stateProperty;
	}

	@Override
	public String toString() {
		String ret = "\nComposite declaration " + super.toString();
		ret += "\n   Main Implementation: " + (mainComponent != null ? mainComponent.getName() : "");
		return ret;
	}

}
