package fr.imag.adele.apam.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class represents the declaration of an instance that must be dynamically
 * created, as a result of a triggering condition.
 * 
 * @author vega
 * 
 */
public class FutureInstance {

	private final Composite owner;
	private final Implementation implementation;
	private final String name;
	private final Map<String, String> properties;
	private final List<RelToResolve> triggers;

	private boolean isInstantiated;

	public FutureInstance(Composite owner, InstanceDeclaration declaration) throws InvalidConfiguration {

		this.owner = owner;
		this.implementation = CST.apamResolver.findImplByName(owner.getMainInst(), declaration.getImplementation().getName());
		this.name = declaration.getName();
		this.properties = declaration.getProperties();
		this.isInstantiated = false;

		/*
		 * Verify that the implementation exists
		 */
		if (implementation == null || !(implementation instanceof Implementation)) {
			throw new InvalidConfiguration("Invalid instance declaration, implementation can not be found " + declaration.getImplementation().getName());
		}

		/*
		 * Parse the list of triggering conditions
		 */

		int counter = 1;
		triggers = new ArrayList<RelToResolve>();
		for (ConstrainedReference trigger : declaration.getTriggers()) {

			RelationDeclaration triggerRelation = new RelationDeclaration(declaration.getReference(), "trigger-" + counter, trigger.getTarget(), false);
			triggerRelation.getImplementationConstraints().addAll(trigger.getImplementationConstraints());
			triggerRelation.getInstanceConstraints().addAll(trigger.getInstanceConstraints());

			RelToResolveImpl parsedTrigger = new RelToResolveImpl(owner, triggerRelation);
			// parsedTrigger.computeFilters(owner);
			triggers.add(parsedTrigger);
			counter++;
		}

	}

	/**
	 * Whether this future instance has already been instantiated
	 * 
	 * TODO NOTE currently we only try to instantiate the instance once. If the created instance is destroyed
	 * later, we will not try to create it again. We need to specify the expected semantics in this case.
	 */
	public synchronized boolean isInstantiated() {
		return isInstantiated;
	}

	/**
	 * Verifies whether all triggering conditions are satisfied
	 */
	public boolean isInstantiable() {

		/*
		 * Verify if this instance has already been triggered, to avoid nested trigger evaluation
		 */
		if (isInstantiated()) {
			return true;
		}

		/*
		 * evaluate all triggering conditions
		 * 
		 * NOTE notice that evaluation of the triggering condition of a future instance may be performed
		 * in parallel by several threads. Currently we do not try to optimize this situation, however we
		 * synchronize the actual instantiation.
		 */
		boolean satisfied = true;
		EVALUATE_CONDITIONS : for (RelToResolve trigger : triggers) {

			/*
			 * evaluate the specified trigger
			 */
			boolean matchingCandidate = false;
			SEARCH_MATCHING_CANDIDATE : for (Instance candidate : owner.getContainInsts()) {

				/*
				 * ignore non matching candidates
				 */

				ResolvableReference target = trigger.getTarget();

				if (trigger.getTarget() instanceof SpecificationReference && !candidate.getSpec().getDeclaration().getReference().equals(target)) {
					continue;
				}

				if (trigger.getTarget() instanceof ImplementationReference<?> && !candidate.getImpl().getDeclaration().getReference().equals(target)) {
					continue;
				}

				if (!candidate.matchRelationConstraints(trigger)) {
					continue;
				}

				if (!candidate.getImpl().matchRelationConstraints(trigger)) {
					continue;
				}

				/*
				 * Stop evaluation at first match
				 */
				matchingCandidate = true;
				break SEARCH_MATCHING_CANDIDATE;
			}

			/*
			 * stop at the first unsatisfied trigger
			 */
			if (!matchingCandidate) {
				satisfied = false;
				break EVALUATE_CONDITIONS;

			}
		}

		return satisfied;
	}

	/**
	 * Instantiate the corresponding instance
	 */
	public void instantiate() {

		/*
		 * If some trigger is not satisfied, just keep waiting for all conditions to be satisfied
		 */
		if (!isInstantiable()) {
			return;
		}

		/*
		 * synchronize to avoid multiple simultaneous instantiations, we optimistically assume that
		 * instantiation will succeed if the triggers are satisfied
		 */
		synchronized (this) {
			if (isInstantiated) {
				return;
			}
			
			isInstantiated = true;
		}
		

		/*
		 * Try to instantiate the specified implementation.
		 * 
		 * TODO BUG We are initializing the properties of the instance, but we lost the relation overrides.
		 * 
		 * We need to modify the API to allow specifying explicitly an instance declaration for 
		 * Implementation.createInstance.
		 * 
		 * NOTE notice that we perform instantiation outside any synchronization, because it may block
		 * waiting for dependencies
		 */
		
		Instance instance = null;
		
		try {
			String instanceName = owner.isSingleton() ? this.name : owner.getName() + ":" + this.name;
			properties.put("instance.name", instanceName);
			instance = implementation.createInstance(owner, properties);
		}
		catch (Exception unrecoverableError ) {
		}
		
		/*
		 * check the actual outcome of the instantiation
		 */
		synchronized (this) {
			isInstantiated = (instance != null);
		}
	}

	/**
	 * The declaring composite
	 */
	public Composite getOwner() {
		return owner;
	}

}
