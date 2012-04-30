package fr.imag.adele.apam.util;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.FieldInjection;
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
    private static final String        SPECIFICATION           = "specification";
    private static final String        IMPLEMENTATION          = "implementation";
    private static final String        COMPOSITE               = "composite";
    private static final String        INSTANCE                = "instance";
    private static final String        DEFINITIONS             = "definitions";
    private static final String        DEFINITION              = "definition";
    private static final String        PROPERTIES              = "properties";
    private static final String        PROPERTY                = "property";
    private static final String        DEPENDENCIES            = "dependencies";
    private static final String        INTERFACE               = "interface";
    private static final String        MESSAGE                 = "message";
    private static final String        CONSTRAINTS             = "constraints";
    private static final String        PREFERENCES             = "preferences";

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
    private static final String        ATT_FILTER              = "filter";


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
            instrumentedCode = (className != UNDEFINED && introspector != null) ? introspector.getInstrumentedClass(className) : null; 
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
            		return UNDEFINED;
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
            for (Class<?> supportedCollection : supportedCollections) {
                if (supportedCollection.equals(fieldClass)) {
                	
                	/* Try to get the underlying element type if possible, otherwise
                	 * return UNDEFINED
                	 */
                	if (fieldType instanceof ParameterizedType) {
                        Type[] parameters = ((ParameterizedType)fieldType).getActualTypeArguments();
                        if ((parameters.length == 1) && (parameters[0] instanceof Class))
                            return ((Class<?>) parameters[0]).getCanonicalName();
                        else
                        	return UNDEFINED;
                	}
                	
                	return UNDEFINED;
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

            for (Class<?> supportedCollection : supportedCollections) {
                if (supportedCollection.getCanonicalName().equals(fieldType)) {
                	return UNDEFINED;
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
            for (Class<?> supportedMessage : supportedMessages) {
                if (supportedMessage.equals(fieldClass)) {
                	
                	/* Try to get the underlying data type if possible, otherwise
                	 * return UNDEFINED
                	 */
                	if (fieldType instanceof ParameterizedType) {
                        Type[] parameters = ((ParameterizedType)fieldType).getActualTypeArguments();
                        if ((parameters.length == 1) && (parameters[0] instanceof Class))
                            return ((Class<?>) parameters[0]).getCanonicalName();
                        else
                        	return UNDEFINED;
                	}
                	
                	return UNDEFINED;
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
        	
            for (Class<?> supportedMessage : supportedMessages) {
                if (supportedMessage.getCanonicalName().equals(fieldType)) {
                	return UNDEFINED;
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
            if (pojoMetadata != null && pojoMetadata.getField(fieldName) != null)
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
            	String collectionType 	= getCollectionType(fieldReflectionMetadata);
            	if (collectionType != null)
            		return collectionType != UNDEFINED ?  new InterfaceReference(collectionType) : ResourceReference.UNDEFINED ;
            	
            	/*
            	 * Then verify if it is a message
            	 */
            	String messageType 	= getMessageType(fieldReflectionMetadata);
            	if (messageType != null)
            		return messageType != UNDEFINED ? new MessageReference(messageType) : ResourceReference.UNDEFINED;
            	
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
            	String collectionType 	= getCollectionType(fieldIPojoMetadata);
            	if (collectionType != null)
            		return collectionType != UNDEFINED ?  new InterfaceReference(collectionType) : ResourceReference.UNDEFINED ;
            	
            	/*
            	 * Then verify if it is a message
            	 */
            	String messageType 	= getMessageType(fieldIPojoMetadata);
            	if (messageType != null)
            		return messageType != UNDEFINED ? new MessageReference(messageType) : ResourceReference.UNDEFINED;
            	
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
						if (method.getName().equals(callbackName) && method.getParameterTypes().length == 1)
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
                 if (parameterClass != null && Message.class.equals(parameterClass)) {
                	 
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
            if (pojoMetadata != null && pojoMetadata.getField(fieldName) != null)
            	fieldIPojoMetadata = pojoMetadata.getField(fieldName);
            
            if (fieldReflectionMetadata != null)
            	return getCollectionType(fieldReflectionMetadata) != null;

            if (fieldIPojoMetadata != null)
            	return getCollectionType(fieldIPojoMetadata) != null;

          	throw new NoSuchFieldException("unvailbale metadata for field "+fieldName);

        }

    }
    /**
     * Parse a composite declaration
     */
    private CompositeDeclaration parseComposite(Element element)  {

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
        if (CoreMetadataParser.isResourceDependency(element)){

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
                if ((target != null) &&  !target.equals(dependencyInjection.getResource())) {
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

        String field = parseString(element, CoreMetadataParser.ATT_FIELD,false);
        String method = parseString(element, CoreMetadataParser.ATT_METHOD,false);
        
        if ( field == null && method == null)
        	errorHandler.error(Severity.ERROR, "attribute \""+ATT_FIELD+"\" or \""+ATT_METHOD+"\" must be specified in "+element);

        if (field == null && isInterfaceDependency(element))
        	errorHandler.error(Severity.ERROR, "attribute \""+ATT_FIELD+"\" must be specified in "+element);
       if (field == null && method == null)
        	field = UNDEFINED;
        
        return field != null ? new DependencyInjection.Field(primitive,field) : new DependencyInjection.Callback(primitive,method);

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
        return	 isInterfaceDependency(element) || isMessageDependency(element);
    }

    /**
     * Determines if this element represents an specification declaration
     */
    private static final boolean isInterfaceDependency(Element element) {
        return CoreMetadataParser.INTERFACE.equals(element.getName());
    }
    
    /**
     * Determines if this element represents an specification declaration
     */
    private static final boolean isMessageDependency(Element element) {
        return CoreMetadataParser.MESSAGE.equals(element.getName());
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
