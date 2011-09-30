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

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.AttributesImpl;
//import fr.imag.adele.apam.samMan.SamMan;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Instance;

//import fr.imag.adele.apam.util.Util;

public class ApamMan implements Manager {

    @Override
    public String getName() {
        return CST.APAMMAN;
    }

    @Override
    public void getSelectionPathSpec(CompositeType compTypeFrom, String interfaceName, String[] interfaces,
            String specName, Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
    }

    @Override
    public void getSelectionPathImpl(CompositeType compTypeFrom, String implName, List<Manager> selPath) {
    }

    @Override
    public void getSelectionPathInst(Composite compoFrom, ASMImpl impl,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
    }

    /*
    @Override
    public ASMInst resolveSpec(Composite composite, String interfaceName, String specName,
            Set<Filter> constraints, List<Filter> preferences) {
        ASMSpec spec = null;
        if (specName == null) {
            if (interfaceName == null)
                return null;
            spec = CST.ASMSpecBroker.getSpecInterf(interfaceName);
        } else
            spec = CST.ASMSpecBroker.getSpec(specName);
        if (spec == null)
            return null;
        ASMImpl impl = findImplByInterface(composite.getCompType(), interfaceName, null, constraints, preferences);
        if (impl == null)
            return null;
        return resolveImpl(composite, impl, constraints, preferences);
    }

    @Override
    public Set<ASMInst> resolveSpecs(Composite composite, String interfaceName,
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
        ASMImpl impl = findImplByInterface(composite.getCompType(), interfaceName, null, constraints, preferences);
        if (impl == null)
            return null;
        return resolveImpls(composite, impl, constraints, preferences);
    }
    */

    /*
        private ASMInst resolveSpecxx(Composite instComposite, String interfaceName, String specName,
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
                    if ((inst.getComposite().getMainInst() != inst) && Wire.checkNewWire(instComposite, inst)) {
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
        */

    @Override
    public ASMInst resolveImpl(Composite composite, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> insts = new HashSet<ASMInst>();
        for (ASMInst inst : impl.getInsts(constraints)) {
            if (Wire.checkNewWire(composite, inst))
                insts.add(inst);
        }
        if (!insts.isEmpty())
            return impl.getPreferedInst(insts, preferences);
//        Manager samMan = CST.apam.getManager(CST.SAMMAN);
//        if (samMan != null) {
//            return ((SamMan) samMan).findSamInstForImpl(composite, impl, constraints, preferences, null);
//        }
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite composite, ASMImpl impl, Set<Filter> constraints) {
        Set<ASMInst> insts = new HashSet<ASMInst>();
        for (ASMInst asmInst : impl.getInsts(constraints)) {
            if (Wire.checkNewWire(composite, asmInst))
                insts.add(asmInst);
        }
        return insts;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public void newComposite(ManagerModel model, CompositeType composite) {
    }

    @Override
    public ASMImpl findImplByName(CompositeType compoType, String implName) {
        if (implName == null)
            return null;
        ASMImpl impl = CST.ASMImplBroker.getImpl(implName);
        if (impl == null)
            return null;
        if (Util.checkImplVisible(impl, compoType)) {
            return impl;
        }
        return null;
    }

    @Override
    public ASMImpl resolveSpecByInterface(CompositeType compoType, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences) {
        ASMSpec spec = null;
        if (interfaceName != null)
            spec = CST.ASMSpecBroker.getSpecInterf(interfaceName);
        else
            spec = CST.ASMSpecBroker.getSpec(interfaces);
        if (spec == null)
            return null;
        Set<ASMImpl> impls = new HashSet<ASMImpl>();
        // select those that are visible
        for (ASMImpl impl : spec.getImpls()) {
            if (Util.checkImplVisible(impl, compoType))
                impls.add(impl);
        }
        // AND those that match the constraints
        impls = spec.getImpls(impls, constraints);
        // and then the prefered ones.
        return spec.getPreferedImpl(impls, preferences);
    }

    @Override
    public ASMImpl resolveSpecByName(CompositeType compoType, String specName,
            Set<Filter> constraints, List<Filter> preferences) {
        ASMSpec spec = null;
        if (specName == null)
            return null;
        spec = CST.ASMSpecBroker.getSpec(specName);
        if (spec == null)
            return null;
        return spec.getImpl(constraints, preferences);
    }

    @Override
    public void notifySelection(ASMInst client, String resName, String depName, ASMImpl impl, ASMInst inst,
            Set<ASMInst> insts) {
        // do not care
    }

}