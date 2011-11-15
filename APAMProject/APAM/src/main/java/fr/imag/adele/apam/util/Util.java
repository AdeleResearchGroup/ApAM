package fr.imag.adele.apam.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.ASMImpl;
import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.ASMSpec;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.util.AttributesImpl;

/**
 * The static Class Util provides a set of static method for the iPOJO service concrete machine.
 * 
 * @author SAM team
 */
public class Util {

    private Util() {
    };

    /** The logger. */
    private static Logger logger = Logger.getLogger(Util.class);

    /**
     * Orders the array in lexicographical order.
     * 
     * @param interfaces
     */
    public static String[] orderInterfaces(String[] interfaces) {
        if (interfaces == null)
            return null;
        boolean ok = false;
        String tmp;
        while (!ok) {
            ok = true;
            for (int i = 0; i < interfaces.length - 1; i++) {
                if (interfaces[i].compareTo(interfaces[i + 1]) > 0) {
                    tmp = interfaces[i];
                    interfaces[i] = interfaces[i + 1];
                    interfaces[i + 1] = tmp;
                    ok = false;
                }
            }
        }
        return interfaces;
    }

    /**
     * compares the two arrays of interfaces. They may be in a different order. Returns true if they contain exactly the
     * same interfaces (strings).
     * 
     * @param i1 : an array of strings
     * @param i2
     * @return
     */
    public static boolean sameInterfaces(String[] i1, String[] i2) {
        if ((i1 == null) || (i2 == null) || (i1.length == 0) || (i2.length == 0))
            return false;
        if (i1.length != i2.length)
            return false;
        if ((i1.length == 1) && (i1[0].equals(i2[0])))
            return true;
        boolean ok;
        for (int i = 0; i < i1.length; i++) {
            if (!i1[i].equals(i2[i]))
                return false;
        }
        return true;
    }

    /**
     * Adds in ASM object the properties found in the associated SAM object.
     * Checks the predefined attribute : upperCase and valid values.
     * Either samProp or initProp can be null
     * 
     * @param initProp : the initial properties provided in the ASM constructor
     * @param samProp : the properties found in SAM.
     * @return
     */
//    public static Map<String, Object> mergeProperties(AttributesImpl asmObj, Attributes initProp,
//            Map<String, Object> samPropParam) {
//        if ((initProp == null) && (samPropParam == null))
//            return new HashMap<String, Object>();
//        return asmObj.checkPredefinedAttributes(samPropParam);
//    }

//       
//        if ((initProp == null) && (samPropParam == null))
//            return new HashMap<String, Object>();
//        Map<String, Object> samProp = new HashMap<String, Object>(samPropParam);
//        String attr;
//        Object val;
//        if ((initProp != null) && (samProp != null)) { // merge
//            for (Enumeration<String> e = ((AttributesImpl) initProp).keys(); e.hasMoreElements();) {
//                attr = e.nextElement();
//                val = initProp.getProperty(attr);
//                if (samProp.get(attr) == null) {
//                    samProp.put(attr, val);
//                } else { // different values, pas normal !
//                    if (initProp.getProperty(attr) != samProp.get(attr)) {
//                        System.out.println("Warning ! attribut " + attr + "in " + asmObj
//                                + " different in SAM and init val : "
//                                + samProp.get(attr) + ", " + val);
//                    }
//                }
//            }
//        }
//
//        if (samProp == null)
//            samProp = initProp.getProperties();
//        return asmObj.checkPredefinedAttributes(samProp);
//     }

    public static String ANDLDAP(String... params) {
        StringBuilder sb = new StringBuilder("(&");
        for (String p : params) {
            sb.append(p);
        }
        sb.append(")");
        return sb.toString();
    }

