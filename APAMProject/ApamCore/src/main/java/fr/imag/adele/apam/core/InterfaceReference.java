package fr.imag.adele.apam.core;

/**
 * A reference to a provided or required functional interface
 * 
 * @author vega
 *
 */
public class InterfaceReference extends ProvidedResourceReference {

    public InterfaceReference(String name) {
        super(name, ResourceType.INTERFACE);
    }

    @Override
    public String toString() {
        return "interface " + name;
    }

}
