package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;



public class ApamCapability {

	private static Logger logger = LoggerFactory.getLogger(ApamCapability.class);

	private static Map<String, ApamCapability> capabilities = new HashMap<String, ApamCapability>();
	private static List <ComponentDeclaration> components   = new ArrayList<ComponentDeclaration>();

	Capability cap = null ;
	ComponentDeclaration dcl = null ;
	
	private Map <String, String> properties ;
	private Map <String,String>  propertiesTypes 	=  new HashMap <String, String> ();
	private Map <String,String>  propertiesDefaults =  new HashMap <String, String> ();
	
	private Map <String, String> finalProperties = new HashMap <String, String> () ;
	
	//If true, no obr repository found. Cannot look for the other components
	private static boolean noRepository = true ;

	private static DataModelHelper dataModelHelper; 
	private static String repos = "";
	private static List<Resource> resources = new ArrayList<Resource> ();

	public static void init (List<ComponentDeclaration> components, List<URL> OBRRepos) {
		ApamCapability.components = components ;

		//First, compute the list of available resources
		try {
			for (URL repo : OBRRepos){
				//File theRepo = new File();
				if (repo.getFile().isEmpty()) {
					break ;
				}
				repos += repo.toString() ;
				noRepository = false ;
			dataModelHelper =  new DataModelHelperImpl();
				Repository repoModel = dataModelHelper.repository(repo.toURI().toURL());
				resources.addAll(Arrays.asList(repoModel.getResources()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//public static void init(String defaultOBRRepo) {
		System.out.println("start CheckOBR. Used OBR repositories: " + OBRRepos);


	}

	public ApamCapability (String name, Capability cap) {
		this.cap = cap ;
		capabilities.put(name, this) ;
		properties = cap.getPropertiesAsMap();
		
		for (Property property : cap.getProperties()) {
			if (property.getName().startsWith(CST.A_DEFINITION_PREFIX)) {
				String key = property.getName().substring(11);
				propertiesTypes.put(key, property.getType());
				if (!property.getValue().equals(""))
					propertiesDefaults.put(key,property.getValue());
			}
		}
	}

	public ApamCapability (String name, ComponentDeclaration dcl) {
		this.dcl = dcl ;
		capabilities.put(name, this) ;
		properties = dcl.getProperties();
		
		for (PropertyDefinition definition : dcl.getPropertyDefinitions()) {
			propertiesTypes.put(definition.getName(), definition.getType());
			if (definition.getDefaultValue() != null)
				propertiesDefaults.put(definition.getName(),definition.getDefaultValue());
		}
	}

	public String getName () {
		if (dcl != null)
			return dcl.getName() ;
		return cap.getName();
	}
	
	public static ApamCapability get(ComponentReference<?> reference) {
		String name = reference.getName();
		if (capabilities.containsKey(name))
			return capabilities.get(name);
		
		//Look for in the components in this bundle
		for (ComponentDeclaration component : components) {
			if (component.getName().equals(name))
				return new ApamCapability(name, component) ;
		}
		
		//look in OBR
		if (noRepository) return null ;
		for (Resource res : resources) {
			for (Capability cap : res.getCapabilities()) {
				if (cap.getName().equals(CST.CAPABILITY_COMPONENT)
						&& (getAttributeInCap(cap, "name").equals(name))) {
					//look if this component is in the list of maven dependencies; if so, it must be the same version
					if (OBRGeneratorMojo.versionRange.get(name) != null) {
						if (!OBRGeneratorMojo.bundleDependencies.contains(res.getId())) {
							System.out.println("Bad version for " + name 
									+ " expected : " + OBRGeneratorMojo.versionRange.get(name) 
									+ " found " + res.getId());
							continue ;
						}
					}
					System.out.println("     Component " + name + " found in bundle " + res.getId());
					//CheckObr.readCapabilities.put(name, cap);
					//return cap;
					return new ApamCapability(name, cap) ;
				}
			}
		}
		
		logger.error("     Component " + name + " not found in repositories " + repos);
		return null;
	}

	@SuppressWarnings("unchecked")
	private  <R extends ResourceReference> Set<R> asSet(String set, Class<R> kind) {
		Set<ResourceReference> references = new HashSet<ResourceReference>();
		for (String  id : Util.split(set)) {
			if (InterfaceReference.class.isAssignableFrom(kind))
				references.add(new InterfaceReference(id));
			if (MessageReference.class.isAssignableFrom(kind))
				references.add(new MessageReference(id));
		}
		return (Set<R>) references;
	}

	private static String getAttributeInCap(Capability cap, String name) {
		if (cap == null)
			return null;
		Map<String, Object> props = cap.getPropertiesAsMap();
		String prop =  (String) props.get(name);
		if (prop == null)
			return null;
		return prop;
	}
	
	public String getProperty (String name) {
		return (String)properties.get(name) ;
	}
	
	public Map<String, String> getProperties () {
		return Collections.unmodifiableMap(properties);	
	}
	
	public Set<InterfaceReference> getProvideInterfaces () {
		if (dcl != null) {
			return dcl.getProvidedResources(InterfaceReference.class) ;
		}
		return asSet(getProperty(CST.A_PROVIDE_INTERFACES), InterfaceReference.class);
	}
	
	public Set<ResourceReference> getProvideResources () {
		if (dcl != null) {
			return dcl.getProvidedResources() ;
		}
		Set<ResourceReference> references = new HashSet<ResourceReference>();
		references.addAll(getProvideInterfaces()) ;
		references.addAll(getProvideMessages()) ;
		return references;
	}

	
	public Set<MessageReference> getProvideMessages () {
		if (dcl != null) {
			return dcl.getProvidedResources(MessageReference.class) ;
		}
		return asSet(getAttributeInCap(cap,
				CST.A_PROVIDE_MESSAGES), MessageReference.class);
	}

	public String getAttrDefinition (String name) {
		return propertiesTypes.get(name);
	}

	public String getAttrDefault (String name) {
		return propertiesDefaults.get(name);
	}

//		if (dcl != null) {
//			for (PropertyDefinition def : dcl.getPropertyDefinitions()) {
//				if (def.getName().equals(name)) return def.getType() ;
//			}
//		}
//		for (String def : getProperties().keySet()) {
//			if (def.startsWith(CST.A_DEFINITION_PREFIX) 
//					&& def.substring(11).equals(name)) 
//				return getProperty(def) ;
//		}
//		return null ;
//	}

//	public Set<String> getAttrDefinitions () {
//		Set<String> ret = new HashSet<String> () ;
//		if (dcl != null) {
//			for (PropertyDefinition def : dcl.getPropertyDefinitions()) {
//				ret.add(def.getName()) ;
//			}
//			return ret ;
//		}
//		for (String def : getProperties().keySet()) {
//			if (def.startsWith(CST.A_DEFINITION_PREFIX)) 
//				ret.add(def.substring(11)) ;
//		}
//		return ret ;
//	}

	/**
	 * returns all the attribute that can be found associated with this component members.
	 * i.e. all the actual attributes plus those defined on component, 
	 * and those defined above.
	 * @return
	 */
	public Map<String, String> getValidAttrNames () {
		Map<String, String> ret = new HashMap<String, String> () ;
		for (String predef : CST.predefAttributes)
			ret.put(predef, "string");
		
		ret.putAll(propertiesTypes);
		if (getGroup() != null)
			ret.putAll(getGroup().getValidAttrNames()) ;

		return ret ;
	}

	public ApamCapability getGroup () {
		if (dcl != null) {
			if (dcl instanceof SpecificationDeclaration) return null ;
			if (dcl instanceof ImplementationDeclaration) {
				if (((ImplementationDeclaration)dcl).getSpecification() == null)
					return null ;
				return get(((ImplementationDeclaration)dcl).getSpecification()) ;
			}
			if (dcl instanceof InstanceDeclaration) return get(((InstanceDeclaration)dcl).getImplementation()) ;
		}
		if (getProperty(CST.COMPONENT_TYPE).equals(CST.INSTANCE)) {
			if (getProperty(CST.A_IMPLNAME) == null) return null ;
			ImplementationReference implRef = new ImplementationReference(getProperty(CST.A_IMPLNAME)) ;
			return (get(implRef)) ;
		}
		if (getProperty(CST.COMPONENT_TYPE).equals(CST.IMPLEMENTATION)) {
			if (getProperty(CST.A_SPECNAME) == null) return null ;
			SpecificationReference specRef = new SpecificationReference(getProperty(CST.A_SPECNAME)) ;
			return (get(specRef)) ;
		}
		if (getProperty(CST.COMPONENT_TYPE).equals(CST.SPECIFICATION)) {
			return null ;
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
		if (finalProperties.get(attr) != null) {
			CheckObr.warning("Attribute " + attr + " already set on " + getName() 
					+ " (old value=" + finalProperties.get(attr) + " new value=" + value + ")") ;
			return false ;
		}
		finalProperties.put(attr, value) ;
		return true ;
	}
	
	public void finalize () {
		properties = finalProperties ;
	}
	
}
