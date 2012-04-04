package fr.imag.adele.apam.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration.Instrumentation;
import fr.imag.adele.apam.core.ComponentDeclaration;
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
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.AbstractProducer;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler.Severity;

/**
 * Parse an APAM declaration from its iPojo metadata representation. 
 * 
 * Notice that this parser tries to build a representation of the metadata declarations even in the presence of 
 * errors. It is up to the error handler to abort parsing if necessary by throwing unrecoverable parsing exceptions.
 * It will add place holders for missing information that can be verified after parsing by another tool.
 * 
 * @author vega
 *
 */
public class CoreMetadataParser implements CoreParser {

    /**
     * Constants defining the different element and attributes
     */
    public static final String APAM 				= 	"fr.imag.adele.apam";
    public static final String SPECIFICATION 		= 	"specification";
    public static final String IMPLEMENTATION 		= 	"implementation";
    public static final String COMPOSITE 			= 	"composite";
    public static final String INSTANCE 			= 	"instance";
    public static final String DEFINITIONS 			= 	"definitions";
    public static final String DEFINITION 			= 	"definition";
    public static final String PROPERTIES 			= 	"properties";
    public static final String PROPERTY 			= 	"property";
    public static final String DEPENDENCIES 		= 	"dependencies";
    public static final String INTERFACE 			= 	"interface";
    public static final String MESSAGE 				= 	"message";
    public static final String CONSTRAINTS 			= 	"constraints";
    public static final String PREFERENCES 			= 	"preferences";

    public static final String ATT_NAME 				= 	"name";
    public static final String ATT_CLASSNAME 			= 	"classname";
    public static final String ATT_SPECIFICATION		= 	"specification";
    public static final String ATT_MAIN_IMPLEMENTATION	= 	"mainImplem";
    public static final String ATT_IMPLEMENTATION		= 	"implementation";
    public static final String ATT_INTERFACES 			= 	"interfaces";
    public static final String ATT_MESSAGES 			= 	"messages";
    public static final String ATT_TYPE 				= 	"type";
    public static final String ATT_VALUE 				= 	"value";
    public static final String ATT_FIELD 				= 	"field";
    public static final String ATT_ID 					= 	"id";
    public static final String ATT_MULTIPLE				= 	"multiple";
    public static final String ATT_FILTER				= 	"filter";


    /**
     * The parsed metatadata
     */
    private Element metadata;

    /**
     * The last parsed declarations
     */
    private Set<ComponentDeclaration> declaredElements;

    /**
     * The currently used error handler
     */
    private ErrorHandler errorHandler;


    public CoreMetadataParser(Element metadata) {
        setMetadata(metadata);
    }

    /**
     * Initialize parser with the given metadata and instrumentation code
     */
    public synchronized void setMetadata(Element metadata) {
        this.metadata 		= metadata;
        declaredElements	= null;
    }

