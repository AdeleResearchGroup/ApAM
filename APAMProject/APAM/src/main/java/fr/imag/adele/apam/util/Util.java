package fr.imag.adele.apam.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;

/**
 * The static Class Util provides a set of static method for the iPOJO service concrete machine.
 * 
 * @author SAM team
 */
public class Util {
	private static Logger logger = LoggerFactory.getLogger(Util.class);
    static boolean failed;

    private Util() {
    };

    public static List<ComponentDeclaration> getComponents(Element root) {
        Util.failed = false;
        CoreParser parser = new CoreMetadataParser(root);
        return parser.getDeclarations(new ErrorHandler() {

            @Override
            public void error(Severity severity, String message) {
                logger.error("error parsing component declaration : "+message);
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
        if ((names == null) || (names.size() == 0))
            return null;
        String ret = "{";
        for (String name : names) {
            ret += name + ", ";
        }
        return ret.substring(0, ret.length() - 2) + "}";
    }
    public static String toStringArrayString(String[] names) {
    	return toStringResources(new HashSet<String> (Arrays.asList(names))) ;
    }

    public static Set<Filter> toFilter(Set<String> filterString) {
        Set<Filter> filters = new HashSet<Filter>();
        for (String f : filterString) {
            filters.add(ApamFilter.newInstance(f));
        }
        return filters;
    }

    public static List<Filter> toFilterList(List<String> filterString) {
        List<Filter> filters = new ArrayList<Filter>();
        for (String f : filterString) {
            filters.add(ApamFilter.newInstance(f));
        }
        return filters;
    }
    /**
     * Warning: returns an unmodifiable List !
     * @param str
     * @return
     */
    public static List<String> splitList(String str) {
        return Arrays.asList(Util.split(str));
    }
    
    public static Set<String> splitSet(String str) {
        return new HashSet<String> (Arrays.asList(Util.split(str))) ;
    }

    /**
     * Provided a string contain a list of values, return an array of string containing the different values.
     * A list is of the form "{A, B, .... G}" or "[A, B, .... G]"
     * 
     * If the string is empty or is not a list, return the empty array.
     * @param str
     * @return
     */
    public static String[] split(String str) {
        if ((str == null) || (str.length() == 0) 
        		|| ((str.charAt(0) != '{') && (str.charAt(0) != '['))) {
            return new String[0];
        }
        String internal;
        str = str.replaceAll("\\ ", "");
        str = str.replaceAll(";", ",");
        str = str.replaceAll("\\[,", "[");
        str = str.replaceAll(",]", "]");

        // Remove { and } or [ and ]
        if (((str.charAt(0) == '{') && (str.charAt(str.length() - 1) == '}'))
                || ((str.charAt(0) == '[') && (str.charAt(str.length() - 1) == ']'))) {
            internal = (str.substring(1, str.length() - 1)).trim();
            // Check empty array
            if (internal.length() == 0) {
                return new String[0];
            }
            return internal.split(",");
        } else {
            return new String[] { str };
        }
    }


    public static final String splitSeparator = ", |\\s|\\{|\\[|\\]|\\}";
    /** The logger. */
    //    private static Logger logger = Logger.getLogger(Util.class);

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
        
        for (int i = 0; i < i1.length; i++) {
            if (!i1[i].equals(i2[i]))
                return false;
        }
        return true;
    }


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
            logger.debug("Invalid filters : ");
            for (Filter f : filters) {
                logger.debug("   " + f.toString());
                ;
            }
            e.printStackTrace();
        }
        return ret;
    }


    public static boolean checkImplVisibilityExpression(String expre, Implementation impl) {
        if ((expre == null) || expre.equals(CST.V_TRUE))
            return true;
        if (expre.equals(CST.V_FALSE))
            return false;
        Filter f = ApamFilter.newInstance(expre);
        if (f == null)
            return false;
        return impl.match(f);
    }

