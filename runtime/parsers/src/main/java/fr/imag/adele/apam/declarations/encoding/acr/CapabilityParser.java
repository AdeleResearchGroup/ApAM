package fr.imag.adele.apam.declarations.encoding.acr;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.PackageReference;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.Reporter;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.util.Attribute;

public class CapabilityParser implements Decoder<Capability> {

	private Reporter reporter;
	
	private ComponentDeclaration component;
	private Map<String, Property>  properties;
	
	
	
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
            
		info("parsing OBR for : " + componentName + " of type " + componentkind);

		switch(kind(componentkind)) {
			case SPECIFICATION:
            	component = new SpecificationDeclaration(componentName);
				break;
			case IMPLEMENTATION:
		        if (isDefined(CST.APAM_COMPOSITE) && flag(CST.APAM_COMPOSITE)) {
               	 
		        	SpecificationReference specification = reference(CST.PROVIDE_SPECIFICATION, ComponentKind.SPECIFICATION);
		        	String range = property(CST.REQUIRE_VERSION);
		        	SpecificationReference.Versioned specificationVersion = specification != null ? specification.range(range) : null;

		        	ComponentReference<?> main = reference(CST.APAM_MAIN_COMPONENT,ComponentKind.COMPONENT);

		        	component = new CompositeDeclaration(componentName,specificationVersion,main);
		        } 
		        else {

		        	SpecificationReference specification = reference(CST.PROVIDE_SPECIFICATION, ComponentKind.SPECIFICATION);
		        	String range = property(CST.REQUIRE_VERSION);
		        	SpecificationReference.Versioned specificationVersion = specification != null ? specification.range(range) : null;
		        	
		        	AtomicImplementationDeclaration.CodeReflection instrumentedClass =  new MinimalClassReflection(property(CST.PROVIDE_CLASSNAME));
		            component = new AtomicImplementationDeclaration(componentName, specificationVersion, instrumentedClass);
		        }
				break;
			case INSTANCE:
	            ImplementationReference<?> implementation = reference(CST.IMPLNAME,ComponentKind.IMPLEMENTATION);
	        	String range = property(CST.REQUIRE_VERSION);
	            component = new InstanceDeclaration(implementation.range(range), componentName, null);
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
        	component.setDefinedSingleton(flag(CST.SINGLETON));
        }
        
        if(isDefined(CST.SHARED)) {
        	component.setDefinedShared(flag(CST.SHARED));
        }
        
        if(isDefined(CST.INSTANTIABLE)) {
        	component.setDefinedInstantiable(flag(CST.INSTANTIABLE));
        }
    
        if(isDefined(CST.VERSION)) { // get the OSGi Versioned of the component
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
            	info("Property "+propertyName+" is a definition of property");
                component.getPropertyDefinitions().add(definition(propertyName));
        	}

        	if (isRelationDefinition(propertyName)) {
        		info("Relation "+propertyName+" is a definition of relation");
                component.getDependencies().add(relation(propertyName));
        	}
        
        	if (! isPredefinedProperty(propertyName)) {
                info("Property " + propertyName + " is not a reserved keyword, it must be an user defined property");
                component.getProperties().put(propertyName, property(propertyName));
        	}
        }
                
        info("Capability matching found : "+component.getName());
        
        return (result());
	}

	private final boolean isPropertyDefinition(String propertyName) {
    	return propertyName.startsWith(CST.DEFINITION_PREFIX);
	}

	private final PropertyDefinition definition(String propertyName) {
    	String property = propertyName.substring(CST.DEFINITION_PREFIX.length());
    	return new PropertyDefinition(component,property,type(propertyName),property(propertyName),null,null,null);
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
    		return (!encodedReference.isEmpty()) ? new InterfaceReference(encodedReference) : new UndefinedReference(new InterfaceReference("unknown"));
    	}
    	else if (encodedReference.startsWith("{"+ComponentParser.MESSAGE+"}")) {
    		encodedReference = encodedReference.substring(("{"+ComponentParser.MESSAGE+"}").length());
    		return (!encodedReference.isEmpty()) ? new MessageReference(encodedReference) : new UndefinedReference(new MessageReference("unknown"));
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

	private final static Pattern LIST_PATTERN = Pattern.compile(",");
	
	private final String[] list(String propertyName) {
		return LIST_PATTERN.split(property(propertyName));	
	}

	@SuppressWarnings("unchecked")
	private final <T extends ComponentReference<?>> T reference(String propertyName, ComponentKind kind) {
		String component = property(propertyName);
    	return component != null ? (T) kind.createReference(component) : null;
	}
	
	public final void error(String message) {
		reporter.report(Severity.ERROR, message);
	}
	
	public final void warning(String message) {
		reporter.report(Severity.WARNING, message);
	}

	public final void info(String message) {
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
	 * Convert a component kind as represented internal
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
