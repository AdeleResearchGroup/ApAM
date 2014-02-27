package fr.imag.adele.apam;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.util.Util;

public class AttrType {
	private final static Logger logger = LoggerFactory.getLogger(AttrType.class);

	public static final int INTEGER = 0;
	public static final int STRING = 1;
	public static final int BOOLEAN = 2;
	public static final int ENUM = 3;
	public static final int FLOAT = 4;
	public static final int VERSION = 5;
	
	public boolean isSet = false;
	public int type;
	public String typeString;
	// private String singletonType ;
	public Set<String> enumValues = null;

	public AttrType(String typeString) {
		if (typeString == null || typeString.isEmpty()) {
			return;
		}
		this.typeString = typeString;
		typeString = typeString.trim();

		if (typeString.charAt(0) == '{') {
			isSet = true;
			typeString = typeString.substring(1, typeString.length() - 1);
			typeString = typeString.trim();
		}

		/*
		 * check if type is correct. It can be either an enumeration
		 * "a, b, c, ...", or "int", "integer", "string", "boolean", "float" or "version"
		 */

		if (typeString.indexOf(',') != -1) {
			type = ENUM;
			enumValues = Util.splitSet(typeString);
			return;
		}

		if (typeString.equalsIgnoreCase("int") || typeString.equalsIgnoreCase("integer")) {
			type = INTEGER;
			return;
		}

		if (typeString.equalsIgnoreCase("boolean")) {
			type = BOOLEAN;
			return;
		}

		if (typeString.equalsIgnoreCase("string")) {
			type = STRING;
			return;
		}
		
		if (typeString.equalsIgnoreCase("float")) {
			type = FLOAT;
			return;
		}	
		
		if (typeString.equalsIgnoreCase("version")) {
			type = VERSION;
			return;
		}			
		
		logger.error("Invalid type " + typeString + ". Expected enumeration, string, integer, boolean, float, version.");
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof AttrType)) {
			return false;
		}
		AttrType type2 = (AttrType) object;
		if (isSet != type2.isSet) {
			return false;
		}
		if (type != type2.type) {
			return false;
		}
		if (type == ENUM) {
			if (enumValues.size() != type2.enumValues.size()) {
				return false;
			}
			for (String val : enumValues) {
				if (!type2.enumValues.contains(val)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashBase = 14;
		int hashMul = 3;

		hashBase = hashBase * hashMul + (isSet ? 0 : 1);
		hashBase = hashBase * hashMul + type;
		hashBase = hashBase * hashMul + (typeString == null ? 0 : typeString.hashCode());
		hashBase = hashBase * hashMul + (enumValues == null ? 0 : enumValues.hashCode());
		return hashBase;

	}
}
