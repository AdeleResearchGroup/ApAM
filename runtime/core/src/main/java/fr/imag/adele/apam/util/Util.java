/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;

/**
 * The static Class Util provides a set of static method for the iPOJO service concrete machine.
 *
 * @author SAM team
 */
public final class Util {
	private static Logger logger = LoggerFactory.getLogger(Util.class);
	private static boolean        failed;

	private Util() {
	};

	public static List<ComponentDeclaration> getComponents(Element root) {
		Util.failed = false;
		CoreParser parser = new CoreMetadataParser(root);
		return parser.getDeclarations(new ErrorHandler() {

			@Override
			public void error(Severity severity, String message) {
				logger.error("error parsing component declaration : " + message);
				Util.failed = true;
			}
		});
	}

	public static boolean getFailedParsing() {
		return Util.failed;
	}

	/**
	 * takes a list of string "A" "B" "C" ... and produces "{A, B, C, ...}"
	 *
	 * @param names
	 * @return
	 */
	public static String toStringResources(Set<String> names) {
		if ((names == null) || (names.size() == 0)) return null;

		StringBuffer ret = new StringBuffer () ;
		ret.append("{");
		for (String name : names) {
			ret.append(name + ", ");
		}
		return ret.toString().substring(0, ret.length() - 2) + "}";
	}

	public static String toStringArrayString(String[] names) {
		return toStringResources(new HashSet<String>(Arrays.asList(names)));
	}

	public static Set<Filter> toFilter(Set<String> filterString) {
		Set<Filter> filters = new HashSet<Filter>();
		if (filterString == null)
			return filters;

		for (String f : filterString) {
			ApamFilter filter = ApamFilter.newInstance(f) ;
			if (filter != null) {
				filters.add(filter);
			}
		}
		return filters;
	}

	public static List<Filter> toFilterList(List<String> filterString) {
		List<Filter> filters = new ArrayList<Filter>();
		if (filterString == null) {
			return filters;
		}

		for (String f : filterString) {
			ApamFilter filter = ApamFilter.newInstance(f) ;
			if (filter != null) {
				filters.add(filter);
			}
		}
		return filters;
	}

	/**
	 * Warning: returns an unmodifiable List !
	 *
	 * @param str
	 * @return
	 */
	public static List<String> splitList(String str) {
		return Arrays.asList(Util.split(str));
	}

	public static Set<String> splitSet(String str) {
		return new HashSet<String>(Arrays.asList(Util.split(str)));
	}

	/**
	 * Provided a string contain a list of values, return an array of string containing the different values.
	 * A list is of the form "{A, B, .... G}" or simply "A, B, .... G"
	 * Ignores spaces around commas, and around braces.
	 *
	 * If the string is empty or empty list ("{}"), return the empty array.
	 *
	 * @param str
	 * @return a string array. Never null, but can be length 0
	 */
	public static String[] split(String str) {
		if ((str == null) || (str.length() == 0)) {
			return new String[0];
		}

		str = str.trim();
		if  (str.length() == 0){
			return new String[0];
		}

		//It is an explicit set. Remove braces.
		if (str.charAt(0) == '{')  {
			if (str.charAt(str.length() - 1) != '}') {
				logger.error("Invalid string. \"}\" missing: " + str) ;
			}
			str = (str.substring(1, str.length() - 1)).trim();
			//It was an empty set ("{}"
			if (str.length() == 0) {
				return new String[0];
			}
		}

		//It is a simple set of values or a singleton
		return stringArrayTrim(str.split(","));
	}

	//	/**
	//	 * Provided a string contain a list of values, return an array of string containing the different values.
	//	 * A list is of the form "{A, B, .... G}" or "[A, B, .... G]"
	//	 *
	//	 * If the string is empty or is not a list, return the empty array.
	//	 *
	//	 * @param str
	//	 * @return
	//	 */
	//	public static String[] split(String str) {
	//		if ((str == null) || (str.length() == 0)) {
	//			return new String[0];
	//		}
	//
	//		str.trim();
	//		if ((str.charAt(0) != '{') && (str.charAt(0) != '[')) {
	//			return stringArrayTrim(str.split(","));
	//		}
	//
	//		str.replaceAll("\\ ", "");
	//		str.replaceAll(";", ",");
	//		str.replaceAll("\\[,", "[");
	//		str.replaceAll(",]", "]");
	//
	//		String internal;
	//		if (((str.charAt(0) == '{') && (str.charAt(str.length() - 1) == '}'))
	//				|| ((str.charAt(0) == '[') && (str.charAt(str.length() - 1) == ']'))) {
	//
	//			internal = (str.substring(1, str.length() - 1)).trim();
	//			// Check empty array
	//			if (internal.length() == 0) {
	//				return new String[0];
	//			}
	//			return internal.split(",");
	//		} else {
	//			return new String[] { str };
	//		}
	//	}

