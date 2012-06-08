package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

public class TargetDeclaration implements ConstrainedReference {

	/**
	 * The reference to the target component.
	 */
	private final ResolvableReference resource;
	
	/**
	 * The set of constraints that must be satisfied by the target component implementation
	 */
	private final Set<String> implementationConstraints;
	
	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private final Set<String> instanceConstraints;
	
	

	public TargetDeclaration(ResolvableReference resource) {
		
        assert resource != null;
        this.resource 		= resource;

        this.implementationConstraints 	= new HashSet<String>();
        this.instanceConstraints 		= new HashSet<String>();
	}

	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
	    return resource;
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.core.ConstrainedReference#getImplementationConstraints()
	 */
	@Override
	public Set<String> getImplementationConstraints() {
	    return implementationConstraints;
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.core.ConstrainedReference#getInstanceConstraints()
	 */
	@Override
	public Set<String> getInstanceConstraints() {
	    return instanceConstraints;
	}

}