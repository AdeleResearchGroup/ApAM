package fr.imag.adele.apam.core;

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

    public InterfaceReference(String name, boolean defined) {
        super(name, defined);
    }

    @Override
    public Type getType() {
        return Type.INTERFACE;
    }   

    @Override
    public String toString() {
        return "interface " + getIdentifier();
    }

}
