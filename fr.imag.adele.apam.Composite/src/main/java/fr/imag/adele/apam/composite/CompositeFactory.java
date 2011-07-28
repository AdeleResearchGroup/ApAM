package fr.imag.adele.apam.composite;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.ManagerModel;


public class CompositeFactory extends ComponentFactory  {

	
    /**
     * The name space of this handler
     */
    private final static String     APAM_NAMESPACE						= "fr.imag.adele.apam";

    /**
     * Configuration property to specify the composites's main implementation
     */
    private final static String     COMPONENT_IMPLEMENTATION_PROPERTY	= "implementation";

    /**
     * Configuration property to specify the composites's provided specification
     */
    private final static String     COMPONENT_SPECIFICATION_PROPERTY	= "specification";

   
    /**
     * Defines the composite type description.
     * @see ComponentTypeDescription
     */
    private class ApamCompositeDescription extends ComponentTypeDescription {

       	public static final String PROPERTY_COMPOSITE 						= "apam.composite";
       	public static final String PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION	= "apam.composite.main.implementation";
       	public static final String PROPERTY_COMPOSITE_MAIN_SPECIFICATION	= "apam.composite.main.specification";
       	public static final String PROPERTY_COMPOSITE_MODELS				= "apam.composite.manager.models";
    	
        /**
         * Creates the ApamCompositeDescription.
         * @param factory the factory.
         * @see ComponentTypeDescription#ComponentTypeDescription(Factory)
         */
        public ApamCompositeDescription() {
            super(CompositeFactory.this);
            
            for (String providedInterface : getProvidedInterfaces()) {
            	addProvidedServiceSpecification(providedInterface);
			}
        }

 		@Override @SuppressWarnings({"rawtypes","unchecked"}) public Dictionary getPropertiesToPublish() {
        	Dictionary properties = super.getPropertiesToPublish();
        	properties.put(PROPERTY_COMPOSITE, true);
        	properties.put(PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION, getMainImplementation());
        	properties.put(PROPERTY_COMPOSITE_MAIN_SPECIFICATION, getProvidedSpecification());
        	properties.put(PROPERTY_COMPOSITE_MODELS, getManagerModels());
        	return properties;
        }
		
 		@Override
 		public Element getDescription() {
 			
 			Element description = super.getDescription();
 			
 			Element compositeDescription	= new Element("composite","fr.imag.adele.apam");
 			compositeDescription.addAttribute(new Attribute("implementation", getMainImplementation()));
 			compositeDescription.addAttribute(new Attribute("specification", getProvidedSpecification()));
 			
 			for (ManagerModel managerModel : getManagerModels()) {
				Element modelDescription = new Element("model","fr.imag.adel.apam");
				modelDescription.addAttribute(new Attribute("manager", managerModel.getManagerName()));
				modelDescription.addAttribute(new Attribute("url", managerModel.getURL().toExternalForm()));
				compositeDescription.addElement(modelDescription);
			}
 			description.addElement(compositeDescription);
 			
 			return description;
 		}
		
    }

    
    /**
     * The name of the APAM component for the main instance of the composite
     */
    private String                  apamComponent;

    /**
     * The name of the APAM specification for the main instance of the composite
     */
    private String                  apamSpecification;

    /**
     * The list of provided interfaces for the main instance of the component
     */
    private String[]                apamInterfaces;
    
    /**
     * The list of models associated to this composite
     */
	private List<ManagerModel>		managerModels;

	
    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
	public CompositeFactory(BundleContext context, Element metadata) throws ConfigurationException {
		super(context, metadata);
		
		/*
		 * Get the composite's main component (either an implementation or specification name )
		 */
        apamComponent		= metadata.getAttribute(CompositeFactory.COMPONENT_IMPLEMENTATION_PROPERTY,CompositeFactory.APAM_NAMESPACE);
        apamSpecification	= metadata.getAttribute(CompositeFactory.COMPONENT_SPECIFICATION_PROPERTY,CompositeFactory.APAM_NAMESPACE);

        if (apamComponent == null) 		apamComponent = "";
        if (apamSpecification == null) 	apamSpecification = "";

		/*
		 * Get the composite's provided interfaces
		 */
        apamInterfaces		= ParseUtils.parseArrays(metadata.getElements("Provides")[0].getAttribute("specifications"));

		/*
		 * look for manager models in the root directory of the bundle
		 */
		managerModels = new ArrayList<ManagerModel>();
		
		@SuppressWarnings("unchecked") Enumeration<String> paths = context.getBundle().getEntryPaths("/");
		while (paths.hasMoreElements()) {
			String path = paths.nextElement();
			if (! path.endsWith(".xml"))
				continue;
			
			URL modelURL 		= context.getBundle().getEntry(path);
			String modelName 	= path.substring(0,path.lastIndexOf(".xml"));
			String managerName	= modelName;
			managerModels.add(new ManagerModel(modelName,managerName,modelURL,0));
		}
	}

