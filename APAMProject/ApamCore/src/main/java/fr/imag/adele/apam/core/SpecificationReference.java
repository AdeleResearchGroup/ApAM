package fr.imag.adele.apam.core;

/**
 * A reference to a required specification
 * 
 * @author vega
 *
 */
public class SpecificationReference extends ResourceReference {

    public SpecificationReference(String name) {
        super(name, ResourceType.SPECIFICATION);
    }

    @Override
    public String toString() {
        return " specification " + name;
    }

}
