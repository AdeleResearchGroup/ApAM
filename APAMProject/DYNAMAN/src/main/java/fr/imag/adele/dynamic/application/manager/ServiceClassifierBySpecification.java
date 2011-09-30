package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;

/**
 * A representation of the equivalence class of all services providing a given specification.
 *  
 * @author vega
 *
 */
public class ServiceClassifierBySpecification extends ServiceClassifier {

	
	/**
	 * The specification provided by all services
	 */
	private final String specificationName;
	
	
	public ServiceClassifierBySpecification(String specificationName) {
		assert specificationName != null;
		
		this.specificationName = specificationName;
	}
	
	/**
	 * The specification implemented by all members of this class
	 */
	public String getSpecification() {
		return specificationName;
	}

	/**
	 * Whether the service provides the specification associated to this class
	 */
	public @Override boolean contains(ASMInst instance) {
		
		ASMSpec specification	= instance.getSpec();
		if (specification == null)
			return false;
		
		return specificationName.equals(specification.getName());
	}
	
	/**
	 * Whether the service provides the specification associated to this class
	 */
	public @Override boolean contains(Instance instance) {
		
		Specification specification;
		try {
			specification = instance.getSpecification();
			if (specification == null)
				return false;
			
			return specificationName.equals(specification.getName());
			
		} catch (ConnectionException e) {
			return false;
		}
	}

	/**
	 * This classifier works nominally by the specification name
	 * 
	 */
	public @Override boolean equals(Object object) {
		
		if (object == null)
			return false;
		
		if (!(object instanceof ServiceClassifierBySpecification ))
			return false;
		
		ServiceClassifierBySpecification that = (ServiceClassifierBySpecification)object;
		
		return this.specificationName.equals(that.specificationName);
	}
	
	/**
	 * Adjust hash code to be consistent to the equality definition
	 */
	public @Override int hashCode() {
		return this.specificationName.hashCode();
	}

}
