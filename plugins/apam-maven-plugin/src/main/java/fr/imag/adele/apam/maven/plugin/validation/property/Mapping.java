package fr.imag.adele.apam.maven.plugin.validation.property;

/**
 * This class groups a number of utility methods useful to define mappings between APAM properties and
 * Java classes
 * 
 * @author vega
 *
 */
public class Mapping {

	/**
	 * Verifies if the specified parameter class is assignable from the receiver class (represented by its name).
	 * 
	 * NOTE because the receiver class is not loaded, we can not use reflection to perform this calculation
	 * 
	 * see {@link Class#isAssignableFrom(Class)}
	 */
	public static boolean isAssignableFrom(String receiver, Class<?> clazz) {
		
		if (receiver.equals(clazz.getCanonicalName())) {
			return true;
		}
		
		Class<?> superClass = clazz.getSuperclass(); 
		if (superClass != null && isAssignableFrom(receiver, superClass)) {
			return true;
		}

		for (Class<?> implemented : clazz.getInterfaces()) {
			if (isAssignableFrom(receiver, implemented))
				return true;
		}
		
		return false;
	}

	/**
	 * Verifies if the specified parameter class (represented by its name) is assignable from the receiver class.
	 * 
	 * NOTE because the specified class class is not loaded,  we can not use reflection to perform this calculation
	 * and we can only verify it has the exact same type. In principle, the specified class can be a subclass of 
	 * the receiver, but this can not be validated without loading the class. 
	 * 
	 * see {@link Class#isAssignableFrom(Class)}
	 */
	public static boolean isAssignableFrom(Class<?> receiver, String clazz) {
		return clazz.equals(receiver.getCanonicalName());
	}

}
