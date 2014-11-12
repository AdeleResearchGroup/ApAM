package fr.imag.adele.apam.maven.plugin.validation.property;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.util.Util;

/**
 * This class represents a type that is a set of values of a single type
 */
public class CollectionType implements Type {

	/**
	 * The name of this type
	 */
	private final String name;
	
	/**
	 * The type of the elements
	 */
	private final Type elementType;
	
	/**
	 * Whether the collection is internally represented as a set of objects or a set of strings
	 */
	private final boolean storeStrings;
	
	public CollectionType(Type elementType, boolean storeStrings) {
		this.name			= "{"+elementType.getName()+"}";
		this.elementType	= elementType;
		this.storeStrings	= storeStrings;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public Type getElementType() {
		return elementType;
	}
	
	@Override
	public boolean isValue(Object value) {
		
		if ( !(value instanceof Set))
			return false;
		
		for (Object element : (Set<?>)value) {
			
			if (storeStrings && ! (element instanceof String)) {
				return false;
			}
			
			if ( !storeStrings && ! elementType.isValue(element))
				return false;
		}
		
		return true;
	}

	@Override
	public Object value(String value) {
		
		Set<Object> conversion = new HashSet<Object>();
		
		/*
		 * Convert elements
		 */
		for (String element : Util.splitList(value)) {
			
			Object converted = elementType.value(element);
			if (converted == null) {
				return null;
			}
		
			conversion.add(storeStrings ? element : converted);
		}
		
		return conversion;
	}

	@Override
	public String toString(Object value) {
		StringBuilder result = new StringBuilder();
		
		result.append("{");
		boolean first = true;
		
		for (Object element : (Set<?>)value ) {
			
			if ( !first) {
				result.append(",");
			}
			
			if (storeStrings) {
				result.append((String) element);
			}
			else {
				result.append(elementType.toString(element));
			}
			
			first = false;
		}
		
		result.append("}");

		return result.toString();
	}
	
	@Override
	public boolean isAssignableTo(String className) {
		return Mapping.isAssignableFrom(className,Set.class);
	}

	@Override
	public boolean isAssignableFrom(String className) {
		return Mapping.isAssignableFrom(Set.class,className);
	}
	
	@Override
	public int hashCode() {
		return elementType.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		
		if (this == object)
			return true;
		
		if (object == null)
			return false;
		
		if ( ! (object instanceof CollectionType))
			return false;
		
		CollectionType that = (CollectionType) object;
		
		return this.elementType.equals(that.elementType) && this.storeStrings == that.storeStrings;
	}

	@Override
	public boolean isAssignableTo(Type type) {
		
		if (this == type)
			return true;
		
		if (type == ANY)
			return true;
		
		if ( ! (type instanceof CollectionType))
			return false;
		
		CollectionType that = (CollectionType) type;
		
		return (this.elementType.isAssignableTo(that.elementType) && this.storeStrings == that.storeStrings) ||
			   (this.storeStrings && PrimitiveType.STRING.isAssignableTo(that.elementType));
	}
	
	@Override
	public String toString() {
		return getName();
	}

}