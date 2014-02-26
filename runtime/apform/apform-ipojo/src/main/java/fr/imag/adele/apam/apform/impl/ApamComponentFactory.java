/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform.impl;

import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.apform.impl.handlers.MessageProviderHandler;
import fr.imag.adele.apam.apform.impl.handlers.PropertyInjectionHandler;
import fr.imag.adele.apam.apform.impl.handlers.RelationInjectionHandler;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.util.CoreMetadataParser;
import fr.imag.adele.apam.util.CoreMetadataParser.IntrospectionService;
import fr.imag.adele.apam.util.CoreParser;

/**
 * This is the base class for all component factories that are used to represent APAM components at the iPojo
 * level.
 * 
 * @author vega
 *
 */
public abstract class ApamComponentFactory extends ComponentFactory implements IntrospectionService, CoreParser.ErrorHandler {

    /**
     * The name space of this factory
     */
    public static final String APAM_NAMESPACE = "fr.imag.adele.apam";
    /**
     * Configuration property to specify the component declaration
     */
    public static final String COMPONENT_DECLARATION_PROPERTY = "declaration";

	/**
     * A dynamic reference to the APAM platform
     */
    protected final ServiceTracker apamTracker;
    
    /**
     * The corresponding component declaration
     */
    protected ComponentDeclaration  declaration;

    /**
     * If the declaration can not be loaded this is the cause
     */
    protected ConfigurationException  declarationError;
    
	/**
	 * The associated Apform component
	 */
	protected final ApformComponent 	apform;
    
