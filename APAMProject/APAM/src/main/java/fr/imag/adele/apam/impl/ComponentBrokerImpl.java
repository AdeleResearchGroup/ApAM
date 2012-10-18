package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.ComponentBroker;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.util.ApamInstall;

public class ComponentBrokerImpl implements ComponentBroker{
	
	private Logger logger = LoggerFactory.getLogger(ComponentBrokerImpl.class);

    private final Set<Specification> specifications   = Collections.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());

    private final Set<Implementation> implementations = Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    private final Set<Instance>             instances = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

	@Override
	public Implementation addImpl(CompositeType composite, ApformImplementation apfImpl) {

		String implementationName = apfImpl.getDeclaration().getName();

		assert apfImpl != null;
		assert getImpl(implementationName) == null;

		if (apfImpl == null) {
			logger.error("Error adding implementation:  null Apform instance");
			return null;
		}

		Implementation implementation = getImpl(implementationName);
		if (implementation != null) {
			logger.error("Error adding implementation: already exists" + implementationName);
			return implementation;
		}

		if (composite == null)
			composite = CompositeTypeImpl.getRootCompositeType();

		/*
		 * Create and register the object in the APAM state model
		 */
		try {

			// create a primitive or composite implementation
			if (apfImpl instanceof ApformCompositeType) {
				implementation = new CompositeTypeImpl(composite,(ApformCompositeType)apfImpl);
			} else {
				implementation = new ImplementationImpl(composite, apfImpl);
			}

			((ImplementationImpl) implementation).register(null);
			return implementation;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding implementation: exception in APAM registration",configurationError);
		}
		return null;
	}

    @Override
    public Specification addSpec(ApformSpecification apfSpec) {
    	
    	String specificationName	= apfSpec.getDeclaration().getName();
    	
    	assert apfSpec != null;
    	assert getSpec(specificationName) == null;
    	
    	if (apfSpec == null)     	{
        	logger.error("Error adding specification: null Apform");
            return null;
    	}
    	
        Specification specification = getSpec(specificationName);
        if (specification != null) { 
        	logger.error("Error adding specification: already exists " + specificationName);
            return specification;
        }

    	/*
    	 * Create and register the object in the APAM state model
    	 */
        try {

        	specification = new SpecificationImpl(apfSpec);
	        ((SpecificationImpl)specification).register(null);
	        return specification;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding specification: exception in apam registration",configurationError);
		}

		return null;

    }
    
    @Override
    public Instance addInst(Composite composite, ApformInstance apfInst) {
 
    	String instanceName			= apfInst.getDeclaration().getName();
    	String implementationName	= apfInst.getDeclaration().getImplementation().getName();
    	
    	assert apfInst != null;
    	assert getInst(instanceName) == null;
    	
    	if (apfInst == null)     	{
        	logger.error("Error adding instance: null Apform instance");
            return null;
    	}

        Instance instance = getInst(instanceName);
        if (instance != null) { 
        	logger.error("Error adding instance: already exists " + instanceName);
            return instance;
        }
    	  	
        /*
         * Get the implementation group to create the instance. If the implementation can
         * not be resolved the creation of the instance fails.
         */
        Implementation implementation = getImpl(implementationName);
        if (implementation == null) {
        	logger.error("Error adding instance: implementation not found" + implementationName);
        	return null;
        }
        
    	if (composite == null)     	{
    		composite = CompositeImpl.getRootAllComposites();
    	}
        
    	/*
    	 * Create and register the object in the APAM state model
    	 */
        try {

        	instance = ((ImplementationImpl)implementation).reify(composite,apfInst);
			((InstanceImpl)instance).register(null);
	        return instance;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding instance: exception in APAM registration",configurationError);
		}

		return null;
    }



    @Override
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, String> properties) {
    	
        assert specName != null && resources != null;
        return addSpec(new ApamOnlySpecification(specName,resources,properties));
    }
   
    


    @Override
	public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, String> properties) {
		assert implName != null && url != null;
	
		Implementation impl = getImpl(implName);
		if (impl != null) { // do not create twice
			return impl;
		}
		impl = ApamInstall.intallImplemFromURL(url, implName);
		if (impl == null) {
			logger.error("deployment failed :" + implName + " at URL " + url);
			return null;
		}
	
		if (compo != null && !((CompositeTypeImpl) compo).isSystemRoot())
			((CompositeTypeImpl) compo).deploy(impl);
	
		return impl;
	}

	@Override
    public Component getComponent(String name) {    	
        if (name == null)
            return null;    	
        for (Specification spec : specifications) {
            if (name.equals(spec.getName()))
                return spec;
        }        
        for (Implementation impl : implementations) {
            if (name.equals(impl.getName()))
                return impl;
        }        
        for (Instance inst : instances) {
            if (name.equals(inst.getName()))
                return inst;
        }        
       	return null;
    }

    @Override
    public Specification getSpec(String name) {    	
        if (name == null)
            return null;    	
        for (Specification spec : specifications) {
            if (name.equals(spec.getName()))
                return spec;
        }        
       	return null;
    }


    @Override
    public Set<Specification> getSpecs() {

        return new HashSet<Specification>(specifications);
    }

    @Override
    public Set<Specification> getSpecs(Filter goal)  {
        if (goal == null)
            return getSpecs();

        Set<Specification> ret = new HashSet<Specification>();
        for (Specification spec : specifications) {
            if (spec.match(goal))
                ret.add(spec);
        }
        return ret;
    }

    @Override
    public Specification getSpecResource(ResolvableReference resource) {
        for (Specification spec : specifications) {
            // Verify if the requested resource is the spec itself
            if (spec.getDeclaration().getReference().equals(resource))
                return spec;
            // Verify if the requested resource is provided by the spec
            if (spec.getDeclaration().getProvidedResources().contains(resource))
                return spec;
        }
        return null;
    }

	@Override
	public Set<Implementation> getImpls() {
		return Collections.unmodifiableSet(implementations);
		// return new HashSet<ASMImpl> (implems) ;
	}

	@Override
	public Implementation getImpl(String name) {

		if (name == null)
			return null;

		for (Implementation impl : implementations) {
			if (name.equals(impl.getName()))
				return impl;
		}
		return null;
	}

	@Override
	public Set<Implementation> getImpls(Filter goal)  {
		if (goal == null)
			return getImpls();

		Set<Implementation> ret = new HashSet<Implementation>();
		for (Implementation impl : implementations) {
			if (impl.match(goal))
				ret.add(impl);
		}
		return ret;
	}

    
    @Override
    public Instance getInst(String instName) {
        if (instName == null)
            return null;
        for (Instance inst : instances) {
            if (inst.getName().equals(instName)) {
                return inst;
            }
        }
        return null;
    }

    @Override
    public Set<Instance> getInsts() {
        return Collections.unmodifiableSet(instances);
    }

    @Override
    public Set<Instance> getInsts(Filter goal) {
        if (goal == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (inst.match(goal))
                ret.add(inst);
        }
        return ret;
    }
 
    ///Remove
	// Not in the interface. No control
	/**
	 * TODO change visibility, currently this method is public to be visible
	 * from Apform
	 */
	
    public void removeSpec(Specification spec) {
    	removeSpec(spec,true);
    }
    
    protected void removeSpec(Specification spec, boolean notify) {
     	((SpecificationImpl)spec).unregister(); 	
    }
    void remove(Specification spec) {
    	assert spec != null && specifications.contains(spec);
    	specifications.remove(spec);
    }

    
	public void removeImpl(Implementation implementation) {
		removeImpl(implementation, true);
	}
	protected void removeImpl(Implementation implementation, boolean notify) {
		((ComponentImpl) implementation).unregister();
	}
	protected void remove(Implementation implementation) {
		assert implementation != null && implementations.contains(implementation);
		implementations.remove(implementation);
	}

	
    public void removeInst(Instance inst) {
    	removeInst(inst,true);
    }
    protected void removeInst(Instance inst, boolean notify) {
        ((InstanceImpl)inst).unregister();
    }
    protected void remove(Instance instance) {
    	assert instance != null && instances.contains(instance);
    	instances.remove(instance);
    }
    
    
