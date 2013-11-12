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
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
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

    private boolean isTriggered;

    public FutureInstance(Composite owner, InstanceDeclaration declaration)
	    throws InvalidConfiguration {

	this.owner = owner;
	this.implementation = CST.apamResolver.findImplByName(
		owner.getMainInst(), declaration.getImplementation().getName());
	this.name = declaration.getName();
	this.properties = declaration.getProperties();
	this.isTriggered = false;

	/*
	 * Verify that the implementation exists
	 */
	if (implementation == null
		|| !(implementation instanceof Implementation)) {
	    throw new InvalidConfiguration(
		    "Invalid instance declaration, implementation can not be found "
			    + declaration.getImplementation().getName());
	}

	/*
	 * Parse the list of triggering conditions
	 */

	int counter = 1;
	triggers = new ArrayList<RelToResolve>();
	for (ConstrainedReference trigger : declaration.getTriggers()) {

	    RelationDeclaration triggerRelation = new RelationDeclaration(
		    declaration.getReference(), "trigger-" + counter,
		    trigger.getTarget(), false);
	    triggerRelation.getImplementationConstraints().addAll(
		    trigger.getImplementationConstraints());
	    triggerRelation.getInstanceConstraints().addAll(
		    trigger.getInstanceConstraints());

	    RelToResolveImpl parsedTrigger = new RelToResolveImpl(owner,
		    triggerRelation);
	    // parsedTrigger.computeFilters(owner);
	    triggers.add(parsedTrigger);
	    counter++;
	}

    }

    /**
     * Verifies whether all triggering conditions are satisfied, and in that
     * case instantiate the instance in the APAM state
     */
    public void checkInstatiation() {

	/*
	 * Verify if this instance has already been triggered, to avoid nested
	 * triggers
	 */
	if (isInstantiated()) {
	    return;
	}

	/*
	 * evaluate all triggering conditions
	 */
	boolean satisfied = true;
	for (RelToResolve trigger : triggers) {

	    /*
	     * evaluate the specified trigger
	     */
	    boolean satisfiedTrigger = false;
	    for (Instance candidate : owner.getContainInsts()) {

		/*
		 * ignore non matching candidates
		 */

		ResolvableReference target = trigger.getTarget();

		if (trigger.getTarget() instanceof SpecificationReference
			&& !candidate.getSpec().getDeclaration().getReference()
				.equals(target)) {
		    continue;
		}

		if (trigger.getTarget() instanceof ImplementationReference<?>
			&& !candidate.getImpl().getDeclaration().getReference()
				.equals(target)) {
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
		satisfiedTrigger = true;
		break;
	    }

	    /*
	     * stop at the first unsatisfied trigger
	     */
	    if (!satisfiedTrigger) {
		satisfied = false;
		break;

	    }
	}

	/*
	 * If some trigger is not satisfied, just keep waiting fort all
	 * conditions to be satisfied
	 */
	if (!satisfied) {
	    return;
	}

	String instanceName = owner.isSingleton() ? this.name : owner.getName()
		+ ":" + this.name;
	properties.put("instance.name", instanceName);

	/*
	 * Try to instantiate the specified implementation.
	 * 
	 * TODO BUG We are initializing the properties of the instance, but we
	 * lost the relation overrides. We need to modify the API to allow
	 * specifying explicitly an instance declaration for
	 * Implementation.createInstance.
	 */
	isTriggered = true;
	implementation.createInstance(owner, properties);

    }

    /**
     * The declaring composite
     */
    public Composite getOwner() {
	return owner;
    }

    /**
     * Whether this future instance has already bee instantiated
     */
    public boolean isInstantiated() {
	return isTriggered;
    }

}
