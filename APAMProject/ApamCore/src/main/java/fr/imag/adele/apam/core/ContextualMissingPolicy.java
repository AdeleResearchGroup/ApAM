package fr.imag.adele.apam.core;

/**
 * The specification of a contextual missing policy in a composite, used to override the
 * default behavior specified intrinsically in the dependnecy
 *
 */
public class ContextualMissingPolicy {

	/**
	 * The dependency to override
	 */
	private final DependencyDeclaration.Reference dependency;
	
	/**
	 * The new policy
	 */
	private final MissingPolicy policy;
	
	public ContextualMissingPolicy(DependencyDeclaration.Reference dependency, MissingPolicy policy) {
		this.dependency = dependency;
		this.policy	= policy;
	}
	
	public DependencyDeclaration.Reference getDependency() {
		return dependency;
	}
	
	public MissingPolicy getPolicy() {
		return policy;
	}
}
