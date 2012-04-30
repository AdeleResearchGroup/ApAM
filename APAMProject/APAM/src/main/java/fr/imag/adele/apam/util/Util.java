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

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;



/**
 * The static Class Util provides a set of static method for the iPOJO service concrete machine.
 * 
 * @author SAM team
 */
public class Util {

    static boolean failed;

    private Util() {
    };

    public static List<ComponentDeclaration> getComponents(Element root) {
        Util.failed = false;
        CoreParser parser = new CoreMetadataParser(root);
        return parser.getDeclarations(new ErrorHandler() {

            @Override
            public void error(Severity severity, String message) {
                System.err.println("error parsing component declaration : "+message);
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

    public static List<String> splitList(String str) {
        if ((str == null) || (str.length() == 0)) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(Util.split(str));
    }

    public static String[] split(String str) {
        if ((str == null) || (str.length() == 0)) {
            return new String[0];
        }
        String internal;
        str = str.replaceAll("\\ ", "");
        //        String[] tab = str.split(Util.splitSeparator);
        //    }

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
        boolean ok;
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
            System.out.print("Invalid filters : ");
            for (Filter f : filters) {
                System.out.println("   " + f.toString());
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
     * compoFrom accepts to borrow the attribute
     * toImpl is inside compoFrom.
     * attribute friendImplementation is set, and toImpl is inside a friend and matches the attribute.
     * toImpl does not matches the attribute localImplementation.
     * 
     * @param compoFrom
     * @param toImpl
     * @return
     */
    public static boolean checkImplVisible(CompositeType compoFrom, Implementation toImpl) {
        // First check inst can be borrowed
        String borrow = ((String) compoFrom.get(CST.A_BORROWIMPLEM));
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
            String friend = ((String) compoFrom.get(CST.A_FRIENDINSTANCE));
            if ((friend != null) && Util.checkImplVisibilityExpression(friend, toImpl))
                return true;
        }
        String local = ((String) compoFrom.get(CST.A_LOCALINSTANCE));
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
     * compoFrom accpets to borroxww the attribute
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
        // First check inst can be borrowed
        String borrow = ((String) compoFrom.get(CST.A_BORROWINSTANCE));
        if ((borrow != null) && (Util.checkInstVisibilityExpression(borrow, toInst) == false))
            return false;

        Composite toCompo = toInst.getComposite();
        if (compoFrom == toCompo)
            return true;
        if (compoFrom.dependsOn(toCompo)) {
            String friend = ((String) compoFrom.get(CST.A_FRIENDINSTANCE));
            if ((friend != null) && Util.checkInstVisibilityExpression(friend, toInst))
                return true;
        }
        if (compoFrom.getRootComposite() == toCompo.getRootComposite()) {
            String appli = ((String) compoFrom.get(CST.A_APPLIINSTANCE));
            if ((appli != null) && Util.checkInstVisibilityExpression(appli, toInst))
                return true;
        }
        String local = ((String) compoFrom.get(CST.A_LOCALINSTANCE));
        if ((local != null) && Util.checkInstVisibilityExpression(local, toInst))
            return false;
        return true;
    }

    public static boolean isPredefinedAttribute(String attr) {
        for (String pred : CST.predefAttributes) {
            if (pred.equals(attr.toLowerCase()))
                return true;
        }
        for (String pred : CST.finalAttributes) {
            if (pred.equals(attr.toLowerCase()))
                return true;
        }
        return false;
    }

    public static boolean isFinalAttribute(String attr) {
        for (String pred : CST.finalAttributes) {
            if (pred.equals(attr.toLowerCase()))
                return true;
        }
        return false;
    }

    public static boolean isReservedAttribute(String attr) {
        for (String pred : OBR.reservedAttributes) {
            if (pred.equals(attr.toLowerCase()))
                return true;
        }
        return false;
    }

}


