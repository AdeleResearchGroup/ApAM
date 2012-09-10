package fr.imag.adele.apam.core;

import java.util.Set;


/**
 * The declaration of an instance.
 * 
 *  It can include dependency and provided declarations that override the ones in the implementation
 *  
 * @author vega
 *
 */
public class InstanceDeclaration extends ComponentDeclaration {


	/**
	 * A reference to the implementation
	 */
    private final ImplementationReference<?> implementation;

    /**
     * The list of triggers that must be met to start this instance
     */
    private final Set<TargetDeclaration> triggers;
    
    public InstanceDeclaration(ImplementationReference<?> implementation, String name, Set<TargetDeclaration> triggers) {
        super(name);

        assert implementation != null;
        
        this.implementation	= implementation;
        this.triggers 		= triggers;
    }

    /**
     * The implementation of this instance
     */
    public ImplementationReference<?> getImplementation() {
        return implementation;
    }

    /**
     * The triggering specification
     */
    public Set<TargetDeclaration> getTriggers() {
		return triggers;
	}
    

    @Override
    protected ComponentReference<InstanceDeclaration> generateReference() {
        return new InstanceReference(getName());
    }

    @Override
    public String toString() {
        String ret = "Instance declaration " + super.toString();
        ret += "\n    Implementation: " + implementation.getIdentifier();
        return ret;
    }

	@Override
	public ComponentReference<?> getGroupReference() {
		return getImplementation();
	}
}
