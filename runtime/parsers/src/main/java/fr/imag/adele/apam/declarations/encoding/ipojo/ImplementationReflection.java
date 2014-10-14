package fr.imag.adele.apam.declarations.encoding.ipojo;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;
import fr.imag.adele.apam.declarations.encoding.Decoder;

/**
 * A utility class to obtain information about declared fields and methods.
 * 
 * It tries to use java reflection metadata if available, otherwise it fall
 * backs to use the iPojo metadata
 * 
 * @author vega
 * 
 */
public class ImplementationReflection implements CodeReflection {

	/**
	 * The iPojo generated metadata
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

	public ImplementationReflection(String className, PojoMetadata pojoMetadata, Class<?> instrumentedCode) {
		this.className = className;
		this.pojoMetadata = pojoMetadata;
		this.instrumentedCode = instrumentedCode;
	}

	/**
	 * The list of supported collections for aggregate dependencies
	 */
	private final static Class<?>[] supportedCollections = new Class<?>[] { Collection.class, List.class, Vector.class, Set.class };

	/**
	 * The list of supported types for push message queues
	 */
	private final static Class<?>[] supportedMessageQueues = new Class<?>[] { Queue.class, };

	/**
	 * Utility method to get the associated wrapper class name for a
	 * primitive type
	 */

	private final static Map<String, Class<?>> wrappers = new HashMap<String, Class<?>>();

	static {
		wrappers.put(Boolean.TYPE.getName(), Boolean.class);
		wrappers.put(Character.TYPE.getName(), Character.class);
		wrappers.put(Byte.TYPE.getName(), Byte.class);
		wrappers.put(Short.TYPE.getName(), Short.class);
		wrappers.put(Integer.TYPE.getName(), Integer.class);
		wrappers.put(Float.TYPE.getName(), Float.class);
		wrappers.put(Long.TYPE.getName(), Long.class);
		wrappers.put(Double.TYPE.getName(), Double.class);
	}

	/**
	 * If the type of the specified field is one of the supported
	 * collections returns the type of the elements in the collection,
	 * otherwise return null.
	 * 
	 * May return {@link CoreParser#UNDEFINED} if field is defined as a
	 * collection but the type of the elements in the collection cannot be
	 * determined.
	 */
	private static String getCollectionType(Field field) {

		Type fieldType = field.getGenericType();
		Class<?> fieldClass = getRawClass(fieldType);

		if (fieldClass == null) {
			return null;
		}

		/*
		 * First try to see if the field is an array declaration
		 */
		if (fieldType instanceof Class && fieldClass.isArray()) {
			return wrap(fieldClass.getComponentType().getCanonicalName());
		}

		if (fieldType instanceof GenericArrayType) {
			Type elementType = ((GenericArrayType) fieldType).getGenericComponentType();
			if (elementType instanceof Class) {
				return ((Class<?>) elementType).getCanonicalName();
			} else {
				return Decoder.UNDEFINED;
			}
		}

		/*
		 * Verify if the class of the field is one of the supported
		 * collections and get the element type
		 */

		for (Class<?> supportedCollection : supportedCollections) {
			if (supportedCollection.equals(fieldClass)) {
				Class<?> element = getSingleTypeArgument(fieldType);
				return element != null ? wrap(element.getCanonicalName()) : Decoder.UNDEFINED;
			}
		}

		/*
		 * If it is not an array or one of the supported collections just
		 * return null
		 */
		return null;

	}

	/**
	 * If the type of the specified field is one of the supported
	 * collections returns the type of the elements in the collection,
	 * otherwise return null.
	 * 
	 * May return {@link CoreParser#UNDEFINED} if field is defined as a
	 * collection but the type of the elements in the collection cannot be
	 * determined.
	 */
	private static String getCollectionType(FieldMetadata field) {
		String fieldType = field.getFieldType();

		if (fieldType.endsWith("[]")) {
			int index = fieldType.indexOf('[');
			return wrap(fieldType.substring(0, index));
		}

		for (Class<?> supportedCollection : supportedCollections) {
			if (supportedCollection.getCanonicalName().equals(fieldType)) {
				return Decoder.UNDEFINED;
			}
		}

		return null;
	}

