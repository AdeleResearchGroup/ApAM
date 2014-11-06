package fr.imag.adele.apam.maven.plugin.validation;

import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;

/**
 * This validator inherit the common validations.
 * 
 * @author vega
 *
 */
public class SpecificationValidator extends ComponentValidator<SpecificationDeclaration> {

	public SpecificationValidator(ValidationContext context, Classpath classpath) {
		super(context, classpath);
	}
	
}
