package fr.imag.adele.apam.core;


/**
 * This class represent references to APAM components. They are parameterized by the type of component declaration
 * so that they can be statically typed.
 * 
 * Notice that a component reference can in turn be used as namespace for referencing entities locally declared in
 * the scope of the component declaration; 
 * 
 * @author vega
 *
 */
public class ComponentReference <D extends ComponentDeclaration> extends Reference implements Reference.Namespace {

	/**
	 * The global name space associated with all APAM components. 
	 * 
	 * NOTE All APAM declarations share a single name space, this means that even declarations at different
	 * abstraction levels (Specification, Implementation, Instance) must have unique names.
	 */
	private final static Namespace APAM_COMPONENT_NAMESPACE = new Namespace() {};
	
	/**
	 * The name of the component
	 */
	private final String name;

	public ComponentReference(String name) {
		super(APAM_COMPONENT_NAMESPACE);
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
