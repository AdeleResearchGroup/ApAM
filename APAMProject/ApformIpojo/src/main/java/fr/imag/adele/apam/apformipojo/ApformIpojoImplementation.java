package fr.imag.adele.apam.apformipojo;


import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ImplementationDeclaration;

//import fr.imag.adele.apam.util.Attributes;

public class ApformIpojoImplementation extends ApformIpojoComponent implements ApformImplementation {

    /**
     * The specification provided by this implementation
     */
    private ApformSpecification specification;

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformIpojoImplementation(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
    }

    @Override
    public void check(Element element) throws ConfigurationException {
    	super.check(element);
    	specification = null;
    }
    
    @Override
    public ImplementationDeclaration getDeclaration() {
    	return (ImplementationDeclaration) super.getDeclaration();
    }
    
    /**
     * Get the provided specification representation
     */
    @Override
	public ApformSpecification getSpecification() {
		return specification;
	}

    
    /**
     * Register this implementation with APAM
     */
    protected void bindToApam(Apam apam) {
    	
    	/*
    	 * Cross-reference to provided interface, if already installed in APAM
    	 */
    	if (getDeclaration().getSpecification() != null) {
    		String specName = getDeclaration().getSpecification().getName();
    		Specification provided = CST.SpecBroker.getSpec(specName);
    		if (provided != null && provided.getApformSpec() != null)
    			specification = provided.getApformSpec();
    	}
    	
        Apform2Apam.newImplementation(getName(), this);
    }

    /**
     * Unregister this implementation from APAM
     * 
     * @param apam
     */
    protected void unbindFromApam(Apam apam) {
        Apform2Apam.vanishImplementation(getName());
    }
    

    private final ThreadLocal<Boolean> insideApamCall = new ThreadLocal<Boolean>() {
                                                          @Override
                                                          protected Boolean initialValue() {
                                                              return false;
                                                          };
                                                      };

    private final boolean isApamCall() {
        return insideApamCall.get();
    }

    /**
     * Creates an instance of the implementation, and initialize its properties with the set of
     * provided properties.
     * 
     * NOTE this method is called when an instance is created by the APAM platform (explicitly by
     * the API or implicitly by a dependency resolution)
     */	
    @Override
    public ApformInstance createInstance(Map<String, Object> initialproperties) {
        try {

            ApformIpojoInstance instance = null;

            try {
                insideApamCall.set(true);
                Properties configuration = new Properties();
                if (initialproperties != null)
                    configuration.putAll(initialproperties);
                instance = (ApformIpojoInstance) createComponentInstance(configuration);
            } finally {
                insideApamCall.set(false);
            }

            return instance;

        } catch (Exception cause) {
            throw new IllegalArgumentException(cause);
        }

    }
    
    
	/**
	 * Creates the iPOjo instance corresponding to a newly created native APAM instance.
	 * 
	 * NOTE this method can be called by APAM or from an iPojo instance declaration
	 */ 
	@Override
	public ApformIpojoInstance createApamInstance(IPojoContext context, HandlerManager[] handlers) {
		return new ApformIpojoInstance(this, isApamCall(), context, handlers);
	}

	@Override
	public boolean hasInstrumentedCode() {
		return true;
	}

	@Override
	public boolean isInstantiable() {
		return true;
	}

	@Override
	public void setProperty(String attr, Object value) {
		// TODO change factory publication?
	}
 
}
