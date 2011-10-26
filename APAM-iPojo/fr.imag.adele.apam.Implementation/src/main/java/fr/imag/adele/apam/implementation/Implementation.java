package fr.imag.adele.apam.implementation;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apamAPI.ASMImpl.DependencyModel;

public class Implementation  extends ComponentFactory {

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
        	return getFactory().getSpecification();
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
        	properties.put(CST.A_APAMSPECNAME,getSpecification());
        	properties.put(CST.A_DEPENDENCIES, getDependencies());
        	return properties;
        }
        
	    /**
	     * Gets the component type description.
	     */
        @Override
        public Element getDescription() {

        	Element description =  super.getDescription();
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

        /*
         * automatically provides all interfaces inferred from the instrumented code
         */
        if ( hasInstrumentedCode() && ! ImplementationHandler.isDefined(m_componentMetadata.getElements("provides")))
        	m_componentMetadata.addElement(new Element("provides",null));
    }

    /**
     * Whether this implementation has an associated instrumented class
     */
    public boolean hasInstrumentedCode() {
    	return true;
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
        if (m_specification == null) { 
        	throw new ConfigurationException("An implementation needs an specification name : " + element);
        }
    }
    
    /**
     * Get the name of the specification provided by this implementation
     */
    public String getSpecification() {
    	return m_specification;
    }
    
    /**
     * Computes required handlers.
     * 
     * Add automatically a provides handler to publish interfaces inferred from the instrumented code
     */
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List getRequiredHandlerList() {
    	List requiredHandlers =  super.getRequiredHandlerList();
    	
    	RequiredHandler providesHandlerReference = new RequiredHandler("provides",null);
    	if (hasInstrumentedCode() && ! requiredHandlers.contains(providesHandlerReference))
    		requiredHandlers.add(providesHandlerReference);
    	
    	return requiredHandlers;
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
 
}
