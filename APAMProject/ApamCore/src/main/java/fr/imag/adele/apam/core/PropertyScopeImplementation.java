package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic implementation of a property scope.
 * 
 * @author vega
 *
 */
public class PropertyScopeImplementation implements PropertyScope {

	private final List<PropertyDefinition> defintions;
	
	public PropertyScopeImplementation() {
		this.defintions = new ArrayList<PropertyDefinition>();
	}
	
	@Override
	public List<PropertyDefinition> getPropertyDefinitions() {
		return this.defintions;
	}
	
	@Override
	public PropertyDefinition getPropertyDefinition(String propertyName) {
		for (PropertyDefinition defintion : defintions) {
			if (defintion.getName().equals(propertyName))
				return defintion;
		}
		return null;
	}


	@Override
	public boolean isDefined(String propertyName) {
		return getPropertyDefinition(propertyName) != null;
	}

}
