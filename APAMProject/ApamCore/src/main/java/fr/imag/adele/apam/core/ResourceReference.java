package fr.imag.adele.apam.core;

/**
 * This class represents a reference to a required or provide resource.
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
	
	
	/**
	 * Default constructor
	 */
	protected ResourceReference(String name) {
		assert name != null;
		
		this.name = name;
	}
	
	/**
	 * Get the name identifying the referenced resource
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Resources are uniquely identified by name.
	 * 
	 * TODO Maybe we should support an optional name space for different kinds
	 * of resources
	 */
	@Override
	public final boolean equals(Object object) {
		if (this == object)
			return true;
		
		if (! (object instanceof ResourceReference))
			return false;
		
		ResourceReference that = (ResourceReference) object;
		
		return this.name.equals(that.name); 
		
	}
	
	/**
	 * Hash code is based on identity
	 */
	@Override
	public final int hashCode() {
		return this.name.hashCode();
	}
	
}