    /**
     * Implementation toImpl can be borrowed by composite type compoFrom if :
     * compoFrom accepts to borrow the service
     * toImpl is inside compoFrom.
     * attribute friendImplementation is set, and toImpl is inside a friend and matches the attribute.
     * toImpl does not matches the attribute localImplementation.
     * 
     * @param compoFrom
     * @param toImpl
     * @return
     */
    public static boolean checkImplVisible(CompositeType compoFrom, Implementation toImpl) {
        if (toImpl.getInCompositeType().isEmpty() || toImpl.getInCompositeType().contains(compoFrom))
            return true;

        // First check inst can be borrowed
        String borrow =  ((CompositeDeclaration)compoFrom.getDeclaration()).getVisibility().getBorrowImplementations() ; //   getProperty(CST.A_BORROWIMPLEM));
        if ((borrow != null) && (Util.checkImplVisibilityExpression(borrow, toImpl) == false))
            return false;

        // true if at least one composite type that own toImpl accepts to lend it to compoFrom.
        for (CompositeType compoTo : toImpl.getInCompositeType()) {
            if (Util.checkImplVisibleInCompo(compoFrom, toImpl, compoTo))
                return true;
        }
        return false;
    }

    public static boolean
    checkImplVisibleInCompo(CompositeType compoFrom, Implementation toImpl, CompositeType compoTo) {
        if (compoFrom == compoTo)
            return true;
        if (compoFrom.isFriend(compoTo)) {
            String friend = ((CompositeDeclaration)compoTo.getDeclaration()).getVisibility().getFriendImplementations() ;     //.getProperty(CST.A_FRIENDIMPLEM));
//            String friend = ((String) compoTo.getProperty(CST.A_FRIENDIMPLEM));
            if ((friend != null) && Util.checkImplVisibilityExpression(friend, toImpl))
                return true;
        }
        String local = ((CompositeDeclaration)compoTo.getDeclaration()).getVisibility().getLocalImplementations() ;
//        String local = ((String) compoTo.getProperty(CST.A_LOCALIMPLEM));
        if ((local != null) && Util.checkImplVisibilityExpression(local, toImpl))
            return false;
        return true;
    }

    public static boolean checkInstVisibilityExpression(String expre, Instance inst) {
        if ((expre == null) || expre.equals(CST.V_TRUE))
            return true;
        if (expre.equals(CST.V_FALSE))
            return false;
        Filter f = ApamFilter.newInstance(expre);
        if (f == null)
            return false;
        return inst.match(f);
    }

    /**
     * Instance toInst can be borrowed by composite compoFrom if :
     * compoFrom accepts to borrow the attribute
     * toInst is inside compoFrom.
     * attribute friendInstance is set, and toInst is inside a friend and matches the attribute.
     * attribute appliInstance is set, and toInst is in same appli and matches the attribute.
     * toInst does not matches the attribute localInstance.
     * 
     * @param compoFrom
     * @param toInst
     * @return
     */
    public static boolean checkInstVisible(Composite compoFrom, Instance toInst) {
        Composite toCompo = toInst.getComposite();
        CompositeType toCompoType = toInst.getComposite().getCompType();
        CompositeType fromCompoType = compoFrom.getCompType() ;
        
        if (compoFrom == toCompo)
            return true;

        // First check inst can be borrowed
        String borrow = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getBorrowInstances() ;
//      String borrow = ((String) compoFrom.getCompType().getProperty(CST.A_BORROWINSTANCE));
        if ((borrow != null) && (Util.checkInstVisibilityExpression(borrow, toInst) == false))
            return false;

        if (compoFrom.dependsOn(toCompo)) {
        	String friend = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getFriendInstances() ;
//          String friend = ((String) toCompoType.getProperty(CST.A_FRIENDINSTANCE));
            if ((friend != null) && Util.checkInstVisibilityExpression(friend, toInst))
                return true;
        }
        if (compoFrom.getAppliComposite() == toCompo.getAppliComposite()) {
        	String appli = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getApplicationInstances() ;
        	//String appli = ((String) toCompoType.getProperty(CST.A_APPLIINSTANCE));
            if ((appli != null) && Util.checkInstVisibilityExpression(appli, toInst))
                return true;
        }
        String local = ((CompositeDeclaration) fromCompoType.getDeclaration()).getVisibility().getLocalInstances() ;
        //String local = ((String) toCompoType.getProperty(CST.A_LOCALINSTANCE));
        if ((local != null) && Util.checkInstVisibilityExpression(local, toInst))
            return false;
        return true;
    }

//    public static boolean isPredefinedAttribute(String attr) {
//        for (String pred : CST.predefAttributes) {
//            if (pred.equals(attr))
//                return true;
//        }
//        return false;
//    }

