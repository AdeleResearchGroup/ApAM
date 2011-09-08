package fr.imag.adele.dynamic.application.interpreter;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.CompositeOLD;
import fr.imag.adele.dynamic.application.manager.DynamicApplicationPlatform;
import fr.imag.adele.dynamic.application.manager.ServiceClassifier;
import fr.imag.adele.sam.Instance;

/**
 * This class handles the execution of a single composite according to the specified model.
 * 
 * @author vega
 *
 */
public class CompositeServiceInterpreter {

	
	/**
	 * A factory method to create an interpreter for the given composite and model
	 */
	public static CompositeServiceInterpreter create(DynamicApplicationPlatform platform, CompositeOLD composite, URL model) {
		return new CompositeServiceInterpreter(composite, platform);
	}
	
	/**
	 * The composite handled by this interpreter
	 */
	private final CompositeOLD		composite;
	
	/**
	 * The execution platform for running this interpreter
	 */
	private final DynamicApplicationPlatform platform;
	
	
	/**
	 * The list of dynamic binders
	 */
	private Set<DynamicBinder> binders;
	
	/**
	 * The list of failure managers
	 */

	private Set<FailureManager> failureManagers;
	
	/**
	 * Thelist of substitution managers
	 */
	private Set<SubtstitutionManager> substitutionManagers;
	
	/**
	 * Builds a new instance of the interpreter
	 */
	public CompositeServiceInterpreter(CompositeOLD composite, DynamicApplicationPlatform platform) {
		this.composite			= composite;
		this.platform			= platform;
		
		this.binders				= new HashSet<DynamicBinder>();
		this.failureManagers 		= new HashSet<FailureManager>();
		this.substitutionManagers	= new HashSet<SubtstitutionManager>();
	}
	
	/**
	 * Get the associated composite
	 */
	public CompositeOLD getComposite() {
		return composite;
	}
	
	/**
	 * Get the underlying execution platform
	 */
	public DynamicApplicationPlatform getPlatform() {
		return platform;
	}
	
	/**
	 * Register a new dynamic binder
	 */
	public void addDynamicBinder(ServiceClassifier source, ServiceClassifier target, String dependency) {
		binders.add(new DynamicBinder(this, source, target, dependency));
	}

	/**
	 * Gets an estimate of the number of bindings that can be resolved by the dynamic binders of this
	 * composite if the specified instance is activated. 
	 */
	public int getEstimatedBindingCount(Instance instance) {
		int count = 0;

		for (DynamicBinder binder : binders) {
			count += binder.getEstimatedBindingCount(instance);
		}

		return count;
		
	}

	/**
	 * Register a new failure manager
	 */
	public void addFailureManager(ServiceClassifier target, FailureManager.Policy policy) {
		failureManagers.add(new FailureManager(this,target,policy));
	}

	/**
	 * Register a new substitution manager
	 */
	public void addSubstitutionManager(ServiceClassifier source, ServiceClassifier target, String dependency, SubtstitutionManager.Policy policy) {
		substitutionManagers.add(new SubtstitutionManager(this, source, target, policy,dependency));
	}

	/**
	 * Starts interpreting the model
	 */
	public void start() {
		for (DynamicBinder binder : binders) {
			binder.start();
		}
		
		for (FailureManager failureManager : failureManagers) {
			failureManager.start();
		}

		for (SubtstitutionManager substitutionManager : substitutionManagers) {
			substitutionManager.start();
		}
	}
	
	
	/**
	 * Aborts interpreting the model
	 */
	public void abort() {
		for (DynamicBinder binder : binders) {
			binder.abort();
		}
		
		for (FailureManager failureManager : failureManagers) {
			failureManager.abort();
		}
		
		for (SubtstitutionManager substitutionManager : substitutionManagers) {
			substitutionManager.abort();
		}

	}


}
