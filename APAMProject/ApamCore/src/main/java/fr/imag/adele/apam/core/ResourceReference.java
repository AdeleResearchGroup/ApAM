package fr.imag.adele.apam.core;

/**
 * This class represents references to resources that are named using java identifiers,
 * like for example Services and Messages.
 * 
 * We use a single namespace to ensure java identifiers are unique.
 * 
 * @author vega
 *
 */
public abstract class ResourceReference extends Reference implements ResolvableReference {

    private final static Namespace JAVA_RESOURCE = new Namespace() {};

    private final String type;
    private final boolean          defined;

    protected ResourceReference(String type, boolean defined) {
        super(ResourceReference.JAVA_RESOURCE);

        this.type = type;
        this.defined = defined;
    }

    protected ResourceReference(String type) {
        super(ResourceReference.JAVA_RESOURCE);
        defined = true;
        this.type = type;
    }

    public boolean isDefined() {
        return defined;
    }

    /**
     * The java type associated with this resource
     */
    public final String getJavaType() {
        return type;
    }

    @Override
    protected final String getIdentifier() {
        return getJavaType();
    }
}
