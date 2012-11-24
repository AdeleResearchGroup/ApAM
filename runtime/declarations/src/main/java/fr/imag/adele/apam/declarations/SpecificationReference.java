package fr.imag.adele.apam.declarations;


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
