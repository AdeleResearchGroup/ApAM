package fr.imag.adele.apam.maven.plugin.validation;

import java.util.Collection;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.references.Reference;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.maven.plugin.validation.property.CollectionType;
import fr.imag.adele.apam.maven.plugin.validation.property.Type;
import fr.imag.adele.apam.maven.plugin.validation.property.TypeParser;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Attribute;

/**
 * The common validation for constrained referenced defined in components
 *
 * @author vega
 *
 */
public class ConstrainedReferenceValidator<R extends ConstrainedReference> extends AbstractValidator<R,Void> {

	/**
	 * The parser used to validate all defined property types
	 */
	private final TypeParser 					typeParser;

	/**
	 * The validator used to validate substitution expressions
	 */
	private final ContextualExpressionValidator expressionValidator;
	
	public ConstrainedReferenceValidator(ComponentValidator<?> parent) {
		super(parent);

		this.typeParser				= new TypeParser();
		this.expressionValidator	= new ContextualExpressionValidator(this,typeParser);
	}

	public Void validate(R reference) {
		initializeState(reference);
		
		validateTarget();
		validateConstraints();

		return null;
	}

	/**
	 * validate the target of the reference
	 */
	protected void validateTarget() {
		
		if (getTargetReference() == null) {
			return;
		}
		else if (targetIsComponent() && getTarget() == null) {
			error("the specified target component "+getTargetReference().getName()+ " is not available");
		}
		else if (targetIsResource()) {
			checkResourceExists(getTargetReference(ResourceReference.class));
		}
	}

	/**
	 * validate the associated constraints
	 */
	protected void validateConstraints() {
		
		validate(getReference().getImplementationConstraints(),"error in Implementation constraints");
		validate(getReference().getImplementationPreferences(),"error in Implementation preferences");
		validate(getReference().getInstanceConstraints(),"error in Instance constraints");
		validate(getReference().getInstancePreferences(),"error in Instance preferences");
	}

	/**
	 * Parses the specified type
	 */
	protected Type getType(PropertyDefinition property) {
		return typeParser.parse(property.getType());
	}
	
	/**
	 * Validates the specified filter collection
	 */
	private void validate(Collection<String> filters, String message) {
		for (String filter : filters) {
			validate(filter,message);
		}
	}

	/**
	 * Validates the specified filter
	 */
	private void validate(String filter, String message) {
		
		/*
		 * validate syntax
		 */
		ApamFilter expression = parseFilter(filter);
		if (expression == null) {
			error(message+", invalid filter syntax "+quoted(filter));
			return;
		}
		
		/*
		 * validate typing
		 */
		validate(expression,message);
	}
	
	/**
	 * Validates the specified filter
	 */
	private void validate(ApamFilter expression, String message) {
		initializeFilter(expression);
		switch (expression.op) {
			
			case ApamFilter.AND:
			case ApamFilter.OR:
				
				/*
				 * validate filters of sub-expression
				 */
				for (ApamFilter subExpression : ((ApamFilter[]) expression.value)) {
					validate(subExpression,message);
				}
				break;

			case ApamFilter.NOT:
				/*
				 * validate filters of sub-expression
				 */
				validate((ApamFilter)expression.value,message);
				break;
				
			case ApamFilter.SUBSTRING:
			case ApamFilter.EQUAL:
			case ApamFilter.GREATER:
			case ApamFilter.LESS:
			case ApamFilter.APPROX:
			case ApamFilter.SUBSET:
			case ApamFilter.SUPERSET:
			case ApamFilter.PRESENT:
				/*
				 * validate attribute comparison expression
				 */
				validate(expression.op,expression.attr,getValue(expression),message);
				break;
		}
		resetFilter();
	}

