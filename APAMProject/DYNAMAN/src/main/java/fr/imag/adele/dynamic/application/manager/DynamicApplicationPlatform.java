package fr.imag.adele.dynamic.application.manager;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.sam.Instance;

public interface DynamicApplicationPlatform {

	/**
	 * A listener for service instance events of the underlying platform
	 */
	public interface Listener {
		
		/**
		 * Event for signaling a new instance was added to the platform. 
		 */
		public void added(Instance instance);
		
		/**
		 * An existing instance of the class expected by the listener was removed from the platform.
		 */
		public void removed(ASMInst instance);
		
		/**
		 * Event for signaling an instance could not be bound to the unresolved target
		 */
		public void bindingFailure(BindingRequest request);
		
	}
	
	/**
	 * Get access to the underlying APAM platform
	 */
	public Apam getApam();
	
	/**
	 * Add a new listener for events in the platform
	 */
	public void addListener(ServiceClassifier serviceClass, Listener listener);
	
	/**
	 * Removes an already registered listener
	 */
	public void removeListener(Listener listener);
	
	/**
	 * Resolves the given request if possible, by creating appropriate wires between the source and a target
	 * satisfying the specified constraints. If specified, it will recursively resolve the selected target. 
	 * 
	 * Unlike the Apam resolution API, this method is intended for private use by the dynamic composite interpreter,
	 * it will silently ignore resolution failures, in order to prevent potential loops. 
	 */
	public void resolve(BindingRequest request, boolean eager);
	
	/**
	 * Resolves the given dependency for the specified source instance. This method builds a binding request 
	 * based on the basic dependency model of the handler associated to the instance, and delegates process
	 * to the resolve(BindingRequest, boolean) method.
	 */
	public void resolve(ASMInst instance, String dependency, boolean eager);
	
	
	/**
	 * Resolves all the dependencies for the specified source instance. This method builds a binding request 
	 * based on the basic dependency model of the handler associated to the instance, and delegates process
	 * to the resolve(BindingRequest, boolean) method.
	 */
	public void resolve(ASMInst instance, boolean eager);
	
	/**
	 * Blocks the specified request until a future resolve satisfies it 
	 * 
	 * IMPORTANT this method has to be called in the context of the thread performing the request. This thread
	 * will be blocked until a resolution creates a wire that satisfies the pending request.
	 */
	public void waitForResolution(BindingRequest request);
	
	/**
	 * Refines a given target by including all additional constraints that the platform want to enforce for
	 * resolution of the given dependency
	 */
	public ServiceClassifier refine(String dependency, ServiceClassifier target);
	
	
	/**
	 * Decides to activate a dynamic physical instance into the platform, this is a global decision that may
	 * involve several composite interpreters and may require knowledge of global platform state
	 */
	public ASMInst activate(Instance instance);

}
