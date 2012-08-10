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

    /**
     * the associated field in the code, if any.
     */
    private final String field;

    /**
     * True if declared "internal"
     */
    private final boolean internal ;

    public PropertyDefinition(String name, String type, String defaultValue, String field, boolean internal) {

        assert name != null;

        this.name 			= name;
        this.type			= type;
        this.defaultValue	= PropertyDefinition.convert(type,defaultValue);
        this.field = field ;
        this.internal = internal ;
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
	 * get the internal property
	 * @return
	 */
    public boolean isInternal () {
    	return internal ;
    }
    
    /**
     * returns the associated field name; null if no field.
     * @return
     */
    public String getField () {
    	return field ;
    }
    
    /**
     * Get the default value of the property
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Verifies if the value is valid for the type of this property definition and
     * converts it to an object.
     * 
     * If the serialized value is null, returns the default value associated
     * to this property
     * 
     */
    public Object getValue(String serializedValue) {
        return (serializedValue == null) ? defaultValue : PropertyDefinition.convert(type, serializedValue);
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
        // TODO
        return serializedValue;
    }
    
    @Override
    public String toString() {
        return "name: " + name + ". Type: " + type + ". default value: " + defaultValue;
    }

}
