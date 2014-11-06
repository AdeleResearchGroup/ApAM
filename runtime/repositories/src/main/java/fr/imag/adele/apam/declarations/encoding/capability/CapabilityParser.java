package fr.imag.adele.apam.declarations.encoding.capability;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.PackageReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.util.Attribute;

/**
 * Parses a component declaration encoded in a capability in an OSGi Bundle Repository.
 * 
 * This is the format used by the Apam Component Repository at build time, and the OBR
 * manager at runtime.
 * 
 * @author vega
 *
 */
public class CapabilityParser implements Decoder<Capability> {

	private Reporter 				reporter;
	
	private ComponentDeclaration 	component;
	private Map<String, Property>	properties;
	
	
	
	@Override
	public ComponentDeclaration decode(Capability capability, Reporter reporter) {

		this.reporter 	= reporter;
		this.component	= null;
	
		
		if (capability != null && ! CST.CAPABILITY_COMPONENT.equals(capability.getName())) {
            info("Capability " + (capability == null ? null : capability.getName()) + " is not an apam component");
            return result();
		}
		
		if( capability.getProperties() == null || capability.getProperties().length == 0) {
            warning("No properties found for the capability : "+capability.getName());
            return result();
        }

		/*
		 * Intialiaze the property map
		 */
		this.properties = new HashMap<String, Property>();
		for(Property property : capability.getProperties() ) {
			properties.put(property.getName(), property);
		}
	        

		/*
		 * Create the appropriate component declaration, depending on the kind of component
		 */
		String componentName 	= property(CST.NAME);
		String componentkind	= property(CST.COMPONENT_TYPE);
            
		info("parsing ACR capability for component : " + componentName + " of type " + componentkind);

		switch(kind(componentkind)) {
			case SPECIFICATION:
            	component = new SpecificationDeclaration(componentName);
				break;
			case IMPLEMENTATION:
		        if (isDefined(CST.APAM_COMPOSITE) && flag(CST.APAM_COMPOSITE)) {
               	 
		        	SpecificationReference specification 						= reference(CST.PROVIDE_SPECIFICATION, ComponentKind.SPECIFICATION);
		        	String range 												= property(CST.REQUIRE_VERSION);
		        	Versioned<SpecificationDeclaration> specificationVersion	= specification != null ? Versioned.range(specification,range) : null;

		        	ComponentReference<?> main = reference(CST.APAM_MAIN_COMPONENT,ComponentKind.COMPONENT);

		        	component = new CompositeDeclaration(componentName,specificationVersion,main);
		        } 
		        else {

		        	SpecificationReference specification						= reference(CST.PROVIDE_SPECIFICATION, ComponentKind.SPECIFICATION);
		        	String range 												= property(CST.REQUIRE_VERSION);
		        	Versioned<SpecificationDeclaration> specificationVersion	= specification != null ? Versioned.range(specification,range) : null;
		        	
		        	AtomicImplementationDeclaration.CodeReflection instrumentedClass =  new MinimalClassReflection(property(CST.PROVIDE_CLASSNAME));
		            component = new AtomicImplementationDeclaration(componentName, specificationVersion, instrumentedClass);
		        }
				break;
			case INSTANCE:
	            ImplementationReference<ImplementationDeclaration> implementation 	= reference(CST.IMPLNAME,ComponentKind.IMPLEMENTATION);
	        	String range 														= property(CST.REQUIRE_VERSION);
	           	Versioned<ImplementationDeclaration> implementationVersion			= Versioned.range(implementation,range);

	        	component = new InstanceDeclaration(implementationVersion, componentName);
				break;
			default:
		}
		
        
        /*
         * Verify we could create the declaration
         */
        if (component == null) {
        	warning("Unknown apam component type : "+componentkind+" for "+componentName);
        	return result();
		}

        /*
         * Add predefined properties
         */
        if(isDefined(CST.SINGLETON)) {
        	component.setSingleton(flag(CST.SINGLETON));
        }
        
        if(isDefined(CST.SHARED)) {
        	component.setShared(flag(CST.SHARED));
        }
        
        if(isDefined(CST.INSTANTIABLE)) {
        	component.setInstantiable(flag(CST.INSTANTIABLE));
        }

        if(isDefined(CST.VERSION)) {
            component.getProperties().put(CST.VERSION, property(CST.VERSION));
        }

        // Check the provides interfaces and messages
        if(isDefined(CST.PROVIDE_INTERFACES)) {
            for(String ref : list(CST.PROVIDE_INTERFACES)) {
            	component.getProvidedResources().add(new InterfaceReference(ref));
            }
        }
        
        if(isDefined(CST.PROVIDE_MESSAGES)) {
            for(String ref : list(CST.PROVIDE_MESSAGES)) {
            	component.getProvidedResources().add(new MessageReference(ref));
            }
        }
        
        // Add encoded properties, definitions and relations

        for (String propertyName : properties.keySet()) {
            
        	if (isPropertyDefinition(propertyName)) {
                component.getPropertyDefinitions().add(definition(propertyName));
        	}

        	if (isRelationDefinition(propertyName)) {
                component.getRelations().add(relation(propertyName));
        	}
        
        	if (! isPredefinedProperty(propertyName)) {
                component.getProperties().put(propertyName, property(propertyName));
        	}
        }
                
        return (result());
	}

	private final boolean isPropertyDefinition(String propertyName) {
    	return propertyName.startsWith(CST.DEFINITION_PREFIX);
	}

	private final PropertyDefinition definition(String propertyName) {
    	String property = propertyName.substring(CST.DEFINITION_PREFIX.length());
    	return new PropertyDefinition(component.getReference(),property,type(propertyName),property(propertyName));
	}


