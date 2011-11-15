package fr.imag.adele.dynamic.application.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.ManagerModel;
import fr.imag.adele.apam.apamImpl.Wire;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apamImpl.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apamImpl.apamAPI.ManagersMng;
import fr.imag.adele.dynamic.application.interpreter.CompositeServiceInterpreter;
import fr.imag.adele.sam.Instance;


/**
 * This class is the entry point of the dynamic manager implementation. 
 * 
 * Most of the methods are called by the APAM, and they are delegated to the model interpreter that
 * handles dynamic bindings, recovery and failure.
 * 
 * It also implements service methods that can be invoked by the dynamic interpreter to access Apam
 * information and events
 *  
 * @author vega
 *
 */
public class DynamicManagerImplementation implements Manager, DynamicApplicationPlatform {

	
	/**
	 * Dynamic manager identifier
	 */
	public String getName() {
		return "fr.imag.adele.dynaman";
	}
	
	/**
	 * TODO: Ensure this manager has the minimum priority, so that it is called only in case of
	 * binding resolution failure.
	 * 
	 */
	public int getPriority() {
		return 0;
	}

	/**
	 * A reference to the APAM manager registry.
	 */
	private ManagersMng apamRegistry;
	
	/**
	 * A reference to the APAM resolver 
	 */
	private ApamResolver apamResolver;
	
	/**
	 * A reference to the APAM machine
	 */
	private Apam apam;
	
	
	/**
	 * This method is automatically invoked when APAM register is bound, we register this manager
	 * with Apam.
	 */
	
	private @SuppressWarnings("unused") synchronized void bindApamRegistry(ManagersMng apamRegistry)  {
		
		apamRegistry.addManager(this, getPriority());
	}
	
	/**
	 * This method is automatically invoked when APAM register is unbound, we unregister this manager
	 * from Apam
	 */
	private  @SuppressWarnings("unused") synchronized void unbindApamRegistry(ManagersMng apamRegistry) {
		
		/*
		 * If APAM is still active, unregister
		 */
		if (apamRegistry != null)
			apamRegistry.removeManager(this);

	}
	
	/**
	 * The list of currently running dynamic composites
	 */
	private Set<CompositeServiceInterpreter> runningComposites;
	
	/**
	 * This method is automatically invoked when the manager is validated
	 * 
	 * TODO Should we try to recreate interpreters for the existing APAM composites
	 * with associated dynamic models?
	 * 
	 */
	
	private @SuppressWarnings("unused") synchronized void start()  {
		
		runningComposites = new HashSet<CompositeServiceInterpreter>();
	}
	
	/**
	 * This method is automatically invoked when the manager is invalidated
	 * 
	 */
	private  @SuppressWarnings("unused") synchronized void stop() {

		for (CompositeServiceInterpreter composite : runningComposites) {
			composite.abort();
		}
		
		runningComposites = null;
	}

	/**
	 * Give access to APAM to the dynamic composite interpreters
	 */
	public Apam getApam() {
		return apam;
	}
	
	

	
	/**
	 * Activates a physical instance into the platform. This is a global decision because we need to decide in
	 * which of the executing composites the instance should be, and the associated sharing properties.
	 * 
	 * We implement a simple policy in which we try first to find the composite that can satisfy the greater number
	 * of pending requests.
	 * 
	 * If there are no pending requests that would be satisfied we try to find the composite with the greatest number
	 * of dynamic binds that can be satisfied by this instance.
	 */
	public Instance activate(Instance instance) {
		
		/*
		 * Return if already mapped
		 */
		if (apam.getInstBroker().getInst(instance) != null)
			return apam.getInstBroker().getInst(instance);
		
		/*
		 * Try first all the composites waiting with pending requests that could be satisfied by
		 * the appearing instance
		 */

		Composite composite = getCompositeWaitingFor(instance);

		/*
		 * If none found,  try those composites that would bind dynamically to the instance
		 */
		if (composite == null)
			composite = getCompositeBindingTo(instance);

		/*
		 * Avoid instantiating an instance that is not going to be used
		 */
		if (composite == null)
			return null;
		
		/*
		 * Activate the instance in APAM as globally accessible
		 * 
		 * TODO How to best determine access control?
		 */
		Attributes properties = new AttributesImpl();
		properties.setProperty(CST.A_SCOPE,CST.V_GLOBAL);
		return apam.getInstBroker().addSamInst(composite,instance,null,properties);
	}

