package fr.imag.adele.apam.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.util.Util;

public class ApamMan implements DependencyManager {

    @Override
    public String getName() {
        return CST.APAMMAN;
    }

    // when in Felix.
    public void start() {
        System.out.println("APAMMAN started");
    }

    public void stop() {
    	System.out.println("APAMMAN stoped");
    }

    
    @Override
    public void getSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dep, List<DependencyManager> selPath) {
    }

    @Override
    public Instance resolveImpl(Composite composite, Implementation impl, DependencyDeclaration dep) {

    	Set<Filter> constraints = Util.toFilter(dep.getInstanceConstraints()) ;
    	List<Filter> preferences = Util.toFilterList(dep.getInstancePreferences()) ;
        if ((constraints == null) && (preferences == null)) {
            for (Instance inst : impl.getInsts()) {
                if (inst.isSharable() && Util.checkInstVisible(composite, inst))
                    return inst;
            }
        }

        Set<Instance> insts = new HashSet<Instance>();
        for (Instance inst : impl.getInsts()) {
            if (inst.isSharable() && inst.match(constraints) && Util.checkInstVisible(composite, inst))
                insts.add(inst);
        }
        if (!insts.isEmpty())
            return impl.getPreferedInst(insts, preferences);
        return null;
    }

    @Override
    public Set<Instance> resolveImpls(Composite composite, Implementation impl, DependencyDeclaration dep) {

    	Set<Filter> constraints = Util.toFilter(dep.getInstanceConstraints()) ;	
    	Set<Instance> insts = new HashSet<Instance>();
        for (Instance inst : impl.getInsts()) {
            if (inst.isSharable() && inst.match(constraints) && Util.checkInstVisible(composite, inst))
                insts.add(inst);
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
        if (Util.checkImplVisible(compoType, impl)) {
            return impl;
        }
        return null;
    }

    @Override
    public Specification findSpecByName(CompositeType compTypeFrom, String specName) {
        if (specName == null)
            return null;
        return CST.SpecBroker.getSpec(specName);
    }

    @Override
    public Implementation resolveSpecByResource(CompositeType compoType, DependencyDeclaration dep) {
//        assert (resource != null);
    	
        Specification spec = CST.SpecBroker.getSpecResource(dep.getTarget());
        if (spec == null)
            return null;
        
    	Set<Filter> constraints = Util.toFilter(dep.getImplementationConstraints()) ;
    	List<Filter> preferences = Util.toFilterList(dep.getImplementationPreferences()) ;

        Set<Implementation> impls = new HashSet<Implementation>();
        // select only those that are visible
        for (Implementation impl : spec.getImpls()) {
            if (Util.checkImplVisible(compoType, impl))
                impls.add(impl);
        }
        // AND those that match the constraints
        impls = spec.getImpls(impls, constraints);
        // and then the prefered ones.
        return spec.getPreferedImpl(impls, preferences);
    }


    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
            Set<Instance> insts) {
        // do not care
    }


}