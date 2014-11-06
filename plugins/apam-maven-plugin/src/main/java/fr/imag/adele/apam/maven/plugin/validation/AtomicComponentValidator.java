package fr.imag.adele.apam.maven.plugin.validation;

import java.util.HashMap;
import java.util.Map;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ProviderInstrumentation;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;

/**
 * This validator inherit the common validations , and adds all the validations
 * regarding instrumentation declared in {@link AtomicImplementationDeclaration}
 * 
 * @author vega
 *
 */
public class AtomicComponentValidator extends ComponentValidator<AtomicImplementationDeclaration> {

	public AtomicComponentValidator(ValidationContext context, Classpath classpath) {
		super(context, classpath);
	}
	
	@Override
	protected void validateProvides() {
		
		super.validateProvides();
		
		/*
		 *	Validate the instrumented provided resources are actually declared as provided  
		 */
		ComponentDeclaration effective = getGroup() != null ? getComponent().getEffectiveDeclaration(getGroup()) : getComponent();
		for (ProviderInstrumentation instrumentation : getComponent().getProviderInstrumentation()) {
			
			ResourceReference instrumented = instrumentation.getProvidedResource();
			
			checkResourceExists(instrumented);
			
			if ( instrumented != null && ! (instrumented instanceof UnknownReference) && !effective.getProvidedResources().contains(instrumented)) {
				error("the return type of the specified method "+quoted(instrumentation.getName())+ " is not one of the provided resources");
			}
		}
	}
	
	@Override
	protected void validateRelations() {
		
		super.validateRelations();
	
		/*
		 * validate fields are not injected several times
		 */
		
		Map<RequirerInstrumentation.InjectedField,RelationDeclaration> injectedFields = new HashMap<RequirerInstrumentation.InjectedField,RelationDeclaration>();
		
		for (RelationDeclaration relation : getComponent().getRelations()) {
			for (RequirerInstrumentation instrumentation : relation.getInstrumentations()) {
				if (instrumentation instanceof RequirerInstrumentation.InjectedField) {
					
					RequirerInstrumentation.InjectedField injectedField = (RequirerInstrumentation.InjectedField) instrumentation;
					if (injectedFields.containsKey(injectedField)) {
						error("invalid relation injection " + relation.getIdentifier() + ", field "+quoted(injectedField.getName())+" is already injected in relation "+quoted(injectedFields.get(injectedField).getIdentifier()));
					}
					
					injectedFields.put(injectedField,relation);
					
				}
			}
		}
	}
	
	@Override
	protected PropertyValidator createPropertyValidator() {
		return new InstrumentedPropertyValidator(this);
	}
	
	@Override
	protected RelationValidator createRelationValidator() {
		return new InstrumentedRelationValidator(this);
	}
	
	@Override
	protected SpecificationDeclaration getGroup() {
		return (SpecificationDeclaration) super.getGroup();
	}
	
}
