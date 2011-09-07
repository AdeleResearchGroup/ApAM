package fr.imag.adele.apam.apamAPI;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.ManagerModel;

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
     * Provided that a resolution will be asked for a wire between from and the required specification (or interface),
     * each manager is asked if it want to be involved. If not, does nothing. If so, it must return the list "involved"
     * including itself somewhere (the order is important), and perhaps, it can add the contraints that it will require
     * from each manager. WARNING: Either (or both) interfaceName or specName are needed.
     * 
     * @param interfaceName the name of one of the interfaces of the specification to resolve. May be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param initConstraints The constraints allr'eady set on that object.
     * @param involved the managers currently involved in this resolution.
     * @return The list of managers involved, including this manager if it feels involved; in the right order.
     */
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints);

    /**
     * Provided that a resolution will be asked for a wire between from and the required specification (or interface),
     * each manager is asked if it want to be involved. If not, does nothing. If so, it must return the list "involved"
     * including itself somewhere (the order is important), and perhaps, it can add the contraints that it will require
     * from each manager. WARNING: Either (or both) interfaceName or specName are needed;
     * either or both client or composite are needed.
     * 
     * 
     * @param from the instance origin of the future wire. Can be null.
     * @param composite the composite in which is located the client (if any). Cannot be null.
     * @param interfaceName the name of one of the interfaces of the specification to resolve. May be null.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return The list of managers involved, including this manager if it feels involved; in the right order.
     */
    public List<Manager> getSelectionPathSpec(ASMInst from, CompositeType compType, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences, List<Manager> involved);

    /**
     * Provided that a resolution will be asked for a wire between from and the required specification (or interface),
     * each manager is asked if it want to be involved. If not, does nothing. If so, it must return the list "involved"
     * including itself somewhere (the order is important), and perhaps, it can add the contraints that it will require
     * from each manager.
     * 
     * @param from the instance origin of the future wire. Can be null.
     * @param composite the composite in which is located the client (if any). Cannot be null.
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return The list of managers involved, including this manager if it feels involved; in the right order.
     */
    public List<Manager> getSelectionPathImpl(ASMInst from, CompositeType compType, String implName,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath);

    /**
     * The manager is asked to find the "right" resolution for the required specification (or interface).
     * If an implementation has to be created, it must be inside implComposite.
     * If an instance must be created, it must be created inside instComposite.
     * 
     * @param ImplComposite the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param InstComposite the composite in which is located the calling instances. Cannot be null.
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return an instance if resolved, null otherwise
     */
    public ASMInst resolveSpec(Composite instComposite, String interfaceName, String specName,
            Set<Filter> constraints, List<Filter> preferences);

    /**
     * The manager is asked to find the "right" resolution for the required specification (or interface).
     * If an implementation has to be created, it must be inside implComposite.
     * If an instance must be created, it must be created inside instComposite.
     * 
     * @param ImplComposite the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param InstComposite the composite in which is located the calling instances. Cannot be null.
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return all the instances if resolved, null otherwise
     */
    public Set<ASMInst> resolveSpecs(Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences);

    /**
     * The manager is asked to find the "right" resolution for the required implementation.
     * If an implementation has to be created, it must be inside implComposite.
     * If an instance must be created, it must be created inside instComposite.
     * 
     * @param ImplComposite the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param InstComposite the composite in which is located the calling instances. Cannot be null.
     * @param implName the name of implementation to resolve. May be different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return an instance if resolved, null otherwise
     */
    public ASMInst resolveImpl(CompositeType compType, Composite composite, String implName, Set<Filter> constraints,
            List<Filter> preferences);

    /**
     * The manager is asked to find the "right" resolution for the required implementation.
     * If an implementation has to be created, it must be inside implComposite.
     * If an instance must be created, it must be created inside instComposite.
     * 
     * @param from the instance origin of the future wire. Can be null.
     * @param ImplComposite the composite in which is located the calling implem (and where to create implementation, if
     *            needed). Cannot be null.
     * @param InstComposite the composite in which is located the calling instances. Cannot be null.
     * @param implName the name of implementation to resolve. May be different from SAM. May be null.
     * @param filter The constraints added by this manager.
     * @param involved the managers currently involved in this resolution.
     * @return All the instances if resolved, null otherwise
     */
    public Set<ASMInst> resolveImpls(CompositeType compType, Composite composite, String implName,
            Set<Filter> constraints, List<Filter> preferences);

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

    //Simple resolutions
    public ASMImpl resolveImplByName(Composite composite, String implName);

    public ASMImpl resolveSpecByName(Composite composite, String specName,
            Set<Filter> constraints, List<Filter> preferences);

    public ASMImpl resolveSpecByInterface(Composite composite, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences);
}
