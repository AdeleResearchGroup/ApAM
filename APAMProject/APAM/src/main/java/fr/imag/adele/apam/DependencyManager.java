package fr.imag.adele.apam;

import java.net.URL;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;

/**
 * Interface that each manager MUST implement. Used by APAM to resolve the dependencies and manage the application.
 * 
 * @author Jacky
 * 
 */

public interface DependencyManager {

    /**
     * 
     * @return the name of that manager.
     */
    public String getName();

    public interface ComponentBundle {
     	URL    getBundelURL () ;
    	public Set<String> getComponents ();
    }
    
    public Component install (ComponentBundle selected) ;
    
    public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName);
    /**
     * Provided that a dependency resolution is required,
     * each manager is asked if it want to be involved. If this manager is not involved, it does nothing. If involved,
     * it must return the list "selPath" including itself somewhere (the order is important).
     * It can *add* constraints or preferences that will used by each manager during the resolution.
     * 
     * @param compTypeFrom the source composite type
     * @param dependency the dependency to resolve. It contains the target type and name; and the constraints. 
     * @param selPath the managers currently involved in this resolution.
     */
     public void getSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dependency,  List<DependencyManager> selPath);

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
     * The manager is asked to find the "right" implementation for the specification defined by the resources it
     * implements.
     * WARNING : since a specification may implement more than one resource, it can be ambiguous.
     * If an implementation has to be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param dependency a dependency declaration containing the type and name of the resource. It can be
     *            -the specification Name (new SpecificationReference (specName))
     *            -an interface name (new InterfaceReference (interfaceName))
     *            -a message name (new MessageReference (dataTypeName))
     *            - or any future resource ...
     * @return the implementations if resolved, null otherwise
     */
    public Implementation resolveSpec(CompositeType compoTypeFrom, DependencyDeclaration dependency);
    
    public Set<Implementation> resolveSpecs(CompositeType compoTypeFrom, DependencyDeclaration dependency);

    /**
     * The manager is asked to find the implementation given its name.
     * If it must be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). If null, the system root composite is assumed.
     *            The search scope is compoType. 
     * @param compo the composite in which is located the calling implem
     * @param implName the name of implementation to find.
     * @return the implementations if resolved, null otherwise
     */
    public Implementation findImplByName(CompositeType compoType, String implName);
    
    /**
     * The manager is asked to find the component given its name.
     * If it must be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create component, if
     *            needed). If null, the system root composite is assumed.
     *            The search scope is compoType. 
     * @param compo the composite in which is located the calling implem      * @param implName the name of implementation to find.
     * @return the implementations if resolved, null otherwise
     */
    public Component findComponentByName(CompositeType compoType, String componentName);

     /**
     * The manager is asked to find the specification given its name.
     * If it must be created, it must be inside compoType.
     * 
     * @param compoType the composite in which is located the calling implem (and where to create implementation, if
     *            needed). If null, root composite is assumed.
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
     * @param dependency a dependency declaration containing the constraints to apply for this resolution. 
     * @return an instance if resolved, null otherwise
     */
    public Instance resolveImpl(Composite compo, Implementation impl, DependencyDeclaration dependency);

    /**
     * The manager is asked to find the all "right" instances for the required implementation.
     * If an instance must be created, it must be created inside the composite "compo".
     * 
     * @param compo the composite in which is located the calling instances. Cannot be null.
     * @param impl the implementation to resolve. Cannot be null.
     * @param dependency a dependency declaration containing the constraints to apply for this resolution. 
     * @return all the instances instance if resolved, null otherwise
     */
    public Set<Instance> resolveImpls(Composite compo, Implementation impl, DependencyDeclaration dependency);

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
