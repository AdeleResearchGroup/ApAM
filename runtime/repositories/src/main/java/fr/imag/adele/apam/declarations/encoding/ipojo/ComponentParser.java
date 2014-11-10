package fr.imag.adele.apam.declarations.encoding.ipojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InjectedPropertyPolicy;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.ProviderInstrumentation;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RelationPromotion;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.Reporter.Severity;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.ResolvePolicy;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.ipojo.MetadataParser.IntrospectionService;
import fr.imag.adele.apam.declarations.instrumentation.CallbackDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.InstanceReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.PackageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

public class ComponentParser implements Decoder<Element> {

	/**
	 * Constants defining the different element and attributes
	 */
	public static final String APAM = "fr.imag.adele.apam";
	public static final String COMPONENT = "component";
	public static final String SPECIFICATION = "specification";
	public static final String IMPLEMENTATION = "implementation";
	public static final String COMPOSITE = "composite";
	public static final String INSTANCE = "instance";
	public static final String INSTANCE_ALT = "apam-instance";
	public static final String DEFINITIONS = "definitions";
	public static final String DEFINITION = "definition";
	public static final String PROPERTIES = "properties";
	public static final String PROPERTY = "property";
	public static final String DEPENDENCIES = "dependencies";
	public static final String DEPENDENCY = "dependency";
	public static final String RELATIONS = "relations";
	public static final String RELATION = "relation";
	public static final String OVERRIDE = "override";
	public static final String LINK = "link";
	public static final String INTERFACE = "interface";
	public static final String PACKAGE = "package";
	
	public static final String MESSAGE = "message";
	public static final String CONSTRAINTS = "constraints";
	public static final String CONSTRAINT = "constraint";
	public static final String PREFERENCES = "preferences";
	public static final String CONTENT = "contentMngt";
	public static final String START = "start";
	public static final String TRIGGER = "trigger";
	public static final String OWN = "own";
	public static final String PROMOTE = "promote";
	public static final String GRANT = "grant";
	public static final String DENY = "deny";
	public static final String STATE = "state";
	public static final String IMPORTS = "import";
	public static final String EXPORT = "export";

	public static final String EXPORTAPP = "exportapp";
	public static final String CALLBACK = "callback";
	public static final String ATT_NAME = "name";
	public static final String ATT_CLASSNAME = "classname";
	public static final String ATT_EXCLUSIVE = "exclusive";
	public static final String ATT_SINGLETON = "singleton";
	public static final String ATT_SHARED = "shared";
	public static final String ATT_INSTANTIABLE = "instantiable";
	public static final String ATT_SPECIFICATION = "specification";
	public static final String ATT_IMPLEMENTATION = "implementation";
	public static final String ATT_MAIN_IMPLEMENTATION = "main"; // "mainImplem"
	public static final String ATT_INSTANCE = "instance";
	public static final String ATT_INTERFACES = "interfaces";
	public static final String ATT_PACKAGES = "packages";
	public static final String ATT_MESSAGES = "messages";
	public static final String ATT_TYPE = "type";
	public static final String ATT_DEFAULT = "default";
	public static final String ATT_VALUE = "value";
	public static final String ATT_FIELD = "field";
	public static final String ATT_INJECTED = "injected";
	public static final String ATT_MULTIPLE = "multiple";
	public static final String ATT_SOURCE = "source";
	public static final String ATT_TARGET = "target";
	public static final String ATT_SOURCE_KIND = "sourceKind";
	public static final String ATT_TARGET_KIND = "targetKind";
	public static final String ATT_FAIL = "fail";
	public static final String ATT_EXCEPTION = "exception";
	public static final String ATT_HIDE = "hide";
	public static final String ATT_FILTER = "filter";
	public static final String ATT_PROPERTY = "property";
	public static final String ATT_RELATION = "relation";
	public static final String ATT_TO = "to";
	public static final String ATT_WHEN = "when";
	public static final String ATT_ON_REMOVE = "onRemove";
	public static final String ATT_ON_INIT = "onInit";
	public static final String ATT_METHOD = "method";
	public static final String ATT_PUSH = "push";
	public static final String ATT_PULL = "pull";
	public static final String ATT_BIND = "added";
	public static final String ATT_UNBIND = "removed";
    public static final String ATT_REQUIRE_VERSION = "require-version";


	public static final String ATT_CREATION_POLICY = "creation";
	public static final String ATT_RESOLVE_POLICY = "resolve";
	public static final String VALUE_OPTIONAL = "optional";

	public static final String VALUE_WAIT = "wait";
	public static final String VALUE_EXCEPTION = "exception";
	public static final String VALUE_KIND_INSTANCE = "instance";

	public static final String VALUE_KIND_IMPLEMENTATION = "implementation";

	public static final String VALUE_KIND_SPECIFICATION = "specification";

	/**
	 * The optional service that give access to introspection information
	 */
	private IntrospectionService introspector;

	/**
	 * The currently used error handler
	 */
	private Reporter errorHandler;

	public ComponentParser(IntrospectionService introspector) {
		this.introspector = introspector;
	}

	/**
	 * The list of allowed values for specifying the missing policy
	 */
	private static final List<String> MISSING_VALUES = Arrays.asList(VALUE_WAIT, VALUE_EXCEPTION, VALUE_OPTIONAL);

	/**
	 * The list of allowed values for specifying the missing policy
	 */
	private static final List<String> KIND_VALUES = Arrays.asList(VALUE_KIND_INSTANCE, VALUE_KIND_IMPLEMENTATION, VALUE_KIND_SPECIFICATION);

	/**
	 * The list of possible kinds of component references
	 */
	private final static List<String> COMPONENT_REFERENCES = Arrays.asList(SPECIFICATION, IMPLEMENTATION, INSTANCE, COMPONENT);

	/**
	 * The list of possible kinds of component references
	 */
	private final static List<String> RESOURCE_REFERENCES = Arrays.asList(INTERFACE, MESSAGE, PACKAGE);

	/**
	 * The list of possible kinds of references
	 */
	private final static List<String> ALL_REFERENCES = Arrays.asList(SPECIFICATION, IMPLEMENTATION, INSTANCE, COMPONENT, INTERFACE, MESSAGE, PACKAGE);

	/**
	 * Handle transparently optional elements in the metadata
	 */
	private final static Element[] EMPTY_ELEMENTS = new Element[0];

	/**
	 * Tests whether the specified element is an Apam declaration
	 */
	private static final boolean isApamDefinition(Element element) {
		return (element.getNameSpace() != null) && APAM.equals(element.getNameSpace());

	}

	/**
	 * Determines if this element represents a composite declaration
	 */
	private static final boolean isCompositeImplementation(Element element) {
		return COMPOSITE.equals(element.getName());
	}

