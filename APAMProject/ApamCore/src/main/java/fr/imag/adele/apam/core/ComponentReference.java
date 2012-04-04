package fr.imag.adele.apam.core;

/**
 * This class represent references to APAM components. They are parameterized by the type of component declaration
 * so that they can be statically typed.
 * 
 * @author vega
 *
 */
public abstract class ComponentReference <D extends ComponentDeclaration> extends Reference {

	/**
	 * The name of the component
	 */
	private final String name;

	protected ComponentReference(Namespace namespace, String name) {
		super(namespace);
		this.name = name;
	}
	
	
	/**
	 * The component name
	 */
	public final String getName() {
		return name;
	}
	
	@Override
	protected final String getIdentifier() {
		return getName();
	};
	
}
