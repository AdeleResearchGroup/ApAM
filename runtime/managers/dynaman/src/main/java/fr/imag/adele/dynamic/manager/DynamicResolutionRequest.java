package fr.imag.adele.dynamic.manager;

import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

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
public class DynamicResolutionRequest {

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
	 * The source of the dependency
	 */
	public Instance getSource() {
		return source;
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
			valid = implementation.getDeclaration().getProvidedResources().contains(dependency.getTarget()) ||
					implementation.getSpec().getDeclaration().getProvidedResources().contains(dependency.getTarget());
		
		return valid;

	}

	/**
	 * The dynamic request that is being resolved in the current thread, if any 
	 */
	static private ThreadLocal<DynamicResolutionRequest> current	= new ThreadLocal<DynamicResolutionRequest>();
	

    /**
     * Perform a recalculation of this dependency
     */
    public synchronized void resolve() {
    	
    	/*
    	 * Avoid performing several resolutions for the same dependency in parallel. Usually this is not
    	 * useful as the current resolution will find all solutions, but in some circumstances we may lost
    	 * a triggering event.
    	 */
    	if (isScheduled)
    		return;
    	
    	/*
    	 * Invoke resolver to try to find a solution to the dynamic dependency.
    	 * 
    	 * IMPORTANT Notice that resolution is performed in the context of the thread that triggered the
    	 * recalculation event. If resolution fails, the resolver must simply ignore the failure, otherwise
    	 * this will block or kill an unrelated thread. This is insured by the dynamic manager. 
    	 * 
    	 * We need to evaluate if it is safer to resolve dynamic dependencies in a background thread, but this
    	 * mat introduce some race conditions
    	 */
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

    /**
     * Start of resolution
     */
	private void beginResolve() {
   		isScheduled = true;	
		current.set(this);
	}
	
	/**
	 * End of resolution
	 */
	private void endResolve() {
		current.set(null);
		isScheduled = false;
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
