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
import java.lang.reflect.Array;
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
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
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
	public static String isSetAttrType (String type) {
		type = type.trim() ;
		if ((type == null) || (type.isEmpty()) || type.charAt(0) !='{') {
			return null;
		}
			return type.substring(1, type.length()-1) ;	
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

		boolean isSet = false ;
		if (type.charAt(0)=='{' ) {
			isSet = true ;
			type = type.substring(1, type.length()-1) ;	
		}
		Set<String> enumVals = Util.splitSet(type);
		if (enumVals == null || enumVals.size()==0) {
			logger.error("Invalid empty property type ") ;
			return false;
		}
		
		if (enumVals.size()> 1) return true ;
		type = enumVals.iterator().next() ;

		if (type==null || !(type.equals("string") || type.equals("int") ||type.equals("integer") || type.equals("boolean") || type.charAt(0)=='{' )) {
			logger.error("Invalid type " + type + ". Supported: string, int, boolean, enumeration; and sets");
			return false ;
		}
		return true ;
	}
	
	/**
	 * only string, int, boolean and enumerations attributes are accepted.
	 * Return the value if it is correct.
	 * For "int" returns an Integer object, otherwise it is the string "value"
	 * TODO : does not work for set of integer, treated as string set. Matching may fail.
	 * Should be reimplemented with real object, like Set<Integer> or any Object. 
	 * TODO : should check constraints to be sure it will be correctly interpreted by LDAP matching.
	 *
	 * @param value : a singleton, or a set "a, b, c, ...."
	 * @param type : a singleton "int", "boolean" or "string" or enumeration "v1, v2, v3 ..."
	 * 				or a set of these : "{int}", "{boolean}" or "{string}" or enumeration "{v1, v2, v3 ...}"
	 */
	public static Object checkAttrType(String attr, String value, String types) {
		if ((types == null) || (value == null) || types.isEmpty() || value.isEmpty() || attr==null || attr.isEmpty()) {
			logger.error("Invalid property " + attr + " = " + value + " type=" + types) ;
			return null;
		}

		boolean isSet = false ;
		if (types.charAt(0)=='{' ) {
			isSet = true ;
			types = types.substring(1, types.length()-1) ;	
		}

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

		//Type is a singleton : it must be only "string", "int", "boolean" 
		//but value can still be a set
		if (enumVals.size() == 1) {

			if (type.equals("boolean")) {
				for (String val : values) {
					if (!val.equalsIgnoreCase(CST.V_TRUE) && !val.equalsIgnoreCase(CST.V_FALSE)) {
						logger.error("Invalid attribute value \"" + val + "\" for attribute \"" + attr + "=" + value
								+ "\".  Boolean value expected");
						return null;
					}
				}
				return value ; 
			}

			if (type.equals("int") || type.equals("integer")) {
				try {
					if (values.size() == 1) {
						// the only case where we return something else than a string !
						return Integer.parseInt(values.iterator().next());
					}
					Set<Integer> intArray = new HashSet<Integer> () ;
					for (String val : values) {
						intArray.add (Integer.parseInt(val));
					}
					System.err.println("tableau de Integer : " + intArray.toString() + " en string : " + value);
					//unfortunately, match does not recognizes a set of integer.
					//return intArray ;
					return value ;
				} catch (Exception e) {
					logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr
							+ "\".  Integer value(s) expected");
					return null;
				}
			}

			if (!type.equals("string")) {
				logger.error("Invalid attribute type \"" + type + "\" for attribute \"" + attr
						+ "\".  int, integer, boolean or string expected");
				return null ;
			}
			//All values are Ok for string.
			return value ;
		}

		//Type is an enumeration with at least 2 values
		if (enumVals.containsAll(values)) {
			return value;
		}

		String errorMes = "Invalid attribute value(s) \"" + value + "\" for attribute \"" + attr
				+ "\".  Expected subset of: " + types;
		logger.error(errorMes);
		return null;
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

	///About dependencies
	/**
	 * Provided a client instance, checks if its dependency "clientDep", matches another dependency: "compoDep".
	 *
	 * matches only based on same name (same resource or same component).
	 * If client cardinality is multiple, compo cardinallity must be multiple too.
	 * No provision for the client constraints or characteristics (missing, eager)
	 *
	 * @param compoInst the composite instance containing the client
	 * @param compoDep the dependency that matches or not
	 * @param clientDep the client dependency we are trying to resolve
	 * @return
	 */
	public static boolean matchDependency(Instance compoInst, DependencyDeclaration compoDep, DependencyDeclaration clientDep) {
		boolean multiple = clientDep.isMultiple();
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account

		// if same nature (spec, implem, internface ... make a direct comparison.
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())) { 
			if (compoDep.getTarget().equals(clientDep.getTarget())) {
				if (!multiple || compoDep.isMultiple()) {
					return true;
				}
			}
		}

		//Look for a compatible dependency.
		//Stop at the first dependency matching only based on same name (same resource or same component)
		//No provision for : cardinality, constraints or characteristics (missing, eager)

		//Look if the client requires one of the resources provided by the specification
		if (compoDep.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.apamResolver.findSpecByName(compoInst,
					((SpecificationReference) compoDep.getTarget()).getName());
			if ((spec != null) && spec.getDeclaration().getProvidedResources().contains(clientDep.getTarget())
					&& (!multiple || compoDep.isMultiple())) {
				return true;
			}
		} 

		//If the composite has a dependency toward an implementation
		//and the client requires a resource provided by that implementation
		else {
			if (compoDep.getTarget() instanceof ImplementationReference) {
				String implName = ((ImplementationReference<?>) compoDep.getTarget()).getName();
				Implementation impl = CST.apamResolver.findImplByName(compoInst, implName);
				if (impl != null) {
					//The client requires the specification implemented by that implementation
					if (clientDep.getTarget() instanceof SpecificationReference) {
						String clientReqSpec = ((SpecificationReference) clientDep.getTarget()).getName();
						if (impl.getImplDeclaration().getSpecification().getName().equals(clientReqSpec)
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					} else {
						//The client requires a resource provided by that implementation
						if (impl.getImplDeclaration().getProvidedResources().contains(clientDep.getTarget())
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}


	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance.
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * For each dependency, we clone it, and we aggregate the constraints as found at all level,
	 * including the generic ones found in the composite type.
	 * The dependencies returned are clones of the original ones.
	 *
	 */
	public static Set<DependencyDeclaration> computeAllEffectiveDependency (Instance client) {
		if (client == null) return null ;
		Set<DependencyDeclaration> allDeps = new HashSet <DependencyDeclaration> ();
		for (DependencyDeclaration dep : computeAllDependencies (client)) {
			allDeps.add(computeEffectiveDependency(client, dep.getIdentifier())) ;
		}
		return allDeps ;
	}

	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance/
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * In case the same dependency is defined multiple time, it is the most concrete one that must be taken into account.
	 * There is no attempt to compute all the constraints that apply on a given dependency;
	 * We are only interested in the target.
	 * @param client
	 * @return
	 */
	public static Set<DependencyDeclaration> computeAllDependencies (Instance client) {
		Set<DependencyDeclaration> allDeps = new HashSet<DependencyDeclaration> () ;
		allDeps.addAll(client.getDeclaration().getDependencies());

		boolean found ;
		for (DependencyDeclaration dep : client.getImpl().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		for (DependencyDeclaration dep : client.getSpec().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		return allDeps ;
	}

	/**
	 * A dependency may have properties fail= null, wait, exception; exception = null, exception
	 * A contextual dependency can also have hide: null, true, false and eager: null, true, false
	 * The most local definition overrides the others.
	 * Exception null can be overriden by an exception; only generic exception overrides another non null one.
	 *
	 * @param dependency : the low level one that will be changed if needed
	 * @param dep: the group dependency
	 * @param generic: the dep comes from the composite type. It can override the exception, and has hidden and eager.
	 * @return
	 */
	private static void overrideDepFlags (DependencyDeclaration dependency, DependencyDeclaration dep, boolean generic) {
		//If set, cannot be changed by the group definition.
		//NOTE: This strategy is because it cannot be compiled, and we do not want to make an error during resolution
		if (dependency.getMissingPolicy() == null || (generic && dep.getMissingPolicy() != null)) {
			dependency.setMissingPolicy(dep.getMissingPolicy()) ;
		}

		if (dependency.getMissingException() == null || (generic && dep.getMissingException() != null)) {
			dependency.setMissingException(dep.getMissingException()) ;
		}

		if (generic) {
			dependency.setHide(dep.isHide()) ;
			dependency.setEager(dep.isEager()) ;
		}
	}

	/**
	 * The dependencies "depName" that apply on a client are those of the instance, plus those of the implem,
	 * plus those of the spec, and finally those in the composite.
	 * We aggregate the constraints as found at all level, including the generic one found in the composite type.
	 * We compute also the dependency flags.
	 *
	 * @param client
	 * @param dependency
	 * @return
	 */
	public static DependencyDeclaration computeEffectiveDependency (Instance client, String depName) {

		//Find the first dependency declaration.
		Component depComponent = client ;

		//take the declaration declared at the most concrete level
		DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName);
		if (dependency == null) {
			dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);
			depComponent = client.getImpl() ;
		}

		//the dependency can be defined at spec level if implem is a composite
		if (dependency == null) {
			dependency = client.getSpec().getApformSpec().getDeclaration().getDependency(depName);
			depComponent =client.getSpec();
		}

		if (dependency == null) return null ;

		//Now compute the inheritance instance, Implem, Spec.
		dependency = dependency.clone();
		Component group = depComponent.getGroup() ;

		while (group != null) {
			for (DependencyDeclaration dep : group.getDeclaration().getDependencies()) {
				if (dep.getIdentifier().equals(dependency.getIdentifier())) {
					overrideDepFlags (dependency, dep, false);
					dependency.getImplementationConstraints().addAll(dep.getImplementationConstraints()) ;
					dependency.getInstanceConstraints().addAll(dep.getInstanceConstraints()) ;
					dependency.getImplementationPreferences().addAll(dep.getImplementationPreferences()) ;
					dependency.getInstancePreferences().addAll(dep.getInstancePreferences()) ;
					break ;
				}
			}
			group = group.getGroup() ;
		}

		//Add the composite generic constraints
		CompositeType compoType = client.getComposite().getCompType() ;
		Map<String, String> validAttrs = client.getValidAttributes() ;
		for ( DependencyDeclaration  genDep  : compoType.getCompoDeclaration().getContextualDependencies()) {

			if (matchGenericDependency(client, genDep, dependency)) {
				overrideDepFlags (dependency, genDep, true) ;

				if (Util.checkFilters(genDep.getImplementationConstraints(), null, validAttrs, client.getName())) {
					dependency.getImplementationConstraints().addAll(genDep.getImplementationConstraints()) ;
				}

				dependency.getInstanceConstraints().addAll(genDep.getInstanceConstraints()) ;

				if (Util.checkFilters(null, genDep.getImplementationPreferences(), validAttrs, client.getName())) {
					dependency.getImplementationPreferences().addAll(genDep.getImplementationPreferences()) ;
				}

				if (Util.checkFilters(null, genDep.getInstancePreferences(), validAttrs, client.getName())) {
					dependency.getInstancePreferences().addAll(genDep.getInstancePreferences()) ;
				}
			}
		}
		return dependency ;
	}

	/**
	 * Provided a composite (compoInst), checks if the provided generic dependency constraint declaration
	 * matches the compoClient dependency declaration.
	 *
	 * @param compoInst the composite instance containing the client
	 * @param genericDeps the dependencies of the composite: a regExpression
	 * @param clientDep the client dependency we are trying to resolve.
	 * @return
	 */
	public static boolean matchGenericDependency(Instance compoInst, DependencyDeclaration compoDep, DependencyDeclaration clientDep) {

		String pattern = compoDep.getTarget().getName() ;
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account

		// same nature: direct comparison
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())
				&& (clientDep.getTarget().getName().matches(pattern))) {
			return true;
		}

		//If the client dep is an implementation dependency, check if the specification matches the pattern
		if (compoDep.getTarget() instanceof SpecificationReference
				&& clientDep.getTarget() instanceof ImplementationReference) {
			String implName = ((ImplementationReference<?>) clientDep.getTarget()).getName();
			Implementation impl = CST.apamResolver.findImplByName(compoInst, implName);
			if (impl != null && impl.getSpec().getName().matches(pattern)) {
				return true ;
			}
		}
		return false;
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