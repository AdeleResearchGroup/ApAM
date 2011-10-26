package fr.imag.adele.apam.implementation;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;

/**
 * The base class for all iPojo handlers manipulating APAM implementations and instances
 * @author vega
 *
 */
public abstract class ImplementationHandler extends PrimitiveHandler {

    /**
     * The name space of this handler
     */
    protected final static String     APAM_NAMESPACE                     = "fr.imag.adele.apam";

 
    /**
     * Quote String in message
     */
    protected static final String quote(String arg) {
        return "\"" + arg + "\"";
    }

    /**
     * Utility method to facilitate iteration over possibly null element lists
     */
    protected static final Element[] EMPTY_ELEMENT_LIST = new Element[0];

    protected static final Element[] optional(Element[] elements) {
        return elements != null ? elements : EMPTY_ELEMENT_LIST;
    }

    protected static final boolean isDefined(Element[] elements) {
    	return elements != null && elements.length > 0;
    }

    protected static final boolean isSingleton(Element[] elements) {
    	return elements != null && elements.length == 1;
    }
    
    protected static final Element singleton(Element[] elements) {
        return elements[0];
    }

}
