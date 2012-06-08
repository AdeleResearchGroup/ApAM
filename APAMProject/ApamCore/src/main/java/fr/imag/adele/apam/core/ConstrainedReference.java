package fr.imag.adele.apam.core;

import java.util.Set;

/**
 * This class is a marker for all declarations that add constraints to a component target
 * reference
 * 
 */
public interface ConstrainedReference {

	/**
	 * Get the target reference
	 */
	public abstract ResolvableReference getTarget();
	
	/**
	 * Get the constraints that need to be satisfied by the implementation that resolves the reference
	 */
	public abstract Set<String> getImplementationConstraints();

	/**
	 * Get the constraints that need to be satisfied by the instance that resolves the reference
	 */
	public abstract Set<String> getInstanceConstraints();

}