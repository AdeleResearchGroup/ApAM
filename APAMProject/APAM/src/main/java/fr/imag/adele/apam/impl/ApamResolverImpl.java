package fr.imag.adele.apam.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.ContextualResolutionPolicy;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyPromotion;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;
//import fr.imag.adele.obrMan.internal.OBRManager;
//import fr.imag.adele.obrMan.internal.OBRManager.Selected;

public class ApamResolverImpl implements ApamResolver {

	private APAMImpl apam;

	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	public ApamResolverImpl(APAMImpl theApam) {
		this.apam = theApam;
	}


	/**
	 * If a client dependency matches a composite dependency, this object contains the information needed for a promotion
	 * 	
	 * @author Jacky
	 *
	 */
	private class DepMult {
		//Name (Id) of the dependency in the composite
		private String        depType = null;

		//set of instances in the composite (if any) associated with the composite dependency
		private Set<Instance> insts   = null;

		public DepMult(Instance client, DependencyDeclaration dep) {
			depType = dep.getIdentifier();
			this.insts = client.getComposite().getWireDests(dep.getIdentifier());
		}

		public Set<Instance> getInsts () {
			return insts ;
		}

	}

	/**
	 * Provided a composite (compoInst), checks if one of its dependency declaration
	 * matches the compoClient dependency declaration.
	 * If so, it will be a promotion, it returns a DepMult object.
	 * 
	 * Stop at the first dependency matching only based on same name (same resource or same component).
	 * If client cardinality is multiple, compo cardinallity must be multiple too.
	 * No provision for the client constraints or characteristics (missing, eager)
	 * 
	 * @param compoInst the composite instance containing the client
	 * @param compoDeps the dependencies of the composite
	 * @param clientDep the client dependency we are trying to resolve
	 * @return
	 */
	private DepMult matchDependency(Instance compoInst, DependencyDeclaration compoDep, DependencyDeclaration clientDep) {
		boolean multiple = clientDep.isMultiple();
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account
		//		for (DependencyDeclaration compoDep : compoDeps) {
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())) { // same nature
			if (compoDep.getTarget().equals(clientDep.getTarget())) {
				if (!multiple || compoDep.isMultiple())
					return new DepMult(compoInst, compoDep);
			}
		}
		//		}

		//Look for a compatible dependency.
		//Stop at the first dependency matching only based on same name (same resource or same component)
		//No provision for : cardinality, constraints or characteristics (missing, eager)
		//		for (DependencyDeclaration compoDep : compoDeps) {
		//Look if the client requires one of the resources provided by the specification
		if (compoDep.getTarget() instanceof SpecificationReference) {
			Specification spec = findSpecByName(compoInst.getComposite().getCompType(),
					((SpecificationReference) compoDep.getTarget()).getName());
			if ((spec != null) && spec.getDeclaration().getProvidedResources().contains(clientDep.getTarget()))
				if (!multiple || compoDep.isMultiple())
					return new DepMult(compoInst, compoDep);
		} else {
			//If the composite has a dependency toward an implementation
			//and the client requires a resource provided by that implementation
			if (compoDep.getTarget() instanceof ImplementationReference) {
				String implName = ((ImplementationReference<?>) compoDep.getTarget()).getName();
				Implementation impl = findImplByName(compoInst.getComposite().getCompType(), implName);
				if (impl != null) {
					//The client requires the specification implemented by that implementation
					if (clientDep.getTarget() instanceof SpecificationReference) {
						String clientReqSpec = ((SpecificationReference) clientDep.getTarget()).getName();
						if (impl.getImplDeclaration().getSpecification().getName().equals(clientReqSpec))
							if (!multiple || compoDep.isMultiple())
								return new DepMult(compoInst, compoDep);
					} else {
						//The client requires a resource provided by that implementation
						if (impl.getImplDeclaration().getProvidedResources().contains(clientDep.getTarget()))
							if (!multiple || compoDep.isMultiple())
								return new DepMult(compoInst, compoDep);
					}
				}
			}
		}
		//		}
		return null;
	}


	/**
	 * Compares the client dependency wrt the enclosing composite dependencies.
	 * If it matches one composite dependency, it is a promotion.
	 * Must use the composite dependency, but WARNING it may not fit the client's requirements
	 * if client's constraints does not match the composite selection.
	 * The client becomes the embedding composite; visibility and scope become the one of the embedding composite
	 * 
	 * @param client
	 * @param dependency definition
	 * @return the composite dependency from the composite.
	 */
	private  DepMult getPromotion(Instance client, DependencyDeclaration dependency) {

		DepMult promotion = null;
		if (client.getComposite().getDeclaration() == null)
			return null;

		// take the dependency first in the instance, and if not found, in the implementation
		Set<DependencyDeclaration> compoDeps = computeAllDependencies(client.getComposite()) ;
		if (compoDeps.isEmpty()) return null ;

		//look if a promotion is explicitly declared for that client component
		// <promotion implementation="A" dependency="clientDep" to="compoDep" />
		// <promotion specification="SA" dependency="clientDep" to="compoDep" />		
		for (DependencyPromotion promo: client.getComposite().getCompType().getCompoDeclaration().getPromotions()) {			
			if (! promo.getContentDependency().getIdentifier().equals(dependency.getIdentifier()))
				continue;		// this promotion is not about our dependency  (not "clientDep")
			String sourceName = promo.getContentDependency().getDeclaringComponent().getName();
			//sourceName = "SA" or "A"
			if (sourceName.equals(client.getImpl().getName()) || sourceName.equals(client.getSpec().getName())) {	
				// We found the right promotion from client side. 
				//Look for the corresponding composite dependency "compoDep"
				String toName =  promo.getCompositeDependency().getIdentifier();
				for (DependencyDeclaration compoDep : compoDeps) {
					if (compoDep.getIdentifier().equals(toName)) {
						//We found the composite side. It is an explicit promotion. It should match.
						promotion = matchDependency (client, compoDep, dependency) ;
						if (promotion == null) {
							logger.error ("Promotion is invalid. Dependency " + promo.getContentDependency().getIdentifier()
									+ " of component " + sourceName + " does not match the composite dependency " + compoDep) ;
							return null ;
						}	
						return promotion ;
					}
				}
			}
		}

		for (DependencyDeclaration compoDep : compoDeps) {
			promotion = matchDependency(client, compoDep, dependency) ;
			if (promotion != null) break ; ;
		}

		return promotion ;
	}

	// if the instance is unused, it will become the main instance of a new composite.
	private Composite getClientComposite(Instance mainInst) {

		if (mainInst.isUsed())
			return mainInst.getComposite();

		/*
		 * We are resolving a reference from an unused client instance. We automatically build a new composite
		 * to create a context of execution. This allow to use Apam without requiring the explicit definition of
		 * composites, just instantiating any implementation.
		 */

		Implementation mainComponent			= mainInst.getImpl();
		String applicationName 					= mainComponent.getName() + "_Appli";
		SpecificationReference specification	= mainComponent.getImplDeclaration().getSpecification();
		Set<ManagerModel> models				= new HashSet<ManagerModel>();

		CompositeType application = apam.createCompositeType((CompositeType)null,
				applicationName, specification != null ? specification.getName() : null, mainComponent.getName(),
						models, null);

		/*
		 * Create an instance of the application with the specified main
		 */
		Map<String, String> initialProperties = new HashMap<String, String>();
		initialProperties.put(CST.APAM_MAIN_INSTANCE, mainInst.getName()) ;
		return (Composite)application.createInstance(null, initialProperties);
	}

	/**
	 * An APAM client instance requires to be wired with one or all the instances that satisfy the dependency.
	 * WARNING : in case of interface or message dependency , since more than one specification can implement the same
	 * interface, any specification implementing at least the provided interface (technical name of the interface) will
	 * be considered satisfactory.
	 * If found, the instance(s) are bound is returned.
	 * 
	 * @param client the instance that requires the specification
	 * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
	 * @return
	 */
	@Override
	public boolean resolveWire(Instance client, String depName) {
		logger.info("Resolving dependency " + depName + " from instance " + client.getName() );
		if ((depName == null) || (client == null)) {
			logger.error("missing client or dependency name");
			return false;
		}

		//compute the set of constraints that apply to that resolution: inst + impl + spec + composite generique
		DependencyDeclaration dependency = computeEffectiveDependency (client, depName) ;

		if (dependency == null) {
			logger.error("dependency declaration not found " + depName);
			return false;
		}

		// Promotion control
		Composite compo = getClientComposite(client);
		// if it is a promotion, visibility and scope is the one of the embedding composite.
		DepMult depMult = getPromotion(client, dependency);

		Set<Instance> insts = null ;
		Implementation impl = null;
		Set<Implementation> impls = null;

		if (depMult != null) { // it is a promotion
			compo = compo.getComposite();
			if ((depMult.getInsts() != null) && !depMult.getInsts().isEmpty()) {
				insts = depMult.getInsts();
				impl = ((Instance)insts.toArray()[0]).getImpl ();			
				logger.info("Selected from promotion " + insts);
			}
		}


		// normal case. Try to find the instances.
		if (insts == null) {
			// Look for the implementation(s)
			CompositeType compoType = compo.getCompType();
			if (impl == null) {
				if (dependency.getTarget() instanceof ImplementationReference) {
					String implName = ((ImplementationReference<?>) dependency.getTarget()).getName();
					impl = CST.apamResolver.findImplByName(compoType, implName);
				} else {
					if (dependency.isMultiple()) {
						impls = this.resolveSpecByResources(compoType, dependency);
					} else {
						impl = this.resolveSpecByResource(compoType, dependency);
					}
				}
			}
			if (impl == null && impls == null) {
				logger.error("Failed to resolve " + dependency.getTarget()
						+ " from " + client + "(" + depName + ")");
				return false;
			}

			// Look for the instances
			insts = new HashSet <Instance> () ;
			if (dependency.isMultiple()) {
				for (Implementation imp : impls) {
					impl = imp ;
					insts.addAll(this.resolveImpls(compo, imp, dependency));
				}
				logger.info("Selected set " + insts);
			} else {
				Instance inst = this.resolveImpl(compo, impl, dependency);
				if (inst != null) {
					insts.add(inst);
					logger.info("Selected " + inst);
				}
			}

			//No existing instance. Create one.
			if (insts.isEmpty()) {
				Instance inst = impl.createInstance(compo, null);

				if (inst == null){// may happen if impl is non instantiable
					logger.error("Failed creating instance of " + impl );
					return false;
				}

				insts.add(inst);
				logger.info("Instantiated " + insts.toArray()[0]);
			}
		}

		if ((insts == null) || insts.isEmpty()) return false ;

		// We got the instances. Create the wires.
		//the set is a singleton if multiple = false.
		boolean ok = false ;
		for (Instance inst : insts) {
			// For promotions we must wire the composite and wire the client if the target matches the client constraints
			if (depMult != null) { // it was a promotion, embedding composite must be linked as the source
				client.getComposite().createWire(inst, depMult.depType);
				logger.info("Promoting " + client + " -" + depName + "-> " + inst + "\n      as: "
						+ client.getComposite() + " -" + depMult.depType + "-> " + inst);
				// wire the client only if the composite target matches the client constraints
				if (inst.getImpl().match(Util.toFilter(dependency.getImplementationConstraints())) 
						&& inst.match(Util.toFilter(dependency.getInstanceConstraints()))) {
					client.createWire(inst, depName);
					ok = true ;
				}
			}

			//Not a promotion. Normal case: we wire the client toward all the instances.
			else 	{
				client.createWire(inst, depName);
				ok = true ;
			}
		} 

		// notify the managers
		ApamResolverImpl.notifySelection(client, dependency.getTarget(), depName,
				((Instance) insts.toArray()[0]).getImpl(), null, insts);
		return ok;
	}

	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance.
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * For each dependency, we clone it, and we aggregate the constraints as found at all level, 
	 * including the generic ones found in the composite type.
	*/
	public Set<DependencyDeclaration> computeAllEffectiveDependency (Instance client) {
		if (client == null) return null ;
		Set<DependencyDeclaration> allDeps = new HashSet <DependencyDeclaration> ();
		for (DependencyDeclaration dep : computeAllDependencies (client)) {
			allDeps.add(computeEffectiveDependency(client, dep.getIdentifier())) ;
		}
		return allDeps ;
	}
	
	/**
	 * Provided an instance, computes all the dependency declaration that applies to that instance/
	 * If can be defined on the instance, the implementation, the specification, or on both.
	 * In case the same dependency is defined multiple time, it is the most concrete one that must be taken into account.
	 * There is no attempt to compute all the constraints that apply on a given dependency; 
	 * We are only interested in the target.
	 * @param client
	 * @return
	 */
	public Set<DependencyDeclaration> computeAllDependencies (Instance client) {
		Set<DependencyDeclaration> allDeps = new HashSet<DependencyDeclaration> () ;
		allDeps.addAll(client.getDeclaration().getDependencies());

		boolean found ;
		for (DependencyDeclaration dep : client.getImpl().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		for (DependencyDeclaration dep : client.getSpec().getDeclaration().getDependencies()) {
			found= false ;
			for (DependencyDeclaration allDep : allDeps) {
				if (allDep.getIdentifier().equals(dep.getIdentifier())) {
					found= true;
					break ;
				}
			}
			if (!found) allDeps.add(dep) ;
		}
		return allDeps ;
	}

	/**
	 * The dependencies "depName" that apply on a client are those of the instance, plus those of the implem, 
	 * plus those of the spec, and finally those in the composite. 
	 * We aggregate the constraints as found at all level, including the generic one found in the composite type.
	 * 
	 * @param client
	 * @param dependency
	 * @return
	 */
	public DependencyDeclaration computeEffectiveDependency (Instance client, String depName) {

		//Find the first dependency declaration.
		Component depComponent = client ;
		//take the declaration declared at the most concrete level
		DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName);
		if (dependency == null) {
			dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);
			depComponent = client.getImpl() ;
		}
		//the dependency can be defined at spec level if implem is a composite
		if (dependency == null) {
			dependency = client.getSpec().getApformSpec().getDeclaration().getDependency(depName);
			depComponent =client.getSpec();
		}

		if (dependency == null) return null ;

		//Now compute the inheritance instance, Implem, Spec.
		dependency = dependency.clone();
		Component group = depComponent.getGroup() ;

		while (group != null) {
			for (DependencyDeclaration dep : group.getDeclaration().getDependencies()) {
				if (dep.getIdentifier().equals(dependency.getIdentifier())) {
					dependency.getImplementationConstraints().addAll(dep.getImplementationConstraints()) ;
					dependency.getInstanceConstraints().addAll(dep.getInstanceConstraints()) ;
					dependency.getImplementationPreferences().addAll(dep.getImplementationPreferences()) ;
					dependency.getInstancePreferences().addAll(dep.getInstancePreferences()) ;
					break ;
				}
			}
			group = group.getGroup() ;
		}

		//Add the composite generic constraints
		CompositeType compoType = client.getComposite().getCompType() ;
		Map<String, String> validAttrs = client.getValidAttributes() ;
		for ( ContextualResolutionPolicy  pol  : compoType.getCompoDeclaration().getContextualResolutionPolicies()) {

			if (matchGenericDependency(client, pol, dependency)) {
				if (Util.checkFilters(pol.getImplementationConstraints(), null, validAttrs, client.getName())) {
					dependency.getImplementationConstraints().addAll(pol.getImplementationConstraints()) ;
				}
				if (Util.checkFilters(pol.getInstanceConstraints(), null, validAttrs, client.getName())) {
					dependency.getInstanceConstraints().addAll(pol.getInstanceConstraints()) ;
				}
				if (Util.checkFilters(null, pol.getImplementationPreferences(), validAttrs, client.getName())) {
					dependency.getImplementationPreferences().addAll(pol.getImplementationPreferences()) ;
				}
				if (Util.checkFilters(null, pol.getInstancePreferences(), validAttrs, client.getName())) {
					dependency.getInstancePreferences().addAll(pol.getInstancePreferences()) ;
				}
			}
		}
		return dependency ;
	}

	/**
	 * Provided a composite (compoInst), checks if the provided generic dependency constraint declaration
	 * matches the compoClient dependency declaration.
	 * 
	 * @param compoInst the composite instance containing the client
	 * @param genericDeps the dependencies of the composite: a regExpression
	 * @param clientDep the client dependency we are trying to resolve.
	 * @return
	 */
	private boolean matchGenericDependency(Instance compoInst, ContextualResolutionPolicy compoDep, DependencyDeclaration clientDep) {

		String pattern = compoDep.getTarget().getName() ;
		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account
		if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())) { // same nature
			if (clientDep.getTarget().getName().matches(pattern)) {
				return true;
			}
		}

		//If the client dep is an implementation dependency, check if the specification matches the pattern
		if (compoDep.getTarget() instanceof SpecificationReference) {
			if (clientDep.getTarget() instanceof ImplementationReference) {
				String implName = ((ImplementationReference<?>) clientDep.getTarget()).getName();
				Implementation impl = findImplByName(compoInst.getComposite().getCompType(), implName);
				if (impl != null && impl.getSpec().getName().matches(pattern)) {
					return true ;
				}
			}
		}

		return false;
	}


	/**
	 * Before to resolve an implementation (i.e. to select one of its instance), this method is called to
	 * know which managers are involved, and what are the constraints and preferences set by the managers to this
	 * resolution.
	 * 
	 * @param compTypeFrom : the origin of this resolution.
	 * @param impl : the implementation to resolve.
	 * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
	 * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
	 * @return : the managers that will be called for that resolution.
	 */
	private List<DependencyManager> computeSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dependency) {

		List<DependencyManager> selectionPath = new ArrayList<DependencyManager>();
		for (DependencyManager dependencyManager : ApamManagers.getManagers()) {
			/*
			 * Skip apamman
			 */
			if (dependencyManager.getName().equals(CST.APAMMAN) || dependencyManager.getName().equals(CST.APAMMAN))
				continue;
			dependencyManager.getSelectionPath(compTypeFrom, dependency,selectionPath);
		}

		// To select first in Apam
		selectionPath.add(0, apam.getApamMan());
		selectionPath.add(0, apam.getUpdateMan());
		return selectionPath;
	}

	/**
	 * Impl has been deployed, it becomes embedded in compoType.
	 * If physically deployed, it is in the Unused list. remove.
	 * 
	 * @param compoType
	 * @param impl
	 */
	public static void deployedImpl(CompositeType compoType, Implementation impl, boolean deployed) {
		// it was not deployed
		if (!deployed && impl.isUsed()) {
			logger.info(" : selected " + impl);
			return;
		}

		// it is deployed or was never used so far
		if (impl.isUsed()) {
			logger.info(" : logically deployed " + impl);
		} else {// it was unused so far.
			((ComponentImpl)impl).setFirstDeployed(compoType);
			logger.info(" : deployed " + impl);
		}
		((CompositeTypeImpl)compoType).deploy(impl);
	}

	/**
	 * Look for an implementation with a given name "implName", visible from composite Type compoType.
	 * 
	 * @param compoType
	 * @param componentName
	 * @return
	 */

	private <C extends Component> C findByName (CompositeType compoType, String componentName, Class<C> kind, Composite composite) {
		if (componentName == null) return null;

		if (compoType == null)
			compoType = CompositeTypeImpl.getRootCompositeType();
		DependencyDeclaration dep = new DependencyDeclaration (compoType.getImplDeclaration().getReference(), 
				componentName, false, new ComponentReference<ComponentDeclaration>(componentName)) ; //new ImplementationReference<ImplementationDeclaration>(componentName));

		List<DependencyManager> selectionPath = computeSelectionPath(compoType, dep);

		Component compo = null;
		logger.info("Looking for component " + componentName + ": ");
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN) && !manager.getName().equals(CST.UPDATEMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			if (kind == Instance.class)
				compo = manager.findInstByName(composite, componentName);
			else
				compo = manager.findComponentByName(compoType, componentName);

			if (compo != null) { //We found it, but maybe the wrong type
				if (! kind.isAssignableFrom(compo.getClass())) {
					logger.error ("Component " + componentName + " found by not a " + kind.getName()) ;
					return null ;
				}

				if (compo instanceof Implementation) {
					deployedImpl(compoType, (Implementation)compo, deployed);
				}
				return (C)compo;
			}
		}
		return null;
	}


	@Override
	public fr.imag.adele.apam.Component findComponentByName(CompositeType compoType, String componentName) {
		return findByName (compoType, componentName, fr.imag.adele.apam.Component.class, null) ;
	}

	@Override
	public Specification findSpecByName(CompositeType compoType, String specName) {
		return findByName (compoType, specName, fr.imag.adele.apam.Specification.class, null) ;
	}

	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		return findByName (compoType, implName, fr.imag.adele.apam.Implementation.class, null) ;
	}

	public Instance findInstByName(Composite compo, String instName) {
		CompositeType compoType = null ;
		if (compo == null) {
			compo = CompositeImpl.getRootAllComposites() ;
			compoType = CompositeTypeImpl.getRootCompositeType() ;
		}
		else compoType = compo.getCompType() ;
		return findByName (compoType, instName, fr.imag.adele.apam.Instance.class, compo) ;
	}


	//	@Override
	//	public Component findComponentByName(CompositeType compoTypeFrom, String componentName) {
	//		if (compoTypeFrom == null)
	//			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
	//		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getImplDeclaration().getReference(), 
	//				componentName, false, new ImplementationReference<ImplementationDeclaration>(componentName));
	//
	//		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dep);
	//
	//		Component compo = null;
	//		logger.error("Looking for component " + componentName + ": ");
	//		boolean deployed = false;
	//		for (DependencyManager manager : selectionPath) {
	//			if (!(manager.getName().equals(CST.APAMMAN) || manager.getName().equals(CST.UPDATEMAN)))
	//				deployed = true;
	//			logger.debug(manager.getName() + "  ");
	//			compo = manager.findComponentByName(compoTypeFrom, componentName);
	//
	//			if (compo != null) {
	//				if (compo instanceof Implementation) {
	//					deployedImpl(compoTypeFrom, (Implementation)compo, deployed);
	//				}
	//				return compo;
	//			}
	//		}
	//		return null;
	//	}



	//	@Override
	//	public Implementation findImplByName(CompositeType compoTypeFrom, String implName) {
	//		if (compoTypeFrom == null)
	//			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
	//		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getImplDeclaration().getReference(), 
	//				implName, false, new ImplementationReference<ImplementationDeclaration>(implName));
	//
	//		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dep);
	//
	//		Implementation impl = null;
	//		logger.error("Looking for implementation " + implName + ": ");
	//		boolean deployed = false;
	//		for (DependencyManager manager : selectionPath) {
	//			if (!manager.getName().equals(CST.APAMMAN))
	//				deployed = true;
	//			logger.debug(manager.getName() + "  ");
	//			impl = manager.findImplByName(compoTypeFrom, implName);
	//
	//			if (impl != null) {
	//				deployedImpl(compoTypeFrom, impl, deployed);
	//				return impl;
	//			}
	//		}
	//		return null;
	//	}

	/**
	 * Look for an implementation with a given name "implName", visible from composite Type compoType.
	 * 
	 * @param compoType
	 * @param specName
	 * @return
	 */
	//	@Override
	//	public Specification findSpecByName(CompositeType compoTypeFrom, String specName) {
	//
	//		if (compoTypeFrom == null)
	//			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
	//		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getImplDeclaration().getReference(), 
	//				specName, false, new SpecificationReference(specName));
	//
	//		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dep);
	//
	//		Specification spec = null;
	//		logger.error("Looking for specification " + specName + ": ");
	//		boolean deployed = false;
	//		for (DependencyManager manager : selectionPath) {
	//			if (!manager.getName().equals(CST.APAMMAN))
	//				deployed = true;
	//			logger.debug(manager.getName() + "  ");
	//			spec = manager.findSpecByName(compoTypeFrom, specName);
	//			if (spec != null) {
	//				if (deployed) {
	//					logger.error("Deployed specificaiton " + specName);
	//				} else
	//					logger.error("Selected specificaiton " + specName);
	//
	//				return spec;
	//			}
	//		}
	//		logger.error("Could not find specification " + specName);
	//		return null;
	//	}

	/**
	 * First looks for the specification defined by its name, and then resolve that specification.
	 * Returns the implementation that implement the specification and that satisfies the constraints.
	 * 
	 * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
	 * @param specName
	 * @param constraints. The constraints to satisfy. They must be all satisfied.
	 * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
	 *            maximum
	 *            number of preferences, taken in the order, and stopping at the first failure.
	 * @return
	 */
	@Override
	public Implementation resolveSpecByName(CompositeType compoTypeFrom, String specName,
			Set<String> constraints, List<String> preferences) {
		if (constraints == null)
			constraints = new HashSet<String>();
		if (preferences == null)
			preferences = new ArrayList<String>();
		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();

		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getImplDeclaration().getReference(), specName, false, new SpecificationReference(specName));
		dep.getImplementationConstraints().addAll(constraints) ;
		dep.getImplementationPreferences().addAll(preferences) ;

		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dep);

		if (constraints.isEmpty() && preferences.isEmpty())
			logger.info("Looking a \"" + specName + "\" implementation.");
		else
			logger.info("Looking a \"" + specName + "\" implementation. Constraints:" + constraints
					+ ". Preferences: " + preferences);
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			Implementation impl = manager.resolveSpec(compoTypeFrom, dep) ;
			//			Implementation impl = manager.resolveSpecByResource(compoTypeFrom, new SpecificationReference(specName),
			//					constraints, preferences);

			if (impl != null) {
				deployedImpl(compoTypeFrom, impl, deployed);
				return impl;
			}
		}
		return null;
	}

	/**
	 * First looks for the specification defined by its interface, and then resolve that specification.
	 * Returns the implementation that implement the specification and that satisfies the constraints.
	 * 
	 * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
	 * @param interfaceName. The full name of one of the interfaces of the specification.
	 *            WARNING : different specifications may share the same interface.
	 * @param interfaces. The complete list of interface of the specification. At most one specification can be
	 *            selected.
	 * @param constraints. The constraints to satisfy. They must be all satisfied.
	 * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
	 *            maximum
	 *            number of preferences, taken in the order, and stopping at the first failure.
	 * @return
	 */
	public Implementation resolveSpecByResource(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dependency);

		logger.info("Looking for an implem with" + dependency);
		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
		Implementation impl = null;
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			impl = manager.resolveSpec(compoTypeFrom, dependency);
			if (impl != null) {
				deployedImpl(compoTypeFrom, impl, deployed);
				return impl;
			}
		}
		return null;
	}

	public Set<Implementation> resolveSpecByResources(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
		List<DependencyManager> selectionPath = computeSelectionPath(compoTypeFrom, dependency);

		logger.info("Looking for all implems with" + dependency);
		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
		Set<Implementation> impls = null;
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			impls = manager.resolveSpecs(compoTypeFrom, dependency);
			if (impls != null && !impls.isEmpty()) {
				for (Implementation impl : impls) {
					deployedImpl(compoTypeFrom, impl, deployed);
				}
				return impls ;
			}
		}
		return null;
	}

	/**
	 * Look for an instance of "impl" that satisfies the constraints. That instance must be either
	 * - shared and visible from "compo", or
	 * - instantiated if impl is visible from the composite type.
	 * 
	 * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
	 *            visible.
	 * @param impl
	 * @param constraints. The constraints to satisfy. They must be all satisfied.
	 * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
	 *            maximum
	 * @return
	 */
	public Instance resolveImpl(Composite compo, Implementation impl, DependencyDeclaration dependency) {
		if (compo == null)
			compo = CompositeImpl.getRootAllComposites();		

		List<DependencyManager> selectionPath = computeSelectionPath(compo.getCompType(), dependency);

		Instance inst = null;
		logger.info("Looking for an instance of " + impl + ": ");
		for (DependencyManager manager : selectionPath) {
			logger.debug(manager.getName() + "  ");
			inst = manager.resolveImpl(compo, impl, dependency);
			if (inst != null) {
				return inst;
			}
		}
		// TODO Notify dynaman ?

		return null;
	}

	/**
	 * Look for all the existing instance of "impl" that satisfy the constraints.
	 * These instances must be either shared and visible from "compo".
	 * If no existing instance can be found, one is created if impl is visible from the composite type.
	 * 
	 * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
	 *            visible.
	 * @param impl
	 * @param constraints. The constraints to satisfy. They must be all satisfied.
	 * @return
	 */
	public Set<Instance> resolveImpls(Composite compo, Implementation impl, DependencyDeclaration dependency) {
		if (compo == null)
			compo = CompositeImpl.getRootAllComposites();

		List<DependencyManager> selectionPath = computeSelectionPath(compo.getCompType(), dependency);

		Set<Instance> insts = null;
		logger.info("Looking for an instance of " + impl + ": ");
		for (DependencyManager manager : selectionPath) {
			logger.debug(manager.getName() + "  ");
			insts = manager.resolveImpls(compo, impl, dependency);
			if ((insts != null) && !insts.isEmpty()) {
				logger.debug("selected " + insts);
				return insts;
			}
		}
		// TODO Notify dynaman ?
		return Collections.emptySet();

	}

	/**
	 * Once the resolution terminated, either successful or not, the managers are notified of the current
	 * selection.
	 * Currently, the managers cannot "undo" nor change the current selection.
	 * 
	 * @param spec
	 * @param impl
	 * @param inst
	 * @param insts
	 */
	private static void notifySelection(Instance client, ResolvableReference resName, String depName,
			Implementation impl, Instance inst, Set<Instance> insts) {
		for (DependencyManager dependencyManager : ApamManagers.getManagers()) {
			dependencyManager.notifySelection(client, resName, depName, impl, inst, insts);
		}
	}


	@Override
	public Implementation resolveSpecByInterface(CompositeType compoTypeFrom,
			String interfaceName, Set<String> constraints,
			List<String> preferences) {
		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getDeclaration().getReference(),
				interfaceName, false, new InterfaceReference(interfaceName));

		dep.getImplementationConstraints().addAll(constraints) ;
		dep.getImplementationPreferences().addAll(preferences) ;
		return resolveSpecByResource(compoTypeFrom, dep);
	}

	@Override
	public Implementation resolveSpecByMessage(CompositeType compoTypeFrom,
			String messageName, Set<String> constraints, List<String> preferences) {

		DependencyDeclaration dep = new DependencyDeclaration (compoTypeFrom.getDeclaration().getReference(),
				messageName, false, new MessageReference(messageName));

		dep.getImplementationConstraints().addAll(constraints) ;
		dep.getImplementationPreferences().addAll(preferences) ;
		return resolveSpecByResource(compoTypeFrom, dep);
	}

	@Override
	public Instance resolveImpl(Composite compo, Implementation impl,
			Set<String> constraints, List<String> preferences) {

		DependencyDeclaration dep = new DependencyDeclaration (compo.getDeclaration().getReference(),
				impl.getName(), false, new ImplementationReference<ImplementationDeclaration>(impl.getName()));

		dep.getImplementationConstraints().addAll(constraints) ;
		dep.getImplementationPreferences().addAll(preferences) ;

		return resolveImpl(compo, impl, dep);
	}

	@Override
	public Set<Instance> resolveImpls(Composite compo, Implementation impl, Set<String> constraints) {

		DependencyDeclaration dep = new DependencyDeclaration (compo.getDeclaration().getReference(),
				impl.getName(), true, new ImplementationReference<ImplementationDeclaration>(impl.getName()));

		dep.getImplementationConstraints().addAll(constraints) ;

		return resolveImpls(compo, impl, dep);
	}

	@Override
	public void updateComponent(String componentName) {
		Component compo = CST.componentBroker.getComponent(componentName) ;
		if (compo == null) {
			logger.error ("Unknown component " + componentName) ;
			return ;
		}
		UpdateMan.updateComponent(compo) ;
	}


}
