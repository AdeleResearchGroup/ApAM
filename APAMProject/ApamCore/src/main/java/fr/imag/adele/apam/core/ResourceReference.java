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

	/**
	 * A singleton object to represent undefined references
	 */
	public final static ResourceReference UNDEFINED = new ResourceReference("<Unavailable>") {
		
		@Override
		public Type getType() {
			return null;
		}
	};
	
    private final String type;

    protected ResourceReference(String type) {
        super(ResourceReference.JAVA_RESOURCE);
        this.type = type;
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
