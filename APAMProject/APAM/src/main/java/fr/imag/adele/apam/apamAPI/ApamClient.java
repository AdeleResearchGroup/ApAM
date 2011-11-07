package fr.imag.adele.apam.apamAPI;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

/**
 * Interface called by client instances.
 * 
 * @author Jacky
 * 
 */
public interface ApamClient {

    /**
     * An APAM client instance requires to be wired with an instance implementing the specification. WARNING : if no
     * logical name is provided, since more than one specification can implement the same interface, any specification
     * implementing the provided interface (technical name of the interface) will be considered satisfactory. If found,
     * the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param depName the name of the dependency; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return
     */
    public ASMInst newWireSpec(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * An APAM client instance requires to be wired with all the instance implementing the specification and satisfying
     * the constraints.
     * WARNING : if no specification name is provided, since more than one specification can implement the same
     * interface,
     * any specification implementing at least the provided interface (technical name of the interface) will be
     * considered satisfactory.
     * If found, the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param depName the name of the dependency; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return
     */
    public Set<ASMInst> newWireSpecs(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return
     */
    public ASMInst newWireImpl(ASMInst client, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name
     * @param constraints The constraints for this resolution.
     * @return
     */
    public Set<ASMInst> newWireImpls(ASMInst client, String implName, String depName,
            Set<Filter> constraints);

    /**
     * In the case a client realizes that a dependency disappeared, it has to call this method. APAM will try to resolve
     * the problem (DYNAMAM in practice), and return a new instance.
     * 
     * @param client the instance that looses it dependency
     * @param lostInstance the instance that disappeared.
     * @return
     */
    public ASMInst faultWire(ASMInst client, ASMInst lostInstance, String depName);

    /**
     * This method has to be called by a client instance when it is created. It allows APAM to know where is the
     * dependency manager attached to the instance. This dependency manager (an iPOJO Handler currently) must implement
     * the ApamDependencyHandler interface.
     * 
     * @param instanceName the name of that instance, as it will be returned by SAM
     * @param client the dependency handler (this)
     */
    public void newClientCallBack(String instanceName, ApamDependencyHandler client);

    /*
     *     public void newInstance       (String instanceName, ApformInstance client);
     *     public void newImplementation (String implemName, ApformImplementation client);
     *     public void newSpecification  (String specName, ApformSpecification client);
     *     
     *     public void vanishInstance (String instanceName) ;
     *     public void vanishImplementation (String implementationName) ;
     *     public void vanishSpecification (String specificationName) ;
     */

    /**
     * Before to resolve a specification (i.e. to select one of its implementations)
     * defined by one interface, all its interfaces, or its name, this method is called to
     * know which managers are involved, and what are the constraints and preferences set by the managers to this
     * resolution.
     * 
     * @param compTypeFrom : the origin of this resolution.
     * @param interfaceName : the full name of one of the interfaces of the specification.
     * @param interfaces : the full list of interfaces of the specificaiton.
     * @param specName : the name of the specification.
     * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
     * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
     * @return : the managers that will be called for that resolution.
     */
    public List<Manager> computeSelectionPathSpec(CompositeType compTypeFrom, String interfaceName,
            String[] interfaces, String specName, Set<Filter> constraints, List<Filter> preferences);

    /**
     * Before to resolve an implementation (i.e. to select one of its instance), this method is called to
     * know which managers are involved, and what are the constraints and preferences set by the managers to this
     * resolution.
     * 
     * @param compTypeFrom : the origin of this resolution.
     * @param impl : the implementation to resolve.
     * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
     * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
     * @return : the managers that will be called for that resolution.
     */
    public List<Manager> computeSelectionPathInst(Composite compoFrom, ASMImpl impl,
            Set<Filter> constraints, List<Filter> preferences);

}
