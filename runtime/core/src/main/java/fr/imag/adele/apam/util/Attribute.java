package fr.imag.adele.apam.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.CST;

public class Attribute {

	/*
	 * ================== Attributes ===================//
	 */
	private static Logger logger = LoggerFactory.getLogger(Attribute.class);

	/**
	 * Check if the attribute value is valid; if so return the value to put into
	 * the object property Map. In the Map, values are String, Integer or
	 * Set<String> for all sets. Type can be a singleton "int", "boolean" or
	 * "string" or enumeration "v1, v2, v3 ..." or a set of these : "{int}", or
	 * "{string}" or enumeration "{v1, v2, v3 ...}"
	 * 
	 * Parameter "value" can be String, Integer, Boolean, Set<Integer> or
	 * Set<String>. If the value is String, it is checked and transformed into
	 * the Map type. If it is an Object, it is checked and transformed into the
	 * Set.
	 * 
	 * Type ca be a substitution, in which case type is prefixed bt "$" : $int,
	 * $boolean, .. ${int} ... Value for a substitution must be a string with at
	 * least a "$"
	 * 
	 * If the attribute in not declared or the value invalid,return null. If
	 * valid, return the Map value (String or int).
	 */
	@SuppressWarnings("unchecked")
	public static Object checkAttrType(String attribute, Object value, String typeString) {
		if ((typeString == null) || (value == null) || typeString.isEmpty() || attribute == null || attribute.isEmpty()) {
			logger.error("Invalid property " + attribute + " = " + value + " type=" + typeString);
			return null;
		}

		AttrType aType = splitType(typeString);
		if (aType == null) {
			return null;
		}

		if (value instanceof String) {
			if (Substitute.isSubstitution(value)) {
				return (value);
			}
			// It is not a substitution, with a string as value
			return checkAttrTypeString(attribute, (String) value, aType);
		}

		/*
		 * integers. They are stored as Integer if singleton, as Set<String> for
		 * sets<String> or Set<Integer>
		 */
		if (aType.type == AttrType.INTEGER) { // aType.singletonType.equals
			// ("integer")) {
			if (aType.isSet) {
				// Value MUST be a Set of Integer
				if (!(value instanceof Set<?>)) {
					logger.error("Attribute value " + value + " not an a Set<Integer> for attribute " + attribute);
					return false;
				}
				Set<String> valSetInt = new HashSet<String>();
				try {
					for (Object i : (Set<?>) value) {
						if (i instanceof Integer) {
							valSetInt.add(Integer.toString((Integer) i));
						} else {
							if (i instanceof String) {
								// to be sure it is an integer
								Integer.valueOf((String) i);
								valSetInt.add((String) i);
							}
						}
					}
				} catch (Exception e) {
					logger.error("In attribute value " + value + " is not an Integer Set, for attribute " + attribute);
					return false;
				}
				return Collections.unmodifiableSet(valSetInt);
			}
			// A singleton
			if (value instanceof Integer) {
				return value;
			}
			logger.error("Invalid integer value " + value + " for attribute " + attribute);
			return false;
		}

		/*
		 * Booleans
		 */
		if (aType.type == AttrType.BOOLEAN) {
			if (aType.isSet) {
				logger.error("Set of booleans are not alowed");
				return null;
			}
			if (value instanceof Boolean) {
				return value;
			}
			logger.error("Invalid value: not a Boolean " + value + " for attribute " + attribute);
			return null;
		}

		/*
		 * array of String or array of enumerated. Array of string in all cases
		 */
		if (!aType.isSet) {
			logger.error("Invalid value: not a single String " + value + " for attribute " + attribute);
			return null;
		}
		if (!(value instanceof Set)) {
			logger.error("Invalid value: not a Set of String " + value + " for attribute " + attribute);
			return null;
		}

		for (Object i : (Set<?>) value) {
			if (!(i instanceof String)) {
				logger.error("In attribute value " + value + ", " + i.toString() + " is not a String, for attribute " + attribute);
				return false;
			}
		}

		/*
		 * String array
		 */
		if (aType.type == AttrType.STRING) {
			return Collections.unmodifiableSet((Set<String>) value);
		}

		/*
		 * It is a set of enumeration Compute all values in type. Check if all
		 * values are in type.
		 */
		// Set<String> enumType = Util.splitSet(type) ;
		if (aType.enumValues.containsAll((Set<String>) value)) {
			return Collections.unmodifiableSet((Set<String>) value);
		} else {
			logger.error("Invalid value " + value + " for attribut " + attribute + ". Expected subset of " + aType.typeString);
			return false;
		}
	};

