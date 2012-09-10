package fr.imag.adele.apam;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;

//import fr.imag.adele.sam.Specification;

public interface SpecificationBroker {
    /**
     * Adds a new specification to the Apam state from its underlying platform definition
     * 
     * @param samSpec : A Apform specification.
     */
    public Specification addSpec(ApformSpecification apfSpec);

    /**
     * Creates a specification. 
     * 
     * @param specName the *logical* name of that specification
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties.
     *            return an ASM Specification
     */
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, String> properties);

    /**
     * Creates and deploys a specification.
     * 
     * @param specName the *logical* name of that specification;
     * @param url the location of the executable to deploy
     */
    public Specification createSpec(String specName, URL url);

    /**
     * WARNING : it will also destroy all implems and instances.
     * 
     * @param spec the spec to delete.
     */
    //    public void removeSpec(Specification spec);
    
    /**
     * Returns the specification with the given logical name. 
     * 
     * @param name the logical name of the specification
     * @return the abstract service or null if not deployed
     */
    public Specification getSpec(String name);

    /**
     * Returns the specification with the given logical name. If the specification
     * is not deployed waits until installed, if specified. 
     * 
     * @param name the logical name of the specification
     * @param wait whether to wait for deployment if not installed
     * @return the abstract service
     */
    public Specification getSpec(String name, boolean wait);

    /**
     * Returns all the specifications.
     * 
     * @return the abstract services
     */

    public Set<Specification> getSpecs();
    

    /**
     * Returns the specification that satisfies the goal. If goal is null all
     * the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specification that satisfies the goal
     */
    public Specification getSpec(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns all the specifications that satisfies the goal. If goal is null
     * all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specifications
     */
    public Set<Specification> getSpecs(Filter goal) throws InvalidSyntaxException;
    
    /**
     * Returns the specification that implement all and only the provided interfaces. 
     * 
     * At most one specification can satisfy that requirement (by definition of specification)
     * 
     * @param providedResources : the interfaces and messages of the required specification. 
     * 		The returned specification must provide all the interfaces and messages.
     *      providedResources cannot be null nor empty.
     * 
     * @return the specification
     */
    public Specification getSpec(Set<ResourceReference> providedResources);

    /**
     * 
     * @param resource
     * @return the first specification that implements the provided resource ! WARNING this is peligrous.
     */
    public Specification getSpecResource(ResolvableReference resource);


 

}
