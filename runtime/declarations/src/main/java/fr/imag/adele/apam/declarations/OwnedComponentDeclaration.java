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

public class OwnedComponentDeclaration {

	/**
	 * The owned component reference
	 */
	private final ComponentReference<?> component;
	/**
	 * The property used to filter the owned instances
	 */
	private final PropertyDefinition.Reference property;

	/**
	 * The value of the property for all owned instances of the component
	 */
	private final Set<String> values;

	/**
	 * The list of automatic resource grants
	 */
	private final List<GrantDeclaration> grants;

	/**
	 * The list of explicit resources denied
	 */
	private final List<GrantDeclaration> denies;

	public OwnedComponentDeclaration(ComponentReference<?> component, String property, Set<String> values) {
		this.component	= component;
		this.property 	= property != null ? new PropertyDefinition.Reference(component, property) : null;
		this.values 	= values;
		this.grants 	= new ArrayList<GrantDeclaration>();
		this.denies 	= new ArrayList<GrantDeclaration>();
	}

	public OwnedComponentDeclaration(OwnedComponentDeclaration original) {
		
		this(original.component, original.property != null ? original.property.getIdentifier() : null, new HashSet<String>(original.values));

		for (GrantDeclaration grant : original.grants) {
			this.grants.add(new GrantDeclaration(grant));
		}
		
		for (GrantDeclaration deny : original.denies) {
			this.denies.add(new GrantDeclaration(deny));
		}
		
	}
	
	/**
	 * The owned component
	 */
	public ComponentReference<?> getComponent() {
		return component;
	}

	/**
	 * The list of resources explicitly denied
	 */
	public List<GrantDeclaration> getDenies() {
		return denies;
	}

	/**
	 * The list of resource grants
	 */
	public List<GrantDeclaration> getGrants() {
		return grants;
	}

	/**
	 * The property used to filter which instances must be owned
	 */
	public PropertyDefinition.Reference getProperty() {
		return property;
	}

	/**
	 * The value of the property for all owned instances of the component
	 */
	public Set<String> getValues() {
		return values;
	}

	@Override
	public String toString() {
		StringBuffer description = new StringBuffer();
		description.append("own").append(getComponent());
		if (getProperty() != null) {
			description.append(" property ")
					.append(getProperty().getIdentifier())
					.append(" values : " + getValues());
		}
		return description.toString();
	}
}
