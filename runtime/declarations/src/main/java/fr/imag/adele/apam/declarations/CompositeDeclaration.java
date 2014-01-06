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
	private static class Reference extends
			ImplementationReference<CompositeDeclaration> {

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
	 * The list of contextual link declarations in this composite
	 */
	private final List<LinkDeclaration> contextualLinks;

	/**
	 * The list of dependencies promotions of this composite
	 */
	private final List<RelationPromotion> promotions;

	public CompositeDeclaration(String name,
			SpecificationReference specification,
			ComponentReference<?> mainComponent) {
		super(name, specification);

		this.mainComponent = mainComponent;

		this.visibility = new VisibilityDeclaration();
		this.ownedComponents = new HashSet<OwnedComponentDeclaration>();
		this.instances = new ArrayList<InstanceDeclaration>();
		this.contextualDependencies = new HashSet<RelationDeclaration>();
		this.contextualOverrides = new HashSet<RelationDeclaration>();
		this.contextualLinks = new ArrayList<LinkDeclaration>();
		this.promotions = new ArrayList<RelationPromotion>();

	}

	/**
	 * Generates the reference to this implementation
	 */
	@Override
	protected ImplementationReference<CompositeDeclaration> generateReference() {
		return new Reference(getName());
	}

	/**
	 * The list of contextual dependencies
	 */
	public Set<RelationDeclaration> getContextualDependencies() {
		return contextualDependencies;
	}

	/**
	 * The list of contextual link declarations
	 */
	public List<LinkDeclaration> getContextualLinks() {
		return contextualLinks;
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
	 * The list of contextual dependencies
	 */
	public Set<RelationDeclaration> getOverridenDependencies() {
		return contextualOverrides;
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
	 * Override the return type to a most specific class in order to avoid
	 * unchecked casting when used
	 */
	@Override
	@SuppressWarnings("unchecked")
	public ImplementationReference<CompositeDeclaration> getReference() {
		return (ImplementationReference<CompositeDeclaration>) super
				.getReference();
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
		ret += "\n   Main Implementation: " + mainComponent != null ? mainComponent
				.getIdentifier() : "";
		return ret;
	}

}
