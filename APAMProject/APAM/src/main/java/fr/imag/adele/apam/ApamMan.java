package fr.imag.adele.apam;

import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.Set;

//import org.apache.felix.bundlerepository.Capability;
//import org.apache.felix.bundlerepository.Resource;
//import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.AttributesImpl;

//import fr.imag.adele.apam.util.Util;

public class ApamMan implements Manager {

    @Override
    public String getName() {
        return CST.APAMMAN;
    }

    @Override
    public List<Manager> getSelectionPathSpec(ASMInst from, Composite composite, String interfaceName, String specName,
            Set<Filter> filter, List<Manager> involved) {
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, Composite composite, String samImplName, String implName,
            Set<Filter> filter, List<Manager> involved) {
        return involved;
    }

    @Override
    public ASMInst resolveSpec(Composite implComposite, Composite instComposite, String interfaceName, String specName,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveSpec0(implComposite, instComposite, interfaceName, specName, constraints, preferences, false,
                null);
    }

    @Override
    public Set<ASMInst> resolveSpecs(Composite implComposite, Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveSpec0(implComposite, instComposite, interfaceName, specName, constraints, preferences, true, allInst);
        return allInst;
    }

    private ASMInst resolveSpec0(Composite implComposite, Composite instComposite, String interfaceName,
            String specName,
            Set<Filter> constraints, List<Filter> preferences, boolean multiple, Set<ASMInst> allInst) {
        //  look for a sharable instance that satisfies the constraints
        // make sure we have the ASM specification
        ASMSpec spec = null;
        if (specName == null) {
            if (interfaceName == null)
                return null;
            spec = CST.ASMSpecBroker.getSpecInterf(interfaceName);
        } else
            spec = CST.ASMSpecBroker.getSpec(specName);
        if (spec == null)
            return null;

        try {
            for (ASMInst inst : CST.ASMInstBroker.getInsts(spec, null)) {
                if (Wire.checkNewWire(instComposite, inst)) {
                    boolean satisfies = true;
                    for (Filter filter : constraints) {
                        if (!filter.match((AttributesImpl) inst.getProperties())) {
                            satisfies = false;
                            break;
                        }
                    }
                    if (satisfies) { // accept only if it satisfies the constraints and if a wire is possible
                        if (multiple)
                            allInst.add(inst);
                        else
                            return inst;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

        if (multiple && !allInst.isEmpty())
            return null; // we found at least one

        // try to find an  implementation and instantiate.
        for (ASMImpl impl : CST.ASMImplBroker.getImpls(spec)) {
            //    if (Util.checkImplVisible(impl, implComposite)) {
            boolean satisfies = true;
            for (Filter filter : constraints) {
                if (!filter.match((AttributesImpl) impl.getProperties())) {
                    satisfies = false;
                    break;
                }
            }
            if (satisfies) { // This implem satisfies the constraints. Instantiate.
                ASMInst inst = impl.createInst(instComposite, null);
                if (multiple) {
                    allInst.add(inst);
                    return null;
                } else {
                    return inst;
                }
            }
        }
        return null;
    }

    @Override
    public ASMInst resolveImpl(Composite implComposite, Composite instComposite, String samImplName, String implName,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveImpl0(implComposite, instComposite, implName, constraints, preferences, false, null);
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite implComposite, Composite instComposite, String samImplName,
            String implName, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveImpl0(implComposite, instComposite, implName, constraints, preferences, true, allInst);
        return allInst;
    }

    private ASMInst resolveImpl0(Composite implComposite, Composite instComposite, String implName,
            Set<Filter> constraints, List<Filter> preferences, boolean multiple, Set<ASMInst> allInst) {

        // second pass : look for a sharable instance that satisfies the constraints
        if (implName == null)
            return null;
        ASMImpl impl = null;
        impl = CST.ASMImplBroker.getImpl(implName);
        if (impl != null) {
            // Set<ASMInst> sharable = ASM.ASMInstBroker.getShareds(impl, client.getComposite().getApplication(), client
            // .getComposite());
            //boolean valide = false;
            for (ASMInst inst : impl.getInsts()) {
                if (Wire.checkNewWire(implComposite, inst)) {
                    boolean satisfies = true;
                    for (Filter filter : constraints) {
                        if (!filter.match((AttributesImpl) inst.getProperties())) {
                            satisfies = false;
                            break;
                        }
                    }
                    if (satisfies) { // accept only if satisfies constraints and a wire is possible
                        if (multiple)
                            allInst.add(inst);
                        else
                            return inst;
                    }
                }
            }

            if (multiple && !allInst.isEmpty())
                return null; // we found at least one

            // The impl does not have sharable instance. try to instanciate.
            boolean satisfies = true;
            for (Filter filter : constraints) {
                if (!filter.match((AttributesImpl) impl.getProperties())) {
                    satisfies = false;
                    break;
                }
            }

            if (satisfies) { // This implem is sharable and satisfies the constraints. Instantiate.
                ASMInst inst = impl.createInst(instComposite, null);
                // accept only if a wire is possible
                if (Wire.checkNewWire(instComposite, inst)) {
                    if (multiple) // At most one instantiation, even if multiple
                        allInst.add(inst);
                    return inst; // If not we have created an instance unused ! delete it ?
                }
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

    @Override
    public ASMImpl resolveImplByName(Composite implComposite, Composite instComposite,
            String samImplName, String implName, Set<Filter> constraints, List<Filter> preferences) {
        if (implName == null)
            return null;
        ASMImpl impl = null;
        impl = CST.ASMImplBroker.getImpl(implName);
        if (impl != null) {
            boolean satisfies = true;
            for (Filter filter : constraints) {
                if (!filter.match((AttributesImpl) impl.getProperties())) {
                    satisfies = false;
                    break;
                }
            }
            if (satisfies)
                return impl;
        }
        return null;
    }

    @Override
    public ASMImpl resolveSpecByName(Composite implComposite, Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        ASMSpec spec = null;
        if (specName == null) {
            if (interfaceName == null)
                return null;
            spec = CST.ASMSpecBroker.getSpecInterf(interfaceName);
        } else
            spec = CST.ASMSpecBroker.getSpec(specName);
        if (spec == null)
            return null;
        boolean prefer = ((preferences != null) && !preferences.isEmpty());

        // try to find an  implementation and instantiate.
        Set<ASMImpl> impls = new HashSet<ASMImpl>();
        for (ASMImpl impl : CST.ASMImplBroker.getImpls(spec)) {
            boolean satisfies = true;
            for (Filter filter : constraints) {
                if (!filter.match((AttributesImpl) impl.getProperties())) {
                    satisfies = false;
                    break;
                }
            }
            if (satisfies) {
                if (prefer) {
                    impls.add(impl);
                } else
                    return impl;
            }
        }
        if (!prefer)
            return null;
        return matchPreferences(impls, preferences);
    }

    private ASMImpl matchPreferences(Set<ASMImpl> candidates, List<Filter> preferences) {
        ASMImpl winner = null;
        int maxMatch = -1;
        for (ASMImpl impl : candidates) {
            int match = 0;
            for (Filter filter : preferences) {
                if (!filter.match((AttributesImpl) impl.getProperties()))
                    break;
                match++;
            }
            if (match > maxMatch) {
                maxMatch = match;
                winner = impl;
            }
        }
        System.out.println("   Selected : " + winner);
        return winner;
    }

}