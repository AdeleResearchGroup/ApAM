package fr.imag.adele.apam.maven.plugin.validation.property;

/**
 * This class is used to represent the type of a property.
 * 
 * It handles validation and conversion of values.
 *  
 * TODO We should change APAM core to use this class and unify treatment of property
 * types at build and runtime
 * 
 * @author vega
 *
 */
public interface Type {

	/**
	 * The name of the type
	 */
	public String getName();

	/**
	 * Whether the specified value is valid for this type
	 * 
	 * NOTE this does not consider cast or string values, this is the basic validation when no
	 * conversion is performed
	 */
	public boolean isValue(Object value);

	/**
	 * Convert a string to a value of this type
	 * 
	 * Return null if the value is not valid.
	 */
	public Object value(String value);

	/**
	 * converts a value of this type to an string
	 */
	public String toString(Object value);
	
	/**
	 * Determines if values of this type can be assigned to a property of the specified type
	 * 
	 * NOTE this defines a binary relation that induces a partial order over the set of types
	 * that is used for some simple type inferences. To allow easy extensibility of the type
	 * system, it is the responsibility of new type constructors to define this method. 
	 * 
	 * To be well defined, implementors must ensure that the following expressions are always
	 * true :
	 * 
	 * 		this.equals(that) implies this.isAssignableTo(that)
	 * 		NONE.isAssignableTo(this)
	 *      this.isAssignableTo(ANY)
	 *      
	 */
	public boolean isAssignableTo(Type type);
	
	/**
	 * Determines if a value of this type can be assigned to a Java field or argument of the 
	 * specified class
	 */
	public boolean isAssignableTo(String className);
	
	/**
	 * Determines if a value of the specified class can be assigned to a property of this type
	 * 
	 * NOTE this does not consider automatic casts or string values, this is the basic validation
	 * when no conversion is performed
	 */
	public boolean isAssignableFrom(String className);
	
	/**
	 * This type represents the type with no values, this is the bottom type
	 */
	public static Type NONE = new Type() {

		@Override
		public String getName() {
			return "NONE";
		}

		@Override
		public boolean isValue(Object value) {
			return false;
		}

		@Override
		public Object value(String value) {
			return null;
		}

		@Override
		public String toString(Object value) {
			throw new UnsupportedOperationException("There shold be no values of this type");
		}
		
		@Override
		public boolean isAssignableTo(Type type) {
			return true;
		}

		@Override
		public boolean isAssignableTo(String className) {
			return false;
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return false;
		}

	};
	
	/**
	 * This type represents the type that accepts all values, this is the top type
	 */
	public static Type ANY = new Type() {

		@Override
		public String getName() {
			return "ANY";
		}

		@Override
		public boolean isValue(Object value) {
			return true;
		}

		@Override
		public Object value(String value) {
			return value;
		}

		@Override
		public String toString(Object value) {
			return value.toString();
		}
		
		@Override
		public boolean isAssignableTo(Type type) {
			return false;
		}

		@Override
		public boolean isAssignableTo(String className) {
			return className.equals(Object.class.getCanonicalName());
		}

		@Override
		public boolean isAssignableFrom(String className) {
			return true;
		}
		
	};


}
