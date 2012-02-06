package fr.imag.adele.apam.core;

/**
 * This class is the parent of all kind of resource that can be provided by service implementations
 * 
 * @author vega
 *
 */
public abstract class ProvidedResourceReference extends ResourceReference {

    protected ProvidedResourceReference(String name, ResourceType type) {
        super(name, type);
    }

}
