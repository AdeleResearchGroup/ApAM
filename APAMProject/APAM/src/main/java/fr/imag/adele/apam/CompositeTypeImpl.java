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
import fr.imag.adele.apam.ASMImpl.ASMImplBrokerImpl;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.CompositeImpl;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class CompositeTypeImpl extends ASMImplImpl implements CompositeType {

    // Global variable. The actual content of the ASM
    private static Map<String, CompositeType> compositeTypes = new HashMap<String, CompositeType>();
    private static CompositeType              rootCompoType  = new CompositeTypeImpl();
    private int                               instNumber     = -1;

    private ASMImpl                           mainImpl       = null;                                 ;
    private Set<ManagerModel>                 models;

    // All the implementations deployed (really or logically) by this composite type!
    // Warning : implems may be deployed (logically) by more than one composite Type.
    private final Set<ASMImpl>                contains       = new HashSet<ASMImpl>();

    // The composites types that have been deployed inside the current one.
    private final Set<CompositeType>          embedded       = new HashSet<CompositeType>();
    private final Set<CompositeType>          invEmbedded    = new HashSet<CompositeType>();

    // all the dependencies between composite types
    private final Set<CompositeType>          imports        = new HashSet<CompositeType>();
    private final Set<CompositeType>          invImports     = new HashSet<CompositeType>();

    // special attributes for visibility and scope control
    private String                            internalImplementations;                              // true false
    private String                            internalInstances;                                    // true false
    private Set<String>                       globalScope;
    private Set<String>                       appliScope;
    private Set<String>                       compositeScope;
    private Set<String>                       localScope;
    private Set<String>                       localVisible;
    private Set<String>                       compositeVisible;
    private Set<String>                       globalVisible;

    // private Set<DependencyModel> dependencyModel;

    private CompositeTypeImpl() {
        name = CST.ROOTCOMPOSITETYPE;
    }

    public static CompositeType getRootCompositeType() {
        return CompositeTypeImpl.rootCompoType;
    }

    /**
     * 
     * @param fromCompo. the father composite type. is null for root composites.
     * @param compositeName. unique name.
     * @param mainImpl. the main implementation.
     * @param models. the models. Can be null.
     * @param attributes. initial properties. Can be null.
     */
    private CompositeTypeImpl(CompositeType fromCompo, String compositeName, String mainImplName, ASMImpl mainImpl,
            Set<ManagerModel> models, Attributes attributes, String specName) {
        // the composite itself as an ASMImpl. Warning created empty. Must be fully initialized.
        super();
        // The main implem resolution must be interpreted with the new models
        if (models != null) {
            Manager man;
            for (ManagerModel managerModel : models) { // call the managers to indicate the new composite and the model
                man = CST.apam.getManager(managerModel.getManagerName());
                if (man != null) {
                    man.newComposite(managerModel, this);
                }
            }
        } else
            models = Collections.emptySet();

        if (mainImpl == null) {
            mainImpl = ((CST.apam)).findImplByName(this, mainImplName);
            if (mainImpl == null) {
                System.err.println("cannot find main implementation " + mainImplName);
                return;
            }
            if (specName != null)
                ((ASMSpecImpl) mainImpl.getSpec()).setName(specName);
        }

        this.models = models;
        mySpec = mainImpl.getSpec();
        ((ASMSpecImpl) mySpec).addImpl(this);
        samImpl = mainImpl.getSamImpl();
        this.mainImpl = mainImpl;
        name = compositeName;
        ((ASMImplImpl) mainImpl).initializeNewImpl(this, null); // complete attribute value init, and chainings.
        CompositeTypeImpl.compositeTypes.put(name, this);
        ((ASMImplBrokerImpl) CST.ASMImplBroker).addImpl(this);

        fromCompo.addImpl(this);
        ((CompositeTypeImpl) fromCompo).addEmbedded(this);
        inComposites.add(fromCompo);
        initializeCompoAttributes(attributes);
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
        if (fromCompo == null)
            fromCompo = CompositeTypeImpl.rootCompoType;

        return new CompositeTypeImpl(fromCompo, name, mainImplName, null, models, attributes, specName);
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
        // String implName = null;
        String specName = null;
        String mainImplName = null;
        Set<ManagerModel> models = null;
        String[] interfaces = null;
        try {
            Attributes properties = new AttributesImpl();
            Map<String, Object> p = samImpl.getProperties();
            for (String prop : p.keySet()) {
                properties.setProperty(prop, p.get(prop));
            }
            mainImplName = (String) samImpl.getProperty(CST.A_MAIN_IMPLEMENTATION);
            specName = (String) samImpl.getProperty(CST.A_MAIN_SPECIFICATION);
            models = (Set<ManagerModel>) samImpl.getProperty(CST.A_MODELS);

            if (implComposite == null)
                implComposite = CompositeTypeImpl.rootCompoType;

            return CompositeTypeImpl.createCompositeType(implComposite, samImpl.getName(),
                    mainImplName, specName, models, properties);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initializes the attributes of the composite implementation.
     * The handler has set the specific attributes on the corresponding SAM object (when created from a bundle),
     * and/or provided when creating the composite type by API.
     * 
     * @param properties composite specific attributes
     */
    public void initializeCompoAttributes(Attributes properties) {
        if (properties == null)
            return;
        setProperties(properties.getProperties());
        internalImplementations = (String) getProperty(CST.A_INTERNALIMPL);
        internalInstances = (String) getProperty(CST.A_INTERNALINST);
        globalScope = (Set<String>) getProperty(CST.A_GLOBALSCOPE);
        appliScope = (Set<String>) getProperty(CST.A_APPLISCOPE);
        compositeScope = (Set<String>) getProperty(CST.A_COMPOSITESCOPE);
        localScope = (Set<String>) getProperty(CST.A_LOCALSCOPE);
        localVisible = (Set<String>) getProperty(CST.A_LOCALVISIBLE);
        compositeVisible = (Set<String>) getProperty(CST.A_COMPOSITEVISIBLE);
        globalVisible = (Set<String>) getProperty(CST.A_GLOBALVISIBLE);
    }

    public String getScopeInComposite(ASMInst inst) {
        String name = inst.getName();

        if (localScope != null) {
            for (String scope : localScope) {
                if (name.matches(scope)) {
                    System.out.println("overloaded local scope for " + inst + " in composite type " + this);
                    return CST.V_LOCAL;
                }
            }
        }
        if (compositeScope != null) {
            for (String scope : compositeScope) {
                if (name.matches(scope)) {
                    System.out.println("overloaded composite scope for " + inst + " in composite type " + this);
                    return CST.V_COMPOSITE;
                }
            }
        }
        if (appliScope != null) {
            for (String scope : appliScope) {
                if (name.matches(scope)) {
                    System.out.println("overloaded appli scope for " + inst + " in composite type " + this);
                    return CST.V_APPLI;
                }
            }
        }
        if (localScope != null) {
            for (String scope : globalScope) {
                if (name.matches(scope)) {
                    System.out.println("overloaded global scope for " + inst + " in composite type " + this);
                    return CST.V_GLOBAL;
                }
            }
        }
        return null;
    }

    public String getVisibleInCompoType(ASMImpl impl) {
        String name = impl.getName();
        if (localVisible != null) {
            for (String visible : localVisible) {
                if (name.matches(visible)) {
                    System.out.println("overloaded local visible for " + name + " in composite type " + this);
                    return CST.V_LOCAL;
                }
            }
        }
        if (compositeVisible != null) {
            for (String visible : compositeVisible) {
                if (name.matches(visible)) {
                    System.out.println("overloaded composite visible for " + name + " in composite type " + this);
                    return CST.V_COMPOSITE;
                }
            }
        }
        if (globalVisible != null) {
            for (String visible : globalVisible) {
                if (name.matches(visible)) {
                    System.out.println("overloaded global visible for " + name + " in composite type " + this);
                    return CST.V_GLOBAL;
                }
            }
        }
        return null;
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
        if (fromCompo == null)
            fromCompo = CompositeTypeImpl.rootCompoType;
        if (mainImpl instanceof CompositeType) {
            return (CompositeType) mainImpl;
        }
        return new CompositeTypeImpl(fromCompo, name, mainImpl.getName(), mainImpl, models, properties, null);
    }

    public static Collection<CompositeType> getRootCompositeTypes() {
        return CompositeTypeImpl.rootCompoType.getEmbedded();
    }

    public static Collection<CompositeType> getCompositeTypes() {
        return Collections.unmodifiableCollection(CompositeTypeImpl.compositeTypes.values());
    }

    public static CompositeType getCompositeType(String name) {
        return CompositeTypeImpl.compositeTypes.get(name);
    }

    // overloads the usual createInst method for ASMImpl
    @Override
    public ASMInst createInst(Composite instCompo, Attributes initialproperties) {
        Composite comp = CompositeImpl.createComposite(this, instCompo, initialproperties);
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

    public String getNewInstName() {
        instNumber = instNumber + 1;
        return name + "-" + instNumber;
    }

    @Override
    public void addImport(CompositeType destination) {
        imports.add(destination);
        ((CompositeTypeImpl) destination).addInvImport(this);
    }

    @Override
    public boolean removeImport(CompositeType destination) {
        ((CompositeTypeImpl) destination).removeInvImport(this);
        return imports.remove(destination);
    }

    public void addEmbedded(CompositeType destination) {
        embedded.add(destination);
        if (this == CompositeTypeImpl.rootCompoType)
            return;
        ((CompositeTypeImpl) destination).addInvEmbedded(this);
    }

    public boolean removeEmbedded(CompositeType destination) {
        ((CompositeTypeImpl) destination).removeInvEmbedded(this);
        return imports.remove(destination);
    }

    public void addInvEmbedded(CompositeType origin) {
        invEmbedded.add(origin);
    }

    public boolean removeInvEmbedded(CompositeType origin) {
        return invEmbedded.remove(origin);
    }

    @Override
    public Set<CompositeType> getEmbedded() {
        return Collections.unmodifiableSet(embedded);
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
        contains.add(impl);
        ((ASMImplImpl) impl).addInComposites(this);
    }

    public void removeImpl(ASMImpl impl) {
        contains.remove(impl);
        ((ASMImplImpl) impl).removeInComposites(this);
    }

    @Override
    public boolean containsImpl(ASMImpl impl) {
        return contains.contains(impl);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(contains);
    }

    public void addInvImport(CompositeType dependent) {
        invImports.add(dependent);
    }

    public boolean removeInvImport(CompositeType dependent) {

        return invImports.remove(dependent);
    }

    @Override
    public Set<CompositeType> getInvEmbedded() {
        return Collections.unmodifiableSet(invEmbedded);
    }

    @Override
    public boolean getInternal() {
        if (internalImplementations == null)
            return false;
        return internalImplementations.equals(CST.V_TRUE);
    }

    public boolean getInternalInst() {
        if (internalInstances == null)
            return false;
        return internalInstances.equals(CST.V_TRUE);
    }

    @Override
    public String toString() {
        return name;
    }
}
