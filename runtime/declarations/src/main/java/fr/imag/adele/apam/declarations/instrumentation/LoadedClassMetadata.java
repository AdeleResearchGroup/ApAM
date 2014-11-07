package fr.imag.adele.apam.declarations.instrumentation;

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


import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;

/**
 * A utility class to obtain information about declared fields and methods.
 * 
 * @author vega
 * 
 */
public class LoadedClassMetadata implements InstrumentedClass {

	/**
	 * The loaded class
	 */
	private final Class<?> loadedClass;

	public LoadedClassMetadata(Class<?> loadedClass) {
		this.loadedClass = loadedClass;
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
	public String getName() {
		return loadedClass.getCanonicalName();
	}

    @Override
	public String getDeclaredFieldType(String fieldName) throws NoSuchFieldException {

		/*
		 * Try to get reflection information if available,.
		 */
		Field fieldReflectionMetadata = null;
		try {
			fieldReflectionMetadata = loadedClass.getDeclaredField(fieldName);
		} catch (Exception e) {
		}

		/*
		 * Try to use reflection information
		 */
		if (fieldReflectionMetadata != null) {
			return fieldReflectionMetadata.getType().getCanonicalName();
		}


		throw new NoSuchFieldException("unavailable field " + fieldName);
    }
    
	@Override
	public String getFieldType(String fieldName) throws NoSuchFieldException {

		/*
		 * Try to get reflection information if available,.
		 */
		Field fieldReflectionMetadata = null;
		try {
			fieldReflectionMetadata = loadedClass.getDeclaredField(fieldName);
		} catch (Exception e) {
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


		throw new NoSuchFieldException("unavailable field " + fieldName);

	}

	@Override
	public int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException {

		for (Method method : includeInherited ? loadedClass.getMethods() : loadedClass.getDeclaredMethods()) {

			if (!method.getName().equals(methodName)) {
				continue;
			}

			return method.getParameterTypes().length;
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException {

		Method methodReflectionMetadata = null;
		for (Method method : includeInherited ? loadedClass.getMethods() : loadedClass.getDeclaredMethods()) {

			if (!method.getName().equals(methodName)) {
				continue;
			}

			Class<?> parameters[] = method.getParameterTypes();
			boolean match = (1 == parameters.length);

			if (match) {
				methodReflectionMetadata = method;
			}
		}

		if (methodReflectionMetadata != null) {
			return boxed(methodReflectionMetadata.getParameterTypes()[0].getCanonicalName());
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);

	}

	@Override
	public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {

		List<String> signature = new ArrayList<String>();

		for (Method method : includeInherited ? loadedClass.getMethods() : loadedClass.getDeclaredMethods()) {

			if (!method.getName().equals(methodName)) {
				continue;
			}

			for (Class<?> parameterType : method.getParameterTypes()) {
				signature.add(boxed(parameterType.getCanonicalName()));
			}

			return signature.toArray(new String[0]);
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodReturnType(String methodName, String methodSignature, boolean includeInherited) throws NoSuchMethodException {


		Method methodReflectionMetadata = null;
		for (Method method : includeInherited ? loadedClass.getMethods() : loadedClass.getDeclaredMethods()) {

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
				if (!signature[i].equals(parameters[i].getCanonicalName())) {
					match = false;
				}
			}

			if (match) {
				methodReflectionMetadata = method;
				break;
			}
		}

		if (methodReflectionMetadata != null) {
			return boxed(methodReflectionMetadata.getReturnType().getCanonicalName());
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName + "(" + methodSignature != null ? methodSignature : "" + ")");

	}

	@Override
	public boolean isCollectionField(String fieldName) throws NoSuchFieldException {

		/*
		 * Try to get reflection information if available,.
		 */
		Field fieldReflectionMetadata = null;
		try {
			fieldReflectionMetadata = loadedClass.getDeclaredField(fieldName);
		} catch (Exception ignored) {
		}

		if (fieldReflectionMetadata != null) {
			return getCollectionType(fieldReflectionMetadata) != null;
		}

		throw new NoSuchFieldException("unavailable metadata for field " + fieldName);
	}

	@Override
	public boolean isMessageQueueField(String fieldName) throws NoSuchFieldException {
		/*
		 * Try to get reflection information if available,.
		 */
		Field fieldReflectionMetadata = null;
		try {
			fieldReflectionMetadata = loadedClass.getDeclaredField(fieldName);
		} catch (Exception ignored) {
		}

		if (fieldReflectionMetadata != null) {
			return getMessageType(fieldReflectionMetadata) != null;
		}

		throw new NoSuchFieldException("unavailable metadata for field " + fieldName);
	}
}