	/**
	 * Determines if this element represents an instance declaration
	 */
	private static final boolean isInstance(Element element) {
		return INSTANCE.equals(element.getName()) || INSTANCE_ALT.equals(element.getName());
	}

	/**
	 * Determines if this element represents a primitive declaration
	 */
	private static final boolean isPrimitiveImplementation(Element element) {
		return IMPLEMENTATION.equals(element.getName());
	}

	/**
	 * Determines if this element represents an specification declaration
	 */
	private static final boolean isSpecification(Element element) {
		return SPECIFICATION.equals(element.getName());
	}

	private Element[] every(Element[]... alternatives) {
		if (alternatives == null) {
			return EMPTY_ELEMENTS;
		}

		List<Element> all = new ArrayList<Element>();
		for (Element[] elements : alternatives) {
			if (elements != null) {
				all.addAll(Arrays.asList(elements));
			}
		}
		return all.toArray(new Element[all.size()]);

	}

	/**
	 * Parse the ipojo metadata to get the component declarations
	 */
	@Override
	public ComponentDeclaration decode(Element metadata, Reporter errorHandler) {
		
		this.errorHandler 	= errorHandler;

		ComponentDeclaration component = null;
		
		/*
		 * Ignore not APAM elements
		 */
		if (!isApamDefinition(metadata)) {
			return null;
		}

		/*
		 * switch depending on component type
		 */
		if (isSpecification(metadata)) {
			component = parseSpecification(metadata);
		}

		if (isPrimitiveImplementation(metadata)) {
			component = parsePrimitive(metadata);
		}

		if (isCompositeImplementation(metadata)) {
			component = parseComposite(metadata);
		}

		if (isInstance(metadata)) {
			component = parseInstance(metadata);
		}
		

		// Release references to aneble reuse
		
		this.errorHandler	= null;

		return component;
	}

	/**
	 * Infer the kind of a reference from the specified element tag and
	 * attribute name
	 */
	private final String getReferenceKind(Element element, String attribute) {

		if (ALL_REFERENCES.contains(attribute)) {
			return attribute;
		}

		if (ALL_REFERENCES.contains(element.getName())) {
			return element.getName();
		}

		return Decoder.UNDEFINED;
	}

	private Element[] optional(Element[] elements) {
		if (elements == null) {
			return EMPTY_ELEMENTS;
		}

		return elements;

	}

