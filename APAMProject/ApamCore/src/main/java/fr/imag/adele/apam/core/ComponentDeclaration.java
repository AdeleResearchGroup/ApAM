package fr.imag.adele.apam.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class represents the common description of a component  at all levels of abstraction 
 * (specification, implementation or instance)
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
	 * The resources provided by this service
	 */
	private final Set<ProvidedResourceReference> providedResources;
	
	/**
	 * The resources required by this service
	 */
	private final Set<DependencyDeclaration> dependencies;
	
	/**
	 * The properties describing this service provider
	 */
	private Map<String,Object> properties;
	
	
	protected ComponentDeclaration(String name) {
		
		assert name != null;
		
		this.name				= name;
		this.properties			= new HashMap<String,Object>();
		this.providedResources	= new HashSet<ProvidedResourceReference>();
		this.dependencies		= new HashSet<DependencyDeclaration>();
	}
	
	/**
	 * Get the name of the provider
	 */
	public String getName() {
		return name;
	}
	
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
}
