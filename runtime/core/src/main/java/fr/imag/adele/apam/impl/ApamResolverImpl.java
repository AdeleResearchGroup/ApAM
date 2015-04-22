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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.RelationPromotion;
import fr.imag.adele.apam.declarations.ResolvePolicy;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.InstanceReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;

public class ApamResolverImpl implements ApamResolver {


	private APAMImpl apam;

	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	/**
	 * The current state of the resolver. The resolver can be temporarily
	 * disabled, for instance for waiting for the installation of required
	 * managers;
	 * 
	 */
	private boolean enabled = false;

	/**
	 * A description of the condition that must be met to enable the resolver
	 * again.
	 */
	private String condition = "resolver startup";

	/**
	 * If the resolver is disabled, the time at which it will be automatically
	 * enabled, even if the condition is not met. This is not an delay, but the
	 * actual future time.
	 */
	private long maxDisableTime = 0L;

	public ApamResolverImpl(APAMImpl theApam) {
		this.apam = theApam;
	}
	
	/**
	 * Impl is either unused or deployed (and therefore also unused). It becomes
	 * embedded in compoType. If unused, remove from unused list.
	 * 
	 * @param compoType
	 * @param impl
	 */
	private static void deployedImpl(Component source, Component comp, boolean deployed) {
		// We take care only of implementations
		if (!(comp instanceof Implementation)) {
			return;
		}

		Implementation impl = (Implementation) comp;
		// it was not deployed
		if (!deployed && impl.isUsed()) {
			logger.info(" : selected " + impl);
			return;
		}

		CompositeType compoType;
		if (source instanceof Instance) {
			compoType = ((Instance) source).getComposite().getCompType();
		} else if (source instanceof Implementation) {
			compoType = ((Implementation) source).getInCompositeType().iterator().next();
		} else {
			logger.error("Should not call deployedImpl on a source Specification " + source);
			// TODO in which composite to put it. Still in root ?
			return;
		}
		((CompositeTypeImpl) compoType).deploy(impl);

		// it is deployed or was never used so far
		if (impl.isUsed()) {
			logger.info(" : logically deployed " + impl);
		} else {// it was unused so far.
			((ComponentImpl) impl).setFirstDeployed(compoType);
			if (deployed) {
				logger.info(" : deployed " + impl);
			} else {
				logger.info(" : was here, unused " + impl);
			}
		}
	}


