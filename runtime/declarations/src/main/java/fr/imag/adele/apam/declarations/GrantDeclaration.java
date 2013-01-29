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

import java.util.Set;

public class GrantDeclaration {

	private final DependencyDeclaration.Reference dependency;
	
	private final Set<String> states;
	
	public GrantDeclaration(DependencyDeclaration.Reference dependency, Set<String> states) {
		this.dependency = dependency;
		this.states = states;
	}
	
	public DependencyDeclaration.Reference getDependency() {
		return dependency;
	}
	
	public Set<String> getStates() {
		return states;
	}
	
	@Override
	public String toString() {
		return "<grant when " + states + " " + dependency.getDeclaringComponent() + " dependency=\"" 
				+ dependency.getIdentifier() + "\"/>" ;
	}
}
