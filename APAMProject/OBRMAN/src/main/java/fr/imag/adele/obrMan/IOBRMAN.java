package fr.imag.adele.obrMan;

import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;

public interface IOBRMAN {
    /**
     * Returns all the resources found in the associated repositories that satisfy the requirements.
     * filterStr is a string filter that is matched into the provided capability (if found).
     * constraint is a list of filter matched against the same capability.
     * A null filter is evaluated to true.
     * 
     * @param capability a capability as found in the OBR repository.xml
     * @param filterStr a String representing a constraint. can be null.
     * @param constraints a set of filters. Can be null.
     * @return all the resource matching both filterStr and all constraints.
     */
    public Set<Resource> getResources(String capability, String filterStr, Set<Filter> constraints);

    /**
     * Returns the first resource found in the associated repositories that satisfy the requirements.
     * filterStr is a string filter that is matched into the provided capability (if found).
     * constraint is a list of filter matched against the same capability.
     * A null filter is evaluated to true.
     * 
     * @param capability a capability as found in the OBR repository.xml
     * @param filterStr a String representing a constraint. can be null.
     * @param constraints a set of filters. Can be null.
     * @return a resource matching filterStr and all constraints.
     */
    public Resource getResource(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences);

    /**
     * Install and starts the associated resource. False if it could not be successfully deployed.
     * 
     * @param resource A valid resource in one of the repositories.
     * @return true if successful.
     */
    public boolean install(Resource resource);

}
