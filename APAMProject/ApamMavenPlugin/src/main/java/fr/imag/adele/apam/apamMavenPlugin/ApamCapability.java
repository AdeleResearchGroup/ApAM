package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.util.ArrayList;
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
	
	Map <String, Object> properties ;
	Map <String, Object> finalProperties = new HashMap <String, Object> () ;
	

	private static DataModelHelper dataModelHelper; 
	private static Repository repo;
	private static Resource[] resources;

	public static void init (List<ComponentDeclaration> components, String defaultOBRRepo) {
		ApamCapability.components = components ;

		//public static void init(String defaultOBRRepo) {
		System.out.println("start CheckOBR. Default repo= " + defaultOBRRepo);
		try {
			File theRepo = new File(defaultOBRRepo);
			dataModelHelper =  new DataModelHelperImpl();
			repo = dataModelHelper.repository(theRepo.toURI().toURL());
			resources = repo.getResources();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ApamCapability (String name, Capability cap) {
		this.cap = cap ;
		capabilities.put(name, this) ;
		properties = cap.getPropertiesAsMap();
	}

	public ApamCapability (String name, ComponentDeclaration dcl) {
		this.dcl = dcl ;
		capabilities.put(name, this) ;
		properties = dcl.getProperties();
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
		for (Resource res : resources) {
//			if (reference instanceof SpecificationReference) {
//				if (!OBRGeneratorMojo.bundleDependencies.contains(res.getId()))
//					continue ;
//			}
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
		
		logger.error("     Component " + name + " not found in repository " + repo.getURI());
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
	
	public Map<String, Object> getProperties () {
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
		if (dcl != null) {
			for (PropertyDefinition def : dcl.getPropertyDefinitions()) {
				if (def.getName().equals(name)) return def.getType() ;
			}
		}
		for (String def : getProperties().keySet()) {
			if (def.startsWith(CST.A_DEFINITION_PREFIX) 
					&& def.substring(11).equals(name)) 
				return getProperty(def) ;
		}
		return null ;
	}

	public Set<String> getAttrDefinitions () {
		Set<String> ret = new HashSet<String> () ;
		if (dcl != null) {
			for (PropertyDefinition def : dcl.getPropertyDefinitions()) {
				ret.add(def.getName()) ;
			}
			return ret ;
		}
		for (String def : getProperties().keySet()) {
			if (def.startsWith(CST.A_DEFINITION_PREFIX)) 
				ret.add(def.substring(11)) ;
		}
		return ret ;
	}

	/**
	 * returns all the attribute that can be found associated with the component.
	 * i.e. all the actual attributes plus those defined above and not yet instantiated.
	 * @return
	 */
	public Set<String> getValidAttrNames () {
		Set<String> ret = new HashSet<String> () ;
		for (String predef : CST.predefAttributes)
			ret.add(predef);
		
		if (dcl != null) {
			ret.addAll(getProperties().keySet()) ;
			for (PropertyDefinition def : dcl.getPropertyDefinitions()) {
				ret.add(def.getName()) ;
			}
			return ret ;
		}
		
		for (String def : getProperties().keySet()) {
			if (def.startsWith(CST.A_DEFINITION_PREFIX)) 
				ret.add(def.substring(11)) ;
			else ret.add(def) ;			
		}
		if (getGroup() == null)
			return ret ;
		else return getGroup().getValidAttrNames() ;
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
	public boolean putAttr (String attr, Object value) {
		if (finalProperties.get(attr) != null) {
			return false ;
		}
		finalProperties.put(attr, value) ;
		return true ;
	}
	
	public void finalize () {
		properties = finalProperties ;
	}
	
}
