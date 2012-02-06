package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//import fr.imag.adele.apam.util.String;

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
     * The set of constraints that must be satisfied by the resource provider implementation
     */
    private final Set<String>       implementationConstraints;

    /**
     * The set of constraints that must be satisfied by the resource provider instance
     */
    private final Set<String>       instanceConstraints;

    /**
     * The list of preferences to choose among candidate service provider implementation
     */
    private final List<String>      implementationPreferences;

    /**
     * The list of preferences to choose among candidate service provider instances
     */
    private final List<String>      instancePreferences;


    public DependencyDeclaration(String name, ResourceReference resource, boolean isMultiple) {

        assert name != null;
        assert resource != null;

        this.name			= name;
        this.resource 		= resource;
        this.isMultiple 	= isMultiple;

        implementationConstraints = new HashSet<String>();
        instanceConstraints = new HashSet<String>();
        implementationPreferences = new ArrayList<String>();
        instancePreferences = new ArrayList<String>();
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

    // private Set<String> stringToFilter (set<String> stringFilter) {
    // Set<String> filters = new HashSet<String> () ;
    //            for (String filter : str) {
    // implementationConstraints.add(String.newInstance(filter));
    //            }
    //    
    //        }
    /**
     * Get the resource provider constraints
     */
    public Set<String> getImplementationConstraints() {
        return implementationConstraints;
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
    public List<String> getImplementationPreferences() {
        return implementationPreferences;
    }


    /**
     * Get the instance provider preferences
     */
    public List<String> getInstancePreferences() {
        return instancePreferences;
    }


}