	/**
	 * Get a generic component reference coded in an attribute
	 */
	private ComponentReference<?> parseAnyComponentReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String component = parseString(inComponent, element, attribute, mandatory);
		return ((component == null) && !mandatory) ? null : new ComponentReference<ComponentDeclaration>(component);
	}

	/**
	 * Get a boolean attribute value
	 */
	private boolean parseBoolean(String componentName, Element element, String attribute, boolean mandatory, boolean defaultValue) {
		String valueString = parseString(componentName, element, attribute, mandatory);
		return ((valueString == null) && !mandatory) ? defaultValue : Boolean.parseBoolean(valueString);
	}

	/**
	 * parse callback declaration
	 */

	private CallbackDeclaration parseCallback(AtomicImplementationDeclaration implementation, String methodName) {
		CallbackDeclaration callback = new CallbackDeclaration(implementation, methodName);
		if (!callback.isValidInstrumentation()) {
			errorHandler.report(Severity.ERROR, implementation.getName() + " : the specified method \"" + methodName + "\" is invalid or not found");
		}
		return callback;

	}

	/**
	 * parse the common attributes shared by all declarations
	 */
	private void parseComponent(Element element, ComponentDeclaration component) {

		parseProvidedResources(element, component);
		parsePropertyDefinitions(element, component);
		parseProperties(element, component);
		parseRelations(element, component);


		/*
		 * parse predefined properties
		 */
		
		boolean isDefinedInstantiable 	= isDefined(component.getName(), element, ATT_INSTANTIABLE);
		boolean isDefinedExclusive 		= isDefined(component.getName(), element, ATT_EXCLUSIVE);
		boolean isDefinedSingleton 		= isDefined(component.getName(), element, ATT_SINGLETON);
		boolean isDefinedShared 		= isDefined(component.getName(), element, ATT_SHARED);

		boolean isInstantiable 			= parseBoolean(component.getName(), element, ATT_INSTANTIABLE, false, true);
		boolean isExclusive 			= parseBoolean(component.getName(), element, ATT_EXCLUSIVE, false, false);
		boolean isSingleton 			= parseBoolean(component.getName(), element, ATT_SINGLETON, false, false);
		boolean isShared				= parseBoolean(component.getName(), element, ATT_SHARED, false, true);
		
		if (isDefinedInstantiable)	component.setInstantiable(isInstantiable);
		if (isDefinedExclusive) 	component.setExclusive(isExclusive);
		if (isDefinedSingleton) 	component.setSingleton(isSingleton);
		if (isDefinedShared) 		component.setShared(isShared);

		// Exclusive means: shared=false and singleton=true.
		if (isDefinedExclusive && isExclusive) {
			if (isDefinedShared && isShared) {
				errorHandler.report(Severity.ERROR, "A component cannot be both exclusive and shared");
			}
			if (isDefinedSingleton && isSingleton) {
				errorHandler.report(Severity.ERROR, "A component cannot be both exclusive and not singleton");
			}
			component.setSingleton(true);
			component.setShared(false);
		}
	}

	/**
	 * Get a component reference implicitly coded in the element (either in a
	 * name attribute or an attribute named after the kind of reference)
	 */
	private ComponentReference<?> parseComponentReference(String inComponent, Element element, boolean mandatory) {

		String attribute = Decoder.UNDEFINED;

		/*
		 * If the kind of reference is coded in the element name, the actual
		 * value must be coded in the attribute NAME
		 */
		if (COMPONENT_REFERENCES.contains(element.getName())) {
			attribute = ATT_NAME;
		}

		/*
		 * Otherwise try to find a defined attribute matching the kind of
		 * reference
		 */
		for (Attribute definedAttribute : element.getAttributes()) {

			if (!COMPONENT_REFERENCES.contains(definedAttribute.getName())) {
				continue;
			}

			attribute = definedAttribute.getName();
			break;
		}

		if (attribute.equals(Decoder.UNDEFINED) && mandatory) {
			errorHandler.report(Severity.ERROR, "component name must be specified in " + element.getName());
			return new ComponentReference<ComponentDeclaration>(Decoder.UNDEFINED);
		}

		if (attribute.equals(Decoder.UNDEFINED) && !mandatory) {
			return null;
		}

		return parseComponentReference(inComponent, element, attribute, mandatory);
	}

	/**
	 * Get a component reference coded in an attribute
	 */
	private ComponentReference<?> parseComponentReference(String inComponent, Element element, String attribute, boolean mandatory) {

		String referenceKind = getReferenceKind(element, attribute);

		if (SPECIFICATION.equals(referenceKind)) {
			return parseSpecificationReference(inComponent, element, attribute, mandatory);
		}

		if (IMPLEMENTATION.equals(referenceKind)) {
			return parseImplementationReference(inComponent, element, attribute, mandatory);
		}

		if (INSTANCE.equals(referenceKind)) {
			return parseInstanceReference(inComponent, element, attribute, mandatory);
		}

		if (COMPONENT.equals(referenceKind)) {
			return parseAnyComponentReference(inComponent, element, attribute, mandatory);
		}

		if (mandatory) {
			errorHandler.report(Severity.ERROR, "component name must be specified in " + element);
			return new ComponentReference<ComponentDeclaration>(Decoder.UNDEFINED);
		}

		return null;

	}

	/**
	 * Parse a composite declaration
	 */
	private CompositeDeclaration parseComposite(Element element) {

		String name = parseName(element);

		SpecificationReference specification 						= parseSpecificationReference(name, element, ATT_SPECIFICATION, false);
        String  versionRange 										= parseString(name, element, ATT_REQUIRE_VERSION, false);
        VersionedReference<SpecificationDeclaration> specificationVersion	= specification != null ? VersionedReference.range(specification,versionRange) : null;
		
        ComponentReference<?> implementation 	= parseAnyComponentReference(name, element, ATT_MAIN_IMPLEMENTATION, false);

        CompositeDeclaration declaration = new CompositeDeclaration(name, specificationVersion, implementation);

        parseComponent(element, declaration);
		parseCompositeContent(element, declaration);

		return declaration;
	}

	/**
	 * parse the content management policies of a composite
	 */
	private void parseCompositeContent(Element element, CompositeDeclaration declaration) {

		/*
		 * Look for content management specification
		 */
		Element contents[] = optional(element.getElements(CONTENT, APAM));

		if (contents.length > 1) {
			errorHandler.report(Severity.ERROR, "A single content management is allowed in a composite declaration" + element);
		}

		if (contents.length == 0) {
			return;
		}

		Element content = contents[0];

		parseState(content, declaration);
		parseVisibility(content, declaration);
		parsePromotions(content, declaration);
		parseOwns(content, declaration);

		parseContextualRelations(content, declaration);

		parseOwnedInstances(content, declaration);
	}

	/**
	 * parse a constraints declaration
	 */
	private void parseConstraints(String componentName, Element element, ConstrainedReference reference) {

		for (Element constraint : optional(element.getElements())) {

			String filter = parseString(componentName, constraint, ATT_FILTER);

			if (constraint.getName().equals(IMPLEMENTATION) || constraint.getName().equals(CONSTRAINT)) {
				reference.getImplementationConstraints().add(filter);
			}

			if (constraint.getName().equals(INSTANCE)) {
				reference.getInstanceConstraints().add(filter);
			}

		}

	}

	/**
	 * parse the contextual relations defined in a composite
	 */
	private void parseContextualRelations(Element element, CompositeDeclaration composite) {

		/*
		 * Skip the optional enclosing list
		 */
		for (Element dependencies : every(element.getElements(DEPENDENCIES, APAM), element.getElements(RELATIONS, APAM))) {
			parseContextualRelations(dependencies, composite);
		}

		/*
		 * Iterate over all sub elements looking for relation declarations
		 */
		for (Element relation : every(element.getElements(RELATION, APAM), element.getElements(OVERRIDE, APAM))) {

			/*
			 * Add to content contextual declaration
			 */
			RelationDeclaration relationDeclaration = parseRelation(relation, composite, true);
			Collection<RelationDeclaration> declarations = relationDeclaration.isOverride() ? composite.getOverridenDependencies() : composite.getContextualDependencies();

			if (!declarations.add(relationDeclaration)) {
				errorHandler.report(Severity.ERROR, "Duplicate relation identifier " + relationDeclaration);
			}

		}

	}

	/**
	 * Get an implementation reference coded in an attribute
	 */
	private ImplementationReference<ImplementationDeclaration> parseImplementationReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String implementation = parseString(inComponent, element, attribute, mandatory);
		return ((implementation == null) && !mandatory) ? null : new ImplementationReference<ImplementationDeclaration>(implementation);
	}

	/**
	 * Parse the strategy for field synchronization (NOT compatible with
	 * deprecated internal=true attribute)
	 */
	private InjectedPropertyPolicy parseInjectedPropertyPolicy(String componentName, Element element) {
		String value = parseString(componentName, element, ATT_INJECTED, false);
		InjectedPropertyPolicy injected;
		injected = InjectedPropertyPolicy.getPolicy(value);

		if (value == null) {
			return InjectedPropertyPolicy.BOTH;
		} else {
			return injected;
		}
	}

	/**
	 * Parse an instance declaration
	 */
	private InstanceDeclaration parseInstance(Element element) {

		String name = parseName(element);
		
		ImplementationReference<ImplementationDeclaration> implementation 	= parseImplementationReference(name, element, ATT_IMPLEMENTATION, true);
        String  range 														= parseString(name, element, ATT_REQUIRE_VERSION, false);
    	VersionedReference<ImplementationDeclaration> implementationVersion 			= VersionedReference.range(implementation,range);

		/*
		 * look for optional trigger declarations
		 */

		Element triggers[] = optional(element.getElements(TRIGGER, APAM));
		Element trigger = triggers.length > 0 ? triggers[0] : null;

		if (triggers.length > 1) {
			errorHandler.report(Severity.ERROR, "A single trigger declaration is allowed in an instance declaration" + element);
		}

		/*
		 * Parse triggering conditions
		 */
		Set<ConstrainedReference> triggerDeclarations = new HashSet<ConstrainedReference>();
		for (Element triggerCondition : optional(trigger != null ? trigger.getElements() : null)) {

			/*
			 * ignore elements that are not from APAM
			 */
			if (!isApamDefinition(triggerCondition)) {
				continue;
			}

			ConstrainedReference triggerDeclaration = new ConstrainedReference(parseResolvableReference(name, triggerCondition, ATT_NAME, true));

			/*
			 * parse optional constraints
			 */
			for (Element constraints : optional(triggerCondition.getElements(CONSTRAINTS, APAM))) {
				parseConstraints(name, constraints, triggerDeclaration);
			}

			triggerDeclarations.add(triggerDeclaration);

		}


        InstanceDeclaration declaration = new InstanceDeclaration(implementationVersion,name,triggerDeclarations);
		parseComponent(element, declaration);

		return declaration;
	}

	/**
	 * Get an instance reference coded in an attribute
	 */
	private InstanceReference parseInstanceReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String instance = parseString(inComponent, element, attribute, mandatory);
		return ((instance == null) && !mandatory) ? null : new InstanceReference(instance);
	}

	/**
	 * Get an interface reference coded in an attribute
	 */
	private InterfaceReference parseInterfaceReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String interfaceName = parseString(inComponent, element, attribute, mandatory);
		return ((interfaceName == null) && !mandatory) ? null : new InterfaceReference(interfaceName);
	}

	/**
	 * Test if an attribute is explicitly defined
	 */
	private boolean isDefined(String componentName, Element element, String attribute) {
		return (parseString(componentName, element, attribute, false) != null);
	}

	/**
	 * Get a missing policy attribute value
	 */
	private ComponentKind parseKind(String componentName, Element element, String attribute, boolean mandatory, ComponentKind defaultValue) {

		String encodedKind = parseString(componentName, element, attribute, mandatory);

		if ((encodedKind == null) && !mandatory) {
			return defaultValue;
		}

		if ((encodedKind == null) && mandatory) {
			return null;
		}

		if (VALUE_KIND_INSTANCE.equalsIgnoreCase(encodedKind)) {
			return ComponentKind.INSTANCE;
		}

		if (VALUE_KIND_IMPLEMENTATION.equalsIgnoreCase(encodedKind)) {
			return ComponentKind.IMPLEMENTATION;
		}

		if (VALUE_KIND_SPECIFICATION.equalsIgnoreCase(encodedKind)) {
			return ComponentKind.SPECIFICATION;
		}

		errorHandler.report(Severity.ERROR, "in component " + componentName + " invalid value for component kind : \"" + encodedKind + "\",  accepted values are " + KIND_VALUES.toString());
		return null;
	}

	/**
	 * Get a message reference coded in an attribute
	 */
	private MessageReference parseMessageReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String messageName = parseString(inComponent, element, attribute, mandatory);
		return ((messageName == null) && !mandatory) ? null : new MessageReference(messageName);
	}
	
	/**
	 * Get a package reference coded in an attribute
	 */
	private PackageReference parsePackageReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String packageName = parseString(inComponent, element, attribute, mandatory);
		return ((packageName == null) && !mandatory) ? null : new PackageReference(packageName);
	}	

	/**
	 * Get a mandatory element name
	 */
	private final String parseName(Element element) {
		return parseString(element.getAttribute(ATT_NAME), element, ATT_NAME);
	}

	/**
	 * Parse the list of owned instances of a composite
	 */
	private void parseOwnedInstances(Element element, CompositeDeclaration composite) {

		for (Element start : optional(element.getElements(START, APAM))) {
			composite.getInstanceDeclarations().add(parseInstance(start));
		}

	}

	/**
	 * Parse the list of owned components of a composite
	 */
	private void parseOwns(Element element, CompositeDeclaration composite) {
		for (Element owned : optional(element.getElements(OWN, APAM))) {

			ComponentReference<?> ownedComponentTarget = parseComponentReference(composite.getName(), owned, true);
			String property = parseString(composite.getName(), owned, ATT_PROPERTY, false);
			String values = parseString(composite.getName(), owned, ATT_VALUE, property != null);

			OwnedComponentDeclaration ownedComponent = new OwnedComponentDeclaration(ownedComponentTarget, property, new HashSet<String>(list(values,true)));

			/*
			 * parse optional grants
			 */
			for (Element grant : optional(owned.getElements(GRANT, APAM))) {

				ComponentReference<?> definingComponent = parseComponentReference(composite.getName(), grant, true);
				String identifier = parseString(composite.getName(), grant, ATT_RELATION, false);
				identifier = identifier != null ? identifier : ownedComponent.getComponent().getName();
				RelationDeclaration.Reference relation = new RelationDeclaration.Reference(definingComponent, identifier);

				String states = parseString(composite.getName(), grant, ATT_WHEN, true);

				GrantDeclaration grantDeclaration = new GrantDeclaration(relation, new HashSet<String>(list(states,true)));
				ownedComponent.getGrants().add(grantDeclaration);
			}

			/*
			 * parse explicit denies
			 */
			for (Element deny : optional(owned.getElements(DENY, APAM))) {

				ComponentReference<?> definingComponent = parseComponentReference(composite.getName(), deny, true);
				String identifier = parseString(composite.getName(), deny, ATT_RELATION, false);
				identifier = identifier != null ? identifier : ownedComponent.getComponent().getName();
				RelationDeclaration.Reference relation = new RelationDeclaration.Reference(definingComponent, identifier);

				String states = parseString(composite.getName(), deny, ATT_WHEN, true);

				GrantDeclaration denyDeclaration = new GrantDeclaration(relation, new HashSet<String>(list(states,true)));
				ownedComponent.getDenies().add(denyDeclaration);
			}

			composite.getOwnedComponents().add(ownedComponent);
		}
	}

	/**
	 * Get a missing policy attribute value
	 */
	private MissingPolicy parsePolicy(String componentName, Element element, String attribute, boolean mandatory, MissingPolicy defaultValue) {

		String encodedPolicy = parseString(componentName, element, attribute, mandatory);

		if ((encodedPolicy == null) && !mandatory) {
			return defaultValue;
		}

		if ((encodedPolicy == null) && mandatory) {
			return null;
		}

		if (VALUE_WAIT.equalsIgnoreCase(encodedPolicy)) {
			return MissingPolicy.WAIT;
		}

		if (VALUE_OPTIONAL.equalsIgnoreCase(encodedPolicy)) {
			return MissingPolicy.OPTIONAL;
		}

		if (VALUE_EXCEPTION.equalsIgnoreCase(encodedPolicy)) {
			return MissingPolicy.EXCEPTION;
		}

		errorHandler.report(Severity.ERROR, "in component " + componentName + " invalid value for missing policy : \"" + encodedPolicy + "\",  accepted values are " + MISSING_VALUES.toString());
		return null;
	}

	/**
	 * parse a preferences declaration
	 */
	private void parsePreferences(String componentName, Element element, ConstrainedReference reference) {

		for (Element preference : optional(element.getElements())) {

			String filter = parseString(componentName, preference, ATT_FILTER);

			if (preference.getName().equals(IMPLEMENTATION) || preference.getName().equals(CONSTRAINT)) {
				reference.getImplementationPreferences().add(filter);
			}

			if (preference.getName().equals(INSTANCE)) {
				reference.getInstancePreferences().add(filter);
			}

		}

	}

	/**
	 * Parse an atomic implementation declaration
	 */
	private AtomicImplementationDeclaration parsePrimitive(Element element) {

		String name = parseName(element);

		/*
		 * load implementation class and Pojo instrumentation metadata
		 */

		String className = parseString(name, element, ATT_CLASSNAME, true);

		PojoMetadata pojoMetadata = null;
		Class<?> instrumentedCode = null;
		try {
			pojoMetadata = new PojoMetadata(element);
			instrumentedCode = ((className != Decoder.UNDEFINED) && (introspector != null)) ? introspector.getInstrumentedClass(className) : null;
		} catch (ClassNotFoundException e) {
			errorHandler.report(Severity.ERROR, "Apam component " + name + ": " + "the component class " + className + " can not be loaded");
		} catch (Exception ignoredException) {
		}

		InstrumentedClass instrumentedClass = new InstrumentedClassMetadata(className, pojoMetadata, instrumentedCode);

		/*
		 * load specification
		 */
		SpecificationReference specification						= parseSpecificationReference(name, element, ATT_SPECIFICATION, false);
        String  versionRange										= parseString(name, element, ATT_REQUIRE_VERSION, false);
        VersionedReference<SpecificationDeclaration> specificationVersion	= specification != null ? VersionedReference.range(specification,versionRange) : null;
        
        AtomicImplementationDeclaration declaration = new AtomicImplementationDeclaration(name, specificationVersion, instrumentedClass);
        
		parseComponent(element, declaration);

		/*
		 * Parse message producer method interception
		 */
		String messageMethods = parseString(name, element, ATT_PUSH, false);
		for (String messageMethod : list(messageMethods,true)) {

			/*
			 * Parse optionally specified method signature
			 */
			String methodName = messageMethod.trim();
			String methodSignature = null;

			if (methodName.indexOf("(") != -1 && methodName.endsWith(")")) {
				methodSignature = methodName.substring(methodName.indexOf("(") + 1, methodName.length() - 1);
				methodName = methodName.substring(0, methodName.indexOf("(") - 1);
			}

			ProviderInstrumentation instrumentation = new ProviderInstrumentation.MessageProviderMethodInterception(declaration, methodName, methodSignature);

			if (!instrumentation.isValidInstrumentation()) {
				errorHandler.report(Severity.ERROR, name + " : the specified method \"" + ATT_PUSH + "\" in \"" + ATT_PUSH + "\" is invalid or not found");
			}

			declaration.getProviderInstrumentation().add(instrumentation);
		}

		/*
		 * Verify that at least one method is intercepted for each declared
		 * produced message.
		 */
		for (MessageReference message : declaration.getProvidedResources(MessageReference.class)) {

			boolean declared = declaration.getProviderInstrumentation().size() > 0;
			boolean injected = false;
			boolean defined = false;

			for (ProviderInstrumentation providerInstrumentation : declaration.getProviderInstrumentation()) {

				ResourceReference instrumentedResource = providerInstrumentation.getProvidedResource();

				if (instrumentedResource instanceof UnknownReference) {
					continue;
				}

				defined = true;

				if (!instrumentedResource.equals(message)) {
					continue;
				}

				injected = true;
				break;
			}

			/*
			 * If we could determine the method types and there was no injection
			 * then signal error
			 * 
			 * NOTE Notice that some errors will not be detected at build time
			 * since all the reflection information is not available, and
			 * validation must be delayed until run time
			 */
			if (!declared || (defined && !injected)) {
				errorHandler.report(Severity.ERROR, "Apam component " + name + ": " + " message of type " + message.getJavaType() + " is not produced by any push method");
			}

		}

		/*
		 * if not explicitly provided, get all the implemented interfaces.
		 */
		if (declaration.getProvidedResources().isEmpty() && (pojoMetadata != null)) {
			for (String implementedInterface : pojoMetadata.getInterfaces()) {
				declaration.getProvidedResources().add(new InterfaceReference(implementedInterface));
			}
		}

		/*
		 * If not explicitly provided, get all produced messages from the
		 * declared intercepted methods
		 */
		Set<MessageReference> declaredMessages = declaration.getProvidedResources(MessageReference.class);
		for (ProviderInstrumentation providerInstrumentation : declaration.getProviderInstrumentation()) {

			MessageReference instrumentedMessage = providerInstrumentation.getProvidedResource().as(MessageReference.class);

			if (instrumentedMessage == null) {
				continue;
			}

			if (declaredMessages.contains(instrumentedMessage)) {
				continue;
			}

			declaration.getProvidedResources().add(instrumentedMessage);
		}

		/*
		 * If instrumented code is provided verify that all provided resources reference accessible classes
		 */
		if (introspector != null) {
			for (ResourceReference providedResource : declaration.getProvidedResources()) {

				if (providedResource instanceof UnknownReference	|| providedResource instanceof PackageReference) {
					continue;
				}
				
				// TODO : check the provided package (one class belonging to this package should exists)

				try {
					introspector.getInstrumentedClass(providedResource.getJavaType());
				} catch (ClassNotFoundException e) {
					errorHandler.report(Severity.ERROR, "Apam component " + name + ": " + "the provided resource " + providedResource.getJavaType() + " can not be loaded");
				}
			}
		}

		/*
		 * Iterate over all sub elements looking for callback declarations
		 */
		for (Element callback : optional(element.getElements(CALLBACK, APAM))) {
			String onInit = parseString(name, callback, ATT_ON_INIT, false);
			String onRemove = parseString(name, callback, ATT_ON_REMOVE, false);

			if (onInit != null) {
				declaration.addCallback(AtomicImplementationDeclaration.Event.INIT, parseCallback(declaration, onInit));
			}

			if (onRemove != null) {
				declaration.addCallback(AtomicImplementationDeclaration.Event.REMOVE, parseCallback(declaration, onRemove));
			}

		}

		return declaration;

	}

	/**
	 * Parse the list of promoted dependencies of a composite
	 */
	private void parsePromotions(Element element, CompositeDeclaration composite) {
		for (Element promotion : optional(element.getElements(PROMOTE, APAM))) {

			RelationDeclaration.Reference source = parseRelationReference(composite.getName(), promotion, true);
			String target = parseString(composite.getName(), promotion, ATT_TO);

			composite.getPromotions().add(new RelationPromotion(source, new RelationDeclaration.Reference(composite.getReference(), target)));
		}
	}

	/**
	 * parse the properties of the component
	 */
	private void parseProperties(Element element, ComponentDeclaration component) {

		/*
		 * Skip the optional enclosing list
		 */
		for (Element properties : optional(element.getElements(PROPERTIES, APAM))) {
			parseProperties(properties, component);
		}

		for (Element property : optional(element.getElements(PROPERTY, APAM))) {

			/*
			 * If a name is specified, get the associated value
			 */
			String name = parseString(component.getName(), property, ATT_NAME);
			String value = parseString(component.getName(), property, ATT_VALUE);
			component.getProperties().put(name, value);

		}
	}

	/**
	 * parse the property definitions of the component
	 */
	private void parsePropertyDefinitions(Element element, ComponentDeclaration component) {

		/*
		 * Skip the optional enclosing list
		 */
		for (Element definitions : optional(element.getElements(DEFINITIONS, APAM))) {
			parsePropertyDefinitions(definitions, component);
		}

		for (Element definition : optional(element.getElements(DEFINITION, APAM))) {

			String name = parseString(component.getName(), definition, ATT_NAME);
			String type = parseString(component.getName(), definition, ATT_TYPE);
			String defaultValue = parseString(component.getName(), definition, ATT_DEFAULT, false);

			String field = null;
			String callback = null;
			InjectedPropertyPolicy injected = null;

			if (component instanceof AtomicImplementationDeclaration) {
				field = parseString(component.getName(), definition, ATT_FIELD, false);
				callback = parseString(component.getName(), definition, ATT_METHOD, false);
				injected = parseInjectedPropertyPolicy(component.getName(), definition);

			}

			component.getPropertyDefinitions().add(new PropertyDefinition(component.getReference(), name, type, defaultValue, field, callback, injected));

		}
	}

	/**
	 * Get a property declaration reference coded in the element
	 */
	private PropertyDefinition.Reference parsePropertyReference(String inComponent, Element element, boolean mandatory) {

		ComponentReference<?> definingComponent = parseComponentReference(inComponent, element, mandatory);
		String identifier = parseString(definingComponent.getName(), element, ATT_PROPERTY, mandatory);

		if (!mandatory && (definingComponent == null || identifier == null)) {
			return null;
		}

		return new PropertyDefinition.Reference(definingComponent, identifier);
	}

	/**
	 * parse the provided resources of a component
	 */
	private void parseProvidedResources(Element element, ComponentDeclaration component) {

		String interfaces = parseString(component.getName(), element, ATT_INTERFACES, false);
		String messages = parseString(component.getName(), element, ATT_MESSAGES, false);
		String packages = parseString(component.getName(), element, ATT_PACKAGES, false);

		for (String interfaceName : list(interfaces,true)) {
			component.getProvidedResources().add(new InterfaceReference(interfaceName));
		}

		for (String message : list(messages,true)) {
			component.getProvidedResources().add(new MessageReference(message));
		}
		
		for (String reqpackage : list(packages,true)) {
			component.getProvidedResources().add(new PackageReference(reqpackage));
		}		

	}

	/**
	 * parse a relation declaration
	 */
	private RelationDeclaration parseRelation(Element element, ComponentDeclaration component) {
		return parseRelation(element, component, false);
	}

	/**
	 * parse a relation declaration
	 */
	private RelationDeclaration parseRelation(Element element, ComponentDeclaration component, boolean isContextual) {
		/*
		 * Get the reference to the target of the relation if specified
		 */
		ResolvableReference targetDef = parseResolvableReference(component.getName(), element, false);

		/*
		 * All dependencies have an optional identifier and multiplicity
		 * specification, as well as an optional source kind and target kind
		 */
		String id = parseString(component.getName(), element, ATT_NAME, false);
		boolean isOverride = isContextual && element.getName().equals(OVERRIDE);

		boolean isMultiple = parseBoolean(component.getName(), element, ATT_MULTIPLE, false, false);

		String sourceName = parseString(component.getName(), element, ATT_SOURCE, isContextual && !isOverride);

		ComponentKind sourceKind = parseKind(component.getName(), element, ATT_SOURCE_KIND, isContextual && !isOverride, null);
		ComponentKind targetKind = parseKind(component.getName(), element, ATT_TARGET_KIND, false, null);

		/*
		 * For atomic components, dependency declarations may optionally have a number of nested instrumentation declarations.
		 * 
		 * These are parsed first, as a number of attributes of the relation that are not explicitly declared can be inferred
		 * from the instrumentation metadata.
		 */

		List<RequirerInstrumentation> instrumentations = new ArrayList<RequirerInstrumentation>();
		if (component instanceof AtomicImplementationDeclaration) {

			AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;

			/*
			 * Optionally, as a shortcut, a single injection may be specified
			 * directly as an attribute of the relation
			 */
			RequirerInstrumentation directInstrumentation = parseRelationInstrumentation(element, atomic, false);
			if (directInstrumentation != null) {
				instrumentations.add(directInstrumentation);
			}

			for (Element instrumentation : optional(element.getElements())) {

				/*
				 * ignore elements that are not from APAM
				 */
				if (!isApamDefinition(instrumentation)) {
					continue;
				}

				/*
				 * Accept only resource references
				 */
				String resourceKind = instrumentation.getName();
				if (!(INTERFACE.equals(resourceKind) || MESSAGE.equals(resourceKind) 
						|| PACKAGE.equals(resourceKind))) {
					continue;
				}

				instrumentations.add(parseRelationInstrumentation(instrumentation, atomic, true));

			}

		}


		/*
		 * If no ID was explicitly specified, but a single instrumentation was declared the the name of the field or 
		 * method becomes the ID of the relation.
		 */
		if (id == null && instrumentations.size() == 1) {
			id = instrumentations.get(0).getName();
		}

		/*
		 * If no target was explicitly specified, sometimes we can infer it from the instrumentation metadata.
		 */
		if (!instrumentations.isEmpty() && (targetDef == null || targetKind == null)) {

			ComponentKind inferredKind = null;
			ResolvableReference inferredTarget = null;

			for (RequirerInstrumentation instrumentation : instrumentations) {

				String javaType = instrumentation.getRequiredResource().getJavaType();
				ComponentKind candidateKind = null;
				ResolvableReference candidateTarget = null;

				if (ComponentKind.COMPONENT.isAssignableTo(javaType)) {
					candidateKind = null;
					candidateTarget = null;
				} else if (ComponentKind.SPECIFICATION.isAssignableTo(javaType)) {
					candidateKind = ComponentKind.SPECIFICATION;
					candidateTarget = null;
				} else if (ComponentKind.IMPLEMENTATION.isAssignableTo(javaType)) {
					candidateKind = ComponentKind.IMPLEMENTATION;
					candidateTarget = null;
				} else if (ComponentKind.INSTANCE.isAssignableTo(javaType)) {
					candidateKind = ComponentKind.INSTANCE;
					candidateTarget = null;
				} else {
					candidateKind = ComponentKind.INSTANCE;
					candidateTarget = instrumentation.getRequiredResource();
				}

				/*
				 * If there are conflicting declarations we gave up inferring
				 * target
				 */
				if (inferredKind != null && candidateKind != null && !inferredKind.equals(candidateKind)) {
					inferredKind = null;
					inferredTarget = null;
					break;
				}

				if (inferredTarget != null && candidateTarget != null && !inferredTarget.equals(candidateTarget)) {
					inferredKind = null;
					inferredTarget = null;
					break;
				}

				inferredKind = candidateKind != null ? candidateKind : inferredKind;
				inferredTarget = candidateTarget != null ? candidateTarget : inferredTarget;
			}

			if (targetDef == null && inferredTarget != null) {
				targetDef = inferredTarget;
			}

			if (targetKind == null && inferredKind != null) {
				targetKind = inferredKind;
			}
		}

		if (id == null && targetDef == null) {
			errorHandler.report(Severity.ERROR, "relation name or target must be specified " + element);
		}


		/*
		 * Get the resolution policies
		 */
		String creationPolicyString = parseString(component.getName(), element, ATT_CREATION_POLICY, false);
		CreationPolicy creationPolicy = CreationPolicy.getPolicy(creationPolicyString);

		String resolvePolicyString = parseString(component.getName(), element, ATT_RESOLVE_POLICY, false);
		ResolvePolicy resolvePolicy = ResolvePolicy.getPolicy(resolvePolicyString);

		/*
		 * Get the optional missing policy
		 */
		MissingPolicy missingPolicy = parsePolicy(component.getName(), element, ATT_FAIL, false, null);
		String missingException = parseString(component.getName(), element, ATT_EXCEPTION, false);

		/*
		 * Get the optional contextual properties
		 */
		String mustHide = parseString(component.getName(), element, ATT_HIDE, false);

		/*
		 * Create the relation and add the declared instrumentation
		 */
		RelationDeclaration relation;
		if (! isContextual) {
			relation = new RelationDeclaration(component.getReference(), id, sourceKind, targetDef, targetKind, isMultiple,
								creationPolicy, resolvePolicy, 
								missingPolicy, missingException, mustHide != null ? Boolean.valueOf(mustHide) : null); 
		}
		else {
			relation = new RelationDeclaration(component.getReference(), id, sourceKind, targetDef, targetKind, isMultiple,
					creationPolicy, resolvePolicy, 
					missingPolicy, missingException, mustHide != null ? Boolean.valueOf(mustHide) : null,
					true, sourceName, isOverride);
		}
					

		for (RequirerInstrumentation instrumentation : instrumentations) {
			relation.getInstrumentations().add(instrumentation);
		}
		
		/*
		 * look for bind and unbind callbacks
		 */
		String bindCallback = parseString(component.getName(), element, ATT_BIND, false);
		String unbindCallback = parseString(component.getName(), element, ATT_UNBIND, false);

		if (component instanceof AtomicImplementationDeclaration) {
			if (bindCallback != null) {
				CallbackDeclaration callback = new CallbackDeclaration((AtomicImplementationDeclaration) component, bindCallback,true);
				if (!callback.isValidInstrumentation()) {
					errorHandler.report(Severity.ERROR, component.getName() + " : the specified method \"" + bindCallback + "\" in \"" + ATT_BIND + "\" is invalid or not found");
				}
				relation.addCallback(RelationDeclaration.Event.BIND, callback);
			}
			if (unbindCallback != null) {
				CallbackDeclaration callback = new CallbackDeclaration((AtomicImplementationDeclaration) component, unbindCallback,true);
				if (!callback.isValidInstrumentation()) {
					errorHandler.report(Severity.ERROR, component.getName() + " : the specified method \"" + unbindCallback + "\" in \"" + ATT_UNBIND + "\" is invalid or not found");
				}
				relation.addCallback(RelationDeclaration.Event.UNBIND, callback);
			}
		}

		/*
		 * Get the optional constraints and preferences
		 */
		for (Element constraints : optional(element.getElements(CONSTRAINTS, APAM))) {
			parseConstraints(component.getName(), constraints, relation);
		}

		for (Element preferences : optional(element.getElements(PREFERENCES, APAM))) {
			parsePreferences(component.getName(), preferences, relation);
		}

		return relation;
	}

	/**
	 * parse the injected dependencies of a primitive
	 */
	private RequirerInstrumentation parseRelationInstrumentation(Element element, AtomicImplementationDeclaration atomic, boolean mandatory) {

		String field = parseString(atomic.getName(), element, ATT_FIELD, false);
		// String method = parseString(element, CoreATT_METHOD,
		// false);

		String push = parseString(atomic.getName(), element, ATT_PUSH, false);
		String pull = parseString(atomic.getName(), element, ATT_PULL, false);

		if ((field == null) && (push == null) && (pull == null) && mandatory) {
			errorHandler.report(Severity.ERROR, "in the component \"" + atomic.getName() + "\" relation attribute \"" + ATT_FIELD + "\" or \"" + ATT_PUSH + "\" or \"" + ATT_PULL + "\" must be specified in " + element.getName());
		}

		if ((field == null) && INTERFACE.equals(element.getName()) && mandatory) {
			errorHandler.report(Severity.ERROR, "in the component \"" + atomic.getName() + "\" relation attribute \"" + ATT_FIELD + "\" must be specified in " + element.getName());
		}

		if ((push == null) && (pull == null) && MESSAGE.equals(element.getName()) && mandatory) {
			errorHandler.report(Severity.ERROR, "in the component \"" + atomic.getName() + "\" relation attribute \"" + ATT_PUSH + " or " + ATT_PULL + "\" must be specified in " + element.getName());
		}

		if ((field == null) && (push == null) && (pull == null)) {
			return mandatory ? new RequirerInstrumentation.RequiredServiceField(atomic, Decoder.UNDEFINED) : null;
		}

		RequirerInstrumentation instrumentation = null;

		if (field != null) {
			instrumentation = new RequirerInstrumentation.RequiredServiceField(atomic, field);
		} else if (push != null) {
			instrumentation = new RequirerInstrumentation.MessageConsumerCallback(atomic, push);
		} else if (pull != null) {
			instrumentation = new RequirerInstrumentation.MessageQueueField(atomic, pull);
		}

		if (!instrumentation.isValidInstrumentation()) {
			errorHandler.report(Severity.ERROR, atomic.getName() + " : invalid class type for field or method " + instrumentation.getName());
		}

		return instrumentation;
	}

	/**
	 * Get a relation declaration reference coded in the element
	 */
	private RelationDeclaration.Reference parseRelationReference(String inComponent, Element element, boolean mandatory) {

		ComponentReference<?> definingComponent = parseComponentReference(inComponent, element, mandatory);
		String identifier = parseString(definingComponent.getName(), element, ATT_RELATION, mandatory);

		if (!mandatory && (definingComponent == null || identifier == null)) {
			return null;
		}

		return new RelationDeclaration.Reference(definingComponent, identifier);
	}

	/**
	 * parse the declared relations of a component
	 */
	private void parseRelations(Element element, ComponentDeclaration component) {

		/*
		 * Skip the optional enclosing list
		 */
		for (Element dependencies : every(element.getElements(DEPENDENCIES, APAM), element.getElements(RELATIONS, APAM))) {
			parseRelations(dependencies, component);
		}

		/*
		 * Iterate over all sub elements looking for relation declarations
		 */
		for (Element relation : every(element.getElements(DEPENDENCY, APAM), element.getElements(RELATION, APAM))) {
			/*
			 * Add to component declaration
			 */
			RelationDeclaration relationDeclaration = parseRelation(relation, component);
			if (!component.getRelations().add(relationDeclaration)) {
				errorHandler.report(Severity.ERROR, "Duplicate relation identifier " + relationDeclaration);
			}

		}

	}

	/**
	 * Get a resolvable reference implicitly coded in the element (either in a
	 * name attribute or an attribute named after the kind of reference)
	 */
	private ResolvableReference parseResolvableReference(String inComponent, Element element, boolean mandatory) {

		String attribute = Decoder.UNDEFINED;

		/*
		 * If the kind of reference is coded in the element name, the actual
		 * value must be coded in the attribute NAME
		 */
		if (ALL_REFERENCES.contains(element.getName())) {
			attribute = ATT_NAME;
		}

		/*
		 * Otherwise try to find a defined attribute matching the kind of
		 * reference
		 */
		for (Attribute definedAttribute : element.getAttributes()) {

			if (!ALL_REFERENCES.contains(definedAttribute.getName())) {
				continue;
			}

			attribute = definedAttribute.getName();
			break;
		}

		if (attribute.equals(Decoder.UNDEFINED) && mandatory) {
			errorHandler.report(Severity.ERROR, "component name or resource must be specified in " + element.getName());
			return new UnknownReference(new ResourceReference(Decoder.UNDEFINED));
		}

		if (attribute.equals(Decoder.UNDEFINED) && !mandatory) {
			return null;
		}

		return parseResolvableReference(inComponent, element, attribute, mandatory);
	}

	/**
	 * Get a resolvable reference coded in an attribute
	 */
	private ResolvableReference parseResolvableReference(String inComponent, Element element, String attribute, boolean mandatory) {

		String referenceKind = getReferenceKind(element, attribute);

		if (COMPONENT_REFERENCES.contains(referenceKind)) {
			return parseComponentReference(inComponent, element, attribute, mandatory);
		}

		if (RESOURCE_REFERENCES.contains(referenceKind)) {
			return parseResourceReference(inComponent, element, attribute, mandatory);
		}

		if (mandatory) {
			errorHandler.report(Severity.ERROR, "component name or resource must be specified in " + element.getName());
			return  new UnknownReference(new ResourceReference(Decoder.UNDEFINED));
		}

		return null;
	}

	/**
	 * Get a resource reference coded in an attribute
	 */
	private ResourceReference parseResourceReference(String inComponent, Element element, String attribute, boolean mandatory) {

		String referenceKind = getReferenceKind(element, attribute);

		if (INTERFACE.equals(referenceKind)) {
			return parseInterfaceReference(inComponent, element, attribute, mandatory);
		}

		if (MESSAGE.equals(referenceKind)) {
			return parseMessageReference(inComponent, element, attribute, mandatory);
		}
		
		if (PACKAGE.equals(referenceKind)) {
			return parsePackageReference(inComponent, element, attribute, mandatory);
		}
		

		if (mandatory) {
			errorHandler.report(Severity.ERROR, "resource name must be specified in " + element.getName());
			return new UnknownReference(new ResourceReference(element.getName()));
		}

		return null;
	}

	/**
	 * Parse an specification declaration
	 */
	private SpecificationDeclaration parseSpecification(Element element) {

		SpecificationDeclaration declaration = new SpecificationDeclaration(parseName(element));
		parseComponent(element, declaration);

		return declaration;
	}

	/**
	 * Get an specification reference coded in an attribute
	 */
	private SpecificationReference parseSpecificationReference(String inComponent, Element element, String attribute, boolean mandatory) {
		String specification = parseString(inComponent, element, attribute, mandatory);
		return ((specification == null) && !mandatory) ? null : new SpecificationReference(specification);
	}

	/**
	 * Parse the definition of the state of a composite
	 */
	private void parseState(Element element, CompositeDeclaration composite) {
		/*
		 * Look for content management specification
		 */
		Element states[] = optional(element.getElements(STATE, APAM));

		if (states.length > 1) {
			errorHandler.report(Severity.ERROR, "A single state declaration is allowed in a composite declaration" + element);
		}

		if (states.length == 0) {
			return;
		}

		Element state = states[0];

		composite.setStateProperty(parsePropertyReference(composite.getName(), state, true));
	}

	/**
	 * Get a mandatory string attribute value
	 */
	private final String parseString(String componentName, Element element, String attribute) {
		return parseString(componentName, element, attribute, true);
	}

	/**
	 * Get a string attribute value
	 */
	private final String parseString(String componentName, Element element, String attribute, boolean mandatory) {
		String value = element.getAttribute(attribute);

		if (mandatory && (value == null)) {
			errorHandler.report(Severity.ERROR, "in component \"" + componentName + "\" attribute \"" + attribute + "\" must be specified in " + element.getName());
			value = Decoder.UNDEFINED;
		}

		if (mandatory && (value != null) && value.trim().isEmpty()) {
			errorHandler.report(Severity.ERROR, "in component \"" + componentName + "\" attribute \"" + attribute + "\" cannot be empty in " + element.getName());
			value = Decoder.UNDEFINED;
		}

		return value;
	}

	/**
	 * Get a list of strings
	 */
	private static final Pattern SIMPLE_LIST_PATTERN 	= Pattern.compile(",");
	private static final Pattern TRIMMED_LIST_PATTERN 	= Pattern.compile("\\s*,\\s*");

	private List<String> list(String encodedList, boolean trimmed) {
		Pattern pattern = trimmed ? TRIMMED_LIST_PATTERN : SIMPLE_LIST_PATTERN;
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
				errorHandler.report(Severity.ERROR,"Invalid string. \"}\" missing: " + encodedList);
				return encodedList.substring(1).trim();
			}
		}
		
		return encodedList.trim();
	}
	
	/**
	 * Parse the visibility rules for the content of a composite
	 */
	private void parseVisibility(Element element, CompositeDeclaration composite) {

		for (Element rule : optional(element.getElements())) {

			String implementationsRule = parseString(composite.getName(), rule, ATT_IMPLEMENTATION, false);
			String instancesRule = parseString(composite.getName(), rule, ATT_INSTANCE, false);

			if (rule.getName().equals(IMPORTS)) {
				if (implementationsRule != null) {
					composite.getVisibility().setBorrowImplementations(implementationsRule);
				}
				if (instancesRule != null) {
					composite.getVisibility().setImportInstances(instancesRule);
				}
			}

			// if (rule.getName().equals(CoreFRIEND)) {
			// if (implementationsRule != null) {
			// composite.getVisibility().setFriendImplementations(implementationsRule);
			// }
			// if (instancesRule != null) {
			// composite.getVisibility().setFriendInstances(instancesRule);
			// }
			// }

			if (rule.getName().equals(EXPORT)) {
				if (implementationsRule != null) {
					composite.getVisibility().setExportImplementations(implementationsRule);
				}

				if (instancesRule != null) {
					composite.getVisibility().setExportInstances(instancesRule);
				}
			}

			if (rule.getName().equals(EXPORTAPP)) {

				if (instancesRule != null) {
					composite.getVisibility().setApplicationInstances(instancesRule);
				}
			}

		}

	}


}
