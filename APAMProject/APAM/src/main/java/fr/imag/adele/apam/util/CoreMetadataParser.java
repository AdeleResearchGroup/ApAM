package fr.imag.adele.apam.util;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration.Instrumentation;
import fr.imag.adele.apam.core.CallbackMethod;
import fr.imag.adele.apam.core.CallbackMethod.CallbackTrigger;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ConstrainedReference;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.DependencyPromotion;
import fr.imag.adele.apam.core.GrantDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InstanceReference;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageProducerMethodInterception;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.MissingPolicy;
import fr.imag.adele.apam.core.OwnedComponentDeclaration;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.core.UndefinedReference;
import fr.imag.adele.apam.message.Message;
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
    private static final String  APAM                    = "fr.imag.adele.apam";
    private static final String  COMPONENT               = "component";
    private static final String  SPECIFICATION           = "specification";
    private static final String  IMPLEMENTATION          = "implementation";
    private static final String  COMPOSITE               = "composite";
    private static final String  INSTANCE                = "instance";
    private static final String  INSTANCE_ALT            = "apam-instance";
    private static final String  DEFINITIONS             = "definitions";
    private static final String  DEFINITION              = "definition";
    private static final String  PROPERTIES              = "properties";
    private static final String  PROPERTY                = "property";
    private static final String  DEPENDENCIES            = "dependencies";
    private static final String  DEPENDENCY              = "dependency";
    private static final String  INTERFACE               = "interface";
    private static final String  MESSAGE                 = "message";
    private static final String  CONSTRAINTS             = "constraints";
    private static final String  CONSTRAINT              = "constraint";
    private static final String  PREFERENCES             = "preferences";
    private static final String  CONTENT                 = "contentMngt";
    private static final String  START                   = "start";
    private static final String  TRIGGER                 = "trigger";
    private static final String  OWN                     = "own";
    private static final String  PROMOTE                 = "promote";
    private static final String  GRANT                   = "grant";
    private static final String  STATE                   = "state";
    private static final String  BORROW                  = "borrow";
    private static final String  LOCAL                   = "local";
    private static final String  FRIEND                  = "friend";
    private static final String  APPLICATION             = "application";
    private static final String  CALLBACK                = "callback";

    private static final String  ATT_NAME                = "name";
    private static final String  ATT_CLASSNAME           = "classname";
    private static final String  ATT_EXCLUSIVE           = "exclusive";
    private static final String  ATT_SINGLETON           = "singleton";
    private static final String  ATT_SHARED              = "shared";
    private static final String  ATT_INSTANTIABLE        = "instantiable";
    private static final String  ATT_SPECIFICATION       = "specification";
    private static final String  ATT_IMPLEMENTATION      = "implementation";
    private static final String  ATT_MAIN_IMPLEMENTATION = "main";              // "mainImplem"
    private static final String  ATT_INSTANCE            = "instance";
    private static final String  ATT_INTERFACES          = "interfaces";
    private static final String  ATT_MESSAGES            = "messages";
