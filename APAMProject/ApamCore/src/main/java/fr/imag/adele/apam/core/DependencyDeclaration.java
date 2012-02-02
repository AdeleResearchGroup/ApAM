package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents the declaration of a required resources needed
 * by a service provider
 * 
 * @author vega
 *
 */
public class DependencyDeclaration {

	/**
	 * The identification of the dependency in its declaring component
	 */
	private final String name;
	
	/**
	 * If this dependency requires several providers of the resource
	 */
	private final boolean isMultiple;
	
	/**
	 * The reference to the required resource 
	 */
	private final ResourceReference resource;
	
	/**
	 * The set of constraints that must be satisfied by the resource provider
	 */
	private final Set<String> providerConstraints;
	
	/**
	 * The set of constraints that must be satisfied by the resource provider instance
	 */
	private final Set<String> instanceConstraints;
	
	/**
	 * The list of preferences to choose among candidate service provider
	 */
	private final List<String> providerPreferences;

	/**
	 * The list of preferences to choose among candidate service provider instances
	 */
	private final List<String> instancePreferences;

	public DependencyDeclaration(String name, ResourceReference resource, boolean isMultiple) {
		
		assert name != null;
		assert resource != null;
		
		this.name			= name;
		this.resource 		= resource;
		this.isMultiple 	= isMultiple;
		
		this.providerConstraints	= new HashSet<String>();
		this.instanceConstraints	= new HashSet<String>();
		this.providerPreferences	= new ArrayList<String>();
		this.instancePreferences	= new ArrayList<String>();
	}
	
	/**
	 * Get the name of the dependency
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the reference to the required resource
	 */
	public ResourceReference getResource() {
		return resource;
	}
	
	/**
	 * If this dependency requires multiple resource providers
	 */
	public boolean isMultiple() {
		return isMultiple;
	}
	
	/**
	 * Get the resource provider constraints
	 */
	public Set<String> getProviderConstraints() {
		return providerConstraints;
	}
	
	/**
	 * Get the instance provider constraints
	 */
	public Set<String> getInstanceConstraints() {
		return instanceConstraints;
	}
	

	/**
	 * Get the resource provider preferences
	 */
	public List<String> getProviderPreferences() {
		return providerPreferences;
	}
	
	
	/**
	 * Get the instance provider preferences
	 */
	public List<String> getInstancePreferences() {
		return instancePreferences;
	}
	
	
}
