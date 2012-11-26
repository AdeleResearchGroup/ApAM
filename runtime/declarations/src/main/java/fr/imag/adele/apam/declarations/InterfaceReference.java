package fr.imag.adele.apam.declarations;

/**
 * A reference to a provided or required functional interface
 * 
 * @author vega
 *
 */
public class InterfaceReference extends ResourceReference {

    public InterfaceReference(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return "interface " + getIdentifier();
    }

}
