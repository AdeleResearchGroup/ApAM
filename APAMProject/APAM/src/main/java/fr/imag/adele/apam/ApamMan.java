package fr.imag.adele.apam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.util.Util;

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

    @Override
    public ASMInst resolveImpl(Composite composite, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> insts = new HashSet<ASMInst>();
        for (ASMInst inst : impl.getSharableInsts(constraints)) {
            if (Util.checkInstVisible(composite, inst))
                insts.add(inst);
        }
        if (!insts.isEmpty())
            return impl.getPreferedInst(insts, preferences);
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite composite, ASMImpl impl, Set<Filter> constraints) {
        Set<ASMInst> insts = new HashSet<ASMInst>();
        for (ASMInst asmInst : impl.getSharableInsts(constraints)) {
            if (Util.checkInstVisible(composite, asmInst))
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