package fr.imag.adele.apam.apform;

import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.util.Dependency;
import fr.imag.adele.apam.util.Dependency.ImplementationDependency;

public interface ApformImplementation {
    /**
     * Return the symbolic name of that implementation.
     * 
     * @return
     */
    public String getName();

    /**
     * 
     * @return the array of full interface names
     */
    public String[] getInterfaceNames();

    /**
     * 
     * @return the set of dependencies
     */
    public Set<ImplementationDependency> getDependencies();

    /**
     * 
     * @return the map of properties
     */
    public Map<String, Object> getProperties();

    /**
     * 
     * @return the map of properties
     */
    public Object getProperty(String key);

    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * <p>
     * 
     * @param initialproperties the initial properties
     * @return the platform instance
     */
    public ApformInstance createInstance(Map<String, Object> initialproperties);

    /**
     * If a specification exists in the platform, returns the associated spec.
     * 
     * @return
     */
    public ApformSpecification getSpecification(); // If existing. In general returns null !!

}
