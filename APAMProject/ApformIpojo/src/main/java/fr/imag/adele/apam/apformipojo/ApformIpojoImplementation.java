package fr.imag.adele.apam.apformipojo;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.Implementation.DependencyModel;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.apform.message.impl.MessageConstants;

//import fr.imag.adele.apam.util.Attributes;

public class ApformIpojoImplementation extends ComponentFactory implements ApformImplementation {

    /**
     * The name space of this factory
     */
    public final static String APAM_NAMESPACE                   = "fr.imag.adele.apam";

    /**
     * Configuration property to specify the implementations provided specification
     */
    public final static String COMPONENT_SPECIFICATION_PROPERTY = "specification";

    /**
     * Configuration element to handle APAM dependencies
     */
    public final static String DEPENDENCY_DECLARATION           = "dependency";

    /**
     * Defines the implementation description.
     * 
     * @see ComponentTypeDescription
     */
    public static class Description extends ComponentTypeDescription {

        /**
         * The implementation dependency model
         */
        private final Set<DependencyModel> dependencies;

        /**
         * Creates the Apam Implementation Description.
         */
        protected Description(ApformIpojoImplementation factory) {
            super(factory);

            for (String providedInterface : getProvidedInterfaces()) {
                addProvidedServiceSpecification(providedInterface);
            }

            dependencies = new HashSet<DependencyModel>();
        }

        /**
         * Gets the attached factory.
         * 
         * Redefines with covariant result type.
         **/
        @Override
        public ApformIpojoImplementation getFactory() {
            return (ApformIpojoImplementation) super.getFactory();
        }

        /**
         * Return the specification provided by this implementation
         */
        public String getSpecification() {
            return getFactory().getProvidedSpecification();
        }

        /**
         * Get the list of provided interfaces
         */
        public String[] getProvidedInterfaces() {
            return getFactory().getProvidedInterfaces();
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
                properties.put(CST.A_APAMSPECNAME, getSpecification());

            return properties;
        }

        /**
         * Gets the component type description.
         */
        @Override
        public Element getDescription() {

            Element description = super.getDescription();

            if (getSpecification() != null)
                description
                        .addAttribute(new Attribute(ApformIpojoImplementation.COMPONENT_SPECIFICATION_PROPERTY, getSpecification()));

            for (DependencyModel dependency : getDependencies()) {
                Element dependencyDescription = new Element(ApformIpojoImplementation.DEPENDENCY_DECLARATION,
                        ApformIpojoImplementation.APAM_NAMESPACE);
                dependencyDescription.addAttribute(new Attribute("name", dependency.dependencyName));
                dependencyDescription.addAttribute(new Attribute("kind", dependency.targetKind.toString()));
                dependencyDescription.addAttribute(new Attribute("target", dependency.target));
                dependencyDescription.addAttribute(new Attribute("multiplicity", Boolean
                        .toString(dependency.isMultiple)));
                description.addElement(dependencyDescription);
            }
            return description;
        }

    }

    /**
     * The specification implemented by this implementation
     */
    protected String   m_specification;

    /**
     * The provided interfaces of the implementation
     */
    protected String[] providedInterfaces;

    /**
     * Build a new factory with the specified metadata
     * 
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformIpojoImplementation(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
        apamTracker = new ApamTracker(context);
    }

    /**
     * Whether this implementation has an associated instrumented class
     */
    public boolean hasInstrumentedCode() {
        return true;
    }
    
    /**
     * Whether this implementation is an abstract specification
     */
    public boolean isAbstract() {
    	return false;
    }
    
    @Override
    public List getRequiredHandlerList() {
    	
    	//TODO REverify after creating the handler
    	List requiredHandlers = super.getRequiredHandlerList();
    	RequiredHandler messageProvider = new RequiredHandler(MessageConstants.MESSAGE_PROVIDES_DECLARATION,APAM_NAMESPACE);
    	requiredHandlers.add(messageProvider);
    	return requiredHandlers;
    }

