package fr.imag.adele.apam;

import java.net.URL;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;

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
        
    public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName);
    /**
     * Provided that a dependency resolution is required by client,
     * each manager is asked if it want to be involved. If this manager is not involved, it does nothing. If involved,
     * it must return the list "selPath" including itself somewhere (the order is important).
     * It can *add* constraints or preferences that will used by each manager during the resolution.
     * 
     * @param compTypeFrom the source composite type
     * @param dependency the dependency to resolve. It contains the target type and name; and the constraints. 
     * @param selPath the managers currently involved in this resolution.
     */
     public void getSelectionPath(Instance client, DependencyDeclaration dependency,  List<DependencyManager> selPath);

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
     * @param client the instance calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param dependency a dependency declaration containing the type and name of the resource. It can be
     *            -the specification Name (new SpecificationReference (specName))
     *            -an interface name (new InterfaceReference (interfaceName))
     *            -a message name (new MessageReference (dataTypeName))
     *            - or any future resource ...
     * @return the implementations if resolved, null otherwise
     */
    public Implementation resolveSpec(Instance client, DependencyDeclaration dependency);
    
    public Set<Implementation> resolveSpecs(Instance client, DependencyDeclaration dependency);

    /**
     * The manager is asked to find the component given its name and type.
     * If it must be created, it must be inside the client composite.
     * 
     * @param client the instance calling implem (and where to create the component, if
     *            needed). If null, the system root instance is assumed.
     *            The search scope is compoType. 
     * @param implName the name of implementation to find.
     * @return the implementations if resolved, null otherwise
     */
    public Instance findInstByName      (Instance client, String instName);

    public Implementation findImplByName(Instance client, String implName);
 
    public Specification findSpecByName (Instance client, String specName);

    public Component findComponentByName(Instance client, String compName);

    public Implementation findImplByDependency(Instance client, DependencyDeclaration dependency);

    /**
     * The manager is asked to find the "right" instance for the required implementation.
     * If an instance must be created, it must be created inside the client composite.
     * 
     * @param client the instance the calling the resolution. 
     * @param impl the implementation to resolve. Cannot be null.
     * @param dependency a dependency declaration containing the constraints to apply for this resolution. 
     * @return an instance if resolved, null otherwise
     */
    public Instance resolveImpl(Instance client, Implementation impl, DependencyDeclaration dependency);

    /**
     * The manager is asked to find the all "right" instances for the required implementation.
     * If an instance must be created, it must be created inside the composite "compo".
     * 
     * @param client the instance the calling the resolution. 
     * @param impl the implementation to resolve. Cannot be null.
     * @param dependency a dependency declaration containing the constraints to apply for this resolution. 
     * @return all the instances instance if resolved, null otherwise
     */
    public Set<Instance> resolveImpls(Instance client, Implementation impl, DependencyDeclaration dependency);

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
