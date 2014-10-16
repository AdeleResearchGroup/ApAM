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

/**
 * The declaration of an instance.
 * 
 * It can include dependency and provide declarations that override the ones in
 * the implementation
 * 
 * @author vega
 * 
 */
public class InstanceDeclaration extends ComponentDeclaration {


	/**
	 * A reference to the implementation
	 */
	private final ImplementationReference<?>.Versioned implementation;

	/**
	 * The list of triggers that must be met to start this instance
	 */
	private final Set<ConstrainedReference> triggers;


    public InstanceDeclaration(ImplementationReference<?>.Versioned implementation, String name, Set<ConstrainedReference> triggers) {
    	
		super(name);


		assert implementation != null;

		this.implementation = implementation;
		this.triggers 		= triggers;
	}

	@Override
	protected ComponentReference<InstanceDeclaration> generateReference() {
		return new InstanceReference(getName());
	}

	@Override
	public ImplementationReference<?>.Versioned getGroupVersioned() {
		return implementation;
	}

	@Override
	public ImplementationReference<?> getGroup() {
		return (ImplementationReference<?>) implementation.getComponent();
	}
	

	/**
	 * The implementation of this instance
	 */
	public final ImplementationReference<?> getImplementation() {
		return (ImplementationReference<?>) implementation.getComponent();
	}

 	/**
	 * The implementation of this instance
	 */
	public final ImplementationReference<?>.Versioned getImplementationVersion() {
		return implementation;
	}

	/**
	 * The triggering specification
	 */
	public Set<ConstrainedReference> getTriggers() {
		return triggers;
	}

	@Override
	public String toString() {
		String ret = "Instance declaration " + super.toString();
		ret += "\n    Implementation: " + getImplementation().getIdentifier();
		return ret;
	}
}
