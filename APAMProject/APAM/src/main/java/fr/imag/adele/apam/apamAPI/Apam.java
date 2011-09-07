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

    //Getting the brokers
    public ASMSpecBroker getSpecBroker();

    public ASMImplBroker getImplBroker();

    public ASMInstBroker getInstBroker();

    // Composite type creation. 
    /**
     * Creates an isolated composite type from its representation in SAM.
     * 
     * @param samImpl : An implementation in sam containing the composite.
     */
    public CompositeType createCompositeType(Implementation samImpl);

    /**
     * Creates an isolated composite type.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * @param name : the symbolic name. Unique.
     * @param mainImplem : The name of the main implementation. If not found, returns null.
     * @param models optional : the associated models.
     */

    public CompositeType createCompositeType(String name, String mainImplName,
            Set<ManagerModel> models, Attributes attributes);

    /**
     * Creates an isolated composite type.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * Creates a composite from an URL leading to a bundle containing either the main implem, or the url of the
     * composite itself.
     * 
     * @param name. name of the new composite to create. Unique.
     * @param models. the composite models.
     * @param implName. Name of the main implem. To be found in the bundle. If not found, returns null.
     * @param bundle : URL leading to a bundle containing either the main implementation or the composite.
     *            If main implementation bundle, and implName not found returns null.
     *            if Composite bundle, name, implName and models are not used since found in the composite bundle.
     */
    public CompositeType createCompositeType(String name, String implName, Set<ManagerModel> models,
            URL bundle, String specName, Attributes properties);

    public CompositeType getCompositeType(String name);

    public Collection<CompositeType> getCompositeTypes();

    public Collection<CompositeType> getRootCompositeTypes();

    public Composite getComposite(String name);

    public Collection<Composite> getComposites();

    public Collection<Composite> getRootComposites();

    // starting a new application is starting a composite.
    public Composite startAppli(String compositeTypeName);

    public Composite startAppli(URL compoTypeURL, String compositeName);

    public Composite startAppli(CompositeType composite);

    //Simple resolutions
    public ASMInst resolveImplByName(Composite composite, String implName);

    public ASMInst resolveSpecByName(Composite composite, String specName,
            Set<Filter> constraints, List<Filter> preferences);

    public ASMInst resolveSpecByInterface(Composite composite, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences);
}
