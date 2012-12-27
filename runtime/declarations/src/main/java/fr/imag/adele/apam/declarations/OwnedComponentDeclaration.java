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
import java.util.List;
import java.util.Set;


public class OwnedComponentDeclaration  {
	
	
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
	
	public OwnedComponentDeclaration(PropertyDefinition.Reference property, Set<String> values) {
		this.property		= property;
		this.values			= values;
		this.grants			= new ArrayList<GrantDeclaration>();
	}
	
	/**
	 * The owned component
	 */
	public ComponentReference<?> getComponent() {
		return property.getDeclaringComponent();
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

    /**
     * The list of resource grants
     */
    public List<GrantDeclaration> getGrants() {
		return grants;
	}
	
}
