package fr.imag.adele.apam.apamImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.util.Util;

public class ApamMan implements Manager {

    @Override
    public String getName() {
        return CST.APAMMAN;
    }

    @Override
    public void getSelectionPathSpec(CompositeType compTypeFrom, ResourceReference resource,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
    }

    @Override
    public void getSelectionPathImpl(CompositeType compTypeFrom, String implName, List<Manager> selPath) {
    }

    @Override
    public void getSelectionPathInst(Composite compoFrom, Implementation impl,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
    }

    @Override
    public Instance resolveImpl(Composite composite, Implementation impl, Set<Filter> constraints, List<Filter> preferences) {
        Set<Instance> insts = new HashSet<Instance>();
        for (Instance inst : impl.getSharableInsts(constraints)) {
            if (Util.checkInstVisible(composite, inst))
                insts.add(inst);
        }
        if (!insts.isEmpty())
            return impl.getPreferedInst(insts, preferences);
        return null;
    }

    @Override
    public Set<Instance> resolveImpls(Composite composite, Implementation impl, Set<Filter> constraints) {
        Set<Instance> insts = new HashSet<Instance>();
        for (Instance asmInst : impl.getSharableInsts(constraints)) {
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
    public Implementation findImplByName(CompositeType compoType, String implName) {
        if (implName == null)
            return null;
        Implementation impl = CST.ImplBroker.getImpl(implName);
        if (impl == null)
            return null;
        if (Util.checkImplVisible(impl, compoType)) {
            return impl;
        }
        return null;
    }

    @Override
    public Implementation resolveSpecByResource(CompositeType compoType, ResourceReference resource,
            Set<Filter> constraints, List<Filter> preferences) {
        assert (resource != null);

        Specification spec = CST.SpecBroker.getSpecResource(resource);
        if (spec == null)
            return null;
        Set<Implementation> impls = new HashSet<Implementation>();
        // select those that are visible
        for (Implementation impl : spec.getImpls()) {
            if (Util.checkImplVisible(impl, compoType))
                impls.add(impl);
        }
        // AND those that match the constraints
        impls = spec.getImpls(impls, constraints);
        // and then the prefered ones.
        return spec.getPreferedImpl(impls, preferences);
    }

    //    @Override
    //    public Implementation resolveSpecByName(CompositeType compoType, String specName,
    //            Set<Filter> constraints, List<Filter> preferences) {
    //        Specification spec = null;
    //        if (specName == null)
    //            return null;
    //        spec = CST.SpecBroker.getSpec(specName);
    //        if (spec == null)
    //            return null;
    //        return spec.getImpl(constraints, preferences);
    //    }

    @Override
    public void notifySelection(Instance client, String resName, String depName, Implementation impl, Instance inst,
            Set<Instance> insts) {
        // do not care
    }

}