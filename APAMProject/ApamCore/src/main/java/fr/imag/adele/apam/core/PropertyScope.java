package fr.imag.adele.apam.core;

import java.util.List;

/**
 * This interface represent an entity that defines the name scope for properties describing a provider.
 * 
 * 
 * @author vega
 *
 */
public interface PropertyScope {

	/**
	 * The list of property definitions in this scope
	 */
	public List<PropertyDefinition> getPropertyDefinitions();
	
	/**
	 * Check if a property is defined in this scope
	 */
	public boolean isDefined(String propertyName);
	
	/**
	 * Get a property definition by name
	 */
	public PropertyDefinition getPropertyDefinition(String propertyName);
	
}
