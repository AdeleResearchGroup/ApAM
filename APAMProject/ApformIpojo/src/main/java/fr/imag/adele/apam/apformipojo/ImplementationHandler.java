package fr.imag.adele.apam.apformipojo;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.apamImpl.CST;

/**
 * The base class for all iPojo handlers manipulating APAM implementations and instances
 * 
 * @author vega
 * 
 */
public abstract class ImplementationHandler extends PrimitiveHandler {

    /**
     * The name space of this handler
     */
    protected final static String APAM_NAMESPACE = "fr.imag.adele.apam";

    /**
     * Quote String in message
     */
    public static final String quote(String arg) {
        return "\"" + arg + "\"";
    }

    /**
     * Utility method to facilitate iteration over possibly null element lists
     */
    protected static final Element[] EMPTY_ELEMENT_LIST = new Element[0];

    protected static final Element[] optional(Element[] elements) {
        return elements != null ? elements : ImplementationHandler.EMPTY_ELEMENT_LIST;
    }

    protected static final boolean isDefined(Element[] elements) {
        return (elements != null) && (elements.length > 0);
    }

    protected static final boolean isSingleton(Element[] elements) {
        return (elements != null) && (elements.length == 1);
    }

    protected static final Element singleton(Element[] elements) {
        return elements[0];
    }

    /**
     * Utility functions to unify boolean and scope property values
     */
    protected static final String booleanValue(String value) {
        if (value == null)
            return value;

        return Boolean.parseBoolean(value) ? CST.V_TRUE : CST.V_FALSE;
    }

    protected static final String visibilityValue(String value) {
        if (value == null)
            return value;

        if (value.equalsIgnoreCase(CST.V_LOCAL))
            return CST.V_LOCAL;
        if (value.equalsIgnoreCase(CST.V_COMPOSITE))
            return CST.V_COMPOSITE;
        if (value.equalsIgnoreCase(CST.V_GLOBAL))
            return CST.V_GLOBAL;
        if (value.equalsIgnoreCase(CST.V_APPLI))
            return CST.V_APPLI;

        return null;
    }
}
