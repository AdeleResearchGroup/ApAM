package fr.imag.adele.apam;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.CompositeImpl;
import fr.imag.adele.sam.Implementation;

public class CompositeTypeImpl extends ASMImplImpl implements CompositeType {

    // Global variable. The actual content of the ASM
    private static Map<String, CompositeType> compositeTypes = new HashMap<String, CompositeType>();
    private static Map<String, CompositeType> rootTypes      = new HashMap<String, CompositeType>();

    private ASMImpl                           mainImpl       = null;                                 ;
    private String                            mainImplName;
    private final Set<ManagerModel>           models;

    // All the specs, implem, instances contained in this composite ! Warning :
    // may be shared.
    private final Set<ASMImpl>                hasImplem      = new HashSet<ASMImpl>();

    // all the dependencies between composite types
    private final Set<CompositeType>          imports        = new HashSet<CompositeType>();
    private final Set<CompositeType>          invImports     = new HashSet<CompositeType>();        // reverse dependency

    private ASMInst                           firstInst      = null;

    /**
     * Get access to the internal implementation of the wrapped instance
     */
    @Override
    public Internal asInternal() {
        return asInternal();
    }

    /**
     * 
     * @param fromCompo. the father composite type. is null for root composites.
     * @param compositeName. unique name.
     * @param mainImpl. the main implementation.
     * @param models. the models. Can be null.
     * @param attributes. initial properties. Can be null.
     */
    private CompositeTypeImpl(CompositeType fromCompo, String compositeName, ASMImpl mainImpl,
            Set<ManagerModel> models, Attributes attributes, ASMInst firstInst) {
        //the composite itself as an ASMImpl
        //Warning created without composite. Partialy initialised.
        super(fromCompo, (ASMSpecImpl) mainImpl.getSpec(), mainImpl.getSamImpl(), attributes);
        this.mainImpl = mainImpl;
        this.firstInst = firstInst;
        name = compositeName; //because constructor set the name as the main impl name
        ((ASMImplImpl) mainImpl).initializeNewImpl(this, attributes); // complete attribute value init, and chainings.
        CompositeTypeImpl.compositeTypes.put(name, this);

        if (fromCompo != null) {
            fromCompo.containsImpl(this);
            inComposites.add(fromCompo);
        } else
            CompositeTypeImpl.rootTypes.put(name, this);

        if (models == null) {
            this.models = Collections.emptySet();
        } else {
            this.models = models;
            Manager man;
            for (ManagerModel managerModel : models) { // call the managers to indicate the new composite and the model
                man = CST.apam.getManager(managerModel.getManagerName());
                if (man != null) {
                    man.newComposite(managerModel, this);
                }
            }
        }
    }

    /**
     * Creates an isolated composite type.
     * A single composite with this name can exist in APAM. Returns null if name conflicts.
     * 
     * @param fromCompo : Optional : the father composite from which this one is created.
     * @param name : the symbolic name. Unique.
     * @param mainImplem : The name of the main implementation. If not found, returns null.
     * @param models optional : the associated models.
     */

    public static CompositeType createCompositeType(CompositeType fromCompo, String name, String mainImplName,
            String specName,
            Set<ManagerModel> models, Attributes attributes) {

        if (mainImplName == null) {
            new Exception("ERROR : main implementation Name missing").printStackTrace();
            return null;
        }
        if (CompositeTypeImpl.compositeTypes.get(name) != null) {
            System.err.println("Composite type " + name + " allready existing");
            return null;
        }

        //BUG : the instance should not have been created. It will be lost 
        ASMInst mainInst = ((CST.apam)).resolveAppli(fromCompo, mainImplName, null, null);
        if (mainInst == null) {
            System.err.println("cannot find main implementation " + mainImplName);
            return null;
        }
        if (specName != null)
            ((ASMSpecImpl) mainInst.getSpec()).setName(specName);
        return new CompositeTypeImpl(fromCompo, name, mainInst.getImpl(), models, attributes, mainInst);
    }

