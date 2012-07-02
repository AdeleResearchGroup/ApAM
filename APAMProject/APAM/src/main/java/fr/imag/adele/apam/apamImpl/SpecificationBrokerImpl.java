package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamInstall;

public class SpecificationBrokerImpl implements SpecificationBroker {

    private final Set<Specification> specs = Collections.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());

    protected void removeSpec(Specification spec) {
        if (spec == null)
            return;
        if (specs.contains(spec)) {
            specs.remove(spec);
            ((SpecificationImpl) spec).remove();
        }
    }

    public void addSpec(Specification spec) {
        if (spec == null)
            return;
        specs.add(spec);
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
    public Specification addSpec(String name, ApformSpecification apfSpec, Map<String, Object> properties) {
        if ((apfSpec == null))
            return null;
        SpecificationImpl spec = new SpecificationImpl(name, apfSpec, null, properties);
        specs.add(spec);
        return spec;
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
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, Object> properties) {
        if (resources == null)
            return null;
        Specification ret = null;
        ret = new SpecificationImpl(specName, null, resources, properties);
        return ret;
    }

    /**
     * Creates and deploys a specification.
     * 
     * @param compo the composite in which to create that spec.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param url the location of the executable to deploy
     */
    @Override
    public Specification createSpec(String specName, URL url) {
        assert (url != null);

        Specification spec = getSpec(specName);
        if (spec != null)
            return spec;
        spec = ApamInstall.intallSpecFromURL(url, specName);
        if (spec == null) {
            System.out.println("deployment failed for specification " + specName);
        }
        return spec;
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