	/**
	 * Get the composite containing the greatest number of instances waiting for resolution that can
	 * be potentially satisfied by the specified instance. 
	 */
	private Composite getCompositeWaitingFor(Instance instance) {

		/*
		 * Count the number of request that could be satisfied for any composite
		 * with pending requests.
		 */
		Map<Composite,Integer> counts = new HashMap<Composite, Integer>();
		
		for (Instance source : pendingRequests.keySet()) {
			for (BindingRequest pendingRequest : getPendingRequests(source)) {
				
				if (! pendingRequest.isSatisfiedBy(instance))
					continue;
				
				Composite composite		= pendingRequest.getSource().getComposite(); 

				Integer currentCount	= counts.get(composite);
				currentCount 			= (currentCount == null) ? 1 : currentCount+1;
				counts.put(composite,currentCount);
			}
		}


		/*
		 * Don't activate an instance if it is not being waited
		 */
		if (counts.isEmpty())
			return null;

		Composite SelectedComposite = null;
		int maxCount = -1;
		
		for (Composite composite : counts.keySet()) {
			int count = counts.get(composite);
			if (count > maxCount) {
				maxCount = count;
				SelectedComposite = composite;
			}
		}

		return SelectedComposite;
	}
	
	/**
	 * Get the composite containing the greatest number of dynamic binding declarations that can possibly
	 * be satisfied by the specified instance. 
	 */
	private Composite getCompositeBindingTo(Instance instance) {

		
		/*
		 * TODO Change algorithm to make a better estimate of the good composite to use
		 */
		Composite selectedComposite = null;
		int maxCount = -1;
		
		for (CompositeServiceInterpreter runningComposite : runningComposites) {
			int count = runningComposite.getEstimatedBindingCount(instance);
			if (count > maxCount) {
				maxCount = count;
				selectedComposite = (Composite)runningComposite.getCompositeType().getInst();
			}
				
		}

		return selectedComposite;

	}
	
	/**
	 * Adds a listener interested in service appearing events for the specified class of services
	 */
	private void addAppearListener(ServiceClassifier concernedServices, DynamicManager listener) {
		/*
		 * Call Apam depending on the specified classifier
		 */
		
		if (concernedServices instanceof ServiceClassifierByInterface) {
			String interfaceName = ((ServiceClassifierByInterface)concernedServices).getInterface();
			
			if (interfaceName != null)
				apamRegistry.appearedInterfExpected(interfaceName,listener);
		}
		
		if (concernedServices instanceof ServiceClassifierBySpecification) {
			String specificationName = ((ServiceClassifierBySpecification)concernedServices).getSpecification();
			
			if (specificationName != null) {
				/*
				 * TODO Modify APAM API to be able to listen for specific service specifications
				 */
			}
		}
		
		if (concernedServices instanceof ServiceClassifierByImplementation) {
			String implementationName 	= ((ServiceClassifierByImplementation)concernedServices).getImplementation();
			apamRegistry.appearedImplExpected(implementationName,listener);
		}
		
		/*
		 * In case of a logical operator , we have to listen to any event that can possibly be matched by the
		 * logical expression. Triggered events that do not effectively belong to the service class should be
		 * filtered by the listener
		 */
		if (concernedServices instanceof ServiceClassifier.BinaryOperator) {
			ServiceClassifier.BinaryOperator operation = ((ServiceClassifier.BinaryOperator)concernedServices);
			
			addAppearListener(operation.firstOperand(), listener);
			addAppearListener(operation.secondOperand(), listener);
		}	
	}
	
