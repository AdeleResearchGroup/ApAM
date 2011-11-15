package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

//import fr.imag.adele.sam.Implementation;

public class CompositeTypeImpl extends ImplementationImpl implements CompositeType {

    // Global variable. The actual content of the ASM
    private static Map<String, CompositeType> compositeTypes = new HashMap<String, CompositeType>();
    private static CompositeType              rootCompoType  = new CompositeTypeImpl();
    private int                               instNumber     = -1;

    private Implementation                           mainImpl       = null;
    private Set<ManagerModel>                 models;

    // All the implementations deployed (really or logically) by this composite type!
    // Warning : implems may be deployed (logically) by more than one composite Type.
    private final Set<Implementation>                contains       = new HashSet<Implementation>();

    // The composites types that have been deployed inside the current one.
    private final Set<CompositeType>          embedded       = new HashSet<CompositeType>();
    private final Set<CompositeType>          invEmbedded    = new HashSet<CompositeType>();

    // all the dependencies between composite types
    private final Set<CompositeType>          imports        = new HashSet<CompositeType>();
    private final Set<CompositeType>          invImports     = new HashSet<CompositeType>();

    private CompositeTypeImpl() {
        name = CST.ROOTCOMPOSITETYPE;
    }

    public static CompositeType getRootCompositeType() {
        return CompositeTypeImpl.rootCompoType;
    }

