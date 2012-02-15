package fr.imag.adele.apam.core;

/**
 * This class represents a reference to a particular service implementation
 * 
 * @author vega
 *
 */
public class ImplementationReference extends ResourceReference {

    public ImplementationReference(String name) {
        super(name, ResourceType.IMPLEMENTATION);
    }

    @Override
    public String toString() {
        return "Implementation " + name;
    }

}
