package fr.imag.adele.apam.core;


/**
 * A reference to a specification
 * 
 * @author vega
 *
 */
public class SpecificationReference extends ComponentReference<SpecificationDeclaration> implements ResolvableReference {

	/**
	 * The namespace associated with specifications
	 */
	private final static Namespace APAM_SPECIFICATION = new Namespace() {};
	
    public SpecificationReference(String name) {
        super(APAM_SPECIFICATION,name);
    }


    @Override
    public Type getType() {
    	return Type.SPECIFICATION;
	}
    
    @Override
    public String toString() {
        return " specification " + getIdentifier();
    }

}
