/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.declarations;


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
    public static class Reference extends fr.imag.adele.apam.declarations.Reference {

        private final String name;

        public Reference(ComponentReference<?> definingComponent, String name) {
            super(definingComponent);
            this.name = name;
        }

        @Override
        public String getIdentifier() {
            return name;
        }

        public ComponentReference<?> getDeclaringComponent() {
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
     * The associated callback in the code, if any.
     */
    private final String callback;
    
    /**
     * Whether this is an internal property, whose value can not be modified by API
     */
    private final boolean internal;
    
    /**
     * Whether this is property defined at the same level
     */
    private final boolean local;

    public PropertyDefinition(ComponentDeclaration component, String name, String type, String defaultValue, String field, String callback, boolean internal, boolean local) {

        assert component != null;
        assert name != null;

        this.component		= component;
        this.name 			= name;
        this.reference		= new Reference(component.getReference(),name);
        this.type			= type;
        this.defaultValue	= defaultValue;
        this.field 			= field;
        this.callback		= callback;
        this.internal 		= internal ;
        this.local 			= local ;
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
	 */
    public boolean isInternal () {
    	return internal ;
    }

	/**
	 * get the internal property
	 */
    public boolean isLocal () {
    	return local ;
    }

    /**
     * get the injected field, if specified
     */
    public String getField () {
    	return field ;
    }
    
    /**
     * Get the update callback, if specified
     */
    public String getCallback() {
		return callback;
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
