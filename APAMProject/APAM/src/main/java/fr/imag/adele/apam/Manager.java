package fr.imag.adele.apam;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamImpl.ManagerModel;

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
     * @param compTypeFrom the composite type origin of the future wire. Can be null.
     * @param interfaceName the name of one of the interfaces of the specification to resolve. May be null.
     * @param interfaces the complete list of interface for that specification. May be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @param selPath the managers currently involved in this resolution.
     */
    public void getSelectionPathSpec(CompositeType compTypeFrom, String interfaceName, String[] interfaces,
            String specName, Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath);

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
    public void getSelectionPathInst(Composite compoFrom, ASMImpl impl,
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
    public ASMImpl resolveSpecByName(CompositeType compoType, String specName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * The manager is asked to find the "right" implementation for the provided specification, given its interfaces.
     * If an implementation has to be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param interfaceName the name of one of one of the interfaces of the specification to resolve.
     *            WARNING : since a specification may implement more than one interface, it can be ambiguous.
     * @param interfaces the complete list of interfaces of the specification to resolve.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return the implementations if resolved, null otherwise
     */
    public ASMImpl resolveSpecByInterface(CompositeType compoType, String interfaceName, String[] interfaces,
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
    public ASMImpl findImplByName(CompositeType compoType, String implName);

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
    public ASMInst resolveImpl(Composite compo, ASMImpl impl, Set<Filter> constraints,
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
    public Set<ASMInst> resolveImpls(Composite compo, ASMImpl impl, Set<Filter> constraints);

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
    public void notifySelection(ASMInst client, String resName, String depName, ASMImpl impl, ASMInst inst,
            Set<ASMInst> insts);

}