	/**
	 * Validates the specified attribute comparison expression is valid
	 */
	private void validate(int operation, String propertyName, String value, String message) {

		if (targetIsResource()) {
			warning(message+", substitute expression "+quoted(filter.toString())+ " cannot be completely validated at build-time");
		}

		/*
		 * Validate property is well defined
		 */
		PropertyDefinition property = getTarget() != null ? getTarget().getPropertyDefinition(propertyName) : null;
		
		if (getTarget() != null) {
			
			if (property == null && !Attribute.isFinalAttribute(propertyName)) {
				error(message+", invalid filter "+quoted(filter.toString())+", the property "+quoted(propertyName)+ " is not defined in component "+getTarget().getName());
			}
			
			if (property != null && property.getDefaultValue() != null && expressionValidator.isContextualExpression(property.getDefaultValue())) {
				error(message+", invalid filter "+quoted(filter.toString())+", the property "+quoted(propertyName)+ " can not be used in filters because it is a substitution "+quoted(property.getDefaultValue()));
			}
		}

		/*
		 * check value is valid with respect to the type of the property 
		 */
		
		if (value == null)
			return;

		Type propertyType 	= property != null ? getType(property) : null;

		if (expressionValidator.isContextualExpression(value)) {
			
			/*
			 * If the value is a substitution expression try to parse and type it.  Verify the type of the
			 * expression is comparable to the type of the property
			 * 
			 * TODO We need to validate what are the automatic conversions performed by the filter at runtime 
			 */
			Type expressionType	= validate(value,expressionValidator);
			
			if (expressionType != null && propertyType != null && 
				!expressionType.isAssignableTo(propertyType) && !propertyType.isAssignableTo(expressionType)) {
				error(message+", invalid filter "+quoted(filter.toString())+", the property "+quoted(propertyName)+ " has type "+propertyType+" and the value "+quoted(value)+" has type "+expressionType);
			}
			
		}
		else {

			/*
			 * Operators involving sets perform automatic conversion of the property to a collection 
			 */
			if (operation == ApamFilter.SUBSET || operation == ApamFilter.SUPERSET) {
				if (propertyType != null && !(propertyType instanceof CollectionType)) {
					propertyType = new CollectionType(propertyType, false);
				} 
			}
			
			/*
			 * If the property is a collection, the equality operator is overloaded to mean set containment
			 */
			if (operation == ApamFilter.EQUAL && propertyType != null && propertyType instanceof CollectionType) {
				propertyType = ((CollectionType)propertyType).getElementType();
			}

			
			/*
			 * For normal values, just validates the literal is accepted by the type
			 */
			if (propertyType != null && propertyType.value(value) == null) {
				error(message+", invalid filter "+quoted(filter.toString())+", the value "+quoted(value)+" is not valid for the expected type "+quoted(propertyType.getName()));
			}
		}

	}


	/**
	 * The declaration being validated
	 */
	private R reference;

	/**
	 * The target component, if defined
	 */
	private ComponentDeclaration target;
	
	/**
	 * The filter being validated
	 */
	private ApamFilter filter;
	
	/**
	 * Initializes the state of this validator
	 */
	protected void initializeState(R reference) {
		this.reference = reference;
		
		/*
		 * try to load the target component, if any
		 */
		this.target		= getComponent(getTargetReference(ComponentReference.class),true);
		
	}

	protected void initializeFilter(ApamFilter filter) {
		this.filter = filter;
	}
	
	
	/**
	 * The constrained reference declaration that is being validated
	 */
	protected R getReference() {
		return reference;
	}
	
	/**
	 * The referenced target
	 */
	protected ResolvableReference getTargetReference() {
		return getReference().getTarget();
	}

	/**
	 * The referenced target cast to the appropriate type
	 */
	protected <T extends Reference> T getTargetReference(Class<T> kind) {
		return kind.isInstance(getTargetReference()) ? kind.cast(getTargetReference()) : null;
	}

	/**
	 * Determines if the target is a resource
	 */
	protected boolean targetIsResource() {
		return getTargetReference() != null && getTargetReference(ResourceReference.class) !=null;
	}
	
	/**
	 * Determines if the target is a resource
	 */
	protected boolean targetIsComponent() {
		return getTargetReference() != null && getTargetReference(ComponentReference.class) !=null;
	}
	
	
	/**
	 * The component target of the current declaration, if one is explicitly defined and is available
	 * on the repository
	 */
	protected ComponentDeclaration getTarget() {
		return target;
	}
	

	public void resetFilter() {
		this.filter = null;
	}
	
	/**
	 * Frees all references to the state of validation
	 */
	@Override
	public void resetState() {
		this.reference 	= null;
		super.resetState();
	}

	/**
	 * Get the encoded value of an attribute in a comparison expression.
	 */
	private static String getValue(ApamFilter expression) {
		/*
		 * For operations not involving a pattern just return the parsed value
		 */
		if (expression.op != ApamFilter.SUBSTRING)
			return (String) expression.value;
		/*
		 * for pattern matching rebuild the pattern from the parsed values
		 */
		String[] substrings = (String[]) expression.value;
		StringBuilder pattern = new StringBuilder();
		for (String substr : substrings) {
			if (substr == null) /* wildcard */{
				pattern.append('*');
			} else /* text */{
				pattern.append(ApamFilter.encodeValue(substr));
			}
		}
		return pattern.toString();
	}

}
