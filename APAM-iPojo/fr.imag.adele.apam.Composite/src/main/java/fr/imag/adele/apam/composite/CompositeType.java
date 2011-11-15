package fr.imag.adele.apam.composite;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.ManagerModel;
import fr.imag.adele.apam.implementation.ApamFactory;

public class CompositeType extends ApamFactory {


    /**
     * Configuration property to specify the composites's main implementation
     */
    private final static String COMPOSITE_IMPLEMENTATION_PROPERTY 	= "mainImplem";


    /**
     * Configuration property to specify the composites's implemented interfaces
     */
    private final static String COMPOSITE_INTERFACES_PROPERTY 		= "interfaces";

    /**
     * Configuration property to specify the composites's registered models
     */
    private final static String COMPOSITE_MODELS_DECLARATION		= "models";
    

    /**
     * Configuration property to specify the composites's registered model
     */
    private final static String COMPOSITE_MODEL_DECLARATION 		= "model";

    /**
     * Defines the composite type description.
     * 
     * @see ComponentTypeDescription
     */
    public static class Description extends ApamFactory.Description {

        /**
         * Creates the ApamCompositeDescription.
         * 
         * @param factory the factory.
         * @see ComponentTypeDescription#ComponentTypeDescription(Factory)
         */
        protected Description(CompositeType factory) {
        	super(factory);
        }

        /**
         * Gets the attached factory.
         * 
       	 * Redefines with covariant result type.
         *
         **/
        @Override
        public CompositeType getFactory() {
        	return (CompositeType) super.getFactory();
        }

        
        /**
         * Get the name of the main implementation of the composite
         */
        public String getMainImplementation() {
        	return getFactory().getMainImplementation();
        }
        
        /**
         * Get The list of models associated to this composite
         */
        public Set<ManagerModel> getManagerModels() {
            return getFactory().getManagerModels();
        }
        
        /**
         * Computes the default service properties to publish the factory.
         */
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Dictionary getPropertiesToPublish() {
            Dictionary properties = super.getPropertiesToPublish();
            
            properties.put(CST.A_COMPOSITE, true);
            properties.put(CST.A_MAIN_IMPLEMENTATION,getMainImplementation());
            properties.put(CST.A_MODELS,getManagerModels());
            
            return properties;
        }

	    /**
	     * Gets the component type description.
	     */
        @Override
        public Element getDescription() {

            Element description = super.getDescription();
            description.addAttribute(new Attribute("implementation", getMainImplementation()));
            
            Element models = new Element(COMPOSITE_MODELS_DECLARATION,APAM_NAMESPACE);
            description.addElement(models);
            
            for (ManagerModel managerModel : getManagerModels()) {
                Element modelDescription = new Element(COMPOSITE_MODEL_DECLARATION,APAM_NAMESPACE);
                modelDescription.addAttribute(new Attribute("manager", managerModel.getManagerName()));
                modelDescription.addAttribute(new Attribute("url", managerModel.getURL().toExternalForm()));
                models.addElement(modelDescription);
            }

            return description;
        }

    }

    /**
     * The name of the APAM implementation for the main instance of the composite
     */
    private String		mainImplementation;
    
    
    /**
     * The list of models associated to this composite
     */
    private final Set<ManagerModel> managerModels;

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public CompositeType(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        /*
         * Get the composite's main implementation
         */
        mainImplementation = metadata.getAttribute(CompositeType.COMPOSITE_IMPLEMENTATION_PROPERTY);

        /*
         * Get the composite's provided interfaces
         */
        providedInterfaces = ParseUtils.parseArrays(metadata.getAttribute(COMPOSITE_INTERFACES_PROPERTY));

        /*
         * look for manager models in the root directory of the bundle
         */
        managerModels = new HashSet<ManagerModel>();

        @SuppressWarnings("unchecked")
        Enumeration<String> paths = context.getBundle().getEntryPaths("/");
        while (paths.hasMoreElements()) {
            String path = paths.nextElement();
            if (!path.endsWith(".xml"))
                continue;

            URL modelURL = context.getBundle().getEntry(path);
            String modelName = path.substring(0, path.lastIndexOf(".xml"));
            String managerName = modelName;
            managerModels.add(new ManagerModel(modelName, managerName, modelURL, 0));
        }
    }

    /**
     * This factory doesn't have an associated instrumented class
     */
    @Override
    public boolean hasInstrumentedCode() {
    	return false;
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
        String encodedInterfaces = metadata.getAttribute(COMPOSITE_INTERFACES_PROPERTY);
        if (encodedInterfaces == null) {
        	metadata.addAttribute(new Attribute(COMPOSITE_INTERFACES_PROPERTY,null,""));
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
     * Get the composites's main implementation
     */
    public String getMainImplementation() {
        return mainImplementation;
    }

    /**
     * Get the composites's provided interfaces
     */
    public String[] getProvidedInterfaces() {
        return providedInterfaces;
    }

    /**
     * Get The list of models associated to this composite
     */
    public Set<ManagerModel> getManagerModels() {
        return managerModels;
    }


}
