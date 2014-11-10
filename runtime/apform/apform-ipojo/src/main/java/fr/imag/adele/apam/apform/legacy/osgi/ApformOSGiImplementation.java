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
package fr.imag.adele.apam.apform.legacy.osgi;

import java.util.Map;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime
 *
 */
public class ApformOSGiImplementation extends BaseApformComponent<Implementation,ImplementationDeclaration> implements ApformImplementation {

	/**
	 * A legacy implementation declaration
	 */
	private final static class Declaration extends ImplementationDeclaration {

		protected Declaration(String name, SpecificationReference specification) {
			super(name, VersionedReference.any(specification));
			setInstantiable(false);
		}

		/**
		 * Clone declaration
		 */
		protected Declaration(Declaration original) {
			super(original);
		}
		
		@Override
		protected ImplementationReference<?> generateReference() {
			return new Reference(getName());
		}
		
	}
	
	/**
	 * A reference to a legacy declaration
	 *
	 */
	public static class Reference extends ImplementationReference<Declaration> {

		protected Reference(String name) {
			super(name);
		}
		
	}
	
	/**
	 * The prototype instance used to create this implementation
	 */
	private final ApformOSGiInstance prototype;
	
	/**
	 * Creates a new implementation reference from a prototype instance
	 */
	public ApformOSGiImplementation(ApformOSGiInstance prototype) {

		super(new Declaration(prototype.getDeclaration().getImplementation().getName(),prototype.getSpecification().getApformSpec().getDeclaration().getReference()));
   		declaration.getProvidedResources().addAll(prototype.getSpecification().getProvidedResources());
		
		this.prototype		= prototype;
		
	}
	
	@Override
	public Bundle getBundle() {
		return prototype.getBundle();
	}
	
	/**
	 * Create a new legacy instance
	 */
	@Override
	public ApformInstance createInstance(Map<String, String> initialProperties) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Register a discovered instance
	 */
	@Override
	public ApformInstance addDiscoveredInstance(Map<String, Object> configuration) throws InvalidConfiguration,	UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
 
}
