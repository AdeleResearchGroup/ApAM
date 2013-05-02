/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.impl;


import java.util.ArrayList;
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
import fr.imag.adele.apam.Dependency;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.DependencyPromotion;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

public class ApamResolverImpl implements ApamResolver {
	private APAMImpl apam;
	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	public ApamResolverImpl(APAMImpl theApam) {
		this.apam = theApam;
	}

	/**
	 * Only instance have a well-defined and unique enclosing composite (type and instance). 
	 * Promotion control will apply only on sources that are instances; but for relations, targetType can be any component.
	 * We will look for a relation at the composite level, that matches the target (target and targetType), 
	 * whatever the Id, composite rel cardinality must be multiple if the dependency is multiple. 
	 * If so, it is a promotion. The composite dependency is resolved, then the source dependency is resolved as a subset of the composite rel.
	 * The client becomes the embedding composite; visibility and scope become the one of the embedding composite
	 * 
	 * Note that is more than one composite dependency matches the source dependency, one arbitrarily is selected. 
	 * To closely control promotions, use the tag “promotion” in the composite dependency definition.

	 *
	 * @param client
	 * @param dependency definition
	 * @return the composite dependency from the composite.
	 */
	//	private  Resolved getPromotion(Component clientC, Dependency dependency) {
	//		//Only for instances
	//		if (! (clientC instanceof Instance)) {
	//			return null ;
	//		}
	//
	//		Instance source = (Instance)clientC ;
	//
	//		Composite compo = getClientComposite(source);
	//		//Instance refClient = source;
	//		Dependency promotionDependency = getPromotionDep(source, dependency);
	//		// if it is a promotion, visibility and scope is the one of the embedding composite.
	//		if (promotionDependency == null) {
	//			return resolveWire (compo, promotionDependency) ;
	//		}		
	//		return new Resolved (compo.getLinkDests(promotionDependency.getIdentifier()), null) ;
	//	}


	private  Dependency getPromotionDep(Component clientC, Dependency dependency) {

		Instance client = (Instance)clientC ;
		Composite composite = client.getComposite() ;

		if (composite.getDeclaration() == null) {
			return null;
		}

		//look if a promotion is explicitly declared for that client component
		// <promotion implementation="A" dependency="clientDep" to="compoDep" />
		// <promotion specification="SA" dependency="clientDep" to="compoDep" />
		for (DependencyPromotion promo: composite.getCompType().getCompoDeclaration().getPromotions()) {
			if (! promo.getContentDependency().getIdentifier().equals(dependency.getIdentifier())) {
				continue;		// this promotion is not about our dependency  (not "clientDep")
			}

			String sourceName = promo.getContentDependency().getDeclaringComponent().getName();
			//sourceName = "SA" or "A"
			if (sourceName.equals(client.getImpl().getName()) || sourceName.equals(client.getSpec().getName())) {
				// We found the right promotion from client side.
				//Look for the corresponding composite dependency "compoDep"
				String toName =  promo.getCompositeDependency().getIdentifier();
				Dependency foundPromo = composite.getCompType().getDependency(toName) ;
				//					if (compoDep.getIdentifier().equals(toName)) {
				//We found the composite side. It is an explicit promotion. It should match.
				if (DependencyUtil.matchDependency (client, foundPromo, dependency)) {
					return foundPromo ;
				}
				logger.error ("Promotion is invalid. Dependency " + promo.getContentDependency().getIdentifier()
						+ " of component " + sourceName + " does not match the composite dependency " + foundPromo) ;
				return null ;
			}
		}

		//Look if a relation, defined in the composite, matches the current dependency
		//Do no check composite
		Component group = client ;
		while (group != null) {
			for (Dependency compoDep : group.getLocalDependencies()) {
				if  (DependencyUtil.matchDependency(client, compoDep, dependency)) {
					return compoDep ;
				}
			}
		}
		return null ;
	}

