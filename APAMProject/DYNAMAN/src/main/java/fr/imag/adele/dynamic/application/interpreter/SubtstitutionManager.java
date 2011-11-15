package fr.imag.adele.dynamic.application.interpreter;

import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.apamImpl.Wire;
import fr.imag.adele.dynamic.application.manager.BindingRequest;
import fr.imag.adele.dynamic.application.manager.DynamicApplicationPlatform.Listener;
import fr.imag.adele.dynamic.application.manager.ServiceClassifier;
import fr.imag.adele.dynamic.application.manager.ServiceClassifierByImplementation;
import fr.imag.adele.dynamic.application.manager.ServiceClassifierBySpecification;
import fr.imag.adele.sam.Instance;

/**
 * A class to reify the substitution management strategies associated with a composite.
 * 
 * 
 * A substitution management strategy specifies a filter on the target services, if a service
 * satisfying this filter is removed from the platform the policy is applied to find a
 * substitute to replace the disappeared instance.
 * 
 */
public class SubtstitutionManager implements Listener {

	private final CompositeServiceInterpreter compositeInterpreter;
	private final ServiceClassifier source;
	private final ServiceClassifier target;
	private final String dependency;
	private final Policy policy;
	
	/**
	 * The basic failure actions managed by this handler
	 */
	public enum Policy {
		
		
		/**
		 * A simple policy that try to substitute by another instance of that respect the same specification
		 */
		STRONG {
			public void substitute(CompositeServiceInterpreter compositeInterpreter, Wire wire) {
				ASMInst source = wire.getSource();
				ServiceClassifier target = new ServiceClassifierBySpecification(wire.getDestination().getSpec().getName());
				compositeInterpreter.getPlatform().resolve(new BindingRequest(source,wire.getDepName(),false,target),false);
			}
		},		
		
		/**
		 * A simple policy that try to substitute by another instance of the same implementation currently
		 * used
		 */
		WEAK {
			public void substitute(CompositeServiceInterpreter compositeInterpreter, Wire wire) {
				ASMInst source = wire.getSource();
				ServiceClassifier target = new ServiceClassifierByImplementation(wire.getDestination().getImpl().getName());
				compositeInterpreter.getPlatform().resolve(new BindingRequest(source,wire.getDepName(),false,target),false);
			}
		};

		/**
		 * Substitutes a wire that will be disappearing according to this policy
		 */
		public abstract void substitute(CompositeServiceInterpreter compositeInterpreter, Wire wire);
	}
	
	
	/**
	 * Creates a new binder in charge of automating wire creation between any source contained in the specified 
	 * source class in the managed composite and any appearing service satisfying the specified target class. 
	 */
	public SubtstitutionManager(CompositeServiceInterpreter compositeInterpreter, ServiceClassifier source, ServiceClassifier target, Policy policy, String dependency) {

		assert source != null && target != null && dependency != null;
		
		this.compositeInterpreter	= compositeInterpreter;
		this.source					= source;
		this.target					= target;
		this.dependency				= dependency;
		this.policy					= policy;
	}
	
	public void start() {
		compositeInterpreter.getPlatform().addListener(target,this);
		
	}
	
	public void abort() {
		compositeInterpreter.getPlatform().removeListener(this);
	}
	
	/**
	 * If an instance concerned by this manager try to substitute all the existing wires that will be
	 * removed 
	 */
	public void removed(ASMInst instance) {
		
		/*
		 * Verify this manager handles the removed target instance
		 */
		if (! target.contains(instance))
			return;
		
		/*
		 * Iterate over all the sources concerned by this handler
		 */
		for (Wire wire : instance.getInvWires(dependency)) {
			
			/*
			 *	Verify this manager handles the removed instance
			 */
			if (! source.contains(wire.getSource()))
				continue;
			
			policy.substitute(compositeInterpreter,wire);
		}
		
	}

	
	/**
	 * This class dosen't handle service appearance, just let APAM perform the usual behavior
	 */
	public void added(Instance instance) {
	}

	/**
	 * This class dosen't handle failure, just let APAM perform the usual behavior
	 */
	public void bindingFailure(BindingRequest request) {
	}



}
