package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

/**
 * The common validation for relations defined in abstract components
 *
 * NOTE this class is intended to be sub-classed to specialize the verification
 * in specific kinds of components 
 * 
 * @author vega
 *
 */
public class RelationValidator extends ConstrainedReferenceValidator<RelationDeclaration> {

	public RelationValidator(ComponentValidator<?> parent) {
		super(parent);
	}

	public Void validate(RelationDeclaration relation) {
		super.validate(relation);
		
		validateRefinement();
		validateInstrumentation();
		
		return null;
	}

	
	@Override
	protected void validateTarget() {
		
		/*
		 * For overrides the target may be a wildcard, so we skip validation
		 */
		if (!getRelation().isOverride()) {
			super.validateTarget();
		}
		
		/*
		 *  checking that the targetKind is not higher than the target
		 */
		if (getTarget() != null && getTargetKind().isMoreAbstractThan(getTarget().getKind())) {
			error("Invalid relation " + quoted(getRelation().getIdentifier()) + " target kind "+quoted(getTargetKind().toString())+" is more abstract than the target "+getTarget().getName());
		}
		
	}

	@Override
	protected void validateConstraints() {
		
		super.validateConstraints();
		
		/*
		 * check preferences according to cardinality
		 */
		if (getRelation().isMultiple()) {
			if (!getRelation().getImplementationPreferences().isEmpty() || !getRelation().getInstancePreferences().isEmpty()) {
				error("invalid relation "+quoted(getRelation().getIdentifier())+ " preferences cannot be defined for multiple cardinality");
			}
		}
	}

	/**
	 * Validates if a refinement of a relation defined in the group is allowed
	 * 
	 */
	protected void validateRefinement() {


		if (CST.isFinalRelation(getRelation().getIdentifier())) {
			error("Invalid relation " + quoted(getRelation().getIdentifier()) + " is predefined and cannot be refined");
			return;
		}
		
		if (getGroupRelation() == null) {
			return;
		}

		
		boolean relationTargetDefined	= ! (getTargetReference() instanceof UnknownReference);
		boolean groupTargetDefined 		= ! (getGroupRelation().getTarget() instanceof UnknownReference);

		if (!relationTargetDefined || !groupTargetDefined) {
			return;
		}

		/*
		 * If the targets are defined at several levels they must be compatible
		 */
		
			
		if (getGroupRelation().getTarget() instanceof ResourceReference) {
		
			/*
			 * If the group targets a resource
			 */
			ResourceReference groupTarget = getGroupRelation().getTarget().as(ResourceReference.class);
			
			/*
			 * and the refinement targets a resource it must match
			 */
			if (targetIsResource() && !getTargetReference().equals(groupTarget)) {
				error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " is not compatible with "+groupTarget);
			}
			
			/*
			 * and the refinement targets a component, it must provide the resource
			 */
			if (targetIsComponent() && getTarget() != null && !getTarget().getProvidedResources().contains(groupTarget)) {
				error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " doesn't provide the resource specified in the group "+groupTarget);
			}
		}
		
		if (getGroupRelation().getTarget() instanceof ComponentReference) {
			
			/*
			 * If the group targets a component
			 */
			ComponentDeclaration groupTarget = getComponent(getGroupRelation().getTarget().as(ComponentReference.class),true);

			/*
			 * and the refinement targets a resource, it must be provided by the component
			 */
			if (targetIsResource() && groupTarget != null && !groupTarget.getProvidedResources().contains(getTargetReference())) {
				error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " is not provided by the component specified in the group "+groupTarget.getName());
			}

			if (targetIsComponent() && getTarget() != null && groupTarget != null) {

				/*
				 * and the refinement targets a most concrete component, it must be a descendant
				 */

				if (groupTarget.getKind().isMoreAbstractThan(getTarget().getKind()) && ! isAncestor(getTarget(), groupTarget.getReference(),true)) {
					error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " is not a descendant of the component specified in the group "+groupTarget.getName());
				}
				
				/*
				 * and the refinement targets a most abstract  component, it must be an ancestor
				 */
				if (getTarget().getKind().isMoreAbstractThan(groupTarget.getKind()) && ! isAncestor(groupTarget,getTarget().getReference(),true)) {
					error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " is not a ancestor of the component specified in the group "+groupTarget.getName());
				}

				/*
				 * and the refinement targets a equally abstract  component, it must be the samer
				 */
				if (getTarget().getKind().equals(groupTarget.getKind()) && ! getTarget().equals(groupTarget)) {
					error("Invalid target refinement in relation " + quoted(getRelation().getIdentifier()) + ", "+getTargetReference()+ " is not compatible with the component specified in the group "+groupTarget.getName());
				}
				
			}
			
		}

	}

	/**
	 * Validates if a instrumentation of a relation is valid
	 * 
	 * In general, instrumentation is not allowed at all level of abstraction. This method must
	 * be redefined in subclasses specialized for specific levels of abstraction that support it
	 */
	protected void validateInstrumentation() {
		
		/*
		 * validate callbacks
		 */
		for (RelationDeclaration.Event event : RelationDeclaration.Event.values()) {
			if (getRelation().getCallback(event) != null) {
				error("Invalid relation " + quoted(getRelation().getIdentifier()) + ", cannot specify a "+event+" callback in an abstract component");
			}
		}

		/*
		 * validate instrumented fields and methods
		 */
		if (!getRelation().getInstrumentations().isEmpty()) {
			error("Invalid relation " + quoted(getRelation().getIdentifier()) + ", cannot specify field or method injection in an abstract component");
		}

		/*
		 * validate missing exception
		 */

		if (getRelation().getMissingException() != null) {
			checkResourceExists(new ResourceReference(getRelation().getMissingException()));
		}
		
	}


	/**
	 * This is the group declaration that is refined by the relation being validated
	 */
	private RelationDeclaration groupRelation;

	/**
	 * This is the fully refined declaration corresponding to the relation being validated.
	 * 
	 * NOTE Notice that when there are errors in the refinement this may give a wrong merge
	 * that will lead to some false negatives during validation
	 * 
	 */
	private RelationDeclaration effectiveRelation;
	
	@Override
	protected void initializeState(RelationDeclaration reference) {
		super.initializeState(reference);

		this.groupRelation 		= getGroup() != null ? getGroup().getRelation(getRelation().getIdentifier()) : null;
		this.effectiveRelation	= getGroupRelation() != null ? getGroupRelation().refinedBy(getRelation()) : getRelation();
	}

	@Override
	public void resetState() {
		super.resetState();
		this.groupRelation 		= null;
		this.effectiveRelation	= null;
	}


	/**
	 * The declaration that is being validated
	 */
	protected RelationDeclaration getRelation() {
		return super.getReference();
	}

	/**
	 * The corresponding group declaration
	 */
	protected RelationDeclaration getGroupRelation() {
		return groupRelation;
	}

	/**
	 * The effective declaration after merging with group declaration
	 */
	protected RelationDeclaration getEffectiveRelation() {
		return effectiveRelation;
	}

	/**
	 * The effective target kind
	 */
	protected ComponentKind getTargetKind() {
		ComponentKind effectiveTargetKind 	= getEffectiveRelation().getTargetKind();
		return effectiveTargetKind != null ? effectiveTargetKind : ComponentKind.INSTANCE;
	}

}
	