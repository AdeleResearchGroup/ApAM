package fr.imag.adele.apam;

import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.declarations.DependencyDeclaration;

public interface ApamResolver {

	/**
	 * Ask to update an existing component. It will uninstall the bundle in which this component is located. 
	 * If the new bundle does not contain the same components, it will make some side effects. 
	 * @param toUpdate
	 * @return
	 */
	public void updateComponent (String componentName) ;

	
    /**
     * An APAM client instance requires to be wired with one or all the instance that satisfy the dependency.
     * WARNING : in case of interface or message dependency , since more than one specification can implement the same
     * interface, any specification implementing at least the provided interface (technical name of the interface) will
     * be
     * considered satisfactory.
     * If found, the instance(s) are bound is returned.
     * 
     * @param client the instance that requires the specification
     * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
     * @return
     */
    public boolean resolveWire(Instance client, String depName);


    /**
     * Look for an implementation with a given name "implName", visible from composite Type compo or compoType.
     * if compo or compoType is null, root composite are assumed
     * 
     * @param compoType
     * @param implName
     * @return
     */
    public Instance       findInstByName(Instance client, String instName);

    public Implementation findImplByName(Instance client, String implName);

    public Specification  findSpecByName(Instance client, String specName);

    public Component findComponentByName(Instance client, String compName);

	public Implementation findImplByDependency (Instance client, DependencyDeclaration dep) ;


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
    public Implementation resolveSpecByName(Instance client, String specName,
            Set<String> constraints, List<String> preferences);

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
    public Implementation resolveSpecByInterface(Instance client,  String interfaceName, Set<String> constraints, List<String> preferences);
    public Implementation resolveSpecByMessage  (Instance client,  String messageName,   Set<String> constraints, List<String> preferences);

    /**
     * Look for an instance of "impl" that satisfies the constraints. That instance must be either
     * - shared and visible from "compo", or
     * - instantiated if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     * @return
     */
    public Instance resolveImpl(Instance client, Implementation impl, Set<String> constraints, List<String> preferences);

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
    public Set<Instance> resolveImpls(Instance client, Implementation impl, Set<String> constraints);


}