	private final boolean isPredefinedProperty(String propertyName) {
        return 	Attribute.isFinalAttribute(propertyName) ||
        		Attribute.isBuiltAttribute(propertyName) ||
        		!Attribute.isInheritedAttribute(propertyName);
	}
	
	private final boolean isRelationDefinition(String propertyName) {
    	return propertyName.startsWith(CST.RELATION_PREFIX);
	}
	
	private final RelationDeclaration relation(String propertyName) {
    	String relation = propertyName.substring(CST.RELATION_PREFIX.length());
        return new RelationDeclaration(component.getReference(),relation,reference(propertyName),false);
		
	}
	
	private final ResolvableReference reference(String propertyName) {

		String encodedReference = property(propertyName);

    	if (encodedReference.startsWith("{"+ComponentParser.INTERFACE+"}")) {
    		encodedReference = encodedReference.substring(("{"+ComponentParser.INTERFACE+"}").length());
    		return (!encodedReference.isEmpty()) ? new InterfaceReference(encodedReference) : new UnknownReference(new InterfaceReference("unknown"));
    	}
    	else if (encodedReference.startsWith("{"+ComponentParser.MESSAGE+"}")) {
    		encodedReference = encodedReference.substring(("{"+ComponentParser.MESSAGE+"}").length());
    		return (!encodedReference.isEmpty()) ? new MessageReference(encodedReference) : new UnknownReference(new MessageReference("unknown"));
    	}
    	else if (encodedReference.startsWith("{"+ComponentParser.PACKAGE+"}")) {
    		encodedReference = encodedReference.substring(("{"+ComponentParser.PACKAGE+"}").length());
    		return new PackageReference(encodedReference);
    	}
    	else {
    		return new ComponentReference<ComponentDeclaration>(encodedReference);
    	}
	}
	

    /**
     * Free references to all intermediate objects (to allow reuse of this parser) and
     * returns the final result
     */
	private ComponentDeclaration result() {
        this.properties 	= null;
        this.reporter	= null;
        
        ComponentDeclaration result = component;
        this.component		= null;
        
        return result;
	}

	private final String property(String propertyName) {
		Property property = properties.get(propertyName); 
		return property != null ? property.getValue() : null;
	}

	private final String type(String propertyName) {
		Property property = properties.get(propertyName); 
		return property != null ? property.getType() : null;
	}

	private final boolean isDefined(String propertyName) {
		return properties.containsKey(propertyName);
	}
 	
	private final boolean flag(String propertyName) {
		return Boolean.valueOf(property(propertyName));
	}

	/**
	 * Get a list of strings
	 */
	private static final Pattern SIMPLE_LIST_PATTERN 	= Pattern.compile(",");
	private static final Pattern TRIMMED_LIST_PATTERN 	= Pattern.compile("\\s*,\\s*");

	private List<String> list(String propertyName) {
		return list(propertyName,true);
	}
	
	private List<String> list(String propertyName, boolean trimmed) {
		Pattern pattern 	= trimmed ? TRIMMED_LIST_PATTERN : SIMPLE_LIST_PATTERN;
		String encodedList	= property(propertyName);
		return encodedList != null ? Arrays.asList(pattern.split(delimited(encodedList))) : Collections.<String> emptyList();
	}

	/**
	 * Skip all optional leading and ending delimiters for lists
	 */
	private String delimited(String encodedList) {
		
		if (encodedList.startsWith("{")) {
			if (encodedList.endsWith("}")) {
				return encodedList.substring(1, encodedList.length() - 1).trim();
			}
			else {
				error("Invalid string. \"}\" missing: " + encodedList);
				return encodedList.substring(1).trim();
			}
		}
		
		return encodedList.trim();
	}
	
	@SuppressWarnings("unchecked")
	private final <T extends ComponentReference<?>> T reference(String propertyName, ComponentKind kind) {
		String component = property(propertyName);
    	return component != null ? (T) kind.createReference(component) : null;
	}
	
	private final void error(String message) {
		reporter.report(Severity.ERROR, message);
	}
	
	private final void warning(String message) {
		reporter.report(Severity.WARNING, message);
	}

	private final void info(String message) {
		reporter.report(Severity.INFO, message);
	}

	/**
	 * Whether this capability represents an Apam component
	 */
	public static boolean isComponent(Capability capability) {
		
		return	capability != null &&
				CST.CAPABILITY_COMPONENT.equals(capability.getName()) &&
				capability.getProperties() != null &&
				capability.getProperties().length> 0 &&
				capability.getPropertiesAsMap().get(CST.NAME) != null &&
				capability.getPropertiesAsMap().get(CST.COMPONENT_TYPE) != null;
	}

	/**
	 * The Apam component represented by this capability
	 */
	public static ComponentReference<?> getComponent(Capability capability) {
		
		if (! isComponent(capability))
			return null;

		ComponentKind kind	= kind((String) capability.getPropertiesAsMap().get(CST.COMPONENT_TYPE));
	    return kind != null ? kind.createReference((String) capability.getPropertiesAsMap().get(CST.NAME)) : null;
	}
	
	/**
	 * Convert a component kind from its internal representation
	 */
	private static ComponentKind kind(String componentkind) {
		
		if (CST.SPECIFICATION.equals(componentkind)) {
			return ComponentKind.SPECIFICATION;
	    } 
		else if (CST.IMPLEMENTATION.equals(componentkind)) {
			return ComponentKind.IMPLEMENTATION;
	    } 
	    else if (CST.INSTANCE.equals(componentkind)) {
	    	return ComponentKind.INSTANCE;
	    }
	    else {
	    	return null;
	    }
	}
}
