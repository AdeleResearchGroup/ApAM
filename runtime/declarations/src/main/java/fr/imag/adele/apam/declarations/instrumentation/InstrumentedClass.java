package fr.imag.adele.apam.declarations.instrumentation;

/**
 * An interface giving access to reflection data associated with the class of an implementation
 * 
 * @author vega
 * 
 */
public interface InstrumentedClass {

	/**
	 * The name of the associated java class
	 */
	String getName();

	/**
	 * Whether the specified java field is one of the supported collections
	 */
	boolean isCollectionField(String fieldName) throws NoSuchFieldException;

	/**
	 * Whether the specified java field is one of the supported message queues
	 */
	boolean isMessageQueueField(String fieldName) throws NoSuchFieldException;

	/**
	 * The type of the specified java field, for collections is the type of the element type
	 */
	String getFieldType(String fieldName) throws NoSuchFieldException;

	/**
	 * The type of the specified java field 
	 */
	String getDeclaredFieldType(String fieldName) throws NoSuchFieldException;
	
	/**
	 * A special type to signal an unknown field type
	 */
	public static final String UNKNOWN_TYPE = new String("<UNKNOWN_TYPE>");
	
	/**
	 * The number of parameters of the specified java method
	 */
	int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException;

	/**
	 * The type of of the specified single-parameter java method
	 */
	String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException;

	/**
	 * The list of parameter types
	 */
	String[] getMethodParameterTypes(String methodName,	boolean includeInherited) throws NoSuchMethodException;

	/**
	 * The type of return of the specified java method
	 */
	String getMethodReturnType(String methodName, String signature,	boolean includeInherited) throws NoSuchMethodException;

}