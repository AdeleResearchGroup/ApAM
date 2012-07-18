package fr.imag.adele.apam.util;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration.Instrumentation;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ConstrainedReference;
import fr.imag.adele.apam.core.ContextualMissingPolicy;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.FieldInjection;
import fr.imag.adele.apam.core.GrantDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.MissingPolicy;
import fr.imag.adele.apam.core.OwnedComponentDeclaration;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ReleaseDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.core.TargetDeclaration;
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.AbstractProducer;
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
    private static final String        APAM                    = "fr.imag.adele.apam";
    private static final String        COMPONENT               = "component";
    private static final String        SPECIFICATION           = "specification";
    private static final String        IMPLEMENTATION          = "implementation";
    private static final String        COMPOSITE               = "composite";
    private static final String        INSTANCE                = "instance";
    private static final String        INSTANCE_ALT            = "apam-instance";
    private static final String        DEFINITIONS             = "definitions";
    private static final String        DEFINITION              = "definition";
    private static final String        PROPERTIES              = "properties";
    private static final String        PROPERTY                = "property";
    private static final String        DEPENDENCIES            = "dependencies";
    private static final String        DEPENDENCY              = "dependency";
    private static final String        INTERFACE               = "interface";
    private static final String        MESSAGE                 = "message";
    private static final String        CONSTRAINTS             = "constraints";
    private static final String        PREFERENCES             = "preferences";
    private static final String        OWNS                    = "owns";
    private static final String        WAIT                    = "wait";
    private static final String        DELETE                  = "delete";
    private static final String        MANDATORY               = "mandatory";
    private static final String        OPTIONAL                = "optional";
    private static final String        GRANT                   = "grant";
    private static final String        RELEASE                 = "release";
    private static final String        STATES                  = "states";

    private static final String        ATT_NAME                = "name";
    private static final String        ATT_CLASSNAME           = "classname";
    private static final String        ATT_SPECIFICATION       = "specification";
    private static final String        ATT_MAIN_IMPLEMENTATION = "mainImplem";
    private static final String        ATT_IMPLEMENTATION      = "implementation";
    private static final String        ATT_INTERFACES          = "interfaces";
    private static final String        ATT_MESSAGES            = "messages";
    private static final String        ATT_MESSAGE_FIELDS	   = "message-fields";
    private static final String        ATT_TYPE                = "type";
    private static final String        ATT_VALUE               = "value";
    private static final String        ATT_FIELD               = "field";
    private static final String        ATT_METHOD              = "method";
    private static final String        ATT_ID                  = "id";
    private static final String        ATT_MULTIPLE            = "multiple";
    private static final String        ATT_MISSING             = "missing";
    private static final String        ATT_FILTER              = "filter";
    private static final String        ATT_WHEN                = "when";
    private static final String        ATT_VALUES              = "values";
    private static final String        ATT_DEFAULT             = "default";


    /**
     * The parsed metatadata
     */
    private Element metadata;

    /**
     * The optional service that give access to introspection  information
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
    private ErrorHandler errorHandler;


    public CoreMetadataParser(Element metadata) {
        this(metadata,null);
    }

    public CoreMetadataParser(Element metadata, IntrospectionService introspector) {
        setMetadata(metadata, introspector);
    }


    /**
     * Initialize parser with the given metadata and instrumentation code
     */
    public synchronized void setMetadata(Element metadata, IntrospectionService introspector) {
        this.metadata 		= metadata;
        this.introspector	= introspector;
        declaredElements	= null;
    }

    /**
     * Parse the ipojo metadata to get the component declarations
     */
    @Override
    public synchronized List<ComponentDeclaration> getDeclarations(ErrorHandler errorHandler) {
        if (declaredElements != null)
            return declaredElements;

        declaredElements 	= new ArrayList<ComponentDeclaration>();
        this.errorHandler	= errorHandler;

        List<SpecificationDeclaration> specifications 		= new ArrayList<SpecificationDeclaration>();
        List<AtomicImplementationDeclaration> primitives 	= new ArrayList<AtomicImplementationDeclaration>();
        List<CompositeDeclaration> composites 				= new ArrayList<CompositeDeclaration>();
        List<InstanceDeclaration> instances 				= new ArrayList<InstanceDeclaration>();

        for (Element element : metadata.getElements()) {

            /*
             * Ignore not APAM elements 
             */
            if (! CoreMetadataParser.isApamDefinition(element))
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
        metadata 			= null;
        introspector		= null;
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

        String className						= parseString(element,CoreMetadataParser.ATT_CLASSNAME,true);

        /*
         * load Pojo instrumentation metadata
         */
        PojoMetadata pojoMetadata = null;
        Class<?> instrumentedCode = null;
        try {
            pojoMetadata = new PojoMetadata(element);
            instrumentedCode = ((className != CoreParser.UNDEFINED) && (introspector != null)) ? introspector.getInstrumentedClass(className) : null; 
        }
        catch (ClassNotFoundException e) {
            errorHandler.error(Severity.ERROR, "Apam component "+ name + ": "+"the component class "+ className+" can not be loaded");
        }
        catch (Exception ignoredException) {
        }

        Instrumentation instrumentation = new ApamIpojoInstrumentation(className,pojoMetadata,instrumentedCode); 

        AtomicImplementationDeclaration declaration = new AtomicImplementationDeclaration(name,specification,instrumentation);
        parseComponent(element,declaration);

        /*
         *  Parse message producer field injection
         */
        String messageFields		= parseString(element,CoreMetadataParser.ATT_MESSAGE_FIELDS,false);
        for (String messageField : Util.split(messageFields)) {

            /*
             * TODO Verify that the type of the field can be assigned to AbstractConsumer<?> 
             */
            declaration.getProducerInjections().add(new FieldInjection(declaration,messageField));
        }

        /*
         *  Verify that at least one field is injected for each declared produced message.
         */
        for (MessageReference message : declaration.getProvidedResources(MessageReference.class)) {

            boolean declared	= declaration.getProducerInjections().size() > 0;
            boolean injected 	= false;
            boolean defined		= false;

            for (FieldInjection messageField : declaration.getProducerInjections()) {
                if (messageField.getResource() == ResourceReference.UNDEFINED)
                    continue;
                defined = true;

                if (! messageField.getResource().equals(message))
                    continue;
                injected = true;
                break;
            }

            /*
             * If we could determine the field types and there was no injection then signal error
             * 
             * NOTE Notice that we some errors will not be detected at build time since all the reflection
             * information is not available, and validation must be delayed until run time
             */
            if (!declared || (defined && !injected))
                errorHandler.error(Severity.ERROR, "Apam component "+ name + ": "+"produced message "+ message.getJavaType()+" is not injected in any field");

        }

        /*
         *  if not explicitly provided, get all the implemented interfaces.
         */
        if (declaration.getProvidedResources().isEmpty() && (pojoMetadata != null) ) {
            for (String implementedInterface : pojoMetadata.getInterfaces()) {
                if (!implementedInterface.startsWith("java.lang"))
                    declaration.getProvidedResources().add(new InterfaceReference(implementedInterface));
            }
        }


        /*
         * If not explicitly provided, get all produced messages from the declared injected fields
         */
        Set<MessageReference> declaredMessages = declaration.getProvidedResources(MessageReference.class);
        for (FieldInjection messageField : declaration.getProducerInjections()) {

            if (messageField.getResource() == ResourceReference.UNDEFINED)
                continue;

            if (declaredMessages.contains(messageField.getResource()))
                continue;

            declaration.getProvidedResources().add(messageField.getResource());
        }

        /*
         *  If instrumented code is provided verify that all provided resources reference accessible classes
         */
        if (introspector != null) {
            for (ResourceReference providedResource : declaration.getProvidedResources()) {
                try {
                    introspector.getInstrumentedClass(providedResource.getJavaType());
                } catch (ClassNotFoundException e) {
                    errorHandler.error(Severity.ERROR, "Apam component "+ name + ": "+"the provided resource "+ providedResource.getJavaType()+" can not be loaded");
                }
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

        /**
         * The optional reflection information
         */
        private final Class<?> instrumentedCode;

        public ApamIpojoInstrumentation(String className, PojoMetadata pojoMetadata, Class<?> instrumentedCode) {
            this.className			= className;
            this.pojoMetadata		= pojoMetadata;
            this.instrumentedCode	= instrumentedCode;
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
        private final static Class<?>[] supportedMessages = new Class<?>[] { 
            AbstractConsumer.class,
            AbstractProducer.class };


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
            if (fieldType  instanceof Class) {
                Class<?> fieldClass = (Class<?>) fieldType;
                Class<?> elementType = fieldClass.getComponentType();
                if (fieldClass.isArray())
                    return elementType.getCanonicalName();
            }

            if (fieldType instanceof GenericArrayType) {
                GenericArrayType fieldClass = (GenericArrayType) fieldType;
                Type elementType = fieldClass.getGenericComponentType();
                if (elementType instanceof Class) 
                    ((Class<?>)elementType).getCanonicalName();
                else
                    return CoreParser.UNDEFINED;
            }

            /*
             * Next try to see if the raw class of the field is one of the supported collections
             */
            Class<?> fieldClass = null;
            if (fieldType  instanceof Class)
                fieldClass = (Class<?>) fieldType;
            if (fieldType instanceof ParameterizedType) {
                fieldClass = (Class<?>)((ParameterizedType) fieldType).getRawType();
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
                        Type[] parameters = ((ParameterizedType)fieldType).getActualTypeArguments();
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
            if (fieldType  instanceof Class)
                fieldClass = (Class<?>) fieldType;
            if (fieldType instanceof ParameterizedType) {
                fieldClass = (Class<?>)((ParameterizedType) fieldType).getRawType();
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
                        Type[] parameters = ((ParameterizedType)fieldType).getActualTypeArguments();
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
        public ResourceReference getFieldType(String fieldName)  throws NoSuchFieldException {

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
                String collectionType 	= ApamIpojoInstrumentation.getCollectionType(fieldReflectionMetadata);
                if (collectionType != null)
                    return collectionType != CoreParser.UNDEFINED ?  new InterfaceReference(collectionType) : ResourceReference.UNDEFINED ;

                    /*
                     * Then verify if it is a message
                     */
                    String messageType 	= ApamIpojoInstrumentation.getMessageType(fieldReflectionMetadata);
                    if (messageType != null)
                        return messageType != CoreParser.UNDEFINED ? new MessageReference(messageType) : ResourceReference.UNDEFINED;

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
                String collectionType 	= ApamIpojoInstrumentation.getCollectionType(fieldIPojoMetadata);
                if (collectionType != null)
                    return collectionType != CoreParser.UNDEFINED ?  new InterfaceReference(collectionType) : ResourceReference.UNDEFINED ;

                    /*
                     * Then verify if it is a message
                     */
                    String messageType 	= ApamIpojoInstrumentation.getMessageType(fieldIPojoMetadata);
                    if (messageType != null)
                        return messageType != CoreParser.UNDEFINED ? new MessageReference(messageType) : ResourceReference.UNDEFINED;

                        /*
                         * Otherwise it's a normal field we just return its type name
                         */
                        return new InterfaceReference(fieldIPojoMetadata.getFieldType());
            }

            throw new NoSuchFieldException("unavailable type for field "+fieldName);

        }

        /**
         * Get the type of message from the instrumented metadata of the callback method
         */
        @Override
        public ResourceReference getCallbackType(String callbackName) throws NoSuchMethodException {
            /*
             * Get iPojo metadata
             */
            MethodMetadata methodIPojoMetadata = null;
            if (pojoMetadata != null ) {
                for (MethodMetadata method : pojoMetadata.getMethods(callbackName)) {
                    if (method.getMethodArguments().length == 1) {
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
                        if (method.getName().equals(callbackName) && (method.getParameterTypes().length == 1))
                            methodReflectionMetadata = method;
                    }
                } catch (Exception e) {
                } 
            }

            /*
             * Try to use reflection information
             */
            if (methodReflectionMetadata != null) {

                Type parameterType 		= methodReflectionMetadata.getGenericParameterTypes()[0];
                Class<?> parameterClass = null;

                if (parameterType  instanceof Class)
                    parameterClass = (Class<?>) parameterType;
                if (parameterType instanceof ParameterizedType) {
                    parameterClass = (Class<?>)((ParameterizedType) parameterType).getRawType();
                }

                /*
                 * Verify if the parameter type is a parameterized generic Message<D> ant try to get its
                 * actual payload
                 */
                if ((parameterClass != null) && Message.class.equals(parameterClass)) {

                    if (parameterType instanceof ParameterizedType) {
                        Type[] genericParameters = ((ParameterizedType)parameterType).getActualTypeArguments();
                        if ((genericParameters.length == 1) && (genericParameters[0] instanceof Class))
                            return new MessageReference(((Class<?>) genericParameters[0]).getCanonicalName());
                    }

                    return ResourceReference.UNDEFINED;

                }

                /*
                 * Otherwise it is the type of the actual message payload
                 */
                if (parameterClass != null)
                    return new MessageReference(parameterClass.getCanonicalName());

            }

            /*
             * Try to use iPojo metadata
             */
            if (methodIPojoMetadata != null) {

                String parameterType = methodIPojoMetadata.getMethodArguments()[0];

                /*
                 * If the single parameter type is a parameterized generic Message<D> we cannot determine its
                 * actual message payload
                 */
                if (Message.class.getCanonicalName().equals(parameterType))
                    return ResourceReference.UNDEFINED;

                /*
                 * Otherwise it is the type of the actual message payload
                 */
                return new MessageReference(parameterType);
            }

            throw new NoSuchMethodException("unavailable type for callback "+callbackName);

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
                }
                catch (Exception ignored) {
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

            throw new NoSuchFieldException("unavailable metadata for field "+fieldName);

        }

    }
    /**
     * Parse a composite declaration
     */
    private CompositeDeclaration parseComposite(Element element)  {

        String name 								= parseName(element);
        SpecificationReference specification		= parseSpecificationReference(element,CoreMetadataParser.ATT_SPECIFICATION,false);
        ComponentReference<?> implementation 		= parseComponentReference(element,CoreMetadataParser.COMPONENT,CoreMetadataParser.ATT_MAIN_IMPLEMENTATION, true);

        Element states[]	= optional(element.getElements(CoreMetadataParser.STATES, CoreMetadataParser.APAM));

        if (states.length > 1)
            errorHandler.error(Severity.ERROR, "A single state declaration is allowed "+element);

        String enocedInitial	= states.length > 0 ? parseString(states[0],CoreMetadataParser.ATT_DEFAULT,true) : CoreParser.UNDEFINED;
        String encodedStates 	= states.length > 0 ? parseString(states[0],CoreMetadataParser.ATT_VALUES,true)  : CoreParser.UNDEFINED;

        String initialState		= enocedInitial != CoreParser.UNDEFINED ? enocedInitial : null;
        List<String> statesDeclaration = encodedStates != CoreParser.UNDEFINED ? Util.splitList(encodedStates) : Util.splitList("");

        CompositeDeclaration declaration = new CompositeDeclaration(name,specification,implementation, initialState, statesDeclaration);
        parseComponent(element,declaration);

        parseOwns(element,declaration);
        parseOwnedInstances(element,declaration);
        parseContextualMissingPolicies(element,declaration);
        parseResourceConflictPolicies(element,declaration);

        return declaration;
    }


    /**
     * Parse an instance declaration
     */
    private InstanceDeclaration parseInstance(Element element) {

        String name 								= parseName(element);
        ImplementationReference<?> implementation	= parseImplementationReference(element,CoreMetadataParser.ATT_IMPLEMENTATION,true);

        TargetDeclaration targetDeclaration = null;

        /*
         * look for optional target declaration
         */
        for (Element target : optional(element.getElements())) {

            String targetKind = CoreMetadataParser.getTargetKind(target);

            /*
             * Skip unrelated elements
             */
            if (targetKind == CoreParser.UNDEFINED)
                continue;

            /*
             * Try to avoid conflict with dependency declarations
             * 
             * TODO Currently if the dependencies tag is not used, it is ambiguous if the
             * declaration corresponds to a dependency of the instance or a target instantiation
             * condition 
             */

            if (! target.getName().equals(targetKind))
                continue;

            String targetAttribute			= CoreMetadataParser.getTargetAttribute(target,targetKind);
            ResolvableReference reference 	= parseResolvableReference(target,targetKind,targetAttribute,true);

            targetDeclaration = new TargetDeclaration(reference);

            /*
             * parse optional constraints
             */
            for (Element constraints : optional(target.getElements(CoreMetadataParser.CONSTRAINTS,CoreMetadataParser.APAM))) {
                parseConstraints(constraints,targetDeclaration);
            }

        }
        InstanceDeclaration declaration = new InstanceDeclaration(implementation,name,targetDeclaration);
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
            value = CoreParser.UNDEFINED;
        }

        if (mandatory && (value != null) && value.trim().isEmpty()) {
            errorHandler.error(Severity.ERROR, "attribute \""+attributeName+"\" cannot be empty in "+element);
            value = CoreParser.UNDEFINED;
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
        return ((specification == null) && ! mandatory) ? null : new SpecificationReference(specification);
    }

    /**
     * Get an implementation reference coded in an attribute
     */
    private ImplementationReference<?> parseImplementationReference(Element element, String attibute, boolean mandatory) {
        String implementation = parseString(element,attibute,mandatory);
        return ((implementation == null) && ! mandatory) ? null : new ImplementationReference<ImplementationDeclaration>(implementation);
    }

    /**
     * Get a component reference coded in an attribute
     */
    private ComponentReference<?> parseAnyComponentReference(Element element, String attibute, boolean mandatory) {
        String component = parseString(element,attibute,mandatory);
        return ((component == null) && ! mandatory) ? null : new ComponentReference<ComponentDeclaration>(component);
    }

    /**
     * Get an interface reference coded in an attribute
     */
    private InterfaceReference parseInterfaceReference(Element element, String attibute, boolean mandatory) {
        String interfaceName = parseString(element,attibute,mandatory);
        return ((interfaceName == null) && ! mandatory) ? null : new InterfaceReference(interfaceName);
    }

    /**
     * Get a message reference coded in an attribute
     */
    private MessageReference parseMessageReference(Element element, String attibute, boolean mandatory) {
        String messageName = parseString(element,attibute,mandatory);
        return ((messageName == null) && ! mandatory) ? null : new MessageReference(messageName);
    }

    /**
     * Get a resolvable reference coded in an attribute
     */
    private ResolvableReference parseResolvableReference(Element element, String referenceKind, String attribute, boolean mandatory) {

        if (CoreMetadataParser.isComponentTarget(referenceKind))
            return parseComponentReference(element, referenceKind, attribute, mandatory);

        if (CoreMetadataParser.isResourceTarget(referenceKind))
            return parseResourceReference(element, referenceKind, attribute, mandatory);

        return mandatory? new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED) : null;
    }

    /**
     * Get a resource reference coded in an attribute
     */
    private ResourceReference parseResourceReference(Element element, String referenceKind, String attribute, boolean mandatory) {

        if (CoreMetadataParser.isInterfaceReference(referenceKind))
            return parseInterfaceReference(element,attribute,mandatory);

        if (CoreMetadataParser.isMessageReference(referenceKind))
            return parseMessageReference(element,attribute,mandatory);

        return mandatory? ResourceReference.UNDEFINED : null;

    }

    /**
     * Get a component reference coded in an attribute
     */
    private ComponentReference<?> parseComponentReference(Element element, String referenceKind, String attribute, boolean mandatory) {

        if (CoreMetadataParser.isSpecificationReference(referenceKind))
            return parseSpecificationReference(element,attribute,mandatory);

        if (CoreMetadataParser.isImplementationReference(referenceKind))
            return parseImplementationReference(element,attribute,mandatory);

        if (CoreMetadataParser.isAnyComponentReference(referenceKind))
            return parseAnyComponentReference(element,attribute,mandatory);


        return mandatory ? new ComponentReference<ComponentDeclaration>(CoreParser.UNDEFINED): null;

    }

    /**
     * Determines if this element represents an specification reference
     */
    private static final boolean isSpecificationReference(String referenceKind) {
        return CoreMetadataParser.SPECIFICATION.equals(referenceKind);
    }

    /**
     * Determines if this element represents an implementation reference
     */
    private static final boolean isImplementationReference(String referenceKind) {
        return CoreMetadataParser.IMPLEMENTATION.equals(referenceKind);
    }

    /**
     * Determines if this element represents a component reference
     */
    private static final boolean isAnyComponentReference(String referenceKind) {
        return CoreMetadataParser.COMPONENT.equals(referenceKind);
    }

    /**
     * Determines if this element represents an interface reference
     */
    private static final boolean isInterfaceReference(String referenceKind) {
        return CoreMetadataParser.INTERFACE.equals(referenceKind);
    }

    /**
     * Determines if this element represents a message reference
     */
    private static final boolean isMessageReference(String referenceKind) {
        return CoreMetadataParser.MESSAGE.equals(referenceKind);
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

        /*
         *	Skip the optional enclosing list 
         */
        for (Element dependencies : optional(element.getElements(CoreMetadataParser.DEPENDENCIES,CoreMetadataParser.APAM))) {
            parseDependencies(dependencies, component);
        }

        /*
         * Iterate over all sub elements looking for dependency declarations
         */
        for (Element dependency : optional(element.getElements())) {

            /*
             * ignore elements that are not from APAM
             */
            if (!CoreMetadataParser.isApamDefinition(dependency))
                continue;

            /*
             * ignore elements that are not dependencies 
             */
            if (! CoreMetadataParser.isDependency(dependency))
                continue;

            parseDependency(dependency,component);
        }


    }


    /**
     * parse a dependency declaration
     */
    private void parseDependency(Element element, ComponentDeclaration component) {

        String targetKind		= CoreMetadataParser.getTargetKind(element);
        String attributeTarget  = CoreMetadataParser.getTargetAttribute(element,targetKind);

        /*
         * All dependencies have an optional identifier 
         */
        String id = parseString(element,CoreMetadataParser.ATT_ID,false);
        DependencyDeclaration dependency = null;


        /*
         * Complex dependencies reference a single mandatory specification, and in the case of atomic components
         * may optionally have a number of field injection declarations
         */
        if (CoreMetadataParser.isComponentTarget(targetKind)) {

            ResolvableReference target = parseComponentReference(element,targetKind,attributeTarget,true);
            dependency = new DependencyDeclaration(component,id,target);

            if (component instanceof AtomicImplementationDeclaration) {

                AtomicImplementationDeclaration atomic = (AtomicImplementationDeclaration) component;
                for (Element injection : optional(element.getElements())) {

                    /*
                     * ignore elements that are not from APAM
                     */
                    if (!CoreMetadataParser.isApamDefinition(injection))
                        continue;

                    String injectionTarget = CoreMetadataParser.getTargetKind(injection);
                    if (injectionTarget == CoreParser.UNDEFINED)
                        continue;

                    if (!CoreMetadataParser.isResourceTarget(injectionTarget))
                        continue;

                    DependencyInjection dependencyInjection = parseDependencyInjection(injection,atomic);
                    dependencyInjection.setDependency(dependency);

                }

                String field = parseString(element, CoreMetadataParser.ATT_FIELD,false);
                String method = parseString(element, CoreMetadataParser.ATT_METHOD,false);

                if ( (field != null) || (method != null)) {
                    DependencyInjection dependencyInjection =  field != null ? new DependencyInjection.Field(atomic,field) : new DependencyInjection.Callback(atomic,method);
                    dependencyInjection.setDependency(dependency);
                }

                if (dependency.getInjections().isEmpty()) {
                    errorHandler.error(Severity.ERROR,
                            "A field must be defined for dependencies in primitive implementation "
                            + component.getName());
                }

            }
        }

        /*
         * Simple dependencies reference a single resource. 
         */
        if (CoreMetadataParser.isResourceTarget(targetKind) || (targetKind == CoreParser.UNDEFINED)) {

            ResourceReference target = parseResourceReference(element,targetKind,attributeTarget,false);

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
                if ((target != null) &&  !target.equals(dependencyInjection.getResource())) {
                    errorHandler.error(Severity.ERROR, "dependency target doesn't match the type of the field in "+element);
                }

                /*
                 * If a target is not explicitly declared use the injected field metadata
                 */
                if (target == null) {
                    id 		= dependencyInjection.getName();
                    target	= dependencyInjection.getResource();
                }

                dependency = new DependencyDeclaration(component,id,target);
                dependencyInjection.setDependency(dependency);

            } 
            else {
                /*
                 * For other components, a target must be explicitly specified
                 */
                target = parseResourceReference(element,targetKind,attributeTarget,true);
                dependency = new DependencyDeclaration(component,id,target);
            }

        }


        for (Element constraints : optional(element.getElements(CoreMetadataParser.CONSTRAINTS,CoreMetadataParser.APAM))) {
            parseConstraints(constraints, dependency);
        }

        for (Element preferences : optional(element.getElements(CoreMetadataParser.PREFERENCES,CoreMetadataParser.APAM))) {
            parsePreferences(preferences, dependency);
        }

        /*
         * Get the optional missing policy
         */
        String encodedPolicy = parseString(element,CoreMetadataParser.ATT_MISSING,false);
        MissingPolicy policy = encodedPolicy != null ? parsePolicy(encodedPolicy) : MissingPolicy.OPTIONAL;

        dependency.setMissingPolicy(policy);
    }


    /**
     * Parse an encoded missing policy name
     */
    private MissingPolicy parsePolicy(String encodedPolicy) {
        if (CoreMetadataParser.WAIT.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.WAIT;

        if (CoreMetadataParser.DELETE.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.DELETE;

        if (CoreMetadataParser.MANDATORY.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.MANDATORY;

        if (CoreMetadataParser.OPTIONAL.equalsIgnoreCase(encodedPolicy))
            return MissingPolicy.OPTIONAL;

        errorHandler.error(Severity.ERROR, "invalid value for miising policy : \""+encodedPolicy+"\",  accepted values are "+CoreMetadataParser.MISSING_VALUES.toString());
        return null;
    }

    /**
     * parse the injected dependencies of a primitive
     */
    private DependencyInjection parseDependencyInjection(Element element, AtomicImplementationDeclaration primitive) {

        String injectionKind = CoreMetadataParser.getTargetKind(element);

        String field = parseString(element, CoreMetadataParser.ATT_FIELD,false);
        String method = parseString(element, CoreMetadataParser.ATT_METHOD,false);

        if ( (field == null) && (method == null))
            errorHandler.error(Severity.ERROR, "attribute \""+CoreMetadataParser.ATT_FIELD+"\" or \""+CoreMetadataParser.ATT_METHOD+"\" must be specified in "+element);

        if ((field == null) && CoreMetadataParser.isInterfaceReference(injectionKind))
            errorHandler.error(Severity.ERROR, "attribute \""+CoreMetadataParser.ATT_FIELD+"\" must be specified in "+element);

        if ((field == null) && (method == null))
            field = CoreParser.UNDEFINED;

        return field != null ? new DependencyInjection.Field(primitive,field) : new DependencyInjection.Callback(primitive,method);

    }

    /**
     * parse a constraints declaration
     */
    private void parseConstraints(Element element, ConstrainedReference reference) {

        for (Element constraint : optional(element.getElements())) {

            String filter = parseString(constraint, CoreMetadataParser.ATT_FILTER);

            if (constraint.getName().equals(CoreMetadataParser.IMPLEMENTATION))
                reference.getImplementationConstraints().add(filter);

            if (constraint.getName().equals(CoreMetadataParser.INSTANCE))
                reference.getInstanceConstraints().add(filter);

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
     * Parse the list of owned components of a composite
     */
    private void parseOwns(Element element, CompositeDeclaration composite) {
        for (Element owned : optional(element.getElements(CoreMetadataParser.OWNS, CoreMetadataParser.APAM))) {

            String targetKind = CoreMetadataParser.getTargetKind(owned);
            if (! CoreMetadataParser.isComponentTarget(targetKind))
                continue;

            String targetAttribute 		= CoreMetadataParser.getTargetAttribute(owned,targetKind);
            ResolvableReference target	= parseComponentReference(owned,targetKind,targetAttribute,true);

            OwnedComponentDeclaration ownedComponent = new OwnedComponentDeclaration((ComponentReference<?>)target);

            /*
             * parse optional constraints
             */
            for (Element constraints : optional(owned.getElements(CoreMetadataParser.CONSTRAINTS,CoreMetadataParser.APAM))) {
                parseConstraints(constraints,ownedComponent);
            }

            composite.getOwnedComponents().add(ownedComponent);
        }
    }

    /**
     * Parse the list of owned instances of a composite
     */
    private void parseOwnedInstances(Element element, CompositeDeclaration composite) {

        for (Element instance : element.getElements()) {

            if (!CoreMetadataParser.isInstance(instance))
                continue;

            composite.getInstanceDeclarations().add(parseInstance(instance));

        }

    }


    /**
     * Parse the list of contextual dependency policies of a composite
     */
    private void parseContextualMissingPolicies(Element element, CompositeDeclaration composite) {

        for (Element policy : optional(element.getElements())) {

            if (! CoreMetadataParser.isContextualMissingPolicy(element))
                continue;

            String targetKind = CoreMetadataParser.getTargetKind(policy);

            if (!CoreMetadataParser.isComponentTarget(targetKind)) {
                errorHandler.error(Severity.ERROR, "component name must be specified for \""+policy.getName()+"\" declaration");
                continue;
            }

            String targetAttribute 		= CoreMetadataParser.getTargetAttribute(policy,targetKind);
            ResolvableReference target	= parseComponentReference(policy,targetKind,targetAttribute,true);
            String identifier 			= parseString(policy,CoreMetadataParser.ATT_ID,true);

            DependencyDeclaration.Reference dependency = new DependencyDeclaration.Reference((ComponentReference<?>)target,identifier);
            MissingPolicy missingPoliciy = parsePolicy(policy.getName());

            ContextualMissingPolicy policyDeclaration = new ContextualMissingPolicy(dependency,missingPoliciy);

            composite.getMissingPolicies().add(policyDeclaration);
        }
    }

    /**
     * Parse the list of resource policies (grants/releases) of a composite
     */
    private void parseResourceConflictPolicies(Element element, CompositeDeclaration composite) {

        for (Element policy : optional(element.getElements())) {

            if (! CoreMetadataParser.isResourceConflictPolicy(element))
                continue;

            String targetKind = CoreMetadataParser.getTargetKind(policy);

            if (!CoreMetadataParser.isComponentTarget(targetKind)) {
                errorHandler.error(Severity.ERROR, "component name must be specified for \""+policy.getName()+"\" declaration");
                continue;
            }

            String targetAttribute 		= CoreMetadataParser.getTargetAttribute(policy,targetKind);
            ResolvableReference target	= parseComponentReference(policy,targetKind,targetAttribute,true);
            String identifier 			= parseString(policy,CoreMetadataParser.ATT_ID,true);

            DependencyDeclaration.Reference dependency = new DependencyDeclaration.Reference((ComponentReference<?>)target,identifier);

            String states = parseString(policy,CoreMetadataParser.ATT_WHEN,true);

            if (CoreMetadataParser.GRANT.equals(policy.getName()))
                composite.getGrants().add(new GrantDeclaration(dependency, new HashSet<String>(Arrays.asList(Util.split(states)))));

            if (CoreMetadataParser.RELEASE.equals(policy.getName()))
                composite.getReleases().add(new ReleaseDeclaration(dependency, new HashSet<String>(Arrays.asList(Util.split(states)))));

        }
    }

    /**
     * Tests whether the specified element is an Apam declaration
     */
    private static final boolean isApamDefinition(Element element) {
        return  (element.getNameSpace() != null) && CoreMetadataParser.APAM.equals(element.getNameSpace());

    }

    private final static List<String> COMPONENT_TARGETS	= Arrays.asList(CoreMetadataParser.SPECIFICATION, CoreMetadataParser.IMPLEMENTATION, CoreMetadataParser.COMPONENT); 
    private final static List<String> RESOURCE_TARGETS	= Arrays.asList(CoreMetadataParser.INTERFACE, CoreMetadataParser.MESSAGE); 

    @SuppressWarnings("unchecked")
    private final static List<String> ALL_TARGETS  		= CoreMetadataParser.union(CoreMetadataParser.COMPONENT_TARGETS,CoreMetadataParser.RESOURCE_TARGETS); 

    /**
     * Get the kind of a target
     */
    private static final String getTargetKind(Element element) {

        if (CoreMetadataParser.ALL_TARGETS.contains(element.getName()))
            return element.getName();

        for (String dependencyKind : CoreMetadataParser.ALL_TARGETS) {
            if (element.getAttribute(dependencyKind) != null)
                return dependencyKind;
        }

        return CoreParser.UNDEFINED;

    }

    /**
     * Get the attribute specifying the target of dependency
     */
    private static final String getTargetAttribute(Element element, String targetKind) {
        return targetKind.equals(element.getName()) ? CoreMetadataParser.ATT_NAME : targetKind;
    }


    /**
     * Determines if the element represents a dependency
     */
    private static final boolean isDependency(Element element) {
        return CoreMetadataParser.DEPENDENCY.equals(element.getName()) || CoreMetadataParser.ALL_TARGETS.contains(element.getName());
    }

    /**
     * Determines if the element represents a component target
     */
    private static final boolean isComponentTarget(String targetKind) {
        return CoreMetadataParser.COMPONENT_TARGETS.contains(targetKind);
    }

    /**
     * Determines if the element represents a resource target
     */
    private static final boolean isResourceTarget(String targetKind) {
        return CoreMetadataParser.RESOURCE_TARGETS.contains(targetKind);
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
        return CoreMetadataParser.INSTANCE.equals(element.getName()) || CoreMetadataParser.INSTANCE_ALT.equals(element.getName());
    }

    private static final List<String> MISSING_VALUES 			= Arrays.asList(CoreMetadataParser.WAIT,CoreMetadataParser.DELETE,CoreMetadataParser.MANDATORY,CoreMetadataParser.OPTIONAL);

    /**
     * Whether this element represents a contextual policy definition
     */
    private static final boolean isContextualMissingPolicy(Element element) {
        return CoreMetadataParser.MISSING_VALUES.contains(element.getName());
    }

    private static final List<String> RESOURCE_POLICY_VALUES 	= Arrays.asList(CoreMetadataParser.GRANT,CoreMetadataParser.RELEASE);

    /**
     * Whether this element represents a resource policy definition
     */
    private static final boolean isResourceConflictPolicy(Element element) {
        return CoreMetadataParser.RESOURCE_POLICY_VALUES.contains(element.getName());
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
     * Utility method to have a static union of lists
     */
    private static List<String> union(List<String>... lists) {
        List<String> result = new ArrayList<String>();
        for (List<String> list : lists) {
            result.addAll(list);
        }
        return result;
    }


}
