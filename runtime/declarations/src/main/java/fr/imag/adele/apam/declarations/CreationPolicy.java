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
 * Defines the moment at which the framework will lookup for an instance of a
 * dependency
 * 
 * @author jnascimento
 * 
 */
public enum CreationPolicy {

	/**
	 * Resolution is done only via XML
	 */
	MANUAL,

	/**
	 * Resolves only when the client tries to use the dependency for the first
	 * time
	 */
	LAZY,

	/**
	 * Resolves the dependency as soon as the client starts
	 */
	EAGER;

	public static CreationPolicy getPolicy(String id) {
		for (CreationPolicy p : values()) {
			if (p.toString().toLowerCase().equals(id)) {
				return p;
			}
		}

		return null;
	}

}
