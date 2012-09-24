package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.util.ApamInstall;

public class SpecificationBrokerImpl implements SpecificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(SpecificationBrokerImpl.class);
	
    private final Set<Specification> specs = Collections.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());
 
    @Override
    public Specification addSpec(ApformSpecification apfSpec) {
    	
    	String specificationName	= apfSpec.getDeclaration().getName();
    	
    	assert apfSpec != null;
    	assert getSpec(specificationName) == null;
    	
    	if (apfSpec == null)     	{
        	logger.error("Error adding specification: null Apform");
            return null;
    	}
    	
        Specification specification =getSpec(specificationName);
        if (specification != null) { 
        	logger.error("Error adding specification: already exists " + specificationName);
            return specification;
        }

    	
        specification = new SpecificationImpl(apfSpec);
        ((SpecificationImpl)specification).register(null);
        return specification;
    }

    @Override
    public Specification createSpec(String specName, URL url) {
    	assert specName != null && url != null;

        Specification spec = getSpec(specName);
        if (spec != null)
            return spec;
        spec = ApamInstall.intallSpecFromURL(url, specName);
        if (spec == null) {
            logger.debug("deployment failed for specification " + specName);
        }
        return spec;
    }
 

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

    }

    @Override
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, String> properties) {
    	
        assert specName != null && resources != null;
        return addSpec(new ApamOnlySpecification(specName,resources,properties));
    }
   
    // Not in the interface. No control
    /**
     * TODO change visibility, currently this method is public to be visible from Apform
     */
    public void removeSpec(Specification spec) {
    	removeSpec(spec,true);
    }
    
    protected void removeSpec(Specification spec, boolean notify) {
    	
        /*
         * remove from Apam state model
         */
    	((SpecificationImpl)spec).unregister();
    	
    }


    @Override
    public Specification getSpec(String name) {
    	
        if (name == null)
            return null;
    	
        for (Specification spec : specs) {
            if (name.equals(spec.getName()))
                return spec;
        }
        
       	return null;
    }

    @Override
    public Specification getSpec(String name, boolean wait) {

    	Specification specification = getSpec(name);
    	if ( specification != null || !wait)
    		return specification;
    	
    	/*
    	 * If not found wait and try again 
    	 */
    	Apform2Apam.waitForComponent(name);
    	specification = getSpec(name);
    	
        if (specification == null) // should never occur
            logger.debug("wake up but specification is not present " + name);
    	
       	return specification;
    }

    @Override
    public Set<Specification> getSpecs() {

        return new HashSet<Specification>(specs);
    }

    @Override
    public Set<Specification> getSpecs(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getSpecs();

        Set<Specification> ret = new HashSet<Specification>();
        for (Specification spec : specs) {
            if (spec.match(goal))
                ret.add(spec);
        }
        return ret;
    }

    @Override
    public Specification getSpec(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return null;
        for (Specification spec : specs) {
            if (spec.match(goal))
                return spec;
        }
        return null;
    }

    @Override
    public Specification getSpec(Set<ResourceReference> providedResources) {
        for (Specification spec : specs) {
            if (spec.getDeclaration().getProvidedResources().equals(providedResources))
                return spec;
        }
        return null;
    }

    @Override
    public Specification getSpecResource(ResolvableReference resource) {
        for (Specification spec : specs) {
            // Verify if the requested resource is the spec itself
            if (spec.getDeclaration().getReference().equals(resource))
                return spec;
            // Verify if the requested resource is provided by the spec
            if (spec.getDeclaration().getProvidedResources().contains(resource))
                return spec;
        }
        return null;
    }
    
    public void add(Specification spec) {
    	assert spec != null;
        specs.add(spec);
    }
    
    public void remove(Specification spec) {
    	assert spec != null && specs.contains(spec);
    	specs.remove(spec);
    }
    

}
