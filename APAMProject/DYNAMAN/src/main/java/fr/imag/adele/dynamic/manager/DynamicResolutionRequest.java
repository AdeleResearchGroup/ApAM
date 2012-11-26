package fr.imag.adele.dynamic.manager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;

/**
 * This class handles resolution for dynamic dependencies.
 * 
 * Dynamic dependencies are automatically resolved in a background thread, independently of access to injected
 * fields.
 * 
 * Currently this is done in following cases:
 * 
 * 1) For dependencies marked as multiple (in order to keep up to date automatically the list of targets)
 * 2) For dependencies that define pushed message consumer (and so never use injected fields)
 * 
 * @author vega
 *
 */
public class DynamicResolutionRequest implements Runnable {

	/**
	 * The APAM resolver
	 */
	private final ApamResolver resolver;

	/**
	 * The source of the resolution
	 */
	private final Instance source;
	
	/**
	 * The dependency to resolve
	 */
	private final DependencyDeclaration dependency;
	
    /**
     * whether this request is currently scheduled for resolution
     */
    private boolean isScheduled;
	
	public DynamicResolutionRequest(ApamResolver resolver, Instance source, DependencyDeclaration dependency) {
		this.resolver		= resolver;
		this.source			= source;
		this.dependency		= dependency;
		this.isScheduled	= false;
	}
	
	
	/**
	 * Test whether the specified instance is a possible candidate to resolve this request. This
	 * is just used as a hint to trigger a background resolution.
	 *
	 * 
	 * TODO Should we also trigger resolution in the case that a new instantiable implementation
	 * is available? this may create a lot of instances automatically.
	 */
	public boolean isSatisfiedBy(Instance instance) {

		/*
		 * If this is dependency is already resolved, ignore any triggering event
		 */
		if (! dependency.isMultiple() && ! source.getWireDests(dependency.getIdentifier()).isEmpty())
			return false;

		/*
		 * If the candidate is already a result, ignore it
		 */
		if (source.getWireDests(dependency.getIdentifier()).contains(instance))
			return false;
		
		/*
		 * verify the candidate instance is a valid target of the dependency
		 * 
		 */
		Implementation implementation	= instance.getImpl();
		boolean valid 					= false;
		
		if (dependency.getTarget() instanceof ImplementationReference<?>)
			valid = implementation.getDeclaration().getReference().equals(dependency.getTarget());

		if (dependency.getTarget() instanceof SpecificationReference)
			valid = implementation.getSpec().getDeclaration().getReference().equals(dependency.getTarget());

		if (dependency.getTarget() instanceof ResourceReference)
			valid = implementation.getDeclaration().getProvidedResources().contains(dependency.getTarget());
		
		/*
		 * TODO we could also validate constraints and visibility but this may be costly, and will be done
		 * again by ApamMan
		 * 
		 * valid = valid && instance.isSharable() && Util.checkInstVisible(source.getComposite(), instance);
		 * 
		 * for (String constraint : dependency.getImplementationConstraints()) {
		 * 		valid = valid && implementation.match(constraint);
		 * }
		 * 
		 * for (String constraint : dependency.getInstanceConstraints()) {
		 * 	    valid = valid && instance.match(constraint);
		 * }
		 *
		 */
		
		return valid;

	}

    /**
     * The event executor. We use a pool of a threads to handle notification to APAM of underlying platform
     * events, without blocking the platform thread.
     */
    static private final Executor backgroundResolver      			= Executors.newCachedThreadPool();
	static private ThreadLocal<DynamicResolutionRequest> current	= new ThreadLocal<DynamicResolutionRequest>();
	

    /**
     * Schedule a recalculation of this dependency in the background
     */
    public synchronized void resolve() {
    	
    	/*
    	 * Avoid performing several resolutions for the same dependency in parallel. Usually this is not
    	 * useful as the current resolution will find all solutions, but in some circumstances we may lost
    	 * a triggering event.
    	 */
    	if (isScheduled)
    		return;
    	
   		isScheduled = true;	
    	backgroundResolver.execute(this);
    }

    /**
     * Start of resolution
     */
	private void beginResolve() {
		current.set(this);
	}
	
	/**
	 * End of resolution
	 */
	private void endResolve() {
		current.set(null);
		isScheduled = false;
	}

	@Override
	public void run() {
		synchronized (this) {
			try {
				beginResolve();
				resolver.resolveWire(source, dependency.getIdentifier());
			}
			catch (Throwable ignoredError) {
			}
			finally {
				endResolve();
			}
		}
	}
    
	
	/**
	 * Whether the current thread is performing a resolution retry
	 */
	public static boolean isRetry() {
		return current() != null;
	}
	
	/**
	 * The request that is being resolved by the current thread
	 */
	public static DynamicResolutionRequest current() {
		return current.get();
	}
	
}