	// if the instance is unused, it will become the main instance of a new composite.
	private Composite getClientComposite(Instance mainInst) {

		if (mainInst.isUsed()) {
			return mainInst.getComposite();
		}

		/*
		 * We are resolving a reference from an unused client instance. We automatically build a new composite
		 * to create a context of execution. This allow to use Apam without requiring the explicit definition of
		 * composites, just instantiating any implementation.
		 */

		Implementation mainComponent			= mainInst.getImpl();
		String applicationName 					= mainComponent.getName() + "_Appli";
		SpecificationReference specification	= mainComponent.getImplDeclaration().getSpecification();
		Set<ManagerModel> models				= new HashSet<ManagerModel>();

		CompositeType application = apam.createCompositeType(null,
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
	 * @param source the instance that requires the specification
	 * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
	 * @return
	 */

	@Override
	public Resolved<?> resolveLink(Component source, String depName) {
		if ((depName == null) || (source == null)) {
			logger.error("missing client or dependency name");
			return null;
		}

		//Get the dependency
		Dependency dependencyDef = source.getDependency(depName)  ;
		if (dependencyDef == null) {
			logger.error("Relation declaration invalid or not found " + depName);
			return null;
		}
		return resolveLink (source, dependencyDef) ;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Resolved<?> resolveLink(Component source, Dependency dep) {
		if (source == null || dep == null){
			logger.error("missing client or dependency ");
			return null;
		}

		logger.info("Resolving relation " + dep + " from " + source );

		//Will contain the candidate solution (before constraint matching).
		Resolved resolved = null;

		boolean isPromotion = false ;
		boolean promoHasConstraints = false ;

		/*
		 *  Promotion control
		 *  Only for instances
		 */
		if (source instanceof Instance) {
			Composite compo = getClientComposite((Instance)source);
			Dependency promotionDependency = getPromotionDep((Instance)source, dep);

			// if it is a promotion, visibility and scope is the one of the embedding composite. We got the candidates.
			if (promotionDependency != null) {
				isPromotion = true;
				promoHasConstraints = promotionDependency.hasConstraints() ;
				if (promotionDependency.isMultiple())
					resolved = new Resolved (compo.getLinkDests(promotionDependency.getIdentifier()));
				else resolved = new Resolved (compo.getLinkDest(promotionDependency.getIdentifier()));

				if (resolved.isEmpty()) 
					resolved = resolveLink (compo, promotionDependency) ;
				if (resolved == null) {
					logger.error("Failed to resolve " + dep.getTarget()
							+ " from " + source + "(" + dep.getIdentifier() + ")");
					return null;
				}
				resolved = DependencyUtil.getResolved(resolved, dep) ;
			}
		}


		//in case promotion already provided resolved.
		//Resolve the dependency now
		if (resolved == null) {
			//call the managers ...
			resolved = this.resolveDependency(source, dep);
			if (resolved == null) {
				logger.error("Failed to resolve " + dep.getTarget()
						+ " from " + source + "(" + dep.getIdentifier() + ")");
				return null;
			}
		}

		//It is resolved.
		if (resolved.singletonResolved != null) {
			source.createLink(resolved.singletonResolved, dep, dep.hasConstraints() || promoHasConstraints, isPromotion);
			return resolved ;
		}
		for (Object target : resolved.setResolved) {
			source.createLink((Component)target, dep, dep.hasConstraints() || promoHasConstraints, isPromotion);			
		}
		return resolved ;
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
	private List<DependencyManager> computeSelectionPath(Component source, Dependency dependency) {

		List<DependencyManager> selectionPath = new ArrayList<DependencyManager>();
		for (DependencyManager dependencyManager : ApamManagers.getDependencyManagers()) {
			/*
			 * Skip apamman and UpdateMan
			 */
			if (dependencyManager.getName().equals(CST.APAMMAN) || dependencyManager.getName().equals(CST.UPDATEMAN)) {
				continue;
			}
			dependencyManager.getSelectionPath(source, dependency, selectionPath);
		}

		((DependencyImpl)dependency).computeFilters() ;
		logger.info("Looking for all " + dependency.getTarget() + " satisfying " + dependency);

		// To select first in Apam
		selectionPath.add(0, apam.getApamMan());
		selectionPath.add(0, apam.getUpdateMan());
		return selectionPath;
	}

	/**
	 * Impl is either unused or deployed (and therefore also unused). 
	 * It becomes embedded in compoType.
	 * If unused, remove from unused list.
	 *
	 * @param compoType
	 * @param impl
	 */
	private static void deployedImpl(Component source, Component comp, boolean deployed) {
		//We take care only of implementations
		if ( !(comp instanceof Implementation)) 
			return ;

		Implementation impl = (Implementation)comp ;
		// it was not deployed
		if (!deployed && impl.isUsed()) {
			logger.info(" : selected " + impl);
			return;
		}

		CompositeType compoType ;
		if (source instanceof Instance) {
			compoType = ((Instance)source).getComposite().getCompType();
		} else if (source instanceof Implementation) {
			compoType = ((Implementation)source).getInCompositeType().iterator().next ();
		} else {
			logger.error("Should not call deployedImpl on a source Specification " + source) ;
			//TODO in which composite to put it. Still in unused ?
			return ;
		}
		((CompositeTypeImpl)compoType).deploy(impl);


		// it is deployed or was never used so far
		if (impl.isUsed()) {
			logger.info(" : logically deployed " + impl);
		} else {// it was unused so far.
			((ComponentImpl)impl).setFirstDeployed(compoType);
			if (deployed) {
				logger.info(" : deployed " + impl);				
			} else {
				logger.info(" : was here, unused " + impl);
			}
		}
	}


	@Override
	public Implementation resolveSpecByInterface(Component client, String interfaceName, Set<String> constraints, List<String> preferences) {

		Dependency dep = new DependencyImpl (client, interfaceName, false, new InterfaceReference(interfaceName), null, ComponentKind.IMPLEMENTATION);

		if (constraints != null)
			dep.getImplementationConstraints().addAll(constraints) ;
		if (preferences != null)
			dep.getImplementationPreferences().addAll(preferences) ;

		return resolveSpecByResource(client, dep);
	}

	@Override
	public Implementation resolveSpecByMessage(Component client, String messageName, Set<String> constraints, List<String> preferences) {

		Dependency dep = new DependencyImpl (client, messageName, false, new MessageReference(messageName), null, ComponentKind.IMPLEMENTATION);

		if (constraints != null)
			dep.getImplementationConstraints().addAll(constraints) ;
		if (preferences != null)
			dep.getImplementationPreferences().addAll(preferences) ;

		return resolveSpecByResource(client, dep);
	}


	@Override
	public void updateComponent(String componentName) {
		Implementation impl = CST.componentBroker.getImpl(componentName) ;
		if (impl == null) {
			logger.error ("Unknown component " + componentName) ;
			return ;
		}
		UpdateMan.updateComponent(impl) ;
	}

	@Override
	public Instance resolveImpl(Component client, Implementation impl, Set<String> constraints, List<String> preferences) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		Dependency dep = new DependencyImpl (client, impl.getName(), false, new ImplementationReference<ImplementationDeclaration>(impl.getName()), null, ComponentKind.INSTANCE);
		if (constraints != null)
			dep.getImplementationConstraints().addAll(constraints) ;
		if (preferences != null)
			dep.getImplementationPreferences().addAll(preferences) ;

		Resolved<?> resolve = resolveLink (client, dep) ;
		if (resolve == null) 
			return null ;
		return (Instance)resolve.setResolved ;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Instance> resolveImpls(Component client, Implementation impl, Set<String> constraints) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		Dependency dep = new DependencyImpl (client, impl.getName(), true, new ImplementationReference<ImplementationDeclaration>(impl.getName()), null, ComponentKind.INSTANCE);
		if (constraints != null)
			dep.getImplementationConstraints().addAll(constraints) ;

		Resolved<?> resolve = resolveLink (client, dep) ;
		if (resolve == null) 
			return null ;

		//return (set<Instance>resolve.setResolved est invalide
		return (Set<Instance>)resolve.setResolved ;

		//		Set<Instance> ret = new HashSet<Instance> () ;
		//		for (Object c : resolve.setResolved) {
		//			ret.add((Instance)c) ;
		//		}
		//		return ret ;
	}

	/**
	 * Look for an implementation with a given name "implName", visible from composite Type compoType.
	 *
	 * @param compoType
	 * @param componentName
	 * @return
	 */

	private Component findByName (Component client, String componentName, ComponentKind targetKind) {
		if (componentName == null) return null;
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		Dependency dependency = new DependencyImpl (client, componentName, false, new ComponentReference<ComponentDeclaration>(componentName), client.getKind(), targetKind) ;
		Resolved<?> res = resolveLink (client, dependency) ;
		if (res == null) return null ;
		return res.singletonResolved ;
	}

	@Override
	public Specification findSpecByName(Component client, String specName) {
		return (Specification)findByName (client, specName, ComponentKind.SPECIFICATION) ;
	}

	@Override
	public Implementation findImplByName(Component client, String implName) {
		return (Implementation)findByName (client, implName, ComponentKind.IMPLEMENTATION) ;
	}

	public Instance findInstByName(Component client, String instName) {
		return (Instance)findByName (client, instName, ComponentKind.INSTANCE) ;
	}

	public Component findComponentByName(Component client, String name) {
		Component ret = findImplByName(client, name) ;
		if (ret != null)
			return ret ;
		ret = findSpecByName(client, name) ;
		if (ret != null)
			return ret ;
		return findInstByName (client, name) ;	
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
	public Implementation resolveSpecByName(Instance client, String specName, Set<String> constraints, List<String> preferences) {
		if (client == null) {
			client = CompositeImpl.getRootInstance () ;
		}

		Dependency dep = new DependencyImpl (client, specName, false, new SpecificationReference(specName), null, ComponentKind.IMPLEMENTATION);
		if (constraints != null) 
			dep.getImplementationConstraints().addAll(constraints) ;		

		if (preferences != null) 
			dep.getImplementationPreferences().addAll(preferences) ;

		return resolveSpecByResource(client, dep) ;
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
	public Implementation resolveSpecByResource(Component client, Dependency dependency) {
		if (dependency.getTargetType() != ComponentKind.IMPLEMENTATION) {
			logger.error("Invalid target type for resolveSpecByResource. Implemntation expected, found : " + dependency.getTargetType()) ;
			return null ;
		}
		Resolved<?> resolve = resolveLink (client, dependency) ;
		if (resolve == null) return null ;

		if (resolve.singletonResolved != null) 
			return (Implementation)resolve.singletonResolved ;
		return (Implementation)resolve.setResolved.iterator().next() ;
	}


	/**
	 * Performs a complete resolution of the dependency EXCEPT : isMultiple always assumed to be True, and preferences ignored.
	 * 
	 * The manager is asked to find the "right" implementation and instances for the provided dependency.
	 * First computes all the implementations that satisfy the constraints, preferences not taken into account.
	 * Add in insts (if present) all the instances of the implems (visible or not) 
	 * 	that satisfy the instance constraints and that are visible.
	 * Returns those  implementations that are visible.
	 * 
	 * @param client the instance calling implem (and where to create implementation, if
	 *            needed). Cannot be null.
	 * @param dependency a dependency declaration containing the type and name of the dependency target. It can be
	 *            -the specification Name (new SpecificationReference (specName))
	 *            -an implementation name (new ImplementationRefernece (name)
	 *            -an interface name (new InterfaceReference (interfaceName))
	 *            -a message name (new MessageReference (dataTypeName))
	 *            - or any future resource ...
	 * @return the implementations and the instances if resolved, null otherwise
	 * @return null if not resolved at all. Never returns an empty set.
	 */
	private Resolved<?> resolveDependency(Component source, Dependency dependency) {
		List<DependencyManager> selectionPath = computeSelectionPath(source, dependency);
		//Transform the dependency constraints into filters after interpreting the substitutions.

		Resolved<?> res = null ;
		boolean deployed = false;

		for (DependencyManager manager : selectionPath) {
			if (!manager.getName().equals(CST.APAMMAN) && !manager.getName().equals(CST.UPDATEMAN)) {
				deployed = true;
			}
			logger.debug(manager.getName() + "  ");

			//Does the real job
			res = manager.resolveDependency(source, dependency);
			if (res == null || res.isEmpty()) 
				//This manager did not found a solution, try the next manager
				continue ;

			/*
			 *  a manager succeeded to find a solution 
			 */
			//If an unused or deployed implementation. Can be into singleton  or in toInstantiate if an instance is required
			Component depl = (res.toInstantiate != null) ? res.toInstantiate : res.singletonResolved ; 
			deployedImpl(source, depl, deployed);

			/*
			 * If an implementation is returned as "toInstantiate" it has to be instantiated
			 */
			if (res.toInstantiate != null) {			
				if (dependency.getTargetType() != ComponentKind.INSTANCE) {
					logger.error("Invalid Resolved value. toInstantiate is set, but target kind is not Instance") ;
					continue ;
				}

				Composite compo = (source instanceof Instance) ? ((Instance)source).getComposite() : CompositeImpl.getRootInstance() ;
				Instance inst = res.toInstantiate.createInstance(compo, null);
				if (inst == null) { // may happen if impl is non instantiable
					logger.error("Failed creating instance of " + res.toInstantiate );
					continue ;
				}
				logger.info("Instantiated " + inst) ;							
				if (dependency.isMultiple()) {
					Set <Instance> insts = new HashSet <Instance> () ;
					insts.add(inst) ;
					return new Resolved<Instance> (insts) ;
				}
				else return new Resolved<Instance> (inst) ;												
			}

			/*
			 * Because managers can be third party, we cannot trust them. Verify that the result is correct.
			 */
			if (dependency.isMultiple()) {
				if (res.setResolved == null || res.setResolved.isEmpty()) {
					logger.info("manager " + manager + " returned an empty result. Should be null." ) ;
					continue ;
				}
				if (((Component)res.setResolved.iterator().next()).getKind() != dependency.getTargetType()) {
					logger.error("Manager " + manager + " returned objects of the bad type for dependency " + dependency) ;
					continue ;
				}
				logger.info("Selected : " + res.setResolved) ;
				return res ;
			}

			//Result is a singleton
			if (res.singletonResolved == null ) {
				logger.info("manager " + manager + " returned an empty result. " ) ;
				continue ;
			}
			if (res.singletonResolved.getKind() != dependency.getTargetType()) {
				logger.error("Manager " + manager + " returned objects of the bad type for dependency " + dependency) ;
				continue ;
			}
			logger.info("Selected : " + res.singletonResolved) ;
			return res ;
		}

		//No solution found
		return null ;
	}


}
