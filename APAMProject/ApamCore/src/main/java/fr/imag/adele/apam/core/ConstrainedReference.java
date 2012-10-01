package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is a marker for all declarations that add constraints to a component target
 * reference
 * 
 */
public class ConstrainedReference {

	/**
	 * The reference to the target component.
	 */
	private final ResolvableReference 	resource;
	
	/**
	 * The set of constraints that must be satisfied by the target component implementation
	 */
	private final Set<String> 			implementationConstraints;
	
	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private final Set<String> 			instanceConstraints;
	
    /**
     * The list of preferences to choose among candidate service provider implementation
     */
    private final List<String> 			implementationPreferences;

    /**
     * The list of preferences to choose among candidate service provider instances
     */
    private final List<String> 			instancePreferences;
	
    
	public ConstrainedReference(ResolvableReference resource) {
		
        assert resource != null;
        this.resource 		= resource;

        this.implementationConstraints 	= new HashSet<String>();
        this.instanceConstraints 		= new HashSet<String>();
        this.implementationPreferences 	= new ArrayList<String>();
        this.instancePreferences 		= new ArrayList<String>();
	}

	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
	    return resource;
	}

	/**
	 * Get the constraints that need to be satisfied by the implementation that resolves the reference
	 */
	public Set<String> getImplementationConstraints() {
	    return implementationConstraints;
	}

	/**
	 * Get the constraints that need to be satisfied by the instance that resolves the reference
	 */
	public Set<String> getInstanceConstraints() {
	    return instanceConstraints;
	}

    /**
     * Get the resource provider preferences
     */
    public List<String> getImplementationPreferences() {
        return implementationPreferences;
    }

    /**
     * Get the instance provider preferences
     */
    public List<String> getInstancePreferences() {
        return instancePreferences;
    }

	
}