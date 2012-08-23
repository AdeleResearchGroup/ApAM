package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;

public class APAMImpl implements Apam {

    private static Logger logger = LoggerFactory.getLogger(APAMImpl.class);

	/*
	 * The bundle context used for deployment in the execution paltform
	 */
    public static BundleContext context;
    
    /*
     * A reference to the ApamMan manager. 
     * 
     * This is the only manager required to start the platform.
     */
    private DependencyManager	apamMan;

    public APAMImpl(BundleContext context) {
        APAMImpl.context = context;
        new CST(this);
//        APAMImpl.apamMan = new ApamMan();
        ApamManagers.addDependencyManager(apamMan, -1); // -1 to be sure it is not in the main loop
    }

    @Override
    public Composite startAppli(CompositeType composite) {
        return (Composite) composite.createInstance(null, null);
    }
    
    @Override
    public Composite startAppli(String compositeName) {
    	
        Implementation compoType = CST.apamResolver.findImplByName(null,compositeName);
        if (compoType == null) {
            logger.error("ERROR : " + compositeName + " is not a deployed composite.");
            return null;
        }
        
        if (!(compoType instanceof CompositeType)) {
            logger.error("ERROR : " + compoType.getName() + " is not a composite.");
            return null;
        }

        return startAppli((CompositeType) compoType);
    }

    @Override
    public Composite startAppli(URL compoURL, String compositeName) {
    	
    	Implementation compoType = CST.ImplBroker.createImpl(null,compositeName,compoURL,null);
    	
        if (compoType == null) {
            logger.error("ERROR : " + compositeName + " can not be deployed.");
            return null;
        }
        
        if (!(compoType instanceof CompositeType)) {
            logger.error("ERROR : " + compoType.getName() + " is not a composite.");
            return null;
        }

        return startAppli((CompositeType) compoType);
    }

    @Override
    public CompositeType createCompositeType(String inCompoType, String name, String mainComponent,
            Set<ManagerModel> models, Map<String, Object> attributes) {

    	/*
    	 * Verify if it already exists
    	 */
    	CompositeType compositeType = getCompositeType(name);
        if (compositeType != null) {
            logger.error("Composite type " + name + " already existing");
            return null;
        }
    	
     	/*
    	 * Get the specified enclosing composite type
    	 */
    	Implementation parent = null;
        if (inCompoType != null) {
        	parent = CST.apamResolver.findImplByName(null, inCompoType);
            if (parent == null || !(parent instanceof CompositeType)) {
            	logger.error(inCompoType + " is not a deployed composite type.");
                return null;
            }
        }
        
        String specification = null;
        return createCompositeType((CompositeType)parent,name,specification,mainComponent,models,attributes);
    }
    
    @Override
    public CompositeType createCompositeType(String inCompoType, String name, String mainImplName,
            Set<ManagerModel> models, URL mainBundle, String specName, Map<String, Object> attributes) {
    	
    	/*
    	 * Verify if it already exists
    	 */
    	CompositeType compositeType = getCompositeType(name);
        if (compositeType != null) {
            logger.error("Composite type " + name + " already existing");
            return null;
        }
    	
     	/*
    	 * Get the specified enclosing composite type
    	 */
    	Implementation parent = null;
        if (inCompoType != null) {
        	parent = CST.apamResolver.findImplByName(null, inCompoType);
            if (parent == null || !(parent instanceof CompositeType)) {
            	logger.error(inCompoType + " is not a deployed composite type.");
                return null;
            }
        }

        /*
         * Deploy the bundle with the main implementation.
         * 
         * TODO this method blocks until the specified implementation is deployed, this requires knowing
         * the name of the expected implementation. This is in contradiction with the description of the
         * method signature in the API that allows either the main implementation or composite name. It
         * doesn't work neither if the main component is an specification. We should review the API to
         * avoid confusion, perhaps provide two different methods for each case.
         * 
         * For now just assume we deploy the main implementation and get the composite information from
         * the parameters. Notice also that to enforce resolution from the  specified URL we deploy the
         * main implementation in the context of the parent, not in the context of the newly created
         * composite type. The main implementation will be later deployed logically in the created 
         * composite type, if allowed by visibility rules.
         */
        CST.ImplBroker.createImpl((CompositeType)parent,mainImplName,mainBundle,attributes);
        return createCompositeType((CompositeType)parent,name,specName,mainImplName,models,attributes);
    }
    