	/**
	 * Removes a listener interested in service appearing events for the specified class of services
	 */
	private void removeAppearListener(ServiceClassifier concernedServices, DynamicManager listener) {
		/*
		 * Call Apam depending on the specified classifier
		 */
		
		if (concernedServices instanceof ServiceClassifierByInterface) {
			String interfaceName = ((ServiceClassifierByInterface)concernedServices).getInterface();
			
			if (interfaceName != null)
				apamRegistry.appearedInterfNotExpected(interfaceName,listener);
		}
		
		if (concernedServices instanceof ServiceClassifierBySpecification) {
			String specificationName = ((ServiceClassifierBySpecification)concernedServices).getSpecification();
			
			if (specificationName != null) {
				/*
				 * TODO Modify APAM API to be able to listen for specific service specifications
				 */
			}
		}
		
		if (concernedServices instanceof ServiceClassifierByImplementation) {
			String implementationName 	= ((ServiceClassifierByImplementation)concernedServices).getImplementation();
			apamRegistry.appearedImplNotExpected(implementationName,listener);
		}
		
		/*
		 * In case of a logical operator , we have to remove all listener previously registered
		 */
		if (concernedServices instanceof ServiceClassifier.BinaryOperator) {
			ServiceClassifier.BinaryOperator operation = ((ServiceClassifier.BinaryOperator)concernedServices);
			
			removeAppearListener(operation.firstOperand(), listener);
			removeAppearListener(operation.secondOperand(), listener);
		}
	}
	
	/**
	 * Adds a listener interested in service disappearing events for the specified class of services
	 */
	private void addDisappearListener(ServiceClassifier concernedServices, DynamicManager listener) {
		
		/*
		 * We have to listen to all disappear events, as this is the granularity provided by the APAM
		 * API. Triggered events that do not effectively belong to the service class should be
		 * filtered by the listener
		 */
		apamRegistry.listenLost(listener);
	}
	
	/**
	 * Removes a listener interested in service disappearing events for the specified class of services
	 */
	private void removeDisappearListener(ServiceClassifier concernedServices, DynamicManager listener) {
		apamRegistry.listenNotLost(listener);
	}


	/**
	 * An adapter class that implements the APAM interface and relays events to a given listener.
	 * 
	 * TODO Modify APAM API such that managers can register listeners of a small granularity, instead
	 * of having a single listener per manager.
	 */
	private class DynamicManagerListener implements DynamicManager {
		
		private final ServiceClassifier expectedServiceClassifier;
		private final Listener listener;
		
		/**
		 * Creates a new wrapper and register it with APAM to listen for specified events
		 */
		public DynamicManagerListener(ServiceClassifier expectedServiceClassifier, Listener listener) {
			this.listener 					= listener;
			this.expectedServiceClassifier	= expectedServiceClassifier;
			
			addAppearListener(expectedServiceClassifier,this);
			addDisappearListener(expectedServiceClassifier,this);
		}

		/**
		 * Dispose this adapter, unregistering from APAM
		 */
		public void dispose() {
			removeAppearListener(expectedServiceClassifier,this);
			removeDisappearListener(expectedServiceClassifier,this);
		}

		/**
		 * Dispatch APAM event to signal instance apparition to concerned listeners.
		 */
		public void appeared(Instance samInstance) {
			
			/*
			 * Ignore events not concerning this listener
			 */
			if (!expectedServiceClassifier.contains(samInstance))
				return;
			
			/*
			 * relay event
			 */
			listener.added(samInstance);
		}

		/**
		 * Dispatch APAM event to signal instance dispparition to concerned listeners.
		 */
		public Instance lostInst(Instance lost) {
			/*
			 * Ignore events not concerning this listener
			 */
			if (!expectedServiceClassifier.contains(lost))
				return null;
			
			/*
			 * relay event
			 */
			listener.removed(lost);
			
			return lost;
		}


		/**
		 * Dispatch APAM event to signal binding failure to concerned listeners.
		 */
		public void bindingFailure(BindingRequest request) {
			
			/*
			 * Ignore events not concerning this listener
			 */
			if (! expectedServiceClassifier.contains(request.getTarget()))
				return;
			
			/*
			 * relay event
			 */
			listener.bindingFailure(request);
			
		}

