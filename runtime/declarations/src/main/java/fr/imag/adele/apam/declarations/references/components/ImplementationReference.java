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
package fr.imag.adele.apam.declarations.references.components;

import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;

/**
 * This class represents a reference to a particular service implementation.
 * 
 * Notice that atomic and composite references will be in the same namespace.
 * 
 * @author vega
 * 
 */
public class ImplementationReference<D extends ImplementationDeclaration> extends ComponentReference<D> {

	public ImplementationReference(String name) {
		super(name);
	}

	@Override
	public ComponentKind getKind() {
		return ComponentKind.IMPLEMENTATION;
	}

	@Override
	public String toString() {
		return "Implementation " + getIdentifier();
	}

}
