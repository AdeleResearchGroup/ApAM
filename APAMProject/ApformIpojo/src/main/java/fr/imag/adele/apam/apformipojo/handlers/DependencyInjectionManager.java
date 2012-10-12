package fr.imag.adele.apam.apformipojo.handlers;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.DependencyInjection;

/**
 * This class represents a kind of injection manager for a dependency. The injection manager is in charge of translating
 * the APAM events into platform specific action to inject the dependency into a field.
 * 
 * @author vega
 *
 */
public interface DependencyInjectionManager extends FieldInterceptor {

	/**
	 * The interface of the external resolver that is used to bind this field. 
	 * 
	 */
	public static interface Resolver {
		
		/**
		 * Registers an injection manager with a resolver.
		 * 
		 * The resolver can asynchronously update the dependency to modify the binding.
	 	 *
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.addTarget
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.removeTarget
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.substituteTarget
		 *  
		 */
		public void addInjection(DependencyInjectionManager injection);
		
		/**
		 * Request to lazily resolve an injection.
		 * 
		 *  This method is invoked by a injection manager to calculate its initial binding
		 *  when it is first accessed.
		 *  
		 *  The resolver must call back the manager to modify the resolved target.
		 *   
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.addTarget
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.removeTarget
		 *  @see fr.imag.adele.apam.apformipojo.InterfaceInjectionManager.substituteTarget
		 *  
		 */
		public boolean resolve(DependencyInjectionManager injection);
		
	} 
	
	/**
	 * The dependency injection that is managed by this manager
	 */
	public abstract DependencyInjection getDependencyInjection();

	
    /**
     * Get an XML representation of the state of this injection
     */
    public abstract Element getDescription();

    /**
     * The current state of the manager. 
     * 
     * A specific manager implementation can have dependencies on some platform services that may
     * become unavailable. In that case the translation from APAM action to platform actions is no
     * longer possible; 
     */
    
    public abstract boolean isValid();
    
	/**
	 * Adds a new target to this injection
	 */
	public abstract void addTarget(Instance target);

	/**
	 * Removes a target from the injection
	 * 
	 * @param target
	 */
	public abstract void removeTarget(Instance target);

	/**
	 * Substitutes an existing target by a new one
	 * 
	 * @param oldTarget
	 * @param newTarget
	 */
	public abstract void substituteTarget(Instance oldTarget, Instance newTarget);

}