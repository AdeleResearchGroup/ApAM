package fr.imag.adele.apam.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.Select;
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
	public void getSelectionPath(Instance client, DependencyDeclaration dep, List<DependencyManager> selPath) {
	}

	@Override
	public Instance resolveImpl(Instance client, Implementation impl, DependencyDeclaration dep) {
		//		Set<Filter> constraints = Util.toFilter(dep.getInstanceConstraints()) ;
		List<Filter> preferences = Util.toFilterList(dep.getInstancePreferences()) ;
		return Select.getPrefered(resolveImpls(client, impl, dep), preferences) ;
	}

	//		if ((constraints.isEmpty()) && (preferences.isEmpty())) { 
	//			for (Instance inst : impl.getInsts()) {
	//				if (inst.isSharable() && Util.checkInstVisible(client.getComposite(), inst))
	//					return inst;
	//			}
	//			return null ;
	//		}
	//
	//		Set<Instance> insts = new HashSet<Instance>();
	//		for (Instance inst : impl.getInsts()) {
	//			if (inst.isSharable() && inst.match(constraints) && Util.checkInstVisible(client.getComposite(), inst))
	//				insts.add(inst);
	//		}
	//		if (!insts.isEmpty())
	//			return Select.getPrefered(insts, preferences);
	//		return null;
	//	}

	@Override
	public Set<Instance> resolveImpls(Instance client, Implementation impl, DependencyDeclaration dep) {

		Set<Filter> constraints = Util.toFilter(dep.getInstanceConstraints()) ;	
		Set<Instance> insts = new HashSet<Instance>();
		for (Instance inst : impl.getInsts()) {
			if (inst.isSharable() && inst.match(constraints) && Util.checkInstVisible(client.getComposite(), inst))
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
	public Instance findInstByName(Instance client, String instName) {
		if (instName == null) return null;
		Instance inst = CST.componentBroker.getInst(instName);
		if (inst == null) return null;
		if (Util.checkInstVisible(client.getComposite(), inst)) {
			return inst;
		}
		return null;
	}

	@Override
	public Implementation findImplByName(Instance client, String implName) {
		if (implName == null)
			return null;
		Implementation impl = CST.componentBroker.getImpl(implName);
		if (impl == null)
			return null;
		if (Util.checkImplVisible(client.getComposite().getCompType(), impl)) {
			return impl;
		}
		return null;
	}

	@Override
	public Specification findSpecByName(Instance client, String specName) {
		if (specName == null)
			return null;
		return CST.componentBroker.getSpec(specName);
	}

	@Override
	public Component findComponentByName(Instance client, String componentName) {
		Component ret = CST.componentBroker.getComponent (componentName) ;
		if (ret == null) return null ;
		if (ret instanceof Specification) return ret ;
		if (ret instanceof Implementation) {
			if (Util.checkImplVisible(client.getComposite().getCompType(), (Implementation)ret)) 
				return ret ;
			return null ;
		}
		//It is an instance
		if (Util.checkInstVisible(client.getComposite(), (Instance)ret)) 
			return ret ;
		return null ;
	}

	/**
	 * dep can be a specification, an implementation or a resource: interface or message.
	 * We have to find out all the implementations and all the instances that can be a target for that dependency 
	 * and satisfy visibility and the constraints,
	 * 
	 * First compute all the implementations, visible or not that is a good target; 
	 * then add in insts all the instances of these implementations that satisfy the constraints and are visible.
	 * 
	 * If parameter insts is null, do not take care of the instances.
	 * 
	 * Then remove the implementations that are not visible  .
	 * 
	 */
	@Override
	@SuppressWarnings("unchecked") 
	public Set<Implementation> resolveDependency(Instance client, DependencyDeclaration dep, Set<Instance> insts) {
		Set<Filter> constraints = Util.toFilter(dep.getImplementationConstraints()) ;
		Set<Implementation> impls = null ;
		String name = dep.getTarget().getName() ;

		if (dep.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.componentBroker.getSpec(dep.getTarget().getName());
			if (spec == null) {
				System.err.println("No spec for " + dep.getTarget().getName()); 
				return null;
			}
			impls = spec.getImpls();
		} else 	{
			impls = new HashSet<Implementation> () ;
			if (dep.getTarget() instanceof ResourceReference) {
				for (Implementation impl : CST.componentBroker.getImpls()) {
					for (ResourceReference ref : impl.getAllProvidedResources())  {
						if (ref.equals(dep.getTarget())) 
							impls.add(impl) ;
					}
				}
			} else 
				if (dep.getTarget() instanceof ImplementationReference) {
					Implementation impl = CST.componentBroker.getImpl(name);
					if (impl != null) 
						impls.add(impl) ;
				}
		}

		/*
		 * We have in impls all the implementations satisfying the dependency target (type and name only).
		 * Select only those that satisfy the constraints (visible or not)
		 */
		impls = Select.getConstraintsComponents(impls, constraints);
		if (impls == null || impls.isEmpty()) return null ;

		/*
		 * Take all the instances of these implementations satisfying the dependency constraints.
		 */		
		if (insts != null) {
			Set<Instance> validInsts ;
			constraints = Util.toFilter(dep.getInstanceConstraints()) ;
			//Compute all the instances visible and satisfying the constraints  ;
			for (Implementation impl : impls) {
				validInsts = (Set<Instance>)Select.getConstraintsComponents(impl.getMembers(), constraints) ;
				if (validInsts != null && !validInsts.isEmpty()) {
					validInsts = Util.getVisibleInsts(client, validInsts) ;
					if (validInsts != null && !validInsts.isEmpty())
						insts.addAll(validInsts) ;
				}
			}
		}
		
		// returns only those implems that are visible
		impls = Util.getVisibleImpls(client, impls) ;
		if (dep.isMultiple()) return impls ;
		
		/*
		 * If dependency is not multiple, select the best instance and implem.
		 * Return a single element in both impls and insts
		 */
		if (insts != null && !insts.isEmpty()) {
			//logger.debug("Candidates : " + insts) ;
			Instance inst = Select.selectBestInstance (impls, insts, dep) ;
			insts.clear();
			insts.add(inst) ;
			return Collections.singleton(inst.getImpl()) ;
		} 
		List<Filter> implPreferences = Util.toFilterList(dep.getImplementationPreferences()) ;
			return Collections.singleton(Select.getPrefered(impls, implPreferences)) ;
	}

//	@Override
//	public Implementation resolveSpec(Instance client, DependencyDeclaration dep) {
//		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
//		if (spec == null)
//			return null;	
//
//		//List of visible implementations that satisfy the constraints.
//		Set<Implementation> candidates = resolveDependency (client, dep, null) ;
//		if (candidates == null || candidates.isEmpty()) 
//			return null ;	
//		if (candidates.size() == 1) 
//			return candidates.iterator().next() ;
//
//		List<Filter> instPreferences = Util.toFilterList(dep.getInstancePreferences()) ;
//		Set<Filter> instConstraints  = Util.toFilter(dep.getInstanceConstraints()) ;
//		return Select.getPreferedComponentFromMembers(candidates, instPreferences, instConstraints) ;
//	}


	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// do not care
	}


	@Override
	public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName) {
		return null;
	}

//	@Override
//	public Implementation findImplByDependency(Instance client,	DependencyDeclaration dependency) {
//		return findImplByName(client, dependency.getTarget().getName());
//	}


}