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
     * The component in which this dependency is declared
     */
    private final ComponentDeclaration component;

    /**
     * The identification of the dependency in its declaring component
     */
    private final String 			name;

    /**
     * If this dependency requires several providers of the resource
     */
    private Boolean 			isMultiple;

    /**
     * The reference to the required resource. For complex dependencies, it is the specification.
     */
    private final ResourceReference resource;

    /**
     * The set of constraints that must be satisfied by the resource provider implementation
     */
    private final Set<String>       implementationConstraints;

    /**
     * The set of constraints that must be satisfied by the resource provider instance
     */
    private final Set<String> 		instanceConstraints;

    /**
     * The list of preferences to choose among candidate service provider implementation
     */
    private final List<String>      implementationPreferences;

    /**
     * The list of preferences to choose among candidate service provider instances
     */
    private final List<String> 		instancePreferences;

    /**
     * The list of fields that will be injected with this dependency in a primitive component
     */
    private final List<DependencyInjection> injections;

    @Override
    public String toString() {
        String ret = "\n      dependency name: " + name + ". isMultiple: " + isMultiple + ". toward " + resource;
        if (injections.size() != 0) {
            for (DependencyInjection inj : injections) {
                ret += "\n         " + inj;
            }
        }
        if (implementationConstraints.size() != 0) {
            ret += "\n         Implementation Constraints";
            for (String inj : implementationConstraints) {
                ret += "\n            " + inj;
            }
        }
        if (instanceConstraints.size() != 0) {
            ret += "\n         Instance Constraints";
            for (String inj : instanceConstraints) {
                ret += "\n            " + inj;
            }
        }
        if (implementationPreferences.size() != 0) {
            ret += "\n         Implementation Preferences";
            for (String inj : implementationPreferences) {
                ret += "\n            " + inj;
            }
        }
        if (instancePreferences.size() != 0) {
            ret += "\n         Instance Preferences";
            for (String inj : instancePreferences) {
                ret += "\n            " + inj;
            }
        }
        return ret;
    }
    /**
     * The list of promotions for this dependency in a composite component
     */
    private final List<DependencyPromotion> promotions;

    public DependencyDeclaration(ComponentDeclaration component, String name, ResourceReference resource, Boolean isMultiple) {

        assert component != null;
        assert name != null;
        assert resource != null;

        // Bidirectional reference to encompassing declaration
        this.component		= component;
        this.component.getDependencies().add(this);

        this.name			= name;
        this.resource 		= resource;
        this.isMultiple 	= isMultiple;

        implementationConstraints 	= new HashSet<String>();
        instanceConstraints 		= new HashSet<String>();
        implementationPreferences 	= new ArrayList<String>();
        instancePreferences 		= new ArrayList<String>();

        injections			= new ArrayList<DependencyInjection>();
        promotions			= new ArrayList<DependencyPromotion>();
    }

    /**
     * The defining component
     */
    public ComponentDeclaration getComponent() {
        return component;
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
    public Boolean isMultiple() {
        return isMultiple;
    }

    public void setMultiple(boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

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

    /**
     * Get the injections associated to this dependency declaration
     */
    public List<DependencyInjection> getInjections() {
        return injections;
    }

    /**
     * Get the promotions associated to this dependency declaration
     */
    public List<DependencyPromotion> getPromotions() {
        return promotions;
    }

}
