package fr.imag.adele.apam.apformipojo;

import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionHandler;
import fr.imag.adele.apam.apformipojo.handlers.MessageProviderHandler;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.util.CoreMetadataParser;
import fr.imag.adele.apam.util.CoreMetadataParser.IntrospectionService;
import fr.imag.adele.apam.util.CoreParser;

public abstract class ApformIpojoComponent extends ComponentFactory implements IntrospectionService, CoreParser.ErrorHandler {

	public ApformIpojoComponent(BundleContext context, Element element) throws ConfigurationException {
		super(context,element);
        apamTracker = new ApamTracker(context);
	}

	/**
	 * The name space of this factory
	 */
	public static final String APAM_NAMESPACE = "fr.imag.adele.apam";
	/**
	 * Configuration property to specify the component declaration
	 */
	public static final String COMPONENT_DECLARATION_PROPERTY = "declaration";

	/**
	 * The associated declaration of this component
	 */
	private ComponentDeclaration declaration;
	
	/**
	 * Get the declaration of this component if available
	 */
	public ComponentDeclaration getDeclaration() {
		return declaration;
	}
	
	/**
	 * Defines the implementation description.
	 * 
	 * @see ComponentTypeDescription
	 */
	protected static class Description extends ComponentTypeDescription {
	
	
	    /**
	     * Creates the Apam Implementation Description.
	     */
	    protected Description(ApformIpojoComponent factory) {
	        super(factory);
	
	        for (InterfaceReference providedInterface : getFactory().getDeclaration().getProvidedResources(InterfaceReference.class)) {
	            addProvidedServiceSpecification(providedInterface.getJavaType());
	        }
	
	    }
	
	    /**
	     * Gets the attached factory.
	     * 
	     * Redefines with covariant result type.
	     **/
	    @Override
	    public ApformIpojoComponent getFactory() {
	        return (ApformIpojoComponent) super.getFactory();
	    }
	
