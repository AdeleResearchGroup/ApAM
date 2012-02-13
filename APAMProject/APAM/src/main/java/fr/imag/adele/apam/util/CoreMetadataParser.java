package fr.imag.adele.apam.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.DependencyPromotion;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler.Severity;

/**
 * Parse an APAM declaration from its iPojo metadata representation
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
	public static final String ATT_SOURCE				= 	"source";

	
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
	 * Initialize parser with the given metadata
	 */
	public synchronized void setMetadata(Element metadata) {
		this.metadata 			= metadata;
		this.declaredElements	= null;
	}

	/**
	 * Parse the ipojo metadata to get the component declarations
	 */
	@Override
	public synchronized Set<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler) {
		if (declaredElements != null)
			return declaredElements;
		
		this.declaredElements 	= new HashSet<ComponentDeclaration>();
		this.errorHandler		= errorHandler;
		for (Element element : metadata.getElements()) {
			
			/*
			 * Ignore not APAM elements 
			 */
			if (! isApamDefinition(element))
				continue;
			
			/*
			 * switch depending on component type
			 */
			if (isSpecification(element))
				declaredElements.add(parseSpecification(element));
			
			if (isPrimitiveImplementation(element))
				declaredElements.add(parsePrimitive(element));
			
			if (isCompositeImplementation(element))
				declaredElements.add(parseComposite(element));
			
			if (isInstance(element))
				declaredElements.add(parseInstance(element));
		}
		
		// Release references once the parsed data is cached
		this.metadata 		= null;
		this.errorHandler 	= null;
		
		return declaredElements;
	}

	/**
	 * Parse an specification declaration
	 */
	private SpecificationDeclaration parseSpecification(Element element) {
		SpecificationDeclaration declaration = new SpecificationDeclaration(parseName(element));
		return parseComponent(element,declaration);
	}

	/**
	 * Parse a composite declaration
	 */
	private CompositeDeclaration parseComposite(Element element) {
		CompositeDeclaration declaration = new CompositeDeclaration(parseName(element),
													parseSpecificationReference(element,ATT_SPECIFICATION),
													parseImplementationReference(element,ATT_MAIN_IMPLEMENTATION));
		
		parseComponent(element,declaration);
		parseDependencyPromotions(element,declaration);
		
		return declaration;
	}

	/**
	 * Parse an instance declaration
	 */
	private InstanceDeclaration parseInstance(Element element) {
		InstanceDeclaration declaration = new InstanceDeclaration(parseImplementationReference(element,ATT_IMPLEMENTATION),
													parseName(element));
		return parseComponent(element,declaration);
	}

	/**
	 * Parse an atomic implementation declaration
	 */
	private AtomicImplementationDeclaration parsePrimitive(Element element) {
		AtomicImplementationDeclaration declaration = new AtomicImplementationDeclaration(parseName(element),
																parseSpecificationReference(element,ATT_SPECIFICATION),
																parseClassName(element));
		parseComponent(element,declaration);
		parseDependencyInjections(element,declaration);
		
		return declaration;
	}

	/**
	 * Get a string attribute value
	 */
	private final String parseString(Element element, String attributeName, boolean mandatory) {
		String value = element.getAttribute(attributeName);
		if (mandatory && value == null)
			errorHandler.error(Severity.ERROR, "attribute "+attributeName+" must be specified in "+element);

		if (mandatory && value.trim().equals(""))
			errorHandler.error(Severity.ERROR, "attribute "+attributeName+" cannot be empty in "+element);

		if (value == null)
			value = "";
		
		return value;

	}

	
	private final String parseString(Element element, String attributeName) {
		return parseString(element,attributeName,true);
	}

	/**
	 * Get a string list attribute value
	 */
	private final String[] parseStringList(Element element, String attributeName, boolean mandatory) {
		String encodedList = parseString(element,attributeName,mandatory);
		if (mandatory && encodedList == null)
			errorHandler.error(Severity.ERROR, "attribute "+attributeName+" must be specified");

		if (mandatory && encodedList.trim().equals(""))
			errorHandler.error(Severity.ERROR, "attribute "+attributeName+" cannot be empty");

		return Util.split(encodedList);

	}

	/**
	 * Get the element name
	 */
	private final String parseName(Element element) {
		return parseString(element,ATT_NAME);
	}

	/**
	 * Get the component class name, 
	 */
	private final String parseClassName(Element element) {
		return parseString(element,ATT_CLASSNAME);
	}
	
	/**
	 * Get an specification reference coded in an attribute
	 */
	private SpecificationReference parseSpecificationReference(Element element, String attibute) {
		return new SpecificationReference(parseString(element,attibute));
	}

	/**
	 * Get an specification reference coded in an attribute
	 */
	private ImplementationReference parseImplementationReference(Element element, String attibute) {
		return new ImplementationReference(parseString(element,attibute));
		
	}

	/**
	 * parse the common attributes shared by all declarations
	 */
	private <C extends ComponentDeclaration> C parseComponent(Element element, C component) {
		
		parseProvidedResources(element,component);
		parsePropertyDefinitions(element,component);
		parseProperties(element, component);
		parseDependencies(element, component);
		
		return component;
	}

	/**
	 * parse the provided resources of a component
	 */
	private <C extends ComponentDeclaration> C parseProvidedResources(Element element, C component) {
		
		for (String interfaceName : parseStringList(element,ATT_INTERFACES,false)) {
			component.getProvidedResources().add(new InterfaceReference(interfaceName));
		}

		for (String message : parseStringList(element,ATT_MESSAGES,false)) {
			component.getProvidedResources().add(new MessageReference(message));
		}
		
		return component;
	}

	/**
	 * parse the required resources of a component
	 */
	private <C extends ComponentDeclaration> C parseDependencies(Element element, C component) {
		
		for (Element dependencies : element.getElements(DEPENDENCIES, APAM)) {
			for (Element dependency : dependencies.getElements()) {
				
				/*
				 * ignore elements that are not from APAM
				 */
				if (!isApamDefinition(dependency))
					continue;

				/*
				 * ignore dependencies without identifier. 
				 * 
				 * Notice that in some cases it is possible to infer a dependency Id from injected fields or methods,
				 * these cases are treated apart in the specialized parse methods 
				 */
				String name = parseString(dependency,ATT_NAME,false);
				if (name.equals(""))
					continue;
				
				parseDependency(dependency,component,name);
			}
		}
		
		return component;
	}

	/**
	 * parse a dependency declaration
	 */
	private <C extends ComponentDeclaration> C parseDependency(Element element, C component, String name) {
		assert name != null && ! name.equals("");
		
		String multiple = parseString(element, ATT_MULTIPLE, false);
		Boolean isMultiple = multiple != null? Boolean.parseBoolean(multiple) : null;
		

			
		// create a typed dependency based on the target kind
			
		ResourceReference target = null;
			
		if (element.getName().equals(SPECIFICATION))
			target = new SpecificationReference(name);

		if (element.getName().equals(INTERFACE))
			target = new InterfaceReference(name);
		
		if (element.getName().equals(MESSAGE))
			target = new MessageReference(name);
		
		DependencyDeclaration dependency = new DependencyDeclaration(component,name,target,isMultiple);
		for (Element constraints : element.getElements(CONSTRAINTS, APAM)) {
			parseConstraints(constraints, dependency);
		}
		
		for (Element preferences : element.getElements(PREFERENCES, APAM)) {
			parsePreferences(preferences, dependency);
		}

		component.getDependencies().add(dependency);
		
		return component;
	}

	/**
	 * parse the promoted dependencies of a composite
	 */
	private <C extends CompositeDeclaration> C parseDependencyPromotions(Element element, C composite) {
		
		for (Element dependencies : element.getElements(DEPENDENCIES, APAM)) {
			for (Element dependency : dependencies.getElements()) {
				
				/*
				 * ignore elements that are not from APAM
				 */
				if (!isApamDefinition(dependency))
					continue;

				/*
				 * ignore dependencies without identifier or not successfully parsed. 
				 */
				String name = parseString(dependency,ATT_NAME,false);
				if (name.equals(""))
					continue;

				DependencyDeclaration dependencyDeclaration = composite.getDependency(name);
				if (dependencyDeclaration == null)
					continue;
				
				/*
				 * avoid creating unnecessary promotions
				 */
				String sources[] = parseStringList(dependency,ATT_SOURCE,false);
				if (sources.length == 0)
					continue;
				
				/*
				 *	Create the promotion and set sources 
				 */
				DependencyPromotion promotion = new DependencyPromotion(composite, dependencyDeclaration);
				for (String source : sources) {
					promotion.getSources().add(new SpecificationReference(source));
				}
			}
		}
		
		return composite;
	}
	
	/**
	 * parse the injected dependencies of a primitive
	 */
	private <C extends AtomicImplementationDeclaration> C parseDependencyInjections(Element element, C primitive) {
		
		for (Element dependencies : element.getElements(DEPENDENCIES, APAM)) {
			for (Element dependency : dependencies.getElements()) {
				
				/*
				 * ignore elements that are not from APAM
				 */
				if (!isApamDefinition(dependency))
					continue;

				//TODO recurse in case of specification
				/*
				 * ignore dependencies without identifier or not successfully parsed. 
				 */
				String name		= parseString(dependency,ATT_NAME,false);
				String field 	= parseString(element, ATT_FIELD,false);
				
				//TODO get type from field
				String fieldType = null;

				if (fieldType.equals(""))
					continue;

				// TODO message reference
				ResourceReference resource = new InterfaceReference(fieldType);
				/*
				 * Infer a dependency name from a field injection, this is a facility for
				 * people writing the metadata 
				 */
				if (name.equals("") && ! field.equals("")) {
					name = fieldType;
					parseDependency(dependency,primitive,name);
				}
				
				/*
				 * ignore not named or unsuccessfully parsed dependencies
				 */
				if (name.equals(""))
					continue;

				DependencyDeclaration dependencyDeclaration = primitive.getDependency(name);
				if (dependencyDeclaration == null)
					continue;
				
				/*
				 * Verify a field is specified
				 */
				if (field.equals(""))
					continue;
				
				
				/*
				 *	Create the injection and add to primitive
				 */
				DependencyInjection injection = new DependencyInjection(primitive, dependencyDeclaration, field, resource);
			}
		}
		
		return primitive;
	}


	/**
	 * parse a constraints declaration
	 */
	private <D extends DependencyDeclaration> D parseConstraints(Element element, D dependency) {
		
		String filter = parseString(element,ATT_FILTER);

		if (element.getName().equals(IMPLEMENTATION))
			dependency.getImplementationConstraints().add(filter);

		if (element.getName().equals(INSTANCE))
			dependency.getInstanceConstraints().add(filter);
		
		return dependency;
	}

	/**
	 * parse a preferences declaration
	 */
	private <D extends DependencyDeclaration> D parsePreferences(Element element, D dependency) {
		
		String filter = parseString(element,ATT_FILTER);

		if (element.getName().equals(IMPLEMENTATION))
			dependency.getImplementationPreferences().add(filter);

		if (element.getName().equals(INSTANCE))
			dependency.getInstancePreferences().add(filter);
		
		return dependency;
	}

	/**
	 * parse the property definitions of the component
	 */
	private <C extends ComponentDeclaration> C parsePropertyDefinitions(Element element, C component) {
		
		for (Element definitions : element.getElements(DEFINITIONS,APAM)) {
			for (Element definition : definitions.getElements(DEFINITION,APAM)) {
				
				String name 		= parseString(definition,ATT_NAME);
				String type			= parseString(definition,ATT_TYPE) ;
				String defaultValue = parseString(definition,ATT_VALUE,false);
				
				component.getPropertyDefinitions().add(new PropertyDefinition(name, type, defaultValue));
			}
		}
		
		return component;
	}


	/**
	 * parse the properties of the component
	 */
	private <C extends ComponentDeclaration> C parseProperties(Element element, C component) {
		
		for (Element properties : element.getElements(PROPERTIES,APAM)) {
			
			// parse global predefined properties
			for (Attribute attribute : properties.getAttributes()) {
				component.getProperties().put(attribute.getName(), attribute.getValue());

			}
			
			// parse user defined properties
			for (Element property : properties.getElements(PROPERTY,APAM)) {
				
				// consider attributes as properties
				for (Attribute attribute : property.getAttributes()) {
					
					// skip special attributes
					if (attribute.getName().equals(ATT_TYPE))
						continue;

					if (attribute.getName().equals(ATT_FIELD))
						continue;

					component.getProperties().put(attribute.getName(), attribute.getValue());
					
				}
				
			}
		}
		
		return component;
	}

	/**
	 * Tests whether the specified element is an Apam declaration
	 */
	private static final boolean isApamDefinition(Element element) {
		return  element.getNameSpace() != null && APAM.equals(element.getNameSpace());

	}
	
	/**
	 * Determines if this element represents an specification declaration
	 */
	public static final boolean isSpecification(Element element) {
		return SPECIFICATION.equals(element.getName());
	}

	/**
	 * Determines if this element represents a primitive declaration
	 */
	public static final boolean isPrimitiveImplementation(Element element) {
		return IMPLEMENTATION.equals(element.getName());
	}
	
	/**
	 * Determines if this element represents a composite declaration
	 */
	public static final boolean isCompositeImplementation(Element element) {
		return COMPOSITE.equals(element.getName());
	}

	/**
	 * Determines if this element represents a composite declaration
	 */
	public static final boolean isInstance(Element element) {
		return INSTANCE.equals(element.getName());
	}


}
