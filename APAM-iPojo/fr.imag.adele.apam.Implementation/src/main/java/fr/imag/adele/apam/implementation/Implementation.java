package fr.imag.adele.apam.implementation;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl.DependencyModel;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apformAPI.Apform2Apam;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;
import fr.imag.adele.apam.instance.Instance;
import fr.imag.adele.apam.util.Attributes;

public class Implementation  extends ComponentFactory implements ApformImplementation {

    /**
     * The name space of this factory
     */
    public final static String APAM_NAMESPACE						= "fr.imag.adele.apam";

    /**
     * Configuration property to specify the implementations provided specification
     */
    public final static String COMPONENT_SPECIFICATION_PROPERTY		= "specification";

    /**
     * Configuration element to handle APAM dependencies
     */
    public final static String     DEPENDENCY_DECLARATION			= "dependency";

    /**
     * Defines the implementation description.
     * 
     * @see ComponentTypeDescription
     */
    public static class Description extends ComponentTypeDescription {
    	
    	/**
    	 * The implementation dependency model
    	 */
    	private Set<DependencyModel> dependencies;

        /**
         * Creates the Apam Implementation Description.
         */
        protected Description(Implementation factory) {
            super(factory);
            
            this.dependencies = new HashSet<DependencyModel>();
        }

        /**
         * Gets the attached factory.
         * 
      	 * Redefines with covariant result type.
         **/
        @Override
        public Implementation getFactory() {
        	return (Implementation) super.getFactory();
        }
        
        /**
         * Return the specification provided by this implementation
         */
        public String getSpecification() {
        	return getFactory().getProvidedSpecification();
        }
        
        /**
         * Adds a new dependency model to this description
         */
        public void addDependency(DependencyModel dependency) {
        	dependencies.add(dependency);
        }
        
        /**
         * Get the dependency model associated to this implementation
         */
        public Set<DependencyModel> getDependencies() {
        	return dependencies;
        }
        
        /**
         * Computes the default service properties to publish the factory.
         */
		@Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Dictionary getPropertiesToPublish() {

        	Dictionary properties = super.getPropertiesToPublish();
        	
        	/*
        	 * Add the Apam specific properties
        	 */
        	properties.put(CST.A_DEPENDENCIES, getDependencies());
        	if (getSpecification() != null) 
        		properties.put(CST.A_APAMSPECNAME,getSpecification());
        	
        	return properties;
        }
        
	    /**
	     * Gets the component type description.
	     */
        @Override
        public Element getDescription() {

        	Element description =  super.getDescription();
        	
        	if (getSpecification() != null)
        		description.addAttribute(new Attribute(COMPONENT_SPECIFICATION_PROPERTY, getSpecification()));
        	
        	for (DependencyModel dependency : getDependencies()) {
				Element dependencyDescription = new Element(DEPENDENCY_DECLARATION,APAM_NAMESPACE);
				dependencyDescription.addAttribute(new Attribute("name",dependency.dependencyName));
				dependencyDescription.addAttribute(new Attribute("kind",dependency.targetKind.toString()));
				dependencyDescription.addAttribute(new Attribute("target",dependency.target));
				dependencyDescription.addAttribute(new Attribute("multiplicity",Boolean.toString(dependency.isMultiple)));
				description.addElement(dependencyDescription);
			}
        	return description;
        }
        
    }

    
    /**
     * The specification implemented by this implementation
     */
    protected String m_specification;
    
    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public Implementation(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
    }

