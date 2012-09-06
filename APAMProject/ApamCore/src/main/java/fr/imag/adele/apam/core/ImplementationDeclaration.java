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
        this.specification = specification;
    }

    /**
     * Override with a narrower return type
     */
    @Override
    protected abstract ImplementationReference<?> generateReference();

    /**
     * Override the return type to a most specific class in order to avoid unchecked casting when used
     */
    @Override
    public final ImplementationReference<?> getReference() {
        return (ImplementationReference<?>) super.getReference();
    }
    
    /**
     * Get the specification implemented by this implementation
     */
    public SpecificationReference getSpecification() {
        return specification;
    }

    @Override
    public String toString() {
        String ret = "Implementation declaration " + super.toString();
        String specificationName = (specification != null? specification.getIdentifier() : "null");
        ret += "\n   Specification: " + specificationName;
        return ret;
    }
    
	@Override
	public ComponentReference<?> getGroupReference() {
		return getSpecification();
	}

}
