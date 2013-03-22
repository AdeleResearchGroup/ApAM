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
	 * The list of contextual resolution policies of this composite 
	 */
	private final List<DependencyDeclaration> contextualDependencies;
	
	/**
	 * The list of dependencies promotions of this composite
	 */
	private final List<DependencyPromotion> promotions;
	
    public CompositeDeclaration(String name, SpecificationReference specification, ComponentReference<?> mainComponent) {
        super(name, specification);

        this.mainComponent 			= mainComponent;
        
        this.visibility				= new VisibilityDeclaration();
        this.ownedComponents		= new HashSet<OwnedComponentDeclaration>();
        this.instances				= new ArrayList<InstanceDeclaration>();
        this.contextualDependencies	= new ArrayList<DependencyDeclaration>();
        this.promotions				= new ArrayList<DependencyPromotion>();
        
    }

	/**
	 * A reference to a composite implementation
	 */
    private static class Reference extends ImplementationReference<CompositeDeclaration> {

		public Reference(String name) {
			super(name);
		}

	}

    /**
     * Generates the reference to this implementation
     */
    @Override
    protected ImplementationReference<CompositeDeclaration> generateReference() {
    	return new Reference(getName());
    }

    /**
     * Override the return type to a most specific class in order to avoid unchecked casting when used
     */
	@Override
    @SuppressWarnings("unchecked")
    public ImplementationReference<CompositeDeclaration> getReference() {
        return (ImplementationReference<CompositeDeclaration>) super.getReference();
    }
    
    /**
     * Get the main implementation
     */
    public ComponentReference<?> getMainComponent() {
        return mainComponent;
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
    
    /**
     * Get the list of owned appearing components 
     */
    public Set<OwnedComponentDeclaration> getOwnedComponents() {
    	return ownedComponents;
    }
    
    /**
     * Get the list of declared instances
     */
    public List<InstanceDeclaration> getInstanceDeclarations() {
    	return instances;
    }
    
    /**
     * The list of contextual promotions
     */
    public List<DependencyPromotion> getPromotions() {
		return promotions;
	}
    
    /**
     * The list of contextual dependencies
     */
    public List<DependencyDeclaration> getContextualDependencies() {
		return contextualDependencies;
	}
    

    @Override
    public String toString() {
        String ret = "\nComposite declaration " + super.toString();
        ret += "\n   Main Implementation: " + mainComponent.getIdentifier();
        return ret;
    }

}
