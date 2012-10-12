package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
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
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
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
     * This are the managers required to start the platform.
     */
    private DependencyManager	apamMan;
    private UpdateMan	updateMan;

    public APAMImpl(BundleContext context) {
        APAMImpl.context = context;
        new CST(this);
        apamMan = new ApamMan();
        updateMan = new UpdateMan();
        ApamManagers.addDependencyManager(apamMan, -1); // -1 to be sure it is not in the main loop
        ApamManagers.addDependencyManager(updateMan, -2); // -2 to be sure it is not in the main loop
        ApamManagers.addDynamicManager(updateMan); 
    }

    @Override
    public Composite startAppli(CompositeType composite) {
        return (Composite) composite.createInstance(null, null);
    }
    
    @Override
    public Composite startAppli(String compositeName) {
    	
        Implementation compoType = CST.apamResolver.findImplByName(null,compositeName);
        if (compoType == null) {
            logger.error("Error starting application: " + compositeName + " is not a deployed composite.");
            return null;
        }
        
        if (!(compoType instanceof CompositeType)) {
            logger.error("Error starting application: " + compoType.getName() + " is not a composite.");
            return null;
        }

        return startAppli((CompositeType) compoType);
    }

    @Override
    public Composite startAppli(URL compoURL, String compositeName) {
    	
    	Implementation compoType = CST.componentBroker.createImpl(null,compositeName,compoURL,null);
    	
        if (compoType == null) {
            logger.error("Error starting application: " + compositeName + " can not be deployed.");
            return null;
        }
        
        if (!(compoType instanceof CompositeType)) {
            logger.error("Error starting application: " + compoType.getName() + " is not a composite.");
            return null;
        }

        return startAppli((CompositeType) compoType);
    }

    @Override
    public CompositeType createCompositeType(String inCompoType,
    		String name, String specification, String mainComponent, 
            Set<ManagerModel> models, Map<String, String> attributes) {

    	/*
    	 * Verify if it already exists
    	 */
    	CompositeType compositeType = getCompositeType(name);
        if (compositeType != null) {
            logger.error("Error creating composite type: already exists " + name );
            return compositeType;
        }
    	
     	/*
    	 * Get the specified enclosing composite type
    	 */
    	Implementation parent = null;
        if (inCompoType != null) {
        	parent = CST.apamResolver.findImplByName(null, inCompoType);
            if (parent == null || !(parent instanceof CompositeType)) {
            	logger.error("Error creating composite type: specified enclosing composite "+ inCompoType + " is not a deployed composite type.");
                return null;
            }
        }
        
        return createCompositeType((CompositeType)parent, name, specification, mainComponent, models, attributes);
    }
    
    /**
     * Creates a composite type from the specified parameters 
     */
    public CompositeType createCompositeType(CompositeType parent,
    		String name, String specification, String mainComponent,
            Set<ManagerModel> models, Map<String, String> properties) {

    	assert name != null && mainComponent != null;
    	
    	if (models == null)
    		models = new HashSet<ManagerModel>();
    	
    	if (parent == null)
    		parent = CompositeTypeImpl.getRootCompositeType();
    	
    	ApformImplementation apfCompo = new ApamOnlyCompositeType(name,
    											specification, mainComponent,
    											models, properties);
    	
    	/* 
    	 * If the provided specification is not installed force a resolution
    	 */
    	if (specification != null && CST.componentBroker.getSpec(specification) == null) {
    		CST.apamResolver.findSpecByName(parent,specification);
    	}
    	
    	return (CompositeType) CST.componentBroker.addImpl(parent,apfCompo);
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
	public DependencyManager getUpdateMan() {
		return updateMan;
	}

    /**
     * An special apform implementation created only for those composites types that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlyCompositeType implements ApformCompositeType {

    	/**
    	 * The declaration with all the information regarding this composite type
    	 */
       	private final CompositeDeclaration declaration;

       	/**
       	 * The associated models
       	 */
       	private final Set<ManagerModel> models = new HashSet<ManagerModel>();
    	/**
    	 * The number of instances created for this composite type
    	 */
    	private int  numInstances;
 
    	public ApamOnlyCompositeType(String name, String specificationName, String mainName, Set<ManagerModel> models, Map<String,String> properties) {
    		assert name != null && mainName != null && models != null;
    		
    		SpecificationReference specification = specificationName != null? new SpecificationReference(specificationName) : null;
    		ComponentReference<?>  mainComponent = new ComponentReference<ComponentDeclaration>(mainName);
    		
    		declaration = new CompositeDeclaration(name,specification, mainComponent);
    		if (properties != null)
    			declaration.getProperties().putAll(properties);
    		
    		if (models != null)
    			this.models.addAll(models);
    		
    		numInstances = 0;
    	}
    	
		@Override
		public CompositeDeclaration getDeclaration() {
			return declaration;
		}

		@Override
		public ApformInstance createInstance(Map<String, String> initialProperties) {
			numInstances ++;
			String name = declaration.getName()+"-"+numInstances;
			return new ApamOnlyComposite(declaration.getReference(),name,initialProperties);
		}

		@Override
		public void setProperty(String attr, String value) {
		}
		
		@Override
		public ApformSpecification getSpecification() {
			return null;
		}

		@Override
		public Set<ManagerModel> getModels() {
			return models;
		}

		@Override
		public Bundle getBundle() {
			// no Bundle
			return null;
		}

    }
 
    /**
     * An special apform instance created only for those composites that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlyComposite implements ApformInstance {

    	private final InstanceDeclaration declaration;
    	
    	public ApamOnlyComposite(ImplementationReference<?>implementation,String name,Map<String, String> initialProperties) {
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
		public void setProperty(String attr, String value) {
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

		@Override
		public Bundle getBundle() {
			// no bundle
			return null;
		}

    } 
    

}
