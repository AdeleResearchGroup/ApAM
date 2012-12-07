package fr.imag.adele.apam;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;

public interface Component {

    /**
     * The name of the component
     */
    public String getName();

    /**
     * The underlying entity in the execution platform
     */
    public ApformComponent getApformComponent();

    /**
     * The component declaration
     */
    public ComponentDeclaration getDeclaration () ;

    /**
     * Give the composite type that physically deployed that component.
     * Warning : null when unused. 
     */
    public CompositeType getFirstDeployed() ;

    /**
     * Whether the component is instantiable
     */
    public boolean isInstantiable() ;

    /**
     * Whether the component is singleton
     */
    public boolean isSingleton() ;

    /**
     * Whether the component is shared
     */
    public boolean isShared() ;

    /**
     * Get a component provided its name
     * @param name
     * @return
     */
//    public Component getComponent (String name) ;
    /**
     * Return a candidate, selected (by default) among those satisfying the constraints
     * @param <T>
     * @param candidates
     * @param constraints
     * @return
     */
    public <T extends Component> T getSelectedComponent(Set<T> candidates, Set<Filter> constraints) ;

    /**
     * Return the sub-set of candidates that satisfy all the constraints
     * @param <T>
     * @param candidates
     * @param constraints
     * @return
     */
    public <T extends Component> Set<T> getSelectedComponents(Set<T> candidates, Set<Filter> constraints) ;

	/**
	 * Return the candidates that best matches the preferences
	 * Take the preferences in orden: m candidates
	 * find  the n candidates that match the constraint.
	 * 		if n= 0 ignore the constraint
	 *      if n=1 return it.
	 * iterate with the n candidates.
	 * At the end, if n > 1 return the default one.
	 * 
	 * @param <T>
	 * @param candidates
	 * @param preferences
	 * @return
	 */
	public <T extends Component> T getPrefered (Set<T> candidates, List<Filter> preferences ) ;

	/**
	 * Return the set (if no preferences), of component matching the constraints, and then selected following the preferences.
	 * If preferences set, return a single component, otherwise returns all the components matching the constraints.
	 * @param <T> A component type
	 * @param candidates
	 * @param preferences
	 * @param constraints
	 * @return
	 */
	public <T extends Component> Set<T> getConstraintsPreferedComponent(Set<T> candidates, List<Filter> preferences, Set<Filter> constraints) ;

	/**
	 * returns the component for which at least one member satisfies the constraints and preferences.
	 * If none, returns the default component.
	 */
	public <T extends Component> T getPreferedComponentFromMembers(Set<T> candidates, List<Filter> memberPreferences, Set<Filter> memberConstraints) ;

    /**
     * Return the "best" component among the candidates. 
     * Best depends on the component nature. 
     * For implems, it is those that have sharable instance or that is instantiable.
     * @param <T>
     * @param candidates
     * @return
     */
    public <T extends Component> T getDefaultComponent (Set<T> candidates) ;

    /**
     * Match.
     *
     * @param goal the goal
     * @return true is the instance matches the goal
     */
    public boolean match(String goal);

    /**
     * Match.
     *
     * @param goal the goal
     * @return true is the instance matches the goal
     */
    public boolean match(Filter goal);

    /**
     * return true if the instance matches ALL the constraints in the set.
     *
     * @param goals
     * @return
     */
    public boolean match(Set<Filter> goals);

    /**
     * Get the value of a property, the property can be valued in this component or in its
     * defining group
     */
    public String getProperty(String attribute);

    /**
     * Set the value of the property for this component
     */
    public boolean setProperty(String attr, String value);

    /**
     * Get the value of all the properties of the component, including those in the enclosing
     * groups
     */
    public Map<String, Object> getAllProperties();

    /**
     * Change the value of the specified properties of the component
     */
    public boolean setAllProperties(Map<String, String> properties);

    /**
     * Removes the specified property of the component
     */
    public boolean removeProperty(String attr);

    /**
     * return all the members of this component. Null if leaf (instance).
     * @return
     */
    public Set<? extends Component> getMembers ();

    /**
     * return the representant of this group member. Null if root (Specification)
     */
    public Component getGroup ();

    public Map<String, String> getValidAttributes () ;
    
    public Set<ResourceReference> getAllProvidedResources () ;


}
