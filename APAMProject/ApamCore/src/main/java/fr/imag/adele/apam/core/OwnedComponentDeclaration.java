package fr.imag.adele.apam.core;


public class OwnedComponentDeclaration extends TargetDeclaration {
	
	public OwnedComponentDeclaration(ComponentReference<?> resource) {
		super((ResolvableReference)resource);
	}
	
	/**
	 * The reference to the component of the appearing instance
	 */
	public ComponentReference<?> getComponent() {
		return (ComponentReference<?>) getTarget();
	}
	
}
