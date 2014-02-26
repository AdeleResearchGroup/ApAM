package fr.imag.adele.apam;


/**
 * A manager is an external entity that is used to extend the behavior of the APAM core.
 * 
 * Specific subclasses of this interface represents the different extension point available
 * that can be provided by a manager.
 *   
 * @author vega
 *
 */
public interface Manager {

	/**
	 * 
	 * @return the name of that manager.
	 */
	public String getName();

}
