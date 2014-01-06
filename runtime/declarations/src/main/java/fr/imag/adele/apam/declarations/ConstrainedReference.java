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

/**
 * This class is a marker for all declarations that add constraints to a
 * component target reference
 * 
 */
public class ConstrainedReference {

	/**
	 * The reference to the target component.
	 */
	private final ResolvableReference resource;

	/**
	 * The set of constraints that must be satisfied by the target component
	 * implementation
	 */
	private final Set<String> implementationConstraints;

	/**
	 * The set of constraints that must be satisfied by the target component
	 * instance
	 */
	private final Set<String> instanceConstraints;

	/**
	 * The list of preferences to choose among candidate service provider
	 * implementation
	 */
	private final List<String> implementationPreferences;

	/**
	 * The list of preferences to choose among candidate service provider
	 * instances
	 */
	private final List<String> instancePreferences;

	public ConstrainedReference(ResolvableReference resource) {

		assert resource != null;
		this.resource = resource;

		this.implementationConstraints = new HashSet<String>();
		this.instanceConstraints = new HashSet<String>();
		this.implementationPreferences = new ArrayList<String>();
		this.instancePreferences = new ArrayList<String>();
	}

	/**
	 * Get the constraints that need to be satisfied by the implementation that
	 * resolves the reference
	 */
	public Set<String> getImplementationConstraints() {
		return implementationConstraints;
	}

	/**
	 * Get the resource provider preferences
	 */
	public List<String> getImplementationPreferences() {
		return implementationPreferences;
	}

	/**
	 * Get the constraints that need to be satisfied by the instance that
	 * resolves the reference
	 */
	public Set<String> getInstanceConstraints() {
		return instanceConstraints;
	}

	/**
	 * Get the instance provider preferences
	 */
	public List<String> getInstancePreferences() {
		return instancePreferences;
	}

	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return resource;
	}

}