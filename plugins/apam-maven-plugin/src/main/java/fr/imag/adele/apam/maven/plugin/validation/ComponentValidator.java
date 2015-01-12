package fr.imag.adele.apam.maven.plugin.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;
import fr.imag.adele.apam.declarations.tools.Reporter;
import fr.imag.adele.apam.util.Util;

/**
 * This class implements the common validations to all kinds of components
 * 
 * @author vega
 *
 */
public abstract class ComponentValidator<D extends ComponentDeclaration> extends AbstractValidator<D,Void> {

	private final PropertyValidator propertyValidator;
	private final RelationValidator relationValidator;

	protected ComponentValidator(ComponentValidator<?> parent) {
		super(parent);
		
		this.propertyValidator	= createPropertyValidator();
		this.relationValidator	= createRelationValidator();
	}
	
	protected ComponentValidator(ValidationContext context, Classpath classpath) {
		super(context,classpath);
		
		this.propertyValidator	= createPropertyValidator();
		this.relationValidator	= createRelationValidator();
	}

	protected PropertyValidator createPropertyValidator() {
		return new PropertyValidator(this);
	}

	protected RelationValidator createRelationValidator() {
		return new RelationValidator(this);
	}


	/**
	 * Initializes the validation context and state and validates the specified component 
	 */
	public final void validate(D component, Reporter reporter) {
		try {
			startValidation(component,reporter);
			info("Performing validation of component "+component.getName()+ " version "+component.getProperty(CST.VERSION));

			validate(component);
		}
        finally {
        	resetState();
        }
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected D getComponent() {
		return (D) super.getComponent();
	}
	
	/**
	 * Perform the common validation to all kinds of components.
	 * 
	 * NOTE this method is intended to be refined in subclasses to add additional verifications.
	 */
	public Void validate(D component) {
		
		/*
		 * Reinitialize validation state, this is necessary in case of nested declarations
		 */
		startValidation(component);

		/*
		 * Validate the group is available
		 */
        VersionedReference<?> groupReference = component.getGroupVersioned();
        if(groupReference != null && getGroup() == null) {
        	error("group "+groupReference.getName()+" with version "+groupReference.getRange()+" is not available in dependencies or ACR!");
        }
        
        
        validateProperties();

        validateGlobalProperties();

        validateProvides();
        
        validateRelations();

        return null;
	}


	/**
	 * Delegate to property validator checking of properties
	 */
	private void validateProperties() {
		
		Set<String> declared = new HashSet<String>();
		
        for (PropertyDefinition	property : getComponent().getPropertyDefinitions()) {
        	validate(property,propertyValidator);
        	
    		// Checking for unique identifiers
			if (declared.contains(property.getName())) {
				error("property " + property.getName() + " already declared");
				continue;
			}
			
			declared.add(property.getName());
        	
		}
        
        for (Map.Entry<String,String>	property : getComponent().getProperties().entrySet()) {
   			validate(property.getKey(),property.getValue(),propertyValidator);
		}
        
	}


	private void validate(String propertyName, String value, PropertyValidator delegate) {
		try {
			this.transferStateTo(delegate);
	    	delegate.validate(propertyName,value);
		} 
		finally {
			delegate.resetState();
		}
	}

	/**
	 * Checks if the component characteristics : shared, exclusive, instantiable, singleton,
	 * when explicitly defined, are not in contradiction with the group definition.
	 *
	 */
	protected void validateGlobalProperties() {

		if (getGroup() != null && getComponent().isDefinedShared() && getGroup().isDefinedShared() && getComponent().isShared() != getGroup().isShared()) {
			error("The \"Shared\" property is incompatible with the value declared in "+ getGroup().getName());
		}

		if (getGroup() != null && getComponent().isDefinedInstantiable() && getGroup().isDefinedInstantiable() && getComponent().isInstantiable() != getGroup().isInstantiable()) {
			error("The \"Instantiable\" property is incompatible with the value declared in "+ getGroup().getName());
		}

		if (getGroup() != null && getComponent().isDefinedSingleton() && getGroup().isDefinedSingleton() && getComponent().isSingleton() != getGroup().isSingleton()) {
			error("The \"Singleton\" property is incompatible with the value declared in "+ getGroup().getName());
		}
		
	}

	/**
	 * validate provided resources
	 */
	protected void validateProvides() {
		validateProvides(InterfaceReference.class);
		validateProvides(MessageReference.class);
	}

	/**
	 * Validate the provided resources of the specified kind
	 */
	private void validateProvides(Class<? extends ResourceReference> kind) {

		/*
		 * validate all referenced java types are in the classpath
		 */
		Set<? extends ResourceReference> providedResources = getComponent().getProvidedResources(kind);
		for (ResourceReference providedResource : providedResources) {
			checkResourceExists(providedResource);
		}
		
		/*
		 * validate consistency with group 
		 */
		if (getGroup() != null) {

			Set<? extends ResourceReference> expected	= getGroup().getProvidedResources(kind);
			Set<? extends ResourceReference> declared	= providedResources;
			Set<? extends ResourceReference> unknown	= unknownProvided(kind);
			
			if (!declared.isEmpty() && !declared.containsAll(expected)) {
				if (!unknown.isEmpty()) {
					warning("Unable to verify type of provided resources at compilation time, this may cause errors at the runtime!"
							+ "\n make sure that "+ Util.list(unknown,true) + " are of the following types "+ Util.list(expected,true));
				} else {
					error("Must provide resources " + Util.list(expected,true));
				}
			}

		}
		
		
	}

	/**
	 * Delegate to relation validator checking of relations
	 */
	protected void validateRelations() {
		
		Set<String> declared = new HashSet<String>();
		
        for (RelationDeclaration relation : getComponent().getRelations()) {
        	validate(relation,relationValidator);
    		
        	// Checking for unique identifiers
			if (declared.contains(relation.getIdentifier())) {
				error("relation " + relation.getIdentifier() + " already declared");
				continue;
			}
			
			declared.add(relation.getIdentifier());
 		}
	}
	
	/**
	 * Get the set of unknown provided resources of the specified kind
	 */
	protected <R extends ResourceReference> Set<R> unknownProvided(Class<R> kind) {
		
		Set<R> result = new HashSet<R>();
		for (UnknownReference unknown : getComponent().getProvidedResources(UnknownReference.class)) {
			
			R cast = unknown.as(kind);
			if (cast != null) {
				result.add(cast);
			}
		}

		return result;
	}
	
	
}
