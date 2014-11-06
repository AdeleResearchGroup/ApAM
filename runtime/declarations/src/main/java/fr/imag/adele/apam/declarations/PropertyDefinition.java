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

import fr.imag.adele.apam.declarations.references.components.ComponentReference;

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
	 * A reference to a property definition declaration. Notice that property
	 * identifiers must be only unique in the context of their defining
	 * component declaration.
	 */
	public static class Reference extends FeatureReference {

		public Reference(ComponentReference<?> definingComponent, String name) {
			super(definingComponent, name);
		}

	}

	private static String getCollectionElementType(String type) {
		type = type.trim();
		if ((type == null) || (type.isEmpty()) || type.charAt(0) != '{') {
			return null;
		}
		return type.substring(1, type.length() - 1).trim();
	}

	/**
	 * The component in which this property definition is declared
	 */
	private final ComponentReference<?> component;

	/**
	 * The name of the property
	 */
	private final String name;

	/**
	 * The reference to this declaration
	 */
	private final Reference reference;

	/**
	 * the type of the property
	 */
	private final String type;

	private final String baseType;

	private final boolean isSet;

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
	 * Whether this is an internal, external or both property (modified by java
	 * only, API only or both ways)
	 */
	private final InjectedPropertyPolicy injected;

	public PropertyDefinition(ComponentReference<?> component, String name, String type, String defaultValue) {
		this(component, name, type, defaultValue, null, null, InjectedPropertyPolicy.INTERNAL);
	}
	
	public PropertyDefinition(ComponentReference<?> component, String name, String type, String defaultValue, 
					String field, String callback, InjectedPropertyPolicy injected) {

		assert component != null;
		assert name != null;

		this.component 		= component;
		this.name			= name;
		this.reference 		= new Reference(component, name);

		
		this.type 			= type;

		String elementType	= getCollectionElementType(type);
		this.baseType		= elementType != null ? elementType : type;
		this.isSet 			= elementType != null;
		
		this.defaultValue 	= defaultValue;
		this.field 			= field;
		this.callback 		= callback;
		this.injected 		= injected;
	}

	/**
	 * The defining component
	 */
	public ComponentReference<?> getComponent() {
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

	public String getBaseType() {
		return baseType;
	}
	
	public boolean isSet() {
		return isSet;
	}
	
	/**
	 * Get the default value of the property
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Whether a defualt value was specified
	 */
	public boolean hasDefaultValue() {
		return defaultValue != null && !defaultValue.isEmpty();
	}
	
	/**
	 * get the injected field, if specified
	 */
	public String getField() {
		return field;
	}

	/**
	 * Get the update callback, if specified
	 */
	public String getCallback() {
		return callback;
	}
	
	/**
	 * get the internal property
	 */
	public InjectedPropertyPolicy getInjected() {
		return injected;
	}

	/**
	 * Computes the effective declaration that is the result of applying the specified refinement
	 * to this declaration.
	 * 
	 * We can only refine the instrumentation of the property 
	 */
	public PropertyDefinition refinedBy(PropertyDefinition refinement) {
		return new PropertyDefinition(this.getComponent(),this.getName(), this.getType(), this.getDefaultValue(),
						refinement.getField(), refinement.getCallback(), refinement.getInjected());
	}

	@Override
	public String toString() {
		return "name: " + name + ". Type: " + type + ". default value: "
				+ defaultValue;
	}

}
