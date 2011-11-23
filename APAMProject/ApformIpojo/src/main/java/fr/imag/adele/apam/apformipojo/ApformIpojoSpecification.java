package fr.imag.adele.apam.apformipojo;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformSpecification;

public class ApformIpojoSpecification extends ApformIpojoImplementation implements ApformSpecification {

    /**
     * Configuration property to specify the specification's provided interfaces
     */
    private final static String SPECIFICATION_INTERFACES_PROPERTY 		= "interfaces";

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformIpojoSpecification(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        /*
         * Get the specification's provided interfaces
         */
        providedInterfaces = ParseUtils.parseArrays(metadata.getAttribute(SPECIFICATION_INTERFACES_PROPERTY));
    }

    /**
     * This factory doesn't have an associated instrumented class
     */
    @Override
    public boolean hasInstrumentedCode() {
    	return false;
    }
 
    /**
     * Whether this implementation is an abstract specification
     */
    public boolean isAbstract() {
    	return true;
    }

    /**
     * Check if the metadata are well formed.
     */
    @Override
    public void check(Element metadata) throws ConfigurationException {
    	
    	super.check(metadata);
    	
    	/*
    	 * composite provided interfaces are optional
    	 */
        String encodedInterfaces = metadata.getAttribute(SPECIFICATION_INTERFACES_PROPERTY);
        if (encodedInterfaces == null) {
        	metadata.addAttribute(new Attribute(SPECIFICATION_INTERFACES_PROPERTY,null,""));
        }

    }

    /**
     * Gets the class name.
     * 
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    /**
     * Register this implementation with APAM
     */
    @Override
    protected void bindToApam(Apam apam) {
        Apform2Apam.newSpecification(getName(), this);
    }

    /**
     * Unregister this implementation from APAM
     * 
     * @param apam
     */
    @Override
    protected void unbindFromApam(Apam apam) {
        Apform2Apam.vanishSpecification(getName());
    }
    
}
