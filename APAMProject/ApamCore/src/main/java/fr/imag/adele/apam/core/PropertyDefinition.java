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
    private final String defaultValue;

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
        this.defaultValue	= defaultValue;
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
    public String getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "name: " + name + ". Type: " + type + ". default value: " + defaultValue;
    }

}
