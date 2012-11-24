package fr.imag.adele.apam.declarations;

public class DependencyPromotion {

	/**
	 * The dependency to be promoted
	 */
	private final DependencyDeclaration.Reference source;
	
	/**
	 * The composite dependency that will be the target of the promotion
	 */
	private final DependencyDeclaration.Reference target;
	
	public DependencyPromotion(DependencyDeclaration.Reference source, DependencyDeclaration.Reference target) {
		this.source = source;
		this.target	= target;
	}
	
	/**
	 * The dependency to be promote
	 */
	public DependencyDeclaration.Reference getContentDependency() {
		return source;
	}
	
	/**
	 * The target of the promotion
	 */
	public DependencyDeclaration.Reference getCompositeDependency() {
		return target;
	}
}
