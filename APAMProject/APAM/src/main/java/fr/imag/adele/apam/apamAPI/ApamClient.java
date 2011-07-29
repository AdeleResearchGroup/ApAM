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
     * Provided that a resolution will be asked for a wire to the required specification (or interface), each manager is
     * asked for the constraints that it will require.
     * 
     * WARNING: Either (or both) interfaceName or specName are needed.
     */
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints);

    /**
     * An APAM client instance requires to be wired with an instance implementing the specification. WARNING : if no
     * logical name is provided, since more than one specification can implement the same interface, any specification
     * implementing the provided interface (technical name of the interface) will be considered satisfactory. If found,
     * the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @return
     */
    public ASMInst newWireSpec(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * An APAM client instance requires to be wired with an instance implementing the specification. WARNING : if no
     * logical name is provided, since more than one specification can implement the same interface, any specification
     * implementing the provided interface (technical name of the interface) will be considered satisfactory. If found,
     * the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
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
     * @return
     */
    public ASMInst newWireImpl(ASMInst client, String samImplName, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name
     * @return
     */
    public Set<ASMInst> newWireImpls(ASMInst client, String samImplName, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences);

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
     * @param implName the *logical* name of that implementation; different from SAM. May be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     */
    public void newClientCallBack(String instanceName, ApamDependencyHandler client, String implName, String specName);

}
