package fr.imag.adele.apam.core;

/**
 * This enumeration represents the different policies available for handling events in which
 * a dependency is no resolvable
 * 
 * @author vega
 *
 */
public enum MissingPolicy {

	/**
	 * The default policy is just to ignore this event. If a client uses the dependency a null 
	 * reference will be returned to the calling thread.
	 */
	OPTIONAL,
	
	/**
	 * Automatically creates the wire for resolving the dependency when a suitable instance is
	 * available. If a client tries to use the dependency, the invoking thread will be blocked until
	 * the dependency is resolved again.
	 */
	WAIT,
	
	/**
	 * If a client effectively accesses the dependency an exception will be thrown to signal the
	 * missing target.
	 */
	EXCEPTION
	
}
