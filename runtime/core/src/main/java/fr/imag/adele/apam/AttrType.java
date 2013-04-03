package fr.imag.adele.apam;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.util.Util;


public class AttrType {
	private final static Logger	logger = LoggerFactory.getLogger(AttrType.class);

	public static final int INTEGER = 0 ;
	public static final int STRING = 1 ;
	public static final int BOOLEAN = 2 ;
	public static final int ENUM = 3 ;

	public boolean isSet = false ;
	public int type ;
	public String typeString ;
//	private String singletonType ;
	public Set<String> enumValues = null ;

	public AttrType (String typeString) {
		if (typeString == null || typeString.isEmpty())
			return ;
		this.typeString = typeString ;
		typeString = typeString.trim();

		if (typeString.charAt(0)=='{' ) {
			isSet = true ;
			typeString = typeString.substring(1, typeString.length()-1) ;	
			typeString = typeString.trim() ;
		} 

		/*
		 * check if type is correct. It can be either an enumeration "a, b, c, ...", or
		 * "int", "integer", "string", "boolean"
		 */

		if (typeString.indexOf(',') != -1) {
			type = ENUM ;
			enumValues = Util.splitSet(typeString);
			return ;
		}

		if (typeString.equalsIgnoreCase("int") || typeString.equalsIgnoreCase("integer")) {
			type = INTEGER;
			return ;
		}

		if ( typeString.equalsIgnoreCase("boolean")) {
			type = BOOLEAN;
			return ;
		}

		if ( typeString.equalsIgnoreCase("string")) {
			type = STRING;
			return ;
		}
		logger.error ("Invalid type " + typeString + ". Expected enumeration, string, integer, boolean.") ;
	}
	
	//@Override
	public boolean equals (AttrType type2) {
		if (isSet != type2.isSet) return false ;
		if (type != type2.type) return false ;
		if (type==ENUM) {
			if (enumValues.size() != type2.enumValues.size())
				return false ;
			for (String val : enumValues) {
				if (!type2.enumValues.contains(val))
					return false ;
			}
		}
		return true ;
	}
}


