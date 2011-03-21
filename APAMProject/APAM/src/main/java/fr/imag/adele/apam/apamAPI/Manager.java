package fr.imag.adele.apam.apamAPI;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.sam.ipojo.util.LDAP;

/**
 * Interface that each manager MUST implement.
 * Used by APAM to resolve the dependencies and manage the application.
 * @author Jacky
 *
 */

public interface Manager extends DynamicManager {
	
	/**
	 * Provided that a resolution will be asked for a wire between from and to, each manager is 
	 * asked if it want to be involved. 
	 * If not, does nothing.
	 * If so, it must return the list "involved" including itself somewhere (the order is important),
	 * and perhaps, it can add the contraints that it will require from each manager. 
	 * @param from the instance origin of the future wire.
	 * @param to the Specification to resolve; destination of the future wire.
	 * @param filter The constraints added by this manager.
	 * @param involved the managers currently involved in this resolution.
	 * @return The list of managers involved, including this manager if it feels involved; in the right order.
	 */
	public List<Manager> getSelectionPath (ASMInst from, ASMSpec to, String depName, Filter filter, 
			List<Manager> involved) ;
	public List<Manager> getSelectionPath (ASMInst from, ASMImpl to, String depName, Filter filter, 
			List<Manager> involved) ;


	/**
	 * The manager is asked to find the "right" resolution for "to", in order to create a wire between from and to. 
	 * @param from the instance origin of the future wire.
	 * @param to the Specification or implementation to resolve; destination of the future wire.
	 * @param contraints The constraints that the resolved instance MUST satisfy. Can be null
	 * @param abort true if this resolution MUST FAIL.
	 * @return an instance if resolved, null otherwise
	 */
	public ASMInst resolve (ASMInst from, ASMSpec to, String depName, Set<Filter> constraints) ;
	public ASMInst resolve (ASMInst from, ASMImpl to, String depName, Set<Filter> constraints) ;

		// returns the relative priority of that manager, for the resolution algorithm
	public int getPriority () ;
}
