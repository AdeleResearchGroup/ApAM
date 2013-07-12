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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
//import java.util.Dictionary;
//import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

//import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
//import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Component;


/**
 * This filter implementation is based on the official OSGi filter with additional
 * support for the SUPERSET (&gt;*) and SUBSET (&lt;*) operators.
 * This filter also has a few optimizations (cached transformation).
 */
@SuppressWarnings("rawtypes") 
public class ApamFilter /* implements Filter */ {
	private static Logger logger = LoggerFactory.getLogger(ApamFilter.class);

	/* filter operators */
	public static final int          EQUAL     = 1;
	public static final int          APPROX    = 2;
	public static final int          GREATER   = 3;
	public static final int          LESS      = 4;
	public static final int          PRESENT   = 5;
	public static final int          SUBSTRING = 6;
	public static final int          AND       = 7;
	public static final int          OR        = 8;
	public static final int          NOT       = 9;
	public static final int          SUBSET    = 10;
	public static final int          SUPERSET  = 11;

	/** filter operation */
	public final int                  op;
	/** filter attribute or null if operation AND, OR or NOT */
	public final String               attr;
	/** filter operands */
	public final Object               value;
	/** optim in case of version */
	private final Object              converted;

	/* normalized filter string for Filter object */
	private transient volatile String filterString;


