package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.sam.Instance;

/**
 * This class is used to reify a runtime binding request.
 * 
 * @author vega
 *
 */
public class BindingRequest {

	/**
	 * The source of the binding request
	 */
	private final Instance source;
	
	
	/**
	 * The dependency to resolve
	 */
	private final String dependency;
	
	/**
	 * The class of target services
	 */
	private final ServiceClassifier target;
	
	
	/**
	 * Whether this request is for multiple instances
	 */
	private final boolean isAggregate;
	
	/**
	 * Builds a new binding request reification
	 */
	public BindingRequest(Instance source, String dependency, boolean isAggregate, ServiceClassifier target) {
		
		assert source != null && dependency != null && target != null;
		
		this.source			= source;
		this.target			= target;
		this.dependency		= dependency;
		this.isAggregate	= isAggregate;
	}
	
	/**
	 * The source of the unresolved request
	 */
	public Instance getSource() {
		return source;
	}
	
	/**
	 * The required dependency of the binding
	 */
	public String getDependency() {
		return dependency;
	}
	
	/**
	 * Whether this is a request for multiple resolution
	 */
	public boolean isAggregate() {
		return isAggregate;
	}
	
	/**
	 * Decides whether the specified service could resolve this request
	 */
	public boolean isSatisfiedBy(Instance candidate) {
		return target.contains(candidate);
	}
	
	/**
	 * Decides whether the specified service could resolve this request
	 */
	public boolean isSatisfiedBy(Instance candidate) {
		return target.contains(candidate);
	}

	/**
	 * The service class to which the resolution of this request must belong
	 */
	public ServiceClassifier getTarget() {
		return target;
	}
	
	
	/**
	 * Redefines equality
	 */
	@Override
	public boolean equals(Object object) {

		if (object == this)
			return true;

		if (object == null)
			return false;

		if (!(object instanceof BindingRequest ))
			return false;
		
		BindingRequest that = (BindingRequest)object;
		
		return	this.source.equals(that.source) &&
				this.dependency.equals(that.dependency) &&
				this.target.equals(that.target);
	}
	
	/**
	 * Adjust hash code to be consistent to the equality definition
	 */
	public @Override int hashCode() {
		return this.source.hashCode()+this.dependency.hashCode()+this.target.hashCode();
	}

}
