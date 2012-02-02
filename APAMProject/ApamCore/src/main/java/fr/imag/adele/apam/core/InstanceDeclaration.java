package fr.imag.adele.apam.core;

/**
 * The declaration of an instance.
 * 
 *  It can include dependency and provided declarations that override the ones in the implementation
 *  
 * @author vega
 *
 */
public class InstanceDeclaration extends ComponentDeclaration {

	
	private final ImplementationReference implementation;
	
	public InstanceDeclaration(ImplementationReference implementation, String name) {
		super(name);
		
		assert implementation != null;
		this.implementation	= implementation;
	}
	
	/**
	 * The implementation of this instance
	 */
	public ImplementationReference getImplementation() {
		return implementation;
	}

}
