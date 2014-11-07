package fr.imag.adele.apam.declarations.encoding.ipojo;

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

import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
import fr.imag.adele.apam.declarations.instrumentation.LoadedClassMetadata;

/**
 * A utility class to obtain information about declared fields and methods.
 * 
 * It tries to use java reflection metadata if available, otherwise it falls backs to use
 * the iPojo metadata
 * 
 * @author vega
 * 
 */
public class InstrumentedClassMetadata implements InstrumentedClass {

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
	private final LoadedClassMetadata loadedClass;

	public InstrumentedClassMetadata(String className, PojoMetadata pojoMetadata, Class<?> instrumentedCode) {
		this.className 			= className;
		this.pojoMetadata 		= pojoMetadata;
		this.loadedClass 		= instrumentedCode != null ? new LoadedClassMetadata(instrumentedCode) : null;
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


	private static String getMessageType(FieldMetadata field) {
		String fieldType = field.getFieldType();

		for (Class<?> supportedMessage : supportedMessageQueues) {
			if (supportedMessage.getCanonicalName().equals(fieldType)) {
				return UNKNOWN_TYPE;
			}
		}

		return null;
	}

	private static String boxed(String type) {
		Class<?> boxed = box.get(type);
		return boxed != null ? boxed.getCanonicalName() : type;
	}

	@Override
	public String getName() {
		return className;
	}

    @Override
	public String getDeclaredFieldType(String fieldName) throws NoSuchFieldException {

		/*
		 * Try to get reflection information if available,.
		 */
		String result = null;
		if (loadedClass != null) {
			try {
				result = loadedClass.getDeclaredFieldType(fieldName);
			} catch (Exception e) {
			}
		}

		if (result != null) {
			return result;
		}

		/*
		 * Get iPojo metadata
		 */
		FieldMetadata fieldIPojoMetadata = null;
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
		}

		if (fieldIPojoMetadata != null) {
			return fieldIPojoMetadata.getFieldType();
		}

		throw new NoSuchFieldException("unavailable field " + fieldName);

    }
    
	@Override
	public String getFieldType(String fieldName) throws NoSuchFieldException {

		/*
		 * Try to get reflection information if available,.
		 */
		String result = null;
		if (loadedClass != null) {
			try {
				result = loadedClass.getFieldType(fieldName);
			} catch (Exception e) {
			}
		}

		if (result != null) {
			return result;
		}

		/*
		 * Get iPojo metadata
		 */
		FieldMetadata fieldIPojoMetadata = null;
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
		}

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

		/*
		 * Try to get reflection information if available,.
		 */
		if (loadedClass != null) {
			try {
				return loadedClass.getMethodParameterNumber(methodName,includeInherited);
			} catch (Exception e) {
			}
		}

		/*
		 * Try to use iPojo metadata
		 */
		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {
				return method.getMethodArguments().length;
			}
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException {

		/*
		 * Try to get reflection information if available,.
		 */
		String result = null;
		if (loadedClass != null) {
			try {
				result = loadedClass.getMethodParameterType(methodName,includeInherited);
			} catch (Exception e) {
			}
		}

		if (result != null) {
			return result;
		}

		/*
		 * Try to use iPojo metadata
		 */

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


		if (methodIPojoMetadata != null) {
			return boxed(methodIPojoMetadata.getMethodArguments()[0]);
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);

	}

	@Override
	public String[] getMethodParameterTypes(String methodName, boolean includeInherited) throws NoSuchMethodException {

		/*
		 * Try to get reflection information if available,.
		 */

		String[] result = null;
		if (loadedClass != null) {
			try {
				result = loadedClass.getMethodParameterTypes(methodName,includeInherited);
			} catch (Exception e) {
			}
		}

		if (result != null) {
			return result;
		}
		
		/*
		 * Try to use iPojo metadata
		 */
		
		List<String> signature = new ArrayList<String>();

		if (pojoMetadata != null) {
			for (MethodMetadata method : pojoMetadata.getMethods(methodName)) {

				for (String argument : method.getMethodArguments()) {
					signature.add(boxed(argument));
				}

				return signature.toArray(new String[0]);
			}
		}

		throw new NoSuchMethodException("unavailable metadata for method " + methodName);
	}

	@Override
	public String getMethodReturnType(String methodName, String methodSignature, boolean includeInherited) throws NoSuchMethodException {

		/*
		 * Try to get reflection information if available,.
		 */

		String result = null;
		if (loadedClass != null) {
			try {
				result = loadedClass.getMethodReturnType(methodName,methodSignature,includeInherited);
			} catch (Exception e) {
			}
		}

		if (result != null) {
			return result;
		}

		/*
		 * Try to use iPojo metadata
		 */
		
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

		if (loadedClass != null) {
			try {
				return loadedClass.isCollectionField(fieldName);
			} catch (Exception e) {
			}
		}
		
		/*
		 * Get iPojo metadata
		 */
		FieldMetadata fieldIPojoMetadata = null;
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
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

		if (loadedClass != null) {
			try {
				return loadedClass.isMessageQueueField(fieldName);
			} catch (Exception e) {
			}
		}

		/*
		 * Get iPojo metadata
		 */
		FieldMetadata fieldIPojoMetadata = null;
		if ((pojoMetadata != null) && (pojoMetadata.getField(fieldName) != null)) {
			fieldIPojoMetadata = pojoMetadata.getField(fieldName);
		}

		if (fieldIPojoMetadata != null) {
			return getMessageType(fieldIPojoMetadata) != null;
		}

		throw new NoSuchFieldException("unavailable metadata for field " + fieldName);
	}
}