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
    public Object getProperty(String attribute);

    /**
     * Set the value of the property for this component
     */
	public boolean setProperty(String attr, Object value);

    /**
     * Get the value of all the properties of the component, including those in the enclosing
     * groups
     */
	public Map<String, Object> getAllProperties();

	/**
	 * Change the value of the specified properties of the component
	 */
    public boolean setAllProperties(Map<String, Object> properties);

}
