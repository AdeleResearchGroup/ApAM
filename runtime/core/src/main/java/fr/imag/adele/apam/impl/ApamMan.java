package fr.imag.adele.apam.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.util.Util;

public class ApamMan implements DependencyManager {

	private BundleContext context;


	@Override
	public String getName() {
		return CST.APAMMAN;
	}

	public ApamMan(){
	}
	public ApamMan(BundleContext context){
		this.context = context;
	}

	// when in Felix.
	public void start() {
		try {
			Util.printFileToConsole(context.getBundle().getResource("logo.txt"));
		} catch (IOException e) {
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
		if ((constraints.isEmpty()) && (preferences.isEmpty())) { 
			for (Instance inst : impl.getInsts()) {
				if (inst.isSharable() && Util.checkInstVisible(composite, inst))
					return inst;
			}
			return null ;
		}

		Set<Instance> insts = new HashSet<Instance>();
		for (Instance inst : impl.getInsts()) {
			if (inst.isSharable() && inst.match(constraints) && Util.checkInstVisible(composite, inst))
				insts.add(inst);
		}
		if (!insts.isEmpty())
			return impl.getPreferedComponent(insts, preferences, null);
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
	public Instance findInstByName(Composite compo, String instName) {
		if (instName == null) return null;
		Instance inst = CST.componentBroker.getInst(instName);
		if (inst == null) return null;
		if (Util.checkInstVisible(compo, inst)) {
			return inst;
		}
		return null;
	}

	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		if (implName == null)
			return null;
		Implementation impl = CST.componentBroker.getImpl(implName);
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
		return CST.componentBroker.getSpec(specName);
	}

	@Override
	public Component findComponentByName(CompositeType compoType, String componentName) {
		Component ret = CST.componentBroker.getComponent (componentName) ;
		if (ret == null) return null ;
		if (ret instanceof Specification) return ret ;
		if (ret instanceof Implementation) {
			if (Util.checkImplVisible(compoType, (Implementation)ret)) 
				return ret ;
		}
		//It is an instance
		// TODO We do not have the composite instance at that point; cannot check the visibility !
		//return the instance in all case: the resolution checks the visibility
		//if (Util.checkInstVisible(compoFrom, (Instance)ret)) 
		//return ret ;
		return null ;
	}


	@Override
	public Set<Implementation> resolveSpecs(CompositeType compoType, DependencyDeclaration dep) {
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null) return null;

		Set<Filter> constraints = Util.toFilter(dep.getImplementationConstraints()) ;
		Set<Implementation> impls = new HashSet<Implementation>();

		// select only those that are visible
		for (Implementation impl : spec.getImpls()) {
			if (Util.checkImplVisible(compoType, impl))
				impls.add(impl);
		}
		// AND those that match the constraints
		return spec.getSelectedComponents(impls, constraints);
	}

	@Override
	public Implementation resolveSpec(CompositeType compoType, DependencyDeclaration dep) {
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null)
			return null;	

		Set<Implementation> impls = resolveSpecs (compoType, dep) ;
		Set<Filter> instConstraints = Util.toFilter(dep.getInstanceConstraints()) ;

		/*
		 * keep only the implems that have at least an instance matching the instance constraints.
		 */
//		if (!dep.getInstanceConstraints().isEmpty()) {
//			Set<Implementation> validImpls = new HashSet <Implementation> () ;
//			//TODO WRONG implem, we should call resolveImple, but we do not hte the composite instance !!
//			for (Implementation impl : impls) {
//				//	if (resolveImpls(composite, impl, dep) != null) {
//				if (impl.getInsts() != null) {
//					for (Instance inst : impl.getInsts()) {
//						if (inst.match(instConstraints)) {
//							validImpls.add (impl) ;
//							break ;
//						}
//					}
//				}
//			}
//			if (!validImpls.isEmpty())
//				impls = validImpls ;
//		}

		// and then the prefered ones.
		List<Filter> preferences = Util.toFilterList(dep.getImplementationPreferences()) ;
		return spec.getPreferedComponent(impls, preferences, instConstraints);
	}


	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// do not care
	}


	@Override
	public ComponentBundle findBundle(CompositeType compoType,
			String bundleSymbolicName, String componentName) {
		return null;
	}

	@Override
	public Implementation findImplByDependency(CompositeType compoType,
			DependencyDeclaration dependency) {
		return findImplByName(compoType, dependency.getTarget().getName());
	}


}