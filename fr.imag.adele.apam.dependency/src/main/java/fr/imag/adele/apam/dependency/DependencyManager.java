package fr.imag.adele.apam.dependency;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;

public class DependencyManager extends PrimitiveHandler implements ApamDependencyHandler {

    /**
     * The name space of this handler
     */
    private final static String     APAM_NAMESPACE                     = "fr.imag.adele.apam";

    /**
     * Configuration property to specify the dependency's target implementation
     */
    private final static String     COMPONENT_IMPLEMENTATION_PROPERTY  = "implementation";

    /**
     * Configuration property to specify the dependency's target specification
     */
    private final static String     COMPONENT_SPECIFICATION_PROPERTY   = "specification";

    /**
     * Configuration element to handle APAM dependencies
     */
    private final static String     DEPENDENCY_DECLARATION             = "dependency";

    /**
     * Configuration property to specify the dependency's name
     */
    private final static String     DEPENDENCY_NAME_PROPERTY           = "name";

    /**
     * Configuration property to specify the injected field
     */
    private final static String     DEPENDENCY_FIELD_PROPERTY          = "field";

    /**
     * Configuration property to specify the dependency's aggregate attribute
     */
    private final static String     DEPENDENCY_AGGREGATE_PROPERTY      = "multiple";

    /**
     * Configuration property to specify the dependency's target interface
     */
    private final static String     DEPENDENCY_INTERFACE_PROPERTY      = "interface";

    /**
     * Configuration property to specify the dependency's target specification
     */
    private final static String     DEPENDENCY_SPECIFICATION_PROPERTY  = "specification";

    /**
     * Configuration property to specify the dependency's target implementation
     */
    private final static String     DEPENDENCY_IMPLEMENTATION_PROPERTY = "implementation";

    /**
     * A reference to the APAM machine.
     */
    private ApamClient              apam;

    /**
     * A reference to the corresponding instance in the APAM application state
     */
    private ASMInst                 thisInstance;

    /**
     * The name of the APAM component for this instance
     */
    private String                  apamComponent;

    /**
     * The name of the APAM specification for this instance
     */
    private String                  apamSpecification;

    /**
     * The list of dependencies declared in this component
     */
    private Map<String, Dependency> dependencies;

    /**
     * Quote String in message
     */
    private static final String quote(String arg) {
        return "\"" + arg + "\"";
    }

    /*
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.
     * ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    @Override
    public void initializeComponentFactory(ComponentTypeDescription componentDescriptor, Element componentMetadata)
            throws ConfigurationException {

        /*
         * Validate an APAM component name is specified, otherwise use the iPojo component name
         */
        String componentName = componentMetadata.getAttribute(DependencyManager.COMPONENT_IMPLEMENTATION_PROPERTY,DependencyManager.APAM_NAMESPACE);
        if (componentName == null) {
            componentName = componentDescriptor.getName();
            componentMetadata.addAttribute(new Attribute(DependencyManager.COMPONENT_IMPLEMENTATION_PROPERTY,DependencyManager.APAM_NAMESPACE,componentName));
        }

        /*
         * Validate the component class is accessible
         */
        Class<?> componentClass = null;
        try {
            componentClass = getFactory().loadClass(getFactory().getClassName());

        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("iPojo component "
                    + DependencyManager.quote(componentDescriptor.getName()) + ": " + "the component class "
                    + getFactory().getClassName() + " can not be loaded");
        }

        /*
         * Statically validate the component type dependencies
         */

        Element dependencyDeclarations[] = componentMetadata.getElements(DependencyManager.DEPENDENCY_DECLARATION,
                DependencyManager.APAM_NAMESPACE);
        for (Element dependencyDeclaration : dependencyDeclarations) {

            String dependencyName = dependencyDeclaration.getAttribute(DependencyManager.DEPENDENCY_NAME_PROPERTY);
            String dependencyFieldName = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_FIELD_PROPERTY);

            /*
             * If dependency name is not explicitly declared, use the field name as default
             */
            if ((dependencyName == null) && (dependencyFieldName != null)) {
                dependencyName = dependencyFieldName;
                dependencyDeclaration.addAttribute(new Attribute(DependencyManager.DEPENDENCY_NAME_PROPERTY,
                        dependencyName));
            }

