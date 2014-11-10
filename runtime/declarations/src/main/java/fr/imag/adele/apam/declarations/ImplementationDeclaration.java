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

import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;

/**
 * This class represents all the common declarations for an implementation of a
 * service provider.
 * 
 * @author vega
 * 
 */
public abstract class ImplementationDeclaration extends ComponentDeclaration {

	/**
	 * The specification implemented by this implementation
	 */
	private final VersionedReference<SpecificationDeclaration> specification;

    public ImplementationDeclaration(String name, VersionedReference<SpecificationDeclaration> specification) {
        super(name);
        this.specification = specification;
    }

    protected ImplementationDeclaration(ImplementationDeclaration original) {
        super(original);
        
        this.specification = original.specification;
    }

	/**
	 * Override with a narrower return type
	 */
	@Override
	protected abstract ImplementationReference<?> generateReference();

	/**
	 * Override the return type to a most specific class in order to avoid
	 * unchecked casting when used
	 */
	@Override
	public ImplementationReference<?> getReference() {
		return (ImplementationReference<?>) super.getReference();
	}
	
	@Override
	public VersionedReference<SpecificationDeclaration> getGroupVersioned() {
		return specification;
	}

	@Override
	public ComponentReference<SpecificationDeclaration> getGroup() {
		return getSpecification();
	}
	
	/**
	 * Get the specification implemented by this implementation
	 */
	public ComponentReference<SpecificationDeclaration> getSpecification() {
		return specification != null ? specification.getComponent() : null;
	}


	@Override
	public String toString() {
		String ret = "Implementation declaration " + super.toString();
		String specificationName = (specification != null ? getSpecification().getName() : "null");
		ret += "\n   Specification: " + specificationName;
		return ret;
	}

}
