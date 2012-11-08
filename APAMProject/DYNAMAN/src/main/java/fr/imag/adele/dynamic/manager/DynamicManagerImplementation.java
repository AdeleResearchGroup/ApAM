package fr.imag.adele.dynamic.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.ResolutionException;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;


/**
 * This class is the entry point of the dynamic manager implementation. 
 * 
 *  
 * @author vega
 *
 */
@Instantiate(name = "DYNAMAN-Instance")
@org.apache.felix.ipojo.annotations.Component(name = "DYNAMAN" , immediate=true)
@Provides

public class DynamicManagerImplementation implements DependencyManager, DynamicManager, PropertyManager {

	private final static Logger	logger = LoggerFactory.getLogger(DynamicManagerImplementation.class);

	private final BundleContext context;
	
	public DynamicManagerImplementation(BundleContext context) {
		this.context = context;
	}
	
    /**
     * The content managers of all composites in APAM
     */
	private final Map<Composite,ContentManager> contentManagers = new HashMap<Composite, ContentManager>();
	
	/**
	 * A reference to the APAM machine
	 */
    @Requires(proxy = false)
	private Apam apam;
	
	/**
	 * This method is automatically invoked when the manager is validated
	 * 
	 * TODO Should we try to synchronize with existing composite types in
	 * APAM?
	 * 
	 */
	@Validate
	private @SuppressWarnings("unused") synchronized void start()  {
		ApamManagers.addDependencyManager(this,getPriority());
		ApamManagers.addDynamicManager(this);
		ApamManagers.addPropertyManager(this);
	}
	
	/**
	 * This method is automatically invoked when the manager is invalidated
	 * 
	 */
	@Invalidate
	private  @SuppressWarnings("unused") synchronized void stop() {
		ApamManagers.removeDependencyManager(this);
		ApamManagers.removeDynamicManager(this);
		ApamManagers.removePropertyManager(this);
	}
    
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
	 * Give access to the APAM reference
	 */
	public Apam getApam() {
		return apam;
	}
	
