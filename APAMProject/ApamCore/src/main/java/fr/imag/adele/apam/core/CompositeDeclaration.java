package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the declaration of a composite implementation
 * 
 * @author vega
 *
 */
public class CompositeDeclaration extends ImplementationDeclaration {

	/**
	 * The main implementation of the composite
	 */
	private final ImplementationReference mainImplementation;
	
	/**
	 * The list of promotions for this composite
	 */
	private final Set<DependencyPromotion> promotions;

	public CompositeDeclaration(String name, SpecificationReference specification, ImplementationReference mainImplementation) {
		super(name, specification);

		assert mainImplementation != null;

		this.mainImplementation = mainImplementation;
		this.promotions			= new HashSet<DependencyPromotion>();
	}

	
	/**
	 * Get the main implementation
	 */
	public ImplementationReference getMainImplementation() {
		return mainImplementation;
	}
	
	/**
	 * Get the declared promotions for dependencies of components inside this composite
	 */
	public Set<DependencyPromotion> getPromotions() {
		return promotions;
	}
}