	    /**
	     * Gets the component type description.
	     */
	    @Override
	    public Element getDescription() {
	
	        Element description = super.getDescription();
	
	        if (getFactory().getDeclaration() != null) {
	        	
	        	ComponentDeclaration declaration = getFactory().getDeclaration();
	        	
	        	Element componentDescription = new Element(COMPONENT_DECLARATION_PROPERTY, APAM_NAMESPACE);
	        	componentDescription.addAttribute(new Attribute("name",declaration.getName()));
	        	componentDescription.addAttribute(new Attribute("type",declaration.getClass().getSimpleName()));

	        	if (declaration instanceof ImplementationDeclaration) {
	        		ImplementationDeclaration implementation = (ImplementationDeclaration) declaration;
	        		if (implementation.getSpecification() != null ) {
	        			componentDescription.addAttribute(new Attribute("specification",implementation.getSpecification().getName()));
	        		}
	        	}

	        	if (declaration instanceof CompositeDeclaration) {
	        		CompositeDeclaration composite = (CompositeDeclaration) declaration;
	        		if (composite.getSpecification() != null ) {
	        			componentDescription.addAttribute(new Attribute("main",composite.getMainImplementation().getName()));
	        		}
	        	}

	        	if (declaration instanceof InstanceDeclaration) {
	        		InstanceDeclaration instance = (InstanceDeclaration) declaration;
	        		if (instance.getImplementation() != null ) {
	        			componentDescription.addAttribute(new Attribute("implementation",instance.getImplementation().getName()));
	        		}
	        	}

	        	Element providesDescription = new Element("provides", APAM_NAMESPACE);;
	        	for (ResourceReference resource : declaration.getProvidedResources()) {
					Element provideDescription = new Element("provides", APAM_NAMESPACE);
					provideDescription.addAttribute(new Attribute("name", resource.getJavaType()));
					providesDescription.addElement(provideDescription);
				}
	        	componentDescription.addElement(providesDescription);
	        	
	        	Element dependenciesDescription = new Element("dependencies", APAM_NAMESPACE);;
	        	for (DependencyDeclaration dependencyDeclaration : declaration.getDependencies()) {
					Element dependencyDescription = new Element("dependency", APAM_NAMESPACE);
					dependencyDescription.addAttribute(new Attribute("id", dependencyDeclaration.getIdentifier()));
					dependencyDescription.addAttribute(new Attribute("resource", dependencyDeclaration.getTarget().toString()));
					dependencyDescription.addAttribute(new Attribute("multiple", Boolean.toString(dependencyDeclaration.isMultiple())));
					
					
					Element injectionsDescription = new Element("injections", APAM_NAMESPACE);
					for (DependencyInjection injectionDeclaration : dependencyDeclaration.getInjections()) {
						Element injectionDescription = new Element("injection", APAM_NAMESPACE);
						injectionDescription.addAttribute(new Attribute("name", injectionDeclaration.getName()));
						injectionDescription.addAttribute(new Attribute("resource", injectionDeclaration.getResource().toString()));
						injectionDescription.addAttribute(new Attribute("multiple", Boolean.toString(injectionDeclaration.isCollection())));
						injectionsDescription.addElement(injectionDescription);
					}
					dependencyDescription.addElement(injectionsDescription);
					
					Element constraintsDescription = new Element("constraints", APAM_NAMESPACE);
					for (String constraint : dependencyDeclaration.getImplementationConstraints()) {
						Element constraintDescription = new Element("implementation", APAM_NAMESPACE);
						constraintDescription.addAttribute(new Attribute("filter", constraint));
						constraintsDescription.addElement(constraintDescription);
					}
					for (String constraint : dependencyDeclaration.getInstanceConstraints()) {
						Element constraintDescription = new Element("instance", APAM_NAMESPACE);
						constraintDescription.addAttribute(new Attribute("filter", constraint));
						constraintsDescription.addElement(constraintDescription);
					}
					dependencyDescription.addElement(constraintsDescription);
					
					Element preferencesDescription = new Element("preferences", APAM_NAMESPACE);
					int priority=0;
					for ( String preference : dependencyDeclaration.getImplementationPreferences()) {
						Element preferenceDescription = new Element("implementation", APAM_NAMESPACE);
						preferenceDescription.addAttribute(new Attribute("filter", preference));
						preferenceDescription.addAttribute(new Attribute("priority", Integer.toString(priority++)));
						preferencesDescription.addElement(preferenceDescription);
					}
					
					priority=0;
					for (String preference : dependencyDeclaration.getInstancePreferences()) {
						Element preferenceDescription = new Element("instance", APAM_NAMESPACE);
						preferenceDescription.addAttribute(new Attribute("filter", preference));
						preferenceDescription.addAttribute(new Attribute("priority", Integer.toString(priority++)));
						preferencesDescription.addElement(preferenceDescription);
					}
					dependencyDescription.addElement(preferencesDescription);
					
					dependenciesDescription.addElement(dependencyDescription);
				}
	        	componentDescription.addElement(dependenciesDescription);
	        	
	        	Element definitionsDescription = new Element("definitions", APAM_NAMESPACE);;
	        	for (PropertyDefinition propertyDeclaration : declaration.getPropertyDefinitions()) {
					Element definitionDescription = new Element("property", APAM_NAMESPACE);
					definitionDescription.addAttribute(new Attribute("name", propertyDeclaration.getName()));
					definitionDescription.addAttribute(new Attribute("type", propertyDeclaration.getType()));
					if (propertyDeclaration.getDefaultValue() != null)
						definitionDescription.addAttribute(new Attribute("value", propertyDeclaration.getDefaultValue().toString()));
					definitionsDescription.addElement(definitionDescription);
				}
	        	componentDescription.addElement(definitionsDescription);
	        	
	        	Element propertiesDescription = new Element("properties", APAM_NAMESPACE);;
	        	for (Entry<String,Object> propertyEntry : declaration.getProperties().entrySet()) {
					Element propertyDescription = new Element("property", APAM_NAMESPACE);
					propertyDescription.addAttribute(new Attribute("name", propertyEntry.getKey()));
					if (propertyEntry.getValue() != null)
						propertyDescription.addAttribute(new Attribute("value", propertyEntry.getValue().toString()));
					propertiesDescription.addElement(propertyDescription);
				}
	        	componentDescription.addElement(propertiesDescription);
	        	
	            description.addElement(componentDescription);
	        }
	
	        return description;
	    }
	
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
	 * Whether this component declaration has an associated instrumented class
	 */
	public abstract boolean hasInstrumentedCode();

	/**
	 * Whether this component declaration can be instantiated directly via the iPojo API.
	 */
	public abstract boolean isInstantiable();

