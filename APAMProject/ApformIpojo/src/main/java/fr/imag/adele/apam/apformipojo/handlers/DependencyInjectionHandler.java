package fr.imag.adele.apam.apformipojo.handlers;

import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.osgi.service.wireadmin.WireAdmin;

import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;

public class DependencyInjectionHandler extends ApformHandler {
	
	
	/**
	 * The registered name of this iPojo handler
	 */
	public static final String NAME = "injection";

	/**
	 * The WireAdmin reference
	 */
	private WireAdmin wireAdmin;
	
	
	/**
	 * Get the WireAdmin reference
	 */
	public WireAdmin getWireAdmin() {
		return wireAdmin;
	}
	
    /**
     * (non-Javadoc)
     * 
     * @see
     * org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        /*
         * Add interceptors to delegate dependency resolution
         * 
         * NOTE All validations were already performed when validating the
         * factory @see initializeComponentFactory, including initializing
         * unspecified properties with appropriate default values. Here we just
         * assume metadata is correct.
         */

    	if (!(getFactory() instanceof ApformIpojoImplementation))
    		return;

    	ApformIpojoImplementation implementation	= (ApformIpojoImplementation) getFactory();
    	ImplementationDeclaration declaration		= implementation.getDeclaration();
    	
    	if (! (declaration instanceof AtomicImplementationDeclaration))
    		return;
    	
    	AtomicImplementationDeclaration primitive	= (AtomicImplementationDeclaration) declaration;
    	for (DependencyInjection injection : primitive.getDependencyInjections()) {

    		/*
    		 * Get the field interceptor depending on the kind of reference
    		 */
    		
    		InterfaceReference interfaceReference	= injection.getResource().as(InterfaceReference.class);
    		MessageReference messageReference		= injection.getResource().as(MessageReference.class);
    		FieldInterceptor interceptor 			= null;
    		
    		try {
    			
	    		if (interfaceReference != null)
	    			interceptor 	= new InterfaceInjectionManager(getFactory(),getInstanceManager(),injection);
	    		
	    		if (messageReference != null)
	    			interceptor		= new MessageInjectionManager(getFactory(),getInstanceManager(),injection);
	
	    		if (interceptor == null)
	    			continue;
	    		
    		} catch (ClassNotFoundException error) {
    			throw new ConfigurationException("error injecting dependency "+injection.getName()+" :"+error.getLocalizedMessage());
    		}
    		
    		if ( injection instanceof DependencyInjection.Field)  {
    			FieldMetadata field	= getPojoMetadata().getField(injection.getName());
    			if (field != null)
    	   			getInstanceManager().register(field, interceptor);
    		}
    		
		}
    }


    /**
     * The description of this handler instance
     * 
     */
    private static class Description extends HandlerDescription {

        private final DependencyInjectionHandler dependencyHandler;

        public Description(DependencyInjectionHandler dependencyHandler) {
            super(dependencyHandler);
            this.dependencyHandler = dependencyHandler;
        }

        @Override
        public Element getHandlerInfo() {
            Element root = super.getHandlerInfo();

            if (dependencyHandler.getInstanceManager() instanceof ApformIpojoInstance) {
                ApformIpojoInstance instance = (ApformIpojoInstance) dependencyHandler.getInstanceManager();
                for (DependencyInjectionManager dependency : instance.getInjections()) {
                    root.addElement(dependency.getDescription());
                }
            }
            return root;
        }

    }

    @Override
    public HandlerDescription getDescription() {
        return new Description(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public String toString() {
        return "APAM Injection manager for "
                + getInstanceManager().getInstanceName();
    }

}