    public static boolean isInheritedAttribute(String attr) {
    	if (isReservedAttributePrefix(attr)) return false ;
        for (String pred : CST.notInheritedAttribute) {
            if (pred.equals(attr))
                return false;
        }
        return true;
    }
    
    public static boolean isFinalAttribute(String attr) {
        for (String pred : CST.finalAttributes) {
            if (pred.equals(attr))
                return true;
        }
        return false;
    }

    public static boolean isReservedAttributePrefix(String attr) {
        for (String prefix : CST.reservedPrefix) {
            if (attr.startsWith(prefix))
                return true;
        }
        return false;
    }

	/**
	 * Check if attribute "attr=value" is valid when set on object "inst".
	 * inst can be an instance, an implementation or a specification.
	 * Check if the value is consistent with the type.
	 * All predefined attributes are Ok (scope ...)
	 * Cannot be a reserved attribute
	 */
	public static boolean validAttr(String component, String attr) {
		//attr = attr.toLowerCase();
//		if (Util.isPredefinedAttribute(attr))
//			return true;

		if (Util.isFinalAttribute(attr)) {
			logger.error("ERROR: in " + component + ", attribute\"" + attr + "\" is final");
			return false;
		}

		if (Util.isReservedAttributePrefix(attr)) {
			logger.error("ERROR: in " + component + ", attribute\"" + attr + "\" is reserved");
			return false;
		}

		return true ;
	}

    
    /**
     * only string, int and boolean attributes are accepted.
     * 
     * @param value
     * @param type
     */
    public static boolean checkAttrType(String attr, String value, String type) {
        if ((type == null) || (value == null))
            return false;

        if (type.equals("boolean") && !value.equalsIgnoreCase(CST.V_TRUE) && !value.equalsIgnoreCase(CST.V_FALSE)) {
            logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr
                    + "\".  Boolean value expected");
            return false;
        }
        if (type.equals("int")) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (Exception e) {
                logger.error("Invalid attribute value \"" + value + "\" for attribute \"" + attr
                        + "\".  Integer value expected");
                return false;
            }
        }
        if ((type.charAt(0) == '{') || (type.charAt(0) == '[')) { // enumerated value
            String[] enumVals = Util.split(type);
            for (String one : enumVals) {
                if (one.equals(value))
                    return true;
            }
            String errorMes = "Invalid attribute value \"" + value + "\" for attribute \"" + attr + "\".  Expected: \"{" ;
            for (String one : enumVals) {
            	errorMes += one + " ";
            }
            errorMes += "}\"" ;
            logger.error(errorMes);
            return false;
        }

        // it is s string. Anything is Ok
        return true;
    }

    public static String toStringSetReference (Set<? extends ResourceReference> setRef) {
    	String ret = "{" ;
    	for (ResourceReference ref : setRef) {
    		ret = ret+ ref.getJavaType() + ", ";
    	}
    	int i = ret.lastIndexOf(',') ;
    	ret = ret.substring(0, i) ;
    	return ret + "}" ;
    }
    
}


