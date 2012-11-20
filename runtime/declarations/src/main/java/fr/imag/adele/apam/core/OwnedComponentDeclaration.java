package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class OwnedComponentDeclaration  {
	
	
	/**
	 * The property used to filter the owned instances
	 */
	private final PropertyDefinition.Reference property;
	
	/**
	 * The value of the property for all owned instances of the component
	 */
	private final Set<String> values;
	
	/**
	 * The list of automatic resource grants
	 */
	private final List<GrantDeclaration> grants;
	
	public OwnedComponentDeclaration(PropertyDefinition.Reference property, Set<String> values) {
		this.property		= property;
		this.values			= values;
		this.grants			= new ArrayList<GrantDeclaration>();
	}
	
	/**
	 * The owned component
	 */
	public ComponentReference<?> getComponent() {
		return property.getDeclaringComponent();
	}
	
	/**
	 * The property used to filter which instances must be owned
	 */
	public PropertyDefinition.Reference getProperty() {
		return property;
	}
	
	/**
	 * The value of the property for all owned instances of the component
	 */
	public Set<String> getValues() {
		return values;
	}

    /**
     * The list of resource grants
     */
    public List<GrantDeclaration> getGrants() {
		return grants;
	}
	
}
