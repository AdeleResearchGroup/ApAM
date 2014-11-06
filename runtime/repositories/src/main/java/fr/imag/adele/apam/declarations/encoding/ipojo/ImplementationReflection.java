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

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;

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
	 * Utility method to get the associated box class name for a primitive type
	 */

	private final static Map<String, Class<?>> box = new HashMap<String, Class<?>>();

	static {
		box.put(Boolean.TYPE.getName(), Boolean.class);
		box.put(Character.TYPE.getName(), Character.class);
		box.put(Byte.TYPE.getName(), Byte.class);
		box.put(Short.TYPE.getName(), Short.class);
		box.put(Integer.TYPE.getName(), Integer.class);
		box.put(Float.TYPE.getName(), Float.class);
		box.put(Long.TYPE.getName(), Long.class);
		box.put(Double.TYPE.getName(), Double.class);
	}

	/**
	 * If the type of the specified field is one of the supported collections returns the type of the elements
	 * in the collection, otherwise return null.
	 * 
	 * May return {@link #UNKNOWN_TYPE} if the type of the elements in the collection cannot be determined.
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
			return boxed(fieldClass.getComponentType().getCanonicalName());
		}

		if (fieldType instanceof GenericArrayType) {
			Type elementType = ((GenericArrayType) fieldType).getGenericComponentType();
			if (elementType instanceof Class) {
				return ((Class<?>) elementType).getCanonicalName();
			} else {
				return UNKNOWN_TYPE;
			}
		}

		/*
		 * Verify if the class of the field is one of the supported collections and get the element type
		 */

		for (Class<?> supportedCollection : supportedCollections) {
			if (supportedCollection.equals(fieldClass)) {
				Class<?> element = getSingleTypeArgument(fieldType);
				return element != null ? boxed(element.getCanonicalName()) : UNKNOWN_TYPE;
			}
		}

		/*
		 * If it is not an array or one of the supported collections just return null
		 */
		return null;

	}

	/**
	 * If the type of the specified field is one of the supported collections returns the type of the elements
	 * in the collection, otherwise return null.
	 * 
	 * May return {@link UNKNOWN_TYPE} if the type of the elements in the collection cannot be determined.
	 */
	private static String getCollectionType(FieldMetadata field) {
		String fieldType = field.getFieldType();

		if (fieldType.endsWith("[]")) {
			int index = fieldType.indexOf('[');
			return boxed(fieldType.substring(0, index));
		}

		for (Class<?> supportedCollection : supportedCollections) {
			if (supportedCollection.getCanonicalName().equals(fieldType)) {
				return UNKNOWN_TYPE;
			}
		}

		return null;
	}

	/**
	 * If the type of the specified field is one of the supported message queues returns the type of the message
	 * data, otherwise return null.
	 * 
	 * May return {@link UNKNOWN_TYPE} if the type of the data in the queue cannot be determined.
	 */
	private static String getMessageType(Field field) {
		Type fieldType = field.getGenericType();
		Class<?> fieldClass = getRawClass(fieldType);

		if (fieldClass == null) {
			return null;
		}

		/*
		 * Verify if the class of the field is one of the supported message queues and get the element type
		 */
		for (Class<?> supportedMessageQueue : supportedMessageQueues) {
			if (supportedMessageQueue.equals(fieldClass)) {
				Class<?> element = getSingleTypeArgument(fieldType);
				return element != null ? boxed(element.getCanonicalName()) : UNKNOWN_TYPE;
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
				return UNKNOWN_TYPE;
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
	 * Utility method to get the single type argument of a parameterized type
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

	private static String boxed(String type) {
		Class<?> boxed = box.get(type);
		return boxed != null ? boxed.getCanonicalName() : type;
	}

	@Override
	public String getClassName() {
		return className;
	}

    @Override
	public String getDeclaredFieldType(String fieldName) throws NoSuchFieldException {
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
			return fieldReflectionMetadata.getType().getCanonicalName();
		}

		/*
		 * Try to use iPojo metadata
		 */
		if (fieldIPojoMetadata != null) {
			return fieldIPojoMetadata.getFieldType();
		}

		throw new NoSuchFieldException("unavailable field " + fieldName);

    }
    
	@Override
	public String getFieldType(String fieldName) throws NoSuchFieldException {

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
				return collectionType;
			}

			/*
			 * Verify if it is a message
			 */
			String messageType = getMessageType(fieldReflectionMetadata);
			if (messageType != null) {
				return messageType;
			}

			/*
			 * Otherwise we just get the raw type
			 */
			return fieldReflectionMetadata.getType().getCanonicalName();

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
				return collectionType;
			}

			/*
			 * Verify if it is a message
			 */
			String messageType = getMessageType(fieldIPojoMetadata);
			if (messageType != null) {
				return messageType;
			}

			/*
			 * Otherwise we just return the raw type
			 */
			return fieldIPojoMetadata.getFieldType();
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
			return boxed(methodReflectionMetadata.getParameterTypes()[0].getCanonicalName());
		}

		if (methodIPojoMetadata != null) {
			return boxed(methodIPojoMetadata.getMethodArguments()[0]);
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);

	}

	@Override
	public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {

		List<String> signature = new ArrayList<String>();

		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {

				for (String argument : method.getMethodArguments()) {
					signature.add(boxed(argument));
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
					signature.add(boxed(parameterType.getCanonicalName()));
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
			return boxed(methodReflectionMetadata.getReturnType().getCanonicalName());
		}

		if (methodIPojoMetadata != null) {
			return boxed(methodIPojoMetadata.getMethodReturn());
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName + "(" + methodSignature != null ? methodSignature : "" + ")");

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

	@Override
	public boolean isMessageQueueField(String fieldName) throws NoSuchFieldException {
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
			return getMessageType(fieldReflectionMetadata) != null;
		}

		if (fieldIPojoMetadata != null) {
			return getMessageType(fieldIPojoMetadata) != null;
		}

		throw new NoSuchFieldException("unavailable metadata for field " + fieldName);
	}
}