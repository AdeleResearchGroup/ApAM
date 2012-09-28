package fr.imag.adele.apam;

import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.core.ComponentDeclaration;

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
     * Whether the component is exclusive
     */
    public boolean isExclusive() ;

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
	public Map<String, String> getAllProperties();

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

}