    /**
     * Initializes an APAM component factory
     */
    public ApamComponentFactory(BundleContext context, Element element) throws ConfigurationException {
        super(context,element);
        this.apamTracker	= new ApamTracker(context);
        this.apform			= createApform();

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

    /**
     * Creates the Apform object used to mediate between APAM and this factory
     */
    protected abstract ApformComponent createApform();
    
	/**
	 * This class represents the base functionality of Apform mediation object between APAM and a component factory
	 */
	protected abstract class Apform<C extends Component, D extends ComponentDeclaration> extends BaseApformComponent<C,D>  {

		@SuppressWarnings("unchecked")
		public Apform() {
			super( (D) ApamComponentFactory.this.declaration);
		}
		
		@Override
		public Bundle getBundle() {
			return ApamComponentFactory.this.getBundleContext().getBundle();
		}
	}
	
    /**
     * Get the associated Apform component
     */
    public ApformComponent getApform() {
    	return apform;
    }
    
    /**
     * Get the associated declaration
     */
    public ComponentDeclaration getDeclaration() {
		return declaration;
	}
    
    /**
     * Register this component with APAM
     */
	protected abstract void bindToApam(Apam apam);

    /**
     * Unregister this component from APAM
     *
     * @param apam
     */
	protected void unbindFromApam(Apam apam) {
		((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(getName());
    }

    /**
     * Whether this component factory has an associated instrumented class
     */
    protected abstract boolean hasInstrumentedCode();

    /**
     * Computes required handlers.
     */
	@Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List getRequiredHandlerList() {

    	List<RequiredHandler> requiredHandlers = (List<RequiredHandler>) super.getRequiredHandlerList();

        /*
         * APAM do not use a handler for each element in the metadata, so we need to override the default
         * behavior
         */
        for (Iterator<RequiredHandler> handlers = requiredHandlers.iterator(); handlers.hasNext();) {
            RequiredHandler handlerDescription = handlers.next();
            String namespace = handlerDescription.getNamespace();
            if ( namespace != null && APAM_NAMESPACE.equals(namespace))
                handlers.remove();
        }
    	
        /*
         *  Parse metadata to get APAM core declaration
         *
         *  TODO change parser to accept a single declaration instead of a list of
         *  declarations
         */
		try {
			Element root = new Element("apam", APAM_NAMESPACE);
			root.addElement(m_componentMetadata);

			CoreParser parser = new CoreMetadataParser(root, this);
			List<ComponentDeclaration> declarations = parser.getDeclarations(this);
			this.declaration = declarations.get(0);

		} catch (Exception e) {
			e.printStackTrace();
			this.declaration		= null;
			this.declarationError 	= new ConfigurationException(e.getLocalizedMessage());
			return requiredHandlers;
		}

		/*
		 * Calculate the minimal set of handlers based on the component declaration
		 */
		if (this.declaration instanceof AtomicImplementationDeclaration) {
			AtomicImplementationDeclaration componentDeclaration = (AtomicImplementationDeclaration) this.declaration;

			if (MessageProviderHandler.isRequired(componentDeclaration))
				requiredHandlers.add(new RequiredHandler(MessageProviderHandler.NAME, APAM_NAMESPACE));

			if (RelationInjectionHandler.isRequired(componentDeclaration))
				requiredHandlers.add(new RequiredHandler(RelationInjectionHandler.NAME, APAM_NAMESPACE));
			
			if (PropertyInjectionHandler.isRequired(componentDeclaration))
				requiredHandlers.add(new RequiredHandler(PropertyInjectionHandler.NAME, APAM_NAMESPACE));
		}

        return requiredHandlers;
    }

    /**
     * Verify implementation declaration
     */
    @Override
    public void check(Element element) throws ConfigurationException {
        if (hasInstrumentedCode())
            super.check(element);
        
        if (this.declaration == null)
        	throw this.declarationError;
    }

    /**
     * Handle errors in parsing APAM declaration
     */
    @Override
    public void error(Severity severity, String message) {
        switch (severity) {
            case SUSPECT:
            case WARNING:
                getLogger().log(Logger.INFO,"Error parsing APAM declaration " + m_componentMetadata + " : " + message);
                break;

            case ERROR:
                throw new IllegalArgumentException("Error parsing APAM declaration "+getFactoryName()+":  "+message);
        }
    }


    /**
     * Whether this component factory can be instantiated directly via the iPojo API.
     */
    protected abstract boolean isInstantiable();
   

    /**
     * Creates a primitive instance.
     * This method is called when holding the lock.
     *
     * NOTE In APAM component factories we override definitively this method to be sure that the created instance
     * is an implementation of ApamInstanceManager. 
     * 
     * Subclasses should instead override {@link #createApamInstance(IPojoContext, HandlerManager[])} in order to
     * specialize instance creation. 
     */
    @Override
    @SuppressWarnings({ "rawtypes" })
    public final ApamInstanceManager createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers)
            throws ConfigurationException {

        if (! isInstantiable())
            throw new ConfigurationException(
                    "Only APAM instantiable components can be directly instantiated by Ipojo, use instead the APAM API");

        /*
           * Create a native APAM instance and configure it.
           */
        ApamInstanceManager instance = createApamInstance(context,handlers);

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
        } catch (Exception e) { // All others exception are handled here.
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
            m_logger.log(Logger.ERROR, e.getMessage(), e);
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
     * Creates a new native APAM instance, if this component represents an instantiable entity.
     */
    protected abstract ApamInstanceManager createApamInstance(IPojoContext context, HandlerManager[] handlers);
    
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
     * Get reflection information for the loaded implementation class
     */
    public Class<?> getInstrumentedClass(String classname) throws ClassNotFoundException {
        return getBundleContext().getBundle().loadClass(classname);
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
        protected Description(ApamComponentFactory factory) {
            super(factory);

            /*
             * add all provided interfaces of the component to the description
             */
            for (InterfaceReference providedInterface : factory.declaration.getProvidedResources(InterfaceReference.class)) {
                addProvidedServiceSpecification(providedInterface.getJavaType());
            }

        }

        /**
         * Gets the attached factory.
         *
         * Redefines with covariant result type.
         **/
        @Override
        public ApamComponentFactory getFactory() {
            return (ApamComponentFactory) super.getFactory();
        }

        /**
         * Gets the component type description.
         */
        @Override
        public Element getDescription() {

            Element description = super.getDescription();

            if (getFactory().declaration != null) {

                ComponentDeclaration declaration = getFactory().declaration;

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
                    if (composite.getSpecification() != null && composite.getMainComponent() != null) {
                        componentDescription.addAttribute(new Attribute("main",composite.getMainComponent().getName()));
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
                    provideDescription.addAttribute(new Attribute("resource", resource.toString()));
                    providesDescription.addElement(provideDescription);
                }
                componentDescription.addElement(providesDescription);

                Element relationsDescription = new Element("dependencies", APAM_NAMESPACE);;
                for (RelationDeclaration relationDeclaration : declaration.getDependencies()) {
                    Element relationDescription = new Element("dependency", APAM_NAMESPACE);
                    relationDescription.addAttribute(new Attribute("id", relationDeclaration.getIdentifier()));
                    relationDescription.addAttribute(new Attribute("resource", relationDeclaration.getTarget().toString()));
                    relationDescription.addAttribute(new Attribute("multiple", Boolean.toString(relationDeclaration.isMultiple())));


                    Element injectionsDescription = new Element("instrumentations", APAM_NAMESPACE);
                    for (RequirerInstrumentation injectionDeclaration : relationDeclaration.getInstrumentations()) {
                        Element injectionDescription = new Element("instrumentation", APAM_NAMESPACE);
                        injectionDescription.addAttribute(new Attribute("name", injectionDeclaration.getName()));
                        injectionDescription.addAttribute(new Attribute("resource", injectionDeclaration.getRequiredResource().toString()));
                        injectionDescription.addAttribute(new Attribute("multiple", Boolean.toString(injectionDeclaration.acceptMultipleProviders())));
                        injectionsDescription.addElement(injectionDescription);
                    }
                    relationDescription.addElement(injectionsDescription);

                    Element constraintsDescription = new Element("constraints", APAM_NAMESPACE);
                    for (String constraint : relationDeclaration.getImplementationConstraints()) {
                        Element constraintDescription = new Element("implementation", APAM_NAMESPACE);
                        constraintDescription.addAttribute(new Attribute("filter", constraint));
                        constraintsDescription.addElement(constraintDescription);
                    }
                    for (String constraint : relationDeclaration.getInstanceConstraints()) {
                        Element constraintDescription = new Element("instance", APAM_NAMESPACE);
                        constraintDescription.addAttribute(new Attribute("filter", constraint));
                        constraintsDescription.addElement(constraintDescription);
                    }
                    relationDescription.addElement(constraintsDescription);

                    Element preferencesDescription = new Element("preferences", APAM_NAMESPACE);
                    int priority=0;
                    for ( String preference : relationDeclaration.getImplementationPreferences()) {
                        Element preferenceDescription = new Element("implementation", APAM_NAMESPACE);
                        preferenceDescription.addAttribute(new Attribute("filter", preference));
                        preferenceDescription.addAttribute(new Attribute("priority", Integer.toString(priority++)));
                        preferencesDescription.addElement(preferenceDescription);
                    }

                    priority=0;
                    for (String preference : relationDeclaration.getInstancePreferences()) {
                        Element preferenceDescription = new Element("instance", APAM_NAMESPACE);
                        preferenceDescription.addAttribute(new Attribute("filter", preference));
                        preferenceDescription.addAttribute(new Attribute("priority", Integer.toString(priority++)));
                        preferencesDescription.addElement(preferenceDescription);
                    }
                    relationDescription.addElement(preferencesDescription);

                    relationsDescription.addElement(relationDescription);
                }
                componentDescription.addElement(relationsDescription);

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
                for (Entry<String,String> propertyEntry : declaration.getProperties().entrySet()) {
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
     * A class to dynamically track the APAM platform. This allows to dynamically register/unregister this
     * component into the platform.
     *
     * NOTE We implement an static binding policy. Once an Apam platform has been found, it will be used until
     * it is no longer available.
     *
     * @author vega
     *
     */
    class ApamTracker extends ServiceTracker {

        private boolean bound;

        public ApamTracker(BundleContext context) {
            super(context, Apam.class.getName(), null);
            this.bound = false;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            if (bound)
                return null;


            this.bound = true;
            Apam apam = (Apam) this.context.getService(reference);
            bindToApam(apam);

            return apam;
        }


        @Override
        public void removedService(ServiceReference reference, Object service) {

            unbindFromApam((Apam) service);
            this.context.ungetService(reference);

            this.bound = false;
        }

    }

    /**
     * Get a reference to APAM
     */
    public final Apam getApam() {
        return apamTracker.size() != 0 ? (Apam) apamTracker.getService() : null;
    }
 

}