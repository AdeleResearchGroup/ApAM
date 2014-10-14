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

/**
 * This class represent references to APAM components. They are parameterized by the type of component declaration
 * so that they can be statically typed.
 * 
 * Notice that a component reference can in turn be used as namespace for referencing entities locally declared in
 * the scope of the component declaration;
 * 
 * @author vega
 * 
 */
public class ComponentReference<D extends ComponentDeclaration> extends	Reference implements Reference.Namespace, ResolvableReference {

	/**
	 * The global name space associated with all APAM components.
	 * 
	 * NOTE All APAM declarations share a single name space, this means that even declarations at different
	 * abstraction levels (Specification, Implementation, Instance) must have unique names.
	 */
	private final static Namespace APAM_COMPONENT_NAMESPACE = new Namespace() {
	};

	/**
	 * The name of the component
	 */
	private final String name;

	public ComponentReference(String name) {
		super(APAM_COMPONENT_NAMESPACE);
		this.name = name;
	}

	@Override
	protected final String getIdentifier() {
		return getName();
	}

	public ComponentKind getKind() {
		return ComponentKind.COMPONENT;
	}

	/**
	 * The component name
	 */
	@Override
	public final String getName() {
		return name;
	}
	
	/**
	 * Get a reference to a range of revisions of the referenced component
	 */
	public final Versioned range(String range) {
		return this.new Versioned(range);
	} 

	/**
	 * Get a reference to all the revisions of the referenced component
	 */
	public final Versioned any() {
		return this.new Versioned(null);
	} 

	/**
	 * This class is used to represent a range of versions of the referenced component
	 */
	public class Versioned {
		
		private final String range;
		
		private Versioned(String range) {
			this.range = range;
		}
		
		public ComponentReference<D> getComponent() {
			return ComponentReference.this;
		}
		
		public String getRange() {
			return range;
		}
	} 
}
