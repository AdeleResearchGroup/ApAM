package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.InstanceBroker;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformInstance;

public class InstanceBrokerImpl implements InstanceBroker {

    private static final Logger logger 		= LoggerFactory.getLogger(InstanceBrokerImpl.class);
	
    private final Set<Instance> instances   = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

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
        	logger.error("Error adding instance: already exists" + instanceName);
            return instance;
        }
    	  	
        /*
         * Get the implementation group to create the instance. If the implementation could
         * not be resolved wait until it is deployed
         * 
         * TODO Define a timeout to avoid blocking the thread infinitely or implement 
         * change this method signature to become asynchronous.
         */
        Implementation implementation = CST.ImplBroker.getImpl(implementationName,true);

        
    	if (composite == null)     	{
    		composite = CompositeImpl.getRootAllComposites();
    	}
        
    	instance = ((ImplementationImpl)implementation).reify(composite,apfInst);
        ((InstanceImpl)instance).register(null);
        return instance;
    }
    
    /**
     * TODO change visibility, currently this method is public to be visible from Apform
     */
    public  void removeInst(Instance inst) {
    	removeInst(inst,true);
    }
    
    protected void removeInst(Instance inst, boolean notify) {
        ((InstanceImpl)inst).unregister();
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
    public Set<Instance> getInsts(Specification spec, Filter goal) throws InvalidSyntaxException {
        if (spec == null)
            return null;
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if ((inst.getSpec() == spec) && inst.match(goal))
                ret.add(inst);
        }
        return ret;
    }

    @Override
    public Set<Instance> getInsts(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getInsts();
        Set<Instance> ret = new HashSet<Instance>();
        for (Instance inst : instances) {
            if (inst.match(goal))
                ret.add(inst);
        }
        return ret;
    }

    public void add(Instance instance) {
    	assert instance != null && ! instances.contains(instance);
    	instances.add(instance);
    }
    
    public void remove(Instance instance) {
    	assert instance != null && instances.contains(instance);
    	instances.remove(instance);
    }


}
