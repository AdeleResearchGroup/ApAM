package fr.imag.adele.apam.core;



/**
 * This class represents a reference to a particular service implementation.
 * 
 * Notice that atomic and composite references will be in the same namespace.
 * 
 * @author vega
 *
 */
public class ImplementationReference<D extends ImplementationDeclaration> extends ComponentReference<D> {

    public ImplementationReference(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return "Implementation " + getIdentifier();
    }

}