    /**
     * Create a composite type, from its object in sam : samImpl.
     * Look for its main implementation, and deploys it if needed.
     * 
     * @param implComposite : a false implementation that contains the composite type.
     * @param samImpl
     * @return
     */
    public static CompositeType createCompositeType(CompositeType implComposite, Implementation samImpl) {
        //        String implName = null;
        String specName = null;
        String mainImplName = null;
        Set<ManagerModel> models = null;
        String[] interfaces = null;
        try {
            //            implName = (String) samImpl.getProperty(CST.PROPERTY_IMPLEMENTATION_NAME);
            mainImplName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION);
            specName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_SPECIFICATION);
            models = (Set<ManagerModel>) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MODELS);
            //TODO
            //interfaces = samImpl.
        } catch (ConnectionException e) {
            e.printStackTrace();
        }

        return CompositeTypeImpl.createCompositeType(implComposite, samImpl.getName(), mainImplName, specName, models,
                null);
    }

    /**
     * Creates a composite from an URL leading to a bundle containing either the main implem, or the url of the
     * composite itself.
     * If the url of the composite, parameters name, implName and models are not used, but those found in the composite.
     * 
     * @param compoFrom. Optional (for root composites)
     * @param name. name of the new composite to create.
     * @param models. the composite models.
     * @param implName. Name of the main implem.
     * @param url : can be the url of the main implem, or the url of the composite itself.
     * @param specName. Optional. Name of the specification. If not provided it isi the specification of the main
     *            implementation.
     * @param properties
     * @return
     */
    public static CompositeType createCompositeType(CompositeType fromCompo, String name, Set<ManagerModel> models,
            String implName, URL url, String specName, Attributes properties) {
        ASMImpl mainImpl;
        mainImpl = CST.ASMImplBroker.createImpl(null, implName, url, specName, properties);
        if (mainImpl instanceof CompositeType) {
            if (fromCompo != null) {
                fromCompo.containsImpl(mainImpl);
                ((ASMImplImpl) mainImpl).addInComposites(fromCompo);
            }
            return (CompositeType) mainImpl;
        }
        return new CompositeTypeImpl(fromCompo, name, mainImpl, models, properties, null);
    }

    public static Collection<CompositeType> getRootCompositeTypes() {
        return Collections.unmodifiableCollection(CompositeTypeImpl.rootTypes.values());
    }

    public static Collection<CompositeType> getCompositeTypes() {
        return Collections.unmodifiableCollection(CompositeTypeImpl.compositeTypes.values());
    }

    public static CompositeType getCompositeType(String name) {
        return CompositeTypeImpl.compositeTypes.get(name);
    }

    //overloads the usual createInst method for ASMImpl
    @Override
    public ASMInst createInst(Composite instCompo, Attributes initialproperties) {
        ASMInst first = firstInst;
        firstInst = null;
        Composite comp = CompositeImpl.createComposite(this, instCompo, initialproperties, first);
        return comp;
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addImport(CompositeType destination) {
        imports.add(destination);
        destination.asInternal().addInvImport(this);
    }

    @Override
    public boolean removeImport(CompositeType destination) {
        destination.asInternal().removeInvImport(this);
        return imports.remove(destination);
    }

    @Override
    public Set<CompositeType> getImport() {
        return Collections.unmodifiableSet(imports);
    }

    @Override
    public boolean imports(CompositeType destination) {
        return imports.contains(destination);
    }

    @Override
    public ManagerModel getModel(String name) {
        for (ManagerModel model : models) {
            if (model.getName().equals(name))
                return model;
        }
        return null;
    }

    @Override
    public Set<ManagerModel> getModels() {
        return Collections.unmodifiableSet(models);
    }

    @Override
    public void addImpl(ASMImpl impl) {
        hasImplem.add(impl);
        ((ASMImplImpl) impl).addInComposites(this);
    }

    public void removeImpl(ASMImpl impl) {
        hasImplem.remove(impl);
        ((ASMImplImpl) impl).removeInComposites(this);
    }

    @Override
    public boolean containsImpl(ASMImpl impl) {
        return hasImplem.contains(impl);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(hasImplem);
    }
}
