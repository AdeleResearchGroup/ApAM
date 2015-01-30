package fr.imag.adele.apam.apform.impl;

import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

public abstract class ApamImplementationFactory extends ApamComponentFactory {

	
    public ApamImplementationFactory(BundleContext context, Element element) throws ConfigurationException {
        super(context,element);
    }

	@Override
	protected boolean isInstantiable() {
		return true;
	}

    /**
     * Get the associated Apform component
     */
    public ApformImplementation getApform() {
    	return (ApformImplementation) this.apform;
    }
	
    public ImplementationDeclaration getDeclaration() {
    	return (ImplementationDeclaration)declaration;
    }
    
    /**
     * Register this component with APAM
     */
	protected final void bindToApam(Apam apam) {
        Apform2Apam.newImplementation(getApform());
    }
	
    /**
     * Creates a new native APAM instance, if this component represents an instantiable entity.
     */
    protected ApamInstanceManager createApamInstance(IPojoContext context, HandlerManager[] handlers) {
        return new ApamInstanceManager(this, isCreatedByApam(), context, handlers);
    }
    
    /**
     * Whether the instance is being created using the APAM API or using the iPOJO API
     * 
     * NOTE There are slight difference in both cases that are handled in class ApamInstanceManager.
     * Specifically, when the iPOJO API is used, the component instance in APAM is created asynchronously
     * to avoid blocking the calling thread. 
     */
    private final boolean isCreatedByApam() {
        return createdByApam.get();
    }
    
    /**
     * Thread local variable used to determine which API was used (APAM or directly iPOJO)
     */
    private final ThreadLocal<Boolean> createdByApam = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        };
    };
    
    /**
     * Must be used by subclasses to signal that the APAM API is used
     */
    private final void beginApamCreation() {
    	createdByApam.set(true);
    }
    
    /**
     * Must be used by subclasses to signal that the APAM API is used
     */
    private final void endApamCreation() {
    	createdByApam.set(false);
    }

	
	/**
	 * This class represents the base functionality of Apform mediation object between APAM and a implementation factory
	 */
	protected abstract class Apform<I extends Implementation, D extends ImplementationDeclaration> extends ApamComponentFactory.Apform<I,D> implements ApformImplementation {

		public Apform() {
			super();
		}
		
	    /**
		 * Creates an instance of the factory, and initialize its properties with the set of
		 * provided properties.
		 * 
		 * NOTE this method is called when an instance is created by the APAM platform (explicitly
		 * by the API or implicitly by a relation resolution)
		 * 
		 */
	    @Override
		public ApformInstance createInstance(Map<String, String> initialProperties)	throws InvalidConfiguration {
	        try {

	            ApamInstanceManager instance = null;

	            try {
	            	beginApamCreation();
	                instance = (ApamInstanceManager) createComponentInstance(initializeConfiguration(initialProperties));
	            } finally {
	            	endApamCreation();
	            }

	            return instance.getApform();

	        } catch (Exception cause) {
	            throw new ComponentImpl.InvalidConfiguration(cause);
	        }
		}
	    
		/**
		 * Creates an instance of the factory, and register it directly with APAM.
		 * 
		 * This method can be used by external services (like device discovery protocols) to create instances
		 * in APAM that are not the result of a resolution.
		 */
		@Override
		public ApformInstance addDiscoveredInstance(Map<String,Object> initialProperties) throws InvalidConfiguration, UnsupportedOperationException {
			try {
				ApamInstanceManager instance = (ApamInstanceManager) createComponentInstance(initializeConfiguration(initialProperties));
				return instance.getApform();

			} catch (Exception cause) {
				throw new ComponentImpl.InvalidConfiguration(cause);
			}
		}	    
	    
		/**
		 * Handle special properties initialized in the configuration
		 */
		private Properties initializeConfiguration(Map<String,?> initialProperties) {
			
			Properties configuration = new Properties();
			
			if (initialProperties == null)
				return configuration;
			
			configuration.putAll(initialProperties);
			
			/*
			 * Get the name of the component from the initial properties, and translate to
			 * the iPOJO convention
			 */
			Object name = configuration.remove(CST.NAME);
			if (name != null) {
				configuration.put(Factory.INSTANCE_NAME_PROPERTY, name);
			}
			
			return configuration;
		}
	}
	
}
