package fr.imag.adele.apam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;

public class ApamMan implements Manager {

    // The entry point in SAM
    public static ImplementationBroker SAMImplBroker = null;
    public static InstanceBroker       SAMInstBroker = null;

    @Override
    public String getName() {
        return ASM.ASMMAN;
    }

    @Override
    public List<Manager> getSelectionPathSpec(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> filter, List<Manager> involved) {
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> filter, List<Manager> involved) {
        return involved;
    }

    @Override
    public ASMInst resolveSpec(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> constraints) {
        return resolveSpec0(from, interfaceName, specName, depName, constraints, false, null);
    }

    @Override
    public Set<ASMInst> resolveSpecs(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveSpec0(client, interfaceName, specName, depName, constraints, true, allInst);
        return allInst;
    }

    public ASMInst resolveSpec0(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, boolean multiple, Set<ASMInst> allInst) {
        // second step : look for a sharable instance that satisfies the constraints
        if (specName == null)
            return null;
        ASMSpec spec = null;
        spec = ASM.ASMSpecBroker.getSpec(specName);
        if (spec != null) {
            Set<ASMInst> sharable = ASM.ASMInstBroker.getShareds(spec, client.getComposite().getApplication(), client
                    .getComposite());
            for (ASMInst inst : sharable) {
                boolean satisfies = true;
                for (Filter filter : constraints) {
                    if (!filter.match((AttributesImpl) inst.getProperties())) {
                        satisfies = false;
                        break;
                    }
                }
                if (satisfies) { // accept only if a wire is possible
                    if (client.createWire(inst, depName)) {
                        if (multiple)
                            allInst.add(inst);
                        else
                            return inst;
                    }
                }
            }
            if (multiple && !allInst.isEmpty())
                return null; // we found at least one

            // try to find a sharable implementation and instantiate.
            Set<ASMImpl> sharedImpl = ASM.ASMImplBroker.getShareds(spec, client.getComposite().getApplication(), client
                    .getComposite());
            for (ASMImpl impl : sharedImpl) {
                boolean satisfies = true;
                for (Filter filter : constraints) {
                    if (!filter.match((AttributesImpl) impl.getProperties())) {
                        satisfies = false;
                        break;
                    }
                }
                if (satisfies) { // This implem is sharable and satisfies the constraints. Instantiate.
                    ASMInst inst = impl.createInst(null);
                    // accept only if a wire is possible
                    if (client.createWire(inst, depName)) {
                        // At most one instantiation, even if multiple
                        if (multiple)
                            allInst.add(inst);
                        else
                            return inst;
                    }
                }
            }
            if (multiple && !allInst.isEmpty())
                return null; // we found at least one
        }
        return null;
    }

    @Override
    public ASMInst resolveImpl(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints) {
        return resolveImpl0(from, samImplName, implName, depName, constraints, false, null);
    }

    @Override
    public Set<ASMInst> resolveImpls(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveImpl0(from, samImplName, implName, depName, constraints, true, allInst);
        return allInst;
    }

    public ASMInst resolveImpl0(ASMInst client, String samImplName, String implName, String depName,
            Set<Filter> constraints, boolean multiple, Set<ASMInst> allInst) {

        // second pass : look for a sharable instance that satisfies the constraints
        if (implName == null)
            return null;
        ASMImpl impl = null;
        impl = ASM.ASMImplBroker.getImpl(implName);
        if (impl != null) {
            Set<ASMInst> sharable = ASM.ASMInstBroker.getShareds(impl, client.getComposite().getApplication(), client
                    .getComposite());
            for (ASMInst inst : sharable) {
                boolean satisfies = true;
                for (Filter filter : constraints) {
                    if (!filter.match((AttributesImpl) inst.getProperties())) {
                        satisfies = false;
                        break;
                    }
                }
                if (satisfies) { // accept only if a wire is possible
                    if (client.createWire(inst, depName)) {
                        if (multiple)
                            allInst.add(inst);
                        else
                            return inst;
                    }
                }

            }
            // The impl does not have sharable instance. try to instanciate.
            boolean satisfies = true;
            for (Filter filter : constraints) {
                if (!filter.match((AttributesImpl) impl.getProperties())) {
                    satisfies = false;
                    break;
                }
            }

            if (satisfies) { // This implem is sharable and satisfies the constraints. Instantiate.
                ASMInst inst = impl.createInst(null);
                // accept only if a wire is possible
                if (client.createWire(inst, depName))
                    if (multiple) // At most one instantiation, even if multiple
                        allInst.add(inst);
                return inst; // If not we have created an instance unused ! delete it ?
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public void newComposite(ManagerModel model, Composite composite) {

    }

    @Override
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints) {
        return initConstraints;
    }

}