//    private static final String  ATT_MESSAGE_METHODS     = "message-methods";
    private static final String  ATT_TYPE                = "type";
    private static final String  ATT_VALUE               = "value";
    private static final String  ATT_FIELD               = "field";
    private static final String  ATT_INTERNAL            = "internal";
    private static final String  ATT_MULTIPLE            = "multiple";
    private static final String  ATT_FAIL                = "fail";
    private static final String  ATT_EXCEPTION           = "exception";
    private static final String  ATT_EAGER               = "eager";
    private static final String  ATT_HIDE                = "hide";
    private static final String  ATT_FILTER              = "filter";
    private static final String  ATT_ID                  = "id";
    private static final String  ATT_PROPERTY            = "property";
    private static final String  ATT_DEPENDENCY          = "dependency";
    private static final String  ATT_TO                  = "to";
    private static final String  ATT_WHEN                = "when";
    private static final String  ATT_ON_REMOVE           = "onRemove";
    private static final String  ATT_ON_INIT             = "onInit";
    private static final String  ATT_METHOD              = "method";
    private static final String  ATT_PUSH                = "push";
    private static final String  ATT_PULL                = "pull";

    private static final String  VALUE_OPTIONAL          = "optional";
    private static final String  VALUE_WAIT              = "wait";
    private static final String  VALUE_EXCEPTION         = "exception";

    /**
     * The parsed metatadata
     */
    private Element              metadata;

    /**
     * The optional service that give access to introspection information
     */
    private IntrospectionService introspector;

    /**
     * A service to access introspection information for primitive components
     */
    public interface IntrospectionService {

        /**
         * Get reflection information for the implementation class
         */
        public Class<?> getInstrumentedClass(String classname) throws ClassNotFoundException;
    }

    /**
     * The last parsed declarations
     */
    private List<ComponentDeclaration> declaredElements;

    /**
     * The currently used error handler
     */
    private ErrorHandler               errorHandler;

    public CoreMetadataParser(Element metadata) {
        this(metadata, null);
    }

    public CoreMetadataParser(Element metadata, IntrospectionService introspector) {
        setMetadata(metadata, introspector);
    }

    /**
     * Initialize parser with the given metadata and instrumentation code
     */
    public synchronized void setMetadata(Element metadata, IntrospectionService introspector) {
        this.metadata = metadata;
        this.introspector = introspector;
        declaredElements = null;
    }

    /**
     * Parse the ipojo metadata to get the component declarations
     */
    @Override
    public synchronized List<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler) {
        if (declaredElements != null)
            return declaredElements;

        declaredElements = new ArrayList<ComponentDeclaration>();
        this.errorHandler = errorHandler;

        List<SpecificationDeclaration> specifications = new ArrayList<SpecificationDeclaration>();
        List<AtomicImplementationDeclaration> primitives = new ArrayList<AtomicImplementationDeclaration>();
        List<CompositeDeclaration> composites = new ArrayList<CompositeDeclaration>();
        List<InstanceDeclaration> instances = new ArrayList<InstanceDeclaration>();

        for (Element element : metadata.getElements()) {

            /*
             * Ignore not APAM elements 
             */
            if (!CoreMetadataParser.isApamDefinition(element))
                continue;

            /*
             * switch depending on component type
             */
            if (CoreMetadataParser.isSpecification(element))
                specifications.add(parseSpecification(element));

            if (CoreMetadataParser.isPrimitiveImplementation(element))
                primitives.add(parsePrimitive(element));

            if (CoreMetadataParser.isCompositeImplementation(element))
                composites.add(parseComposite(element));

            if (CoreMetadataParser.isInstance(element))
                instances.add(parseInstance(element));

        }

        /*
         *  Add declarations in order of dependency to ease cross-reference validation irrespective of
         *  the declaration order 
         */

        declaredElements.addAll(specifications);
        declaredElements.addAll(primitives);
        declaredElements.addAll(composites);
        declaredElements.addAll(instances);

        // Release references once the parsed data is cached
        metadata = null;
        introspector = null;
        this.errorHandler = null;

        return declaredElements;
    }

    /**
     * parse the common attributes shared by all declarations
     */
    private void parseComponent(Element element, ComponentDeclaration component) {

        parseProvidedResources(element, component);
        parsePropertyDefinitions(element, component);
        parseProperties(element, component);
        parseDependencies(element, component);

        boolean isInstantiable = parseBoolean(element, CoreMetadataParser.ATT_INSTANTIABLE, false, true);
        boolean isExclusive = parseBoolean(element, CoreMetadataParser.ATT_EXCLUSIVE, false, false);
        boolean isSingleton = parseBoolean(element, CoreMetadataParser.ATT_SINGLETON, false, false);
        boolean isShared = parseBoolean(element, CoreMetadataParser.ATT_SHARED, false, true);

        component.setExclusive(isExclusive);
        component.setInstantiable(isInstantiable);
        component.setSingleton(isSingleton);
        component.setShared(isShared);

        boolean isDefinedInstantiable = parseisDefinedBoolean(element, CoreMetadataParser.ATT_INSTANTIABLE);
        boolean isDefinedExclusive = parseisDefinedBoolean(element, CoreMetadataParser.ATT_EXCLUSIVE);
        boolean isDefinedSingleton = parseisDefinedBoolean(element, CoreMetadataParser.ATT_SINGLETON);
        boolean isDefinedShared = parseisDefinedBoolean(element, CoreMetadataParser.ATT_SHARED);

        component.setDefinedExclusive(isDefinedExclusive);
        component.setDefinedInstantiable(isDefinedInstantiable);
        component.setDefinedSingleton(isDefinedSingleton);
        component.setDefinedShared(isDefinedShared);

        // Exclusive means: shared=false and singleton=true.
        if (isDefinedExclusive && isExclusive) {
            if (isDefinedShared && isShared) {
                errorHandler.error(Severity.ERROR, "A component cannot be both exclusive and shared or not singleton");
            }
            if (isDefinedSingleton && isSingleton) {
                errorHandler.error(Severity.ERROR, "A component cannot be both exclusive and not singleton");
            }
            component.setSingleton(true);
            component.setShared(false);
            component.setDefinedSingleton(true);
            component.setDefinedShared(true);
        }
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
     * Parse an atomic implementation declaration
     */
    private AtomicImplementationDeclaration parsePrimitive(Element element) {

        String name = parseName(element);
        SpecificationReference specification = parseSpecificationReference(element,
                CoreMetadataParser.ATT_SPECIFICATION, false);

        String className = parseString(element, CoreMetadataParser.ATT_CLASSNAME, true);

        /*
         * load Pojo instrumentation metadata
         */
        PojoMetadata pojoMetadata = null;
        Class<?> instrumentedCode = null;
        try {
            pojoMetadata = new PojoMetadata(element);
            instrumentedCode = ((className != CoreParser.UNDEFINED) && (introspector != null)) ? introspector.getInstrumentedClass(className) : null;
        } catch (ClassNotFoundException e) {
            errorHandler.error(Severity.ERROR, "Apam component " + name + ": " + "the component class " + className
                    + " can not be loaded");
        } catch (Exception ignoredException) {
        }

        Instrumentation instrumentation = new ApamIpojoInstrumentation(className, pojoMetadata, instrumentedCode);

        AtomicImplementationDeclaration declaration = new AtomicImplementationDeclaration(name, specification,
                instrumentation);
        parseComponent(element, declaration);

        /*
         *  Parse message producer method injection
         */
        String messageMethods = parseString(element, CoreMetadataParser.ATT_PUSH, false);
        for (String messageMethod : Util.split(messageMethods)) {

            /*
             * TODO Verify that the type of the provided message can be assigned to method
             */
            declaration.getProducerInjections().add(new MessageProducerMethodInterception(declaration, messageMethod));
        }

        /*
         *  Verify that at least one field is injected for each declared produced message.
         */
        for (MessageReference message : declaration.getProvidedResources(MessageReference.class)) {

            boolean declared = declaration.getProducerInjections().size() > 0;
            boolean injected = false;
            boolean defined = false;

            for (MessageProducerMethodInterception messageMethod : declaration.getProducerInjections()) {
                if (messageMethod.getResource() instanceof UndefinedReference)
                    continue;

                defined = true;

                if (!messageMethod.getResource().equals(message))
                    continue;

                injected = true;
                break;
            }

            /*
             * If we could determine the field types and there was no injection then signal error
             * 
             * NOTE Notice that some errors will not be detected at build time since all the reflection
             * information is not available, and validation must be delayed until run time
             */
            if (!declared || (defined && !injected))
                errorHandler.error(Severity.ERROR, "Apam component " + name + ": " + "produced message "
                        + message.getJavaType() + " is not injected in any field");

        }

        /*
         *  if not explicitly provided, get all the implemented interfaces.
         */
        if (declaration.getProvidedResources().isEmpty() && (pojoMetadata != null)) {
            for (String implementedInterface : pojoMetadata.getInterfaces()) {
                if (!implementedInterface.startsWith("java.lang"))
                    declaration.getProvidedResources().add(new InterfaceReference(implementedInterface));
            }
        }

        /*
         * If not explicitly provided, get all produced messages from the declared injected fields
         */
        Set<MessageReference> declaredMessages = declaration.getProvidedResources(MessageReference.class);
        for (MessageProducerMethodInterception messageMethod : declaration.getProducerInjections()) {
            ResourceReference resourceRef = messageMethod.getResource();
            if (resourceRef instanceof UndefinedReference){
                if (!((UndefinedReference)resourceRef).getKind().isAssignableFrom(MessageReference.class))
                    continue;
            }
                

            if (declaredMessages.contains(messageMethod.getResource()))
                continue;

            declaration.getProvidedResources().add(messageMethod.getResource());
        }

        /*
         *  If instrumented code is provided verify that all provided resources reference accessible classes
         */
        if (introspector != null) {
            for (ResourceReference providedResource : declaration.getProvidedResources()) {
                try {
                    introspector.getInstrumentedClass(providedResource.getJavaType());
                } catch (ClassNotFoundException e) {
                    errorHandler.error(Severity.ERROR, "Apam component " + name + ": " + "the provided resource "
                            + providedResource.getJavaType() + " can not be loaded");
                }
            }
        }

        /*
         * Iterate over all sub elements looking for callback declarations
         */
        for (Element callback : optional(element.getElements(CoreMetadataParser.CALLBACK, CoreMetadataParser.APAM))) {
            String onInit = parseString(callback, CoreMetadataParser.ATT_ON_INIT, false);
            String onRemove = parseString(callback, CoreMetadataParser.ATT_ON_REMOVE, false);

            if (onInit != null) {
                declaration.addCallbacks(parseCallback(declaration, CallbackTrigger.onInit, onInit));
            }

            if (onRemove != null) {
                declaration.addCallbacks(parseCallback(declaration, CallbackTrigger.onRemove, onRemove));
            }

        }

        return declaration;

    }

    /**
     * Parse a composite declaration
     */
    private CompositeDeclaration parseComposite(Element element) {

        String name = parseName(element);
        SpecificationReference specification = parseSpecificationReference(element,
                CoreMetadataParser.ATT_SPECIFICATION, false);
        ComponentReference<?> implementation = parseAnyComponentReference(element,
                CoreMetadataParser.ATT_MAIN_IMPLEMENTATION, true);

        CompositeDeclaration declaration = new CompositeDeclaration(name, specification, implementation);
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
        Element contents[] = optional(element.getElements(CoreMetadataParser.CONTENT, CoreMetadataParser.APAM));

        if (contents.length > 1)
            errorHandler.error(Severity.ERROR, "A single content management is allowed in a composite declaration"
                    + element);

        if (contents.length == 0)
            return;

        Element content = contents[0];

        parseOwnedInstances(content, declaration);
        parseState(content, declaration);
        parseVisibility(content, declaration);
        parsePromotions(content, declaration);
        parseOwns(content, declaration);
        parseContextualDependencies(content, declaration);

    }

    /**
     * Parse an instance declaration
     */
    private InstanceDeclaration parseInstance(Element element) {

        String name = parseName(element);
        ImplementationReference<?> implementation = parseImplementationReference(element,
                CoreMetadataParser.ATT_IMPLEMENTATION, true);

        /*
         * look for optional trigger declarations
         */

        Element triggers[] = optional(element.getElements(CoreMetadataParser.TRIGGER, CoreMetadataParser.APAM));
        Element trigger = triggers.length > 0 ? triggers[0] : null;

        if (triggers.length > 1)
            errorHandler.error(Severity.ERROR, "A single trigger declaration is allowed in an instance declaration"
                    + element);

        /*
         * Parse triggering conditions
         */
        Set<ConstrainedReference> triggerDeclarations = new HashSet<ConstrainedReference>();
        for (Element triggerCondition : optional(trigger != null ? trigger.getElements() : null)) {

            /*
             * ignore elements that are not from APAM
             */
            if (!CoreMetadataParser.isApamDefinition(triggerCondition))
                continue;

            ConstrainedReference triggerDeclaration = new ConstrainedReference(parseResolvableReference(
                    triggerCondition, CoreMetadataParser.ATT_NAME, true));

            /*
             * parse optional constraints
             */
            for (Element constraints : optional(triggerCondition.getElements(CoreMetadataParser.CONSTRAINTS,
                    CoreMetadataParser.APAM))) {
                parseConstraints(constraints, triggerDeclaration);
            }

            triggerDeclarations.add(triggerDeclaration);

        }

        InstanceDeclaration declaration = new InstanceDeclaration(implementation, name, triggerDeclarations);
        parseComponent(element, declaration);
        return declaration;
    }

    /**
     * parse the provided resources of a component
     */
    private void parseProvidedResources(Element element, ComponentDeclaration component) {

        String interfaces = parseString(element, CoreMetadataParser.ATT_INTERFACES, false);
        String messages = parseString(element, CoreMetadataParser.ATT_MESSAGES, false);

        for (String interfaceName : Util.split(interfaces)) {
            component.getProvidedResources().add(new InterfaceReference(interfaceName));
        }

        for (String message : Util.split(messages)) {
            component.getProvidedResources().add(new MessageReference(message));
        }

    }

    /**
     * parse callback declaration
     */

    private CallbackMethod parseCallback(AtomicImplementationDeclaration implementation, CallbackTrigger trigger,
            String methodName) {
        CallbackMethod callback = new CallbackMethod(implementation, trigger, methodName);
        if (!callback.isValidInstrumentation())
            errorHandler.error(Severity.ERROR, "the specified method \"" + methodName + "\" in \"" + trigger
                    + "\" is invalid or not founded");
        return callback;

    }

    /**
     * parse the required resources of a component
     */
    private void parseDependencies(Element element, ComponentDeclaration component) {

        /*
         *	Skip the optional enclosing list 
         */
        for (Element dependencies : optional(element.getElements(CoreMetadataParser.DEPENDENCIES,
                CoreMetadataParser.APAM))) {
            parseDependencies(dependencies, component);
        }

        /*
         * Iterate over all sub elements looking for dependency declarations
         */
        for (Element dependency : optional(element.getElements(CoreMetadataParser.DEPENDENCY, CoreMetadataParser.APAM))) {
            /*
             * Add to component declaration
             */
            component.getDependencies().add(parseDependency(dependency, component));
        }

    }

    /**
     * parse the required resources of a component
     */
    private void parseContextualDependencies(Element element, CompositeDeclaration component) {

        /*
         *	Skip the optional enclosing list 
         */
        for (Element dependencies : optional(element.getElements(CoreMetadataParser.DEPENDENCIES,
                CoreMetadataParser.APAM))) {
            parseDependencies(dependencies, component);
        }

        /*
         * Iterate over all sub elements looking for dependency declarations
         */
        for (Element dependency : optional(element.getElements(CoreMetadataParser.DEPENDENCY, CoreMetadataParser.APAM))) {
            /*
             * Add to component declaration
             */
            component.getContextualDependencies().add(parseDependency(dependency, component));
        }

    }

    /**
     * parse a dependency declaration
     */
    private DependencyDeclaration parseDependency(Element element, ComponentDeclaration component) {

        /*
         * Get the reference to the target of the dependency if specified
         */
        ResolvableReference target = parseResolvableReference(element, false);

        /*
         * All dependencies have an optional identifier and multiplicity specification
         */
        String id = parseString(element, CoreMetadataParser.ATT_ID, false);
        boolean isMultiple = parseBoolean(element, CoreMetadataParser.ATT_MULTIPLE, false, true);

        DependencyDeclaration dependency = null;

        /*
         * Component dependencies reference a single mandatory component (specification, implementation, instance),
         * and in the case of atomic components they may optionally have a number of nested injection declarations
         */
        if (target != null && target instanceof ComponentReference<?>) {

            dependency = new DependencyDeclaration(component.getReference(), id, isMultiple, target);

            if (component instanceof AtomicImplementationDeclaration) {

                AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
                for (Element injection : optional(element.getElements())) {

                    /*
                     * ignore elements that are not from APAM
                     */
                    if (!CoreMetadataParser.isApamDefinition(injection))
                        continue;

                    /*
                     * Accept only resource references
                     */
                    String resourceKind = injection.getName();
                    if (!(CoreMetadataParser.INTERFACE.equals(resourceKind) || CoreMetadataParser.MESSAGE
                            .equals(resourceKind)))
                        continue;

                    DependencyInjection dependencyInjection = parseDependencyInjection(injection, atomic, true);
                    dependencyInjection.setDependency(dependency);

                }

                /*
                 * Optionally, as a shortcut, a single injection may be specified directly as an attribute of the dependency
                 */
                DependencyInjection dependencyInjection = parseDependencyInjection(element, atomic, false);
                if (dependencyInjection != null) {
                    dependencyInjection.setDependency(dependency);
                }

                /*
                 * At least one injection must be specified in atomic components
                 */
                if (dependency.getInjections().isEmpty()) {
                    errorHandler.error(Severity.ERROR,
                            "A field must be defined for dependencies in primitive implementation "
                                    + component.getName());
                }

            }
        }

        /*
         * Simple dependencies reference a single resource, an for atomic components a single injection must be
         * specified directly as an attribute of the dependency
         */
        if (target != null && target instanceof ResourceReference) {

            dependency = new DependencyDeclaration(component.getReference(), id, isMultiple, target);

            if (component instanceof AtomicImplementationDeclaration) {

                AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
                DependencyInjection dependencyInjection = parseDependencyInjection(element, atomic, true);

                /*
                 * Both the explicit target and the specified injection must match 
                 */
                if (!target.equals(dependencyInjection.getResource())) {
                    errorHandler.error(Severity.ERROR,
                            "dependency target doesn't match the type of the field or method in " + element);
                }

                dependencyInjection.setDependency(dependency);

            }

        }

        /*
         * If no target was explicitly specified, but an injection was specified for an atomic component, we can infer
         * the target from the injection  
         */
        if (target == null && component instanceof AtomicImplementationDeclaration) {

            AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
            DependencyInjection dependencyInjection = parseDependencyInjection(element, atomic, true);

            target = dependencyInjection.getResource();
            id = (id != null) ? id : dependencyInjection.getName();
            dependency = new DependencyDeclaration(component.getReference(), id, isMultiple, target);

            dependencyInjection.setDependency(dependency);

        }

        /*
         * If no target was specified signal the error
         */
        if (target == null && !(component instanceof AtomicImplementationDeclaration)) {
            errorHandler.error(Severity.ERROR, "dependency target must be specified " + element);
            target = new ComponentReference<ComponentDeclaration>(CoreMetadataParser.UNDEFINED);
            dependency = new DependencyDeclaration(component.getReference(), id, isMultiple, target);
        }

        /*
         * Get the optional missing policy
         */
        MissingPolicy policy = parsePolicy(element, CoreMetadataParser.ATT_FAIL, false, MissingPolicy.OPTIONAL);
        dependency.setMissingPolicy(policy);

        /*
         * Get the optional missing exception specification
         */
        String missingException = parseString(element, CoreMetadataParser.ATT_EXCEPTION, false);
        if (policy.equals(MissingPolicy.EXCEPTION) && missingException != null) {
            dependency.setMissingException(missingException);
        }

        String isEager = parseString(element, CoreMetadataParser.ATT_EAGER, false);
        String mustHide = parseString(element, CoreMetadataParser.ATT_HIDE, false);

        if (isEager != null) {
            dependency.setEager(Boolean.parseBoolean(isEager));
        }

        if (mustHide != null) {
            dependency.setHide(Boolean.parseBoolean(mustHide));
        }

        /*
         * Get the optional constraints and preferences
         */
        for (Element constraints : optional(element
                .getElements(CoreMetadataParser.CONSTRAINTS, CoreMetadataParser.APAM))) {
            parseConstraints(constraints, dependency);
        }

        for (Element preferences : optional(element
                .getElements(CoreMetadataParser.PREFERENCES, CoreMetadataParser.APAM))) {
            parsePreferences(preferences, dependency);
        }

        return dependency;
    }

    /**
     * parse the injected dependencies of a primitive
     */
    private DependencyInjection parseDependencyInjection(Element element, AtomicImplementationDeclaration atomic,
            boolean mandatory) {

        String field = parseString(element, CoreMetadataParser.ATT_FIELD, false);
//        String method = parseString(element, CoreMetadataParser.ATT_METHOD, false);

        String push = parseString(element, CoreMetadataParser.ATT_PUSH, false);
        String pull = parseString(element, CoreMetadataParser.ATT_PULL, false);

        String type = parseString(element, CoreMetadataParser.ATT_TYPE, false);

        if ((field == null) && (push == null) && (pull == null) && mandatory)
            errorHandler.error(Severity.ERROR, "attribute \"" + CoreMetadataParser.ATT_FIELD + "\" or \""
                    + CoreMetadataParser.ATT_PUSH + "\" or \"" + CoreMetadataParser.ATT_PULL
                    + "\" must be specified in " + element.getName());

        if ((field == null) && CoreMetadataParser.INTERFACE.equals(element.getName()) && mandatory)
            errorHandler.error(Severity.ERROR, "attribute \""
                    + CoreMetadataParser.ATT_FIELD + "\" must be specified in " + element.getName());

        if ((field == null) && (push == null) && (pull == null)) {
            return mandatory ? new DependencyInjection.Field(atomic, CoreParser.UNDEFINED) : null;
        }
        DependencyInjection injection = null;

        if (field != null) { // The dependency is a field, interface dependency
            injection = new DependencyInjection.Field(atomic, field);
        } else if (push != null) { // the dependency is a method, push message dependency
            injection = new DependencyInjection.CallbackWithArgument(atomic, push, type);
        } else if (pull != null) {// the dependency is a method, pull message dependency
            injection = new DependencyInjection.Field(atomic, pull);
        }

        if (!injection.isValidInstrumentation())
            errorHandler.error(Severity.ERROR, "the specified \"" + CoreMetadataParser.ATT_FIELD + "\" or \""
                    + CoreMetadataParser.ATT_PUSH + "\" or \"" + CoreMetadataParser.ATT_PULL + "\" is invalid "
                    + element.getName());

        return injection;
    }

    /**
     * parse a constraints declaration
     */
    private void parseConstraints(Element element, ConstrainedReference reference) {

        for (Element constraint : optional(element.getElements())) {

            String filter = parseString(constraint, CoreMetadataParser.ATT_FILTER);

            if (constraint.getName().equals(CoreMetadataParser.IMPLEMENTATION) ||
                    constraint.getName().equals(CoreMetadataParser.CONSTRAINT))
                reference.getImplementationConstraints().add(filter);

            if (constraint.getName().equals(CoreMetadataParser.INSTANCE))
                reference.getInstanceConstraints().add(filter);

        }

    }

    /**
     * parse a preferences declaration
     */
    private void parsePreferences(Element element, ConstrainedReference reference) {

        for (Element preference : optional(element.getElements())) {

            String filter = parseString(preference, CoreMetadataParser.ATT_FILTER);

            if (preference.getName().equals(CoreMetadataParser.IMPLEMENTATION) ||
                    preference.getName().equals(CoreMetadataParser.CONSTRAINT))
                reference.getImplementationPreferences().add(filter);

            if (preference.getName().equals(CoreMetadataParser.INSTANCE))
                reference.getInstancePreferences().add(filter);

        }

    }

    /**
     * parse the property definitions of the component
     */
    private void parsePropertyDefinitions(Element element, ComponentDeclaration component) {

        if (component instanceof InstanceDeclaration)
            return;

        /*
         *	Skip the optional enclosing list 
         */
        for (Element definitions : optional(element
                .getElements(CoreMetadataParser.DEFINITIONS, CoreMetadataParser.APAM))) {
            parsePropertyDefinitions(definitions, component);
        }

        for (Element definition : optional(element.getElements(CoreMetadataParser.DEFINITION, CoreMetadataParser.APAM))) {

            String name = parseString(definition, CoreMetadataParser.ATT_NAME);
            String type = parseString(definition, CoreMetadataParser.ATT_TYPE);
            String defaultValue = parseString(definition, CoreMetadataParser.ATT_VALUE, false);
            String field = parseString(definition, CoreMetadataParser.ATT_FIELD, false);
            String callback = parseString(definition, CoreMetadataParser.ATT_METHOD, false);
            boolean internal = parseBoolean(definition, CoreMetadataParser.ATT_INTERNAL, false, false);

            component.getPropertyDefinitions().add(
                    new PropertyDefinition(component, name, type, defaultValue, field, callback, internal));
        }
    }

    /**
     * parse the properties of the component
     */
    private void parseProperties(Element element, ComponentDeclaration component) {

        /*
         *	Skip the optional enclosing list 
         */
        for (Element properties : optional(element.getElements(CoreMetadataParser.PROPERTIES, CoreMetadataParser.APAM))) {
            parseProperties(properties, component);
        }

        for (Element property : optional(element.getElements(CoreMetadataParser.PROPERTY, CoreMetadataParser.APAM))) {

            /*
             * If a name is specified, get the associated value
             */
            String name = parseString(property, ATT_NAME);
            String value = parseString(property, ATT_VALUE);
            component.getProperties().put(name, value);

            /**
             * Special case for specification. The type is in the property, we generate a definition for it.
             */
            // if (component instanceof SpecificationDeclaration && !Util.isPredefinedAttribute(name)) {
            if (component instanceof SpecificationDeclaration) {
                String type = parseString(property, ATT_TYPE);
                component.getPropertyDefinitions().add(
                        new PropertyDefinition(component, name, type, value, null, null, false));
            }
        }
    }

    /**
     * Parse the definition of the state of a composite
     */
    private void parseState(Element element, CompositeDeclaration composite) {
        /*
         * Look for content management specification
         */
        Element states[] = optional(element.getElements(CoreMetadataParser.STATE, CoreMetadataParser.APAM));

        if (states.length > 1)
            errorHandler.error(Severity.ERROR, "A single state declaration is allowed in a composite declaration"
                    + element);

        if (states.length == 0)
            return;

        Element state = states[0];

        composite.setStateProperty(parsePropertyReference(state, true));
    }

    /**
     * Parse the visibility rules for the content of a composite
     */
    private void parseVisibility(Element element, CompositeDeclaration composite) {

        for (Element rule : optional(element.getElements())) {

            String implementationsRule = parseString(rule, CoreMetadataParser.ATT_IMPLEMENTATION, false);
            String instancesRule = parseString(rule, CoreMetadataParser.ATT_INSTANCE, false);

            if (rule.getName().equals(CoreMetadataParser.BORROW)) {
                if (implementationsRule != null) {
                    composite.getVisibility().setBorrowImplementations(implementationsRule);
                }
                if (instancesRule != null) {
                    composite.getVisibility().setBorrowInstances(instancesRule);
                }
            }

            if (rule.getName().equals(CoreMetadataParser.FRIEND)) {
                if (implementationsRule != null) {
                    composite.getVisibility().setFriendImplementations(implementationsRule);
                }
                if (instancesRule != null) {
                    composite.getVisibility().setFriendInstances(instancesRule);
                }
            }

            if (rule.getName().equals(CoreMetadataParser.LOCAL)) {
                if (implementationsRule != null) {
                    composite.getVisibility().setLocalImplementations(implementationsRule);
                }

                if (instancesRule != null) {
                    composite.getVisibility().setLocalInstances(instancesRule);
                }
            }

            if (rule.getName().equals(CoreMetadataParser.APPLICATION)) {

                if (instancesRule != null) {
                    composite.getVisibility().setApplicationInstances(instancesRule);
                }
            }

        }

    }

    /**
     * Parse the list of promoted dependencies of a composite
     */
    private void parsePromotions(Element element, CompositeDeclaration composite) {
        for (Element promotion : optional(element.getElements(CoreMetadataParser.PROMOTE, CoreMetadataParser.APAM))) {

            DependencyDeclaration.Reference source = parseDependencyReference(promotion, true);
            String target = parseString(promotion, CoreMetadataParser.ATT_TO);

            composite.getPromotions().add(
                    new DependencyPromotion(source, new DependencyDeclaration.Reference(composite.getReference(),
                            target)));
        }
    }

    /**
     * Parse the list of owned components of a composite
     */
    private void parseOwns(Element element, CompositeDeclaration composite) {
        for (Element owned : optional(element.getElements(CoreMetadataParser.OWN, CoreMetadataParser.APAM))) {

            PropertyDefinition.Reference property = parsePropertyReference(owned, true);
            String values = parseString(owned, CoreMetadataParser.ATT_VALUE);

            OwnedComponentDeclaration ownedComponent = new OwnedComponentDeclaration(property, new HashSet<String>(
                    Arrays.asList(Util.split(values))));

            /*
             * parse optional grants
             */
            for (Element grant : optional(owned.getElements(CoreMetadataParser.GRANT, CoreMetadataParser.APAM))) {

                ComponentReference<?> definingComponent = parseComponentReference(grant, true);
                String identifier = parseString(grant, CoreMetadataParser.ATT_DEPENDENCY, false);
                identifier = identifier != null ? identifier : ownedComponent.getComponent().getName();
                DependencyDeclaration.Reference dependency = new DependencyDeclaration.Reference(definingComponent,
                        identifier);

                String states = parseString(grant, CoreMetadataParser.ATT_WHEN, true);

                GrantDeclaration grantDeclaration = new GrantDeclaration(dependency, new HashSet<String>(Arrays
                        .asList(Util.split(states))));
                ownedComponent.getGrants().add(grantDeclaration);
            }

            composite.getOwnedComponents().add(ownedComponent);
        }
    }

    /**
     * Parse the list of owned instances of a composite
     */
    private void parseOwnedInstances(Element element, CompositeDeclaration composite) {

        for (Element start : optional(element.getElements(CoreMetadataParser.START, CoreMetadataParser.APAM))) {
            composite.getInstanceDeclarations().add(parseInstance(start));
        }

    }

    /**
     * Tests whether the specified element is an Apam declaration
     */
    private static final boolean isApamDefinition(Element element) {
        return (element.getNameSpace() != null) && CoreMetadataParser.APAM.equals(element.getNameSpace());

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
     * Determines if this element represents an instance declaration
     */
    private static final boolean isInstance(Element element) {
        return CoreMetadataParser.INSTANCE.equals(element.getName())
                || CoreMetadataParser.INSTANCE_ALT.equals(element.getName());
    }

    /**
     * Get a string attribute value
     */
    private final String parseString(Element element, String attribute, boolean mandatory) {
        String value = element.getAttribute(attribute);

        if (mandatory && (value == null)) {
            errorHandler.error(Severity.ERROR, "attribute \"" + attribute + "\" must be specified in "
                    + element.getName());
            value = CoreParser.UNDEFINED;
        }

        if (mandatory && (value != null) && value.trim().isEmpty()) {
            errorHandler.error(Severity.ERROR, "attribute \"" + attribute + "\" cannot be empty in "
                    + element.getName());
            value = CoreParser.UNDEFINED;
        }

        return value;
    }

    /**
     * Get a mandatory string attribute value
     */
    private final String parseString(Element element, String attribute) {
        return parseString(element, attribute, true);
    }

    /**
     * Get a mandatory element name
     */
    private final String parseName(Element element) {
        return parseString(element, CoreMetadataParser.ATT_NAME);
    }

    /**
     * Get a boolean attribute value
     */
    private boolean parseBoolean(Element element, String attribute, boolean mandatory, boolean defaultValue) {
        String valueString = parseString(element, attribute, mandatory);
        return ((valueString == null) && !mandatory) ? defaultValue : Boolean.parseBoolean(valueString);
    }

    /**
     * Get a boolean attribute value
     */
    private boolean parseisDefinedBoolean(Element element, String attribute) {
        return (parseString(element, attribute, false) != null);
    }

    /**
     * The list of allowed values for specifying the missing policy
     */
    private static final List<String> MISSING_VALUES = Arrays.asList(CoreMetadataParser.VALUE_WAIT,
                                                             CoreMetadataParser.VALUE_EXCEPTION,
                                                             CoreMetadataParser.VALUE_OPTIONAL);

    /**
     * Get a missing policy attribute value
     */
    private MissingPolicy parsePolicy(Element element, String attribute, boolean mandatory, MissingPolicy defaultValue) {

        String encodedPolicy = parseString(element, attribute, mandatory);

        if ((encodedPolicy == null) && !mandatory)
            return defaultValue;

        if ((encodedPolicy == null) && mandatory)
            return null;

        if (CoreMetadataParser.VALUE_WAIT.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.WAIT;

        if (CoreMetadataParser.VALUE_OPTIONAL.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.OPTIONAL;

        if (CoreMetadataParser.VALUE_EXCEPTION.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.EXCEPTION;

        errorHandler.error(Severity.ERROR, "invalid value for missing policy : \"" + encodedPolicy
                + "\",  accepted values are " + CoreMetadataParser.MISSING_VALUES.toString());
        return null;
    }

    /**
     * Get an specification reference coded in an attribute
     */
    private SpecificationReference parseSpecificationReference(Element element, String attribute, boolean mandatory) {
        String specification = parseString(element, attribute, mandatory);
        return ((specification == null) && !mandatory) ? null : new SpecificationReference(specification);
    }

    /**
     * Get an implementation reference coded in an attribute
     */
    private ImplementationReference<?>
            parseImplementationReference(Element element, String attribute, boolean mandatory) {
        String implementation = parseString(element, attribute, mandatory);
        return ((implementation == null) && !mandatory) ? null
                : new ImplementationReference<ImplementationDeclaration>(implementation);
    }

    /**
     * Get an instance reference coded in an attribute
     */
    private InstanceReference parseInstanceReference(Element element, String attribute, boolean mandatory) {
        String instance = parseString(element, attribute, mandatory);
        return ((instance == null) && !mandatory) ? null : new InstanceReference(instance);
    }

    /**
     * Get a generic component reference coded in an attribute
     */
    private ComponentReference<?> parseAnyComponentReference(Element element, String attribute, boolean mandatory) {
        String component = parseString(element, attribute, mandatory);
        return ((component == null) && !mandatory) ? null : new ComponentReference<ComponentDeclaration>(component);
    }

    /**
     * Get an interface reference coded in an attribute
     */
    private InterfaceReference parseInterfaceReference(Element element, String attribute, boolean mandatory) {
        String interfaceName = parseString(element, attribute, mandatory);
        return ((interfaceName == null) && !mandatory) ? null : new InterfaceReference(interfaceName);
    }

    /**
     * Get a message reference coded in an attribute
     */
    private MessageReference parseMessageReference(Element element, String attribute, boolean mandatory) {
        String messageName = parseString(element, attribute, mandatory);
        return ((messageName == null) && !mandatory) ? null : new MessageReference(messageName);
    }

    /**
     * Get a component reference coded in an attribute
     */
    private ComponentReference<?> parseComponentReference(Element element, String attribute, boolean mandatory) {

        String referenceKind = getReferenceKind(element, attribute);

        if (CoreMetadataParser.SPECIFICATION.equals(referenceKind))
            return parseSpecificationReference(element, attribute, mandatory);

        if (CoreMetadataParser.IMPLEMENTATION.equals(referenceKind))
            return parseImplementationReference(element, attribute, mandatory);

        if (CoreMetadataParser.INSTANCE.equals(referenceKind))
            return parseInstanceReference(element, attribute, mandatory);

        if (CoreMetadataParser.COMPONENT.equals(referenceKind))
            return parseAnyComponentReference(element, attribute, mandatory);

        if (mandatory) {
            errorHandler.error(Severity.ERROR, "component name must be specified in " + element);
            return new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED);
        }

        return null;

    }

    /**
     * The list of possible kinds of component references
     */
    private final static List<String> COMPONENT_REFERENCES = Arrays.asList(CoreMetadataParser.SPECIFICATION,
                                                                   CoreMetadataParser.IMPLEMENTATION,
                                                                   CoreMetadataParser.INSTANCE,
                                                                   CoreMetadataParser.COMPONENT);

    /**
     * Get a component reference implicitly coded in the element (either in a name attribute or an attribute named
     * after the kind of reference)
     */
    private ComponentReference<?> parseComponentReference(Element element, boolean mandatory) {

        String attribute = CoreMetadataParser.UNDEFINED;

        /*
         * If the kind of reference is coded in the element name, the actual value must be coded in
         * the attribute NAME
         */
        if (CoreMetadataParser.COMPONENT_REFERENCES.contains(element.getName()))
            attribute = CoreMetadataParser.ATT_NAME;

        /*
         * Otherwise try to find a defined attribute matching the kind of reference
         */
        for (Attribute definedAttribute : element.getAttributes()) {

            if (!CoreMetadataParser.COMPONENT_REFERENCES.contains(definedAttribute.getName()))
                continue;

            attribute = definedAttribute.getName();
            break;
        }

        if (attribute.equals(CoreMetadataParser.UNDEFINED) && mandatory) {
            errorHandler.error(Severity.ERROR, "component name must be specified in " + element.getName());
            return new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED);
        }

        if (attribute.equals(CoreMetadataParser.UNDEFINED) && !mandatory)
            return null;

        return parseComponentReference(element, attribute, mandatory);
    }

    /**
     * Get a dependency declaration reference coded in the element
     */
    private DependencyDeclaration.Reference parseDependencyReference(Element element, boolean mandatory) {

        ComponentReference<?> definingComponent = parseComponentReference(element, mandatory);
        String identifier = parseString(element, CoreMetadataParser.ATT_DEPENDENCY, mandatory);

        if (!mandatory && (definingComponent == null || identifier == null)) {
            return null;
        }

        return new DependencyDeclaration.Reference(definingComponent, identifier);
    }

    /**
     * Get a property declaration reference coded in the element
     */
    private PropertyDefinition.Reference parsePropertyReference(Element element, boolean mandatory) {

        ComponentReference<?> definingComponent = parseComponentReference(element, mandatory);
        String identifier = parseString(element, CoreMetadataParser.ATT_PROPERTY, mandatory);

        if (!mandatory && (definingComponent == null || identifier == null)) {
            return null;
        }

        return new PropertyDefinition.Reference(definingComponent, identifier);
    }

    /**
     * Get a resource reference coded in an attribute
     */
    private ResourceReference parseResourceReference(Element element, String attribute, boolean mandatory) {

        String referenceKind = getReferenceKind(element, attribute);

        if (CoreMetadataParser.INTERFACE.equals(referenceKind))
            return parseInterfaceReference(element, attribute, mandatory);

        if (CoreMetadataParser.MESSAGE.equals(referenceKind))
            return parseMessageReference(element, attribute, mandatory);

        if (mandatory) {
            errorHandler.error(Severity.ERROR, "resource name must be specified in " + element.getName());
            return new UndefinedReference(element.getName(),ResourceReference.class);
        }

        return null;
    }

    /**
     * The list of possible kinds of component references
     */
    private final static List<String> RESOURCE_REFERENCES = Arrays.asList(CoreMetadataParser.INTERFACE,
                                                                  CoreMetadataParser.MESSAGE);

    /**
     * Get a resolvable reference coded in an attribute
     */
    private ResolvableReference parseResolvableReference(Element element, String attribute, boolean mandatory) {

        String referenceKind = getReferenceKind(element, attribute);

        if (CoreMetadataParser.COMPONENT_REFERENCES.contains(referenceKind))
            return parseComponentReference(element, attribute, mandatory);

        if (CoreMetadataParser.RESOURCE_REFERENCES.contains(referenceKind))
            return parseResourceReference(element, attribute, mandatory);

        if (mandatory) {
            errorHandler.error(Severity.ERROR, "component name or resource must be specified in " + element.getName());
            return new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED);
        }

        return null;
    }

    /**
     * The list of possible kinds of references
     */
    private final static List<String> ALL_REFERENCES = Arrays.asList(CoreMetadataParser.SPECIFICATION,
                                                             CoreMetadataParser.IMPLEMENTATION,
                                                             CoreMetadataParser.INSTANCE,
                                                             CoreMetadataParser.COMPONENT,
                                                             CoreMetadataParser.INTERFACE,
                                                             CoreMetadataParser.MESSAGE);

    /**
     * Get a resolvable reference implicitly coded in the element (either in a name attribute or an attribute named
     * after the kind of reference)
     */
    private ResolvableReference parseResolvableReference(Element element, boolean mandatory) {

        String attribute = CoreMetadataParser.UNDEFINED;

        /*
         * If the kind of reference is coded in the element name, the actual value must be coded in
         * the attribute NAME
         */
        if (CoreMetadataParser.ALL_REFERENCES.contains(element.getName()))
            attribute = CoreMetadataParser.ATT_NAME;

        /*
         * Otherwise try to find a defined attribute matching the kind of reference
         */
        for (Attribute definedAttribute : element.getAttributes()) {

            if (!CoreMetadataParser.ALL_REFERENCES.contains(definedAttribute.getName()))
                continue;

            attribute = definedAttribute.getName();
            break;
        }

        if (attribute.equals(CoreMetadataParser.UNDEFINED) && mandatory) {
            errorHandler.error(Severity.ERROR, "component name or resource must be specified in " + element.getName());
            return new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED);
        }

        if (attribute.equals(CoreMetadataParser.UNDEFINED) && !mandatory)
            return null;

        return parseResolvableReference(element, attribute, mandatory);
    }

    /**
     * Infer the kind of a reference from the specified element tag and attribute name
     */
    private final String getReferenceKind(Element element, String attribute) {

        if (CoreMetadataParser.ALL_REFERENCES.contains(attribute))
            return attribute;

        if (CoreMetadataParser.ALL_REFERENCES.contains(element.getName()))
            return element.getName();

        return CoreParser.UNDEFINED;
    }

    /**
     * Handle transparently optional elements in the metadata
     */
    private final static Element[] EMPTY_ELEMENTS = new Element[0];

    private Element[] optional(Element[] elements) {
        if (elements == null)
            return CoreMetadataParser.EMPTY_ELEMENTS;
        return elements;

    }

    /**
     * A utility class to obtain information about declared fields and methods, from available instrumented code
     * or iPojo metadata
     * 
     * @author vega
     * 
     */
    private static class ApamIpojoInstrumentation implements Instrumentation {

        /**
         * The iPojo generated metadata
         */
        private final PojoMetadata pojoMetadata;

        /**
         * The name of the instrumented class
         */
        private final String       className;

        /**
         * The optional reflection information
         */
        private final Class<?>     instrumentedCode;

        public ApamIpojoInstrumentation(String className, PojoMetadata pojoMetadata, Class<?> instrumentedCode) {
            this.className = className;
            this.pojoMetadata = pojoMetadata;
            this.instrumentedCode = instrumentedCode;
        }

        @Override
        public String getClassName() {
            return className;
        }

        /**
         * The list of supported collections for aggregate dependencies
         */
        private final static Class<?>[] supportedCollections = new Class<?>[] {
                                                                     Collection.class,
                                                                     List.class,
                                                                     Vector.class,
                                                                     Set.class };

        /**
         * The list of supported messages for aggregate dependencies
         */
        private final static Class<?>[] supportedMessages    = new Class<?>[] {
                                                                     Queue.class,
                                                             };

        /**
         * If the type of the specified field is one of the supported collections returns the type of the
         * elements in the collection, otherwise return null.
         * 
         * May return {@link CoreParser#UNDEFINED} if the type of the elements in the collection
         * cannot be determined.
         */
        private static String getCollectionType(Field field) {

            Type fieldType = field.getGenericType();

            /*
             * First try to see if the field is an array declaration
             */
            if (fieldType instanceof Class) {
                Class<?> fieldClass = (Class<?>) fieldType;
                Class<?> elementType = fieldClass.getComponentType();
                if (fieldClass.isArray())
                    return elementType.getCanonicalName();
            }

            if (fieldType instanceof GenericArrayType) {
                GenericArrayType fieldClass = (GenericArrayType) fieldType;
                Type elementType = fieldClass.getGenericComponentType();
                if (elementType instanceof Class)
                    ((Class<?>) elementType).getCanonicalName();
                else
                    return CoreParser.UNDEFINED;
            }

            /*
             * Next try to see if the raw class of the field is one of the supported collections
             */
            Class<?> fieldClass = null;
            if (fieldType instanceof Class)
                fieldClass = (Class<?>) fieldType;
            if (fieldType instanceof ParameterizedType) {
                fieldClass = (Class<?>) ((ParameterizedType) fieldType).getRawType();
            }

            /*
             * If we could not determine the actual class of the field just return null
             */
            if (fieldClass == null)
                return null;

            /*
             * Verify if the class of the field is one of the supported collections
             */
            for (Class<?> supportedCollection : ApamIpojoInstrumentation.supportedCollections) {
                if (supportedCollection.equals(fieldClass)) {

                    /* Try to get the underlying element type if possible, otherwise
                     * return UNDEFINED
                     */
                    if (fieldType instanceof ParameterizedType) {
                        Type[] parameters = ((ParameterizedType) fieldType).getActualTypeArguments();
                        if ((parameters.length == 1) && (parameters[0] instanceof Class))
                            return ((Class<?>) parameters[0]).getCanonicalName();
                        else
                            return CoreParser.UNDEFINED;
                    }

                    return CoreParser.UNDEFINED;
                }
            }

            /*
             * If it is not an array or one of the supported collections just return null
             */
            return null;

        }

        /**
         * If the type of the specified field is one of the supported collections returns the type of the
         * elements in the collection, otherwise return null.
         * 
         * May return {@link CoreParser#UNDEFINED} if the type of the elements in the collection
         * cannot be determined.
         */
        private static String getCollectionType(FieldMetadata field) {
            String fieldType = field.getFieldType();

            if (fieldType.endsWith("[]")) {
                int index = fieldType.indexOf('[');
                return fieldType.substring(0, index);
            }

            for (Class<?> supportedCollection : ApamIpojoInstrumentation.supportedCollections) {
                if (supportedCollection.getCanonicalName().equals(fieldType)) {
                    return CoreParser.UNDEFINED;
                }
            }

            return null;
        }

        /**
         * If the type of the specified field is one of the supported message interfaces returns
         * the type of the message data, otherwise return null.
         * 
         * May return {@link CoreParser#UNDEFINED} if the type of the data in the message cannot
         * be determined.
         */
        private static String getMessageType(Field field) {

            Type fieldType = field.getGenericType();

            /*
             * Try to see if the raw class of the field is one of the supported message interfaces
             */
            Class<?> fieldClass = null;
            if (fieldType instanceof Class)
                fieldClass = (Class<?>) fieldType;
            if (fieldType instanceof ParameterizedType) {
                fieldClass = (Class<?>) ((ParameterizedType) fieldType).getRawType();
            }

            /*
             * If we could not determine the actual class of the field just return null
             */
            if (fieldClass == null)
                return null;

            /*
             * Verify if the class of the field is one of the supported messages
             */
            for (Class<?> supportedMessage : ApamIpojoInstrumentation.supportedMessages) {
                if (supportedMessage.equals(fieldClass)) {

                    /* Try to get the underlying data type if possible, otherwise
                     * return UNDEFINED
                     */
                    if (fieldType instanceof ParameterizedType) {
                        Type[] parameters = ((ParameterizedType) fieldType).getActualTypeArguments();
                        if ((parameters.length == 1) && (parameters[0] instanceof Class))
                            return ((Class<?>) parameters[0]).getCanonicalName();
                        else
                            return CoreParser.UNDEFINED;
                    }

                    return CoreParser.UNDEFINED;
                }
            }

            /*
             * If it is not one of the supported message types just return null
             */
            return null;

        }

        /**
         * If the type of the specified field is one of the supported message interfaces returns
         * the type of the message data, otherwise return null.
         * 
         * May return {@link CoreParser#UNDEFINED} if the type of the data in the message cannot
         * be determined.
         */
        private static String getMessageType(FieldMetadata field) {
            String fieldType = field.getFieldType();

            for (Class<?> supportedMessage : ApamIpojoInstrumentation.supportedMessages) {
                if (supportedMessage.getCanonicalName().equals(fieldType)) {
                    return CoreParser.UNDEFINED;
                }
            }

            return null;
        }

        /**
         * Get the type of reference from the instrumented metadata of the field
         */
        @Override
        public ResourceReference getFieldType(String fieldName) throws NoSuchFieldException {

            /*
             * Get iPojo metadata
             */
            FieldMetadata fieldIPojoMetadata = null;
            if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null))
                fieldIPojoMetadata = pojoMetadata.getField(fieldName);

            /*
             * Try to get reflection information if available,.
             */
            Field fieldReflectionMetadata = null;
            if (instrumentedCode != null) {
                try {
                    fieldReflectionMetadata = instrumentedCode.getDeclaredField(fieldName);
                } catch (Exception e) {
                }
            }

            /*
             * Try to use reflection information
             */
            if (fieldReflectionMetadata != null) {

                /*
                 * First verify if it is a collection
                 */
                String collectionType = ApamIpojoInstrumentation.getCollectionType(fieldReflectionMetadata);
                if (collectionType != null)
                    return collectionType != CoreParser.UNDEFINED ? new InterfaceReference(collectionType)
                            : new UndefinedReference(fieldName,InterfaceReference.class);

                /*
                 * Then verify if it is a message
                 */
                String messageType = ApamIpojoInstrumentation.getMessageType(fieldReflectionMetadata);
                if (messageType != null)
                    return messageType != CoreParser.UNDEFINED ? new MessageReference(messageType)
                            : new UndefinedReference(fieldName,MessageReference.class);;

                /*
                 * Otherwise it's a normal field we just return its type name
                 */
                return new InterfaceReference(fieldReflectionMetadata.getType().getCanonicalName());
            }

            /*
             * Try to use iPojo metadata
             */
            if (fieldIPojoMetadata != null) {

                /*
                 * First verify if it is a collection
                 */
                String collectionType = ApamIpojoInstrumentation.getCollectionType(fieldIPojoMetadata);
                if (collectionType != null)
                    return collectionType != CoreParser.UNDEFINED ? new InterfaceReference(collectionType)
                            : new UndefinedReference(fieldName,InterfaceReference.class);

                /*
                 * Then verify if it is a message
                 */
                String messageType = ApamIpojoInstrumentation.getMessageType(fieldIPojoMetadata);
                if (messageType != null)
                    return messageType != CoreParser.UNDEFINED ? new MessageReference(messageType)
                            : new UndefinedReference(fieldName,MessageReference.class);

                /*
                 * Otherwise it's a normal field we just return its type name
                 */
                return new InterfaceReference(fieldIPojoMetadata.getFieldType());
            }

            throw new NoSuchFieldException("unavailable field " + fieldName);

        }

        @Override
        public boolean isCollectionField(String fieldName) throws NoSuchFieldException {

            /*
             * Try to get reflection information if available,.
             */
            Field fieldReflectionMetadata = null;
            if (instrumentedCode != null) {
                try {
                    fieldReflectionMetadata = instrumentedCode.getDeclaredField(fieldName);
                } catch (Exception ignored) {
                }
            }

            /*
             * Get iPojo metadata
             */
            FieldMetadata fieldIPojoMetadata = null;
            if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null))
                fieldIPojoMetadata = pojoMetadata.getField(fieldName);

            if (fieldReflectionMetadata != null)
                return ApamIpojoInstrumentation.getCollectionType(fieldReflectionMetadata) != null;

            if (fieldIPojoMetadata != null)
                return ApamIpojoInstrumentation.getCollectionType(fieldIPojoMetadata) != null;

            throw new NoSuchFieldException("unavailable metadata for field " + fieldName);

        }

        @Override
        public boolean checkCallback(String callbackName) throws NoSuchMethodException {
            /*
             * Get iPojo metadata
             */
            MethodMetadata methodIPojoMetadata = null;
            if (pojoMetadata != null) {
                for (MethodMetadata method : pojoMetadata.getMethods(callbackName)) {
                    if (method.getMethodArguments().length <= 1) {
                        methodIPojoMetadata = method;
                    }

                }
            }

            /*
             * Try to get reflection information if available,.
             */
            Method methodReflectionMetadata = null;
            if (instrumentedCode != null) {
                try {
                    for (Method method : instrumentedCode.getDeclaredMethods()) {
                        if (method.getName().equals(callbackName) && (method.getParameterTypes().length <= 1))
                            methodReflectionMetadata = method;
                    }
                } catch (Exception e) {
                }
            }

            /*
             * Try to use reflection information
             */
            if (methodReflectionMetadata != null) {
                if (methodReflectionMetadata.getGenericParameterTypes().length == 1) {
                    Type parameterType = methodReflectionMetadata.getGenericParameterTypes()[0];
                    Class<?> parameterClass = null;

                    if (parameterType instanceof Class)
                        parameterClass = (Class<?>) parameterType;
                    if (parameterType instanceof ParameterizedType) {
                        parameterClass = (Class<?>) ((ParameterizedType) parameterType).getRawType();
                    }
                    /*
                     * Verify if the parameter type is an Apam Instance
                     */
                    if ((parameterClass != null) && Instance.class.equals(parameterClass)) {
                        return true;
                    }
                } else
                    return false;

            }

            /*
             * Try to use iPojo metadata
             */
            if (methodIPojoMetadata != null) {
                if (methodIPojoMetadata.getMethodArguments().length == 1) {
                    String parameterType = methodIPojoMetadata.getMethodArguments()[0];
                    /*
                     * Check If the single parameter type is an Apam Instance 
                     */
                    if (Instance.class.getCanonicalName().equals(parameterType))
                        return true;
                } else
                    return false;
            }
            throw new NoSuchMethodException("unavailable callback Or wrong argument : " + callbackName);
        }

        private Map<MethodMetadata, MessageReferenceExtended>
                getMethodsWithArgFromMetadata(String methodName, String type) {
            Map<MethodMetadata, MessageReferenceExtended> methodsIPojoMetadata = new HashMap<MethodMetadata, MessageReferenceExtended>();
            if (pojoMetadata != null) {
                for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {
                    if (method.getMethodArguments().length == 1) {
                        String parameterType = method.getMethodArguments()[0];
                        MessageReferenceExtended mRef;
                        /*
                         * If the single parameter type is a parameterized generic Message<D> we cannot determine its
                         * actual message payload
                         */
                        if (Message.class.getCanonicalName().equals(parameterType)) {
                            mRef = new MessageReferenceExtended(parameterType, true);
                            mRef.setCallbackMetadata(method);
                            mRef.setResourceUndefined(true);
                            methodsIPojoMetadata.put(method, mRef);
                        } else {// Otherwise it is the type of the actual message payload
                            if (type != null) {
                                if (parameterType.equals(type)) {
                                    mRef = new MessageReferenceExtended(type);
                                    mRef.setCallbackMetadata(method);
                                    methodsIPojoMetadata.put(method, mRef);
                                }
                            } else {
                                mRef = new MessageReferenceExtended(parameterType);
                                mRef.setCallbackMetadata(method);
                                methodsIPojoMetadata.put(method, mRef);
                            }
                        }
                    }
                }
            }
            return methodsIPojoMetadata;
        }

        private Map<MethodMetadata, MessageReferenceExtended> getMethodsWithReturnFromMetadata(String methodName,
                String type) {
            Map<MethodMetadata, MessageReferenceExtended> methodsIPojoMetadata = new HashMap<MethodMetadata, MessageReferenceExtended>();
            if (pojoMetadata != null) {
                for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {
                    if (!method.getMethodReturn().equals("void")) {
                        MessageReferenceExtended mRef;
                        if (Message.class.getCanonicalName().equals(method.getMethodReturn())) { // we cannot determine
                                                                                                 // its actual message
                                                                                                 // payload
                            mRef = new MessageReferenceExtended(method.getMethodReturn(), true);
                            mRef.setCallbackMetadata(method);
                            mRef.setResourceUndefined(true);
                            methodsIPojoMetadata.put(method, mRef);
                        } else { // Otherwise it is the type of the actual message payload
                            if (type != null) {
                                if (method.getMethodReturn().equals(type)) {
                                    mRef = new MessageReferenceExtended(type);
                                    mRef.setCallbackMetadata(method);
                                    methodsIPojoMetadata.put(method, mRef);
                                }
                            } else {
                                mRef = new MessageReferenceExtended(method.getMethodReturn());
                                mRef.setCallbackMetadata(method);
                                methodsIPojoMetadata.put(method, mRef);
                            }
                        }
                    }
                }
            }
            return methodsIPojoMetadata;
        }

        private Map<Method, MessageReferenceExtended> getMethodsWithArgFromReflection(String methodName, String type) {
            Map<Method, MessageReferenceExtended> methodsReflectionMetadata = new HashMap<Method, MessageReferenceExtended>();
            if (instrumentedCode != null) {
                for (Method method : instrumentedCode.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && (method.getParameterTypes().length == 1)) {
                        Type parameterType = method.getGenericParameterTypes()[0];
                        Class<?> parameterClass = null;

                        if (parameterType instanceof Class)
                            parameterClass = (Class<?>) parameterType;
                        if (parameterType instanceof ParameterizedType) {
                            parameterClass = (Class<?>) ((ParameterizedType) parameterType).getRawType();
                        }

                        if ((parameterClass != null) && Message.class.equals(parameterClass)) {

                            if (Message.class.equals(parameterClass)) { // Verify if the parameter type is a
                                                                        // parameterized generic Message<D> ant try to
                                                                        // get its actual payload
                                if (parameterType instanceof ParameterizedType) {
                                    Type[] genericParameters = ((ParameterizedType) parameterType)
                                            .getActualTypeArguments();
                                    if ((genericParameters.length == 1) && (genericParameters[0] instanceof Class))
                                        if (type != null) { // verify with the given type
                                            if (((Class<?>) genericParameters[0]).getCanonicalName().equals(type)) {
                                                methodsReflectionMetadata.put(method, new MessageReferenceExtended(
                                                        type, true));
                                            }
                                        } else {
                                            methodsReflectionMetadata.put(method, new MessageReferenceExtended(
                                                    ((Class<?>) genericParameters[0]).getCanonicalName(), true));
                                        }
                                }
                            } else { // Otherwise it is the type of the actual message payload
                                if (type != null) { // verify with the given type
                                    if (type.equals(parameterClass.getCanonicalName())) {
                                        methodsReflectionMetadata.put(method, new MessageReferenceExtended(
                                                parameterClass
                                                        .getCanonicalName()));
                                    }
                                } else {
                                    methodsReflectionMetadata.put(method, new MessageReferenceExtended(parameterClass
                                            .getCanonicalName()));
                                }
                            }
                        }
                    }
                }
            }
            return methodsReflectionMetadata;
        }

        private Map<Method, MessageReferenceExtended>
                getMethodsWithReturnFromReflection(String methodName, String type) {
            Map<Method, MessageReferenceExtended> methodsReflectionMetadata = new HashMap<Method, MessageReferenceExtended>();
            if (instrumentedCode != null) {
                for (Method method : instrumentedCode.getDeclaredMethods()) {
                    if ((method.getName().equals(methodName)) && (!method.getReturnType().equals(Void.TYPE))) {
                        if (method.getReturnType().getCanonicalName().equals(Message.class.getCanonicalName())) {
                            // TODO verify the parameter Type
                            System.out.println("Something to do here : " + method.getReturnType());
                        } else {
                            if (type != null) {
                                if (method.getReturnType().getCanonicalName().equals(type)) {
                                    methodsReflectionMetadata.put(method, new MessageReferenceExtended(type));
                                }
                            } else {
                                methodsReflectionMetadata.put(method, new MessageReferenceExtended(method
                                        .getReturnType()
                                        .getCanonicalName()));
                            }
                        }
                    }
                }
            }
            return methodsReflectionMetadata;
        }

        @Override
        public ResourceReference getCallbackReturnType(String methodName, String type) throws NoSuchMethodException {
            // get methods from metadata
            Map<MethodMetadata, MessageReferenceExtended> methodsMetadata = getMethodsWithReturnFromMetadata(
                    methodName, type);

            // get method from reflection
            Map<Method, MessageReferenceExtended> methodsReflection = getMethodsWithReturnFromReflection(methodName,
                    type);

            MessageReferenceExtended mr = null;

            // get the first one from reflection
            for (Method method : methodsReflection.keySet()) {
                mr = methodsReflection.get(method);
            }

            /*
             *WARNING: We supposed that the order is the same in  methodsMetadata and methodsReflection
             */
            // get the first one from metadata
            for (MethodMetadata method : methodsMetadata.keySet()) {
                if (mr != null)
                    mr.setCallbackMetadata(method);
                else
                    mr = methodsMetadata.get(method);
            }

            if (mr != null) {
                if (mr.isResourceUndefined())
                    return new UndefinedReference(methodName,MessageReference.class);
                else
                    return mr;
            }
            // no method was found
            throw new NoSuchMethodException("unavailable method : " + methodName);
        }

        @Override
        public ResourceReference getCallbackArgType(String methodName, String type) throws NoSuchMethodException {
            // get methods from metadata
            Map<MethodMetadata, MessageReferenceExtended> methodsMetadata = getMethodsWithArgFromMetadata(methodName,
                    type);

            // get method from reflection
            Map<Method, MessageReferenceExtended> methodsReflection = getMethodsWithArgFromReflection(methodName, type);

            MessageReferenceExtended mr = null;

            // get the first one from reflection
            for (Method method : methodsReflection.keySet()) {
                mr = methodsReflection.get(method);
            }

            /*
             *WARNING: We supposed that the order is the same in  methodsMetadata and methodsReflection
             */
            // get the first one from metadata
            for (MethodMetadata method : methodsMetadata.keySet()) {
                if (mr != null)
                    mr.setCallbackMetadata(method);
                else
                    mr = methodsMetadata.get(method);
            }

            if (mr != null) {
                if (mr.isResourceUndefined())
                    return new UndefinedReference(methodName,MessageReference.class);
                else
                    return mr;
            }
            // no method was found
            throw new NoSuchMethodException("unavailable method : " + methodName);
        }

        @Override
        public boolean isCollectionReturn(String methodName, String type) throws NoSuchMethodException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isCollectionArgument(String methodName, String type) throws NoSuchMethodException {
            // TODO Auto-generated method stub
            return false;
        }

    }

}
