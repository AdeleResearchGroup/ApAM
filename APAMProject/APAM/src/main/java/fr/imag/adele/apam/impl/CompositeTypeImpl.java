package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

//import fr.imag.adele.sam.Implementation;

public class CompositeTypeImpl extends ImplementationImpl implements CompositeType {

	static Logger logger = LoggerFactory.getLogger(CompositeTypeImpl.class);
    // Global variable. The actual content of the ASM
    private static Map<String, CompositeType> compositeTypes = new ConcurrentHashMap<String, CompositeType>();
    private static CompositeType              rootCompoType  = new CompositeTypeImpl();
    private int                               instNumber     = -1;

    private Implementation                    mainImpl       = null;
    private Set<ManagerModel>                 models;

    // All the implementations deployed (really or logically) by this composite type!
    // Warning : implems may be deployed (logically) by more than one composite Type.
    private final Set<Implementation>         contains       = Collections
    .newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    // The composites types that have been deployed inside the current one.
    private final Set<CompositeType>          embedded       = Collections
    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private final Set<CompositeType>          invEmbedded    = Collections
    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    // all the dependencies between composite types
    private final Set<CompositeType>          imports        = Collections
    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private final Set<CompositeType>          invImports     = Collections
    .newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    private CompositeTypeImpl() {
        name = CST.ROOTCOMPOSITETYPE;
        declaration = new CompositeDeclaration(name,null,null,null,new ArrayList<String>());
        mySpec = new SpecificationImpl("rootSpec", null, new HashSet<ResourceReference>(), null);
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
     * @param mainImplName. the name of either the main implementation or the spec of its main implem.
     * @param models. the models. Can be null.
     * @param attributes. initial properties. Can be null.
     */
    private CompositeTypeImpl(CompositeType fromCompo, String nameCompo, ApformImplementation apfCompo,
            String mainImplName,
            Implementation mainImpl, Set<ManagerModel> models, Map<String, Object> attributes, String specName) {
        // the composite itself as an implementation. Warning created empty. Must be fully initialized.
        super();
        name = nameCompo;
        put(CST.A_COMPOSITETYPE, CST.V_TRUE);

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

        if (mainImpl == null) { // normal case
            mainImpl = CST.apamResolver.findImplByName(this, mainImplName);
            if (mainImpl == null) {
                // It is a specification to resolve as the main implem. Do not select another composite
                Set<Filter> noCompo = new HashSet<Filter>();
                ApamFilter f = ApamFilter.newInstance("(!(" + CST.A_COMPOSITETYPE + "=" + CST.V_TRUE + "))");
                noCompo.add(f);
                mainImpl = CST.apamResolver.resolveSpecByName(this, mainImplName, noCompo, null);
                if (mainImpl == null) {
                    logger.error("cannot find main implementation " + mainImplName);
                    return;
                }
            }
        }
        ((ImplementationImpl) mainImpl).put(CST.A_LOCALIMPLEM, "(name=" + mainImpl + ")");

        // Spec and interface consistency checking
        if (specName != null) {
            Specification spec = CST.SpecBroker.getSpec(specName);
            if (spec == null) {
                logger.error("No specification for composite " + nameCompo);
                new Exception("No specification for composite " + nameCompo).printStackTrace();
            }
            // check if mainImpl really implements the composite type resources;
            Set<ResourceReference> mainImplSpec = mainImpl.getApformImpl().getDeclaration().getProvidedResources();
            // Should never happen, checked at compile time.
            if (!mainImplSpec.containsAll(spec.getDeclaration().getProvidedResources())) {
                logger.error("ERROR: Invalid main implementation " + mainImpl + " for composite type "
                        + name + "Main implementation Provided resources " + mainImplSpec
                        + "do no provide all the expected resources : " + spec.getDeclaration().getProvidedResources());
            } else
                ((SpecificationImpl) mainImpl.getSpec()).setName(specName);
        }

        this.models = models;
        mySpec = mainImpl.getSpec();
        ((SpecificationImpl) mySpec).addImpl(this);

        if (apfCompo != null) {
            apfImpl = apfCompo;
        } else {
            apfImpl = new ApformRootCompositeType(this, mainImpl, attributes);
        }

        declaration = apfImpl.getDeclaration(); 

        this.mainImpl = mainImpl;
        ((ImplementationImpl) mainImpl).initializeNewImpl(this, null); // complete attribute value init, and chainings.

        if (attributes != null)
            putAll(attributes);

        CompositeTypeImpl.compositeTypes.put(name, this);
        ((ImplementationBrokerImpl) CST.ImplBroker).addImpl(this);

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
     * @param mainImplem : The main implementation or the specification name. If not found, returns null.
     * @param models optional : the associated models.
     */

    public static CompositeType createCompositeType(CompositeType fromCompo, String name, String mainImplName,
            String specName, Set<ManagerModel> models, Map<String, Object> attributes) {

        assert (mainImplName != null) ;
        if (CompositeTypeImpl.compositeTypes.get(name) != null) {
            logger.error("Composite type " + name + " allready existing");
            return null;
        }
        if (fromCompo == null) {
            fromCompo = CompositeTypeImpl.rootCompoType;
            if (attributes == null)
                attributes = new ConcurrentHashMap<String, Object>();
        }
        return new CompositeTypeImpl(fromCompo, name, (ApformImplementation)null, mainImplName, (Implementation)null, models,
                attributes, specName);
    }

    /**
     * Create a composite type, from its object in OSGi bundle : apfImpl.
     * Look for its main implementation, and deploys it if needed.
     * 
     * @param implComposite : the composite type that will contain the new one.
     * @param apfImpl : the object in iPOJO representing the composite type.
     * @return
     */
    public static CompositeType createCompositeType(CompositeType implComposite, ApformImplementation apfImpl) {
        assert (apfImpl != null);

        if (! (apfImpl.getDeclaration() instanceof CompositeDeclaration)) {
            new Exception("ERROR : the apform object is not a composite "+apfImpl).printStackTrace();
            return null;
        }

        CompositeDeclaration declaration = (CompositeDeclaration) apfImpl.getDeclaration();

        String name						= declaration.getName();
        String mainComponentName = declaration.getMainComponent().getName();
        String specName = null; // may be null. Implementation constructor will create one dummy
        if (declaration.getSpecification() != null)
            specName = declaration.getSpecification().getName();
        Map<String, Object> properties	= declaration.getProperties();

        @SuppressWarnings("unchecked")
        Set<ManagerModel> models 		= (Set<ManagerModel>) declaration.getProperty(CST.A_MODELS);

        if (implComposite == null) {
            implComposite = CompositeTypeImpl.rootCompoType;
        }

        if (properties == null) {
            properties = new ConcurrentHashMap<String, Object>();
        }

        if (mainComponentName == null) {
            new Exception("ERROR : main implementation Name missing").printStackTrace();
            return null;
        }
        if (CompositeTypeImpl.compositeTypes.get(name) != null) {
            logger.error("Composite type " + name + " allready existing");
            return null;
        }

        return new CompositeTypeImpl(implComposite, apfImpl.getDeclaration().getName(), apfImpl, mainComponentName,
                (Implementation) null, models, properties, specName);
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
            String implName, URL url, String specName, Map<String, Object> properties) {
        assert (specName != null);

        Implementation mainImpl;
        if (fromCompo == null) {
            fromCompo = CompositeTypeImpl.rootCompoType;
            if (properties == null)
                properties = new ConcurrentHashMap<String, Object>();
            //            properties.put(CST.A_VISIBLE, CST.V_LOCAL);
        }
        mainImpl = CST.ImplBroker.createImpl(null, implName, url, properties);

        if (mainImpl instanceof CompositeType) {
            return (CompositeType) mainImpl;
        }
        return new CompositeTypeImpl(fromCompo, name, /* apf */null, mainImpl.getName(), mainImpl, models, properties,
                specName);
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
    //    @Override
    @Override
    protected Instance createInst(Composite instCompo, Map<String, Object> initialproperties) {
        return CompositeImpl.newCompositeImpl(this, instCompo, null, initialproperties, apfImpl
                .createInstance(initialproperties));
    }

    /**
     * Public. Must check if instCompos can see this composite type.
     * 
     * @param instCompo
     * @param initialproperties
     * @return
     */
    @Override
    public Instance createInstance(Composite instCompo, Map<String, Object> initialproperties) {
        if ((instCompo != null) && !Util.checkImplVisible(instCompo.getCompType(), this)) {
            logger.error("cannot instantiate " + this + ". It is not visible from composite " + instCompo);
            return null;
        }
        return CompositeImpl.newCompositeImpl(this, instCompo, null, initialproperties, apfImpl
                .createInstance(initialproperties));
    }

    @Override
    public Implementation getMainImpl() {
        return mainImpl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompositeDeclaration getCompoDeclaration() {
        return (CompositeDeclaration) declaration;
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

    private boolean removeImport(CompositeType destination) {
        ((CompositeTypeImpl) destination).removeInvImport(this);
        return imports.remove(destination);
    }

    /**
     * A new implementation is added in the composite type.
     * It has to be notified to the dynamic managers (an implementaiton appeared).
     * 
     * @param destination
     */
    public void addEmbedded(CompositeType destination) {
        embedded.add(destination);
        if (this != CompositeTypeImpl.rootCompoType)
            ((CompositeTypeImpl) destination).addInvEmbedded(this);
        ApamManagers.notifyAddedInApam(this);
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
    public boolean isFriend(CompositeType destination) {
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

    //    @Override
    //    public boolean isInternal() {
    //        String internalImplementations = (String) get(CST.A_INTERNALIMPL);
    //        if (internalImplementations == null)
    //            return false;
    //        return internalImplementations.equals(CST.V_TRUE);
    //    }
    //
    //    public boolean getInternalInst() {
    //        String internalInstances = (String) get(CST.A_INTERNALINST);
    //        if (internalInstances == null)
    //            return false;
    //        return internalInstances.equals(CST.V_TRUE);
    //    }

    @Override
    public String toString() {
        return name;
    }


    /**
     * The apform implementation of a composite type used to bootstrap isolated
     * main instances.
     * 
     * Bootstrapping requires creating an APAM composite type before its corresponding
     * Apform declaration, so this case is carefully handled in the constructor of the
     * composite type.
     * 
     * In the normal case, the Apform composite type is first created and then APAM
     * get all the declaration information from it. In the bootstrap case, the order is
     * reversed, so care must be taken to avoid circular references.
     * 
     * @author vega
     *
     */
    private static class ApformRootCompositeType implements ApformImplementation {

        private final CompositeTypeImpl apamComposite;
        private final CompositeDeclaration declaration;
        private final ApformSpecification specification;

        public ApformRootCompositeType(CompositeTypeImpl apamComposite, Implementation mainImplem,
                Map<String, Object> attributes) {

            this.apamComposite	= apamComposite;

            /*
             * NOTE this constructor is invoked when the APAM composite type is partially
             * constructed. Not all methods can be invoked, this is why the main implementation
             * and attributes are additionally passed as arguments. 
             */
            specification 		= mainImplem.getSpec().getApformSpec();

            declaration = new CompositeDeclaration(apamComposite.getName(),
                    specification.getDeclaration().getReference(),
                    mainImplem.getApformImpl().getDeclaration().getReference(),
                    null, new ArrayList<String>());

            // c'est probablement pas la bonne solution. a voir GERMAN. properties sur composite ??
            // TODO
            //            if (declaration.getProperties() != null) {
            //                declaration.getProperties().putAll(attributes);
            //            }
            declaration.getProvidedResources().addAll(specification.getDeclaration().getProvidedResources());


        }

        @Override
        public ImplementationDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public ApformInstance createInstance(Map<String, Object> initialproperties) {
            return new ApformRootComposite(apamComposite);
        }

        @Override
        public ApformSpecification getSpecification() {
            return specification;
        }
    }

    /**
     * An apform composite instance to represent root composites that don't have
     * an explicit declaration (automatically created)
     * 
     * @author vega
     *
     */
    private static class ApformRootComposite implements ApformInstance {

        private final InstanceDeclaration declaration;

        public ApformRootComposite(CompositeTypeImpl compositeType) {

            String name = compositeType.getNewInstName();
            declaration = new InstanceDeclaration(compositeType.getApformImpl().getDeclaration().getReference(),
                    name, null);
        }

        @Override
        public InstanceDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public Object getServiceObject() {
            throw new UnsupportedOperationException("this method is not available for root composites");
        }

        @Override
        public boolean setWire(Instance destInst, String depName) {
            throw new UnsupportedOperationException("this method is not available for root composites");
        }

        @Override
        public boolean remWire(Instance destInst, String depName) {
            throw new UnsupportedOperationException("this method is not available for root composites");
        }

        @Override
        public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
            throw new UnsupportedOperationException("this method is not available for root composites");
        }

        @Override
        public void setInst(Instance ignored) {
        }
    }

}
