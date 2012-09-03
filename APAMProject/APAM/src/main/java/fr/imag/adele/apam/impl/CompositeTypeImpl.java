package fr.imag.adele.apam.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.util.ApamFilter;

//import fr.imag.adele.sam.Implementation;

public class CompositeTypeImpl extends ImplementationImpl implements CompositeType {

	private static Logger 		logger 				= LoggerFactory.getLogger(CompositeTypeImpl.class);
	private static final long 	serialVersionUID 	= 1L;
	
	/*
	 * Global variables to keep the hierarchy of composite types
	 * 
	 * TODO should we refactor and move these static variables to a CompositeBroker or to Apam implementation? 
	 */
    private static CompositeType              rootCompoType  = new CompositeTypeImpl();
    private static Map<String, CompositeType> compositeTypes = new ConcurrentHashMap<String, CompositeType>();
    
    public static CompositeType getRootCompositeType() {
        return CompositeTypeImpl.rootCompoType;
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
    
    /*
     * The models associated to this composite type to specify the different strategies to handle the
     * instances of this type.
     */
    private Set<ManagerModel>	models		= new HashSet<ManagerModel>();

    /*
     * The contained implementations deployed (really or logically) by this composite type.
     * 
     * WARNING An implementation may be deployed by more than one composite type
     */
    private Set<Implementation>	contains	= Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());
    private Implementation		mainImpl	= null;