    /**
     * Creates a composite type from the specified parameters 
     */
    public CompositeType createCompositeType(CompositeType parent,
    		String name, String specification, String mainComponent,
            Set<ManagerModel> models, Map<String, Object> initialProperties) {

    	assert name != null && mainComponent != null;
    	
    	if (models == null)
    		models = new HashSet<ManagerModel>();
    	
    	if (parent == null)
    		parent = CompositeTypeImpl.getRootCompositeType();
    	
    	ApformImplementation apfCompo = new ApamOnlyCompositeType(name,
    											specification,mainComponent,
    											models,initialProperties);
    	
    	CompositeTypeImpl composite = new CompositeTypeImpl(parent,apfCompo,initialProperties);
    	composite.register();
    	
    	return composite;
    }
    
 
    @Override
    public CompositeType getCompositeType(String name) {
        return CompositeTypeImpl.getCompositeType(name);
    }

    @Override
    public Collection<CompositeType> getCompositeTypes() {
        return CompositeTypeImpl.getCompositeTypes();
    }

    @Override
    public Collection<CompositeType> getRootCompositeTypes() {
        return CompositeTypeImpl.getRootCompositeTypes();
    }

    @Override
    public Composite getComposite(String name) {
        return CompositeImpl.getComposite(name);
    }

    @Override
    public Collection<Composite> getComposites() {
        return CompositeImpl.getComposites();
    }

    @Override
    public Collection<Composite> getRootComposites() {
        return CompositeImpl.getRootComposites();
    }

	public DependencyManager getApamMan() {
		return apamMan;
	}

    /**
     * An special apform implementation created only for those composites types that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlyCompositeType implements ApformImplementation {

    	/**
    	 * The declaration with all the information regarding this composite type
    	 */
       	private final CompositeDeclaration declaration;

    	/**
    	 * The number of instances created for this composite type
    	 */
    	private int  numInstances;
 
    	public ApamOnlyCompositeType(String name, String specificationName, String mainName, Set<ManagerModel> models, Map<String,Object> properties) {
    		assert name != null && mainName != null && models != null;
    		
    		SpecificationReference specification = specificationName != null? new SpecificationReference(specificationName) : null;
    		ComponentReference<?>  mainComponent = new ComponentReference<ComponentDeclaration>(mainName);
    		
    		declaration = new CompositeDeclaration(name,specification, mainComponent,null,new ArrayList<String>());
    		if (properties != null)
    			declaration.getProperties().putAll(properties);
    		declaration.getProperties().put(CST.A_MODELS,models);
    		
    		numInstances = 0;
    	}
    	
		@Override
		public ImplementationDeclaration getDeclaration() {
			return declaration;
		}

		@Override
		public ApformInstance createInstance(Map<String, Object> initialProperties) {
			numInstances ++;
			String name = declaration.getName()+"-"+numInstances;
			return new ApamOnlyComposite(declaration.getReference(),name,initialProperties);
		}

		@Override
		public void setProperty(String attr, Object value) {
		}
		
		@Override
		public ApformSpecification getSpecification() {
			return null;
		}

    }
 
    /**
     * An special apform instance created only for those composites that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlyComposite implements ApformInstance {

    	private final InstanceDeclaration declaration;
    	
    	public ApamOnlyComposite(ImplementationReference<?>implementation,String name,Map<String, Object> initialProperties) {
    		declaration = new InstanceDeclaration(implementation,name,null);
    		if (initialProperties != null)
    			declaration.getProperties().putAll(initialProperties);
    	} 

		@Override
		public InstanceDeclaration getDeclaration() {
			return declaration;
		}

		@Override
		public void setInst(Instance asmInstImpl) {
		}
		
		@Override
		public void setProperty(String attr, Object value) {
		}


		@Override
		public Object getServiceObject() {
			throw new UnsupportedOperationException("method not available in application composite instance");
		}

		@Override
		public boolean setWire(Instance destInst, String depName) {
			throw new UnsupportedOperationException("method not available in application composite instance");
		}

		@Override
		public boolean remWire(Instance destInst, String depName) {
			throw new UnsupportedOperationException("method not available in application composite instance");
		}

		@Override
		public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
			throw new UnsupportedOperationException("method not available in application composite instance");
		}

    } 
    

}
