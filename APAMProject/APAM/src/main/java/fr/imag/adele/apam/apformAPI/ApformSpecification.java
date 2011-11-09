package fr.imag.adele.apam.apformAPI;

import java.util.Map;

public interface ApformSpecification {
    /**
     * Return the symbolic name of that specification.
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