	/**
	 * Dynaman does not have its own model, all the information is in the component declaration.
	 * 
	 */
	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}
	

	@Override
	public void addedInApam(Component component) {
		
		/*
		 * Create content manager associated to composites
		 */
		try {
			if (component instanceof Composite) {
				Composite composite = (Composite) component;
				
				if (contentManagers.get(composite) != null) {
					logger.error("Composite already added in APAM "+composite.getName());
					return;
				}
				
				ContentManager content = new ContentManager(this,composite);
				contentManagers.put(composite,content);
				
				/*
				 * For all the existing unused instances we simulate the same behavior as
				 * if the instance just appeared in APAM. In this way, they are considered
				 * for ownership in the newly created composite.
				 */
				for (Instance instance : CST.componentBroker.getInsts()) {
					if (!instance.isUsed())
						content.instanceAdded(instance);
				}
			}
		} catch (InvalidConfiguration error) {
			logger.error("Error creating content manager",error);
		}
		
		/*
		 * Update the contents of all impacted composites
		 */
		for (ContentManager content : contentManagers.values()) {
			
			if (component instanceof Instance) {
				Instance instance = (Instance) component;
				content.instanceAdded(instance);
			}

			if (component instanceof Implementation) {
				Implementation implementation = (Implementation) component;
				content.implementationAdded(implementation);
			}

		}
	}

	@Override
	public void removedFromApam(Component component) {
		
		if (component instanceof Composite) {
			Composite composite = (Composite) component;
			
			ContentManager content = contentManagers.remove(composite);
			content.dispose();
		}
		
		/*
		 * Remove from the associated content manager
		 */
		if (component instanceof Instance) {
			Instance instance 		= (Instance) component;
			ContentManager owner	= contentManagers.get(instance.getComposite());
			if (owner != null)
				owner.instanceRemoved(instance);
		}
		
	}

	public void propertyChanged(Instance instance, String property) {		
		ContentManager owner = contentManagers.get(instance.getComposite());
		if (owner == null)
			return;
		
		boolean wasOwned = instance.isUsed();
		
		/*
		 * Delegate to the content manager
		 */
		owner.propertyChanged(instance, property);
		
		/*
		 * If the property change triggered an ownership loss, look for a new owner.
		 * 
		 * We simulate as if the instance just appeared unused in APAM. In this way,
		 * it is considered again for ownership.
		 */
		boolean ownerLost = wasOwned && ! instance.isUsed();
		
		if (ownerLost) {
			for (ContentManager content : contentManagers.values()) {
				content.instanceAdded(instance);
			}
		}

	}

	@Override
	public void attributeChanged(Component component, String attr, String newValue, String oldValue) {
		if (component instanceof Instance)
			propertyChanged((Instance) component,attr);
	}

	@Override
	public void attributeRemoved(Component component, String attr, String oldValue) {
		if (component instanceof Instance)
			propertyChanged((Instance) component,attr);
	}

	@Override
	public void attributeAdded(Component component, String attr, String newValue) {
		if (component instanceof Instance)
			propertyChanged((Instance) component,attr);
	}

	/**
	 * Registers the request in the context composite, and blocks the current thread until
	 * a component satisfying the request is available.
	 */
	private void block (PendingRequest<?> request) {
		ContentManager owner = contentManagers.get(request.getContext());
		owner.addPendingRequest(request);
		request.block();
		owner.removePendingRequest(request);
	}
	
	
	/**
	 * Get the exception object associated with a missing dependency
	 */
	private void throwMissingException(DependencyDeclaration dependency) {
		try {
			
			/*
			 * TODO BUG : the class should be loaded using the bundle context of the component  where the dependency is
			 * declared. This can be either the specification, or the implementation of the source instance, or a 
			 * composite in the case of contextual dependencies.
			 * 
			 * The best solution is to modify DependencyDeclaration to load the exception class, but this is not possible
			 * at compile time, so we can not change the signature of DependencyDeclaration.getMissingException. A possible
			 * solution is to move this method to DependencyDeclaration and make it work only at runtime, but we need to
			 * consider merge of contextual dependencies and use the correct bundle context. 
			 * 
			 * Evaluate changes to DependencyDeclaration, CoreMetadataParser.parseDependency and Util.computeEffectiveDependency
			 * 
			 */
			String exceptionName		= dependency.getMissingException();
			Class<?> exceptionClass		= context.getBundle().loadClass(exceptionName);
			RuntimeException exception	= RuntimeException.class.cast(exceptionClass.newInstance());
			throw exception;
		
		} catch (ClassNotFoundException e) {
			throw new ResolutionException();
		} catch (InstantiationException e) {
			throw new ResolutionException();
		} catch (IllegalAccessException e) {
			throw new ResolutionException();
		}
		
	}

	@Override
	public Implementation resolveSpec(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
		
		/*
		 * In case of retry of a waiting request we simply return to avoid blocking or killing the unrelated thread that 
		 * triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (dependency.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(dependency);
			}
			
			case WAIT : {
				PendingRequest.SpecificationResolution request = new PendingRequest.SpecificationResolution((ApamResolverImpl)CST.apamResolver,compoTypeFrom, dependency);
				block(request);
				return request.getResolution().iterator().next();
			}
		}
		
		return null;
	}

	@Override
	public Set<Implementation> resolveSpecs(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
		
		/*
		 * In case of retry of a waiting request we simply return to avoid blocking or killing the unrelated thread that 
		 * triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (dependency.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(dependency);
			}
			
			case WAIT : {
				PendingRequest.SpecificationResolution request = new PendingRequest.SpecificationResolution((ApamResolverImpl)CST.apamResolver,compoTypeFrom, dependency);
				block(request);
				return request.getResolution();
			}
		}
		
		return null;
	}

	@Override
	public Implementation findImplByDependency(CompositeType compoType,	DependencyDeclaration dependency) {
		
		/*
		 * In case of retry of a waiting request we simply return to avoid blocking or killing the unrelated thread that 
		 * triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (dependency.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(dependency);
			}
			
			case WAIT : {
				PendingRequest.SpecificationResolution request = new PendingRequest.SpecificationResolution((ApamResolverImpl)CST.apamResolver,compoType, dependency);
				block(request);
				return request.getResolution().iterator().next();
			}
		}
		
		return null;
	}

	@Override
	public Instance resolveImpl(Composite compo, Implementation impl, DependencyDeclaration dependency) {
		
		/*
		 * In case of retry of a waiting request we simply return to avoid blocking or killing the unrelated thread that 
		 * triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (dependency.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(dependency);
			}
			
			case WAIT : {
				/*
				 * avoid blocking the resolution if an instance can be created
				 */
				if ( impl.isInstantiable())
					return null;
				
				/*
				 * Otherwise block the current thread and schedule a dynamic resolution
				 */
				PendingRequest.ImplementationResolution request = new PendingRequest.ImplementationResolution((ApamResolverImpl)CST.apamResolver,compo,impl,dependency);
				block(request);
				return request.getResolution().iterator().next();
			}
		}
		
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Composite compo, Implementation impl, DependencyDeclaration dependency) {
		
		/*
		 * In case of retry of a waiting request we simply return to avoid blocking or killing the unrelated thread that 
		 * triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (dependency.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(dependency);
			}
			
			case WAIT : {
				/*
				 * avoid blocking the resolution if an instance can be created
				 */
				if ( impl.isInstantiable())
					return null;
				
				/*
				 * Otherwise block the current thread and schedule a dynamic resolution
				 */
				PendingRequest.ImplementationResolution request = new PendingRequest.ImplementationResolution((ApamResolverImpl)CST.apamResolver,compo,impl,dependency);
				block(request);
				return request.getResolution();
			}
		}
		
		return null;
	}

	@Override
	public void getSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dependency, List<DependencyManager> selPath) {
	}

	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst, Set<Instance> insts) {
	}

	/*
	 * Dynaman does not have a component repository, it is usually not concerned with finding a component
	 * 
	 * TODO in certain cases these methods are invoked as part of a dependency resolution, how to distinguish those cases
	 * and enforce the policy of the dependency ? currently is impossible because the resolving dependency is not 
	 * specified in the parameters
	 * 
	 */
	
	@Override
	public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName, String componentName) {
		return null;
	}
	
	@Override
	public Instance findInstByName(Composite composite, String instName) {
		return (Instance) findComponentByName(composite.getCompType(), instName);
	}

	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		return (Implementation) findComponentByName(compoType, implName);
	}

	@Override
	public Specification findSpecByName(CompositeType compoType, String specName) {
		return (Specification) findComponentByName(compoType, specName);
	}

	@Override
	public Component findComponentByName(CompositeType compoType, String compName) {
		return null;
	}



}