    /*
     *  The hierarchy of composite types. 
     *  
     *  This is a subset of the contains hierarchy restricted only to composites. 
     */
    private Set<CompositeType>	embedded	= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private Set<CompositeType>	invEmbedded	= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    /*
     *  all the dependencies between composite types
     */
    private Set<CompositeType>	imports		= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());
    private Set<CompositeType>	invImports	= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());



    /**
     * This is an special constructor only used for the root type of the system 
     */
    private CompositeTypeImpl() {
    	super("rootCompositeType");
        
        /*
         * Look for platform models in directory "load" 
         */
        this.models = new HashSet<ManagerModel>();
        File modelDirectory = new File("load");
        
        if (! modelDirectory.exists())
        	return;
        
        if (! modelDirectory.isDirectory())
        	return;
        
        for (File modelFile : modelDirectory.listFiles()) {
 			try {
 				String modelFileName = modelFile.getName();
 				
 				if (! modelFileName.endsWith(".cfg"))
 					continue;
 				
 				if (! modelFileName.startsWith("root"))
 					continue;
 				
	            String managerName = modelFileName.substring("root".length()+1, modelFileName.lastIndexOf(".cfg"));
				URL modelURL = modelFile.toURI().toURL();
	            models.add(new ManagerModel(managerName, modelURL));
	            
			} catch (MalformedURLException e) {
			}			
		}
    }

    /**
     * Whether this is the system root composite type
     */
    public boolean isSystemRoot() {
    	return this == rootCompoType;
    }
    
    /**
     * Builds a new Apam composite type to represent the specified implementation in the Apam model.
     */
    protected CompositeTypeImpl(CompositeType composite, ApformImplementation apfCompo, Map<String, Object> initialProperties) {
        
    	super(composite,apfCompo,initialProperties);
  
		/*
		 * Reference the enclosing composite hierarchy
		 */
    	addInvEmbedded(composite);
     	
    	/*
    	 * Get declared models
    	 */
       	
    	assert apfCompo.getDeclaration() instanceof CompositeDeclaration;
       	
       	CompositeDeclaration declaration = (CompositeDeclaration)apfCompo.getDeclaration();
    	if (declaration.getProperty(CST.A_MODELS) != null) {
    		@SuppressWarnings("unchecked")
			Set<ManagerModel> declaredModels = (Set<ManagerModel>)declaration.getProperty(CST.A_MODELS);
    		models.addAll(declaredModels);
    	}
    }

    @Override
    public void register() {
    	
    	/*
    	 * Opposite references from the enclosing composite types
    	 */
		for (CompositeType inComposite : invEmbedded) {
	        ((CompositeTypeImpl)inComposite).addEmbedded(this);
		}

    	/*
    	 * Notify managers of their models
    	 * Not at the end of registration because OBR needs its model to find the main implem.
    	 * WARNING Notice that at this stage the composite type is not completely registered in the Apam model
    	 * so managers must be cautious when manipulating the state and navigating the hierarchy.
    	 */
        for (ManagerModel managerModel : models) {
        	DependencyManager manager = ApamManagers.getManager(managerModel.getManagerName());
            if (manager != null) {
            	manager.newComposite(managerModel, this);
            }
        }
		
        /*
         * Resolve main implementation.
         * 
         * First we try to find an implementation with the name of the main component, if we fail to find one we
         * assume the name corresponds to a specification which is resolved. 
         * Notice that resolution of the main component is done in the context of this composite type, 
         * so it will be  deployed in this context if necessary.
         * 
         * WARNING this is done after the composite type is added to the hierarchy but before it is completely
         * registered as a normal implementation. We do not call super.register until the main implem is resolved.
         * 
         */
		String mainComponent = getCompoDeclaration().getMainComponent().getName();
		Set<Filter> constraints = new HashSet<Filter>();
		
		mainImpl = CST.apamResolver.findImplByName(this,mainComponent);
		if (mainImpl == null) {
			/*
			 *  It is a specification to resolve as the main implem. Do not select another composite
			 */
			constraints.clear();
			ApamFilter noComposite = ApamFilter.newInstance("(!(" + CST.A_COMPOSITETYPE + "=" + CST.V_TRUE + "))");
			constraints.add(noComposite);
			mainImpl = CST.apamResolver.resolveSpecByName(this, mainComponent, constraints, null);
        }
        if (mainImpl == null) {
            logger.error("cannot find main implementation " + mainComponent);
            return;
        }
        
        if (! mainImpl.getInCompositeType().contains(this)) deploy(mainImpl) ;
		
        /*
         * Check that the main implementation actually provides all the resources of the composite
         * 
         */
        //if (getSpec() != null ) {
            if (! mainImpl.getDeclaration().getProvidedResources().containsAll(getSpec().getDeclaration().getProvidedResources())) {
                logger.error("ERROR: Invalid main implementation " + mainImpl + " for composite type "
                        + getName() + "Main implementation Provided resources " + mainImpl.getDeclaration().getProvidedResources()
                        + "do no provide all the expected resources : " + getSpec().getDeclaration().getProvidedResources());
            }
        //} 

        /*
		 * add to list of composite types
		 */
		CompositeTypeImpl.compositeTypes.put(getName(),this);
		
		/*
		 * Complete normal registration
		 */
    	super.register(); 	
    }
    
    @Override
    public void unregister() {
		/*
		 * Notify managers and remove the implementation from the broker
		 */ 
    	super.unregister();

    	/*
    	 * Remove import relationships. 
    	 * 
    	 * NOTE We have to copy the list because we update it while iterating it
    	 * 
    	 */
		for (CompositeType imported : new HashSet<CompositeType>(imports)) {
	        removeImport(imported);
		}

		for (CompositeType importedBy : new HashSet<CompositeType>(invImports)) {
	        ((CompositeTypeImpl)importedBy).removeImport(this);
		}

    	/*
    	 * Remove opposite references from embedding composite types
    	 * 
    	 * TODO May be this should be done at the same type that the contains
    	 * hierarchy, but this will require a refactor of the superclass to 
    	 * have a fine control on the order of the steps.
    	 */
		for (CompositeType inComposite : invEmbedded) {
	        ((CompositeTypeImpl)inComposite).removeEmbedded(this);
		}
		
    	invEmbedded.clear();
    }

    /**
     * Deploy (logically) a new implementation to this composite type.
     * 
     * TODO Should this method be in the public API or it is restricted to the
     * resolver and other managers?
     */
    public void deploy(Implementation impl) {
    	
    	/*
    	 * Remove implementation from the unused container if this is the first deployment
    	 */
    	if ( ! impl.isUsed()) {
    		((ImplementationImpl)impl).removeInComposites(CompositeTypeImpl.getRootCompositeType());
    		((CompositeTypeImpl)CompositeTypeImpl.getRootCompositeType()).removeImpl(impl);
    		
    		/*
    		 * If the implementation is composite, it is also embedded in the unused container
    		 */
    		if (impl instanceof CompositeType) {
        		((CompositeTypeImpl)impl).removeInvEmbedded(CompositeTypeImpl.getRootCompositeType());
        		((CompositeTypeImpl)CompositeTypeImpl.getRootCompositeType()).removeEmbedded((CompositeTypeImpl)impl);
    		}
    	}
    	
    	/*
    	 * Add the implementation to this composite 
    	 */
    	((ImplementationImpl)impl).addInComposites(this);
    	this.addImpl(impl);
    	
		/*
		 * Embed in this hierarchy if the implementation is composite
		 */
    	if (impl instanceof CompositeType) {
        	((CompositeTypeImpl)impl).addInvEmbedded(this);
        	this.addEmbedded((CompositeTypeImpl)impl);
    	}
    }
    
    protected Composite reify(Composite composite, ApformInstance platformInstance, Map<String,Object> initialproperties) {
    	return new CompositeImpl(composite,platformInstance,initialproperties);
    }

    @Override
    public Implementation getMainImpl() {
        return mainImpl;
    }

    @Override
    public CompositeDeclaration getCompoDeclaration() {
        return (CompositeDeclaration) getDeclaration();
    }

    @Override
    public Set<ManagerModel> getModels() {
        return Collections.unmodifiableSet(models);
    }

    @Override
    public ManagerModel getModel(String managerName) {
        for (ManagerModel model : models) {
            if (model.getManagerName().equals(managerName))
                return model;
        }
        return null;
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
    public void addImport(CompositeType destination) {
        imports.add(destination);
        ((CompositeTypeImpl) destination).addInvImport(this);
    }

    public boolean removeImport(CompositeType destination) {
        ((CompositeTypeImpl) destination).removeInvImport(this);
        return imports.remove(destination);
    }
    

    public void addInvImport(CompositeType dependent) {
        invImports.add(dependent);
    }

    public boolean removeInvImport(CompositeType dependent) {
        return invImports.remove(dependent);
    }
    
    @Override
    public Set<CompositeType> getEmbedded() {
        return Collections.unmodifiableSet(embedded);
    }

    public void addEmbedded(CompositeType destination) {
        embedded.add(destination);
    }

    public boolean removeEmbedded(CompositeType destination) {
        return embedded.remove(destination);
    }

    public void addInvEmbedded(CompositeType origin) {
        invEmbedded.add(origin);
    }

    public boolean removeInvEmbedded(CompositeType origin) {
        return invEmbedded.remove(origin);
    }

    @Override
    public boolean containsImpl(Implementation impl) {
        return contains.contains(impl);
    }

    @Override
    public Set<Implementation> getImpls() {
        return Collections.unmodifiableSet(contains);
    }

    public void addImpl(Implementation impl) {
        contains.add(impl);
    }

    public void removeImpl(Implementation impl) {
        contains.remove(impl);
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
        return getName();
    }


}
