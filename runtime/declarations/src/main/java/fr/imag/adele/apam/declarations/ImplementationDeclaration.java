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
 * This class represents all the common declarations for an implementation of a
 * service provider.
 * 
 * @author vega
 * 
 */
public abstract class ImplementationDeclaration extends ComponentDeclaration {

    /**
     * The specification implemented by this implementation
     */
    private final SpecificationReference specification;

    public ImplementationDeclaration(String name,
	    SpecificationReference specification) {
	super(name);
	this.specification = specification;
    }

    /**
     * Override with a narrower return type
     */
    @Override
    protected abstract ImplementationReference<?> generateReference();

    @Override
    public ComponentReference<?> getGroupReference() {
	return getSpecification();
    }

    /**
     * Override the return type to a most specific class in order to avoid
     * unchecked casting when used
     */
    @Override
    public ImplementationReference<?> getReference() {
	return (ImplementationReference<?>) super.getReference();
    }

    /**
     * Get the specification implemented by this implementation
     */
    public SpecificationReference getSpecification() {
	return specification;
    }

    @Override
    public boolean resolves(RelationDeclaration relation) {
	return super.resolves(relation)
		|| (getSpecification() != null && getSpecification().equals(
			relation.getTarget()))
		|| relation.getTarget().equals(this.getReference());
    }

    @Override
    public String toString() {
	String ret = "Implementation declaration " + super.toString();
	String specificationName = (specification != null ? specification
		.getIdentifier() : "null");
	ret += "\n   Specification: " + specificationName;
	return ret;
    }

}