		@Override
		public String getName() {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public void getSelectionPathSpec(CompositeType compTypeFrom,
				String interfaceName, String[] interfaces, String specName,
				Set<Filter> constraints, List<Filter> preferences,
				List<Manager> selPath) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public void getSelectionPathImpl(CompositeType compTypeFrom,
				String implName, List<Manager> selPath) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public void getSelectionPathInst(Composite compoFrom, Implementation impl,
				Set<Filter> constraints, List<Filter> preferences,
				List<Manager> selPath) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public int getPriority() {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public void newComposite(ManagerModel model, CompositeType composite) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public Implementation resolveSpecByName(CompositeType compoType,
				String specName, Set<Filter> constraints,
				List<Filter> preferences) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public Implementation resolveSpecByInterface(CompositeType compoType,
				String interfaceName, String[] interfaces,
				Set<Filter> constraints, List<Filter> preferences) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public Implementation findImplByName(CompositeType compoType, String implName) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public Instance resolveImpl(Composite compo, Implementation impl,
				Set<Filter> constraints, List<Filter> preferences) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}

		@Override
		public Set<Instance> resolveImpls(Composite compo, Implementation impl,
				Set<Filter> constraints) {
			throw new UnsupportedOperationException("Error in APAM registry, call unwrongly directed to listener");
		}
		

	}
	
	/**
	 * The list of registered listeners of the underlying platform events
	 */
	private Map<Listener,DynamicManagerListener> registeredListeners = new HashMap<Listener,DynamicManagerListener>();

	
	/**
	 * Delegation method to allow an interpreter to register a listener for events from the underlying APAM platform
	 */
	public void addListener(ServiceClassifier serviceClass, Listener listener) {
		registeredListeners.put(listener, new DynamicManagerListener(serviceClass,listener));
	}

	/**
	 * Unregister listener for events from the underlying APAM platform
	 */
	public void removeListener(Listener listener) {
		DynamicManagerListener listenerAdapter = registeredListeners.remove(listener);
		if (listenerAdapter != null)
			listenerAdapter.dispose();
	}


	/**
	 * Refine a given target by including all additional constraints that the platform want to enforce
	 */
	public ServiceClassifier refine(String dependency, ServiceClassifier target) {
		/*
		 * Call Apam depending on the specified target and dependency 
		 */
		
		List<Filter> constraints = new ArrayList<Filter>();
		/*
		if (target instanceof ServiceClassifierByInterface) {
			String interfaceName = ((ServiceClassifierByInterface)target).getInterface();
			constraints = apamResolver.getConstraintsSpec(interfaceName,null,dependency,constraints);
		}
		
		if (target instanceof ServiceClassifierBySpecification) {
			String specificationName = ((ServiceClassifierBySpecification)target).getSpecification();
			constraints = apamResolver.getConstraintsSpec(null,specificationName,dependency,constraints);
		}
		*/
		/*
		 * build refined service classifier
		 */
		return target.and(new ServiceClassifierByConstraints(new HashSet<Filter>(constraints)));
		
	}

	/**	
	 * Resolves the given request if possible, by creating appropriate wires between the source and a target
	 * satisfying the specified constraints. If specified, it will recursively resolve the selected target. 
	 * 
	 * Unlike the Apam resolution API, this method is intended for private use by the dynamic composite interpreter,
	 * it will silently ignore resolution failures, in order to prevent potential loops. 
	 */
	public void resolve(BindingRequest request, boolean eager) {
		
		/* 
		 * call Apam to resolve the specified request 
		 */
		resolve(request);
		
		/*
		 * Just one step in lazy mode
		 */
		if (!eager)
			return;
		
		
		/*
		 * Iterate over the newly resolved wires
		 */
		for (Wire resolvedWire : request.getSource().getWires(request.getDependency())) {
			
			/*
			 * Get the destination and resolve it recursively
			 */
			resolve(resolvedWire.getDestination(),eager);
			
		}
	}

	/**
	 * Resolves the given dependency for the specified source instance. This method builds a binding request 
	 * based on the basic dependency model of the handler associated to the instance, and delegates process
	 * to the resolve(BindingRequest, boolean) method.
	 */
	public void resolve(Instance instance, String dependency, boolean eager) {
		
		/*
		 * Get a model of the dependency of the destination
		 */
		if (instance.getDependencyHandler() == null)
			return;
		
		/*
		 * Look for specified dependency in the model
		 */
		ApamDependencyHandler.DependencyModel dependencyModel = null;
		
		for (ApamDependencyHandler.DependencyModel declaredDependency : instance.getDependencyHandler().getDependencies()) {
			if (declaredDependency.dependencyName.equals(dependency)) {
				dependencyModel = declaredDependency;
				break;
			}
		}
		
		/*
		 * Resolve if found
		 */
		if (dependencyModel != null)
			resolve(instance,Collections.singleton(dependencyModel),eager);
	}

