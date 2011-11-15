package fr.imag.adele.dynamic.application.interpreter;

import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.dynamic.application.manager.BindingRequest;
import fr.imag.adele.dynamic.application.manager.DynamicApplicationPlatform.Listener;
import fr.imag.adele.dynamic.application.manager.ServiceClassifier;
import fr.imag.adele.sam.Instance;

/**
 * A class to reify the dynamic binding policies associated with a composite.
 * 
 * 
 * A dynamic binding policy specifies a filter on the source and target services that will be 
 * dynamically bind, and the dependency that will be handled.
 *
 */
public class DynamicBinder implements Listener {

	private final CompositeServiceInterpreter	compositeInterpreter;
	private final ServiceClassifier source;
	private final ServiceClassifier target;
	private final String dependency;
	
	/**
	 * Creates a new binder in charge of automating wire creation between any source contained in the specified 
	 * source class in the managed composite and any appearing service satisfying the specified target class. 
	 */
	public DynamicBinder(CompositeServiceInterpreter compositeInterpreter, ServiceClassifier source, ServiceClassifier target, String dependency) {
		
		assert source != null && target != null && dependency != null;
		
		this.compositeInterpreter	= compositeInterpreter;
		this.source					= source;
		this.dependency				= dependency;
		this.target					= compositeInterpreter.getPlatform().refine(dependency, target);
	}
	
	public void start() {
		compositeInterpreter.getPlatform().addListener(target,this);
		
	}
	
	public void abort() {
		compositeInterpreter.getPlatform().removeListener(this);
	}
	
	/**
	 * Gets an estimate of the number of bindings that can be resolved by this binder if the specified 
	 * instance is activated. 
	 */
	public int getEstimatedBindingCount(Instance instance) {

		int count = 0;

		if (!target.contains(instance))
			return count;
		
		for (ASMInst sourceComposite : compositeInterpreter.getCompositeType().getInsts()) {
			
			for (ASMInst sourceInstance : ((Composite) sourceComposite).getContainInsts()) {
				
				if (source.contains(sourceInstance))
					count++;
			}
		}
		
		return count;
	}

	/**
	 * Handle appearance of a conforming target service
	 */
	public void added(Instance instance) {
		
		
		/*
		 * First try to map the dynamic instance in APAM 
		 */
		
		ASMInst targetInstance =  compositeInterpreter.getPlatform().activate(instance);
		
		/*
		 * If the dynamic instance could not be activated simply ignore this event
		 */
		if (targetInstance == null)
			return;
		
		
		/*
		 * Iterate over all potential sources in all the composite type instances
		 */
		
		for (ASMInst sourceComposite : compositeInterpreter.getCompositeType().getInsts()) {
			
			for (ASMInst sourceInstance : ((Composite) sourceComposite).getContainInsts()) {
			
				/*
				 * Ignore instances in the composite not satisfying the source condition 
				 */
				if (! source.contains(sourceInstance))
					continue;
			
				/*
				 * We are sure that the candidate target instance is in the APAM state we can try
				 * to resolve dependency for the potential source
				 */
				if (dependency != null)
					compositeInterpreter.getPlatform().resolve(sourceInstance,dependency,false);
				else
					compositeInterpreter.getPlatform().resolve(sourceInstance,false);
			}
		}
	}

	/**
	 * This class dosen't handle substitution, just let APAM perform the usual behavior of destroying
	 * all the incoming wires to the disappearing instance.
	 */
	public void removed(ASMInst instance) {
	}

	/**
	 * This class doesn't handle failure management.
	 */
	public void bindingFailure(BindingRequest request) {
	}


}
