package fr.imag.adele.apam.specification;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class Specification  extends ComponentFactory {

    /**
     * Defines the specification description.
     * 
     * @see ComponentTypeDescription
     */
    private class Description extends ComponentTypeDescription {

        /**
         * Creates the ApamSpecificationDescription.
         * 
         * @param factory the factory.
         * @see ComponentTypeDescription#ComponentTypeDescription(Factory)
         */
        public Description() {
            super(Specification.this);           
        }
        
        /**
         * Computes the default service properties to publish this factory.
         * 
         * @see : ComponentTypeDescription#getPropertiesToPublish(Factory)
         */

        @Override
        @SuppressWarnings({"rawtypes", "unchecked" })
        public Dictionary getPropertiesToPublish() {
        	Dictionary properties = super.getPropertiesToPublish();
        	properties.put("factory.abstract", "true");
            return properties;       
        }
        

    }

    /**
     * A class to dynamically track the APAM platform. This allows to dynamically register/unregister this
     * specification into the platform.
     * 
     * NOTE We implement an static binding policy. Once an Apam platform has been found, it will be used until
     * it is no longer available.
     * 
     * @author vega
     *
     */
    private class ApamReference extends Tracker {

    	private Apam apam;
    	
    	public ApamReference(BundleContext context) {
    		super(context,Apam.class.getName(),null);
    	}
    	
    	@Override
    	public void addedService(ServiceReference reference) {
    		if (apam == null) {
    			apam = (Apam) this.getService(reference);
        		apamBound(apam);
    		}
    	}
    	
    	@Override
    	public void removedService(ServiceReference reference, Object service) {
    		if (apam != null) {
    			apamUnbound(apam);
    			apam = null;
    			this.ungetService(reference);
    		}
    	}
    	
     }

    /**
     * A dynamic reference to the APAM platform
     */
    private ApamReference apam;
    
    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public Specification(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
        apam = new ApamReference(context);
    }

    /**
     * Check if the metadata are well formed.
     */
    @Override
    public void check(Element metadata) throws ConfigurationException {
        String name = metadata.getAttribute("name");
        if (name == null || name.trim().length() == 0) {
            throw new ConfigurationException("An specification needs a name : " + metadata);
        }
    }

    private void apamBound(Apam apam) {
    	// TODO migrate this to be an abstract implementation
    	//ASMSpecBroker broker = apam.getSpecBroker();
    	//ASMSpec specAPAM = broker.getSpec(getName());
    	
    	//if (specAPAM == null) {
    		
    		ComponentTypeDescription description = getComponentDescription();
    		
    		Attributes attributes = new AttributesImpl();
    		for (PropertyDescription property : description.getProperties()) {
                if (property.isImmutable() && property.getValue() != null) {
                	attributes.setProperty(property.getName(), property.getObjectValue(getBundleContext()));
                }
    		}
    		//specAPAM = broker.createSpec(getName(),description.getprovidedServiceSpecification(),attributes);
    	//}
    		
    }
    
    private void apamUnbound(Apam apam) {
    }
    
    
    /**
     * Starts the factory.
     * This method is called when holding the monitor lock.
     */
    public void starting() {
    	super.starting();
    	apam.open();
    }

    /**
     * Stops all the instance managers.
     * This method is called when holding the lock.
     */
    public void stopping() {
    	super.stopping();
    	apam.close();
    }
    
 
    /**
     * Gets the class name.
     * 
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return "apam.specification";
    }

    /**
     * Computes the factory name. The factory name is computed from
     * the 'name' attribute.
     */
    @Override
    public String getFactoryName() {
        return m_componentMetadata.getAttribute("name");
    }

    /**
     * Gets the version of the component type.
     * 
     * @return the version of <code>null</code> if not set.
     * @see org.apache.felix.ipojo.Factory#getVersion()
     */
    @Override
    public String getVersion() {
        return m_version;
    }

    /**
     * Gets the component type description.
     * 
     * @return the component type description
     * @see org.apache.felix.ipojo.ComponentFactory#getComponentTypeDescription()
     */
    @Override
    public ComponentTypeDescription getComponentTypeDescription() {
        return this.new Description();
    }


   /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * 
     * @param config the instance configuration
     * @param context the iPOJO context to use
     * @param handlers the handler array to use
     * @return the new component instance.
     * @throws ConfigurationException if the instance creation failed during the configuration process.
     */
    @Override
    @SuppressWarnings({"rawtypes" })
    public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers)
            throws ConfigurationException {
        throw new ConfigurationException("APAM specifications can not be directly instantiated");
    }

    /**
     * Reconfigure an existing instance.
     * 
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is
     *             not consistent with the component type.
     * @throws MissingHandlerException : occurs when an handler is unavailable when creating the instance.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration,
            MissingHandlerException {
        throw new UnacceptableConfiguration("APAM specifications can not be directly instantiated");
    }
    
}
