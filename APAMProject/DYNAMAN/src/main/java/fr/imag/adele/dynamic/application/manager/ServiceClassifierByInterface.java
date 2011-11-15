package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.ASMSpec;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;

/**
 * A representation of the equivalence class of all services implementing a given interface.
 *  
 * @author vega
 *
 */
public class ServiceClassifierByInterface extends ServiceClassifier {

	
	/**
	 * The interface implemented by all services
	 */
	private final String interfaceName;
	
	
	public ServiceClassifierByInterface(String interfaceName) {
		assert interfaceName != null;
		
		this.interfaceName = interfaceName;
	}
	
	/**
	 * The interface implemented by all members of this class
	 */
	public String getInterface() {
		return interfaceName;
	}
	
	/**
	 * Whether the service implements the interface associated to this class
	 */
	public @Override boolean contains(ASMInst instance) {
		
		/*
		 * Validate the specification
		 */
		ASMSpec specification	= instance.getSpec();
		if (specification == null)
			return false;
		
		/*
		 * Validate the list of provided interfaces
		 */
		String provideInterfaces[] = specification.getInterfaceNames();
		if (provideInterfaces == null)
			return false;
		
		for (String provideInterface : provideInterfaces) {
			if (interfaceName.equals(provideInterface))
				return true;
		}
		
		return false;
	}

	/**
	 * Whether the service implements the interface associated to this class
	 */
	public @Override boolean contains(Instance instance) {
		
		/*
		 * Validate the specification
		 */
		Specification specification;
		try {
			specification = instance.getSpecification();

			if (specification == null)
				return false;
			
			/*
			 * Validate the list of provided interfaces
			 */
			String provideInterfaces[] = specification.getInterfaceNames();
			if (provideInterfaces == null)
				return false;
			
			for (String provideInterface : provideInterfaces) {
				if (interfaceName.equals(provideInterface))
					return true;
			}
			
			return false;

		} catch (ConnectionException ignored) {
			return false;
		}
	}

	/**
	 * This classifier works nominally by the interface name
	 */
	public @Override boolean equals(Object object) {
		
		if (object == null)
			return false;
		
		if (!(object instanceof ServiceClassifierByInterface ))
			return false;
		
		ServiceClassifierByInterface that = (ServiceClassifierByInterface)object;
		
		return this.interfaceName.equals(that.interfaceName);
	}
	
	/**
	 * Adjust hash code to be consistent to the equality definition
	 */
	public @Override int hashCode() {
		return this.interfaceName.hashCode();
	}

}
