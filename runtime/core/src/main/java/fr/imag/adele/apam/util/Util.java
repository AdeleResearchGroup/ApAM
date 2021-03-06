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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;

/**
 * The static Class Util provides a set of static method for the iPOJO service
 * concrete machine.
 * 
 * @author SAM team
 */
public final class Util {
	private static Logger logger = LoggerFactory.getLogger(Util.class);

	public static Set<Instance> getSharableInsts(Instance client, Set<Instance> validInsts) {
		if (validInsts == null) {
			return null;
		}
		Set<Instance> ret = new HashSet<Instance>();

		for (Instance inst : validInsts) {
			if (inst.isSharable()) {
				ret.add(inst);
			}
		}
		return ret;
	};

	/*
	 * =============== String manipulation =============//
	 */

	/**
	 * Transforms an array of int in a string list in the Ldap format
	 * 
	 * @param value
	 * @return
	 */
	public static String intArray2String(int[] value) {
		StringBuffer sVal = new StringBuffer();
		int lv = value.length;
		for (int i = 0; i < lv; i++) {
			sVal.append(Integer.toString(value[i]));
			if (i < lv - 1) {
				sVal.append(", ");
			}
		}
		return sVal.toString();
	}

	public static void printFileToConsole(URL path) throws IOException {
		DataInputStream in = null;
		BufferedReader br = null;
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
			if (in != null) {
				in.close();
			}
			if (br != null) {
				br.close();
			}
		}
	}

	/**
	 * Provided a string contain a list of values, return an array of string
	 * containing the different values. A list is of the form "{A, B, .... G}"
	 * or simply "A, B, .... G" Ignores spaces around commas, and around braces.
	 * 
	 * If the string is empty or empty list ("{}"), return the empty array.
	 * 
	 * @param str
	 * @return a string array. Never null, but can be length 0
	 */
	public static String[] split(String str) {
		List<String> split = splitList(str); 
		return split.toArray(new String[split.size()]);
	}

	/**
	 * Warning: returns an unmodifiable List !
	 * 
	 * @param str
	 * @return
	 */
	public static List<String> splitList(String str) {
		if (str == null || str.isEmpty()) {
			return Collections.emptyList();
		}

		str = str.trim();
		if (str.isEmpty()) {
			return Collections.emptyList();
		}

		// It is an explicit set. Remove braces.
		if (str.charAt(0) == '{') {
			if (str.charAt(str.length() - 1) != '}') {
				logger.error("Invalid string. \"}\" missing: " + str);
			}
			str = (str.substring(1, str.length() - 1)).trim();
			// It was an empty set ("{}"
			if (str.isEmpty()) {
				return Collections.emptyList();
			}
		}

		String[] elements	= str.split(",");
		List<String> result = new ArrayList<String>(elements.length);
		
		for (String element : elements) {
			if (element != null &&  !element.trim().isEmpty()) {
				result.add(element.trim());
			}
		}

		return result;
	}

	public static Set<String> splitSet(String str) {
		return new HashSet<String>(Util.splitList(str));
	}

	/**
	 * Transforms an array of string in a string list in the Ldap format
	 * 
	 * @param value
	 * @return
	 */
	public static String stringArray2String(String[] value) {
		StringBuffer sVal = new StringBuffer();
		int lv = value.length;
		for (int i = 0; i < lv; i++) {
			sVal.append(value[i]);
			if (i < lv - 1) {
				sVal.append(", ");
			}
		}
		return sVal.toString();
	}


	/**
	 * Transforms an array of string in a string list in the Ldap format
	 * 
	 * @param value
	 * @return
	 */
	public static String stringSet2String(Set<String> values) {
		StringBuffer sVal = new StringBuffer();
		int lv = values.size();
		int i = 0;
		for (String val : values) {
			sVal.append(val);
			if (i < lv - 1) {
				sVal.append(", ");
			}
			i++;
		}
		return sVal.toString();
	}

	public static String toStringArrayString(String[] names) {
		return toStringResources(new HashSet<String>(Arrays.asList(names)));
	}

	/**
	 * Transforms an atytribute value into a string. If a set of String, put it
	 * into the LDAP form "a, b, c, d" If Integer, transform in String
	 * 
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String toStringAttrValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return value.toString();
		}
		if (value instanceof Float) {
			return value.toString();
		}		
		if (value instanceof Version) {
			return value.toString();
		}		
		if (value instanceof Boolean) {
			return value.toString();
		}
		if (value instanceof String) {
			return (String) value;
		}
		return Util.stringSet2String((Set<String>) value);

	}

	/**
	 * takes a list of string "A" "B" "C" ... and produces "{A, B, C, ...}"
	 * 
	 * @param names
	 * @return
	 */
	public static String toStringResources(Set<String> names) {
		if ((names == null) || (names.size() == 0)) {
			return null;
		}

		StringBuffer ret = new StringBuffer();
		ret.append("{");
		for (String name : names) {
			ret.append(name + ", ");
		}
		return ret.toString().substring(0, ret.length() - 2) + "}";
	}

	public static String list(Set<? extends ResourceReference> references) {
		return list(references,false);
	}

	public static String list(Set<? extends ResourceReference> references, boolean delimited) {
		
		boolean first 		= true;
		StringBuffer result = new StringBuffer();
		
		if (delimited) result.append("{");
		
		for (ResourceReference reference : references) {
			
			if (!first)
				result.append(", ");
			
			result.append(reference.getJavaType());
			first = false;
		}
		
		if (delimited) result.append("}");
		
		return result.toString();
	}


	// cannot be instantiated
	private Util() {
	}

}