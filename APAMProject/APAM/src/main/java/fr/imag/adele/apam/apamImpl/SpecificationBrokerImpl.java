package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
//import fr.imag.adele.sam.Specification;

import fr.imag.adele.sam.deployment.DeploymentUnit;

public class SpecificationBrokerImpl implements SpecificationBroker {

    private final Set<Specification> specs = new HashSet<Specification>();

    @Override
    public void removeSpec(Specification spec) {
        if (spec == null)
            return;
        if (specs.contains(spec)) {
            specs.remove(spec);
            spec.remove();
        }
    }

    public void addSpec(Specification spec) {
        if (spec == null)
            return;
        specs.add(spec);
    }

    @Override
    public Specification getSpec(String[] interfaces) {
        if (interfaces == null)
            return null;

        interfaces = Util.orderInterfaces(interfaces);
        for (Specification spec : specs) {
            if (Util.sameInterfaces(spec.getInterfaceNames(), interfaces))
                return spec;
        }
        return null;
    }

    @Override
    public Specification getSpec(String name) {
        if (name == null)
            return null;

        for (Specification spec : specs) {
            if (name.equals(spec.getName()))
                return spec;

//            if (spec.getName() == null) {
//                if (spec.getApformSpec().getName().equals(name))
//                    return spec;
//            } else {
//                if (name.equals(spec.getName()))
//                    return spec;
//            }
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
            if (goal.match((AttributesImpl) spec.getProperties()))
                ret.add(spec);
        }
        return ret;
    }

    @Override
    public Specification getSpec(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return null;
        for (Specification spec : specs) {
            if (goal.match((AttributesImpl) spec.getProperties()))
                return spec;
        }
        return null;
    }

    @Override
    public Specification addSpec(String name, ApformSpecification apfSpec, Attributes properties) {
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
    @Override
    public Specification getSpecInterf(String interfaceName) {
        if (interfaceName == null)
            return null;
        for (Specification spec : specs) {
            String[] interfs = spec.getInterfaceNames();
            for (String interf : interfs) {
                if (interf.equals(interfaceName))
                    return spec;
            }
        }
        return null;
    }

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
                if (spec.getApformSpec().getName().equals(samName))
                    return spec;
            }
        }
        return null;
    }

    @Override
    public Specification createSpec(String specName, String[] interfaces, Attributes properties) {
        if (interfaces == null)
            return null;
        Specification ret = null;
        ret = new SpecificationImpl(specName, null, interfaces, properties);
        return ret;
    }

//        try {
//            if (CST.SAMSpecBroker.getSpecification(interfaces) != null) {
//                ret = addSpec(specName, CST.SAMSpecBroker.getSpecification(interfaces), properties);
//            } else {
//                ret = new ASMSpecImpl(specName, null, properties);
//            }
//        } catch (ConnectionException e) {
//            e.printStackTrace();
//        }
//        return ret;
//    }

    /**
     * Creates and deploys a specification. WARNING : The fact to deploy the specification (the packages containing the
     * interfaces) does not create any spec in SAM. This spec may not have any corresponding spec in SAM. It does not
     * try to create one in SAM. WARNING : xwhat to do if the spec already exists in SAM : Deploy anyway ?
     * 
     * @param compo the composite in which to create that spec.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param url the location of the executable to deploy
     * @param type type of executable to deploy (bundle, jar, war, exe ...)
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties. return an ASM Specification
     */
    @Override
    public Specification createSpec(String specName, URL url, String[] interfaces, Attributes properties) {
        if ((interfaces == null) || (url == null))
            return null;

        try {
            DeploymentUnit du = CST.SAMDUBroker.install(url, "bundle");
            du.getSpecificationsName();
        } catch (ConnectionException e) {
            System.out.println("deployment failed for specification " + specName);
            e.printStackTrace();
            return null;
        }

        Specification asmSpec = getSpec(specName);
        if (asmSpec == null) { // do not create twice
            asmSpec = createSpec(specName, interfaces, properties);
        }
        return asmSpec;
    }

    @Override
    public Set<Specification> getRequires(Specification specification) {
        // TODO Auto-generated method stub
        return null;
    }

}
