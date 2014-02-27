package fr.imag.adele.apam;

import java.util.Set;


/**
 * This interface must be implemented by a manager that can dynamically update components.
 * 
 * @author vega
 *
 */
public interface DeploymentManager extends ContextualManager {

	/**
	 * A representation of a component deployment unit.
	 * 
	 * Because of packaging granularity, the same deployment unit may contain
	 * several other components, that will be updated as a side effect of 
	 * updating the component.
	 * 
	 */
	public interface Unit {
		
		/**
		 * The list of components in this deployment unit
		 */
		public Set<String> getComponents();
		
		/**
		 * Updates all the components in the deployment unit
		 */
		public void update() throws Exception;
	}
	
	/**
	 * Get the deployment unit associated with the given implementation. 
	 * 
	 * When an update is requested all deployment managers will be queried in turn to find the 
	 * deployment unit. The manager that actually deployed the component must respond with its
	 * associated deployment unit, other managers must return null.
	 * 
	 */

	public Unit getDeploymentUnit(CompositeType context, Implementation component);

}
