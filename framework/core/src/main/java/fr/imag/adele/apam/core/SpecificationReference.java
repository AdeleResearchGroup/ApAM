package fr.imag.adele.apam.core;


/**
 * A reference to a specification
 * 
 * @author vega
 *
 */
public class SpecificationReference extends ComponentReference<SpecificationDeclaration>  {

   public SpecificationReference(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return " specification " + getIdentifier();
    }

}
