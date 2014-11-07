package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
import fr.imag.adele.apam.maven.plugin.validation.property.PrimitiveType;
import fr.imag.adele.apam.maven.plugin.validation.property.Type;
import fr.imag.adele.apam.util.Attribute;

/**
 * This class handles declarations of property injection and callbacks
 * 
 * @author vega
 *
 */
public class InstrumentedPropertyValidator extends PropertyValidator {

	public InstrumentedPropertyValidator(AtomicComponentValidator parent) {
		super(parent);
	}

	protected void validateRefinement() {
		
		/*
		 * Allow refinement only to add instrumentation information, otherwise the common rules apply
		 */
		if (getProperty().getCallback() == null && getProperty().getField() == null) {
			super.validateRefinement();
			return;
		}

		/*
		 * Do not allow redefining type or default value
		 */
		boolean isPredefined				= Attribute.isFinalAttribute(getProperty().getName());
		PropertyDefinition groupDeclaration = getGroup() != null ? getGroup().getPropertyDefinition(getProperty().getName()) : null;

		if (groupDeclaration != null || isPredefined) {
			
			Type groupType =  isPredefined ? PrimitiveType.STRING : getType(groupDeclaration);
			if ( groupType != null && getType() != null && !getType().equals(groupType)) {
				error("Property " + quoted(getProperty().getName()) + " is already defined, type "+groupType+" cannot be changed");
			}
			
			String groupDefaultValue = isPredefined ? null : groupDeclaration.getDefaultValue();
			if (groupDefaultValue != null && getProperty().getDefaultValue() != null) {
				error("Property " + quoted(getProperty().getName()) + " is already defined, default value cannot be changed");
			}
			
		}
			
	}
	
	protected void validateInstrumentation() {
		
		InstrumentedClass instrumentedClass = getComponent().getImplementationClass();
		
		/*
		 * Verify the callback method has a single parameter of a type compatible with the type of property
		 */
		if (getType() != null && getProperty().getCallback() != null) {

			String method = getProperty().getCallback().trim();

			try {
				boolean hasParameter = instrumentedClass.getMethodParameterNumber(method,true) > 0;
				if (hasParameter) {
					String parameter = instrumentedClass.getMethodParameterType(method,true);
					if (!getType().isAssignableTo(parameter) && !parameter.equals(String.class.getCanonicalName())) {
						error("Property " + quoted(getProperty().getName()) + ", the specified method "+quoted(method)+" must have a parameter of type "+getType());
					}
				}
			}
			catch (NoSuchMethodException undefined) {
				error("Property " + quoted(getProperty().getName()) + ", the specified single-parameter method "+quoted(method)+" is not defined in class "+instrumentedClass.getName());
			}
		}

		/*
		 * Verify the field has a type compatible with the type of property
		 */
		if (getType() != null  && getProperty().getField() != null) {
			
			String field = getProperty().getField().trim();

			try {
				String fieldType = instrumentedClass.getDeclaredFieldType(field);
				if (!getType().isAssignableTo(fieldType)) {
					error("Property " + quoted(getProperty().getName()) + ", the specified field "+quoted(field)+" must have type "+getType());
				}
			}
			catch (NoSuchFieldException undefined) {
				error("Property " + quoted(getProperty().getName()) + ", the specified field "+quoted(field)+" is not defined in class "+instrumentedClass.getName());
			}
			
		}
		
	}
	
	protected AtomicImplementationDeclaration getComponent() {
		return (AtomicImplementationDeclaration) super.getComponent();
	}
	
	@Override
	protected SpecificationDeclaration getGroup() {
		return (SpecificationDeclaration) super.getGroup();
	}

}
