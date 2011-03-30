package fr.imag.adele.apam.util;


import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.query.Query;
import fr.imag.adele.am.query.QueryByName;
import fr.imag.adele.am.query.QueryLDAP;
//import fr.imag.adele.sam.ipojo.util.LDAP;

/**
 * The Class Utils provides a set of static method for the iPOJO service
 * concrete machine.
 * 
 * @author SAM team
 */
public class Util {

	private Util () {} ;
    /** The logger. */
    private static Logger logger = Logger.getLogger(Util.class);

    /**
     * check if a property list matches a given query
     * @param query
     * @param name : if it is a query by name, it should match "name". Can be null if
     * @param prop : if query LDAP, it should match the property map.
     * @return
     */
    public static boolean matchQuery (Query query, String matchName, Map<String, Object> prop) {
    	int typeQuery = query.getType() ;
    	switch (typeQuery) {
    	case Query.T_BY_NAME:
    		String name = ((QueryByName) query).getName();
    		if ((name != null)  && (name.equals(matchName))) {
    			return true ;  	}
    		else return false ;
    	case Query.T_LDAP:
    		Filter fGoal = null ;
    		try {
    			fGoal = FrameworkUtil.createFilter(((QueryLDAP) query).getLDAP()) ;
    			if ((fGoal != null) && (fGoal.match (Util.properties2Dictionary(prop)))) {  // match dictionary filter
    				return true ; }
    		}
    		catch (Exception e) {
    			logger.error("invalid LDAP query : " + fGoal) ;
    			return false ;
    		}
    	}
   		return false ;
    }

    public static boolean matchLDAP (Filter filter, String matchName, Map<String, Object> prop) {
     		try {
    			if ((filter != null) && (filter.match (Util.properties2Dictionary(prop)))) {  // match dictionary filter
    				return true ; }
    		}
    		catch (Exception e) {
    			logger.error("invalid LDAP query : " + filter) ;
    		}
			return false ;
     }

    /**
     * Return the list of keys (String or PID, or whatever) that have a property map that matches the query
     * @param query
     * @param mapObject2Properties
     * @return
     */
//	public static Set<PID> matchList(Query query, Map <PID, HashMap<String, Object>> mapObject2Properties) {
//		Set<PID> ret = new TreeSet<PID>  () ;
//		Set<PID> impls = mapObject2Properties.keySet();
// 
//		if (query.getType() == Query.T_BY_NAME) {
//    		String name = ((QueryByName) query).getName();
//    		for (PID implName : impls) {
//    			if ((name != null)  && (name.equals(implName))) {
//    				ret.add(implName) ; 
//    				}
//    		} 
//    		return ret ;  	
//    		}
//		if (query.getType() == Query.T_LDAP) {
//			Filter fGoal = null;
//			if (query == null) return ret ;
//    		try {
//    			fGoal = FrameworkUtil.createFilter(((QueryLDAP) query).getLDAP()) ;
//        		for (PID implName : impls) {
//    			if ((fGoal != null) && (fGoal.match (Utils.properties2Dictionary(mapObject2Properties.get(implName))))) {  // match dictionary filter
//    				ret.add (implName); 
//    				}
//        		}
//    		}
//    		catch (Exception e) {
//    			logger.error("invalid LDAP query : " + fGoal) ;
//     		}
//    	}
//   		return ret ;
//    }
	
/**
 * Orders the array in lexicographical order. 
 * @param interfaces
 */
	public static String [] orderInterfaces (String [] interfaces) {
		boolean ok = false ;
		String tmp ;
		while (!ok) {
			ok = true ;
			for (int i = 0; i < interfaces.length -1; i++) {
				if (interfaces[i].compareTo(interfaces[i+1]) > 0) {
					tmp = interfaces[i] ;
					interfaces[i] = interfaces[i+1] ;
					interfaces[i+1] = tmp ;
					ok = false ;
				}
			}
		}
		return interfaces ;
	}
    
    /**
     * compares the two arrays of interfaces. They may be in a different order.
     * Returns true if they contain exactly the same interfaces (strings).
     * @param i1 : an array of strings
     * @param i2
     * @return
     */
    public static boolean sameInterfaces (String [] i1, String [] i2) {
    	if (i1.length != i2.length) return false ;
    	if ((i1.length == 1) && (i1[0].equals(i2[0]))) return true ;
    	boolean ok ;
    	for (int i = 0; i < i1.length; i++) {
			if (!i1[i].equals(i2[i])) return false ;
    	}
    	return true ;
    }
    
    /**
     * Builds a dictionary from a map attribute = value(s).
     * 
     * @param properties the properties, if values can be translated to strings
     * @return the dictionary< string, string>
     */
    public static  Dictionary<String, String> properties2Dictionary(
            Map<String, Object> properties) {
        Dictionary<String, String> retMap = new Hashtable<String, String>();
        Set<String> keys = properties.keySet();
        for (String key : keys) {
            if (!(properties.get(key) instanceof String)) {
                retMap.put(key, properties.get(key).toString());
            } else {
                retMap.put(key, (String) properties.get(key));
            }
        }
        return retMap;
    }

    public static Map<String, Object> properties2Map (Properties props) {
    	Map<String, Object> propsMap = new HashMap<String, Object> () ;
    	for (Object attr : props.keySet()) {
			if (attr instanceof String) 
				propsMap.put ((String)attr, props.get(attr)) ; 
			else propsMap.put(attr.toString(), props.get(attr)) ;
		}
    	return propsMap ;
    }

    /**
     * Adds is samProp the properties of samProp. 
     * @param initProp
     * @param samProp
     * @return
     */
    public static Map<String, Object> mergeProperties (Attributes initProp, Map<String, Object> samProp ) {
    	if (initProp == null) return samProp;
    	String attr ;
    	try {
        for (Enumeration<String> e = ((ApamProperty)initProp).keys() ; e.hasMoreElements() ;) {
        	attr = e.nextElement() ;
    		if (samProp.get(attr) == null) {
    			samProp.put((String)attr, initProp.getProperty(attr)) ;
    		} else { //valeur differente, pas normal !
    			if (initProp.getProperty(attr) != samProp.get(attr)) {
    				System.out.println("Erreur ! attribut " + attr + " different in SAM and init val : "
    						+ samProp.get(attr) + ", " + initProp.getProperty(attr));
    				//TODO raffiner. shared, instantiable etc.
    			}
    		}	
    	}
    	return samProp ;
    	} catch (Exception e) {e.printStackTrace() ; } 
    	return null ;
    }
    
	public static String ANDLDAP(String... params) {
		StringBuilder sb = new StringBuilder("(&");
		for (String p : params) {
			sb.append(p);
		}
		sb.append(")");
		return sb.toString();
	}

    
	public static Filter buildFilter (Set<Filter> filters) {
		if (filters == null || filters.size() == 0) return null ;
		String ldap = null;
		for (Filter f : filters) {
			if (ldap == null) {
				ldap = f.toString() ;
			}
			else {
				ldap = ANDLDAP(ldap, f.toString()) ;
			}
		}
		Filter ret = null ;
		try {
			ret = org.osgi.framework.FrameworkUtil.createFilter (ldap);
		} catch (InvalidSyntaxException e) {
			System.out.print("Invalid filters : ");
			for (Filter f : filters) {
				System.out.println("   " + f.toString()); ;
			}
			e.printStackTrace();
		} 
		return ret ;
	}

}
