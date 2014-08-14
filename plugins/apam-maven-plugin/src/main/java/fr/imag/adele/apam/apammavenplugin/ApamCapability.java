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
package fr.imag.adele.apam.apammavenplugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.CoreMetadataParser;

public class ApamCapability {

	private ComponentDeclaration dcl = null;
	private boolean isFinalized = false;

	private Map<String, String> properties;
	private Map<String, String> propertiesTypes = new HashMap<String, String>();
	private Map<String, String> propertiesDefaults = new HashMap<String, String>();
	private Map<String, String> finalProperties = new HashMap<String, String>();

	public ApamCapability() {
	}

	public ApamCapability(ComponentDeclaration dcl) {
		this.dcl = dcl;
		properties = dcl.getProperties();

		for (PropertyDefinition definition : dcl.getPropertyDefinitions()) {
			propertiesTypes.put(definition.getName().toLowerCase(), definition.getType());
			if (definition.getDefaultValue() != null)
				propertiesDefaults.put(definition.getName().toLowerCase(),
						definition.getDefaultValue());
		}
	}

	public ComponentDeclaration getDcl() {
        return dcl;
	}

	/**
	 * Warning: should be used only once in generateProperty. finalProperties
	 * contains the attributes generated in OBR i.e. the right attributes.
	 * properties contains the attributes found in the xml, i.e. before to check
	 * and before to compute inheritance. At the end of the component processing
	 * we switch in order to use the right attributes if the component is used
	 * after its processing
	 * 
	 * @param attr
	 * @param value
	 */
	public boolean putAttr(String attr, String value) {
		if ((finalProperties.get(attr) != null)
				|| (finalProperties.get(CST.DEFINITION_PREFIX + attr) != null)) {
			CheckObr.error("Attribute " + attr + " already set on "
					+ dcl.getName());
			return false;
		}
		finalProperties.put(attr, value);
		return true;
	}

	public void freeze() {
		isFinalized = true;
		properties = finalProperties;
	}

	public boolean isFinalized() {
		return isFinalized;
	}

	public String getProperty(String name) {
		return (String) properties.get(name);
	}

	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}

	public Set<InterfaceReference> getProvideInterfaces() {
		return dcl.getProvidedResources(InterfaceReference.class);
	}

	public Set<ResourceReference> getProvideResources() {
		return dcl.getProvidedResources();
	}

	public Set<MessageReference> getProvideMessages() {
		return dcl.getProvidedResources(MessageReference.class);
	}

	public String getImplementationClass() {
		if (dcl instanceof AtomicImplementationDeclaration) {
			return ((AtomicImplementationDeclaration) dcl).getClassName();
		} else {
			return null;
		}
	}

	// Return the definition at the current component level
	public String getLocalAttrDefinition(String name) {
		return propertiesTypes.get(name.toLowerCase());
	}

	public String getAttrDefinition(String name) {
		ApamCapability parent = this;
		name = name.toLowerCase() ;
		String defAttr;
		if (Attribute.isFinalAttribute(name)) {
			return "string";
		}
		while (parent != null) {
			defAttr = parent.getLocalAttrDefinition(name);
			if (defAttr != null) {
				return defAttr;
			}
            parent = parent.getGroup();
		}
		return null;
	}

	public String getAttrDefault(String name) {
		return propertiesDefaults.get(name.toLowerCase());
	}

	/**
	 * returns all the attribute that can be found associated with this
	 * component members. i.e. all the actual attributes plus those defined on
	 * component, and those defined above.
	 * 
	 * @return
	 */
	public Map<String, String> getValidAttrNames() {
		Map<String, String> ret = new HashMap<String, String>();

		ret.putAll(propertiesTypes);
		if (getGroup() != null) {
			ret.putAll(getGroup().getValidAttrNames());
		}

		return ret;
	}

	public ApamCapability getGroup() {
        return ApamCapabilityBroker.getGroup(getName());
	}

	public String getName() {
		return dcl.getName();
	}

	/**
	 * return null if Shared is undefined, true of false if it is defined as
	 * true or false.
	 * 
	 * @return
	 */
	public String shared() {
		if (dcl.isDefinedShared()) {
			return Boolean.toString(dcl.isShared());
		}
		return null;
	}

	/**
	 * return null if Instantiable is undefined, true of false if it is defined
	 * as true or false.
	 * 
	 * @return
	 */
	public String instantiable() {
		if (dcl.isDefinedInstantiable()) {
			return Boolean.toString(dcl.isInstantiable());
		}
		return null;
	}

	/**
	 * return null if Singleton is undefined, true of false if it is defined as
	 * true or false.
	 * 
	 * @return
	 */
	public String singleton() {
		if (dcl.isDefinedSingleton()) {
			return Boolean.toString(dcl.isSingleton());
		}
		return null;
	}

}
