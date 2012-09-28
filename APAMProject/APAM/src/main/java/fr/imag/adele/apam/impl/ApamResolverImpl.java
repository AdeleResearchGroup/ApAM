package fr.imag.adele.apam.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.CosNaming.IstringHelper;
import org.osgi.framework.Filter;
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
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

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
		public String        depType = null;

		//set of instances in the composite (if any) associated with the composite dependency
		public Set<Instance> insts   = null;

		//The Implementations this dependency is revolved to 
		public Implementation impl    = null;

		public DepMult(String dep, Set<Instance> insts, Implementation impl) {
			depType = dep;
			this.insts = insts;
			this.impl = impl;
		}
	}

	/**
	 * Provided a composite (compoInst), checks if one of its dependency declaration
	 * matches the compoClient dependency declaration.
	 * If so, it will be a promotion, it returns a DepMult object.
	 * 
	 * Stop at the first dependency matching only based on same name (same resource or same component)
	 * No provision for : cardinality, constraints or characteristics (missing, eager)
	 * 
	 * @param compoInst the composite instance containing the client
	 * @param compoDeps the dependencies of the composite
	 * @param clientDep the client dependency we are trying to resolve
	 * @return
	 */
	private DepMult
	matchDependency(Instance compoInst, Set<DependencyDeclaration> compoDeps, DependencyDeclaration clientDep) {

		//Look for same dependency: the same specification, the same implementation or same resource name
		//Constraints are not taken into account
		for (DependencyDeclaration compoDep : compoDeps) {
			if (compoDep.getTarget().getClass().equals(clientDep.getTarget().getClass())) { // same nature
				if (compoDep.getTarget().equals(clientDep.getTarget())) {
					return new DepMult(compoDep.getIdentifier(), null, null);
				}
			}
		}

		//Look for a compatible dependency.
		//Stop at the first dependency matching only based on same name (same resource or same component)
		//No provision for : cardinality, constraints or characteristics (missing, eager)
		for (DependencyDeclaration compoDep : compoDeps) {
			//Look if the client requires one of the resources provided by the specification
			if (compoDep.getTarget() instanceof SpecificationReference) {
				Specification spec = findSpecByName(compoInst.getComposite().getCompType(),
						((SpecificationReference) compoDep.getTarget()).getName());
				if ((spec != null) && spec.getDeclaration().getProvidedResources().contains(clientDep.getTarget()))
					return new DepMult(compoDep.getIdentifier(), null, null);
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
								return new DepMult(compoDep.getIdentifier(), null, impl);
						} else {
							//The client requires a resource provided by that implementation
							if (impl.getImplDeclaration().getProvidedResources().contains(clientDep.getTarget()))
								return new DepMult(compoDep.getIdentifier(), null, null);
						}
					}
				}
			}
		}
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
		Set<DependencyDeclaration> compoDeps = client.getComposite().getDeclaration().getDependencies();
		if ((compoDeps == null) || compoDeps.isEmpty())
			compoDeps = client.getComposite().getCompType().getCompoDeclaration().getDependencies();

		promotion = matchDependency (client, compoDeps, dependency) ;
		if (promotion == null)
			return null; // no promotion

		// it is a promotion.
		// compute the instances that must be used for the promotion
		promotion.insts = client.getComposite().getWireDests(promotion.depType); // For composite, the wire name is the

		return promotion;

		// TODO I am not sure at all !
		//        if (!promotion.isMultiple() && (dests != null)) {
		//            logger.error("ERROR : wire " + client.getComposite() + " -" + depId + "-> "
		//                    + " allready existing.");
		//            return null;
		//        }
		// logger.error("Promoting " + client + " : " + client.getComposite() + " -" + depFound.dependencyName
		// + "-> ");
		// return ((ApamResolverImpl) CST.apamResolver).new DepMult(depId, dests);
		// return promotion;
	}

	// if the instance is unused, it will become the main instance of a new composite.
	private Composite getClientComposite(Instance mainInst) {

		if (mainInst.isUsed())
			return mainInst.getComposite();

		/*
		 * We are resolving a reference from an unused client instance. We automatically build a new composite
		 * to create a context of execution. This allow to use Apam without requiring the explicit definition of
		 * composites, just instantiating any implementation.
		 * 
		 * TODO should we provide a way to specify properties and models for the composites created automatically?
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
		logger.debug("Resolving dependency " + depName + " from instance " + client.getName());
		if ((depName == null) || (client == null)) {
			logger.error("missing client or dependency name");
			return false;
		}
		// Get required resource from dependency declaration, take the declaration declared at the most concrete level
		DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName);
		if (dependency == null)
			dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);

		if (dependency == null) {
			logger.error("dependency declaration not found " + depName);
			return false;
		}

		// Promotion control
		Composite compo = getClientComposite(client);
		// if it is a promotion, visibility and scope is the one of the embedding composite.
		DepMult depMult = getPromotion(client, dependency);

		Set<Instance> insts = null;
		Implementation impl = null;
		Set<Implementation> impls = new HashSet<Implementation> ();

		if (depMult != null) { // it is a promotion
			compo = compo.getComposite();
			impl = depMult.impl;
			if ((depMult.insts != null) && !depMult.insts.isEmpty()) {
				insts = depMult.insts;
				logger.debug("Selected from promotion " + insts);
			}
		}

		//compute the set of constraints that apply to that resolution: inst + impl + spec + composite generique
		dependency = computeAllConstraints (client, dependency) ;

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
						//impls = CST.apamResolver.resolveSpecByResources(compoType, dependency);
						//TODO Should look for all the implemenentaiton that satisfy the constraints.
					} //else 
					impl = CST.apamResolver.resolveSpecByResource(compoType, dependency);
				}
			}
			if (impl == null) {
				logger.error("Failed to resolve " + dependency.getTarget()
						+ " from " + client + "(" + depName + ")");
				return false;
			}

			// Look for the instances
			if (dependency.isMultiple()) {
				insts = CST.apamResolver.resolveImpls(compo, impl, Util.toFilter(dependency.getInstanceConstraints()));
				logger.debug("Selected set " + insts);
			} else {
				Instance inst = CST.apamResolver.resolveImpl(compo, impl, dependency);
				if (inst != null) {
					insts = new HashSet<Instance>();
					insts.add(inst);
					logger.debug("Selected " + inst);
				}
			}

			//No existing instance. Create one.
			if ((insts == null) || insts.isEmpty()) {
				if (insts == null)
					insts = new HashSet<Instance>();

				Instance inst = impl.createInstance(compo, null);

				if (inst == null){// should never happen
					logger.error("Failed creating instance of " + impl);
					return false;
				}

				insts.add(inst);
				logger.debug("Instantiated " + insts.toArray()[0]);
			}
		}

		// We got the instances. Create the wires.
		if ((insts != null) && !insts.isEmpty()) {
			for (Instance inst : insts) {
				if (depMult != null) { // it was a promotion, embedding composite must be linked as the source
					client.getComposite().createWire(inst, depMult.depType);
					logger.error("Promoting " + client + " -" + depName + "-> " + inst + "\n      as: "
							+ client.getComposite() + " -" + depMult.depType + "-> " + inst);
				}
				// in all cases the client must be linked
				client.createWire(inst, depName);
				//				if (dependency.isMultiple())
				//					break; // in case it is a single dep from a multiple promotion. TODO Is that possible ????
			}
		} else
			return false;

		// notify the managers
		ApamResolverImpl.notifySelection(client, dependency.getTarget(), depName,
				((Instance) insts.toArray()[0]).getImpl(), null, insts);
		return true;
	}

	/**
	 * The dependencies that apply on a dependency are those of the instance, plus those of the implem, 
	 * plus those of the spec, and finally those in the composite, if the name matches the generic constraints
	 * @param client
	 * @param dependency
	 * @return
	 */
	private DependencyDeclaration computeAllConstraints (Instance client, DependencyDeclaration dependencyIn) {
		
		DependencyDeclaration dependency = dependencyIn.clone();
		Component group = client.getGroup() ;

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
				if (impl != null || impl.getSpec().getName().matches(pattern)) {
					return true ;
				}
			}
		}

		return false;
	}

	/**
	 * Before to resolve a specification (i.e. to select one of its implementations)
	 * defined by one interface, all its interfaces, or its name, this method is called to
	 * know which managers are involved, and what are the constraints and preferences set by the managers to this
	 * resolution.
	 * 
	 * @param compTypeFrom : the origin of this resolution.
	 * @param interfaceName : the full name of one of the interfaces of the specification.
	 * @param interfaces : the full list of interfaces of the specification.
	 * @param specName : the name of the specification.
	 * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
	 * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
	 * @return : the managers that will be called for that resolution.
	 */
	private List<DependencyManager> computeSelectionPathSpec(CompositeType compoTypeFrom, String specName) {

		List<DependencyManager> selectionPath = new ArrayList<DependencyManager>();
		for (DependencyManager dependencyManager : ApamManagers.getManagers()) {

			/*
			 * Skip apamman
			 */
			if (dependencyManager == apam.getApamMan())
				continue;

			dependencyManager.getSelectionPathSpec(compoTypeFrom, specName, selectionPath);
		}

		// To select first in Apam
		selectionPath.add(0, apam.getApamMan());
		return selectionPath;
	}

	private List<DependencyManager> computeSelectionPathImpl(CompositeType compTypeFrom, String implName) {

		List<DependencyManager> selectionPath = new ArrayList<DependencyManager>();
		for (DependencyManager dependencyManager : ApamManagers.getManagers()) {

			/*
			 * Skip apamman
			 */
			if (dependencyManager == apam.getApamMan())
				continue;

			dependencyManager.getSelectionPathImpl(compTypeFrom, implName, selectionPath);
		}

		// To select first in Apam
		selectionPath.add(0, apam.getApamMan());
		return selectionPath;
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
	private List<DependencyManager> computeSelectionPathInst(Composite compoFrom, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences) {

		List<DependencyManager> selectionPath = new ArrayList<DependencyManager>();
		for (DependencyManager dependencyManager : ApamManagers.getManagers()) {

			/*
			 * Skip apamman
			 */
			if (dependencyManager == apam.getApamMan())
				continue;

			dependencyManager.getSelectionPathInst(compoFrom,impl,constraints,preferences,selectionPath);
		}

		// To select first in Apam
		selectionPath.add(0, apam.getApamMan());
		return selectionPath;
	}

	/**
	 * Impl has been deployed, it becomes embedded in compoType.
	 * If physically deployed, it is in the Unused list. remove.
	 * 
	 * @param compoType
	 * @param impl
	 */
	private void deployedImpl(CompositeType compoType, Implementation impl, boolean deployed) {
		// it was not deployed
		if (!deployed && impl.isUsed()) {
			logger.debug(" : selected " + impl);
			return;
		}

		// it is deployed or was never used so far

		if (impl.isUsed()) {
			logger.debug(" : logically deployed " + impl);
		} else {// it was unused so far.
			logger.debug(" : deployed " + impl);
		}

		((CompositeTypeImpl)compoType).deploy(impl);
	}

	/**
	 * Look for an implementation with a given name "implName", visible from composite Type compoType.
	 * 
	 * @param compoType
	 * @param implName
	 * @return
	 */
	@Override
	public Implementation findImplByName(CompositeType compoTypeFrom, String implName) {
		List<DependencyManager> selectionPath = computeSelectionPathImpl(compoTypeFrom, implName);

		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
		Implementation impl = null;
		logger.debug("Looking for implementation " + implName + ": ");
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			impl = manager.findImplByName(compoTypeFrom, implName);

			if (impl != null) {
				deployedImpl(compoTypeFrom, impl, deployed);
				return impl;
			}
		}
		return null;
	}

	/**
	 * Look for an implementation with a given name "implName", visible from composite Type compoType.
	 * 
	 * @param compoType
	 * @param specName
	 * @return
	 */
	//    @Override
	//    public Specification findSpecByName(Composite compFrom, String specName) {
	//        return findSpecByName (compFrom.getCompType(), specName) ;
	//    }
	@Override
	public Specification findSpecByName(CompositeType compTypeFrom, String specName) {
		List<DependencyManager> selectionPath = computeSelectionPathSpec(compTypeFrom, specName);

		Specification spec = null;
		logger.debug("Looking for specification " + specName + ": ");
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			spec = manager.findSpecByName(compTypeFrom, specName);
			if (spec != null) {
				if (deployed) {
					logger.debug("Deployed specificaiton " + specName);
				} else
					logger.debug("Selected specificaiton " + specName);

				return spec;
			}
		}
		logger.debug("Could not find specification " + specName);
		return null;
	}

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
			Set<Filter> constraints, List<Filter> preferences) {
		if (constraints == null)
			constraints = new HashSet<Filter>();
		if (preferences == null)
			preferences = new ArrayList<Filter>();
		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();

		List<DependencyManager> selectionPath = computeSelectionPathSpec(compoTypeFrom, specName);

		if (constraints.isEmpty() && preferences.isEmpty())
			logger.debug("Looking a \"" + specName + "\" implementation.");
		else
			logger.debug("Looking a \"" + specName + "\" implementation. Constraints:" + constraints
					+ ". Preferences: " + preferences);
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			Implementation impl = manager.resolveSpecByResource(compoTypeFrom, new SpecificationReference(specName),
					constraints, preferences);

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
	@Override
	public Implementation resolveSpecByResource(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
		// Get the constraints and preferences by merging declarations
		Set<Filter> implementationConstraints = Util.toFilter(dependency.getImplementationConstraints());
		List<Filter> implementationPreferences = Util.toFilterList(dependency.getImplementationPreferences());

		return resolveSpecByResourceLoop(compoTypeFrom, dependency.getTarget(), implementationConstraints, implementationPreferences) ;
//		// TODO Hummm ... toString is not the name.
//		List<DependencyManager> selectionPath = computeSelectionPathSpec(compoTypeFrom, dependency.getTarget()
//				.toString());
//
//		logger.debug("Looking for an implem with" + dependency);
//		if (compoTypeFrom == null)
//			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
//		Implementation impl = null;
//		boolean deployed = false;
//		for (DependencyManager manager : selectionPath) {
//			if (!manager.getName().equals(CST.APAMMAN))
//				deployed = true;
//			logger.debug(manager.getName() + "  ");
//			impl = manager.resolveSpecByResource(compoTypeFrom, dependency.getTarget(),
//					implementationConstraints, implementationPreferences);
//			if (impl != null) {
//				deployedImpl(compoTypeFrom, impl, deployed);
//				return impl;
//			}
//		}
//		return null;
	}

	public Implementation resolveSpecByResourceLoop (CompositeType compoTypeFrom, ResolvableReference ressource,
            Set<Filter> constraints, List<Filter> preferences) {
		//logger.debug("Looking for an implem with" + dependency);
		List<DependencyManager> selectionPath = computeSelectionPathSpec(compoTypeFrom, ressource.toString());

		if (compoTypeFrom == null)
			compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
		Implementation impl = null;
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN))
				deployed = true;
			logger.debug(manager.getName() + "  ");
			impl = manager.resolveSpecByResource(compoTypeFrom, ressource,
					constraints, preferences);
			if (impl != null) {
				deployedImpl(compoTypeFrom, impl, deployed);
				return impl;
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
	@Override
	public Instance resolveImpl(Composite compo, Implementation impl,
			DependencyDeclaration dependency) {
		Set<Filter> constraints = Util.toFilter(dependency.getInstanceConstraints());
		List<Filter> preferences = Util.toFilterList(dependency.getInstancePreferences());
		List<DependencyManager> selectionPath = computeSelectionPathInst(compo, impl, constraints, preferences);

		if (compo == null)
			compo = CompositeImpl.getRootAllComposites();
		Instance inst = null;
		logger.debug("Looking for an instance of " + impl + ": ");
		for (DependencyManager manager : selectionPath) {
			logger.debug(manager.getName() + "  ");
			inst = manager.resolveImpl(compo, impl, constraints, preferences);
			if (inst != null) {
				return inst;
			}
		}
		// TODO Notify dynaman

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
	@Override
	public Set<Instance> resolveImpls(Composite compo, Implementation impl, Set<Filter> constraints) {

		if (impl == null) {
			logger.error("impl is null in resolveImpls");
			return null;
		}
		if (constraints == null)
			constraints = new HashSet<Filter>();
		List<DependencyManager> selectionPath = computeSelectionPathInst(compo, impl, constraints, null);

		if (compo == null)
			compo = CompositeImpl.getRootAllComposites();

		Set<Instance> insts = null;
		logger.debug("Looking for instances of " + impl + ": ");
		for (DependencyManager manager : selectionPath) {
			logger.debug(manager.getName() + "  ");
			insts = manager.resolveImpls(compo, impl, constraints);
			if ((insts != null) && !insts.isEmpty()) {
				// logger.debug("selected " + insts);
				return insts;
			}
		}
		//        if (insts == null)
		//            insts = new HashSet<Instance>();
		//        if (insts.isEmpty()) {
		//            insts.add(impl.createInstance(compo, null));
		//        }
		//        logger.debug("instantiated " + (insts.toArray()[0]));
		return insts;
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

}
