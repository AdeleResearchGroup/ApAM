package fr.imag.adele.apam.samAPIImpl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.deployment.DeploymentUnit;

public class ASMSpecBrokerImpl implements ASMSpecBroker {

    private final Set<ASMSpec> specs = new HashSet<ASMSpec>();

    @Override
    public void removeSpec(ASMSpec spec) {
        if (spec == null)
            return;
        if (specs.contains(spec)) {
            specs.remove(spec);
            spec.remove();
        }
    }

    public void addSpec(ASMSpec spec) {
        if (spec == null)
            return;
        specs.add(spec);
    }

    @Override
    public ASMSpec getSpec(String[] interfaces) {
        if (interfaces == null)
            return null;

        interfaces = Util.orderInterfaces(interfaces);
        for (ASMSpec spec : specs) {
            if (Util.sameInterfaces(spec.getInterfaceNames(), interfaces))
                return spec;
        }
        return null;
    }

    @Override
    public ASMSpec getSpec(String name) {
        if (name == null)
            return null;

        for (ASMSpec spec : specs) {
            if (spec.getASMName() == null) {
                if (spec.getSamSpec().getName().equals(name))
                    return spec;
            } else {
                if (name.equals(spec.getASMName()))
                    return spec;
            }
        }
        return null;
    }

    @Override
    public Set<ASMSpec> getSpecs() {

        return new HashSet<ASMSpec>(specs);
    }

    @Override
    public Set<ASMSpec> getSpecs(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return getSpecs();

        Set<ASMSpec> ret = new HashSet<ASMSpec>();
        for (ASMSpec spec : specs) {
            if (goal.match((AttributesImpl) spec.getProperties()))
                ret.add(spec);
        }
        return ret;
    }

    @Override
    public ASMSpec getSpec(Filter goal) throws InvalidSyntaxException {
        if (goal == null)
            return null;
        for (ASMSpec spec : specs) {
            if (goal.match((AttributesImpl) spec.getProperties()))
                return spec;
        }
        return null;
    }

    @Override
    public Set<ASMSpec> getUses(ASMSpec specification) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ASMSpec addSpec(Composite compo, String name, Specification samSpec, Attributes properties) {
        if ((compo == null) || (samSpec == null))
            return null;
        ASMSpecImpl spec = new ASMSpecImpl(compo, name, samSpec, properties);
        specs.add(spec);
        return spec;
    }

    @Override
    public ASMSpec getSpec(Specification samSpec) {
        if (samSpec == null)
            return null;
        for (ASMSpec spec : specs) {
            if (spec.getSamSpec() == samSpec)
                return spec;
        }
        return null;
    }

    @Override
    public Set<ASMSpec> getUsesRemote(ASMSpec specification) {
        // TODO Auto-generated method stub
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
    public ASMSpec getSpecInterf(String interfaceName) {
        if (interfaceName == null)
            return null;
        for (ASMSpec spec : specs) {
            String[] interfs = spec.getSamSpec().getInterfaceNames();
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
    public ASMSpec getSpecSamName(String samName) {
        if (samName == null)
            return null;
        for (ASMSpec spec : specs) {
            if (spec.getSamSpec().getName().equals(samName))
                return spec;
        }
        return null;
    }

    @Override
    public ASMSpec createSpec(Composite compo, String specName, String[] interfaces, Attributes properties) {
        if ((compo == null) || (interfaces == null))
            return null;
        ASMSpec ret = null;
        try {
            if (ASM.SAMSpecBroker.getSpecification(interfaces) != null) {
                ret = addSpec(compo, specName, ASM.SAMSpecBroker.getSpecification(interfaces), properties);
            } else {
                ret = new ASMSpecImpl(compo, specName, null, properties);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        return ret;
    }

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
    public ASMSpec createSpec(Composite compo, String specName, URL url, String type, String[] interfaces,
            Attributes properties) {
        if ((compo == null) || (interfaces == null) || (url == null))
            return null;

        try {
            DeploymentUnit du = ASM.SAMDUBroker.install(url, type);
            du.getSpecificationsName();
        } catch (ConnectionException e) {
            System.out.println("deployment failed for specification " + specName);
            e.printStackTrace();
            return null;
        }

        ASMSpec asmSpec = getSpec(specName);
        if (asmSpec != null) { // do not create twice
            ((ASMImplImpl) asmSpec).setASMName(specName);
        } else {
            asmSpec = createSpec(compo, specName, interfaces, properties);
        }
        return asmSpec;
    }

}