    /**
     * Computes required handlers.
     */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List getRequiredHandlerList() {
		List<RequiredHandler> requiredHandlers = (List<RequiredHandler>) super.getRequiredHandlerList();

		/*
		 * APAM uses a single handler to manage several concerns, override default behavior
		 * an register the APAM handler
		 */
		for (Iterator<RequiredHandler> handlers = requiredHandlers.iterator(); handlers.hasNext();) {
			RequiredHandler handlerDescription = handlers.next();
			String namespace = handlerDescription.getNamespace();
			if ( namespace != null && APAM_NAMESPACE.equals(namespace))
				handlers.remove();
		}

		requiredHandlers.add(new RequiredHandler(MessageProviderHandler.NAME, APAM_NAMESPACE));
		requiredHandlers.add(new RequiredHandler(DependencyInjectionHandler.NAME, APAM_NAMESPACE));

		return requiredHandlers;
	}

	/**
	 * Creates a new native APAM instance, if this component represents an instantiable entity.
	 * 
	 * TODO  Notice that for Apam an instance declaration is a kind of component, but from Ipojo point
	 * of view it is a factory, so this may seem misguiding
	 */
	public abstract ApformIpojoInstance createApamInstance(IPojoContext context, HandlerManager[] handlers);
	
	/**
	 * Creates an instance.
	 * This method is called with the monitor lock.
	 * 
	 */
	@Override
	@SuppressWarnings({ "rawtypes" })
	public ComponentInstance createInstance(Dictionary configuration,
			IPojoContext context, HandlerManager[] handlers)
			throws ConfigurationException {

		if (! isInstantiable())
			throw new ConfigurationException(
			"Only APAM instantiable components can be directly instantiated by Ipojo, use instead the APAM API");
		
		/*
		 * Create a native APAM instance and configure it.
		 */
		ApformIpojoInstance instance = createApamInstance(context,handlers);

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
	 * Get reflection information for the loaded implementation class
	 */
	public Class<?> getInstrumentedClass(String classname) throws ClassNotFoundException {
		return getBundleContext().getBundle().loadClass(classname);
	}

	/**
	 * Handle errors in parsing APAM declaration
	 */
	@Override
	public void error(Severity severity, String message) {
		switch (severity) {
		case SUSPECT:
		case WARNING: 
            getLogger().log(Logger.INFO,
                    "Error parsing APAM declaration " + m_componentMetadata + " : " + message);
			break;

		case ERROR:
			throw new IllegalArgumentException("Error parsing APAM declaration "+getFactoryName()+":  "+message);
		}
	}

	/**
	 * Verify implementation declaration
	 */
	@Override
	public void check(Element element) throws ConfigurationException {
	
	    if (hasInstrumentedCode())
	        super.check(element);
	
	    /*
	     *  Parse metadata to get APAM core declaration.
	     *  
	     *  TODO change parser to accept a single declaration instead of a list of
	     *  declarations
	     */
	    try {
		    Element root = new Element("apam",APAM_NAMESPACE);
		    root.addElement(m_componentMetadata);

		    CoreParser parser = new CoreMetadataParser(root, this);
		    List<ComponentDeclaration> declarations = parser.getDeclarations(this);
			declaration = declarations.get(0);
	    }
	    catch (IllegalArgumentException e) {
			throw new ConfigurationException(e.getLocalizedMessage());
		}
	
	}

	/**
	 * A dynamic reference to the APAM platform
	 */
	protected final ApamTracker apamTracker;

	   /**
     * A class to dynamically track the APAM platform. This allows to dynamically register/unregister this
     * component into the platform.
     * 
     * NOTE We implement an static binding policy. Once an Apam platform has been found, it will be used until
     * it is no longer available.
     * 
     * @author vega
     * 
     */
    class ApamTracker extends Tracker {

        private boolean bound;

        public ApamTracker(BundleContext context) {
            super(context, Apam.class.getName(), null);
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
            bindToApam(apam);
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            unbindFromApam((Apam) service);
            ungetService(reference);
            bound = false;
        }

    }

	/**
	 * Register this component with APAM
	 */
	protected abstract void bindToApam(Apam apam) ;

	/**
	 * Unregister this implementation from APAM
	 * 
	 * @param apam
	 */
	protected abstract void unbindFromApam(Apam apam);
	
	/**
	 * Get a reference to APAM
	 */
	public final Apam getApam() {
	    return apamTracker.size() != 0 ? (Apam) apamTracker.getService() : null;
	}


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


}