	/**
	 * Resolves all the dependencies for the specified source instance. This method builds a binding request 
	 * based on the basic dependency model of the handler associated to the instance, and delegates process
	 * to the resolve(BindingRequest, boolean) method.
	 */
	public void resolve(Instance instance, boolean eager) {
		
		/*
		 * Get a model of the dependencies of the destination
		 */
		if (instance.getDependencyHandler() == null)
			return;
		
		resolve(instance, instance.getDependencyHandler().getDependencies(),eager);
		
	}


	/**
	 * Resolves the given dependencies for the specified source instance. This method builds a binding request 
	 * based on the basic dependency model of the handler associated to the instance, and delegates process
	 * to the resolve(BindingRequest, boolean) method.
	 */
	private void resolve(Instance instance, Set<ApamDependencyHandler.DependencyModel> dependencies, boolean eager) {
		
		/*
		 * Iterate over all potential dependencies of the resolved instance 
		 */
		for (ApamDependencyHandler.DependencyModel dependencyModel : dependencies) {
			
			/*
			 * If scalar dependency is already resolved stop resolution
			 */
			if (!dependencyModel.isMultiple && !instance.getWires(dependencyModel.dependencyName).isEmpty())
				continue;
			
			/*
			 * Create a new request and resolve it recursively
			 */
			ServiceClassifier target = ServiceClassifier.ANY;
			
			switch (dependencyModel.targetKind) {
			case INTERFACE:
				target = new ServiceClassifierByInterface(dependencyModel.target);
				break;
			case SPECIFICATION:
				target = new ServiceClassifierBySpecification(dependencyModel.target);
				break;
			case IMPLEMENTATION:
				target = new ServiceClassifierByImplementation(dependencyModel.target);
				break;
			}
			
			BindingRequest resolutionRequest = new BindingRequest(instance,dependencyModel.dependencyName,dependencyModel.isMultiple,target);
			resolve(resolutionRequest,eager);
			
		}
	}
	
	private Map<Instance,Set<BindingRequest>> pendingRequests= new HashMap<Instance, Set<BindingRequest>>();
	
	/**
	 * Adds a new request to the list of pending request associated with the source
	 */
	protected synchronized void addPendingRequest(Instance source, BindingRequest request) {
		
		Set<BindingRequest> sourcePendingRequests = pendingRequests.get(source);
		
		if (sourcePendingRequests == null) {
			sourcePendingRequests = new HashSet<BindingRequest>();
			pendingRequests.put(source,sourcePendingRequests);
		}
		
		sourcePendingRequests.add(request);
	}
	
	/**
	 * Removes a new request to the list of pending request associated with the source
	 */
	protected synchronized void removePendingRequest(Instance source, BindingRequest request) {
		
		Set<BindingRequest> sourcePendingRequests = pendingRequests.get(source);
		
		if (sourcePendingRequests == null)
			return ;
		
		sourcePendingRequests.remove(request);
		
		if (sourcePendingRequests.isEmpty())
			pendingRequests.remove(source);
		
	}
	
	/**
	 * Get an immutable copy of the pending request associated with the source
	 */
	protected synchronized Set<BindingRequest> getPendingRequests(Instance source) {
		
		Set<BindingRequest> sourcePendingRequests = new HashSet<BindingRequest>();
		if (pendingRequests.get(source) != null)
			sourcePendingRequests.addAll(pendingRequests.get(source));
		
		return sourcePendingRequests;
	}
	