    public static Filter buildFilter(Set<Filter> filters) {
        if ((filters == null) || (filters.size() == 0))
            return null;
        String ldap = null;
        for (Filter f : filters) {
            if (ldap == null) {
                ldap = f.toString();
            } else {
                ldap = Util.ANDLDAP(ldap, f.toString());
            }
        }
        Filter ret = null;
        try {
            ret = org.osgi.framework.FrameworkUtil.createFilter(ldap);
        } catch (InvalidSyntaxException e) {
            System.out.print("Invalid filters : ");
            for (Filter f : filters) {
                System.out.println("   " + f.toString());
                ;
            }
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Checks if implementation impl is visible from composite type compoFrom.
     * An implementation can pertain to more than one composite type.
     * Each composite type can overload the scope attribute. Returns true if impl is visible
     * through at least one of the composite types in which it is contained.
     * 
     * Otherwise the scope rules are the same.
     */
    public static boolean checkImplVisible(ASMImpl impl, CompositeType compoFrom) {
        for (CompositeType compoTo : impl.getInCompositeType()) {
            if (Util.checkVisibilityImpl(compoFrom, compoTo, impl))
                return true;
        }
        // failed
//        if (compoFrom.isInternal()) {
//            System.out.println("Composite type " + compoFrom.getName()
//                    + " is internal and does not see implementation " + impl + " in " + impl.getInCompositeType());
//        } else
//            System.out.println("Composite type " + compoFrom.getName()
//                        + " does not see implementation " + impl + " in " + impl.getInCompositeType());
        return false;
    }

    /**
     * Provided that scope is the wider possible visibility value of an implementation pertaining to compoTo,
     * return true if the implementation in compoTo is visible from compoFrom.
     * 
     * @param compoFrom : the composite that wants to see an implementation in compoTo
     * @param compoTo : the composite type containing the implementation to see.
     * @return
     */
    private static boolean checkVisibilityImpl(CompositeType compoFrom, CompositeType compoTo, ASMImpl impl) {
        String visible = (compoFrom.isInternal()) ? CST.V_LOCAL : ((CompositeTypeImpl) compoTo)
                .getVisibleInCompoType(impl);

        if (visible.equals(CST.V_GLOBAL))
            return true;
        if (visible.equals(CST.V_COMPOSITE))
            return ((compoFrom == compoTo) || (compoFrom.imports(compoTo)));
        if (visible.equals(CST.V_LOCAL))
            return (compoFrom == compoTo);

        System.err.println("CheckAccess : Invalid Scope value :  " + visible);
        return false;
    }

    public static boolean checkInstVisible(Composite compoFrom, ASMInst toInst) {
        String scope = toInst.getScope();
        if ((compoFrom == null) || (toInst.getComposite() == null)) { // compoFrom or impl are root composite
            return scope.equals(CST.V_GLOBAL);
        }

        if (compoFrom.isInternal())
            scope = CST.V_LOCAL;

        if ((toInst.getShared().equals(CST.V_FALSE) && (!toInst.getInvWires().isEmpty()))) {
//            System.out.println(toInst + " is not sharable");
            return false;
        }

        boolean valid = Util.checkVisibility(compoFrom, toInst.getComposite(), scope);
        if (!valid) {
//            System.out.println("Composite " + compoFrom.getName()
//                    + " does not see instance " + toInst + " in Composite " + toInst.getComposite().getName()
//                    + " (scope is " + scope + ")");
        }
        return valid;
    }

    private static boolean checkVisibility(Composite compoFrom, Composite compoTo, String scope) {
        if ((scope == null) || (scope.equals(CST.V_GLOBAL)))
            return true;
        if (scope.equals(CST.V_APPLI))
            return (compoFrom.getRootComposite() == compoTo.getRootComposite());
        if (scope.equals(CST.V_COMPOSITE))
            return ((compoFrom == compoTo) || (compoFrom.dependsOn(compoTo)));
        if (scope.equals(CST.V_LOCAL))
            return (compoFrom == compoTo);

        System.err.println("CheckAccess : Invalid Scope value :  " + scope);
        return false;
    }

}

/**
 * Builds a dictionary from a map attribute = value(s).
 * 
 * @param properties the properties, if values can be translated to strings
 * @return the dictionary< string, string>
 */
// public static Dictionary<String, String> properties2Dictionary(Map<String, Object> properties) {
// if (properties == null)
// return null;
// Dictionary<String, String> retMap = new Hashtable<String, String>();
// Set<String> keys = properties.keySet();
// for (String key : keys) {
// if (!(properties.get(key) instanceof String)) {
// retMap.put(key, properties.get(key).toString());
// } else {
// retMap.put(key, (String) properties.get(key));
// }
// }
// return retMap;
// }

// public static Map<String, Object> properties2Map(Properties props) {
// if (props == null)
// return null;
// Map<String, Object> propsMap = new HashMap<String, Object>();
// for (Object attr : props.keySet()) {
// if (attr instanceof String)
// propsMap.put((String) attr, props.get(attr));
// else
// propsMap.put(attr.toString(), props.get(attr));
// }
// return propsMap;
// }
/**
 * check if a property list matches a given query
 * 
 * @param query
 * @param name : if it is a query by name, it should match "name". Can be null if
 * @param prop : if query LDAP, it should match the property map.
 * @return
 */
// public static boolean matchQuery(Query query, String matchName, Map<String, Object> prop) {
// if ((query == null) || (matchName == null) || (prop == null))
// return false;
// int typeQuery = query.getType();
// switch (typeQuery) {
// case Query.T_BY_NAME:
// String name = ((QueryByName) query).getName();
// if ((name != null) && (name.equals(matchName))) {
// return true;
// } else
// return false;
// case Query.T_LDAP:
// Filter fGoal = null;
// try {
// fGoal = FrameworkUtil.createFilter(((QueryLDAP) query).getLDAP());
// if ((fGoal != null) && (fGoal.match(Util.properties2Dictionary(prop)))) { // match
// // dictionary
// // filter
// return true;
// }
// } catch (Exception e) {
// Util.logger.error("invalid LDAP query : " + fGoal);
// return false;
// }
// }
// return false;
// }

// public static boolean matchLDAP(Filter filter, String matchName, Map<String, Object> prop) {
// if ((filter == null) || (matchName == null) || (prop == null))
// return false;
//
// try {
// if ((filter != null) && (filter.match(Util.properties2Dictionary(prop)))) { // match
// // dictionary
// // filter
// return true;
// }
// } catch (Exception e) {
// Util.logger.error("invalid LDAP query : " + filter);
// }
// return false;
// }

