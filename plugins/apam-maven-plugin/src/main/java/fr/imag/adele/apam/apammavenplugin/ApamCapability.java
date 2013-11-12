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

	private static Map<String, ApamCapability> capabilities = new HashMap<String, ApamCapability>();
	private static Set<String> missing = new HashSet<String>();
	
	public ComponentDeclaration dcl = null ;
	private boolean isFinalized		= false;

	private Map <String, String> properties ;
	private Map <String,String>  propertiesTypes 	=  new HashMap <String, String> ();
	private Map <String,String>  propertiesDefaults =  new HashMap <String, String> ();
	private Map <String, String> finalProperties = new HashMap <String, String> () ;

	//Only to return a true value
	public static ApamCapability trueCap = new ApamCapability();
	private ApamCapability () { } ;

	
	public ApamCapability (ComponentDeclaration dcl) {
		this.dcl = dcl ;
		capabilities.put(dcl.getName(), this) ;
		properties = dcl.getProperties();

		for (PropertyDefinition definition : dcl.getPropertyDefinitions()) {
			propertiesTypes.put(definition.getName(), definition.getType());
			if (definition.getDefaultValue() != null)
				propertiesDefaults.put(definition.getName(),definition.getDefaultValue());
		}
	}

	public static void init (List<ComponentDeclaration> components, List<ComponentDeclaration> dependencies) {
		capabilities.clear() ;
		missing.clear() ;
		for (ComponentDeclaration dcl : components) {
			new ApamCapability(dcl) ;
		}
		for (ComponentDeclaration dcl : dependencies) {
			if (!capabilities.containsKey(dcl.getName()))
				new ApamCapability(dcl) ;
		}
	}

	public static ApamCapability get(String name) {
		if (name == null) {
		    return null ;
		}
		ApamCapability cap = capabilities.get(name) ;
		if (cap == null && !missing.contains(name)) {
			missing.add(name) ;
			CheckObr.error("Component " + name + " is not in your Maven dependencies.") ;
		}
		return cap;
	}

	
	public static ApamCapability get(ComponentReference<?> reference) {
		if (reference == null) { 
		    return null ;
		}
		if (reference.getName().equals(CoreMetadataParser.UNDEFINED)) {
		    return null;
		}
		
		ApamCapability cap = capabilities.get(reference.getName()) ;
		if (cap == null && !missing.contains(reference.getName())) {
			missing.add(reference.getName()) ;
			CheckObr.error("Component " + reference.getName() + " is not in your Maven dependencies.") ;
		}
		return cap;
	}

	public static ComponentDeclaration getDcl(String name) {
		if (name == null) { 
		    return null ;
		}
		if (name.equals(CoreMetadataParser.UNDEFINED)) {
		    return null;
		}

		if (capabilities.get(name) != null) {
			return capabilities.get(name).dcl;
		}
		return null ;
	}

	public static ComponentDeclaration getDcl(ComponentReference<?> reference) {
		if (reference == null) {
		    return null ;
		}
		if (reference.getName().equals(CoreMetadataParser.UNDEFINED)) {
		    return null;
		}
		
		if (capabilities.get(reference.getName()) != null) {
			return capabilities.get(reference.getName()).dcl;
		}
		return null ;
	}

	/**
	 * Warning: should be used only once in generateProperty.
	 * finalProperties contains the attributes generated in OBR i.e. the right attributes.
	 * properties contains the attributes found in the xml, i.e. before to check and before to compute inheritance.
	 * At the end of the component processing we switch in order to use the right attributes 
	 *          if the component is used after its processing
	 * @param attr
	 * @param value
	 */
	public boolean putAttr (String attr, String value) {
		if ((finalProperties.get(attr) != null) || (finalProperties.get(CST.DEFINITION_PREFIX + attr) != null)) {
			CheckObr.error("Attribute " + attr + " already set on " + dcl.getName() ) ;
			return false ;
		}
		finalProperties.put(attr, value) ;
		return true ;
	}
	
	public void freeze () {
		isFinalized = true;
		properties = finalProperties ;
	}

	public boolean isFinalized() {
		return isFinalized;
	}


	public String getProperty (String name) {
		return (String)properties.get(name) ;
	}

	public Map<String, String> getProperties () {
		return Collections.unmodifiableMap(properties);	
	}

	public Set<InterfaceReference> getProvideInterfaces () {
		return dcl.getProvidedResources(InterfaceReference.class) ;
	}

	public Set<ResourceReference> getProvideResources () {
		return dcl.getProvidedResources() ;
	}


	public Set<MessageReference> getProvideMessages () {
		return dcl.getProvidedResources(MessageReference.class) ;
	}
	
	public String getImplementationClass() {
		if (dcl instanceof AtomicImplementationDeclaration) {
			return ((AtomicImplementationDeclaration) dcl).getClassName();
		}
		else {
			return null;
		}
	}

	//Return the definition at the current component level 
	public String getLocalAttrDefinition (String name) {
		return propertiesTypes.get(name);
	}

	public String getAttrDefinition (String name) {
		ApamCapability group = this ;
		String defAttr ;
		if (Attribute.isFinalAttribute(name)) {
			return "string" ;
		}
		while (group != null) {
			defAttr = group.getLocalAttrDefinition(name)  ;
			if (defAttr != null) {
			    return defAttr ;
			}
			group = group.getGroup() ;
		}
		return null ;
	}


	public String getAttrDefault (String name) {
		return propertiesDefaults.get(name);
	}

	/**
	 * returns all the attribute that can be found associated with this component members.
	 * i.e. all the actual attributes plus those defined on component, 
	 * and those defined above.
	 * @return
	 */
	public Map<String, String> getValidAttrNames () {
		Map<String, String> ret = new HashMap<String, String> () ;

		ret.putAll(propertiesTypes);
		if (getGroup() != null) {
			ret.putAll(getGroup().getValidAttrNames()) ;
		}

		return ret ;
	}


	public ApamCapability getGroup () {
		if (dcl instanceof SpecificationDeclaration) {
		    return null ;
		}
		if (dcl instanceof ImplementationDeclaration) {
			if (((ImplementationDeclaration)dcl).getSpecification() == null) {
				return null ;
			}
			return get(((ImplementationDeclaration)dcl).getSpecification()) ;
		}
		if (dcl instanceof InstanceDeclaration) {
		    return get(((InstanceDeclaration)dcl).getImplementation()) ;
		}
		return null ;
	}
	
	public String getName () {
		return dcl.getName() ;
	}

	/**
	 * return null if Shared is undefined, 
	 * true of false if it is defined as true or false.
	 * @return
	 */
	public String shared () {
			if (dcl.isDefinedShared()) {
				return Boolean.toString(dcl.isShared()) ;
			}
			return null ;
	}

	/**
	 * return null if Instantiable is undefined, 
	 * true of false if it is defined as true or false.
	 * @return
	 */
	public String instantiable () {
			if (dcl.isDefinedInstantiable()) {
				return Boolean.toString(dcl.isInstantiable()) ;
			}
			return null ;
	}

	/**
	 * return null if Singleton is undefined, 
	 * true of false if it is defined as true or false.
	 * @return
	 */
	public String singleton () {
			if (dcl.isDefinedSingleton()) {
				return Boolean.toString(dcl.isSingleton()) ;
			}
			return null ;
	}

	
}