    /*
     * Called once when initializing CompositeImpl. 
     */
    public static CompositeType getRootCompositeType(Composite compo) {
        ((ImplementationImpl) CompositeTypeImpl.rootCompoType).addInst(compo);
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
    private CompositeTypeImpl(CompositeType fromCompo, String compositeName, String mainImplName, Implementation mainImpl,
            Set<ManagerModel> models, Attributes attributes, String specName) {
        // the composite itself as an ASMImpl. Warning created empty. Must be fully initialized.
        super();
        // The main implem resolution must be interpreted with the new models
        if (models != null) {
            Manager man;
            for (ManagerModel managerModel : models) { // call the managers to indicate the new composite and the model
                man = ApamManagers.getManager(managerModel.getManagerName());
                if (man != null) {
                    man.newComposite(managerModel, this);
                }
            }
        } else
            models = Collections.emptySet();

        if (mainImpl == null) {
            mainImpl = ApamResolver.findImplByName(this, mainImplName);
            if (mainImpl == null) {
                System.err.println("cannot find main implementation " + mainImplName);
                return;
            }
            if (specName != null)
                ((SpecificationImpl) mainImpl.getSpec()).setName(specName);
        }

        this.models = models;
        mySpec = mainImpl.getSpec();
        ((SpecificationImpl) mySpec).addImpl(this);
        apfImpl = mainImpl.getApformImpl();
        this.mainImpl = mainImpl;
        name = compositeName;
        ((ImplementationImpl) mainImpl).initializeNewImpl(this, null); // complete attribute value init, and chainings.
        if (attributes != null)
            setProperties(attributes.getProperties());
        setProperty(Attributes.APAMCOMPO, fromCompo.getName());
        CompositeTypeImpl.compositeTypes.put(name, this);
        ((ImplementationBrokerImpl) CST.ASMImplBroker).addImpl(this);

        fromCompo.addImpl(this);
        ((CompositeTypeImpl) fromCompo).addEmbedded(this);
        inComposites.add(fromCompo);
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
        if (fromCompo == null) {
            fromCompo = CompositeTypeImpl.rootCompoType;
            if (attributes == null)
                attributes = new AttributesImpl();
            attributes.setProperty(CST.A_VISIBLE, CST.V_LOCAL);
        }

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
    public static CompositeType createCompositeType(CompositeType implComposite, ApformImplementation samImpl) {
        // String implName = null;
        String specName = null;
        String mainImplName = null;
        Set<ManagerModel> models = null;
        String[] interfaces = null;
        Attributes properties = new AttributesImpl();
        Map<String, Object> p = samImpl.getProperties();
        for (String prop : p.keySet()) {
            properties.setProperty(prop, p.get(prop));
        }
        mainImplName = (String) samImpl.getProperty(CST.A_MAIN_IMPLEMENTATION);
        specName = (String) samImpl.getProperty(CST.A_APAMSPECNAME);
        models = (Set<ManagerModel>) samImpl.getProperty(CST.A_MODELS);

        if (implComposite == null) {
            implComposite = CompositeTypeImpl.rootCompoType;
            properties.setProperty(CST.A_VISIBLE, CST.V_LOCAL);
        }

        return CompositeTypeImpl.createCompositeType(implComposite, samImpl.getName(),
                    mainImplName, specName, models, properties);
    }

    public String getScopeInComposite(Instance inst) {
        String overload = getScopeOverload(inst);
        return CompositeTypeImpl.getEffectiveScope((String) inst.getProperty(CST.A_SCOPE), overload);
    }

    private String getScopeOverload(Instance inst) {
        // the last scope for error message
        String s = "";
        String name = inst.getSpec().getName();
        try {
            String[] localScope = (String[]) getProperty(CST.A_LOCALSCOPE);
            if (localScope != null) {
                for (String scope : localScope) {
                    s = scope;
                    if (name.matches(scope)) {
                        System.out.println("overloaded local scope for " + name + " (" + inst + ") in composite type "
                                + this
                                + "  matching \"" + s + "\"");
                        return CST.V_LOCAL;
                    }
                }
            }
            String[] compositeScope = (String[]) getProperty(CST.A_COMPOSITESCOPE);
            if (compositeScope != null) {
                for (String scope : compositeScope) {
                    s = scope;
                    if (name.matches(scope)) {
                        System.out.println("overloaded composite scope for " + name + " (" + inst
                                + ") in composite type " + this
                                + "  matching \"" + s + "\"");
                        return CST.V_COMPOSITE;
                    }
                }
            }
            String[] appliScope = (String[]) getProperty(CST.A_APPLISCOPE);
            if (appliScope != null) {
                for (String scope : appliScope) {
                    s = scope;
                    if (name.matches(scope)) {
                        System.out.println("overloaded appli scope for " + name + " (" + inst + ") in composite type "
                                + this
                                + "  matching \"" + s + "\"");
                        return CST.V_APPLI;
                    }
                }
            }
        } catch (PatternSyntaxException e) {
            System.err.println("invalid scope expression : " + s);
        }
        return null;
    }

    public String getVisibleInCompoType(Implementation impl) {
        String overload = getImplOverload(impl);
        return CompositeTypeImpl.getEffectiveScope((String) impl.getProperty(CST.A_VISIBLE), overload);
    }

    private String getImplOverload(Implementation impl) {
        // the last scope for error message
        String s = "";
        String name = impl.getSpec().getName();
        try {
            String[] localVisible = (String[]) getProperty(CST.A_LOCALVISIBLE);
            if (localVisible != null) {
                for (String visible : localVisible) {
                    s = visible;
                    if (name.matches(visible)) {
                        System.out.println("overloaded local visible for " + name + " (" + impl
                                + ") in composite type " + this
                                + "  matching \"" + s + "\"");
                        return CST.V_LOCAL;
                    }
                }
            }
            String[] compositeVisible = (String[]) getProperty(CST.A_COMPOSITEVISIBLE);
            if (compositeVisible != null) {
                for (String visible : compositeVisible) {
                    s = visible;
                    if (name.matches(visible)) {
                        System.out.println("overloaded composite visible for " + name + " (" + impl
                                + ") in composite type " + this
                                + "  matching \"" + s + "\"");
                        return CST.V_COMPOSITE;
                    }
                }
            }
        } catch (PatternSyntaxException e) {
            System.err.println("invalid visibility expression : " + s);
        }
        return null;
    }

    /**
     * Provided the scope of an object, and the value that can overload it (from the composite)
     * return what has to be the effective scope.
     * The overload can only reduce the object scope.
     * If scope is null it is assumed to be global; if overload is null it is assumed to be missing.
     */
    private static String getEffectiveScope(String scope, String overload) {
        if (scope == null)
            scope = CST.V_GLOBAL;

        if ((overload == null) || overload.equals(CST.V_GLOBAL))
            return scope;

        if (overload.equals(CST.V_LOCAL) || scope.equals(CST.V_LOCAL))
            return CST.V_LOCAL;

        if (overload.equals(CST.V_APPLI))
            return (scope.equals(CST.V_GLOBAL)) ? CST.V_APPLI : scope;

        if (overload.equals(CST.V_COMPOSITE))
            return CST.V_COMPOSITE;

        return CST.V_GLOBAL;
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
        Implementation mainImpl;
        mainImpl = CST.ASMImplBroker.createImpl(null, implName, url, properties);
        if (fromCompo == null) {
            fromCompo = CompositeTypeImpl.rootCompoType;
            if (properties == null)
                properties = new AttributesImpl();
            properties.setProperty(CST.A_VISIBLE, CST.V_LOCAL);
        }
        if (mainImpl instanceof CompositeType) {
            return (CompositeType) mainImpl;
        }
        return new CompositeTypeImpl(fromCompo, name, mainImpl.getName(), mainImpl, models, properties, specName);
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
    public Instance createInst(Composite instCompo, Attributes initialproperties) {
        // Composite comp = CompositeImpl.createComposite(this, instCompo, initialproperties);
        return new CompositeImpl(this, instCompo, null, initialproperties);
    }

    @Override
    public Implementation getMainImpl() {
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
    public void addImpl(Implementation impl) {
        contains.add(impl);
        ((ImplementationImpl) impl).addInComposites(this);
    }

    public void removeImpl(Implementation impl) {
        contains.remove(impl);
        ((ImplementationImpl) impl).removeInComposites(this);
    }

    @Override
    public boolean containsImpl(Implementation impl) {
        return contains.contains(impl);
    }

    @Override
    public Set<Implementation> getImpls() {
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
    public boolean isInternal() {
        String internalImplementations = (String) getProperty(CST.A_INTERNALIMPL);
        if (internalImplementations == null)
            return false;
        return internalImplementations.equals(CST.V_TRUE);
    }

    public boolean getInternalInst() {
        String internalInstances = (String) getProperty(CST.A_INTERNALINST);
        if (internalInstances == null)
            return false;
        return internalInstances.equals(CST.V_TRUE);
    }

    @Override
    public String toString() {
        return name;
    }
}