    /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * 
     */
    @Override
    @SuppressWarnings({ "rawtypes" })
    public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers)
            throws ConfigurationException {

        if (!hasInstrumentedCode())
            throw new ConfigurationException(
                    "Only APAM implementations with instrumented code can be directly instantiated;  To create specifications and composites use instead the APAM API");

        /*
         * Create a native APAM instance and configure it.
         * 
         */
        ApformIpojoInstance instance = new ApformIpojoInstance(this, isApamCall(), context, handlers);

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

        m_specification = element.getAttribute(ApformIpojoImplementation.COMPONENT_SPECIFICATION_PROPERTY);

        /*
         * Get the list of provided interfaces from the instrumented code
         */
        Set<String> interfaces = new HashSet<String>();

        if (hasInstrumentedCode()) {

            String[] serviceSpecification = getPojoMetadata().getInterfaces();
            String parent = getPojoMetadata().getSuperClass();

            try {
                Set<String> ancestors = new HashSet<String>();
                ApformIpojoImplementation.computeInterfacesAndSuperClasses(serviceSpecification, parent, getBundleContext()
                        .getBundle(), interfaces, ancestors);
                getLogger().log(Logger.INFO,
                        "Collected interfaces from " + element.getAttribute("classname") + " : " + interfaces);
                getLogger().log(Logger.INFO,
                        "Collected super classes from " + element.getAttribute("classname") + " : " + ancestors);

                interfaces.remove(Pojo.class.getName()); // Remove POJO.

            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("An interface or parent class cannot be loaded : " + e.getMessage());
            }
        }

        providedInterfaces = interfaces.toArray(new String[interfaces.size()]);
    }

    /**
     * Collect interfaces implemented by the POJO.
     * 
     * @param specs : implemented interfaces.
     * @param parent : parent class.
     * @param bundle : Bundle object.
     * @param interfaces : the set of implemented interfaces
     * @param classes : the set of extended classes
     * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
     */
    private static void computeInterfacesAndSuperClasses(String[] specs, String parent, Bundle bundle,
            Set<String> interfaces, Set<String> classes) throws ClassNotFoundException {
        // First iterate on found specification in manipulation metadata
        for (String spec : specs) {
            interfaces.add(spec);
            // Iterate on interfaces implemented by the current interface
            Class<?> clazz = bundle.loadClass(spec);
            ApformIpojoImplementation.collectInterfaces(clazz, interfaces, bundle);
        }

        // Look for parent class.
        if (parent != null) {
            Class<?> clazz = bundle.loadClass(parent);
            ApformIpojoImplementation.collectInterfacesFromClass(clazz, interfaces, bundle);
            classes.add(parent);
            ApformIpojoImplementation.collectParentClassesFromClass(clazz, classes, bundle);
        }
    }

    /**
     * Look for inherited interfaces.
     * 
     * @param clazz : interface name to explore (class object)
     * @param acc : set (accumulator)
     * @param bundle : bundle
     * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
     */
    private static void collectInterfaces(Class<?> clazz, Set<String> acc, Bundle bundle) throws ClassNotFoundException {
        Class<?>[] clazzes = clazz.getInterfaces();
        for (Class<?> clazze : clazzes) {
            acc.add(clazze.getName());
            ApformIpojoImplementation.collectInterfaces(clazze, acc, bundle);
        }
    }

    /**
     * Collect interfaces for the given class.
     * This method explores super class to.
     * 
     * @param clazz : class object.
     * @param acc : set of implemented interface (accumulator)
     * @param bundle : bundle.
     * @throws ClassNotFoundException : occurs if an interface cannot be load.
     */
    private static void collectInterfacesFromClass(Class<?> clazz, Set<String> acc, Bundle bundle)
            throws ClassNotFoundException {
        Class<?>[] clazzes = clazz.getInterfaces();
        for (Class<?> clazze : clazzes) {
            acc.add(clazze.getName());
            ApformIpojoImplementation.collectInterfaces(clazze, acc, bundle);
        }
        // Iterate on parent classes
        Class<?> sup = clazz.getSuperclass();
        if (sup != null) {
            ApformIpojoImplementation.collectInterfacesFromClass(sup, acc, bundle);
        }
    }

    /**
     * Collect parent classes for the given class.
     * 
     * @param clazz : class object.
     * @param acc : set of extended classes (accumulator)
     * @param bundle : bundle.
     * @throws ClassNotFoundException : occurs if an interface cannot be load.
     */
    private static void collectParentClassesFromClass(Class<?> clazz, Set<String> acc, Bundle bundle)
            throws ClassNotFoundException {
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            acc.add(parent.getName());
            ApformIpojoImplementation.collectParentClassesFromClass(parent, acc, bundle);
        }
    }

    /**
     * Get the name of the specification provided by this implementation
     */
    public String getProvidedSpecification() {
        return m_specification;
    }

    /**
     * Get the implementation provided interfaces
     */
    public String[] getProvidedInterfaces() {
        return providedInterfaces;
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
    protected void bindToApam(Apam apam) {
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

    /**
     * Get a reference to APAM
     */
    public final Apam getApam() {
        return apamTracker.size() != 0 ? (Apam) apamTracker.getService() : null;
    }

//	public final Apform2Apam getApamPlatform() {
//		return getApam() != null ? CST.apform2Apam : null;
//	}

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
        return (Map<String, Object>) getComponentDescription().getPropertiesToPublish();
    }

    /**
     * Apform: get a property of the implementation
     */
    @Override
    public Object getProperty(String key) {
        return getComponentDescription().getPropertiesToPublish().get(key);
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
     * Apform: create an instance
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
     * Apform: get the provided specification
     */
    @Override
    public ApformSpecification getSpecification() {
        return null;
    }

    /**
     * A dynamic reference to the APAM platform
     */
    private final ApamTracker apamTracker;

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
}
