package fr.imag.adele.dynamic.manager;

import java.util.ArrayList;
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
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;


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
	 * TODO Should we try to synchronize with existing composites in APAM?
	 * 
	 */
	@Validate
	private @SuppressWarnings("unused") synchronized void start()  {
		
		/*
		 * Create the default content manager to be associated with the root composite
		 */
		try {
			Composite root				= CompositeImpl.getRootAllComposites();
			ContentManager rootContent	= new ContentManager(this,root);
			contentManagers.put(root,rootContent);
		} catch (InvalidConfiguration ignored) {
		}
		
		/*
		 * Register with APAM 
		 */
		ApamManagers.addDependencyManager(this,getPriority());
		ApamManagers.addDynamicManager(this);
		ApamManagers.addPropertyManager(this);

		/*
		 * TODO if Dynaman is started or restarted after APAM, should we verify if there
		 * are already created composites? 
		 */
		  logger.info("[DYNAMAN] started");
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
		logger.info("[DYNAMAN] stopped");
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
		return 5;
	}

	
	/**
	 * Give access to the APAM reference
	 */
	public Apam getApam() {
		return apam;
	}
	
	/**
	 * Set the ownership of an instance to one of the requesting composites, signal any detected conflict
	 */
	private void verifyOwnership(Instance instance) {

		/*
		 * Get the current container and owner
		 */
		ContentManager container 	= contentManagers.get(instance.getComposite());
		ContentManager owner		= container.owns(instance) ? container : null;
		
		/*
		 * Get the list of composites requesting ownership
		 */
		List<ContentManager> requesters = new ArrayList<ContentManager>();
		StringBuffer requestersNames	= new StringBuffer();
		
		for (ContentManager manager : contentManagers.values()) {
			if (manager.requestOwnership(instance)) {
				requesters.add(manager);
				
				requestersNames.append(" ");
				requestersNames.append(manager.getComposite().getName());
			}
		}
		
		/*
		 * If there is conflicting requests signal an error
		 * 
		 * TODO In some cases we could do more than simply logging the error. Perhaps avoiding creating composites
		 * that will produce conflicts.
		 */
		if (requesters.size() > 1) {
			logger.error("Conflict in ownership : composites ("+requestersNames+") request ownership of instance "+instance.getName());
		}
		
		/*
		 * If there is no ownership request continue processing event
		 */
		if (requesters.isEmpty())
			return;
		
		/*
		 * Choose an owner arbitrarily among requesters (try to keep the current owner if exists)
		 */
		ContentManager newOwner = requesters.isEmpty() ? null : requesters.contains(container) ? container : requesters.get(0);
		
		/*
		 * Revoke ownership to previous owner (if it has changed)
		 */
		if (owner != null && (newOwner == null || ! newOwner.equals(owner)))
			owner.revokeOwnership(instance);
		
		/*
		 * Grant ownership to new owner
		 */
		if (newOwner != null)
			newOwner.grantOwnership(instance);
	}

	/**
	 * Registers the request in the context composite, and blocks the current thread until
	 * a component satisfying the request is available.
	 */
	private void block (PendingRequest request) {
		ContentManager manager = contentManagers.get(request.getContext());
		manager.addPendingRequest(request);
		request.block();
		manager.removePendingRequest(request);
	}
	
	
	/**
	 * Throws the exception associated with a missing dependency
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
		 * Create a content manager associated to newly created composite
		 */
		if (component instanceof Composite) {
			
			Composite composite = (Composite) component;
			
			if (contentManagers.get(composite) != null) {
				logger.error("Composite already added in APAM "+composite.getName());
				return;
			}

			try {

				ContentManager manager = new ContentManager(this,composite);
				contentManagers.put(composite,manager);
				
				/*
				 * For all the existing instances we consider the impact of the newly created composite
				 * in ownership
				 */
				for (Instance instance : CST.componentBroker.getInsts()) {
					verifyOwnership(instance);
				}
				
			} catch (InvalidConfiguration error) {
				
				/*
				 * TODO We should not add the composite in APAM if the content manager could not be created,
				 * but currently there is no way for a manager to signal an error in creation.
				 */
				logger.error("Error creating content manager for composite "+component.getName(),error);
			}
		}
		
		/*
		 * Verify ownership of newly created instances
		 */
		if (component instanceof Instance) {
			Instance instance = (Instance) component;
			verifyOwnership(instance);
		}

		/*
		 * Update the contents of all impacted composites
		 */
		for (ContentManager manager : contentManagers.values()) {
			
			if (component instanceof Instance) {
				Instance instance = (Instance) component;
				manager.instanceAdded(instance);
			}

			if (component instanceof Implementation) {
				Implementation implementation = (Implementation) component;
				manager.implementationAdded(implementation);
			}

		}
	}

	@Override
	public void removedFromApam(Component component) {

		/*
		 * Remove the instance from the associated content manager
		 */
		if (component instanceof Instance) {
			Instance instance 		= (Instance) component;
			ContentManager manager	= contentManagers.get(instance.getComposite());
			manager.instanceRemoved(instance);
		}

		/*
		 * Remove a content manager when its composite is removed
		 */
		if (component instanceof Composite) {
			ContentManager manager = contentManagers.remove((Composite) component);
			manager.dispose();
		}
		
		
	}

	public void propertyChanged(Instance instance, String property) {		

		/*
		 * verify possible ownership change
		 */
       	verifyOwnership(instance);

		
		/*
		 * Update the contents of all impacted composites
		 */
       for (ContentManager manager : contentManagers.values()) {
        	manager.propertyChanged(instance, property);
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


	@Override
	public Set<Implementation> resolveDependency(Instance client, DependencyDeclaration dependency, Set<Instance> insts) {
		
		/*
		 * In case of retry of a waiting or eager request we simply return to avoid blocking or killing
		 * the unrelated thread that triggered the recalculation
		 * 
		 */
		if (DynamicResolutionRequest.isRetry() || PendingRequest.isRetry())
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
				PendingRequest request = new PendingRequest((ApamResolverImpl)CST.apamResolver,client, dependency, insts);
				block(request);
				return request.getResolution();
			}
		}
		
		return null;
	}


	@Override
	public Instance resolveImpl(Instance client, Implementation impl, DependencyDeclaration dependency) {
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Instance client, Implementation impl, DependencyDeclaration dependency) {
		return null;
	}

	@Override
	public void getSelectionPath(Instance client, DependencyDeclaration dependency, List<DependencyManager> selPath) {
        selPath.add(selPath.size(), this);
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
	public Instance findInstByName(Instance client, String instName) {
		return (Instance) findComponentByName(client, instName);
	}

	@Override
	public Implementation findImplByName(Instance client, String implName) {
		return (Implementation) findComponentByName(client, implName);
	}

	@Override
	public Specification findSpecByName(Instance client, String specName) {
		return (Specification) findComponentByName(client, specName);
	}

	@Override
	public Component findComponentByName(Instance client, String compName) {
		return null;
	}



}