	/*
	 * return true if expression is null, "true" or if the component matches the expression.
	 */
	public static boolean checkVisibilityExpression(String expre, Component comp) {
		if ((expre == null) || expre.equals(CST.V_TRUE)) {
			return true;
		}
		if (expre.equals(CST.V_FALSE)) {
			return false;
		}
		Filter f = ApamFilter.newInstance(expre);
		if (f == null) {
			return false;
		}
		return comp.match(f);
	}


	public static Set<Implementation> getVisibleImpls (Instance client, Set<Implementation> impls) {
		if (impls == null) {return null ; }

		Set<Implementation> ret = new HashSet <Implementation> () ;
		CompositeType compo = client.getComposite().getCompType() ;
		for (Implementation impl : impls) {
			if (checkImplVisible(compo, impl)) {
				ret.add(impl) ;
			}
		}
		return ret ;
	}

	public static Set<Instance> getVisibleInsts (Instance client, Set<Instance> insts) {
		if (insts == null) {return null ;}

		if(client==null) return insts;

		Set<Instance> ret = new HashSet <Instance> () ;
		Composite compo = client.getComposite() ;
		for (Instance inst : insts) {
			if (checkInstVisible(compo, inst)) {
				ret.add(inst) ;
			}
		}
		return ret ;
	}


	/**
	 * Transforms an array of string in a string list in the Ldap format 
	 * @param value
	 * @return
	 */
	public static String stringArray2String (String[] value) {
		StringBuffer sVal = new StringBuffer () ;
		int lv = ((String[])value).length;
		for (int i=0; i < lv ; i++) {
			sVal.append (((String[])value)[i]) ; 
			if (i < lv -1) {
				sVal.append(", ") ;
			}
		}
		return sVal.toString() ;
	}

	/**
	 * Transforms an array of string in a string list in the Ldap format 
	 * @param value
	 * @return
	 */
	public static String stringSet2String (Set<String> values) {
		StringBuffer sVal = new StringBuffer () ;
		int lv = values.size();
		int i = 0 ;
		for (String val : values) {
			sVal.append (val) ; 
			if (i < lv -1) {
				sVal.append(", ") ;
			}
			i++ ;
		}
		return sVal.toString() ;
	}

	/**
	 * Transforms an atytribute value into a string.
	 * If a set of String, put it into the LDAP form "a, b, c, d"
	 * If Integer, transform in String
	 * @param value
	 * @return
	 */
	@SuppressWarnings ("unchecked")
	public static String toStringAttrValue (Object value) {
		if (value == null)            return null ;
		if (value instanceof Integer) return value.toString() ;
		if (value instanceof Boolean) return value.toString() ;
		if (value instanceof String)  return (String)value ;
		return Util.stringSet2String((Set<String>)value);

	}
	/**
	 * Transforms an array of int in a string list in the Ldap format 
	 * @param value
	 * @return
	 */
	public static String intArray2String (int[] value) {
		StringBuffer sVal = new StringBuffer () ;
		int lv = ((int[])value).length;
		for (int i=0; i < lv ; i++) {
			sVal.append (Integer.toString(((int[])value)[i]) ); 
			if (i < lv -1) {
				sVal.append(", ") ;
			}
		}
		return sVal.toString() ;
	}


	/**
	 * Implementation toImpl is exported if it matches the export clause in at least one of it composite types.
	 * compoFrom can see toImpl if toImpl is visible or if it is in the same composite type. 
	 *
	 * @param compoFrom
	 * @param toImpl
	 * @return
	 */
	public static boolean checkImplVisible(CompositeType compoFrom, Implementation toImpl) {
		if (toImpl.getInCompositeType().isEmpty() || toImpl.getInCompositeType().contains(compoFrom)) {
			return true;
		}

		// First check if toImpl can be imported (borrowed) in compoFrom
		String imports = ((CompositeDeclaration) compoFrom.getDeclaration()).getVisibility().getImportImplementations(); 
		if (Util.checkVisibilityExpression(imports, toImpl) == false) {
			return false;
		}

		// true if at least one composite type that owns toImpl exports it.
		for (CompositeType compoTo : toImpl.getInCompositeType()) {
			if (Util.checkImplVisibleInCompo(compoFrom, toImpl, compoTo)) {
				return true;
			}
		}
		return false;
	}

