package fr.imag.adele.apam;

import java.net.URL;
import java.util.Collection;
//import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.osgi.framework.Filter;


public interface Apam {

    /**
     * Simply creates an instance of the composite type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(CompositeType compositeType);
    
    /**
     * Resolve compositeTypeName and, if successful, creates an instance of that type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(String compositeTypeName);

    /**
     * deploys the bundle found at the provided URL. Looks in that bundle for a composite type
     * with name "compositeTypeName".
     * If found creates that composite type and creates an instance of that type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(URL compoTypeURL, String compositeTypeName);

 
    /**
     * Creates a root composite type i.e. an application.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * @param inCompoType: name of the father composite type. Null if root (application).
     * @param name : the symbolic name. Unique.
     * @param mainImplem : The name of the main implementation or a specification name. If not found, returns null.
     * @param models optional : the associated models.
     * @param attributes optional : the initial properties to associate with this composite type (as an implementation).
     *            @ return : the created composite type
     */
    public CompositeType createCompositeType(String inCompoType,
    		String name, String specName, String mainImplSpecName,
            Set<ManagerModel> models, Map<String, String> attributes);


    /**
     * Return the composite type of that name, if existing. Null otherwise.
     * 
     * @param name
     * @return
     */
    public CompositeType getCompositeType(String name);

    /**
     * Return all the composite types known in the system.
     * 
     * @return
     */
    public Collection<CompositeType> getCompositeTypes();

    /**
     * 
     * @return all the root composite types (embeded in the system root composite type)
     */
    public Collection<CompositeType> getRootCompositeTypes();

    /**
     * 
     * @param name
     * @return the composite of that name, null if not existing.
     */
    public Composite getComposite(String name);

    /**
     * 
     * @return return all the composites known by the system.
     */
    public Collection<Composite> getComposites();

    /**
     * 
     * @return all the root composites. Also called "applications"
     */
    public Collection<Composite> getRootComposites();

}
