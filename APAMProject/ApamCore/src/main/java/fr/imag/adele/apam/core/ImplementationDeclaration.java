package fr.imag.adele.apam.core;


/**
 * This class represents all the common declarations for an implementation of a service 
 * provider.
 * 
 * @author vega
 *
 */
public abstract class ImplementationDeclaration extends ComponentDeclaration {

    /**
     * The specification implemented by this implementation
     */
    private final SpecificationReference specification;


    protected ImplementationDeclaration(String name, SpecificationReference specification) {
        super(name);

        assert specification != null;

        this.specification = specification;
    }

    @Override
    protected ResourceReference generateReference() {
    	return new ImplementationReference(getName());
    }

    /**
     * Get the specification implemented by this implementation
     * @return
     */
    public SpecificationReference getSpecification() {
        return specification;
    }


}
