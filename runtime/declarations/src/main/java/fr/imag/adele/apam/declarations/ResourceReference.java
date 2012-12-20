package fr.imag.adele.apam.declarations;


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

	/**
	 * The namespace for all references to resources identified by java class names
	 */
    private final static Namespace JAVA_NAMESPACE = new Namespace() {};	
	
    private final String type;

    protected ResourceReference(String type) {
        super(ResourceReference.JAVA_NAMESPACE);
        this.type = type;
    }

    /**
     * The java type associated with this resource
     */
    public final String getJavaType() {
        return type;
    }

    @Override
    public String getName() {
    	return type;
    }
    
    @Override
    protected final String getIdentifier() {
        return getJavaType();
    }
}
