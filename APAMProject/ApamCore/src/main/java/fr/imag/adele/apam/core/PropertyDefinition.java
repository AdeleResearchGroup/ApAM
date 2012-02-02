package fr.imag.adele.apam.core;

/**
 * This class represents a property declaration.
 * 
 * It can be used to validate the properties describing a provider.
 * 
 * @author vega
 *
 */
public class PropertyDefinition {
	
	/**
	 * The name of the property 
	 */
	private final String name;
	
	/**
	 * the type of the property
	 */
	private final String type;

	/**
	 * The default value for the property
	 */
	private final Object defaultValue;
	
	public PropertyDefinition(String name, String type, String defaultValue) {
		
		assert name != null;
		
		this.name 			= name;
		this.type			= type;
		this.defaultValue	= convert(type,defaultValue);
	}
	
	/**
	 * Get the name of the property
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the type of the property
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the default value of the property
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * Verifies if the value is valid for the type of this property and
	 * converts it to an object.
	 * 
	 * If the serialized value is null, returns the default value associated
	 * to this property
	 * 
	 */
	public Object getValue(String serializedValue) {
		return (serializedValue == null) ? defaultValue : convert(type, serializedValue);
	}
	
	/**
	 * convert a serialized value of the specified type into its corresponding memory
	 * representation.
	 * 
	 * TODO
	 * 
	 * make all conversions for 
	 * 1) the basic types String, int, boolean, float
	 * 2) Sets, List and arrays of basic types
	 * 
	 */
	private static Object convert(String type, String serializedValue) {
		return serializedValue;
	}
}
