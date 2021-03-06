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
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;


/**
 * This class represents the declaration of a service provider specification.
 * 
 * This class abstracts over a set of implementations, and declares the provided
 * and required resources common to all these implementations.
 * 
 * It also defines the property scope for all the properties distinguishing the
 * different implementations
 * 
 * @author vega
 * 
 */
public class SpecificationDeclaration extends ComponentDeclaration {

	/**
	 * Create an empty declration
	 */
	public SpecificationDeclaration(String name) {
		super(name);
	}

	
	/**
	 * Clone this declaration
	 */
	protected SpecificationDeclaration(SpecificationDeclaration original) {
		super(original);
	}


	@Override
	protected SpecificationReference generateReference() {
		return new SpecificationReference(getName());
	}


	/**
	 * Override the return type to a most specific class in order to avoid
	 * unchecked casting when used
	 */
	@Override
	public final SpecificationReference getReference() {
		return (SpecificationReference) super.getReference();
	}

	@Override
	public ComponentReference<ComponentDeclaration> getGroup() {
		return null;
	}

	@Override
	public VersionedReference<ComponentDeclaration> getGroupVersioned() {
		return null;
	}
	
	@Override
	public String toString() {
		return "Specification " + super.toString();
	}

}
