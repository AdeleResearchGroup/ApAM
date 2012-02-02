package fr.imag.adele.apam.core;

import java.util.List;

/**
 * This class represents the declaration of a service provider specification.
 * 
 * This class abstracts over a set of implementations, and declares the provided and
 * required resources common to all these implementations.
 * 
 * It also defines the property scope for all the properties distinguishing the different
 * implementations
 * 
 * @author vega
 *
 */
public class SpecificationDeclaration extends ComponentDeclaration implements PropertyScope {

	private final PropertyScopeImplementation defintions;
	
	protected SpecificationDeclaration(String name) {
		super(name);
		this.defintions = new PropertyScopeImplementation();
	}

	@Override
	public List<PropertyDefinition> getPropertyDefinitions() {
		return defintions.getPropertyDefinitions();
	}

	@Override
	public boolean isDefined(String propertyName) {
		return defintions.isDefined(propertyName);
	}

	@Override
	public PropertyDefinition getPropertyDefinition(String propertyName) {
		return defintions.getPropertyDefinition(propertyName);
	}


}
