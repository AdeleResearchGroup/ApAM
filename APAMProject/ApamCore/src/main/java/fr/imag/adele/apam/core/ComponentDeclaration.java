package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.core.ResourceReference.ResourceType;

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
    private final ResourceReference reference;
    
    /**
     * The resources provided by this service
     */
    protected final Set<ProvidedResourceReference> providedResources;

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
        this.reference		= generateReference();
        properties			= new HashMap<String,Object>();
        providedResources	= new HashSet<ProvidedResourceReference>();
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
    public ResourceReference getReference() {
    	return reference;
    }
    
    /**
     * Generates a unique resource identifier to reference this declaration
     */
    protected  abstract ResourceReference generateReference();
    
    /**
     * Get the properties describing this provider
     */
    public Map<String,Object> getProperties() {
        return properties;
    }

    public String getAttribute(String name) {
        return (String) properties.get(name);
    }
    /**
     * Get the value of a property
     */
    public Object getProperty(String property) {
        return properties.get(property);
    }

    public List<PropertyDefinition> getPropertyDefinitions() {
        return definitions;
    }

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
    public Set<ProvidedResourceReference> getProvidedResources() {
        return providedResources;
    }

    /**
     * Check if the specified resource is provided by this provider
     */
    public boolean isProvided(ResourceReference resource) {
        return providedResources.contains(resource);
    }


    /**
     * Get the required resources of this provider
     */
    public Set<DependencyDeclaration> getDependencies() {
        return dependencies;
    }

    public DependencyDeclaration getDependency(String name) {
        for (DependencyDeclaration dependency : dependencies) {
            if (dependency.getName().equals(name))
                return dependency;
        }

        return null;
    }

    /**
     * Check if this provider requires the specified resource
     */
    public boolean isRequired(ResourceReference resource) {
        for (DependencyDeclaration dependency : dependencies) {
            if ( dependency.getResource().equals(resource))
                return true;
        }

        return false;
    }

    public Set<String> getProvidedRessourceNames(ResourceType type) {
        Set<String> providedRes = new HashSet<String>();
        for (ProvidedResourceReference resRef : providedResources) {
            if (resRef.resourceType == type) {
                providedRes.add(resRef.getName());
            }
        }
        return providedRes;
    }

    public String getProvidedRessourceString(ResourceType type) {
        return ComponentDeclaration.toStringResources(getProvidedRessourceNames(type));
    }

    /**
     * takes a list of string "A" "B" "C" ... and produces "{A, B, C, ...}"
     * 
     * @param names
     * @return
     */
    private static String toStringResources(Set<String> names) {
        if ((names == null) || (names.size() == 0))
            return null;
        String ret = "{";
        for (String name : names) {
            ret += name + ", ";
        }
        return ret.substring(0, ret.length() - 2) + "}";
    }

 }
