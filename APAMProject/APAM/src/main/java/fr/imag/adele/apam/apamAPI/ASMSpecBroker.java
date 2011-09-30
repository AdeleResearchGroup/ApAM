package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Specification;

public interface ASMSpecBroker {
    /**
     * If samSpec is existing, creates the corresponding specification.
     * 
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param samSpec : A SAM specification.
     */
    public ASMSpec addSpec(String specName, Specification samSpec, Attributes properties);

    /**
     * Creates a specification. WARNING : this spec may not have any
     * corresponding spec in SAM. It does not try to create one in SAM.
     * 
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties.
     *            return an ASM Specification
     */
    public ASMSpec createSpec(String specName, String[] interfaces, Attributes properties);

    /**
     * Creates and deploys a specification. WARNING : The fact to deploy the
     * specification (the packages containing the interfaces) does not create
     * any spec in SAM. This spec may not have any corresponding spec in SAM. It
     * does not try to create one in SAM.
     * 
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param url the location of the executable to deploy
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties. return an ASM Specification
     */
    public ASMSpec createSpec(String specName, URL url, String[] interfaces, Attributes properties);

    /**
     * WARNING : it will also destroy all implems and instances.
     * 
     * @param spec the spec to delete.
     */
    public void removeSpec(ASMSpec spec);

    /**
     * return the ASM specification associated with that sam specification
     * 
     * @param samImpl
     * @return
     */
    public ASMSpec getSpec(Specification samSpec);

    /**
     * Returns the specifications currently required by the provided specification
     * 
     * @param specification the specification
     */
    public Set<ASMSpec> getRequires(ASMSpec specification);

    /**
     * Returns the specification that satisfies the goal. If goal is
     * null all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specification that satisfies the goal
     */
    public ASMSpec getSpec(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns the specification that implement all and only the provided
     * interfaces. At most one specification can satisfy that requirement (by
     * definition of specification)
     * 
     * @param interfaces : the interfaces of the required specification. The returned
     *            specification must support all the interfaces in the array.
     *            The order in which the interfaces are provided in the array is
     *            not relevant. NOTE : the SAM specification name is the
     *            concatenation separated by ";" of all the interfaces, ordered
     *            lexicographically. Cannot be null nor empty.
     * 
     * @return the specification
     */
    public ASMSpec getSpec(String[] interfaces);

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
    public ASMSpec getSpecInterf(String interfaceName);

    /**
     * Returns the specification with the given logical name. WARNING: Name is
     * the *logical* name of that specification; if not set, the naame is the sam name, i.e.the concatenation
     * separated by ";" of all the interfaces, ordered lexicographically.
     * 
     * @param name the logical name of the specification, or sam name if no
     *            logical name provided
     * @return the abstract service
     */
    public ASMSpec getSpec(String name);

    /**
     * Returns the specification with the given sam name.
     * 
     * @param samName the sam name of the specification
     * @return the abstract service
     */
    public ASMSpec getSpecSamName(String samName);

    /**
     * Returns all the specifications.
     * 
     * @return the abstract services
     */

    public Set<ASMSpec> getSpecs();

    /**
     * Returns all the specifications that satisfies the goal. If goal is null
     * all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specificaitons
     */
    public Set<ASMSpec> getSpecs(Filter goal) throws InvalidSyntaxException;

}
