package fr.imag.adele.apam.core;


/**
 * This class represents a reference to an externally defined entity. This allows self-contained declarations
 * that only indirectly reference other entities.
 * 
 * References are organized in namespaces, and are always completely qualified so that they can be
 * used as identifiers.
 * 
 * 
 * @author vega
 * 
 */
public abstract class Reference {

	
	/**
	 * A class to represent different namespaces. 
	 * 
	 * Notice that namespaces must be unique objects, this is not verified but must be enforced by 
	 * concrete subclasses of this base class.
	 */
	protected interface Namespace {
		
	}
	
    /**
     * The identifier of the referenced resource
     */
	protected abstract String getIdentifier();
   
	/**
	 * The namespace associated to the identifier
	 */
    private final Namespace namespace;

    /**
     * Default constructor
     */
    protected Reference(Namespace namespace) {

    	assert namespace != null;
        this.namespace = namespace;
    }

	/**
	 * Cast this reference to a more specific class of references. 
	 * 
	 * Returns null if the cast is not possible
	 */
   @SuppressWarnings("unchecked")
    public <R extends Reference> R as(Class<R> kind) {
	   
    	if (kind.isAssignableFrom(this.getClass()))
    		return (R) this;
    	
    	return null;
    }

   /**
    * Resources are uniquely identified by identifier and namespace.
    * 
    */
    @Override
    public final boolean equals(Object object) {
        if (this == object)
            return true;
        
        if (! (object instanceof Reference))
        	return false;

        Reference that = (Reference)object;
        return (this.namespace == that.namespace) && this.getIdentifier().equals(that.getIdentifier());
    }

    /**
     * Hash code is based on identity
     */
    @Override
    public final int hashCode() {
        return namespace.hashCode() + getIdentifier().hashCode();
    }
    
}