    /**
     * Whether this implementation has an associated instrumented class
     */
    public boolean hasInstrumentedCode() {
    	return true;
    }
    
   
    /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * 
     */
    @Override
    @SuppressWarnings({"rawtypes" })
    public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers)
            throws ConfigurationException {
    	
    	if (! hasInstrumentedCode())
    		throw new ConfigurationException("Only APAM implementations with instrumented code can be directly instantiated;  To create specifications and composites use instead the APAM API");
    	
    	/*
    	 * Create a native APAM instance and configure it.
    	 * 
    	 */
        Instance instance = new Instance(this, isApamCall(), context, handlers);

        try {
            instance.configure(m_componentMetadata, configuration);
            instance.start();
            return instance;
        } catch (ConfigurationException e) {
            // An exception occurs while executing the configure or start
            // methods.
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
            throw e;
        } catch (Throwable e) { // All others exception are handled here.
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
            m_logger.log(Logger.ERROR, e.getMessage(), e);
            throw new ConfigurationException(e.getMessage());
        }
    }

     
    /**
     * Verify implementation declaration
     */
    @Override
    public void check(Element element) throws ConfigurationException {

    	if (hasInstrumentedCode())
    		super.check(element);
    	
    	if (getFactoryName() == null)
        	throw new ConfigurationException("An implementation needs a name : " + element);
    		
    	m_specification = element.getAttribute(COMPONENT_SPECIFICATION_PROPERTY);
    }
    
    /**
     * Get the name of the specification provided by this implementation
     */
    public String getProvidedSpecification() {
    	return m_specification;
    }
    
    /**
     * Gets the component type description.
     * 
     * @return the component type description
     * @see org.apache.felix.ipojo.ComponentFactory#getComponentTypeDescription()
     */
    @Override
    public ComponentTypeDescription getComponentTypeDescription() {
        return new Description(this);
    }

    /**
     * Register this implementation with APAM
     */
	private void bindToApam(Apam apam) {
		CST.apform2Apam.newImplementation(getName(),this);
	}

	/**
	 * Unregister this implementation from APAM
	 * @param apam
	 */
	private void unbindFromApam(Apam apam) {
		CST.apform2Apam.vanishImplementation(getName());
	}

	/**
	 * Get a reference to APAM
	 */
	public final Apam getApam() {
		return apamTracker.size() != 0 ? (Apam) apamTracker.getService() : null;
	}
	
	public final Apform2Apam getApamPlatform() {
		return getApam() != null ? CST.apform2Apam : null;
	}
	
	/**
	 * Apform: get the list of interfaces 
	 */
	@Override
	public String[] getInterfaceNames() {
		return getComponentDescription().getprovidedServiceSpecification();
	}

	/**
	 * Apform: get the list of properties of the implementation
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getProperties() {
		/*
		 * TODO We assume the properties to publish is a Properties object, and cast it directly to a Map.
		 * subclasses must be careful not to break this assumption.	 
		 */
		return(Map<String, Object>)getComponentDescription().getPropertiesToPublish();
	}

	/**
	 *  Apform: get a property of the implementation
	 */
	@Override
	public Object getProperty(String key) {
		return getComponentDescription().getPropertiesToPublish().get(key);
	}


	private ThreadLocal<Boolean> insideApamCall = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() { return false; };
	};

	private final boolean isApamCall()  {
		return insideApamCall.get();
	}
	
	/**
	 *  Apform: create an instance
	 */
	@Override
	public ApformInstance createInstance(Attributes initialproperties) {
		try {
			
			Instance instance = null;
			
			try {
				insideApamCall.set(true);
				Properties configuration = initialproperties != null ? initialproperties.attr2Properties() : new Properties();
				instance = (Instance) createComponentInstance(configuration);
			}
			finally {
				insideApamCall.set(false);
			}
			
			return instance;
		
		} catch (Exception cause) {
			throw new IllegalArgumentException(cause);
		}
		
	}

	/**
	 *  Apform: get the provided specification
	 */
	@Override
	public ApformSpecification getSpecification() {
		return null;
	}


    /**
     * A dynamic reference to the APAM platform
     */
    private ApamTracker apamTracker;

    /**
     * Once the factory is started register it in APAM
     */
    @Override
    public synchronized void start() {
    	super.start();
    	apamTracker.open();
    }
    
    /**
     * Once the factory is stopped unregister it from APAM
     */
    @Override
    public synchronized void stop() {
    	super.stop();
    	apamTracker.close();
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
    private class ApamTracker extends Tracker {

    	private boolean bound;
    	
    	public ApamTracker(BundleContext context) {
    		super(context,Apam.class.getName(),null);
    		bound = false;
    	}
    	
    	@Override
    	public boolean addingService(ServiceReference reference) {
    		return !bound;
    	}
    	
    	@Override
    	public void addedService(ServiceReference reference) {
       		bound = true;
   			Apam apam = (Apam) getService(reference);
       		Implementation.this.bindToApam(apam);
    	}
    	
		@Override
    	public void removedService(ServiceReference reference, Object service) {
   			Implementation.this.unbindFromApam((Apam) service);
   			ungetService(reference);
   			bound = false;
    	}
		
    	
     }
}
