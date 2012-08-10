package fr.imag.adele.apam;

import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.core.ComponentDeclaration;

public interface Component {

    public String getName();

    public ApformComponent getApformComponent();
    
    public ComponentDeclaration getDeclaration () ;


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
     * Match.
     * 
     * @param goal the goal
     * @return true is the instance matches the goal
     */
    public boolean match(String filter);

    
	//Properties
	public Map<String, Object> getAllProperties();

    public Object getProperty(String attr);

    public boolean setProperty(String attr, Object value);

    public boolean setAllProperties(Map<String, Object> properties);

}
