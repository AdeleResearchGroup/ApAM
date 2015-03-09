package fr.imag.adele.apam.maven.plugin.validation.property;

import java.util.HashMap;
import java.util.Map;

import fr.imag.adele.apam.util.Util;

/**
 * This class parses a property type specification
 * 
 * @author vega
 *
 */
public class TypeParser {

	private final Map<String,String> alias;	
	
	public TypeParser() {
		
		/*
		 * initialize alias table 
		 */
		this.alias = new HashMap<String, String>();
		
		alias.put("int", "integer");
		alias.put("bool","boolean");
	}
	
	/**
	 * Get the type corresponding to the given type name.
	 * 
	 * Return null if the type is invalid.
	 * 
	 */
	public Type parse(String type) {
		
		if (type == null || type.trim().isEmpty()) {
			return null;
		}
		
		type = type.trim();
		
		/*
		 * if it is a collection
		 */
		if (type.startsWith("{")) {
			
			if (! type.endsWith("}"))
				return null;
			
			String element 		= type.substring(1, type.length() - 1);
			Type elementType	= parse(element);

			/*
			 * Create the collection
			 * 
			 * TODO NOTE Notice that all collections store their values as strings. This is
			 * due to a restriction in ApamFilter when matching collections. We should have
			 * a better implementation of Filter that takes advantage of the typing of the
			 * properties.
			 *  
			 */
			if (elementType != null)
				return new CollectionType(elementType,true);
			else
				return null;
			
		}

		/*
		 * It is an enumeration of a base type
		 */
		if (type.indexOf("[") != -1) {
			
			if (! type.endsWith("]"))
				return null;
			
			int startValues		= type.indexOf("[");
			String base 		= type.substring(0, startValues);
			String accepted		= type.substring(startValues+1, type.length() - 1);
			
			/*
			 * Get the base type
			 */
			Type baseType		= parse(base);
			if (baseType == null)
				return null;

			/*
			 * Validate values
			 * 
			 */
			String[] acceptedElements	= Util.split(accepted);
			for (String acceptedElement : acceptedElements) {
				if (baseType.value(acceptedElement) == null)
					return null;
			}

			/*
			 * Create the enumeration
			 */
			return new EnumerationType(baseType, (Object[])acceptedElements);
		}

		/*
		 * It is an enumeration of strings
		 */
		if (type.indexOf(",") != -1) {
			String[] acceptedElements	= Util.split(type);
			return new EnumerationType(PrimitiveType.STRING, (Object[])acceptedElements);
		}
		
		/*
		 * Check if it is a primitive type
		 */
		if (alias.get(type) != null)
			type = alias.get(type);
		
		for (Type primitive : PrimitiveType.values()) {
			if (type.equalsIgnoreCase(primitive.getName())) {
				return primitive;
			}
		}
		
		/*
		 * check the predefined bottom and top types
		 */
		
		if (type.equalsIgnoreCase(Type.ANY.getName())) {
			return Type.ANY;
		}

		if (type.equalsIgnoreCase(Type.NONE.getName())) {
			return Type.NONE;
		}
		
		return null;
	}
	

}
