package fr.imag.adele.apam.core;


/**
 * This class represents the declaration of a service provider specification.
 * 
 * This class abstracts over a set of implementations, and declares the provided and
 * required resources common to all these implementations.
 * 
 * It also defines the property scope for all the properties distinguishing the different
 * implementations
 * 
 * @author vega
 *
 */
public class SpecificationDeclaration extends ComponentDeclaration {

    public SpecificationDeclaration(String name) {
        super(name);
    }

    @Override
    protected SpecificationReference generateReference() {
        return new SpecificationReference(getName());
    }

    @Override
    public String toString() {
        return "Specification " + super.toString();
    }

}
