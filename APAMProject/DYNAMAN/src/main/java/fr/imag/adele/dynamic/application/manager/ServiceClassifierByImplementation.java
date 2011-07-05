package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

/**
 * A representation of the equivalence class of all services instances of a given implementation.
 *  
 * @author vega
 *
 */
public class ServiceClassifierByImplementation extends ServiceClassifier {

	
	/**
	 * The implementation of all services
	 */
	private final String implementationName;
	
	/**
	 * Whether we should use the logical (ASM) or physical (SAM) name
	 */
	private final boolean isLogicalName;
	
	public ServiceClassifierByImplementation(String implementationName, boolean isLogicalName) {
		
		assert implementationName != null;
		
		this.implementationName = implementationName;
		this.isLogicalName		= isLogicalName;
	}
	
	/**
	 * The implementation of all members of this class
	 */
	public String getImplementation() {
		return implementationName;
	}
	
	/**
	 * Whether the implementation name represents a logical (ASM )or physical (SAM) name
	 */
	public boolean isLogicalName() {
		return isLogicalName;
	}

	/**
	 * Whether the service is an instance of the implementation associated to this class
	 */
	public @Override boolean contains(ASMInst instance) {
		
		ASMImpl implementation	= instance.getImpl();
		if (implementation == null)
			return false;
		
		return implementationName.equals(isLogicalName ? implementation.getASMName() : implementation.getSAMName());
	}
	
	/**
	 * Whether the service is an instance of the implementation associated to this class
	 */
	public @Override boolean contains(Instance instance) {
		
		Implementation implementation;
		try {
			implementation = instance.getImplementation();
			if (implementation == null)
				return false;
			
			return implementationName.equals(implementation.getName());
			
		} catch (ConnectionException ignored) {
			return false;
		}
	}

	/**
	 * This classifier works nominally by the implementation name
	 * 
	 */
	public @Override boolean equals(Object object) {
		
		if (object == null)
			return false;
		
		if (!(object instanceof ServiceClassifierByImplementation ))
			return false;
		
		ServiceClassifierByImplementation that = (ServiceClassifierByImplementation)object;
		
		return	this.implementationName.equals(that.implementationName) &&
				this.isLogicalName == that.isLogicalName;
	}
	
	/**
	 * Adjust hash code to be consistent to the equality definition
	 */
	public @Override int hashCode() {
		return this.implementationName.hashCode() + Boolean.valueOf(this.isLogicalName).hashCode();
	}

}
