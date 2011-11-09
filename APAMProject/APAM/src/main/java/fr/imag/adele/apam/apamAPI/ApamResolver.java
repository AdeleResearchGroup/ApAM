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
public interface ApamResolver {

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
     * Look for an implementation with a given name "implName", visible from composite Type compoType.
     * 
     * @param compoType
     * @param implName
     * @return
     */
    public ASMImpl findImplByName(CompositeType compoType, String implName);

    /**
     * First looks for the specification defined by its name, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param specName
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     *            number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public ASMImpl resolveSpecByName(CompositeType compoType, String specName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * First looks for the specification defined by its interface, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param interfaceName. The full name of one of the interfaces of the specification.
     *            WARNING : different specifications may share the same interface.
     * @param interfaces. The complete list of interface of the specification. At most one specification can be
     *            selected.
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     *            number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public ASMImpl resolveSpecByInterface(CompositeType compoType, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * Look for an instance of "impl" that satisfies the constraints. That instance must be either shared and visible
     * from "compo",
     * or instantiated if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     * @return
     */
    public ASMInst resolveImpl(Composite compo, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences);

    /**
     * Look for all the existing instance of "impl" that satisfy the constraints.
     * These instances must be either shared and visible from "compo".
     * If no existing instance can be found, one is created if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @return
     */
    public Set<ASMInst> resolveImpls(Composite compo, ASMImpl impl, Set<Filter> constraints);

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