    /**
     * Parse the ipojo metadata to get the component declarations
     */
    @Override
    public synchronized Set<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler) {
        if (declaredElements != null)
            return declaredElements;

        declaredElements 	= new HashSet<ComponentDeclaration>();
        this.errorHandler		= errorHandler;
        for (Element element : metadata.getElements()) {

            /*
             * Ignore not APAM elements 
             */
            if (! CoreMetadataParser.isApamDefinition(element))
                continue;

            ComponentDeclaration component = null;
            /*
             * switch depending on component type
             */
            if (CoreMetadataParser.isSpecification(element))
            	component = parseSpecification(element);

            if (CoreMetadataParser.isPrimitiveImplementation(element))
            	component = parsePrimitive(element);

            if (CoreMetadataParser.isCompositeImplementation(element))
            	component = parseComposite(element);

            if (CoreMetadataParser.isInstance(element))
            	component = parseInstance(element);
            
            if (component != null)
            	declaredElements.add(component);
        }

        // Release references once the parsed data is cached
        metadata 			= null;
        this.errorHandler 	= null;

        return declaredElements;
    }

    /**
     * Parse an specification declaration
     */
    private SpecificationDeclaration parseSpecification(Element element) {
    	
        SpecificationDeclaration declaration = new SpecificationDeclaration(parseName(element));
        parseComponent(element,declaration);
        return declaration;
    }

    /**
     * Parse an atomic implementation declaration
     */
    private AtomicImplementationDeclaration parsePrimitive(Element element) {
    	
    	String name 							= parseName(element);
    	SpecificationReference specification	= parseSpecificationReference(element,CoreMetadataParser.ATT_SPECIFICATION,false);
    	
    	String className						= parseString(element,ATT_CLASSNAME,true);

    	/*
         * load Pojo instrumentation metadata
         */
        PojoMetadata pojoMetadata	= null;
        try {
        	pojoMetadata = new PojoMetadata(element);
        } catch (Exception ignoredException) {
        }


		Instrumentation instrumentation = new ApamIpojoInstrumentation(className,pojoMetadata); 
    	
        AtomicImplementationDeclaration declaration = new AtomicImplementationDeclaration(name,specification,instrumentation);
        parseComponent(element,declaration);

        // if not explicitly provided, get all the implemented interfaces.
        if (declaration.getProvidedResources().isEmpty() && pojoMetadata != null ) {
            for (String implementedInterface : pojoMetadata.getInterfaces()) {
                if (!implementedInterface.startsWith("java.lang"))
                	declaration.getProvidedResources().add(new InterfaceReference(implementedInterface));
            }
        }

        return declaration;
    }

    /**
     * A class to obtain metadata about instrumented code
     * @author vega
     *
     */
    private static class ApamIpojoInstrumentation implements Instrumentation {

    	/**
    	 * The iPojo generate metadata
    	 */
    	private final PojoMetadata pojoMetadata;

    	/**
    	 * The name of the instrumented class
    	 */
		private final String className; 


    	
    	public ApamIpojoInstrumentation(String className, PojoMetadata pojoMetadata) {
    		this.className		= (className == null) ? UNDEFINED : className;
    		this.pojoMetadata	= pojoMetadata;
    	}

    	
		@Override
		public String getClassName() {
			return className;
		}

		/**
		 * The list of supported collections for aggregate dependencies
		 */
		private static final List<String> supportedCollections = Arrays.asList( new String[] {
												Collection.class.getName(),
												List.class.getName(),
												Vector.class.getName(), 
												Set.class.getName()
											});

		/**
		 * Get the type of reference from the instrumented metadata of the field
		 */
		@Override
		public ResourceReference getType(String field) {
			
			ResourceReference type = new InterfaceReference("<Unavailable type for field "+field+">");
			FieldMetadata metadata = pojoMetadata.getField(field);
			
			if (metadata != null) {
				
		        /*
		         * Try to get the type from metadata, this is not always available because the iPojo manipulator
		         * doesn't handle java generics in collections and messages
		         */

				String fieldType = metadata.getFieldType();
		        if (fieldType.endsWith("[]")) {
		            int index = fieldType.indexOf('[');
		            type = new InterfaceReference(fieldType.substring(0, index));
		        }
		        else  if (supportedCollections.contains(fieldType)) {
		        	type = new InterfaceReference("<Unavailable type for elements of collection field "+field+" >");
		        }
		        else if (fieldType.equals(AbstractProducer.class.getName())) {
					type = new MessageReference("<Unavailable type for message field "+field+" >");
				}
		        else if (fieldType.equals(AbstractConsumer.class.getName())) {
					type = new MessageReference("<Unavailable type for message field "+field+" >");
				}
				else {
		            type = new InterfaceReference(fieldType);
				}
			}
			return type;
		}

		@Override
		public boolean isCollection(String field) {
			boolean isCollection = false;
			FieldMetadata metadata = pojoMetadata.getField(field);
			if (metadata != null) {
				String fieldType = metadata.getFieldType();
				isCollection = fieldType.endsWith("[]") || supportedCollections.contains(fieldType);
			}
			return isCollection;
		}
    	
    }
    /**
     * Parse a composite declaration
     */
    private CompositeDeclaration parseComposite(Element element) {

    	String name 								= parseName(element);
    	SpecificationReference specification		= parseSpecificationReference(element,CoreMetadataParser.ATT_SPECIFICATION,false);
    	ImplementationReference<?> implementation	= parseImplementationReference(element,CoreMetadataParser.ATT_MAIN_IMPLEMENTATION,true);

        CompositeDeclaration declaration = new CompositeDeclaration(name,specification,implementation);
        parseComponent(element,declaration);
        return declaration;
    }

    /**
     * Parse an instance declaration
     */
    private InstanceDeclaration parseInstance(Element element) {

    	String name 								= parseName(element);
    	ImplementationReference<?> implementation	= parseImplementationReference(element,CoreMetadataParser.ATT_IMPLEMENTATION,true);
    	
        InstanceDeclaration declaration = new InstanceDeclaration(implementation,name);
        parseComponent(element,declaration);
        return declaration;
    }


    
    /**
     * Get a string attribute value
     */
    private final String parseString(Element element, String attributeName, boolean mandatory) {
        String value = element.getAttribute(attributeName);
        
        if (mandatory && (value == null)) {
        	errorHandler.error(Severity.ERROR, "attribute \""+attributeName+"\" must be specified in "+element);
        	value = UNDEFINED;
        }
        
        if (mandatory && (value != null) && value.trim().isEmpty()) {
        	errorHandler.error(Severity.ERROR, "attribute \""+attributeName+"\" cannot be empty in "+element);
        	value = UNDEFINED;
        }

        return value;
    }


    private final String parseString(Element element, String attributeName) {
        return parseString(element,attributeName,true);
    }


    /**
     * Get the element name
     */
    private final String parseName(Element element) {
        return parseString(element,CoreMetadataParser.ATT_NAME);
    }


    /**
     * Get an specification reference coded in an attribute
     */
    private SpecificationReference parseSpecificationReference(Element element, String attibute, boolean mandatory) {
        String specification = parseString(element,attibute,mandatory);
        return (specification == null && ! mandatory) ? null : new SpecificationReference(specification);
    }

    /**
     * Get an implementation reference coded in an attribute
     */
    private ImplementationReference<?> parseImplementationReference(Element element, String attibute, boolean mandatory) {
    	String implementation = parseString(element,attibute,mandatory);
        return (implementation == null && ! mandatory) ? null : new ImplementationReference<ImplementationDeclaration>(implementation);
    }

    /**
     * Get an interface reference coded in an attribute
     */
    private InterfaceReference parseInterfaceReference(Element element, String attibute, boolean mandatory) {
    	String interfaceName = parseString(element,attibute,mandatory);
        return (interfaceName == null && ! mandatory) ? null : new InterfaceReference(interfaceName);
    }

    /**
     * Get a message reference coded in an attribute
     */
    private MessageReference parseMessageReference(Element element, String attibute, boolean mandatory) {
    	String messageName = parseString(element,attibute,mandatory);
        return (messageName == null && ! mandatory) ? null : new MessageReference(messageName);
    }

    /**
     * Get a resource reference coded in an attribute
     */
    private ResourceReference parseResourceReference(Element element, String attibute, boolean mandatory) {
    	
    	if (element.getName().equals(CoreMetadataParser.INTERFACE))
    		return parseInterfaceReference(element,attibute,mandatory);
    	
    	if (element.getName().equals(CoreMetadataParser.MESSAGE))
    		return parseMessageReference(element,attibute,mandatory);
    	
    	return null;
    	
    }

    /**
     * parse the common attributes shared by all declarations
     */
    private void parseComponent(Element element, ComponentDeclaration component) {

        parseProvidedResources(element,component);
        parsePropertyDefinitions(element,component);
        parseProperties(element,component);
        parseDependencies(element,component);

    }

    /**
     * parse the provided resources of a component
     */
    private void parseProvidedResources(Element element, ComponentDeclaration component) {

    	String interfaces	= parseString(element,CoreMetadataParser.ATT_INTERFACES,false);
    	String messages		= parseString(element,CoreMetadataParser.ATT_MESSAGES,false);

    	for (String interfaceName : Util.split(interfaces)) {
            component.getProvidedResources().add(new InterfaceReference(interfaceName));
        }

        for (String message : Util.split(messages)) {
            component.getProvidedResources().add(new MessageReference(message));
        }

    }

    /**
     * parse the required resources of a component
     */
    private void parseDependencies(Element element, ComponentDeclaration component) {

        for (Element dependencies : optional(element.getElements(CoreMetadataParser.DEPENDENCIES,CoreMetadataParser.APAM))) {
            for (Element dependency : optional(dependencies.getElements())) {

                /*
                 * ignore elements that are not from APAM
                 */
                if (!CoreMetadataParser.isApamDefinition(dependency))
                    continue;

                parseDependency(dependency,component);
            }
        }

    }

 
    /**
     * parse a dependency declaration
     */
    private void parseDependency(Element element, ComponentDeclaration component) {
    	
    	/*
    	 * All dependencies have an optional identifier 
    	 */
        String id = parseString(element,CoreMetadataParser.ATT_ID,false);
        DependencyDeclaration dependency = null;
        
        /*
         * Complex dependencies reference a single mandatory specification, and in the case of atomic components
         * may optionally have a number field injection declarations
         */
        if (element.getName().equals(CoreMetadataParser.SPECIFICATION)) {

        	SpecificationReference target = parseSpecificationReference(element,CoreMetadataParser.ATT_NAME,true);
            dependency = new DependencyDeclaration(component,id,target);

            if (component instanceof AtomicImplementationDeclaration) {
            	
            	AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
                for (Element injection : optional(element.getElements())) {

                    /*
                     * ignore elements that are not from APAM
                     */
                    if (!CoreMetadataParser.isApamDefinition(injection))
                        continue;

                    if (!CoreMetadataParser.isResourceDependency(injection))
                    	continue;

                    DependencyInjection dependencyInjection = parseDependencyInjection(injection,atomic);
                    dependencyInjection.setDependency(dependency);
                    
                }
            }
        }

        /*
         * Simple dependencies reference a single resource. 
         */
        if (isResourceDependency(element)){
        	
        	ResourceReference target = parseResourceReference(element,CoreMetadataParser.ATT_NAME,false);
        	
        	if (component instanceof AtomicImplementationDeclaration) {
            	/*
            	 * For atomic components the declaration also defines a field injection and we can infer the target from it
            	 * if not explicitly declared
            	 */
        		
            	AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
                DependencyInjection dependencyInjection = parseDependencyInjection(element,atomic);
                
        		/*
        		 * If both an explicit target and an injection are specified they must match 
        		 */
        		if (target != null &&  !target.equals(dependencyInjection.getResource())) {
        			errorHandler.error(Severity.ERROR, "dependency target doesn't match the type of the field in "+element);
        		}
        		
        		/*
        		 * If a target is not explicitly declared use the injected field metadata
        		 */
        		if (target == null)
        			target = dependencyInjection.getResource();
        		
                dependency = new DependencyDeclaration(component,id,target);
                dependencyInjection.setDependency(dependency);

        	} 
        	else {
            	/*
            	 * For other components a target must be explicitly specified
            	 */
        		target = parseInterfaceReference(element,CoreMetadataParser.ATT_NAME,true);
        		dependency = new DependencyDeclaration(component,id,target);
        	}
         	
        }

        
        for (Element constraints : optional(element.getElements(CoreMetadataParser.CONSTRAINTS,CoreMetadataParser.APAM))) {
            parseConstraints(constraints, dependency);
        }

        for (Element preferences : optional(element.getElements(CoreMetadataParser.PREFERENCES,CoreMetadataParser.APAM))) {
            parsePreferences(preferences, dependency);
        }

    }


    /**
     * parse the injected dependencies of a primitive
     */
    private DependencyInjection parseDependencyInjection(Element element, AtomicImplementationDeclaration primitive) {

    	String field = parseString(element, CoreMetadataParser.ATT_FIELD,true);
        return new DependencyInjection(primitive, field);

    }

    /**
     * parse a constraints declaration
     */
    private void parseConstraints(Element element, DependencyDeclaration dependency) {

        for (Element constraint : optional(element.getElements())) {

            String filter = parseString(constraint, CoreMetadataParser.ATT_FILTER);

            if (constraint.getName().equals(CoreMetadataParser.IMPLEMENTATION))
                dependency.getImplementationConstraints().add(filter);

            if (constraint.getName().equals(CoreMetadataParser.INSTANCE))
                dependency.getInstanceConstraints().add(filter);

        }

    }

    /**
     * parse a preferences declaration
     */
    private void parsePreferences(Element element, DependencyDeclaration dependency) {

        for (Element constraint : optional(element.getElements())) {

            String filter = parseString(constraint, CoreMetadataParser.ATT_FILTER);

            if (constraint.getName().equals(CoreMetadataParser.IMPLEMENTATION))
                dependency.getImplementationPreferences().add(filter);

            if (constraint.getName().equals(CoreMetadataParser.INSTANCE))
                dependency.getInstancePreferences().add(filter);

        }

    }

    /**
     * parse the property definitions of the component
     */
    private void parsePropertyDefinitions(Element element, ComponentDeclaration component) {
    	
    	if (component instanceof InstanceDeclaration)
    		return; 
    	
        for (Element definitions : optional(element.getElements(CoreMetadataParser.DEFINITIONS, CoreMetadataParser.APAM))) {
            for (Element definition : optional(definitions.getElements(CoreMetadataParser.DEFINITION, CoreMetadataParser.APAM))) {

                String name 		= parseString(definition, CoreMetadataParser.ATT_NAME).toLowerCase();
                String type			= parseString(definition,CoreMetadataParser.ATT_TYPE) ;
                String defaultValue = parseString(definition,CoreMetadataParser.ATT_VALUE,false);

                component.getPropertyDefinitions().add(new PropertyDefinition(name, type, defaultValue));
            }
        }
    }


    /**
     * parse the properties of the component
     */
    private void parseProperties(Element element, ComponentDeclaration component) {

        for (Element properties : optional(element.getElements(CoreMetadataParser.PROPERTIES, CoreMetadataParser.APAM))) {

            // parse user defined properties
            for (Element property : optional(properties.getElements(CoreMetadataParser.PROPERTY,
                    CoreMetadataParser.APAM))) {

                // consider attributes as properties
                for (Attribute attribute : property.getAttributes()) {

                    // skip special attributes
                    if (attribute.getName().equals(CoreMetadataParser.ATT_TYPE))
                        continue;

                    if (attribute.getName().equals(CoreMetadataParser.ATT_FIELD))
                        continue;

                    // add all other properties
                    component.getProperties().put(attribute.getName(), attribute.getValue());

                }

            }
        }

    }

    /**
     * Handle transparently optional elements in the metadata
     */
    private Element[] EMPTY_ELEMENTS = new Element[0];

    private Element[] optional(Element[] elements) {
        if (elements == null)
            return EMPTY_ELEMENTS;
        return elements;

    }
    /**
     * Tests whether the specified element is an Apam declaration
     */
    private static final boolean isApamDefinition(Element element) {
        return  (element.getNameSpace() != null) && CoreMetadataParser.APAM.equals(element.getNameSpace());

    }

    /**
     * Determines if the element represents a resource dependency
     */
    private static final boolean isResourceDependency(Element element) {
        return	CoreMetadataParser.INTERFACE.equals(element.getName()) ||
        		CoreMetadataParser.MESSAGE.equals(element.getName());
    }
    
    /**
     * Determines if this element represents an specification declaration
     */
    private static final boolean isSpecification(Element element) {
        return CoreMetadataParser.SPECIFICATION.equals(element.getName());
    }

    /**
     * Determines if this element represents a primitive declaration
     */
    private static final boolean isPrimitiveImplementation(Element element) {
        return CoreMetadataParser.IMPLEMENTATION.equals(element.getName());
    }

    /**
     * Determines if this element represents a composite declaration
     */
    private static final boolean isCompositeImplementation(Element element) {
        return CoreMetadataParser.COMPOSITE.equals(element.getName());
    }

    /**
     * Determines if this element represents a composite declaration
     */
    private static final boolean isInstance(Element element) {
        return CoreMetadataParser.INSTANCE.equals(element.getName());
    }
    


}
