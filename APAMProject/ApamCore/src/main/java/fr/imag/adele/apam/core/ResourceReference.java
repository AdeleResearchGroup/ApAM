package fr.imag.adele.apam.core;


/**
 * This class represents a reference to a required or provided resource.
 * 
 * To represent different kinds of resources this class can be extended
 * 
 * @author vega
 * 
 */
public abstract class ResourceReference {

    /**
     * The identifier of the referenced resource
     */
    protected final String name;
    public final ResourceType resourceType;

    public enum ResourceType {
        SPECIFICATION, INTERFACE, MESSAGE, IMPLEMENTATION
    };
    /**
     * Default constructor
     */
    protected ResourceReference(String name, ResourceType type) {
        assert name != null;
        assert type != null;

        this.name = name;
        resourceType = type;
    }

    /**
     * Get the name identifying the referenced resource
     */
    public String getName() {
        return name;
    }

    public boolean isSpecificationReference() {
        return resourceType == ResourceType.SPECIFICATION;
    }

    public boolean isInterfaceReference() {
        return resourceType == ResourceType.INTERFACE;
    }

    public boolean isMessageReference() {
        return resourceType == ResourceType.MESSAGE;
    }

    public boolean isImplementationReference() {
        return resourceType == ResourceType.IMPLEMENTATION;
    }

    /**
     * Resources are uniquely identified by name and type.
     * 
     * We support a different name space for different kinds of resources
     */
    public final boolean equals(ResourceReference that) {
        if (this == that)
            return true;
        //        if (! (object instanceof ResourceReference))
        //            return false;

        //        ResourceReference that = object;
        return (name.equals(that.name) && (resourceType == that.resourceType));
    }

    /**
     * Hash code is based on identity
     */
    @Override
    public final int hashCode() {
        return resourceType.hashCode() + name.hashCode();
    }

}
