package fr.imag.adele.apam.declarations;



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

    /**
     * Override the return type to a most specific class in order to avoid unchecked casting when used
     */
    @Override
    public final SpecificationReference getReference() {
        return (SpecificationReference) super.getReference();
    }

    @Override
    public boolean resolves(DependencyDeclaration dependency) {
        return	super.resolves(dependency) ||
        		dependency.getTarget().equals(this.getReference());
    }

    @Override
    public String toString() {
        return "Specification " + super.toString();
    }

	@Override
	public ComponentReference<?> getGroupReference() {
		return null;
	}

}