	/**
	 * If the type of the specified field is one of the supported message
	 * queues returns the type of the message data, otherwise return null.
	 * 
	 * May return {@link CoreParser#UNDEFINED} if the type of the data in
	 * the queue cannot be determined.
	 */
	private static String getMessageType(Field field) {
		Type fieldType = field.getGenericType();
		Class<?> fieldClass = getRawClass(fieldType);

		if (fieldClass == null) {
			return null;
		}

		/*
		 * Verify if the class of the field is one of the supported message
		 * queues and get the element type
		 */
		for (Class<?> supportedMessageQueue : supportedMessageQueues) {
			if (supportedMessageQueue.equals(fieldClass)) {
				Class<?> element = getSingleTypeArgument(fieldType);
				return element != null ? wrap(element.getCanonicalName()) : Decoder.UNDEFINED;
			}
		}

		/*
		 * If it is not one of the supported message queues just return null
		 */
		return null;

	}

	private static String getMessageType(FieldMetadata field) {
		String fieldType = field.getFieldType();

		for (Class<?> supportedMessage : supportedMessageQueues) {
			if (supportedMessage.getCanonicalName().equals(fieldType)) {
				return Decoder.UNDEFINED;
			}
		}

		return null;
	}

	/**
	 * Utility method to get the raw class of a possibly parameterized type
	 */
	private static final Class<?> getRawClass(Type type) {

		if (type instanceof Class) {
			return (Class<?>) type;
		}

		if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		}

