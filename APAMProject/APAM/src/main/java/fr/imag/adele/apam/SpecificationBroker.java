package fr.imag.adele.apam;

import java.net.URL;
//import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apform.ApformSpecification;
//import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.ResourceReference;

//import fr.imag.adele.sam.Specification;

public interface SpecificationBroker {
    /**
     * If samSpec is existing, creates the corresponding specification.
     * 
     * @param specName the *logical* name of that specification; different from Apform. May be null.
     * @param samSpec : A Apform specification.
     */
    public Specification addSpec(String specName, ApformSpecification apfSpec, Map<String, Object> properties);

    /**
     * Creates a specification. WARNING : this spec may not have any
     * corresponding spec in Apform. It does not try to create one in Apform.
     * 
     * @param specName the *logical* name of that specification; different from Apform. May be null.
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties.
     *            return an ASM Specification
     */
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, Object> properties);

    /**
     * Creates and deploys a specification.
     * 
     * @param specName the *logical* name of that specification; different from Apform. May be null.
     * @param url the location of the executable to deploy
     */
    public Specification createSpec(String specName, URL url);

    /**
     * WARNING : it will also destroy all implems and instances.
     * 
     * @param spec the spec to delete.
     */
    public void removeSpec(Specification spec);

    /**
     * return the ASM specification associated with that Apform specification
     * 
     * @param ApformImpl
     * @return
     */
    public Specification getSpec(ApformSpecification ApfSpec);

    /**
     * Returns the specifications currently required by the provided specification
     * 
     * @param specification the specification
     */
    public Set<Specification> getRequires(Specification specification);

    /**
     * Returns the specification that satisfies the goal. If goal is
     * null all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specification that satisfies the goal
     */
    public Specification getSpec(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns the specification that implement all and only the provided
     * interfaces. At most one specification can satisfy that requirement (by
     * definition of specification)
     * 
     * @param providedResources : the interfaces and messages of the required specification. The returned
     *            specification must support all the interfaces and messages.
     *            providedResources cannot be null nor empty.
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

    /**
     * Returns *the first* specification that implements the provided
     * interface. WARNING : the same interface can be implemented by different
     * specifications, and a specification may implement more than one interface
     * : the first spec found is returned. WARNING : convenient only if a single
     * spec provides that interface; otherwise it is non deterministic.
     * 
     * @param interfaceName : the name of the interface of the required specification.
     * @return the specification
     */
    //    public Specification getSpecInterf(String interfaceName);


    /**
     * Returns the specification with the given logical name. WARNING: Name is
     * the *logical* name of that specification; if not set, the naame is the Apf name, i.e.the concatenation
     * separated by ";" of all the interfaces, ordered lexicographically.
     * 
     * @param name the logical name of the specification, or Apf name if no
     *            logical name provided
     * @return the abstract service
     */
    public Specification getSpec(String name);

    /**
     * Returns the specification with the given Apf name.
     * 
     * @param ApfName the Apf name of the specification
     * @return the abstract service
     */
    public Specification getSpecApfName(String ApfName);

    /**
     * Returns all the specifications.
     * 
     * @return the abstract services
     */

    public Set<Specification> getSpecs();

    /**
     * Returns all the specifications that satisfies the goal. If goal is null
     * all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specificaitons
     */
    public Set<Specification> getSpecs(Filter goal) throws InvalidSyntaxException;

}
