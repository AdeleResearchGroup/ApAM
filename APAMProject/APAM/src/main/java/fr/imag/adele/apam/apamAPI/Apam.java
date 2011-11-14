package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public interface Apam {

    /**
     * Creates an isolated composite type.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * @param compositeTypeName : the symbolic name. Unique.
     * @param mainImplem : The name of the main implementation. If not found, returns null.
     * @param models optional : the associated models.
     * @param attributes optional : the initial properties to associate with this composite type (as an implementation).
     *            @ return : the created composite type
     */

    public CompositeType createCompositeType(String compositeTypeName, String mainImplName,
            Set<ManagerModel> models, Attributes attributes);

    /**
     * Creates an isolated composite type.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * Creates a composite from an URL leading to a bundle containing either the main implem, or the url of the
     * composite itself.
     * 
     * @param namcompositeTypeNamee. name of the new composite to create. Unique.
     * @param mainImplName. Name of the main implem. To be found in the bundle. If not found, returns null.
     * @param models. the composite models.
     * @param bundle : URL leading to a bundle containing either the main implementation or the composite.
     *            If main implementation bundle, and implName not found returns null.
     *            if Composite bundle, name, implName and models are not used since found in the composite bundle.
     * @param specName. Optional.The symbolic name of the associated specification. Otherwise it will be the interface
     *            concatenation.
     * @param attributes optional : the initial properties to associate with this composite type (as an implementation).
     */
    public CompositeType createCompositeType(String compositeTypeName, String mainImplName, Set<ManagerModel> models,
            URL bundle, String specName, Attributes properties);

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

    // starting a new application is starting a composite.
    /**
     * Resolve compositeTypeName and, if successful, creates an instance of that type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(String compositeTypeName);

    /**
     * deploys the bundle found at the provided URL. Looks in that bundle for a composite type
     * with name "compositeTypeName.
     * If found creates that composite type and creates an instance of that type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(URL compoTypeURL, String compositeTypeName);

    /**
     * Simply creates an instance of the composite type.
     * It starts either because it implements the ApamComponnent interface,
     * of calling its getServiceObject method.
     */
    public Composite startAppli(CompositeType compositeType);

}