	/**
	 * Constructs a {@link ApamFilter} object. This filter object may be
	 * used to match a {@link org.osgi.framework.ServiceReference} or a Dictionary.
	 *
	 * <p>
	 * If the filter cannot be parsed, an {@link org.osgi.framework.InvalidSyntaxException} will be thrown with a human
	 * readable message where the filter became unparsable.
	 *
	 * @param filterString the filter string.
	 * @exception InvalidSyntaxException If the filter parameter contains an
	 *                invalid filter string that cannot be parsed.
	 */
	public static ApamFilter newInstance(String filterString) {
		try {
			return ApamFilter.newInstance(filterString, true);
		} catch  (InvalidSyntaxException e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	public static ApamFilter newInstanceApam(String filterString, Component component) {
		try {
			return new Parser(filterString, false, component).parse();    		
		} catch (Exception e) {
			logger.error("invalid filter syntax " + filterString) ;
			return null ;
		}
	}

	public static boolean isSubstituteFilter (String filterString, Component component) {
		//Inefficient, but simple. Done once.
		ApamFilter f = newInstanceApam(filterString, component) ;
		ApamFilter f2 = newInstance(filterString);
		if (f==null || f2==null) return false ;
		return !f.equals(f2);
	}
	
	private static ApamFilter newInstance(String filterString, boolean ignoreCase)
			throws InvalidSyntaxException {
		return new Parser(filterString, ignoreCase, null).parse();
	}

	ApamFilter(int operation, String attr, Object value) {
		op = operation;
		this.attr = attr ;
		this.value = value;
		Object conv = null;
		try {
			if ((op == ApamFilter.SUBSET) || (op == ApamFilter.SUPERSET)) {
				conv = getSet(value);
			} else if ("version".equalsIgnoreCase(attr)) {
				if (value instanceof String) {
					conv = Version.parseVersion((String) value);
				} else if (value instanceof Version) {
					conv = value;
				}
			}
		} catch (Throwable t) {
			// Ignore any conversion issue
		}
		converted = conv;
	}

	/**
	 * Filter using a <code>Map</code>. This <code>Filter</code> is
	 * executed using the specified <code>Map</code>'s keys and
	 * values. The keys are case insensitively matched with this <code>Filter</code>.
	 *
	 * @param dictionary The <code>Map</code> whose keys are used in
	 *            the match.
	 * @return <code>true</code> if the <code>Dictionary</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 * @throws IllegalArgumentException If <code>dictionary</code> contains
	 *             case variants of the same key name.
	 *             
	 */
//	@SuppressWarnings("unchecked")
	public boolean match(Map map) {
		return match0(new CaseInsensitiveMap<Object>(map));
	}

	/**
	 * Filter with case sensitivity using a <code>Map</code>. This <code>Filter</code> is executed using the
	 * specified <code>Map</code>'s keys and values. The keys are case 
	 * sensitively matched with this <code>Filter</code>.
	 *
	 * @param map The <code>Map</code> whose keys are used in
	 *            the match.
	 * @return <code>true</code> if the <code>Map</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 * @throws IllegalArgumentException If <code>map</code> contains
	 *             case variants of the same key name.
	 */
	public boolean matchCase(Map map) {
		return match0 (map);
	}

	/**
	 * Returns this <code>Filter</code>'s filter string.
	 * <p>
	 * The filter string is normalized by removing whitespace which does not affect the meaning of the filter.
	 *
	 * @return This <code>Filter</code>'s filter string.
	 */
	@Override
	public String toString() {
		String result = filterString;
		if (result == null) {
			filterString = result = normalize();
		}
		return result;
	}

	/**
	 * Returns this <code>Filter</code>'s normalized filter string.
	 * <p>
	 * The filter string is normalized by removing whitespace which does not affect the meaning of the filter.
	 *
	 * @return This <code>Filter</code>'s filter string.
	 */
	private String normalize() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');

		switch (op) {
		case AND: {
			sb.append('&');

			ApamFilter[] filters = (ApamFilter[]) value;
			for (ApamFilter filter : filters) {
				sb.append(filter.normalize());
			}

			break;
		}

		case OR: {
			sb.append('|');

			ApamFilter[] filters = (ApamFilter[]) value;
			for (ApamFilter filter : filters) {
				sb.append(filter.normalize());
			}

			break;
		}

		case NOT: {
			sb.append('!');
			ApamFilter filter = (ApamFilter) value;
			sb.append(filter.normalize());

			break;
		}

		case SUBSTRING: {
			sb.append(attr);
			sb.append('=');

			String[] substrings = (String[]) value;

			for (String substr : substrings) {
				if (substr == null) /* * */{
					sb.append('*');
				} else /* xxx */{
					sb.append(ApamFilter.encodeValue(substr));
				}
			}

			break;
		}
		case EQUAL: {
			sb.append(attr);
			sb.append('=');
			sb.append(ApamFilter.encodeValue((String) value));

			break;
		}
		case GREATER: {
			sb.append(attr);
			sb.append(">=");
			sb.append(ApamFilter.encodeValue((String) value));

			break;
		}
		case LESS: {
			sb.append(attr);
			sb.append("<=");
			sb.append(ApamFilter.encodeValue((String) value));

			break;
		}
		case APPROX: {
			sb.append(attr);
			sb.append("~=");
			sb.append(ApamFilter.encodeValue(ApamFilter.approxString((String) value)));

			break;
		}
		case PRESENT: {
			sb.append(attr);
			sb.append("=*");

			break;
		}
		case SUBSET: {
			sb.append(attr);
			sb.append("<*");
			sb.append(ApamFilter.encodeValue(ApamFilter.approxString((String) value)));

			break;
		}
		case SUPERSET: {
			sb.append(attr);
			sb.append("*>");
			sb.append(ApamFilter.encodeValue(ApamFilter.approxString((String) value)));

			break;
		}
		}

		sb.append(')');

		return sb.toString();
	}

	/**
	 * Compares this <code>Filter</code> to another <code>Filter</code>.
	 *
	 * <p>
	 * This implementation returns the result of calling <code>this.toString().equals(obj.toString()</code>.
	 *
	 * @param obj The object to compare against this <code>Filter</code>.
	 * @return If the other object is a <code>Filter</code> object, then
	 *         returns the result of calling <code>this.toString().equals(obj.toString()</code>; <code>false</code>
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ApamFilter)) {
			return false;
		}

		return toString().equals(obj.toString());
	}

	/**
	 * Returns the hashCode for this <code>Filter</code>.
	 *
	 * <p>
	 * This implementation returns the result of calling <code>this.toString().hashCode()</code>.
	 *
	 * @return The hashCode of this <code>Filter</code>.
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}


	private boolean match0(Map properties) {

		switch (op) {
		case AND: {
			ApamFilter[] filters = (ApamFilter[]) value;
			for (int i = 0, size = filters.length; i < size; i++) {
				if (!filters[i].match0(properties)) {
					return false;
				}
			}

			return true;
		}

		case OR: {
			ApamFilter[] filters = (ApamFilter[]) value;
			for (ApamFilter filter : filters) {
				if (filter.match0(properties)) {
					return true;
				}
			}

			return false;
		}

		case NOT: {
			ApamFilter filter = (ApamFilter) value;

			return !filter.match0(properties);
		}

		case SUBSTRING:
		case EQUAL:
		case GREATER:
		case LESS:
		case APPROX:
		case SUBSET:
		case SUPERSET: {
			Object prop = (properties == null) ? null : properties
					.get(attr);
			//TODO : checking that it is not a substitution. Only to be sure. Should not happen.
			if ((value instanceof String) && (((String)value).charAt(0)=='$' || ((String)value).charAt(0)=='@')) {
				logger.error("Filter attribute  " +  attr + " is a substitution: " + (String)value + ". in Filter :  " + filterString);
			}

			return compare(op, prop, value);
		}

		case PRESENT: {
			Object prop = (properties == null) ? null : properties
					.get(attr);

			return prop != null;
		}
		}

		return false;
	}

	/**
	 * Encode the value string such that '(', '*', ')' and '\' are escaped.
	 *
	 * @param value unencoded value string.
	 * @return encoded value string.
	 */
	public static String encodeValue(String value) {
		boolean encoded = false;
		int inlen = value.length();
		int outlen = inlen << 1; /* inlen 2 */

		char[] output = new char[outlen];
		value.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];

			switch (c) {
			case '(':
			case '*':
			case ')':
			case '\\': {
				output[cursor] = '\\';
				cursor++;
				encoded = true;

				break;
			}
			}

			output[cursor] = c;
			cursor++;
		}
		return encoded ? new String(output, 0, cursor) : value;
	}

	@SuppressWarnings("unchecked") 
	private Collection getSet(Object value) {
		Collection s;
		if (value instanceof Set) {
			s = (Set) value;
		} else if (value instanceof Collection) {
			s = (Collection) value;
			if (s.size() > 1) {
				s = new HashSet(s);
			}
		} else if (value != null) {
			String v = value.toString();
			if (v.indexOf(',') < 0) {
				s = Collections.singleton(v);
			} else {
				//Knowing this is a collection, they have '{' and '}' as prolog and epilog. Thus they should be removed in order to tokenize it 
				StringTokenizer st = new StringTokenizer(value.toString().replaceAll("[\\{\\}]", ""), ",");
				s = new HashSet();
				while (st.hasMoreTokens()) {
					s.add(st.nextToken().trim());
				}
			}
		} else {
			s = Collections.emptySet();
		}
		return s;
	}

	@SuppressWarnings("unchecked") 
	private boolean compare(int operation, Object value1, Object value2) {

		if ((op == ApamFilter.SUPERSET) || (op == ApamFilter.SUBSET)) {

			Collection s1 = getSet(value1);
			Collection s2 = converted instanceof Collection ? (Collection) converted : getSet(value2);

			if (op == ApamFilter.SUPERSET) {
				return s1.containsAll(s2);
			} else {
				return s2.containsAll(s1);
			}
		}

		if (value1 == null) {
			return false;
		}
		if (value1 instanceof String) {
			return compare_String(operation, (String) value1, value2);
		}

		Class clazz = value1.getClass();
		if (clazz.isArray()) {
			Class type = clazz.getComponentType();
			if (type.isPrimitive()) {
				return compare_PrimitiveArray(operation, type, value1,
						value2);
			}
			return compare_ObjectArray(operation, (Object[]) value1, value2);
		}
		if (value1 instanceof Version) {
			if (converted != null) {
				switch (operation) {
				case APPROX:
				case EQUAL: {
					return ((Version) value1).compareTo(converted) == 0;
				}
				case GREATER: {
					return ((Version) value1).compareTo(converted) >= 0;
				}
				case LESS: {
					return ((Version) value1).compareTo(converted) <= 0;
				}
				}
			} else {
				return compare_Comparable(operation, (Version) value1, value2);
			}
		}
		if (value1 instanceof Collection) {
			return compare_Collection(operation, (Collection) value1,
					value2);
		}
		if (value1 instanceof Integer) {
			return compare_Integer(operation,
					((Integer) value1).intValue(), value2);
		}
		if (value1 instanceof Long) {
			return compare_Long(operation, ((Long) value1).longValue(),
					value2);
		}
		if (value1 instanceof Byte) {
			return compare_Byte(operation, ((Byte) value1).byteValue(),
					value2);
		}
		if (value1 instanceof Short) {
			return compare_Short(operation, ((Short) value1).shortValue(),
					value2);
		}
		if (value1 instanceof Character) {
			return compare_Character(operation, ((Character) value1)
					.charValue(), value2);
		}
		if (value1 instanceof Float) {
			return compare_Float(operation, ((Float) value1).floatValue(),
					value2);
		}
		if (value1 instanceof Double) {
			return compare_Double(operation, ((Double) value1)
					.doubleValue(), value2);
		}
		if (value1 instanceof Boolean) {
			return compare_Boolean(operation, ((Boolean) value1)
					.booleanValue(), value2);
		}
		if (value1 instanceof Comparable) {
			return compare_Comparable(operation, (Comparable) value1,
					value2);
		}
		return compare_Unknown(operation, value1, value2); // RFC 59
	}

	@SuppressWarnings("unchecked") 
	private boolean compare_Collection(int operation,
			Collection collection, Object value2) {
		if ((op == ApamFilter.SUBSET) || (op == ApamFilter.SUPERSET)) {
			Set set = new HashSet();
			if (value2 != null) {
				StringTokenizer st = new StringTokenizer(value2.toString(), ",");
				while (st.hasMoreTokens()) {
					set.add(st.nextToken().trim());
				}
			}
			if (op == ApamFilter.SUBSET) {
				return set.containsAll(collection);
			} else {
				return collection.containsAll(set);
			}
		}
		for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
			if (compare(operation, iterator.next(), value2)) {
				return true;
			}
		}
		return false;
	}

	private boolean compare_ObjectArray(int operation, Object[] array,
			Object value2) {
		for (Object element : array) {
			if (compare(operation, element, value2)) {
				return true;
			}
		}
		return false;
	}

	private boolean compare_PrimitiveArray(int operation, Class type,
			Object primarray, Object value2) {
		if (Integer.TYPE.isAssignableFrom(type)) {
			int[] array = (int[]) primarray;
			for (int element : array) {
				if (compare_Integer(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Long.TYPE.isAssignableFrom(type)) {
			long[] array = (long[]) primarray;
			for (long element : array) {
				if (compare_Long(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Byte.TYPE.isAssignableFrom(type)) {
			byte[] array = (byte[]) primarray;
			for (byte element : array) {
				if (compare_Byte(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Short.TYPE.isAssignableFrom(type)) {
			short[] array = (short[]) primarray;
			for (short element : array) {
				if (compare_Short(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Character.TYPE.isAssignableFrom(type)) {
			char[] array = (char[]) primarray;
			for (char element : array) {
				if (compare_Character(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Float.TYPE.isAssignableFrom(type)) {
			float[] array = (float[]) primarray;
			for (float element : array) {
				if (compare_Float(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Double.TYPE.isAssignableFrom(type)) {
			double[] array = (double[]) primarray;
			for (double element : array) {
				if (compare_Double(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		if (Boolean.TYPE.isAssignableFrom(type)) {
			boolean[] array = (boolean[]) primarray;
			for (boolean element : array) {
				if (compare_Boolean(operation, element, value2)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

	private boolean compare_String(int operation, String string,
			Object value2) {
		switch (operation) {
		case SUBSTRING: {
			String[] substrings = (String[]) value2;
			int pos = 0;
			for (int i = 0, size = substrings.length; i < size; i++) {
				String substr = substrings[i];

				if (i + 1 < size) /* if this is not that last substr */{
					if (substr == null) /* * */{
						String substr2 = substrings[i + 1];

						if (substr2 == null) /* ** */
							continue; /* ignore first star */
						/* xxx */
						int index = string.indexOf(substr2, pos);
						if (index == -1) {
							return false;
						}

						pos = index + substr2.length();
						if (i + 2 < size) // if there are more
							// substrings, increment
							// over the string we just
							// matched; otherwise need
							// to do the last substr
							// check
							i++;
					} else /* xxx */{
						int len = substr.length();
						if (string.regionMatches(pos, substr, 0, len)) {
							pos += len;
						} else {
							return false;
						}
					}
				} else /* last substr */{
					if (substr == null) /* * */{
						return true;
					}
					/* xxx */
					return string.endsWith(substr);
				}
			}

			return true;
		}
		case EQUAL: {
			return string.equals(value2);
		}
		case APPROX: {
			string = ApamFilter.approxString(string);
			String string2 = ApamFilter.approxString((String) value2);

			return string.equalsIgnoreCase(string2);
		}
		case GREATER: {
			return string.compareTo((String) value2) >= 0;
		}
		case LESS: {
			return string.compareTo((String) value2) <= 0;
		}
		}
		return false;
	}

	private boolean compare_Integer(int operation, int intval, Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		int intval2;
		try {
			intval2 = Integer.parseInt(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}
		switch (operation) {
		case APPROX:
		case EQUAL: {
			return intval == intval2;
		}
		case GREATER: {
			return intval >= intval2;
		}
		case LESS: {
			return intval <= intval2;
		}
		}
		return false;
	}

	private boolean compare_Long(int operation, long longval, Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		long longval2;
		try {
			longval2 = Long.parseLong(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return longval == longval2;
		}
		case GREATER: {
			return longval >= longval2;
		}
		case LESS: {
			return longval <= longval2;
		}
		}
		return false;
	}

	private boolean compare_Byte(int operation, byte byteval, Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		byte byteval2;
		try {
			byteval2 = Byte.parseByte(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return byteval == byteval2;
		}
		case GREATER: {
			return byteval >= byteval2;
		}
		case LESS: {
			return byteval <= byteval2;
		}
		}
		return false;
	}

	private boolean compare_Short(int operation, short shortval,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		short shortval2;
		try {
			shortval2 = Short.parseShort(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return shortval == shortval2;
		}
		case GREATER: {
			return shortval >= shortval2;
		}
		case LESS: {
			return shortval <= shortval2;
		}
		}
		return false;
	}

	private boolean compare_Character(int operation, char charval,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		char charval2;
		try {
			charval2 = ((String) value2).charAt(0);
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		switch (operation) {
		case EQUAL: {
			return charval == charval2;
		}
		case APPROX: {
			return (charval == charval2)
					|| (Character.toUpperCase(charval) == Character
					.toUpperCase(charval2))
					|| (Character.toLowerCase(charval) == Character
					.toLowerCase(charval2));
		}
		case GREATER: {
			return charval >= charval2;
		}
		case LESS: {
			return charval <= charval2;
		}
		}
		return false;
	}

	private boolean compare_Boolean(int operation, boolean boolval,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		boolean boolval2 = Boolean.valueOf(((String) value2).trim())
				.booleanValue();
		switch (operation) {
		case APPROX:
		case EQUAL:
		case GREATER:
		case LESS: {
			return boolval == boolval2;
		}
		}
		return false;
	}

	private boolean compare_Float(int operation, float floatval,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		float floatval2;
		try {
			floatval2 = Float.parseFloat(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return Float.compare(floatval, floatval2) == 0;
		}
		case GREATER: {
			return Float.compare(floatval, floatval2) >= 0;
		}
		case LESS: {
			return Float.compare(floatval, floatval2) <= 0;
		}
		}
		return false;
	}

	private boolean compare_Double(int operation, double doubleval,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		double doubleval2;
		try {
			doubleval2 = Double.parseDouble(((String) value2).trim());
		} catch (IllegalArgumentException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return Double.compare(doubleval, doubleval2) == 0;
		}
		case GREATER: {
			return Double.compare(doubleval, doubleval2) >= 0;
		}
		case LESS: {
			return Double.compare(doubleval, doubleval2) <= 0;
		}
		}
		return false;
	}

	private static final Class[] constructorType = new Class[] { String.class };

	@SuppressWarnings("unchecked") 
	private boolean compare_Comparable(int operation, Comparable value1,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		Constructor constructor;
		try {
			constructor = value1.getClass().getConstructor(ApamFilter.constructorType);
		} catch (NoSuchMethodException e) {
			return false;
		}
		try {
			if (!constructor.isAccessible())
				AccessController.doPrivileged(new SetAccessibleAction(
						constructor));
			value2 = constructor
					.newInstance(new Object[] { ((String) value2).trim() });
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		} catch (InstantiationException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL: {
			return value1.compareTo(value2) == 0;
		}
		case GREATER: {
			return value1.compareTo(value2) >= 0;
		}
		case LESS: {
			return value1.compareTo(value2) <= 0;
		}
		}
		return false;
	}

	@SuppressWarnings("unchecked") 
	private boolean compare_Unknown(int operation, Object value1,
			Object value2) {
		if (operation == ApamFilter.SUBSTRING) {
			return false;
		}
		Constructor constructor;
		try {
			constructor = value1.getClass().getConstructor(ApamFilter.constructorType);
		} catch (NoSuchMethodException e) {
			return false;
		}
		try {
			if (!constructor.isAccessible())
				AccessController.doPrivileged(new SetAccessibleAction(
						constructor));
			value2 = constructor
					.newInstance(new Object[] { ((String) value2).trim() });
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		} catch (InstantiationException e) {
			return false;
		}

		switch (operation) {
		case APPROX:
		case EQUAL:
		case GREATER:
		case LESS: {
			return value1.equals(value2);
		}
		}
		return false;
	}

	/**
	 * Map a string for an APPROX (~=) comparison.
	 *
	 * This implementation removes white spaces. This is the minimum
	 * implementation allowed by the OSGi spec.
	 *
	 * @param input Input string.
	 * @return String ready for APPROX comparison.
	 */
	public static String approxString(String input) {
		boolean changed = false;
		char[] output = input.toCharArray();
		int cursor = 0;
		for (int i = 0, length = output.length; i < length; i++) {
			char c = output[i];

			if (Character.isWhitespace(c)) {
				changed = true;
				continue;
			}

			output[cursor] = c;
			cursor++;
		}
		return changed ? new String(output, 0, cursor) : input;
	}

	/**
	 * Parser class for OSGi filter strings. This class parses the complete
	 * filter string and builds a tree of Filter objects rooted at the
	 * parent.
	 */
	private static class Parser {
		private final String  filterstring;
		private final boolean ignoreCase;
		private final char[]  filterChars;
		private int           pos;

		private Component component = null ;


		Parser(String filterstring, boolean ignoreCase, Component component) {
			this.filterstring = filterstring;
			this.ignoreCase = ignoreCase;
			this.component = component ;
			filterChars = filterstring.toCharArray();
			pos = 0;
		}

		ApamFilter parse() throws InvalidSyntaxException {
			ApamFilter filter;
			try {
				filter = parse_filter();
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new InvalidSyntaxException("Filter ended abruptly. Filter : " +  filterstring,
						filterstring);
			}

			if (pos != filterChars.length) {
				throw new InvalidSyntaxException(
						"Extraneous trailing characters: "
								+ filterstring.substring(pos), filterstring);
			}
			return filter;
		}

		private ApamFilter parse_filter() throws InvalidSyntaxException {
			ApamFilter filter;
			skipWhiteSpace();

			if (filterChars[pos] != '(') {
				throw new InvalidSyntaxException("Missing '(': "
						+ filterstring.substring(pos), filterstring);
			}

			pos++;

			filter = parse_filtercomp();

			skipWhiteSpace();

			if (filterChars[pos] != ')') {
				throw new InvalidSyntaxException("Missing ')': "
						+ filterstring.substring(pos), filterstring);
			}

			pos++;

			skipWhiteSpace();

			return filter;
		}

		private ApamFilter parse_filtercomp() throws InvalidSyntaxException {
			skipWhiteSpace();

			char c = filterChars[pos];

			switch (c) {
			case '&': {
				pos++;
				return parse_and();
			}
			case '|': {
				pos++;
				return parse_or();
			}
			case '!': {
				pos++;
				return parse_not();
			}
			}
			return parse_item();
		}

		@SuppressWarnings("unchecked") 
		private ApamFilter parse_and() throws InvalidSyntaxException {
			int lookahead = pos;
			skipWhiteSpace();

			if (filterChars[pos] != '(') {
				pos = lookahead - 1;
				return parse_item();
			}

			List operands = new ArrayList(10);

			while (filterChars[pos] == '(') {
				ApamFilter child = parse_filter();
				operands.add(child);
			}

			return new ApamFilter(ApamFilter.AND, null, operands
					.toArray(new ApamFilter[operands.size()]));
		}

		@SuppressWarnings("unchecked") 
		private ApamFilter parse_or() throws InvalidSyntaxException {
			int lookahead = pos;
			skipWhiteSpace();

			if (filterChars[pos] != '(') {
				pos = lookahead - 1;
				return parse_item();
			}

			List operands = new ArrayList(10);

			while (filterChars[pos] == '(') {
				ApamFilter child = parse_filter();
				operands.add(child);
			}

			return new ApamFilter(ApamFilter.OR, null, operands
					.toArray(new ApamFilter[operands.size()]));
		}

		private ApamFilter parse_not() throws InvalidSyntaxException {
			int lookahead = pos;
			skipWhiteSpace();

			if (filterChars[pos] != '(') {
				pos = lookahead - 1;
				return parse_item();
			}

			ApamFilter child = parse_filter();

			return new ApamFilter(ApamFilter.NOT, null, child);
		}

		private ApamFilter parse_item() throws InvalidSyntaxException {
			String attr = parse_attr();

			skipWhiteSpace();

			switch (filterChars[pos]) {
			case '*': {
				if (filterChars[pos + 1] == '>') {
					pos += 2;
					return new ApamFilter(ApamFilter.SUPERSET, attr,
							parse_value());
				}
				break;
			}
			case '~': {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new ApamFilter(ApamFilter.APPROX, attr,
							parse_value());
				}
				break;
			}
			case '>': {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new ApamFilter(ApamFilter.GREATER, attr,
							parse_value());
				}
				break;
			}
			case '<': {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new ApamFilter(ApamFilter.LESS, attr,
							parse_value());
				}
				if (filterChars[pos + 1] == '*') {
					pos += 2;
					return new ApamFilter(ApamFilter.SUBSET, attr,
							parse_value());
				}
				break;
			}
			case '=': {
				if (filterChars[pos + 1] == '*') {
					int oldpos = pos;
					pos += 2;
					skipWhiteSpace();
					if (filterChars[pos] == ')') {
						return new ApamFilter(ApamFilter.PRESENT, attr,
								null);
					}
					pos = oldpos;
				}

				pos++;
				Object string = parse_substring();

				if (string instanceof String) {
					/*
					 * TODO
					 * If it is a substitution, substitute.
					 */
					string = Util.toStringAttrValue(Substitute.substitute(null, string, component)) ;
					if (string == null) {
						System.err.println("Substitution failed. Attribute not set: " + filterstring);
						string ="Null" ;
					}
					return new ApamFilter(ApamFilter.EQUAL, attr,
							((String) string).trim());
				}
				return new ApamFilter(ApamFilter.SUBSTRING, attr,
						string);
			}
			}

			throw new InvalidSyntaxException("Invalid operator: "
					+ filterstring.substring(pos), filterstring);
		}

		private String parse_attr() throws InvalidSyntaxException {
			skipWhiteSpace();

			int begin = pos;
			int end = pos;

			char c = filterChars[pos];

			while ((c != '~') && (c != '<') && (c != '>') && (c != '=') && (c != '(')
					&& (c != ')')) {

				if ((c == '<') && (filterChars[pos + 1] == '*')) {
					break;
				}
				if ((c == '*') && (filterChars[pos + 1] == '>')) {
					break;
				}
				pos++;

				if (!Character.isWhitespace(c)) {
					end = pos;
				}

				c = filterChars[pos];
			}

			int length = end - begin;

			if (length == 0) {
				throw new InvalidSyntaxException("Invalid syntax in filter: " + filterstring + " Missing attr: "
						+ filterstring.substring(pos), filterstring);
			}

			String str = new String(filterChars, begin, length);
			if (ignoreCase) {
				str = str.toLowerCase();
			}
			return str;
		}

		private String parse_value() throws InvalidSyntaxException {
			StringBuffer sb = new StringBuffer(filterChars.length - pos);

			parseloop: while (true) {
				char c = filterChars[pos];

				switch (c) {
				case ')': {
					break parseloop;
				}

				case '(': {
					throw new InvalidSyntaxException("Invalid value: "
							+ filterstring.substring(pos), filterstring);
				}

				case '\\': {
					pos++;
					c = filterChars[pos];
					/* fall through into default */
				}

				default: {
					sb.append(c);
					pos++;
					break;
				}
				}
			}

			if (sb.length() == 0) {
				throw new InvalidSyntaxException("Missing value: "
						+ filterstring.substring(pos), filterstring);
			}

			//TODO Substitute filter 
			String ret = Util.toStringAttrValue (Substitute.substitute(null, sb.toString(), component)) ;
			if (ret == null) {
				throw new InvalidSyntaxException("Substitution failed. Missing value: "
						+ filterstring.substring(pos), filterstring);
			}
			return ret ;

			//			}
			//			return sb.toString();
		}

		@SuppressWarnings("unchecked") 
		private Object parse_substring() throws InvalidSyntaxException {
			StringBuffer sb = new StringBuffer(filterChars.length - pos);

			List operands = new ArrayList(10);

			parseloop: while (true) {
				char c = filterChars[pos];

				switch (c) {
				case ')': {
					if (sb.length() > 0) {
						operands.add(sb.toString());
					}

					break parseloop;
				}

				case '(': {
					throw new InvalidSyntaxException("Invalid value: "
							+ filterstring.substring(pos), filterstring);
				}

				case '*': {
					if (sb.length() > 0) {
						operands.add(sb.toString());
					}

					sb.setLength(0);

					operands.add(null);
					pos++;

					break;
				}

				case '\\': {
					pos++;
					c = filterChars[pos];
					/* fall through into default */
				}

				default: {
					sb.append(c);
					pos++;
					break;
				}
				}
			}

			int size = operands.size();

			if (size == 0) {
				return "";
			}

			if (size == 1) {
				Object single = operands.get(0);

				if (single != null) {
					return single;
				}
			}

			return operands.toArray(new String[size]);
		}

		private void skipWhiteSpace() {
			for (int length = filterChars.length; (pos < length)
					&& Character.isWhitespace(filterChars[pos]);) {
				pos++;
			}
		}
	}

	private static class SetAccessibleAction implements PrivilegedAction {
		private final AccessibleObject accessible;

		SetAccessibleAction(AccessibleObject accessible) {
			this.accessible = accessible;
		}

		@Override
		public Object run() {
			accessible.setAccessible(true);
			return null;
		}
	}

	/**
	 * This Map is used for case-insensitive key lookup during filter
	 * evaluation. This Map implementation only supports the get
	 * operation using a String key as no other operations are used by the
	 * Filter implementation.
	 */
	private static class CaseInsensitiveMap<E> implements Map<String,E> {

		private final Map<String, ? extends E> 	delegate;

		/**
		 * Create a case insensitive dictionary from the specified dictionary.
		 *
		 * @param dictionary
		 * @throws IllegalArgumentException If <code>dictionary</code> contains
		 *             case variants of the same key name.
		 */
		CaseInsensitiveMap(Map<String, ? extends E> delegate) {

			this.delegate 	= delegate;

			/*
			 * verify duplicate case-insensitive keys 
			 */
		}

		@Override
		public E get(Object key) {
			for (String delegateKey : delegate.keySet()) {
				if (delegateKey.equalsIgnoreCase((String) key))
					return delegate.get(delegateKey);
			}

			return null;
		}

		public int size() {
			throw new UnsupportedOperationException();
		}

		public boolean isEmpty() {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(Object key) {
			throw new UnsupportedOperationException();
		}

		public boolean containsValue(Object value) {
			throw new UnsupportedOperationException();
		}

		public E put(String key, E value) {
			throw new UnsupportedOperationException();
		}

		public E remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public void putAll(Map<? extends String, ? extends E> m) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public Set<String> keySet() {
			throw new UnsupportedOperationException();
		}

		public Collection<E> values() {
			throw new UnsupportedOperationException();
		}

		public Set<java.util.Map.Entry<String, E>> entrySet() {
			throw new UnsupportedOperationException();
		}

	}

}