	/**
	 * Keeps a global list of pending request that needs to notified when satisfied by future resolutions. 
	 * 
	 * This method blocks the calling thread until resolution.
	 */
	public synchronized void waitForResolution(BindingRequest request) {
		
		/*
		 * Keep track of the request
		 */
		addPendingRequest(request.getSource(),request);
		
		/*
		 * and wait
		 */
		try {
			synchronized (request) {
				request.wait();
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * Delegation method to allow an interpreter to resolve a given binding request by invoking the APAM
	 * protocol. This has as the side effect of modifying the source instance to add wires to the resolved
	 * destination.
	 * 
	 * WARNING this method impacts failure handler. If dynaman is called in the context of this call it should
	 * ignore all binding failures.
	 * 
	 */
	
	private ThreadLocal<BindingRequest> requestInProgress = new ThreadLocal<BindingRequest>();
	
	private boolean isResolving() {
		return requestInProgress.get() != null;
	}
	
	private void resolve(BindingRequest request) {
		
		/*
		 * Remember current request and call Apam to perform resolution
		 */
		try {
			requestInProgress.set(request);
			
			Instance	source				= request.getSource();
			String dependency			= request.getDependency();
			ServiceClassifier target	= request.getTarget();
			
			/*
			 * Call Apam depending on the specified target and cardinality 
			 */
			
			if (target instanceof ServiceClassifierByInterface) {
				String interfaceName = ((ServiceClassifierByInterface)target).getInterface();
				
				if (request.isAggregate())
					apamResolver.newWireSpecs(source,interfaceName,null,dependency,null,null);
				else
					apamResolver.newWireSpec(source,interfaceName,null,dependency, null, null);
			}
			
			if (target instanceof ServiceClassifierBySpecification) {
				String specificationName = ((ServiceClassifierBySpecification)target).getSpecification();
				
				if (request.isAggregate())
					apamResolver.newWireSpecs(source,null,specificationName,dependency,null,null);
				else
					apamResolver.newWireSpec(source,null,specificationName,dependency,null,null);
				
			}
			
			if (target instanceof ServiceClassifierByImplementation) {
				String implementationName 	= ((ServiceClassifierByImplementation)target).getImplementation();

				if (request.isAggregate())
					apamResolver.newWireImpls(source,implementationName,dependency,null,null);
				else
					apamResolver.newWireImpl(source,implementationName,dependency,null,null);
			}
			
			/*
			 * Notify all pending request satisfied by this resolution
			 */
			for (Instance resolvedDestination : source.getWireDests(dependency)) {
				
				for (BindingRequest pendingRequest :  getPendingRequests(source)) {
					
					if (! pendingRequest.getDependency().equals(dependency))
						continue;
					
					if (! pendingRequest.isSatisfiedBy(resolvedDestination))
						continue;
					
					/*
					 * remove pending request
					 */
					removePendingRequest(source, pendingRequest);
					
					/*
					 * notify thread to unblock, notice that the wire has already been created
					 * by the resolution
					 */
					synchronized (pendingRequest) {
						pendingRequest.notify();
					}
				}
			}
			
		}
		finally {
			requestInProgress.remove();
		}
		
	}



	/**
	 * This method is called to handle resolution failure in Apam, it delegates to the registered
	 * listener interested in handling this event
	 */
	private void bindingFailure(BindingRequest request) {
		
		/*
		 * If this is method is called in the context of manager initiated resolution, we ignore
		 * any failure 
		 */
		if (isResolving())
			return;
		
		/*
		 * Otherwise delegate to appropriate listeners
		 */
		for (DynamicManagerListener listener : registeredListeners.values()) {
			listener.bindingFailure(request);
		}
	}


	/**
	 * Dynaman does not resolve bindings, it does not add new constraints and it
	 * must be the last involved manager in order to handle failure.
	 */
	@Override
	public void getSelectionPathSpec(CompositeType compTypeFrom,
			String interfaceName, String[] interfaces, String specName,
			Set<Filter> constraints, List<Filter> preferences,
			List<Manager> selPath) {
		
		if (!selPath.contains(this))
			selPath.add(selPath.size(),this);
		
	}

	/**
	 * Dynaman does not resolve bindings, it does not add new constraints and it
	 * must be the last involved manager in order to handle failure.
	 */
	@Override
	public void getSelectionPathImpl(CompositeType compTypeFrom,
			String implName, List<Manager> selPath) {
		
		if (!selPath.contains(this))
			selPath.add(selPath.size(),this);
	}

	/**
	 * Dynaman does not resolve bindings, it does not add new constraints and it
	 * must be the last involved manager in order to handle failure.
	 */
	@Override
	public void getSelectionPathInst(Composite compoFrom, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences,
			List<Manager> selPath) {
		
		if (!selPath.contains(this))
			selPath.add(selPath.size(),this);
	}

	/**
	 * Executes the dynamic model to the specified composite
	 */
	
	@Override
	public synchronized void newComposite(ManagerModel model, CompositeType composite) {
		/*
		 * ignore calls while invalidated
		 */
		if (runningComposites == null)
			return;
		
		/*
		 * create a new interpreter instance for this composite
		 */
		assert model.getManagerName().equals(this.getName());
		CompositeServiceInterpreter interpreter = CompositeServiceInterpreter.create(this,composite,model.getURL());
		runningComposites.add(interpreter);
		interpreter.start();
	}


	/**
	 * This  method is invoked by APAM in a two step (specification-implementation-instance) binding resolution. 
	 * Dynaman is not involved in resolution, but must be part of the selection process to handle binding failure.
	 */
	@Override
	public Implementation resolveSpecByName(CompositeType compoType, String specName,
			Set<Filter> constraints, List<Filter> preferences) {
		
		
		/*
		 * TODO:  Because of the two step process we should remember this call to keep track of the original 
		 * request
		 * 
		 *	ServiceClassifier target = ServiceClassifier.ANY;
		 *
		 *	if (specName != null)
		 *		target = new ServiceClassifierBySpecification(specName);
	 	 *
		 *	if (constraints != null)
		 *		target = target.and(new ServiceClassifierByConstraints(constraints));
		 *
		 *	bindingFailure(new BindingRequest(from,depName,false,target));
		 */
		return null;
	}


	/**
	 * This  method is invoked by APAM in a two step (specification-implementation-instance) binding resolution.
	 * Dynaman is not involved in resolution, but must be part of the selection process to handle binding failure.
	 */
	@Override
	public Implementation resolveSpecByInterface(CompositeType compoType,
			String interfaceName, String[] interfaces, Set<Filter> constraints,
			List<Filter> preferences) {
		
		/*
		 * TODO:  Because of the two step process we should remember this call to keep track of the original 
		 * request
		 * 
		 *	ServiceClassifier target = ServiceClassifier.ANY;
		 *
		 *	if (interfaceName != null)
		 *		target = new ServiceClassifierByInterface(interfaceName);
	 	 *
		 *	if (constraints != null)
		 *		target = target.and(new ServiceClassifierByConstraints(constraints));
		 *
		 *	bindingFailure(new BindingRequest(from,depName,false,target));
		 */

		return null;
	}

	/**
	 * This  method is invoked by APAM in a two step (specification-implementation-instance) binding resolution.
	 * Dynaman is not involved in resolution, but must be part of the selection process to handle binding failure.
	 */
	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		/*
		 * TODO:  Because of the two step process we should remember this call to keep track of the original 
		 * request
		 * 
		 *	ServiceClassifier target = ServiceClassifier.ANY;
		 *
		 *  if (implName != null)
		 *		target = new ServiceClassifierByImplementation(implName);
	 	 *
		 *	bindingFailure(new BindingRequest(from,depName,false,target));
		 */

		return null;
	}

	

	/**
	 * This  method is invoked by APAM in a two step (specification-implementation-instance) binding resolution.
	 * Dynaman is not involved in resolution, but must be part of the selection process to handle binding failure.
	 */
	@Override
	public Instance resolveImpl(Composite compo, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences) {
		/*
		 * TODO:  Because of the two step process we should remember this call to keep track of the original 
		 * request
		 * 
		 *	ServiceClassifier target = ServiceClassifier.ANY;
		 *
		 *  if (implName != null)
		 *		target = new ServiceClassifierByImplementation(implName);
	 	 *
		 *	bindingFailure(new BindingRequest(from,depName,false,target));
		 */

		bindingFailure(null);
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Composite compo, Implementation impl,
			Set<Filter> constraints) {
		/*
		 * TODO:  Because of the two step process we should remember this call to keep track of the original 
		 * request
		 * 
		 *	ServiceClassifier target = ServiceClassifier.ANY;
		 *
		 *  if (implName != null)
		 *		target = new ServiceClassifierByImplementation(implName);
	 	 *
		 *	bindingFailure(new BindingRequest(from,depName,false,target));
		 */

		bindingFailure(null);
		return null;
	}


}
