package fr.imag.adele.apam.core;


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
     * A description of an optional instance that must be present in the platform to trigger
     * the actual instantiation for this declaration
     */
    private final TargetDeclaration trigger;
    
    public InstanceDeclaration(ImplementationReference<?> implementation, String name, TargetDeclaration trigger) {
        super(name);

        assert implementation != null;
        this.implementation	= implementation;
        this.trigger = trigger;
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
    public TargetDeclaration getTrigger() {
		return trigger;
	}
    

	/**
     * Instances are never directly referenced
     */
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
