package fr.imag.adele.apam.core;

/**
 * This enumeration represents the different policies available for handling events in which
 * a dependency is no longer resolvable
 * 
 * @author vega
 *
 */
public enum MissingPolicy {

	/**
	 * The default policy is just to ignore this event. If a client uses the dependency a null 
	 * reference will be returned to the calling thread.
	 */
	NOTHING,
	
	/**
	 * Automatically recreates the wire for resolving the dependency when a suitable instance is
	 * available. If a client tries to use the dependency, the invoking thread will be blocked until
	 * the dependency is resolved again.
	 */
	WAIT,
	
	/**
	 * Deletes the client component instance, this may propagate to other instances and forces a
	 * recalculation of another branch in the graph of possible configurations.
	 */
	DELETE,
	
	/**
	 * The dependency is verified before instantiating the source component. Otherwise it behaves as
	 * the delete policy.
	 */
	MANDATORY
}
