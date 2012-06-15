package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.ResourceReference;

//import fr.imag.adele.sam.Implementation;

public class CompositeTypeImpl extends ImplementationImpl implements CompositeType {

    // Global variable. The actual content of the ASM
    private static Map<String, CompositeType> compositeTypes = new HashMap<String, CompositeType>();
    private static CompositeType              rootCompoType  = new CompositeTypeImpl();
    private int                               instNumber     = -1;

    private Implementation                    mainImpl       = null;
    private Set<ManagerModel>                 models;

    // All the implementations deployed (really or logically) by this composite type!
    // Warning : implems may be deployed (logically) by more than one composite Type.
    private final Set<Implementation>         contains       = new HashSet<Implementation>();

    // The composites types that have been deployed inside the current one.
    private final Set<CompositeType>          embedded       = new HashSet<CompositeType>();
    private final Set<CompositeType>          invEmbedded    = new HashSet<CompositeType>();

    // all the dependencies between composite types
    private final Set<CompositeType>          imports        = new HashSet<CompositeType>();
    private final Set<CompositeType>          invImports     = new HashSet<CompositeType>();

    private CompositeTypeImpl() {
        name = CST.ROOTCOMPOSITETYPE;
        declaration = new CompositeDeclaration(name,null,null,null,new ArrayList<String>());
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
            mainImpl = ApamResolver.findImplByName(this, mainImplName);
            if (mainImpl == null) {
                mainImpl = ApamResolver.resolveSpecByName(this, mainImplName, null, null);
                if (mainImpl == null) {
                    System.err.println("cannot find main implementation " + mainImplName);
                    return;
                }
            }
        }
        mainImpl.put(CST.A_LOCALIMPLEM, "(name=" + mainImpl + ")");

        // Spec and interface consistency checking
        if (specName != null) {
            Specification spec = CST.SpecBroker.getSpec(specName);
            if (spec == null) {
                System.err.println("No specification for composite " + nameCompo);
                new Exception("No specification for composite " + nameCompo).printStackTrace();
            }
            // check if mainImpl really implements the composite type resources;
            Set<ResourceReference> mainImplSpec = mainImpl.getApformImpl().getDeclaration().getProvidedResources();
            // Should never happen, checked at compile time.
            if (!mainImplSpec.containsAll(spec.getDeclaration().getProvidedResources())) {
                System.err.println("ERROR: Invalid main implementation " + mainImpl + " for composite type "
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
            apfImpl = new ApformCompositeImpl(name, mainImpl, attributes);
        }

        declaration = apfImpl.getDeclaration(); 

        this.mainImpl = mainImpl;
        ((ImplementationImpl) mainImpl).initializeNewImpl(this, null); // complete attribute value init, and chainings.

        if (attributes != null)
            putAll(attributes);
        // put(CST.A_COMPOSITETYPE, fromCompo.getName());

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
     * @param mainImplem : The name of the main implementation. If not found, returns null.
     * @param models optional : the associated models.
     */

    public static CompositeType createCompositeType(CompositeType fromCompo, String name, String mainImplName,
            String specName, Set<ManagerModel> models, Map<String, Object> attributes) {

        assert (mainImplName != null) ;
        if (CompositeTypeImpl.compositeTypes.get(name) != null) {
            System.err.println("Composite type " + name + " allready existing");
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
        String specName 				= declaration.getSpecification().getName();
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
            System.err.println("Composite type " + name + " allready existing");
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
    @Override
    public Instance createInst(Composite instCompo, Map<String, Object> initialproperties) {
        // Composite comp = CompositeImpl.createComposite(this, instCompo, initialproperties);
        assert (instCompo != null);
        // TODO check if apfImpl.createInstance(initialproperties)); is correct
        return new CompositeImpl(this, instCompo, null, initialproperties, apfImpl.createInstance(initialproperties));
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

    private class ApformComposite implements ApformInstance {

        private InstanceDeclaration declaration;
        private Instance            instance;

        public ApformComposite() {
        }

        @Override
        public InstanceDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public Object getServiceObject() {
            // TODO Auto-generated method stub
            assert (false);
            return null;
        }

        @Override
        public boolean setWire(Instance destInst, String depName) {
            // TODO Auto-generated method stub
            assert (false);
            return false;
        }

        @Override
        public boolean remWire(Instance destInst, String depName) {
            // TODO Auto-generated method stub
            assert (false);
            return false;
        }

        @Override
        public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
            // TODO Auto-generated method stub
            assert (false);
            return false;
        }

        @Override
        public void setInst(Instance asmInstImpl) {
            instance = asmInstImpl;

            String name = ((CompositeTypeImpl) instance.getImpl()).getNewInstName();
            declaration = new InstanceDeclaration(instance.getImpl().getApformImpl().getDeclaration().getReference(),
                    name, null);
        }
    }

    private class ApformCompositeImpl implements ApformImplementation {

        private final CompositeDeclaration declaration;
        private final ApformSpecification specification;

        public ApformCompositeImpl(String name, Implementation mainImplem,
                Map<String, Object> attributes) {

            specification = mainImplem.getSpec().getApformSpec();

            declaration = new CompositeDeclaration(name,
                    specification.getDeclaration().getReference(),
                    mainImplem.getApformImpl().getDeclaration().getReference(),
                    null, new ArrayList<String>());
            declaration.getProperties().putAll(attributes);
            declaration.getProvidedResources().addAll(specification.getDeclaration().getProvidedResources());


        }

        @Override
        public ImplementationDeclaration getDeclaration() {
            return declaration;
        }

        @Override
        public ApformInstance createInstance(Map<String, Object> initialproperties) {
            return new ApformComposite();
        }

        @Override
        public ApformSpecification getSpecification() {
            return specification;
        }
    }

    @Override
    public CompositeDeclaration getCompoDeclaration() {

        return (CompositeDeclaration) declaration;
    }

}
