package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the declaration of a dependency promotion from components inside the composite to
 * its enclosing parent
 * 
 * @author vega
 *
 */
public class DependencyPromotion {
	
	/**
	 * The composite declaration
	 */
	private final CompositeDeclaration composite;
	
	/**
	 * The list of specification identifying the internal components that must be considered for
	 * promotion.
	 */
	private final Set<SpecificationReference> sources;
	
	/**
	 * The dependency defined in the enclosing composite that will be used for promoting the internal
	 * dependency
	 */
	private final DependencyDeclaration dependency;
	
	public DependencyPromotion(CompositeDeclaration composite, DependencyDeclaration dependency) {
		
		assert composite != null;
		assert dependency != null;
		assert dependency.getComponent() == composite;
		
        // bidirectional reference to declaration
		this.composite	= composite;
        this.composite.getPromotions().add(this);
		
        // bidirectional reference to dependency
        assert dependency.getComponent() == composite;
        this.dependency 	= dependency;
        this.dependency.getPromotions().add(this);

		this.sources	= new HashSet<SpecificationReference>();

	}
	
	/**
	 * The dependency of the composite that will be used for this promotion
	 */
	public DependencyDeclaration getDependency() {
		return dependency;
	}
	
	/**
	 * Get the declaring composite
	 */
	public CompositeDeclaration getComposite() {
		return composite;
	}
	
	/**
	 * The list of specifications to filter the internal components that must considered for promotion
	 */
	public Set<SpecificationReference> getSources() {
		return sources;
	}

}
