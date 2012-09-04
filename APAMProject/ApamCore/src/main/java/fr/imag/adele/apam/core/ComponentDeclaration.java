package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents the common description of a component at all levels of abstraction
 * (specification, implementation, composite or instance)
 * 
 * @author vega
 * 
 */
public abstract class ComponentDeclaration {

    /**
     * The identifier of the service provider
     */
    private final String name;

    /**
     * The reference to this component declaration
     */
    private final ComponentReference<?> reference;

    /**
     * The resources provided by this service
     */
    protected final Set<ResourceReference> providedResources;

    /**
     * The resources required by this service
     */
    private final Set<DependencyDeclaration> 	dependencies;

    /**
     * The properties describing this service provider
     */
    private Map<String,Object> 					properties;
    private final List<PropertyDefinition>		definitions;

    protected ComponentDeclaration(String name) {

        assert name != null;

        this.name			= name;
        reference			= generateReference();
        properties			= new HashMap<String,Object>();
        providedResources	= new HashSet<ResourceReference>();
        dependencies		= new HashSet<DependencyDeclaration>();
        definitions 		= new ArrayList<PropertyDefinition>();
    }

    /**
     * Get the name of the provider
     */
    public String getName() {
        return name;
    }

    /**
     * Get the reference to this declaration
     */
    public ComponentReference<?> getReference() {
        return reference;
    }

    /**
     * Get the reference to this declaration
     */
    public abstract ComponentReference<?> getGroupReference() ;

    /**
     * Generates a unique resource identifier to reference this declaration
     */
    protected  abstract ComponentReference<?> generateReference();

    /**
     * Get the properties describing this provider
     */
    public Map<String,Object> getProperties() {
        return properties;
    }

    /**
     * Get the value of a property
     */
    public Object getProperty(String property) {
        return properties.get(property);
    }

    /**
     * Get the property definitions defined by this component
     */
    public List<PropertyDefinition> getPropertyDefinitions() {
        return definitions;
    }

    /**
     * Get the named property definition, if declared
     */
    public PropertyDefinition getPropertyDefinition(String propertyName) {
        for (PropertyDefinition definition : definitions) {
            if (definition.getName().equals(propertyName))
                return definition;
        }
        return null;
    }

    public boolean isDefined(String propertyName) {
        return getPropertyDefinition(propertyName) != null;
    }

    /**
     * Get the provided resources
     */
    public Set<ResourceReference> getProvidedResources() {
        return providedResources;
    }

    /**
     * Get the provided resources of a given kind, for example Services or Messages. 
     * 
     * We use subclasses of ResourceReference as tags to identify kinds of resources. To add a new kind
     * of resource a new subclass must be added.
     * 
     * Notice that we return a set of resource references but typed to particular subtype of references,
     * the unchecked downcast is then safe at runtime.
     */
	@SuppressWarnings("unchecked")
	public <T extends ResourceReference> Set<T> getProvidedResources(Class<T> kind) {
    	Set<ResourceReference> resources = new HashSet<ResourceReference>();
    	for (ResourceReference resourceReference : providedResources) {
			if (kind.isInstance(resourceReference))
				resources.add(resourceReference);
		}
        return (Set<T>) resources;
    }
   

    /**
     * Check if the specified resource is provided by this provider
     * 
     * TODO Notice that we can ask for any ResolvableReference which is less restrictive than a ResourceReference. 
     * 
     * This is to avoid castings when invoking this method in the context of a dependency resolution that can reference a
     * specification. Perhaps we should unify the concepts of provided resources and provided specification for implementations,
     * but it is awkward to generalize to all component descriptions (e.g Specifications provides themselves?)
     */
    public boolean isProvided(ResolvableReference resource) {
        return providedResources.contains(resource);
    }

    /**
     * Get the declared dependencies of this component
     */
    public Set<DependencyDeclaration> getDependencies() {
        return dependencies;
    }

    /**
     * Get a dependency declaration by name
     */
    public DependencyDeclaration getDependency(String id) {
        for (DependencyDeclaration dependency : dependencies) {
            if (dependency.getIdentifier().equals(id))
                return dependency;
        }

        return null;
    }

    /**
     * Get a dependency declaration by reference
     */
    public DependencyDeclaration getDependency(DependencyDeclaration.Reference dependency) {
    	if (! this.getReference().equals(dependency.getDeclaringComponent()))
    		return null;
    	
    	return getDependency(dependency.getIdentifier());
    }

    /**
     * Check if this component requires the specified resource
     */
    public boolean isRequired(ResourceReference resource) {
        for (DependencyDeclaration dependency : dependencies) {
            if ( dependency.getTarget().equals(resource))
                return true;
        }

        return false;
    }


    @Override
    public String toString() {
        return printDeclaration("") ;
    }

    
	/**
	 * Displays the declaration on screen, indented.
	 * Same as toString, plus the indentation.
	 * @param indent: a number of white characters as indentation.
	 */
	public String printDeclaration (String indent) {
		String nl = "\n" + indent ;
        String ret = indent + " Declaration of " + name;
        if (providedResources.size() != 0) {
            ret += nl + "   Provided resources: ";
            for (ResourceReference resRef : providedResources) {
                ret += nl + "      " + resRef;
            }
        }
        if (dependencies.size() != 0) {
            ret += nl + "   Dependencies: \n";
            for (DependencyDeclaration resRef : dependencies) {
                ret += resRef.printDependencyDeclaration(indent + "   ") + "\n";
            }
        }
        if (properties.size() != 0) {
            ret += nl + "   Properties: ";
            for (Object resRef : properties.keySet()) {
                ret += nl + "      " + (String) resRef + " = " + properties.get(resRef);
            }
        }
        if (definitions.size() != 0) {
            ret += nl + "   Attribute definitions: ";
            for (PropertyDefinition resRef : definitions) {
                ret += nl + "      " + resRef;
            }
        }
        
        return ret;
	}

}
