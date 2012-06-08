package fr.imag.adele.apam.core;

/**
 * This is a marker interface to identify those entities that can be referenced in dependencies resolved by APAM
 * 
 * @author vega
 *
 */
public interface ResolvableReference {

	
	/**
	 * Cast this reference to a particular class of reference. Returns null if the cast is not possible
	 */
	public <R extends Reference> R as(Class<R> kind);
	
}
