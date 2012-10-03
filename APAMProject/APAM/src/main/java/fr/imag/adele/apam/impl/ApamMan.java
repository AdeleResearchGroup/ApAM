package fr.imag.adele.apam.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
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

    private BundleContext context;


    @Override
    public String getName() {
        return CST.APAMMAN;
    }
    
    public ApamMan(BundleContext context){
        this.context = context;
    }

    // when in Felix.
    public void start() {
        try {
            Util.printFileToConsole(context.getBundle().getResource("logo.txt"));
        } catch (IOException e) {
           //TODO nothing
        }
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
    public Set<Implementation> resolveSpecByResources(CompositeType compoType, DependencyDeclaration dep) {
        Specification spec = CST.SpecBroker.getSpecResource(dep.getTarget());
        if (spec == null) return null;
        
    	Set<Filter> constraints = Util.toFilter(dep.getImplementationConstraints()) ;
        Set<Implementation> impls = new HashSet<Implementation>();

        // select only those that are visible
        for (Implementation impl : spec.getImpls()) {
            if (Util.checkImplVisible(compoType, impl))
                impls.add(impl);
        }
        // AND those that match the constraints
        return spec.getImpls(impls, constraints);
    }

    @Override
    public Implementation resolveSpecByResource(CompositeType compoType, DependencyDeclaration dep) {
        Specification spec = CST.SpecBroker.getSpecResource(dep.getTarget());
        if (spec == null)
            return null;	

    	Set<Implementation> impls = resolveSpecByResources (compoType, dep) ;
        // and then the prefered ones.
     	List<Filter> preferences = Util.toFilterList(dep.getImplementationPreferences()) ;
        return spec.getPreferedImpl(impls, preferences);
    }


    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
            Set<Instance> insts) {
        // do not care
    }


}