	/**
	 * Verifies if the resolver is enabled. If it is disabled blocks the calling
	 * thread waiting for the enable condition.
	 */
	private synchronized void checkEnabled() {
		while (!this.enabled) {
			try {

				/*
				 * Verify if the disable timeout has expired, in that case
				 * simply enable the resolver again.
				 */
				long currentTime = System.currentTimeMillis();
				if (currentTime > maxDisableTime) {

					logger.debug("APAM RESOLVER resuming resolution, condition did not happen: " + condition);
					enable();
					return;
				}

				logger.debug("APAM RESOLVER waiting for: " + condition);
				wait(this.maxDisableTime - currentTime);

			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * Disables the resolver until the specified condition is met. If the
	 * condition is not signaled before the specified timeout, the resolver will
	 * be automatically enabled.
	 */
	public synchronized void disable(String condition, long timeout) {
		
		logger.debug("Resolver disabled waiting for "+condition);
		this.enabled = false;
		this.condition = condition;
		this.maxDisableTime = System.currentTimeMillis() + timeout;

	}

	/**
	 * Enables the resolver after the condition is met
	 */
	public synchronized void enable() {

		logger.debug("Resolver enabled");

		this.enabled = true;
		this.condition = null;
		this.maxDisableTime = 0L;

		this.notifyAll();
	}

	private Component findByName(Component client, ComponentReference<?> targetComponent, ComponentKind targetKind) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
			// hummmm patch .... TODO
			if (targetComponent.getName().equals(CST.ROOT_COMPOSITE_TYPE)) {
				return CompositeTypeImpl.getRootCompositeType();
			}
		}

		// CompositeType compoType = CompositeTypeImpl.getRootCompositeType();

		RelationDefinition rel = new RelationDefinitionImpl(targetComponent, client.getKind(), targetKind, null, null);
		Resolved<?> res = resolveLink(client, rel);
		if (res == null) {
			return null;
		}
		return res.singletonResolved;
	}

	@Override
	public Component findComponentByName(Component client, String name) {
		Component ret = findImplByName(client, name);
		if (ret != null) {
			return ret;
		}
		ret = findSpecByName(client, name);
		if (ret != null) {
			return ret;
		}
		return findInstByName(client, name);
	}

	@Override
	public Implementation findImplByName(Component client, String implName) {
		return (Implementation) findByName(client, new ImplementationReference<ImplementationDeclaration>(implName), ComponentKind.IMPLEMENTATION);
	}

	@Override
	public Instance findInstByName(Component client, String instName) {
		return (Instance) findByName(client, new InstanceReference(instName), ComponentKind.INSTANCE);
	}


	@Override
	public Specification findSpecByName(Component client, String specName) {
		return (Specification) findByName(client, new SpecificationReference(specName), ComponentKind.SPECIFICATION);
	}

	/**
	 * Get the subset of the links of a composite that can satisfy a promotion for the specified source and relation
	 * inside the composite.
	 * 
	 * NOTE IMPORTANT this method may trigger resolution of the composite's relation 
	 */
	@SuppressWarnings("unchecked")
	private <T extends Component> Resolved<T> getPromotionCandidates(Instance source, RelToResolve relation, RelationDefinition compositeRelation) {

		Set<Component> candidates = source.getComposite().getLinkDests(compositeRelation.getName());
		
		/*
		 * It there is no candidates, force resolution of the composite's relation
		 */
		if (candidates.isEmpty()) {
			resolveLink(source.getComposite(), compositeRelation);
			candidates = source.getComposite().getLinkDests(compositeRelation.getName());
		}
		
		/*
		 * Select the candidates that match the relation constraints
		 */
		return (Resolved<T>) relation.getResolved(candidates,true);
	}

	/**
	 * Look if a promotion is explicitly declared for the specified client and relation, and returns the result
	 * of promoting the relation
	 * 
	 */
	private <T extends Component> Resolved<T> checkExplicitPromotion(Instance client, RelToResolve relation) {
		
		Resolved<T> promotionResult	= null;
		
		search:
		for (RelationPromotion promotion : client.getComposite().getCompType().getCompoDeclaration().getPromotions()) {
			
			/*
			 * check if the relation to resolve matches the identifier of the promoted relation
			 */
			if (!promotion.getContentRelation().getIdentifier().equals(relation.getName())) {
				continue; 
			}

			/*
			 * check if the source of the promoted relation matches the client (or one of its ancestor groups) 
			 */
			String source 		= promotion.getContentRelation().getDeclaringComponent().getName();
			Component matching 	= null;
			Component ancestor	= client;
			
			while (matching == null && ancestor != null) {
				if (ancestor.getName().equals(source)) {
					matching = ancestor;
				}
				
				ancestor = ancestor.getGroup();
			}
			
			/*
			 * If we find a matching explicit promotion try to resolve using the matched composite relation
			 */
			if (matching != null) {
				
				RelationDefinition compositeRelation	= client.getComposite().getRelation(promotion.getCompositeRelation().getIdentifier());
				
				/*
				 * Validate the source's and composite's relations are compatible.
				 * 
				 * NOTE This is already validated at build time, but we do the tests again at runtime
				 */
				if (!relation.getRelationDefinition().matchRelation(client,compositeRelation)) {
					logger.error("Promotion is invalid. relation " + relation.getName() + " of component " + client.getName() + " does not match the composite relation " + compositeRelation);
					continue search;
				}
					
				Resolved<T> candidates = getPromotionCandidates(client, relation, compositeRelation);
				if (candidates != null && !candidates.isEmpty()) {

					/*
					 * Create the promotion links and return
					 */
					updateModel(client, relation, candidates, relation.hasConstraints() || compositeRelation.hasConstraints(), true);
					
					/*
					 * NOTE IMPORTAN SPECIAL CASE for relations with cardinality multiple, we allow merging the result of
					 * several promotions
					 */
					if (relation.isMultiple()) {
						promotionResult = candidates.merge(promotionResult);
						continue search;
					}
					else {
						promotionResult = candidates;
						break search;
					}

				}
			}
		}
		
		
		return promotionResult;
	}

	/**
	 * Look if a relation defined in the composite matches implicitly the specified relation, and tries to perform
	 * an implicit promotion
	 */
	private <T extends Component> Resolved<T> checkImplicitPromotion(Instance client, RelToResolve relation) {

		Resolved<T> promotionResult	= null;

		search:
		for (RelationDefinition compositeRelation : client.getComposite().getRelations()) {

			if (!relation.getRelationDefinition().matchRelation(client,compositeRelation)) {
				continue search;
			}
			
			Resolved<T> candidates = getPromotionCandidates(client, relation, compositeRelation);
			if (candidates != null && !candidates.isEmpty()) {

				/*
				 * Create the promotion links and return
				 */
				updateModel(client, relation, candidates, relation.hasConstraints() || compositeRelation.hasConstraints(), true);
				
				/*
				 * NOTE IMPORTAN SPECIAL CASE for relations with cardinality multiple, we allow merging the result of
				 * several promotions
				 */
				if (relation.isMultiple()) {
					promotionResult = candidates.merge(promotionResult);
					continue search;
				}
				else {
					promotionResult = candidates;
					break search;
				}

			}
		}
		return promotionResult;
	}



	/**
	 * Performs a complete resolution of the relation, or resolution.
	 * 
	 * The managers is asked to find the "right" component.
	 * 
	 * @param client
	 *            the instance calling implem (and where to create
	 *            implementation ans instances if needed). Cannot be null.
	 * @param relToResolve
	 *            a relation declaration containing the type and name of the
	 *            relation target. It can be -the specification Name (new
	 *            SpecificationReference (specName)) -an implementation name
	 *            (new ImplementationRefernece (name) -an interface name (new
	 *            InterfaceReference (interfaceName)) -a message name (new
	 *            MessageReference (dataTypeName))
	 * @return the component(s) if resolved, null otherwise
	 */
	private <T extends Component> Resolved<T> resolveByManagers(RelToResolve relToResolve) {

		/*
		 * Get the list of external managers.
		 * 
		 * NOTE that we invoke getSelectionPath on all managers (even if
		 * resolve policy is specified EXTERNAL). In this way, managers can
		 * influence resolution, by adding constraints, even if they do not
		 * perform resolution themselves.
		 * 
		 */

		SortedSet<RelationManager> externalManagers = new TreeSet<RelationManager>(ApamManagers.getRelationManagers().comparator());
		
		for (RelationManager relationManager : ApamManagers.getRelationManagers()) {
			if (relationManager.beginResolving(relToResolve))
				externalManagers.add(relationManager);
		}
		// Compute filters once for all, and make it final
		((RelToResolveImpl) relToResolve).computeFilters();

		/*
		 * Get the list of all managers
		 */
		List<RelationManager> selectionPath = new ArrayList<RelationManager>();

		selectionPath.add(0, apam.getApamMan());
		selectionPath.add(0, apam.getUpdateMan());
		if (apam.getApamMan() == null) {
			throw new RuntimeException("Error while get of ApamMan");
		}
		if (apam.getUpdateMan() == null) {
			throw new RuntimeException("Error while get of UpdateMan");
		}

		/*
		 * If resolve = exist or internal, only predefined managers must be called
		 */
		boolean resolveExternal = (relToResolve.getResolve() == ResolvePolicy.EXTERNAL);
		if (resolveExternal) {
			selectionPath.addAll(externalManagers);
		}

		if (!relToResolve.isRelation()) { // It is a find
			logger.info("Looking for " + relToResolve.getTargetKind() + " " + relToResolve.getTarget().getName());
		} else {
			logger.info("Resolving " + relToResolve);
		}

		Resolved<T> res ;
		String mess = "";

		for (RelationManager manager : selectionPath) {
			if (manager == null) {
				throw new RuntimeException("Manager is null, SelectionPath " ) ; //+ selectionPath);
			}
			if (manager.getName() == null) {
				throw new RuntimeException("Manager : " + manager + ", manager name is null");
			}			
			mess += manager.getName() + "  ";
            logger.debug("Calling manager "+manager.getName());

			res = resolveOneManager(manager, relToResolve, mess) ;
			if (res != null) 
				return res ;
		}
		
		//All managers have been tried. No solution found
		logger.debug(mess + " : not found");
		return null;
	}


	@SuppressWarnings("unchecked")
	private <T extends Component> Resolved<T> resolveOneManager (RelationManager manager, RelToResolve relToResolve, String mess) {

		Resolved<T> resolved = (Resolved<T>) manager.resolve(relToResolve);
		
        logger.debug("resolveOneManager(...), manager resolve returns "+(resolved==null?null:resolved.toString()));
		if (resolved == null || resolved.isEmpty()) {
			return null;
		}

		/*
		 * This manager succeeded to find a solution If an unused or deployed
		 * implementation. Can be into singleton or in toInstantiate if an
		 * instance is required
		 */
		Component source = relToResolve.getLinkSource();
		boolean deployed = !manager.getName().equals(CST.APAMMAN) && !manager.getName().equals(CST.UPDATEMAN) ;
		Component depl = (resolved.toInstantiate != null) ? resolved.toInstantiate : resolved.singletonResolved;

		deployedImpl(source, depl, deployed);

		/*
		 * If an implementation is returned as "toInstantiate" it has to be instantiated
		 */
		if (resolved.toInstantiate != null) {
			if (relToResolve.getTargetKind() != ComponentKind.INSTANCE) {
				logger.error(mess + "Invalid Resolved value. toInstantiate is set, but target kind is not an Instance");
				return null;
			}

			/*
			 * If resolveExist, we cannot instantiate.
			 */
			if (relToResolve.getResolve() == ResolvePolicy.EXIST) {
				logger.error(mess + "resolve=\"exist\" but no valid instance of " + resolved.toInstantiate + " are found. Resolve failed.");
				return null;
			}

			/*
			 * This external manager returned a non instantiable implem (ApamMan does not do that).
			 * Try this manager again but with a constraint avoiding to find the same implem. 
			 */
			if (!resolved.toInstantiate.isInstantiable()) {
				logger.debug(mess + "Implementation non-instantiable " + resolved.toInstantiate + " was found, but no valid instance. Resolve failed.");
				relToResolve.getMngImplementationConstraints().add("(!(name = " + resolved.toInstantiate.getName() + "))") ;
				((RelToResolveImpl)relToResolve).computeFilters();
				return resolveOneManager(manager, relToResolve, mess);
			}

			Composite compo = (source instanceof Instance) ? ((Instance) source).getComposite() : CompositeImpl.getRootInstance();
			Instance inst = resolved.toInstantiate.createInstance(compo, null);
			if (inst == null) { 
				/*
				 *  Failed to be instantiated.
				 *  Flag instantiateFails is turned to "true" in createInstance. 
				 *  Instantiation will not be attempted again on this implem.
				 */
				logger.error(mess + "Failed creating instance. " + resolved.toInstantiate + " turned to non-instantiable.");
				//try to resolve again from beginning but prohibits this implementation (for external managers like OBRMan)
				relToResolve.getMngImplementationConstraints().add("(!(name = " + resolved.toInstantiate.getName() + "))") ;
				((RelToResolveImpl)relToResolve).computeFilters();
				return resolveOneManager(manager, relToResolve, mess);
			}

			if (!relToResolve.matchRelationConstraints(inst)) {
				logger.debug(mess + " Instantiated instance " + inst + " does not match the constraints");
				((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(inst.getName());
				return null ;
			}

			logger.info(mess + "Instantiated " + inst);
			if (relToResolve.isMultiple()) {
				Set<Instance> insts = new HashSet<Instance>();
				insts.add(inst);
				return (Resolved<T>) new Resolved<Instance>(insts);
			} else {
				return (Resolved<T>) new Resolved<Instance>(inst);
			}
		} //end instantiating	

		/*
		 * We have the solution, including the instance if an instance is required.
		 * But because managers can be third party, we cannot trust them. 
		 * We have to check if the result is correct.
		 */
		if (relToResolve.isMultiple()) {
			if (resolved.setResolved == null || resolved.setResolved.isEmpty()) {
				logger.info(mess + "manager " + manager + " returned an empty result. Should be null.");
				return null ;
			}
			if (((Component) resolved.setResolved.iterator().next()).getKind() != relToResolve.getTargetKind()) {
				logger.error(mess + "Manager " + manager + " returned objects of the bad type for relation " + relToResolve);
				return null ;
			}
			logger.info(mess + "Selected : " + resolved.setResolved);
			return resolved;
		}

		// Result is a singleton
		if (resolved.singletonResolved == null) {
			logger.info(mess + "manager " + manager + " returned an empty result. ");
			return null ;
		}
		if (resolved.singletonResolved.getKind() != relToResolve.getTargetKind()) {
			logger.error(mess + "Manager " + manager + " returned objects of the bad type for relation " + relToResolve);
			return null ;
		}
		logger.info(mess + "Selected : " + resolved.singletonResolved);
		return resolved;
	}
	
	
	@Override
	public Instance resolveImpl(Component client, Implementation impl, Set<String> constraints, List<String> preferences) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		@SuppressWarnings("rawtypes")
		// RelToResolve dep = new RelToResolveImpl(new
		// ImplementationReference(impl.getName()), client.getKind(),
		// ComponentKind.INSTANCE, constraints, preferences);
		RelationDefinition dep = new RelationDefinitionImpl(new ImplementationReference(impl.getName()), client.getKind(), ComponentKind.INSTANCE, constraints, preferences);

		Resolved<?> resolve = resolveLink(client, dep);
		if (resolve == null) {
			return null;
		}
		return (Instance) resolve.setResolved;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<Instance> resolveImpls(Component client, Implementation impl, Set<String> constraints) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		@SuppressWarnings("rawtypes")
		// RelToResolve dep = new RelToResolveImpl(new
		// ImplementationReference(impl.getName()), client.getKind(),
		// ComponentKind.INSTANCE, constraints, null);
		RelationDefinition dep = new RelationDefinitionImpl(new ImplementationReference(impl.getName()), client.getKind(), ComponentKind.INSTANCE, constraints, null);

		Resolved<?> resolve = resolveLink(client, dep);
		if (resolve == null) {
			return null;
		}
		return (Set<Instance>) resolve.setResolved;
	}

	/**
	 * The central method for the resolver.
	 */
	@Override
	public Resolved<?> resolveLink(Component source2, RelationDefinition rel) {

		/*
		 * verify the resolver is actually ready to work (all managers are
		 * present)
		 */
		checkEnabled();

		if (source2 == null || rel == null) {
			logger.error("missing client or relation ");
			return null;
		}

		/*
		 * If manual, the resolution must fail silently
		 */
		boolean createManual = rel.getCreation() == CreationPolicy.MANUAL;
		if (createManual) {
			return null;
		}

		Component source = rel.getRelSource(source2);
		if (source == null) {
			logger.error("Component source not at the right level; found " + source2 + " expected " + rel.getSourceKind());
			return null;
		}


		/*
		 *  Creates an relToResolve only considering the relation. Not completely initialized.
		 */
		RelToResolve relToResolve	= new RelToResolveImpl(source, rel);
		Resolved<Component> resolved 		= null;

		/*
		 * If the source is an instance, verify if there is explicit promotions declared in the composite
		 * 
		 *  TODO When an explicit promotion is declared, we perform resolution inside the composite if the
		 *  promotion fails. This is not very intuitive but is more resilient, to discuss which is the good
		 *  specification.
		 */
		if (source instanceof Instance) {
			resolved = checkExplicitPromotion((Instance) source, relToResolve);
		}

		/*
		 * If the source is not an instance or there is no explicit promotion, delegate to managers
		 */
		if (resolved == null) {

			/*
			 * Delegate resolution to managers and update the model
			 */
			resolved = this.resolveByManagers(relToResolve);
			if (resolved != null) {
				updateModel(source, relToResolve, resolved, relToResolve.hasConstraints(),false);
			}
			
			/*
			 * As a last resort try implicit promotion
			 * 
			 * NOTE Notice that we recreate the relation to resolve from the declarations, to be sure
			 * that we ignore all constraints that could be added by the managers during the first try
			 */
			if (resolved == null && source instanceof Instance) {
				resolved = checkImplicitPromotion((Instance) source,new RelToResolveImpl(source,rel));
			}
			else if (resolved != null && source instanceof Instance && relToResolve.isMultiple()) {
				/*
				 * TODO For relations with cardinality multiple, we try to merge all available targets,
				 * so we merge the managers' result with the implicit promotions. This is not very intuitive
				 * but is more resilient, to discuss which is the good specification.
				 */
				resolved = resolved.merge(checkImplicitPromotion((Instance) source, relToResolve));
			}
		}

		return handleFailure(relToResolve,resolved);
	}


	/**
	 * Updates the model to create all the links corresponding to the resolution result
	 */
	private void updateModel(Component source, RelToResolve relation, Resolved<?> resolutionResult, boolean hasConstraints, boolean isPromotion) {
		
		if (resolutionResult == null || resolutionResult.isEmpty()) {
			return;
		}
		
		if (resolutionResult.singletonResolved != null) {
			source.createLink(resolutionResult.singletonResolved, relation, hasConstraints, isPromotion);
		}
		else {
			for (Component target : resolutionResult.setResolved) {
				source.createLink(target, relation, hasConstraints, isPromotion);
			}
		}
	}

	private Resolved<?> handleFailure(RelToResolve relToResolve, Resolved<?> result) {
		/*
		 * If managers could not resolve and relation cannot be promoted, give a
		 * chance to failure manager
		 */
		if (result == null || result.isEmpty()) {
			result = apam.getFailedResolutionManager().resolve(relToResolve);
		}

		/*
		 * If failure manager could not recover, just give up
		 */
		if (result == null || result.isEmpty()) {
			if (relToResolve.getRelationDefinition().getName().isEmpty())
				logger.error("Failed to resolve " + relToResolve.getRelationDefinition().getTarget().getName() + " from " + relToResolve.getLinkSource() );
			else
				logger.error("Failed to resolve " + relToResolve.getRelationDefinition().getTarget().getName() + " from " + relToResolve.getLinkSource() + "(relation " + relToResolve.getRelationDefinition().getName() + ")");
			return null;
		}

		return result;
	}

	/**
	 * An APAM client instance requires to be wired with one or all the
	 * instances that satisfy the relation. WARNING : in case of interface or
	 * message relation , since more than one specification can implement the
	 * same interface, any specification implementing at least the provided
	 * interface (technical name of the interface) will be considered
	 * satisfactory. If found, the instance(s) are bound is returned.
	 * 
	 * @param source
	 *            the instance that requires the specification
	 * @param depName
	 *            the relation name. Field for atomic; spec name for complex
	 *            dep, type for composite.
	 * @return
	 */

	@Override
	public Resolved<?> resolveLink(Component source, String depName) {
		if ((depName == null) || (source == null)) {
			logger.error("missing client or relation name");
			return null;
		}

		// Get the relation
		RelationDefinition relDef = source.getRelation(depName);
		if (relDef == null) {
			logger.error("Relation declaration invalid or not found " + depName);
			return null;
		}
		return resolveLink(source, relDef);
	}

	@Override
	public Implementation resolveSpecByInterface(Component client, String interfaceName, Set<String> constraints, List<String> preferences) {

		RelationDefinition dep = new RelationDefinitionImpl(new InterfaceReference(interfaceName), client.getKind(), ComponentKind.IMPLEMENTATION, constraints, preferences);
		return resolveSpecByResource(client, dep);
	}

	@Override
	public Implementation resolveSpecByMessage(Component client, String messageName, Set<String> constraints, List<String> preferences) {

		RelationDefinition dep = new RelationDefinitionImpl(new MessageReference(messageName), client.getKind(), ComponentKind.IMPLEMENTATION, constraints, preferences);
		return resolveSpecByResource(client, dep);
	}

	/**
	 * First looks for the specification defined by its name, and then resolve
	 * that specification. Returns the implementation that implement the
	 * specification and that satisfies the constraints.
	 * 
	 * @param compoType
	 *            : the implementation to return must either be visible from
	 *            compoType, or be deployed.
	 * @param specName
	 * @param constraints
	 *            . The constraints to satisfy. They must be all satisfied.
	 * @param preferences
	 *            . If more than one implementation satisfies the constraints,
	 *            returns the one that satisfies the maximum number of
	 *            preferences, taken in the order, and stopping at the first
	 *            failure.
	 * @return
	 */
	@Override
	public Implementation resolveSpecByName(Instance client, String specName, Set<String> constraints, List<String> preferences) {
		if (client == null) {
			client = CompositeImpl.getRootInstance();
		}

		RelationDefinition dep = new RelationDefinitionImpl(new SpecificationReference(specName), client.getKind(), ComponentKind.IMPLEMENTATION, constraints, preferences);

		return resolveSpecByResource(client, dep);
	}

	/**
	 * First looks for the specification defined by its interface, and then
	 * resolve that specification. Returns the implementation that implement the
	 * specification and that satisfies the constraints.
	 * 
	 * @param compoType
	 *            : the implementation to return must either be visible from
	 *            compoType, or be deployed.
	 * @param interfaceName
	 *            . The full name of one of the interfaces of the specification.
	 *            WARNING : different specifications may share the same
	 *            interface.
	 * @param interfaces
	 *            . The complete list of interface of the specification. At most
	 *            one specification can be selected.
	 * @param constraints
	 *            . The constraints to satisfy. They must be all satisfied.
	 * @param preferences
	 *            . If more than one implementation satisfies the constraints,
	 *            returns the one that satisfies the maximum number of
	 *            preferences, taken in the order, and stopping at the first
	 *            failure.
	 * @return
	 */
	public Implementation resolveSpecByResource(Component client, RelationDefinition relDef) {
		if (relDef.getTargetKind() != ComponentKind.IMPLEMENTATION) {
			logger.error("Invalid target type for resolveSpecByResource. Implemntation expected, found : " + relDef.getTargetKind());
			return null;
		}
		Resolved<?> resolve = resolveLink(client, relDef);
		if (resolve == null) {
			return null;
		}

		if (resolve.singletonResolved != null) {
			return (Implementation) resolve.singletonResolved;
		}
		return (Implementation) resolve.setResolved.iterator().next();
	}

	@Override
	public void updateComponent(String componentName) {
		Implementation impl = CST.componentBroker.getImpl(componentName);
		if (impl == null) {
			logger.error("Unknown component " + componentName);
			return;
		}
		UpdateMan.updateComponent(impl);
	}

}