	/**
	 * only string, int, boolean and enumerations attributes are accepted.
	 * Return the value if it is correct. For "int" returns an Integer object,
	 * otherwise it is the string "value" does not work for set of integer,
	 * treated as string set. Matching may fail.
	 * 
	 * @param value
	 *            : a singleton, or a set "a, b, c, ...."
	 * @param type
	 *            : a singleton "int", "boolean" or "string" or enumeration
	 *            "v1, v2, v3 ..." or a set of these : "{int}", "{boolean}" or
	 *            "{string}" or enumeration "{v1, v2, v3 ...}"
	 */
	private static Object checkAttrTypeString(String attr, String value, AttrType at) {

		Set<String> values = Collections.unmodifiableSet(Util.splitSet(value));
		if (values.size() > 1 && !at.isSet) {
			logger.error("Values are a set \"" + values + "\" for attribute \"" + attr + "\". while type is singleton: \"" + at.typeString + "\"");
			return null;
		}

		switch (at.type) {
		case AttrType.ENUM:
			if (at.isSet) {
				if (at.enumValues.containsAll(values)) {
					return values;
				}
				logger.error("Invalid attribute value(s) \"" + value + "\" for attribute \"" + attr + "\".  Expected subset of: " + Util.stringSet2String(at.enumValues));
				return null;
			}
			if (at.enumValues.contains(value)) {
				return value;
			}
			logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr + "\".  Expected a member of: " + Util.stringSet2String(at.enumValues));
			return null;

		case AttrType.BOOLEAN:
			// if (type.equals("boolean")) {
			try {
				if (!at.isSet) {
					return Boolean.valueOf(value);
				}
				logger.error("Boolean set are not allowed. Attribute \"" + attr);
				return null;
			} catch (Exception e) {
				logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr + "\".  Boolean value(s) expected");
				return null;
			}
		case AttrType.INTEGER:
			// if (type.equals("integer")) {
			try {
				if (!at.isSet) {
					return Integer.valueOf(value);
				}
				// unfortunately, match does not recognizes a set of integer.
				// return the list as a string ;
				Set<String> normalizedValues = new HashSet<String>();
				for (String val : values) {
					normalizedValues.add(Integer.toString(Integer.parseInt(val)));
				}
				return normalizedValues;
			} catch (Exception e) {
				logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr + "\".  Integer value(s) expected");
				return null;
			}
		case AttrType.STRING:
			// if (type.equals("string")) {
			// All values are Ok for string.
			return at.isSet ? values : value;
		}

		return null;
	}

	public static boolean isFinalAttribute(String attr) {
		for (String pred : CST.finalAttributes) {
			if (pred.equals(attr)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isInheritedAttribute(String attr) {
		if (isReservedAttributePrefix(attr)) {
			return false;
		}
		for (String pred : CST.notInheritedAttribute) {
			if (pred.equals(attr)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isReservedAttributePrefix(String attr) {
		for (String prefix : CST.reservedPrefix) {
			if (attr.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	public static AttrType splitType(String type) {
		return new AttrType(type);
	}

	/**
	 * Check if attribute "attr" is valid when set on object "inst". inst can be
	 * an instance, an implementation or a specification. Check if the value is
	 * consistent with the type. All predefined attributes are Ok (scope ...)
	 * Cannot be a reserved attribute
	 */
	public static boolean validAttr(String component, String attr) {

		if (isFinalAttribute(attr)) {
			logger.error("ERROR: in " + component + ", attribute\"" + attr + "\" is final");
			return false;
		}

		if (isReservedAttributePrefix(attr)) {
			logger.error("ERROR: in " + component + ", attribute\"" + attr + "\" is reserved");
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public static boolean validAttrType(String type) {
		type = type.trim();
		if ((type == null) || (type.isEmpty())) {
			logger.error("Invalid empty property type ");
			return false;
		}

		if (type.charAt(0) == '{') {
			type = type.substring(1, type.length() - 1);
		}

		Set<String> enumVals = Util.splitSet(type);
		if (enumVals == null || enumVals.size() == 0) {
			logger.error("Invalid empty property type ");
			return false;
		}

		if (enumVals.size() > 1) {
			return true;
		}
		type = enumVals.iterator().next();

		if (type == null || !(type.equals("string") || type.equals("int") || type.equals("boolean") || type.charAt(0) == '{')) {
			logger.error("Invalid type " + type + ". Supported: string, int, boolean, enumeration; and sets");
			return false;
		}
		return true;
	}

	// cannot be instantiated
	private Attribute() {
	}

}
