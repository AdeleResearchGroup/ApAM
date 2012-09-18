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
     * A reference to a property definition declaration. Notice that property identifiers must be only
     * unique in the context of their defining component declaration.
     */
    public static class Reference extends fr.imag.adele.apam.core.Reference {

        private final String name;

        public Reference(ComponentReference<?> definingComponent, String name) {
            super(definingComponent);
            this.name = name;
        }

        @Override
        public String getIdentifier() {
            return name;
        }

        public ComponentReference<?> getDefiningComponent() {
            return (ComponentReference<?>) namespace;
        }

    }

    /**
     * The component in which this property definition is declared
     */
    private final ComponentDeclaration component;
    
    /**
     * The name of the property 
     */
    private final String name;

    /**
     * The reference to this declaration
     */
    private final Reference			reference;
    
    /**
     * the type of the property
     */
    private final String type;

    /**
     * The default value for the property
     */
    private final String defaultValue;

    /**
     * The associated field in the code, if any.
     */
    private final String field;

    /**
     * Whether this is an internal property, whose value can not be modified by API
     */
    private final boolean internal;

    public PropertyDefinition(ComponentDeclaration component, String name, String type, String defaultValue, String field, boolean internal) {

        assert component != null;
        assert name != null;

        this.component		= component;
        this.name 			= name;
        this.reference		= new Reference(component.getReference(),name);
        this.type			= type;
        this.defaultValue	= defaultValue;
        this.field = field ;
        this.internal = internal ;
    }

    /**
     * The defining component
     */
    public ComponentDeclaration getComponent() {
        return component;
    }

    /**
     * Get the name of the property
     */
    public String getName() {
        return name;
    }

    /**
     * Get the reference to this declaration
     */
    public Reference getReference() {
        return reference;
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