//Add internal
    public void add(Specification spec) {
    	assert spec != null;
        specifications.add(spec);
    }

	public void add(Implementation implementation) {
		assert implementation != null && !implementations.contains(implementation);
		implementations.add(implementation);
	}

    public void add(Instance instance) {
    	assert instance != null && ! instances.contains(instance);
    	instances.add(instance);
    }

    
    @Override
    public Component getWaitComponent(String name) {

    	Component compo = getComponent(name);
    	if ( compo != null)
    		return compo;
    	
    	/*
    	 * If not found wait and try again 
    	 */
    	Apform2Apam.waitForComponent(name);
    	compo = getComponent(name);
    	
        if (compo == null) // should never occur
            logger.debug("wake up but component is not present " + name);
    	
       	return compo;
    }

    
    
    ///special case
	
    /**
     * An special apform specification created only for those specifications that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlySpecification implements ApformSpecification {

        private final SpecificationDeclaration declaration;

        public ApamOnlySpecification(String name, Set<ResourceReference> resources, Map<String,String> properties) {
            declaration = new SpecificationDeclaration(name);
            declaration.getProvidedResources().addAll(resources);
            if (properties != null)
            	declaration.getProperties().putAll(properties);
        }

        @Override
        public SpecificationDeclaration getDeclaration() {
            return declaration;
        }
        
        @Override
        public void setProperty (String attr, String value) {
        }

		@Override
		public Bundle getBundle() {
			// No bundle so far
			return null;
		}
    }

	@Override
	public void removeComponent(Component component) {
		if (component instanceof Specification) {
			removeSpec ((Specification)component) ;
		}
		if (component instanceof Implementation) {
			removeImpl ((Implementation)component) ;
		}
		if (component instanceof Instance) {
			removeInst ((Instance)component) ;
		}
	}

	@Override
	public Instance getInstService(Object service) {
		for (Instance inst : instances) {
			if (inst.getServiceObject() == service) 
				return inst ;
		}
		return null;
	}
}