    /**
     * Check if the metadata are well formed.
     */
	 @Override public void check(Element metadata) throws ConfigurationException {
        String name = metadata.getAttribute("name");
        if (name == null) {
            throw new ConfigurationException("A composite needs a name : " + metadata);
        }

        String implementation = metadata.getAttribute(CompositeFactory.COMPONENT_IMPLEMENTATION_PROPERTY,CompositeFactory.APAM_NAMESPACE);
        String specification = metadata.getAttribute(CompositeFactory.COMPONENT_SPECIFICATION_PROPERTY,CompositeFactory.APAM_NAMESPACE);

        if ((implementation == null) && (specification == null)){
            throw new ConfigurationException("A composite needs to specify the main implementation (by an implementation or specification name) : " + metadata);
        }
        
        /*
         * We reuse iPojo provided service handler metadata, so that APAM composites look as much as
         * possible as iPojo components.
         * 
         * WARNING This is compatible with iPojo 1.8.0 provide service handler, must be verified in
         * case of evolution of the specification.
         * 
         */
        Element[] providedServices = metadata.getElements("Provides");
        if ((providedServices == null) || (providedServices.length == 0) || (providedServices.length > 1)) {
            throw new ConfigurationException("A composite needs to specify a provides clause : " + metadata);
        }
        
        String encodedServiceInterfaces = providedServices[0].getAttribute("specifications");
        if (encodedServiceInterfaces == null) {
            throw new ConfigurationException("A composite needs to specify the provided interfaces in its provide clause : " + metadata);
        }
        
        
    }

	 
    /**
     * Gets the class name.
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
	 @Override public String getClassName() {
		return "apam.composite";
	}
		
    /**
     * Computes the factory name. The factory name is computed from
     * the 'name' attribute.
     */
	 @Override public String getFactoryName() {
		return m_componentMetadata.getAttribute("name");
	}

    /**
     * Gets the version of the component type.
     * @return the version of <code>null</code> if not set.
     * @see org.apache.felix.ipojo.Factory#getVersion()
     */
	 @Override public String getVersion() {
		return m_version;
	}
	
    /**
     * Gets the component type description.
     * @return the component type description
     * @see org.apache.felix.ipojo.ComponentFactory#getComponentTypeDescription()
     */
	 @Override public ComponentTypeDescription getComponentTypeDescription() {
        return this.new ApamCompositeDescription();
    }

    /**
     * Get the composites's main implementation
     */
	public String getMainImplementation() {
		return apamComponent;
	}

	/**
	 * Get the composites's provided specification
	 */
	public String getProvidedSpecification() {
		return apamSpecification;
	}

	/**
	 * Get the composites's provided interfaces
	 */
	public String[] getProvidedInterfaces() {
		return apamInterfaces;
	}

	/**
	 * Get The list of models associated to this composite
	 */
	public List<ManagerModel> getManagerModels()  {
		return managerModels;

	}
	 
    /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * @param config the instance configuration
     * @param context the iPOJO context to use
     * @param handlers the handler array to use
     * @return the new component instance.
     * @throws ConfigurationException if the instance creation failed during the configuration process.
     */
	 @Override @SuppressWarnings({"unchecked","rawtypes"}) public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers) throws ConfigurationException {
		ApplicationInstance instance = new ApplicationInstance(this, context, handlers);
        instance.configure(m_componentMetadata, configuration);
        instance.start();
        return instance;
	}

    /**
     * Reconfigure an existing instance.
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is
     * not consistent with the component type.
     * @throws MissingHandlerException : occurs when an handler is unavailable when creating the instance.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
	 @Override @SuppressWarnings("rawtypes") public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || properties.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        String name = (String) properties.get("name");
        
        ApplicationInstance instance = (ApplicationInstance) m_componentInstances.get(name);
        
        if (instance == null) {
            return; // The instance does not exist.
        }
        
        instance.reconfigure(properties); // re-configure the component
    }
	 
	
    /**
     * Computes the required handler list.
     * @return the required handler list
     */
	 @Override @SuppressWarnings({"rawtypes","unchecked"}) public List getRequiredHandlerList() {
		List requiredHandlers =  new ArrayList(0);
        requiredHandlers.add(new RequiredHandler("architecture","fr.imag.adele.apam"));
        return requiredHandlers;
	}

    /**
     * Start all the instance managers.
     */
    public synchronized void starting() {
        if (m_requiredHandlers.size() != 0) {
            try {
                String filter = "(&(" + Constants.OBJECTCLASS + "=" + HandlerFactory.class.getName() + ")"
                    + "(" + Handler.HANDLER_TYPE_PROPERTY + "=" + CompositeHandler.HANDLER_TYPE + ")" 
                    + "(factory.state=1)"
                    + ")";
                m_tracker = new Tracker(m_context, m_context.createFilter(filter), this);
                m_tracker.open();
            } catch (InvalidSyntaxException e) {
                m_logger.log(Logger.ERROR, "A factory filter is not valid: " + e.getMessage());
                stop();
                return;
            }
        }
    }

    /**
     * Stop all the instance managers.
     */
    public synchronized void stopping() {
        if (m_tracker != null) {
            m_tracker.close();
        }
        m_tracker = null;
    }


}
