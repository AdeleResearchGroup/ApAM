package fr.imag.adele.apam;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamImpl.ApamResolverImpl;
import fr.imag.adele.apam.core.Reference;
import fr.imag.adele.apam.core.ResolvableReference;

/**
 * Interface that each manager MUST implement. Used by APAM to resolve the dependencies and manage the application.
 * 
 * @author Jacky
 * 
 */

public interface Manager {

    /**
     * 
     * @return the name of that manager.
     */
    public String getName();

    /**
     * Provided that a resolution will be asked for a specification (or interface),
     * each manager is asked if it want to be involved. If this manager is not involved, it does nothing. If involved,
     * it must return the list "selPath" including itself somewhere (the order is important).
     * It can *add* constraints or preferences that will used by each manager during the resolution.
     * WARNING: Either interfaceName, interfaces or specName are needed;
     * 
     * @param specName: the name of the spec.
     * @param selPath the managers currently involved in this resolution.
     */
    public void getSelectionPathSpec(CompositeType compTypeFrom, String specName, List<Manager> selPath);

    /**
     * Provided that an implementation, known by its name, is required, each manager is asked if it want to be involved.
     * If not, does nothing. If involved it must return the list "selPath" including itself somewhere (the order is
     * important).
     * 
     * @param compTypeFrom the composite type origin of the future wire. Can be null.
     * @param implName the name of implementation to resolve.
     * @param selPath the managers currently involved in this resolution.
     */
    public void getSelectionPathImpl(CompositeType compTypeFrom, String implName, List<Manager> selPath);

    //    /**
    //     * Provided that a specification, known by its name, is required, each manager is asked if it want to be involved.
    //     * If not, does nothing. If involved it must return the list "selPath" including itself somewhere (the order is
    //     * important).
    //     * 
    //     * @param specName the name of implementation to resolve.
    //     * @param selPath the managers currently involved in this resolution.
    //     */
    //    public void getSelectionPathSpec(String specName, List<Manager> selPath);

    /**
     * Provided that a resolution will be asked for a implementation (selecting an instance),
     * each manager is asked if it want to be involved. If this manager is not involved, it does nothing. If involved,
     * it must return the list "selPath" including itself somewhere (the order is important).
     * It can *add* constraints or preferences that will used by each manager during the resolution.
     * WARNING: Either interfaceName or specName are needed;
     * 
     * @param compTypeFrom the composite type origin of the future wire. Can be null.
     * @param interfaceName the name of one of the interfaces of the specification to resolve. May be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @param selPath the managers currently involved in this resolution.
     */
    public void getSelectionPathInst(Composite compoFrom, Implementation impl,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath);

    // returns the relative priority of that manager, for the resolution algorithm
    public int getPriority();

    /**
     * A new composite, holding a model managed by this manager, has been created. The manager is supposed to read and
     * interpret that model.
     * 
     * @param model the model.
     * @param composite the new composite (or appli)
     */
    public void newComposite(ManagerModel model, CompositeType composite);

    /**
     * The manager is asked to find the "right" implementation for the provided specification, given its name.
     * If an implementation has to be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return the implementations if resolved, null otherwise
     */
    //    public Implementation resolveSpecByName(CompositeType compoType, String specName,
    //            Set<Filter> constraints, List<Filter> preferences);

    /**
     * The manager is asked to find the "right" implementation for the specification defined by the ressource it
     * implements.
     * WARNING : since a specification may implement more than one resource, it can be ambiguous.
     * If an implementation has to be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param reource the resource that specification must implement. It can be
     *            -the specification Name (new SpecificationReference (specName))
     *            -an interface name (new InterfaceReference (interfaceName))
     *            -a message name (new MessageReference (dataTypeName))
     *            - or any future resource ...
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return the implementations if resolved, null otherwise
     */
    public Implementation resolveSpecByResource(CompositeType compoType, ResolvableReference ressource,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * The manager is asked to find the implementation given its name.
     * If it must be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param implName the name of implementation to find.
     * @return the implementations if resolved, null otherwise
     */
    public Implementation findImplByName(CompositeType compoType, String implName);

    /**
     * The manager is asked to find the specification given its name.
     * If it must be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Can be null (root composite assumed).
     * @param specName the name of specification to find.
     * @return the specification if found, null otherwise
     */
    public Specification findSpecByName(CompositeType compoType, String specName);

    /**
     * The manager is asked to find the "right" instance for the required implementation.
     * If an instance must be created, it must be created inside the composite "compo".
     * 
     * @param compo the composite in which is located the calling instances. Cannot be null.
     * @param impl the implementation to resolve. Cannot be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return an instance if resolved, null otherwise
     */
    public Instance resolveImpl(Composite compo, Implementation impl, Set<Filter> constraints,
            List<Filter> preferences);

    /**
     * The manager is asked to find the all "right" instances for the required implementation.
     * If an instance must be created, it must be created inside the composite "compo".
     * 
     * @param compo the composite in which is located the calling instances. Cannot be null.
     * @param impl the implementation to resolve. Cannot be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return an instance if resolved, null otherwise
     */
    public Set<Instance> resolveImpls(Composite compo, Implementation impl, Set<Filter> constraints);

    /**
     * Once the resolution terminated, either successful or not, the managers are notified of the current
     * selection.
     * Currently, the managers cannot "undo" nor change the current selection.
     * 
     * @param client the client of that resolution
     * @param resName : either the interfaceName, the spec name or the implementation name to resolve
     *            depending on the fact newWireSpec or newWireImpl has been called.
     * @param depName : the dependency to resolve.
     * @param impl : the implementation selected
     * @param inst : the instance selected (null if cardinality multiple)
     * @param insts : the set of instances selected (null if simple cardinality)
     */
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
            Set<Instance> insts);

}
