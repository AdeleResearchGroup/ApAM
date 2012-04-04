package fr.imag.adele.apam.core;



/**
 * This class represents a reference to a particular service implementation.
 * 
 * Notice that atomic and composite references will be in the same namespace.
 * 
 * @author vega
 *
 */
public class ImplementationReference<D extends ImplementationDeclaration> extends ComponentReference<D> implements ResolvableReference {

	/**
	 * The namespace associated with implementations
	 */
	private final static Namespace APAM_IMPLEMENTATION = new Namespace() {};
	
	
	
    public ImplementationReference(String name) {
        super(APAM_IMPLEMENTATION,name);
    }

    @Override
    public Type getType() {
    	return Type.IMPLEMENTATION;
	}

    @Override
    public String toString() {
        return "Implementation " + getIdentifier();
    }

}
