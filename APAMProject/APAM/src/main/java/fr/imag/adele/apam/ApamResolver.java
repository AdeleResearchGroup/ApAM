package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamImpl.APAMImpl;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

public interface ApamResolver {


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
     * Look for an implementation with a given name "implName", visible from composite Type compoType.
     * 
     * @param compoType
     * @param implName
     * @return
     */
    public Implementation findImplByName(CompositeType compoTypeFrom, String implName);

    public Specification findSpecByName(CompositeType compTypeFrom, String specName);

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
    public Implementation resolveSpecByName(CompositeType compoTypeFrom, String specName,
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
    public Implementation resolveSpecByResource(CompositeType compoTypeFrom, DependencyDeclaration dependency);

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
    public Instance resolveImpl(Composite compo, Implementation impl,
            DependencyDeclaration dependency);

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
    public Set<Instance> resolveImpls(Composite compo, Implementation impl, Set<Filter> constraints);


}