	public static boolean
	checkImplVisibleInCompo(CompositeType compoFrom, Implementation toImpl, CompositeType compoTo) {
		if (compoFrom == compoTo) {
			return true;
		}
		String exports = ((CompositeDeclaration) compoTo.getDeclaration()).getVisibility().getExportImplementations();
		return Util.checkVisibilityExpression(exports, toImpl) ;
	}


	/**
	 * Instance toInst is visible from composite compoFrom if :
	 * compoFrom can import toInst AND
	 * 		toInst is inside compoFrom or 
	 * 		toInst is in same appli than compoFrom or
	 * 		toInst is global.
	 *
	 * @param fromCompo
	 * @param toInst
	 * @return
	 */
	public static boolean checkInstVisible(Composite fromCompo, Instance toInst) {
		Composite toCompo = toInst.getComposite();
		CompositeType fromCompoType = fromCompo.getCompType();
		CompositeType toCompoType   = toInst.getComposite().getCompType();

		if (fromCompo == toCompo) {
			return true;
		}

		// First check if toInst can be imported by fromCompo
		String imports = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getImportInstances();
		if (!Util.checkVisibilityExpression(imports, toInst)) {
			return false;
		}

		//exported ?
		String exports = ((CompositeDeclaration) toCompoType.getDeclaration()).getVisibility().getExportInstances();
		if (Util.checkVisibilityExpression(exports, toInst)) {
			return true;
		}

		//exportApp ?
		if (fromCompo.getAppliComposite() == toCompo.getAppliComposite()) {
			String appli = ((CompositeDeclaration) toCompoType.getDeclaration()).getVisibility()
					.getApplicationInstances();
			if ((appli != null) && Util.checkVisibilityExpression(appli, toInst)) {
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

	public static boolean isFinalAttribute(String attr) {
		for (String pred : CST.finalAttributes) {
			if (pred.equals(attr)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isReservedAttributePrefix(String attr) {
		for (String prefix : CST.reservedPrefix) {
			if (attr.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if attribute "attr" is valid when set on object "inst".
	 * inst can be an instance, an implementation or a specification.
	 * Check if the value is consistent with the type.
	 * All predefined attributes are Ok (scope ...)
	 * Cannot be a reserved attribute
	 */
	public static boolean validAttr(String component, String attr) {

		if (Util.isFinalAttribute(attr)) {
			logger.error("ERROR: in " + component + ", attribute\"" + attr + "\" is final");
			return false;
		}

		if (Util.isReservedAttributePrefix(attr)) {
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
	public static boolean validAttrType (String type) {
		type = type.trim() ;
		if ((type == null) || (type.isEmpty())) {
			logger.error("Invalid empty property type ") ;
			return false;
		}

		if (type.charAt(0)=='{' ) {
			type = type.substring(1, type.length()-1) ;	
		}

		Set<String> enumVals = Util.splitSet(type);
		if (enumVals == null || enumVals.size()==0) {
			logger.error("Invalid empty property type ") ;
			return false;
		}

		if (enumVals.size()> 1) return true ;
		type = enumVals.iterator().next() ;

		if (type==null || !(type.equals("string") || type.equals("int") || type.equals("boolean") || type.charAt(0)=='{' )) {
			logger.error("Invalid type " + type + ". Supported: string, int, boolean, enumeration; and sets");
			return false ;
		}
		return true ;
	}


	/**
	 * Check if the attribute value is valid; if so return the value to put into the object property Map.
	 * 			In the Map, values are String, Integer or Set<String> for all sets.
	 * Type can be a singleton "int", "boolean" or "string" or enumeration "v1, v2, v3 ..."
	 * 				or a set of these : "{int}", or "{string}" or enumeration "{v1, v2, v3 ...}"

	 * Parameter "value" can be String, Integer, Boolean, Set<Integer> or Set<String>.
	 * 		If the value is String, it is checked and transformed into the Map type.
	 * 		If it is an Object, it is checked and transformed into the Set.
	 * 
	 * If the attribute in not declared or the value invalid,return null.
	 * If valid, return the Map value (String or int).
	 */
	@SuppressWarnings("unchecked")
	public static Object  checkAttrType (String attribute, Object value, String type) {
		if ((type == null) || (value == null) || type.isEmpty() ||  attribute==null || attribute.isEmpty()) {
			logger.error("Invalid property " + attribute + " = " + value + " type=" + type) ;
			return null;
		}

		boolean isSet = false ;
		if (type.charAt(0)=='{' ) {
			isSet = true ;
			type = type.substring(1, type.length()-1) ;	
			type = type.trim() ;
		}

		if (value instanceof String) 
			return checkAttrTypeString (attribute, (String)value, type, isSet) ;

		/*
		 * integers. They are stored as Integer if singleton, as Set<String> for sets<String> or Set<Integer>
		 */
		if (type.equals ("int")) {
			if (isSet) {
				//Value MUST be a Set of Integer
				if ( !(value instanceof  Set<?>)) {
					logger.error("Attribute value " + value + " not an a Set<Integer> for attribute " + attribute) ;
					return false ;
				}
				Set <String> valSetInt = new HashSet<String> () ;
				try {
					for (Object i : (Set<?>)value) {
						if (i instanceof Integer) {
							valSetInt.add(Integer.toString((Integer)i)) ;
						}
						else {
							if (i instanceof String) {
								//to be sure it is an integer
								Integer.valueOf((String)i) ;
								valSetInt.add((String)i) ;
							}
						}
					}
				} catch (Exception e) {
					logger.error("In attribute value " + value +  " is not an Integer Set, for attribute " + attribute) ;
					return false ;
				}
				return valSetInt ;
			}
			//A singleton
			if (value instanceof Integer)
				return value ;
			logger.error("Invalid integer value " + value +  " for attribute " + attribute) ;
			return false ;
		}
		//			return ((Integer)value).intValue()  	;	


		/*
		 * Booleans
		 */
		if (type.equals("boolean")) {
			if (isSet) {
				logger.error("Set of booleans are not alowed" ) ;
				return null ;
			}
			if (value instanceof Boolean) {
				return value;
			}
			logger.error("Invalid value: not a Boolean " + value + " for attribute " + attribute) ;
			return null ;
		}


		/*
		 * array of String or array of enumerated
		 * Array of string in all cases
		 */
		if (!isSet) {
			logger.error("Invalid value: not a single String " + value + " for attribute " + attribute) ;			
			return null ;
		}
		if (! (value instanceof Set)) {
			logger.error("Invalid value: not a Set of String " + value + " for attribute " + attribute) ;
			return null ;
		}

		for (Object i : (Set<?>)value) {
			if (! (i instanceof String)) {
				logger.error("In attribute value " + value + ", " + i.toString () + " is not a String, for attribute " + attribute) ;
				return false ;
			}
		}


		/*
		 * String array
		 */
		if (type.equals("string")) {
			return value ;
		}

		/*
		 * It is an enumeration.
		 * 
		 * a set of enumeration
		 * Compute all values in type.
		 * Check if all values are in type.
		 */
		Set<String> enumType = Util.splitSet(type) ;
		if (enumType.containsAll((Set<String>)value)) {
			return value ;
		} else {
			logger.error("Invalid value " + value + " for attribut " + attribute + ". Expected subset of " + type) ;
			return false ;
		}
	}


	/**
	 * only string, int, boolean and enumerations attributes are accepted.
	 * Return the value if it is correct.
	 * For "int" returns an Integer object, otherwise it is the string "value"
	 * does not work for set of integer, treated as string set. Matching may fail.
	 *
	 * @param value : a singleton, or a set "a, b, c, ...."
	 * @param type : a singleton "int", "boolean" or "string" or enumeration "v1, v2, v3 ..."
	 * 				or a set of these : "{int}", "{boolean}" or "{string}" or enumeration "{v1, v2, v3 ...}"
	 */
	private static Object checkAttrTypeString (String attr, String value, String types, boolean isSet) {

		Set<String> enumVals = Util.splitSet(types);
		if (enumVals == null || enumVals.size()==0) {
			logger.error("invalid type \"" + types  + "\" for attribute \"" + attr);
			return null ;
		}
		String type = enumVals.iterator().next() ;

		Set<String> values   = Util.splitSet(value);		
		if (values.size() > 1 && !isSet) {
			logger.error("Values are a set \"" + values  + "\" for attribute \"" + attr
					+ "\". while type is singleton: \"" + types + "\"");
			return null ;
		}


		/*
		 * Type is an enumeration with at least 2 values
		 */
		if (enumVals.size() > 1) {
			if (enumVals.containsAll(values)) {
				return values;
			}
			logger.error( "Invalid attribute value(s) \"" + value + "\" for attribute \"" + attr
					+ "\".  Expected subset of: " + types);
			return null;
		}

		/*
		 * Type is a singleton : it must be only "string", "int", "boolean" 
		 * 		but value can still be a set
		 */
		if (type.equals("boolean")) {
			
			try {
				if (!isSet) {
					return Boolean.valueOf(value);
				}
				//unfortunately, match does not recognizes a set of booleans.
				//return the list as a string  ;
				Set <String> normalizedValues = new HashSet<String> () ;
				for (String val : values) {
					normalizedValues.add(Boolean.toString(Boolean.parseBoolean(val))) ;
				}
				return normalizedValues ;
			} catch (Exception e) {
				logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr
						+ "\".  Boolean value(s) expected");
				return null;
			}
		}

		if (type.equals("int")) {
			try {
				if (!isSet) {
					return Integer.valueOf(value);
				}
				//unfortunately, match does not recognizes a set of integer.
				//return the list as a string  ;
				Set <String> normalizedValues = new HashSet<String> () ;
				for (String val : values) {
					normalizedValues.add(Integer.toString(Integer.parseInt(val))) ;
				}
				return normalizedValues ;
			} catch (Exception e) {
				logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr
						+ "\".  Integer value(s) expected");
				return null;
			}
		}

		if (!type.equals("string")) {
			logger.error("Invalid attribute type \"" + type + "\" for attribute \"" + attr
					+ "\".  int, boolean or string expected");
			return null ;
		}
		//All values are Ok for string.
		return isSet? values : value ;
		//		}

		//		//Type is an enumeration with at least 2 values
		//		if (enumVals.containsAll(values)) {
		//			return value;
		//		}
		//
		//		String errorMes = "Invalid attribute value(s) \"" + value + "\" for attribute \"" + attr
		//				+ "\".  Expected subset of: " + types;
		//		logger.error(errorMes);
		//		return null;
	}



	public static String[] stringArrayTrim(String[] strings) {
		String[] ret = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			ret[i] = strings[i].trim();
		}
		return ret;
	}

	public static String toStringSetReference(Set<? extends ResourceReference> setRef) {
		StringBuffer ret = new StringBuffer () ;
		ret.append("{");
		for (ResourceReference ref : setRef) {
			ret.append(ref.getJavaType() + ", ");
		}
		String rets = ret.toString() ;
		int i = rets.lastIndexOf(',');
		return rets.substring(0, i) + "}";
	}

	public static boolean checkFilters(Set<String> filters, List<String> listFilters, Map<String, String> validAttr,
			String comp) {
		boolean ok = true;
		if (filters != null) {
			for (String f : filters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null || !parsedFilter.validateAttr(validAttr, f, comp)) {
					ok = false;
				}
			}
		}
		if (listFilters != null) {
			for (String f : listFilters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null || !parsedFilter.validateAttr(validAttr, f, comp)) {
					ok = false;
				}
			}
		}
		return ok;
	}

	public static void printFileToConsole(URL path) throws IOException {
		DataInputStream in = null ;
		BufferedReader br = null ;
		try {
			in = new DataInputStream(path.openStream());
			br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				System.out.println(strLine);
			}
		} catch (Exception e) {// Catch exception if any
		} finally {
			// Close the input stream in all cases
			if (in != null) in.close();
			if (br != null) br.close();
		}
	}


	public static Set<Instance> getSharableInsts(Instance client, Set<Instance> validInsts) {
		if (validInsts == null) return null ;
		Set<Instance> ret = new HashSet<Instance> () ;

		for (Instance inst : validInsts) {
			if (inst.isSharable()) {
				ret.add(inst) ;
			}
		}
		return ret ;
	}

	public static String toStringUndefinedResource(Set<UndefinedReference> undefinedReferences) {
		if ((undefinedReferences == null) || (undefinedReferences.size() == 0)) {
			return null;
		}
		StringBuffer ret = new StringBuffer() ; 
		ret.append("{");
		for (UndefinedReference undfinedReference : undefinedReferences) {
			ret.append(undfinedReference.getSubject() + ", ");
		}
		return ret.substring(0, ret.length() - 2) + "}";
	}

}