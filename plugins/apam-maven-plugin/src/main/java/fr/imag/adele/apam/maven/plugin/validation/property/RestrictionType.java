package fr.imag.adele.apam.maven.plugin.validation.property;


/**
 * This class represents a type that is a restriction of a base type that allow only
 * values satisfying a predicate 
 */
public abstract class RestrictionType implements Type {

	/**
	 * The name of this type
	 */
	protected final String name;
	
	/**
	 * The base type
	 */
	protected final Type baseType;
	
	
	protected RestrictionType(Type baseType, String name) {
		this.name			= name;
		this.baseType		= baseType;
	}
	
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Determines if the value is accepted for this restriction
	 */
	public abstract boolean isAccepted(Object value);

	@Override
	public boolean isValue(Object value) {
		return baseType.isValue(value) && isAccepted(value);
	}
	

	@Override
	public Object value(String value) {
		return baseType.value(value);
	}

	@Override
	public String toString(Object value) {
		return baseType.toString(value);
	}
	
	@Override
	public boolean isAssignableTo(Type type) {
		
		if (this == type)
			return true;
		
		if (type == ANY)
			return true;
		
		return baseType.isAssignableTo(type);
	}
	
	@Override
	public boolean isAssignableTo(String className) {
		return baseType.isAssignableTo(className);
	}
	
	@Override
	public boolean isAssignableFrom(String className) {
		return baseType.isAssignableFrom(className);
	}

	
	@Override
	public String toString() {
		return getName();
	} 
}