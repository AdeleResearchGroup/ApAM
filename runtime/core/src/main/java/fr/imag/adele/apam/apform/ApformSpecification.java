package fr.imag.adele.apam.apform;

import fr.imag.adele.apam.declarations.SpecificationDeclaration;

public interface ApformSpecification  extends ApformComponent {
	/**
	 * Get the development model associated with the the specification
	 */
	public SpecificationDeclaration getDeclaration();

}
