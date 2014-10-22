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
package fr.imag.adele.apam.declarations.references.resources;

import fr.imag.adele.apam.declarations.references.Reference;
import fr.imag.adele.apam.declarations.references.ResolvableReference;

/**
 * This class represents references to resources that are named using java
 * identifiers, like for example Services and Messages.
 * 
 * We use a single namespace to ensure java identifiers are unique.
 * 
 * @author vega
 * 
 */
public class ResourceReference extends Reference implements ResolvableReference {

	/**
	 * The namespace for all references to resources identified by java class
	 * names
	 */
	private final static Namespace JAVA_NAMESPACE = new Namespace() {
	};

	private final String type;

	public ResourceReference(String type) {
		super(ResourceReference.JAVA_NAMESPACE);
		this.type = type;
	}

	@Override
	public final String getIdentifier() {
		return getJavaType();
	}

	/**
	 * The java type associated with this resource
	 */
	public final String getJavaType() {
		return type;
	}

	@Override
	public String getName() {
		return type;
	}
}
