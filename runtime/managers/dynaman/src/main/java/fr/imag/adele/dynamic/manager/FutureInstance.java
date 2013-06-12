package fr.imag.adele.dynamic.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.RelationImpl;
import fr.imag.adele.apam.util.Substitute;

/**
 * This class represents the declaration of an instance that must be dynamically created, as a result of a triggering
 * condition.
 * 
 * @author vega
 *
 */
public class FutureInstance {

	private final Composite				owner;
	private final Implementation 		implementation;
	private final Map<String,String>	properties;
	private final List<Relation>		triggers;

	private boolean						isTriggered;

	public FutureInstance(Composite owner, InstanceDeclaration declaration) throws InvalidConfiguration {

		this.owner			= owner;
		this.implementation = CST.apamResolver.findImplByName(owner.getMainInst(),declaration.getImplementation().getName());	
		this.properties		= declaration.getProperties();
		this.isTriggered	= false;

		/*
		 *	Verify that the implementation exists 
		 */
		if (implementation == null || ! (implementation instanceof Implementation)) {
			throw new InvalidConfiguration("Invalid instance declaration, implementation can not be found "+declaration.getImplementation().getName());
		}

		/*
		 * Parse the list of triggering conditions
		 */

		int counter = 1;
		triggers = new ArrayList<Relation>();
		for (ConstrainedReference trigger : declaration.getTriggers()) {

			RelationDeclaration triggerRelation = new RelationDeclaration(
					declaration.getReference(), "trigger-" + counter, false, false,
					trigger.getTarget());
			triggerRelation.getImplementationConstraints().addAll(
					trigger.getImplementationConstraints());
			triggerRelation.getInstanceConstraints().addAll(
					trigger.getInstanceConstraints());

			RelationImpl parsedTrigger = new RelationImpl(triggerRelation);
			parsedTrigger.computeFilters(null);
			triggers.add(parsedTrigger);
			counter++;
		}

	}

	/**
	 * Verifies whether all triggering conditions are satisfied, and in that case instantiate the instance in the APAM
	 * state
	 */
	public void checkInstatiation() {

		/*
		 * Verify if this instance has already been triggered, to avoid nested triggers
		 */
		if (isInstantiated())
			return;

		/*
		 * evaluate all triggering conditions
		 */
		boolean satisfied = true;
		for (Relation trigger : triggers) {

			/*
			 * evaluate the specified trigger
			 */
			boolean satisfiedTrigger = false;
			for (Instance candidate : owner.getContainInsts()) {

				/*
				 * ignore non matching candidates
				 */

				ResolvableReference target = trigger.getTarget();

				if (trigger.getTarget() instanceof SpecificationReference && !candidate.getSpec().getDeclaration().getReference().equals(target))
					continue;

				if (trigger.getTarget() instanceof ImplementationReference<?> && !candidate.getImpl().getDeclaration().getReference().equals(target))
					continue;

				if (!candidate.matchRelationConstraints(trigger))
					continue;

				if (!candidate.getImpl().matchRelationConstraints(trigger))
					continue;

				/*
				 * Stop evaluation at first match 
				 */
				satisfiedTrigger = true;
				break;
			}

			/*
			 * stop at the first unsatisfied trigger
			 */
			if (! satisfiedTrigger) {
				satisfied = false;
				break;

			}
		}

		/*
		 * If some trigger is not satisfied, just keep waiting fort all conditions to be satisfied
		 */
		if (! satisfied)
			return;

		/*
		 * Perform property value substitution in the context of the owner composite
		 * 
		 * TODO We should identify in which cases we want to resolve in the context of
		 * the containing composite, and in which cases in the context of the created
		 * instance
		 */
		Map<String, String> evaluatedProperties = new HashMap<String, String>();
		for (Map.Entry<String, String> property : properties.entrySet()) {
			
			String substituted = (String)Substitute.substitute(null,property.getValue(), owner);
			evaluatedProperties.put(property.getKey(),substituted != null ? substituted : property.getValue());
		}
		
		/*
		 * Try to instantiate the specified implementation.
		 * 
		 * TODO BUG We are initializing the properties of the instance, but we lost the relation overrides. 
		 * We need to modify the API to allow specifying explicitly an instance declaration for
		 * Implementation.craeteInstance.
		 */
		isTriggered			= true;
		implementation.createInstance(owner,evaluatedProperties);
		
	}

	/**
	 * Whether this future instance has already bee instantiated
	 */
	public boolean isInstantiated() {
		return isTriggered;
	}

}