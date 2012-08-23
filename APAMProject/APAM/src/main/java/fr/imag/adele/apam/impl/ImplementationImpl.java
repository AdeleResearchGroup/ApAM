package fr.imag.adele.apam.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

public class ImplementationImpl extends ComponentImpl implements Implementation {

	private static Logger 				logger 				= LoggerFactory.getLogger(ImplementationImpl.class);
    private static final long   		serialVersionUID	= 1L;

    // composite in which it is contained
    private  Set<CompositeType>  		inComposites	= Collections.newSetFromMap(new ConcurrentHashMap<CompositeType, Boolean>());

    private Specification      			mySpec;

    // the instances
    private Set<Instance>      			instances		= Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());
    
    // all relationship use and their reverse
    private Set<Implementation> 		uses			= Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());
    private Set<Implementation> 		invUses			= Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

    // the sharable instances
    //    protected Set<Instance>             sharableInstances = new HashSet<Instance>();      // the sharable instances


    /**
     * This class represents the Apam root implementation. 
     * 
     * This is an APAM concept without mapping at the execution platform level, we build an special
     * apform object to represent it.
     * 
     */
    private static class SystemRootImplementation implements ApformImplementation {

        private final CompositeDeclaration declaration;

		public SystemRootImplementation(String name) {
        	this.declaration =  new CompositeDeclaration(name,
        								(SpecificationReference)null,
        								new ImplementationReference<ImplementationDeclaration>("none"),
        								(String)null,new ArrayList<String>());
		}

		@Override
		public ImplementationDeclaration getDeclaration() {
			return declaration;
		}

		@Override
		public ApformSpecification getSpecification() {
			return null;
		}

		@Override
		public ApformInstance createInstance(Map<String, Object> initialproperties) {
			throw new UnsupportedOperationException("method not available in root type");
		}

		@Override
		public void setProperty(String attr,Object value) {
			throw new UnsupportedOperationException("method not available in root type");
		}
    	
    }

    /**
     * This is an special constructor only used for the root type of the system 
     */
    protected ImplementationImpl(String name) {
    	super(new SystemRootImplementation(name),null);
    	mySpec = CST.SpecBroker.createSpec(name+"_spec",new HashSet<ResourceReference>(),null);
    }

    /**
     * Builds a new Apam implementation to represent the specified platform implementtaion in the Apam model.
     */
   protected ImplementationImpl(CompositeType composite, ApformImplementation apfImpl, Map<String, Object> props) {
        super (apfImpl, props) ;
        
        ImplementationDeclaration declaration = apfImpl.getDeclaration();
        
        /*
         * Reference the declared provided specification
         */
        if (declaration.getSpecification() != null) {
        	mySpec = CST.SpecBroker.getSpec(declaration.getSpecification().getName());
        	
        	/*
        	 * The specification may not have been defined in Apam yet, in this case we create
        	 * a temporary declaration that will be overridden when the actual declaration is
        	 * installed @see ApformApam.newSpecification
        	 * 
        	 * TODO WARNING This is an aproximation as the implementation may provide more
        	 * resources that the specification, to review.
        	 * 
        	 * TODO Should we enforce that the provided specification must be declared and
        	 * installed before any implementation referencing it (or at least providing it)?
        	 * otherwise we can not validate that the implementation actually conforms to the
        	 * declared specification.
        	 */
        	
        	if (mySpec == null) {
        		mySpec = CST.SpecBroker.createSpec(declaration.getSpecification().getName(),
        									declaration.getProvidedResources(),
        									(Map<String,Object>)null);
        	}
        }
        
        /*
         * If the implementation does not provides explicitly any specification, we build a dummy 
         * specification to allow the resolution algorithm to access the provided resources of this
         * implementation
         */
        if (declaration.getSpecification() == null) {
        	mySpec = CST.SpecBroker.createSpec(declaration.getName() + "_spec",
					declaration.getProvidedResources(),
					(Map<String,Object>)null);
        }
        
        /*
         * Reference the enclosing composite type
         */
        addInComposites(composite);
        
        /*
         * Add predefined properties
         */
        put(CST.A_IMPLNAME, declaration.getName());
    }

	@Override
	public void register() {
		
    	/*
    	 * Opposite references from specification and enclosing composite type
    	 */
		((SpecificationImpl)mySpec).addImpl(this);
		
		for (CompositeType inComposite : inComposites) {
	        ((CompositeTypeImpl)inComposite).addImpl(this);
		}
        
        /*
         * Add to broker
         */
        ((ImplementationBrokerImpl)CST.ImplBroker).add(this);
        
        /*
         * Notify managers
         */
        ApamManagers.notifyAddedInApam(this);
        
	}


	@Override
	public void unregister() {

        /*
         * Notify managers
         */
        ApamManagers.notifyRemovedFromApam(this);
		
		/*
		 * remove all existing instances
		 * 
		 */
        for (Instance inst : instances) {
            ((InstanceBrokerImpl)CST.InstBroker).removeInst(inst);
        }

    	/*
    	 * Remove opposite references from specification and enclosing composite types
    	 */
        ((SpecificationImpl) getSpec()).removeImpl(this);
		for (CompositeType inComposite : inComposites) {
	        ((CompositeTypeImpl)inComposite).removeImpl(this);
		}
		
		mySpec = null;
		inComposites.clear();
		

        /*
         * Remove from broker
         */
        ((ImplementationBrokerImpl) CST.ImplBroker).remove(this);
        
	}

    @Override
    public ApformImplementation getApformImpl() {
        return (ApformImplementation) getApformComponent();
    }

    @Override
    public ImplementationDeclaration getImplDeclaration() {
        return (ImplementationDeclaration)getDeclaration();
    }

    @Override
    public Specification getSpec() {
        return mySpec;
    }
    
    @Override
    public Set<CompositeType> getInCompositeType() {
        return Collections.unmodifiableSet(inComposites);
    }
    
    public void addInComposites(CompositeType compo) {
        inComposites.add(compo);
    }

    public void removeInComposites(CompositeType compo) {
        inComposites.remove(compo);
    }

    @Override
    public boolean isUsed() {
        return ! inComposites.contains(CompositeTypeImpl.getRootCompositeType());
    }

    /**
     * only here for future optimization.
     * shared is applied on all the instances
     */
    @Override
    public boolean isSharable() {
        if (get(CST.A_SHARED) == null)
            return true;
        return get(CST.A_SHARED).equals(CST.V_TRUE);
    }

    @Override
    public boolean isInstantiable() {
        String instantiable = (String) get(CST.A_INSTANTIABLE);
        return (instantiable == null) ? true : instantiable.equals(CST.V_TRUE);
    }
	
    /**
     * From an implementation, create an instance. Creates both the apform and APAM instances.
     * Can be called from the API. 
     * 
     * Must check if source composite can instantiate this implementation.
     */
    @Override
    public Instance createInstance(Composite composite, Map<String, Object> initialproperties) {
        if ((composite != null) && !Util.checkImplVisible(composite.getCompType(), this)) {
            logger.error("cannot instantiate " + this + ". It is not visible from composite " + composite);
            return null;
        }

    	if (composite == null)     	{
    		composite = CompositeImpl.getRootAllComposites();
    	}
        
        Instance instance = instantiate(composite, initialproperties);
        ((InstanceImpl)instance).register();
        
        return instance;
    }
	
	/**
     * Create a new instance from this implementation in Apam and in the underlying execution platform.
     * 
     * WARNING The created Apam instance is not automatically published in the Apam state, nor
     * added to the list of instances of this implementation. This is actually done when the returned
     * instance is registered by the caller of this method.
     * 
     * This method is not intended to be used as external API.
     */
    protected Instance instantiate(Composite composite, Map<String,Object> initialproperties) {
        if (! this.isInstantiable()) {
        	logger.debug("Implementation " + this + " is not instantiable");
            return null;
        }
        
        return reify(composite,getApformImpl().createInstance(initialproperties),null);
    }
    
    /**
     * Reifies in Apam an instance of this implementation from the information of the underlying
     * platform. 
     * 
     * This method should be overridden to implement different reification semantics for different
     * subclasses of implementation.
     * 
     * WARNING The reified Apam instance is not automatically published in the Apam state, nor
     * added to the list of instances of this implementation. This is actually done when the returned
     * instance is registered by the caller of this method.
     * 
     * This method is not intended to be used as external API.
     * 
     */
    protected Instance reify(Composite composite, ApformInstance platformInstance, Map<String,Object> initialproperties) {
        return new InstanceImpl(composite,platformInstance,initialproperties);
    }
    

    // WARNING : no control ! Only called by instance registration.
    public void addInst(Instance instance) {
       	assert instance != null && !instances.contains(instance);
       	instances.add(instance);
    }
    
    public void removeInst(Instance instance) {
    	assert instance != null && instances.contains(instance);
        instances.remove(instance);
    }

    @Override
    public Instance getInst(String targetName) {
        if (targetName == null)
            return null;
        for (Instance inst : instances) {
            if (inst.getName().equals(targetName))
                return inst;
        }
        return null;
    }

    /**
     * returns the first instance only.
     */
    @Override
    public Instance getInst() {
        if (instances.size() == 0)
            return null;
        return (Instance) instances.toArray()[0];
    }

    @Override
    public Set<Instance> getInsts() {
        return Collections.unmodifiableSet(instances);
    }

    //    @Override
    //    public Set<Instance> getSharableInsts() {
    //        return Collections.unmodifiableSet(sharableInstances);
    //        // return new HashSet <ASMInst> (instances) ;
    //    }

    @Override
    public Set<Instance> getInsts(Filter query) throws InvalidSyntaxException {
        if (query == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (inst.match(query))
                ret.add(inst);
        }
        return ret;
    }

    //    @Override
    //    public Set<Instance> getSharableInsts(Filter query) throws InvalidSyntaxException {
    //        if (query == null)
    //            return getSharableInsts();
    //        Set<Instance> ret = new HashSet<Instance>();
    //        for (Instance inst : sharableInstances) {
    //            if (inst.match(query))
    //                ret.add(inst);
    //        }
    //        return ret;
    //    }

    @Override
    public Set<Instance> getInsts(Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return Collections.unmodifiableSet(instances);
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            for (Filter filter : constraints) {
                if (inst.match(filter)) {
                    ret.add(inst);
                }
            }
        }
        return ret;
    }

    //    @Override
    //    public Set<Instance> getSharableInsts(Set<Filter> constraints) {
    //        if ((constraints == null) || constraints.isEmpty())
    //            return Collections.unmodifiableSet(sharableInstances);
    //        Set<Instance> ret = new HashSet<Instance>();
    //        for (Instance inst : sharableInstances) {
    //            for (Filter filter : constraints) {
    //                if (inst.match(filter)) {
    //                    ret.add(inst);
    //                }
    //            }
    //        }
    //        return ret;
    //    }

    @Override
    public Instance getInst(Set<Filter> constraints, List<Filter> preferences) {
        Set<Instance> insts = null;
        if ((preferences != null) && !preferences.isEmpty()) {
            insts = getInsts(constraints);
        } else
            insts = instances;
        if ((constraints == null) || constraints.isEmpty())
            return ((Instance) insts.toArray()[0]);

        return getPreferedInst(insts, preferences);
    }

    //    @Override
    //    public Instance getSharableInst(Set<Filter> constraints, List<Filter> preferences) {
    //        Set<Instance> insts = null;
    //        if ((preferences != null) && !preferences.isEmpty()) {
    //            insts = getSharableInsts(constraints);
    //        } else
    //            insts = sharableInstances;
    //
    //        if (insts.isEmpty())
    //            return null;
    //
    //        if ((constraints == null) || constraints.isEmpty())
    //            return ((Instance) insts.toArray()[0]);
    //
    //        return getPreferedInst(insts, preferences);
    //    }

    @Override
    public Instance getPreferedInst(Set<Instance> candidates, List<Filter> preferences) {
        if ((preferences == null) || preferences.isEmpty()) {
            return (Instance) candidates.toArray()[0];
        }
        Instance winner = null;
        int maxMatch = -1;
        for (Instance inst : candidates) {
            int match = 0;
            for (Filter filter : preferences) {
                if (!inst.match(filter))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = inst;
            }
        }
        logger.debug("   Selected : " + winner);
        return winner;
    }


    // relation uses control
    
    @Override
    public Set<Implementation> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    @Override
    public Set<Implementation> getInvUses() {
        return Collections.unmodifiableSet(invUses);
    }
    
    public void addUses(Implementation dest) {
        if (uses.contains(dest))
            return;
        uses.add(dest);
        ((ImplementationImpl) dest).addInvUses(this);
        ((SpecificationImpl) getSpec()).addRequires(dest.getSpec());
    }

    public void removeUses(Implementation dest) {
        for (Instance inst : instances) {
            for (Instance instDest : inst.getWireDests())
                if (instDest.getImpl() == dest) {
                    return; // it exists another instance that uses that destination. Do nothing.
                }
        }
        uses.remove(dest);
        ((ImplementationImpl) dest).removeInvUses(this);
        ((SpecificationImpl) getSpec()).removeRequires(dest.getSpec());
    }

    private void addInvUses(Implementation orig) {
        invUses.add(orig);
    }

    private void removeInvUses(Implementation orig) {
        invUses.remove(orig);
    }

}