            /*
             * Validate a field has been specified
             */
            if (dependencyFieldName == null) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "a field must be specified");
            }

            /*
             * validate referenced field actually exists in the component class
             */
            Field field;
            try {
                field = componentClass.getDeclaredField(dependencyFieldName);
            } catch (SecurityException e1) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": " + "the specified field "
                        + DependencyManager.quote(dependencyFieldName)
                        + " is not accesible in the implementation class");
            } catch (NoSuchFieldException e1) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": " + "the specified field "
                        + DependencyManager.quote(dependencyFieldName) + " is not declared in the implementation class");
            }

            /*
             * validate referenced field has been instrumented by iPojo
             */
            FieldMetadata fieldInstrumentation = getFactory().getPojoMetadata().getField(dependencyFieldName);
            if (fieldInstrumentation == null) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": " + "the specified field "
                        + DependencyManager.quote(dependencyFieldName) + " is not instrumented by iPojo");
            }

            /*
             * validate field's class is compatible with the specified characteristics of the dependency
             */
            Class<?> fieldClass = field.getType();
            boolean isAggregateField = fieldClass.isArray() || Collection.class.isAssignableFrom(fieldClass);

            /*
             * /* Validate the specified cardinality, try to infer sensible defaults if not specified
             */

            String dependencyAggregate = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_AGGREGATE_PROPERTY);

            if (dependencyAggregate == null) {
                dependencyAggregate = Boolean.toString(isAggregateField);
                dependencyDeclaration.addAttribute(new Attribute(DependencyManager.DEPENDENCY_AGGREGATE_PROPERTY,
                        dependencyAggregate));
            }

            /*
             * Validate the class of the field is compatible with the specified cardinality
             */

            boolean isDependencyAggregate = Boolean.valueOf(dependencyAggregate);

            if (isAggregateField && !isDependencyAggregate) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "for scalar dependencies the class of the specified field cannot be a Collection or array");
            }

            if (isDependencyAggregate && !isAggregateField) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "for aggregate dependencies the class of the specified field must be a Collection or array");
            }

            if (isDependencyAggregate && !Dependency.isSupportedCollection(field)) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "for aggregate dependencies the class of the specified field must be "
                        + Dependency.supportedCollectionClasses());

            }

            /*
             * validate the specified target of the dependency is specified
             */

            String dependencySpecification = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_SPECIFICATION_PROPERTY);
            String dependencyImplementation = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_IMPLEMENTATION_PROPERTY);
            String dependencyInterface = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_INTERFACE_PROPERTY);

            if ((dependencySpecification != null) && (dependencyImplementation != null)) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "specification and implementation declarations are exclusive");
            }

            /*
             * validate the specified target is compatible with the field declaration
             * 
             * NOTE it is not always possible to perform this validation, as the class of the elements of a collection
             * can not always be inferred from the field declaration.
             */

            Class<?> fieldElementClass = isDependencyAggregate ? Dependency.getCollectionElement(field) : field
                    .getType();

            if ((fieldElementClass != null) && (dependencyInterface != null)) {
                try {

                    Class<?> interfaceClass = getFactory().loadClass(dependencyInterface);
                    if (!fieldElementClass.isAssignableFrom(interfaceClass)) {
                        throw new ConfigurationException("APAM Dependency "
                                + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                                + "the specified interface " + DependencyManager.quote(dependencyInterface)
                                + " can not be assigned to field " + dependencyFieldName);
                    }

                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("APAM Dependency "
                            + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                            + "the specified interface " + DependencyManager.quote(dependencyInterface)
                            + " is not accessible");
                }
            }

            if ((fieldElementClass != null) && (dependencySpecification != null)) {
                if (!fieldElementClass.isInterface()) {
                    throw new ConfigurationException("APAM Dependency "
                            + DependencyManager.quote(componentName + "." + dependencyName) + ": " + "the field "
                            + DependencyManager.quote(dependencyFieldName)
                            + " must be declared using an interface provided by specification "
                            + dependencySpecification);
                }
            }

            /*
             * If no target is specified, try to infer at least a target interface, otherwise signal an error
             */
            if ((dependencySpecification == null) && (dependencyImplementation == null)
                    && (dependencyInterface == null)) {
                if ((fieldElementClass != null) && fieldElementClass.isInterface()) {
                    dependencyInterface = fieldElementClass.getCanonicalName();
                    dependencyDeclaration.addAttribute(new Attribute(DependencyManager.DEPENDENCY_INTERFACE_PROPERTY,
                            dependencyInterface));
                }
            }

            if ((dependencySpecification == null) && (dependencyImplementation == null)
                    && (dependencyInterface == null)) {
                throw new ConfigurationException("APAM Dependency "
                        + DependencyManager.quote(componentName + "." + dependencyName) + ": "
                        + "has no target; declare a target interface, specification or implementation");
            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
	@Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        /*
         * Get component information and add interceptors to delegate dependency resolution
         * 
         * NOTE All validations were already performed when validating the factory @see initializeComponentFactory,
         * including initializing unspecified properties with appropriate default values. Here we just assume metadata
         * is correct.
         */
        apamComponent = componentMetadata.getAttribute(DependencyManager.COMPONENT_IMPLEMENTATION_PROPERTY,DependencyManager.APAM_NAMESPACE);
        apamSpecification = componentMetadata.getAttribute(DependencyManager.COMPONENT_SPECIFICATION_PROPERTY,DependencyManager.APAM_NAMESPACE);

        dependencies = new HashMap<String, Dependency>();

        Element dependencyDeclarations[] = componentMetadata.getElements(DependencyManager.DEPENDENCY_DECLARATION,
                DependencyManager.APAM_NAMESPACE);
        for (Element dependencyDeclaration : dependencyDeclarations) {

            String dependencyName = dependencyDeclaration.getAttribute(DependencyManager.DEPENDENCY_NAME_PROPERTY);
            String dependencyFieldName = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_FIELD_PROPERTY);
            String dependencyAggregate = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_AGGREGATE_PROPERTY);
            String dependencySpecification = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_SPECIFICATION_PROPERTY);
            String dependencyImplementation = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_IMPLEMENTATION_PROPERTY);
            String dependencyInterface = dependencyDeclaration
                    .getAttribute(DependencyManager.DEPENDENCY_INTERFACE_PROPERTY);

            String dependencyTarget = dependencyInterface;
            Dependency.Kind dependencyKind = Dependency.Kind.INTERFACE;

            if (dependencySpecification != null) {
                dependencyTarget = dependencySpecification;
                dependencyKind = Dependency.Kind.SPECIFICATION;
            }

            if (dependencyImplementation != null) {
                dependencyTarget = dependencyImplementation;
                dependencyKind = Dependency.Kind.IMPLEMENTATION;
            }

            FieldMetadata field = getFactory().getPojoMetadata().getField(dependencyFieldName);
            Dependency dependency = new Dependency(this, dependencyName, Boolean.valueOf(dependencyAggregate), field
                    .getFieldType(), dependencyTarget, dependencyKind);

            dependencies.put(dependencyName, dependency);
            getInstanceManager().register(field, dependency);
        }

    }

    @Override
    public void start() {
        /*
         * The instance is started, nothing to do; we should already be registered
         */
    }

    /**
     * Dynamically register this instance with APAM
     */
    public void apamBound() {
        apam.newClientCallBack(getInstanceManager().getInstanceName(), this, apamComponent, apamSpecification);
    }

    /**
     * Avoid invoking APAM, if not available
     */
    public void apamUnbound() {
        thisInstance = null;
    }

    /**
     * This callback method will be invoked by APAM when the instance is effectively added to the application state
     * model
     */
    public void SetIdentifier(ASMInst inst) {
        thisInstance = inst;
    }

    /**
     * This method will be invoked by APAM to get a minimal model of the dependencies known by this handler
     */
    public Set<DependencyModel> getDependencies() {
    	Set<DependencyModel> dependenciesModel = new HashSet<DependencyModel>();
    	
    	for (Dependency dependency : dependencies.values()) {
    		DependencyModel dependencyModel = new DependencyModel();
    		dependencyModel.dependencyName	= dependency.getName();
    		dependencyModel.isMultiple		= dependency.isAggregate();
    		dependencyModel.target			= dependency.getTarget();
    		switch (dependency.getKind()) {
			case INTERFACE:
				dependencyModel.targetKind = TargetKind.INTERFACE;
				break;
			case SPECIFICATION:
				dependencyModel.targetKind = TargetKind.SPECIFICATION;
				break;
			case IMPLEMENTATION:
				dependencyModel.targetKind = TargetKind.IMPLEMENTATION;
				break;
			}
			dependenciesModel.add(dependencyModel);
		}
    	return dependenciesModel;
    }
    
    @Override
    public void stop() {
    }

    @Override
    public String toString() {
        return "APPAM Dependency manager for " + getInstanceManager().getInstanceName();
    }

    public boolean setWire(ASMInst destInst, String depName) {
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.addDependency(destInst);
        return true;
    }

    public boolean remWire(ASMInst destInst, String depName) {
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.removeDependency(destInst);
        return true;
    }

    public boolean substWire(ASMInst oldDestInst, ASMInst newDestInst, String depName) {
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.substituteDependency(oldDestInst, newDestInst);
        return true;
    }

    /**
     * Delegate APAM to resolve a given dependency.
     * 
     * NOTE nothing is returned from this method, the call to APAM has as side-effect the update of the dependency.
     * 
     * @param dependency
     */
    public void resolve(Dependency dependency) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if ((apam == null) || (thisInstance == null))
            return;

        switch (dependency.getKind()) {
            case IMPLEMENTATION:
                if (dependency.isScalar())
                    apam.newWireImpl(thisInstance, null, dependency.getTarget(), dependency.getName());
                else
                    apam.newWireImpls(thisInstance, null, dependency.getTarget(), dependency.getName());
                break;
            case SPECIFICATION:
                if (dependency.isScalar())
                    apam.newWireSpec(thisInstance, null, dependency.getTarget(), dependency.getName());
                else
                    apam.newWireSpecs(thisInstance, null, dependency.getTarget(), dependency.getName());
                break;
            case INTERFACE:
                if (dependency.isScalar())
                    apam.newWireSpec(thisInstance, dependency.getTarget(), null, dependency.getName());
                else
                    apam.newWireSpecs(thisInstance, dependency.getTarget(), null, dependency.getName());
                break;
        }
    }

}
