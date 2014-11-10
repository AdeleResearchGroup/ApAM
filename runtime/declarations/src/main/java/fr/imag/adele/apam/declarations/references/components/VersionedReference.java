/**
 * Copyright 2011-2014 Universite Joseph Fourier, LIG, ADELE team
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

import fr.imag.adele.apam.declarations.ComponentDeclaration;


/**
 * This class is used to represent a range of versions of the referenced component
 * 
 * @author vega
 * 
 */
public class VersionedReference<C extends ComponentDeclaration> {
	
	private final ComponentReference<C>	component;
	private final String 				range;
	
	/**
	 * Get a reference to any version of the specified component
	 */
	public static final <C extends ComponentDeclaration, R extends ComponentReference<C>> VersionedReference<C> any(R component) {
		return new VersionedReference<C>(component,null);
	}

	/**
	 * Get a reference to some version of the specified component in the specified range
	 */
	public static final  <C extends ComponentDeclaration, R extends ComponentReference<C>> VersionedReference<C> range(R component,String range) {
		return new VersionedReference<C>(component,range);
	}

	private VersionedReference(ComponentReference<C> component, String range) {
		this.component	= component;
		this.range 		= range;
	}
	
	/**
	 * The referenced component
	 */
	public ComponentReference<C> getComponent() {
		return component;
	}

	/**
	 * The name of the referenced component
	 */
	public String getName() {
		return component.getName();
	}

	/**
	 * The range of versions
	 */
	public String getRange() {
		return range;
	}
	
	@Override
	public int hashCode() {
		return component.hashCode()+ (range != null ? range.hashCode() : 0);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof VersionedReference)) {
			return false;
		}

		VersionedReference<?> that = (VersionedReference<?>) object;
		
		return this.component.equals(that.component) &&
			   this.range != null ? that.range != null && this.range.equals(that.range) : that.range == null;
	}
} 
