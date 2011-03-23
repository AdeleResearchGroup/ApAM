package fr.imag.adele.apam.apamAPI;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

/**
 * Interface that each manager MUST implement.
 * Used by APAM to resolve the dependencies and manage the application.
 * @author Jacky
 *
 */

public interface Manager {
	
	/**
	 * Provided that a resolution will be asked for a wire between from and the required specification (or interface), 
	 * each manager is asked if it want to be involved. 
	 * If not, does nothing.
	 * If so, it must return the list "involved" including itself somewhere (the order is important),
	 * and perhaps, it can add the contraints that it will require from each manager. 
	 * @param from the instance origin of the future wire.
	 * @param interfaceName the name of one of the interfaces of the specification to resolve.
	 * @param specName the *logical* name of that specification; different from SAM. May be null. 
	 * @param filter The constraints added by this manager.
	 * @param involved the managers currently involved in this resolution.
	 * @return The list of managers involved, including this manager if it feels involved; in the right order.
	 */
	public List<Manager> getSelectionPathSpec (ASMInst from, String interfaceName, String specName, String depName, Filter filter, 
			List<Manager> involved) ;
	
	
	/**
	 * Provided that a resolution will be asked for a wire between from and the required specification (or interface), 
	 * each manager is asked if it want to be involved. 
	 * If not, does nothing.
	 * If so, it must return the list "involved" including itself somewhere (the order is important),
	 * and perhaps, it can add the contraints that it will require from each manager. 
	 * @param from the instance origin of the future wire.
	 * @param samImplName the technical name of implementation to resolve, as returned by SAM.
	 * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
	 * @param filter The constraints added by this manager.
	 * @param involved the managers currently involved in this resolution.
	 * @return The list of managers involved, including this manager if it feels involved; in the right order.
	 */
	public List<Manager> getSelectionPathImpl (ASMInst from, String samImplName, String implName, String depName, Filter filter, 
			List<Manager> involved) ;

	/**
	 * The manager is asked to find the "right" resolution for the required specification (or interface),
	 * in order to create a wire between from and and the returned instance. 
	 * @param from the instance origin of the future wire.
	 * @param interfaceName the name of one of the interfaces of the specification to resolve.
	 * @param specName the *logical* name of that specification; different from SAM. May be null. 
	 * @param filter The constraints added by this manager.
	 * @param involved the managers currently involved in this resolution.
	 * @return an instance if resolved, null otherwise
	 */
	public ASMInst resolveSpec (ASMInst from, String interfaceName, String specName, String depName, Set<Filter> constraints) ;

	/**
	 * The manager is asked to find the "right" resolution for the required implementation,
	 * in order to create a wire between from and the returned instance. 
	 * @param from the instance origin of the future wire.
	 * @param samImplName the technical name of implementation to resolve, as returned by SAM.
	 * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
	 * @param filter The constraints added by this manager.
	 * @param involved the managers currently involved in this resolution.
	 * @return an instance if resolved, null otherwise
	 */
public ASMInst resolveImpl (ASMInst from, String samImplName, String implName, String depName, Set<Filter> constraints) ;

		// returns the relative priority of that manager, for the resolution algorithm
	public int getPriority () ;
}
