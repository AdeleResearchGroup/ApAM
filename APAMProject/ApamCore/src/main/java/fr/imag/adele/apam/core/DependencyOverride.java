package fr.imag.adele.apam.core;


/**
 * The specification of a contextual dependency in a composite, used to override the
 * default behavior specified intrinsically in the dependency
 *
 */
public class DependencyOverride {

	/**
	 * The overridden dependency
	 */
	private final DependencyDeclaration.Reference original;
	
	/**
	 * The new dependency declaration
	 */
	private final DependencyDeclaration replacement;

	/**
	 * The dependency that must be used for promotion
	 */
	private String promotionTarget;
	
	/**
	 * Whether the missing policy is overridden
	 */
	private boolean missingPolicyOverriden;
	
	/**
	 * Whether the constraints overridden
	 */
	private boolean constraintsOverridden;
	
	/**
	 * Whether the preferences overridden
	 */
	private boolean preferencesOverridden;
	
	
	public DependencyOverride(ComponentReference<CompositeDeclaration> composite, DependencyDeclaration.Reference original) {
		
        assert composite != null;
        
		this.original				= original;
		this.replacement			= new DependencyDeclaration(composite,original.getIdentifier(),false, ResourceReference.UNDEFINED);
		
		this.promotionTarget		= null;
		this.missingPolicyOverriden	= false;
		this.constraintsOverridden	= false;
		this.preferencesOverridden	= false;
	}
	
	public DependencyDeclaration.Reference getOriginalDependency() {
		return original;
	}
	
	public DependencyDeclaration getReplacement() {
		return replacement;
	}

	public String getPromotionTarget() {
		return promotionTarget;
	}
	
	public void setPromotionTarget(String promotionTarget) {
		this.promotionTarget = promotionTarget;
	}

	public boolean isMissingPolicyOverridden() {
		return missingPolicyOverriden;
	}
	
	public void setMissingPolicyOverridden(boolean missingPolicyOverriden) {
		this.missingPolicyOverriden = missingPolicyOverriden;
	}

	public boolean areConstraintsOverridden() {
		return constraintsOverridden;
	}
	
	public void setConstraintsOverridden(boolean constraintsOverridden) {
		this.constraintsOverridden = constraintsOverridden;
	}
	
	public boolean arePreferencesOverridden() {
		return preferencesOverridden;
	}
	
	public void setPreferencesOverridden(boolean preferencesOverridden) {
		this.preferencesOverridden = preferencesOverridden;
	}

}
