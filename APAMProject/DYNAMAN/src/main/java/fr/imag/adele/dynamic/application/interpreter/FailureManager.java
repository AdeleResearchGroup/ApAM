package fr.imag.adele.dynamic.application.interpreter;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.dynamic.application.manager.BindingRequest;
import fr.imag.adele.dynamic.application.manager.DynamicApplicationPlatform.Listener;
import fr.imag.adele.dynamic.application.manager.ServiceClassifier;
import fr.imag.adele.sam.Instance;

/**
 * A class to reify the failure management strategies associated with a composite.
 * 
 * 
 * A failure management strategy specifies a filter on the unresolved target services that will be 
 * dynamically bind, and the policy to apply in case of failure.
 * 
 */
public class FailureManager implements Listener {

	private final CompositeServiceInterpreter compositeInterpreter;
	private final ServiceClassifier target;
	private final Policy policy;
	
	/**
	 * The basic failure policies managed by this handler
	 */
	public enum Policy {
		
		
		/**
		 * A simple policy to block the calling thread that has made the request
		 */
		WAIT {
			public void handleRequest(CompositeServiceInterpreter compositeInperpreter, BindingRequest request) {
				compositeInperpreter.getPlatform().waitForResolution(request);
			}
		},		
		
		/**
		 * A simple policy to throw an exception to the calling thread that has made the request
		 */
		FAIL {
			public void handleRequest(CompositeServiceInterpreter compositeInperpreter, BindingRequest request) {
				throw new IllegalArgumentException("unsatisfied binding for "+request.getSource().getName()+":"+request.getDependency());
			}
		},		
		
		
		/**
		 * A simple policy that simply let the flow control continue.
		 * 
		 * NOTE: this ends up with null reference injected in the dependency, the actual behavior depends on
		 * whether the client service test for this condition or not. 
		 */
		IGNORE {
			public void handleRequest(CompositeServiceInterpreter compositeInperpreter, BindingRequest request) {
			}
		};		

		/**
		 * Handle a request failure in the context of the given interpreter
		 */
 		public abstract void handleRequest(CompositeServiceInterpreter compositeInperpreter, BindingRequest request);

	}
	
	
	/**
	 * Creates a new failure manager in charge of handling request failures targeting the specified service class
	 * with the given policy.
	 */
	public FailureManager(CompositeServiceInterpreter compositeInterpreter, ServiceClassifier target, Policy policy) {
		
		assert target != null;
		
		this.compositeInterpreter	= compositeInterpreter;
		this.target					= target;
		this.policy					= policy;
	}
	
	
	public void start() {
		compositeInterpreter.getPlatform().addListener(target,this);
		
	}
	
	public void abort() {
		compositeInterpreter.getPlatform().removeListener(this);
	}
	
	
	/**
	 * This class dosen't handle service appearance, just let APAM perform the usual behavior
	 */
	public void added(Instance instance) {
	}

	/**
	 * This class dosen't handle substitution, just let APAM perform the usual behavior
	 */
	public void removed(ASMInst instance) {
	}

	/**
	 * Apply failure management policy
	 */
	public void bindingFailure(BindingRequest request) {
		policy.handleRequest(compositeInterpreter,request);
	}

}
