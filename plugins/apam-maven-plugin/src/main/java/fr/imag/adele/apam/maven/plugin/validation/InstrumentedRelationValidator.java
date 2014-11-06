package fr.imag.adele.apam.maven.plugin.validation;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.util.Util;

public class InstrumentedRelationValidator extends RelationValidator {

	public InstrumentedRelationValidator(AtomicComponentValidator parent) {
		super(parent);
	}

	/**
	 * Validates if a instrumentation of a relation is valid
	 * 
	 */
	protected void validateInstrumentation() {
		

		/*
		 * validate callback parameters
		 */
		for (RelationDeclaration.Event event : RelationDeclaration.Event.values()) {
			if (getRelation().getCallback(event) != null) {
				
				/*
				 * TODO validate the parameter corresponds to the target kind, and in case of  instance service
				 * injection it is in the allowed references
				 */
			}
		}

		/*
		 * validate instrumented fields and methods
		 */
		for (RequirerInstrumentation instrumentation : getRelation().getInstrumentations()) {
			
			if (instrumentation.getRequiredResource() instanceof UnknownReference) {
				continue;
			}
			
			/*
			 * validate the required resource matches the target kind
			 */
			
			if (getTargetKind().isAssignableTo(instrumentation.getRequiredResource().getJavaType())) {
				continue;
			}
			
			if (getTargetKind().equals(ComponentKind.INSTANCE) && getTargetProvidedResources().contains(instrumentation.getRequiredResource())) {
				continue;
			}
			
			String allowedType = "apam component "+getTargetKind();
			if (getTargetKind().equals(ComponentKind.INSTANCE)) {
				allowedType = allowedType + " or one of its provided resources "+Util.list(getTargetProvidedResources(),true);
			}
			error("Invalid relation " + quoted(getRelation().getIdentifier()) + ", the type of field or method "+instrumentation.getName()+ " must be "+allowedType);
			
		}

		/*
		 * validate missing exception
		 */
		if (getRelation().getMissingException() != null) {
			checkResourceExists(new ResourceReference(getRelation().getMissingException()));
		}
		
	}

	/**
	 * The provided resources of the target
	 */
	private Set<ResourceReference> targetProvides;

	@Override
	protected void initializeState(RelationDeclaration reference) {
		super.initializeState(reference);
		
		this.targetProvides = new HashSet<ResourceReference>();
		
		if (targetIsResource()) {
			targetProvides.add(getTargetReference(ResourceReference.class));
		} 
		else if (getTarget() != null) {
			targetProvides.addAll(getTarget().getProvidedResources());
			if (getTarget() instanceof AtomicImplementationDeclaration) {
				targetProvides.add(new InterfaceReference(((AtomicImplementationDeclaration)getTarget()).getClassName()));
			}

		} 
	}

	protected Set<ResourceReference> getTargetProvidedResources() {
		return targetProvides;
	}

	
	@Override
	public void resetState() {
		super.resetState();
		
		this.targetProvides = null;
	}
	
	protected AtomicImplementationDeclaration getComponent() {
		return (AtomicImplementationDeclaration) super.getComponent();
	}
	
	@Override
	protected SpecificationDeclaration getGroup() {
		return (SpecificationDeclaration) super.getGroup();
	}

}
