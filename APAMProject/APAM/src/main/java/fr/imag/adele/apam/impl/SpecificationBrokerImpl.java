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
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamInstall;

public class SpecificationBrokerImpl implements SpecificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(SpecificationBrokerImpl.class);
	
    private final Set<Specification> specs = Collections.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());
 
    @Override
    public Specification addSpec(ApformSpecification apfSpec, Map<String, Object> properties) {
        SpecificationImpl spec = new SpecificationImpl(apfSpec,properties);
        spec.register();
        return spec;
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
 
    @Override
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, Object> properties) {
    	
        assert specName != null && resources != null;
        return addSpec(new ApamOnlySpecification(specName,resources), properties);
    }

    /**
     * An special apform specification created only for those specifications that do not exist
     * in the Apform ipojo layer. Creates a minimal definition structure.
     */
    private static class ApamOnlySpecification implements ApformSpecification {

        private final SpecificationDeclaration declaration;

        public ApamOnlySpecification(String name, Set<ResourceReference> resources) {
            declaration = new SpecificationDeclaration(name);
            declaration.getProvidedResources().addAll(resources);
        }

        @Override
        public SpecificationDeclaration getDeclaration() {
            return declaration;
        }
        
        @Override
        public void setProperty (String attr, Object value) {
        }

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

    public void add(Specification spec) {
    	assert spec != null;
        specs.add(spec);
    }
    
    public void remove(Specification spec) {
    	assert spec != null && specs.contains(spec);
    	specs.remove(spec);
    }

    //    @Override
    //    public Specification getSpec(String[] interfaces) {
    //        if (interfaces == null)
    //            return null;
    //
    //        interfaces = Util.orderInterfaces(interfaces);
    //        for (Specification spec : specs) {
    //            if (Util.sameInterfaces(spec.getInterfaceNames(), interfaces))
    //                return spec;
    //        }
    //        return null;
    //    }

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
    public Specification getSpec(ApformSpecification apfSpec) {
        if (apfSpec == null)
            return null;
        for (Specification spec : specs) {
            if (spec.getApformSpec() == apfSpec)
                return spec;
        }
        return null;
    }

    /**
     * Returns *the first* specification that implements the provided interfaces. WARNING : the same interface can be
     * implemented by different specifications, and a specification may implement more than one interface : the first
     * spec found is returned. WARNING : convenient only if a single spec provides that interface; otherwise it is non
     * deterministic.
     * 
     * @param interfaceName : the name of the interface of the required specification.
     * @return the abstract service
     * @throws ConnectionException the connection exception Returns the ExportedSpecification exported by this Machine
     *             that satisfies the interfaces.
     */
    //    @Override
    //    public Specification getSpecResource(ResourceReference resource) {
    //        if (resource == null)
    //            return null;
    //        for (Specification spec : specs) {
    //            if (spec.getDeclaration().getProvidedResources().contains(resource))
    //                return spec;
    //        }
    //        return null;
    //    }

    /**
     * Returns the specification with the given sam name.
     * 
     * @param samName the sam name of the specification
     * @return the abstract service
     */
    @Override
    public Specification getSpecApfName(String samName) {
        if (samName == null)
            return null;
        for (Specification spec : specs) {
            if (spec.getApformSpec() != null) {
                if (spec.getApformSpec().getDeclaration().getName().equals(samName))
                    return spec;
            }
        }
        return null;
    }


    @Override
    public Set<Specification> getRequires(Specification specification) {
        SpecificationReference specRef = new SpecificationReference(specification.getName());
        Set<Specification> specs = new HashSet<Specification>();
        for (Specification spec : specs) {
            if (spec.getDeclaration().getProvidedResources().contains(specRef))
                specs.add(spec);
        }
        return specs;
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

}
