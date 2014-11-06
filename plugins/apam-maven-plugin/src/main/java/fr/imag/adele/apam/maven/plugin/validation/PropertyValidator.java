package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.maven.plugin.validation.property.Type;
import fr.imag.adele.apam.maven.plugin.validation.property.TypeParser;
import fr.imag.adele.apam.util.Attribute;


/**
 * The common validations for property definitions in abstract components
 * 
 * NOTE this class is intended to be sub-classed to specialize the verification
 * in specific kinds of components.
 * 
 * @author vega
 *
 */
public class PropertyValidator extends AbstractValidator<PropertyDefinition,Void> {

	/**
	 * The parser used to validate all defined property types
	 */
	private final TypeParser 					typeParser;
	
	/**
	 * The validator used to validate substitution expressions
	 */
	private final ContextualExpressionValidator expressionValidator;
	
	public PropertyValidator(ComponentValidator<?> parent) {
		super(parent);
		
		this.typeParser				= new TypeParser();
		this.expressionValidator	= new ContextualExpressionValidator(this,typeParser);
	}

	/**
	 * Parses the specified type
	 */
	protected Type getType(PropertyDefinition property) {
		return typeParser.parse(property.getType());
	}

	/**
	 * Validates the property definition
	 */
	public final Void validate(PropertyDefinition property) {
		
		initializeState(property,typeParser.parse(property.getType()));

		validateName();
		validateType();
		validateDefaultValue();

		validateInstrumentation();
		validateRefinement();
		
		return null;
	}

	/**
	 * validates the property values
	 */
	public final void validate(String propertyName, String value) {

		PropertyDefinition property = getComponent().getEffectiveDeclaration(getGroup()).getPropertyDefinition(propertyName);
		Type propertyType 			= property != null ? typeParser.parse(property.getType()) : null;
		
		initializeState(property,propertyType);

		if (Attribute.isFinalAttribute(propertyName)) {
			error("Property " + quoted(propertyName) + " is an internal property and cannot be redefined");
		}
		
		if (getProperty() == null) {
			error("Property " + quoted(propertyName) + " is not defined");
			return;
		}
		
		validateTypedValue(value);
		validateRefinement(value);
	}
	
	/**
	 * Validate allowed property names
	 */
	protected void validateName() {
		if (Attribute.isReservedAttributePrefix(getProperty().getName())) {
			error("Property " + quoted(getProperty().getName()) + " is reserved");
		}
		
	}

	/**
	 * Validate the type is well defined
	 */
	protected void validateType() {
		if (getType() == null) {
			error("Property " + quoted(getProperty().getName()) + " has invalid type "+getProperty().getType());
		}
	}

	/**
	 * Validates the default value has a type compatible with the type of the property
	 * 
	 */
	protected void validateDefaultValue() {
		validateTypedValue(getProperty().getDefaultValue());
	}
	
	/**
	 * Validates that a value (that can have substitution expressions) is compatible with the type
	 * of the property
	 */
	private void validateTypedValue(String value) {
		
		if (value == null)
			return;
		
		/*
		 * If the type could not be determined, we can not perform this validation
		 */
		if (getType() == null)
			return;

			
		if (expressionValidator.isContextualExpression(value)) {
			
			/*
			 * If the value is a substitution expression try to parse and type it.  Verify the type of the expression
			 *  matches the type of the property
			 */
			Type expressionType = validate(value,expressionValidator);
			
			if (expressionType != null && !expressionType.isAssignableTo(getType())) {
				error("Property " + quoted(getProperty().getName()) + " has invalid value "+value+", expected type is "+getType()+" but found "+expressionType);
			}
			
		}
		else {
			
			/*
			 * For normal values, just validates the literal is accepted by the type
			 */
			if (getType().value(value) == null) {
				error("Property " + quoted(getProperty().getName()) + " has invalid value "+value+" for type "+getType());
			}
		}
			
	}

	/**
	 * Validates if a instrumentation of a property is valid
	 * 
	 * In general, instrumentation is not allowed at all level of abstraction. This method must
	 * be redefined in subclasses specialized for specific levels of abstraction that support it
	 */
	protected void validateInstrumentation() {
		if (getProperty().getCallback() != null) {
			error("Property " + quoted(getProperty().getName()) + ", cannot specify a callback for an abstract component");
		}

		if (getProperty().getField() != null) {
			error("Property " + quoted(getProperty().getName()) + ", cannot specify an injection field for an abstract component");
		}
		
	}

	/**
	 * Validates if a refinement of a property defined in the group is allowed
	 * 
	 * In general, this is not allowed at all level of abstraction. This method must be redefined in subclasses
	 * specialized for specific levels of abstraction  that support it
	 */
	protected void validateRefinement() {

		PropertyDefinition groupDeclaration = getGroup() != null ? getGroup().getPropertyDefinition(getProperty().getName()) : null;

		if (Attribute.isFinalAttribute(getProperty().getName())) {
			error("Property " + quoted(getProperty().getName()) + " cannot be redefined");
		}
		else if (groupDeclaration != null && Attribute.isInheritedAttribute(getProperty().getName())) {
			error("Property " + quoted(getProperty().getName()) + " is already defined in component "+getGroup().getName());
		}
	}
	
	/**
	 * Validates if a refinement of a property value in the group is allowed
	 * 
	 */
	protected void validateRefinement(String value) {
		String groupvalue = getGroup() != null ? getGroup().getProperty(getProperty().getName()) : null;
		if (groupvalue != null && value != null && Attribute.isInheritedAttribute(getProperty().getName())) {
			error("Property " + quoted(getProperty().getName()) + " already valued in parent component "+getGroup().getName());
		}
	}

	/**
	 * The property declaration currently validated
	 */
	private PropertyDefinition 	property;

	/**
	 * The type of the property currently validated 
	 */
	private Type				propertyType;

	/**
	 * Initializes the internal state of the validator
	 */
	protected void initializeState(PropertyDefinition property, Type propertType) {
		this.property		= property;
		this.propertyType	= typeParser.parse(getProperty().getType());
	}	
	
	protected PropertyDefinition getProperty() {
		return property;
	}
	
	protected Type getType() {
		return propertyType;
	}

	/**
	 * Frees all references to the state of validation
	 */
	@Override
	public void resetState() {
		
		this.property 		= null;
		this.propertyType 	= null;
		
		super.resetState();
	}
	
}
