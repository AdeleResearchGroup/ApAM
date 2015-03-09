package fr.imag.adele.apam.maven.plugin.validation;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.CallbackDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.InjectedField;
import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
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
		
		InstrumentedClass instrumentedClass = getComponent().getImplementationClass();

		/*
		 * validate callback parameter's type exactly matches the target kind 
		 */
		for (RelationDeclaration.Event event : RelationDeclaration.Event.values()) {
			
			Set<CallbackDeclaration> callbacks = getRelation().getCallback(event);
			if ( callbacks != null) {
				
				for (CallbackDeclaration callback : callbacks) {
					
					try {
						String parameterTypes[] = instrumentedClass.getMethodParameterTypes(callback.getMethodName(), true);
						
						if (parameterTypes.length == 0)
							continue;
						
						if (parameterTypes.length > 1) {
							error("Invalid callback for relation " + quoted(getRelation().getIdentifier()) + ", method "+callback.getName()+ " must have a single parameter");
						}

						InterfaceReference serviceType = new InterfaceReference(parameterTypes[0]);

						if (getTargetKind().isAssignableTo(serviceType.getJavaType())) {
							continue;
						}
						
						if (getTargetKind().equals(ComponentKind.INSTANCE) && getTargetProvidedResources().contains(serviceType)) {
							continue;
						}
						
						String allowedType = getTargetKind().toString();
						if (getTargetKind().equals(ComponentKind.INSTANCE)) {
							allowedType = allowedType + " or one of the following resources "+Util.list(getTargetProvidedResources(),true);
						}
						
						error("Invalid callback for relation " + quoted(getRelation().getIdentifier()) + ", method "+callback.getName()+ " must have a single parameter of type "+allowedType);
						
					} catch (Exception ignored) {
					}
				}
			}
		}

		/*
		 * validate instrumented fields and methods
		 */
		for (RequirerInstrumentation instrumentation : getRelation().getInstrumentations()) {

			/*
			 * validate multiplicity for interface injection
			 * 
			 */
			if ( instrumentation instanceof InjectedField && getRelation().isMultiple() != instrumentation.acceptMultipleProviders()) {
				if (instrumentation.getRequiredResource().as(InterfaceReference.class) != null) {
					error("Invalid relation " + quoted(getRelation().getIdentifier()) + ", the multiplicity of field "+instrumentation.getName()+ " must be "+getRelation().isMultiple());
				}
			}
			
			
			/*
			 * validate the required resource matches the target kind
			 */

			if (instrumentation.getRequiredResource() instanceof UnknownReference) {
				continue;
			}
			
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