		return null;
	}

	/**
	 * Utility method to get the single type argument of a parameterized
	 * type
	 */
	private static final Class<?> getSingleTypeArgument(Type type) {

		if (!(type instanceof ParameterizedType)) {
			return null;
		}

		ParameterizedType parameterizedType = (ParameterizedType) type;
		Type[] arguments = parameterizedType.getActualTypeArguments();

		if ((arguments.length == 1) && (arguments[0] instanceof Class)) {
			return (Class<?>) arguments[0];
		} else {
			return null;
		}
	}

	private static String wrap(String type) {
		Class<?> wrapper = wrappers.get(type);
		return wrapper != null ? wrapper.getCanonicalName() : type;
	}

	@Override
	public String getClassName() {
		return className;
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
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
		}

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
			 * Verify if it is a collection
			 */
			String collectionType = getCollectionType(fieldReflectionMetadata);
			if (collectionType != null) {
				return collectionType != Decoder.UNDEFINED ? new InterfaceReference(collectionType) : new UndefinedReference(new InterfaceReference(fieldName));
			}

			/*
			 * Verify if it is a message
			 */
			String messageType = getMessageType(fieldReflectionMetadata);
			if (messageType != null) {
				return messageType != Decoder.UNDEFINED ? new MessageReference(messageType) : new UndefinedReference(new MessageReference(fieldName));
			}

			/*
			 * Otherwise we consider it as an interface
			 */
			return new InterfaceReference(fieldReflectionMetadata.getType().getCanonicalName());

		}

		/*
		 * Try to use iPojo metadata, less precise specially for generics
		 */
		if (fieldIPojoMetadata != null) {

			/*
			 * Verify if it is a collection
			 */
			String collectionType = getCollectionType(fieldIPojoMetadata);
			if (collectionType != null) {
				return collectionType != Decoder.UNDEFINED ? new InterfaceReference(collectionType) : new UndefinedReference(new InterfaceReference(fieldName));
			}

			/*
			 * Verify if it is a message
			 */
			String messageType = getMessageType(fieldIPojoMetadata);
			if (messageType != null) {
				return messageType != Decoder.UNDEFINED ? new MessageReference(messageType) : new UndefinedReference(new MessageReference(fieldName));
			}

			/*
			 * Otherwise we consider it as an interface
			 */
			return new InterfaceReference(fieldIPojoMetadata.getFieldType());
		}

		throw new NoSuchFieldException("unavailable field " + fieldName);

	}

	@Override
	public int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException {

		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {
				return method.getMethodArguments().length;
			}
		}

		if (instrumentedCode != null) {
			for (Method method : includeInherited ? instrumentedCode.getMethods() : instrumentedCode.getDeclaredMethods()) {

				if (!method.getName().equals(methodName)) {
					continue;
				}

				return method.getParameterTypes().length;
			}
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException {

		MethodMetadata methodIPojoMetadata = null;
		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {

				String arguments[] = method.getMethodArguments();
				boolean match = (1 == arguments.length);
				if (match) {
					methodIPojoMetadata = method;
				}
			}
		}

		Method methodReflectionMetadata = null;
		if (instrumentedCode != null) {
			for (Method method : includeInherited ? instrumentedCode.getMethods() : instrumentedCode.getDeclaredMethods()) {

				if (!method.getName().equals(methodName)) {
					continue;
				}

				Class<?> parameters[] = method.getParameterTypes();
				boolean match = (1 == parameters.length);

				if (match) {
					methodReflectionMetadata = method;
				}
			}
		}

		if (methodReflectionMetadata != null) {
			return wrap(methodReflectionMetadata.getParameterTypes()[0].getCanonicalName());
		}

		if (methodIPojoMetadata != null) {
			return wrap(methodIPojoMetadata.getMethodArguments()[0]);
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);

	}

	@Override
	public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {

		List<String> signature = new ArrayList<String>();

		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {

				for (String argument : method.getMethodArguments()) {
					signature.add(wrap(argument));
				}

				return signature.toArray(new String[0]);
			}
		}

		if (instrumentedCode != null) {
			for (Method method : includeInherited ? instrumentedCode.getMethods() : instrumentedCode.getDeclaredMethods()) {

				if (!method.getName().equals(methodName)) {
					continue;
				}

				for (Class<?> parameterType : method.getParameterTypes()) {
					signature.add(wrap(parameterType.getCanonicalName()));
				}

				return signature.toArray(new String[0]);
			}
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodReturnType(String methodName, String methodSignature, boolean includeInherited) throws NoSuchMethodException {

		MethodMetadata methodIPojoMetadata = null;
		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {

				if (methodSignature == null) {
					methodIPojoMetadata = method;
					break;
				}

				String signature[] = methodSignature.split(",");
				String arguments[] = method.getMethodArguments();
				boolean match = (signature.length == arguments.length);

				for (int i = 0; match && i < signature.length; i++) {
					if (!signature[i].equals(arguments[i])) {
						match = false;
					}
				}

				if (match) {
					methodIPojoMetadata = method;
					break;
				}
			}
		}

		Method methodReflectionMetadata = null;
		if (instrumentedCode != null) {
			for (Method method : includeInherited ? instrumentedCode.getMethods() : instrumentedCode.getDeclaredMethods()) {

				if (!method.getName().equals(methodName)) {
					continue;
				}

				if (methodSignature == null) {
					methodReflectionMetadata = method;
					break;
				}

				String signature[] = methodSignature.split(",");
				Class<?> parameters[] = method.getParameterTypes();
				boolean match = (signature.length == parameters.length);

				for (int i = 0; match && i < signature.length; i++) {
					if (!FieldMetadata.getReflectionType(signature[i]).equals(parameters[i].getName())) {
						match = false;
					}
				}

				if (match) {
					methodReflectionMetadata = method;
					break;
				}
			}
		}

		if (methodReflectionMetadata != null) {
			return wrap(methodReflectionMetadata.getReturnType().getCanonicalName());
		}

		if (methodIPojoMetadata != null) {
			return wrap(methodIPojoMetadata.getMethodReturn());
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName + "(" + methodSignature != null ? methodSignature : "" + ")");

	}

	/**
	 * Get the cardinality of the field from the instrumented metadata
	 */
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
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
		}

		if (fieldReflectionMetadata != null) {
			return getCollectionType(fieldReflectionMetadata) != null;
		}

		if (fieldIPojoMetadata != null) {
			return getCollectionType(fieldIPojoMetadata) != null;
		}

		throw new NoSuchFieldException("unavailable metadata for field " + fieldName);

	}

}