package fr.imag.adele.apam.apform;

import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apamImpl.Dependency;
import fr.imag.adele.apam.apamImpl.Dependency.SpecificationDependency;

public interface ApformSpecification {
    /**
     * Return the symbolic name of that specification.
     * 
     * @return
     */
    public String getName();

    /**
     * 
     * @return the set of dependencies
     */
    public Set<SpecificationDependency> getDependencies();

    /**
     * 
     * @return the array of full interface names
     */
    public String[] getInterfaceNames();

    /**
     * 
     * @return the map of properties
     */
    public Map<String, Object> getProperties();

    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * <p>
     * 
     * @param initialproperties the initial properties
     * @return the platform instance
     */

}
