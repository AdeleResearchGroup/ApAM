package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.Reporter.Severity;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.PackageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;
import fr.imag.adele.apam.util.ApamFilter;


/**
 * This is the base class of all component validators.
 * 
 * NOTE Notice that this validator is not thread-safe, as it maintain a state for the current validating
 * component, it can not be used concurrently. However, several instances of a validator can be used in
 * parallel, and the same instance can be reused again (after {@link #reset() resetting} it)
 * 
 * Notice also that all validation are performed in a context composed of a repository of component
 * declarations an a classpath of available Java classes. 
 *  
 * @author vega
 *
 */
public abstract class AbstractValidator<D,R> {

	/**
	 * The validation classpath to look for referenced Java classes
	 */
	private final Classpath 				classpath;
	
	/**
	 * The validation context to look for referenced components
	 */
	private final ValidationContext			context;

	/**
	 * Creates a new validator
	 */
	protected AbstractValidator(ValidationContext context, Classpath classpath) {
		this.classpath 	= classpath;
		this.context	= context;
	}

	/**
	 * Create a new children validator.
	 * 
	 * Parent validators can delegate validations to its children in order to validate parts of the
	 * component declaration
	 */
	protected AbstractValidator(AbstractValidator<?,?> parent) {
		this(parent.context,parent.classpath);
		parent.transferStateTo(this);
	}
	
	/**
	 * Get a component from the current validation context
	 */
	protected ComponentDeclaration getComponent(ComponentReference<?> reference, boolean effective) {
		return context.getComponent(reference, effective);
	}

	/**
	 * Get a version of a component from the current validation context
	 */
	protected ComponentDeclaration getComponent(Versioned<?> reference, boolean effective) {
		return context.getComponent(reference, effective);
	}

	/**
	 * Determines if the candidate is an ancestor (or equals) to the component
	 */
	protected boolean isAncestor(ComponentDeclaration component, ComponentReference<?> candidate, boolean orEquals) {
		return context.isAncestor(component, candidate, orEquals);
	}
	
	/**
	 * Determines if a component is a candidate to satisfy a relation
	 */
	protected boolean isCandidateTarget(RelationDeclaration relation, ComponentDeclaration candidate) {
		return context.isCandidateTarget(relation, candidate);
	}

	/**
	 * Checks if a resource exists among the bundles in the context classpath
	 *
	 */
	protected boolean checkResourceExists(ResourceReference resource) {
		
		/*
		 *  Can be unknown if the resource is defined by the type of a generic collection, since it is not
		 *  possible to get the type at compile time
		 */
		
		if (resource == null || resource instanceof UnknownReference) {
			return true;
		}
		
		/*
		 * TODO check that the package is exported by some bundle in the classpath
		 */
		if (resource.as(PackageReference.class) != null) {
			return true;
		}
		
		if (! classpath.contains(resource.getJavaType())) {
			error("the referenced class " + resource.getJavaType() + " does not exist in your build dependencies");
			return false;
		}
		return true;
	}
	
	/**
	 * Get a parsed filter
	 */
	protected ApamFilter parseFilter(String filter) {
		return ApamFilter.newInstance(filter);
	}
	
	
	/**
	 * Validates the given declaration, this optionally may return a result
	 */
	public abstract R validate(D declaration);
	
	/**
	 * Delegates the validation of a nested declaration to another validator, using the 
	 * current state this validator.
	 * 
	 * NOTE notice that a delegate can be reused to validate several nested declarations,
	 * as long as the validations are not performed concurrently.
	 */
	public final <N,S> S validate(N nestedDeclaration, AbstractValidator<N,S> delegate) {
		
		S result = null;
		
		try {
			this.transferStateTo(delegate);
	    	result = delegate.validate(nestedDeclaration);
		} 
		finally {
			delegate.resetState();
		}
		
		return result;
	}
	
	
	/**
	 * The reporter to signal errors
	 */
	private  Reporter 					reporter;
	
	/**
	 * The component currently being validated
	 */
	private ComponentDeclaration		component;

	/**
	 * The effective declaration of the group of the component currently being validated
	 */
	private ComponentDeclaration		group;

	
	/**
	 * Initializes the state for validating a new component declaration
	 */
	protected void startValidation(ComponentDeclaration component, Reporter reporter) {
		this.reporter	= reporter;
		this.component	= component;
		this.group		= context.getComponent(component.getGroupVersioned(), true);
	}

	/**
	 * Initializes the state for validating a new component declaration in the current
	 * context. This is useful to allow validating nested declarations
	 * 
	 * TODO We should handle a stack of currently validating components, and give access
	 * to the stack components to allow context sensitive validation
	 */
	protected void startValidation(ComponentDeclaration component) {
		if (this.component != null && !this.component.equals(component)) {
			this.component	= component;
			this.group		= context.getComponent(component.getGroupVersioned(), true);
		}
	}

	/**
	 * Initializes the validation state of a delegate from the current state
	 */
	protected void transferStateTo(AbstractValidator<?,?> delegate) {
		delegate.reporter	= this.reporter;
		delegate.component	= this.component;
		delegate.group		= this.group;
	}

	/**
	 * Release all references to the current validation state
	 */
	public void resetState() {
		this.reporter	= null;
		this.component	= null;
		this.group		= null;
	}
	
	/**
	 * The component currently validated
	 */
	protected ComponentDeclaration getComponent() {
		return component;
	}

	/**
	 * The group of the component currently validated, if defined and valid
	 */
	protected ComponentDeclaration getGroup() {
		return group;
	}
	
	/**
	 * Signal an error to the current reporter
	 */
	public final void error(String msg) {
		reporter.report(Severity.ERROR,"Error in component "+ quoted(component.getName()) + " : "+ msg);
	}

	/**
	 * Signal a warning to the current reporter
	 */
	public final void warning(String msg) {
		reporter.report(Severity.WARNING,msg);
	}

	/**
	 * Signal an information message to the current reporter
	 */
	public final void info(String msg) {
		reporter.report(Severity.INFO,msg);
	}
	
	/**
	 * Utility methods for messages
	 */
	protected final String quoted(String value) {
		StringBuilder quoted = new StringBuilder();
		quoted.append("\'").append(value).append("\'");
		return quoted.toString();
	}
}
