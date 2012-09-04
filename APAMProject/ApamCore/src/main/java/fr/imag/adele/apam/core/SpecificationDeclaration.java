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

    /**
     * Override the return type to a most specific class in order to avoid unchecked casting when used
     */
    @Override
    public final SpecificationReference getReference() {
        return (SpecificationReference) super.getReference();
    }

    /**
     * Verifies if this specification resolves the given dependency.
     * 
     * Either the dependency explicitly requires this specification or it requires one of
     * the provided resources of this specification
     */
    public boolean resolves(DependencyDeclaration dependency) {
    	ResolvableReference requiredResource = dependency.getTarget();
    	return this.getReference().equals(requiredResource) || this.isProvided(requiredResource);
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
