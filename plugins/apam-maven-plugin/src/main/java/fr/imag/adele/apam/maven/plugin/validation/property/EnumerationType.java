package fr.imag.adele.apam.maven.plugin.validation.property;

import java.util.HashSet;
import java.util.Set;

/**
 * Create a restriction from a finite set of constant values.
 * 
 */
public class EnumerationType extends RestrictionType {

	private final Set<Object> acceptedValues;
	
	protected EnumerationType(Type baseType, String ... values) {
		
		super(baseType, getName((Object[])values));

		this.acceptedValues	= new HashSet<Object>();

		for (String value : values) {
			Object converted = baseType.value(value);
			if (converted != null)
				acceptedValues.add(converted);
		}
	}
		
	
	protected EnumerationType(Type baseType, Object ... values) {
		super(baseType, getName(values));
		
		this.acceptedValues	= new HashSet<Object>();

		for (Object value : values) {
			if (baseType.isValue(value)) {
				acceptedValues.add(value);
			}
		}
	}

	@Override
	public final boolean isAccepted(Object value) {
		return acceptedValues.contains(value);
	}

	@Override
	public boolean isAssignableTo(Type type) {
		
		if (super.isAssignableTo(type))
			return true;
		
		/*
		 * Consider additionally the case the type is an enumeration that includes this one
		 */
		boolean included = false;
		if (type instanceof EnumerationType) {
			EnumerationType that = (EnumerationType) type;
			included = this.baseType.equals(that.baseType) && that.acceptedValues.containsAll(this.acceptedValues);
		}
		
		return included;
	}
	
	@Override
	public int hashCode() {
		return baseType.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		
		if (object == null)
			return false;
		
		if ( !(object instanceof EnumerationType))
			return false;
		
		EnumerationType that = (EnumerationType) object;
		return this.baseType.equals(that.baseType) && this.acceptedValues.equals(that.acceptedValues);
	}
	
	/**
	 * Compute the name of the restriction as a list of values
	 */
	private static String getName(Object ... values) {
		StringBuilder name = new StringBuilder();
		boolean first = true;
		for (Object value : values) {
			
			if (!first)
				name.append(",");
			
			name.append(value.toString());
			first = false;
		}
		
		return name.toString();
	}

}