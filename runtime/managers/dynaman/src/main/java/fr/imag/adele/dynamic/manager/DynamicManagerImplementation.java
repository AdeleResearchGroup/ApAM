/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.dynamic.manager;

import java.util.ArrayList;
import java.util.Collection;
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
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.ResolutionException;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
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

public class DynamicManagerImplementation implements RelationManager, DynamicManager, PropertyManager {

	private final static Logger	logger = LoggerFactory.getLogger(DynamicManagerImplementation.class);

	
	/**
	 * A reference to the APAM machine
	 */
    @Requires(proxy = false)
	private Apam apam;
	
    /**
     * The content managers of all composites in APAM
     */
	private Map<Composite,ContentManager> contentManagers;
	
	/**
	 * The content manager associated with the root composite
	 */
	private ContentManager rootManager;
	
	
	/**
	 * The dynamic manager handle all request concerned with dynamic management of links
	 */
	public DynamicManagerImplementation(BundleContext context) {
	}
    
	/**
	 * Give access to the APAM reference
	 */
	public Apam getApam() {
		return apam;
	}
    
	/**
	 * This method is automatically invoked when the manager is validated, so
	 * we can safely assume that APAM is available
	 * 
	 */
	@Validate
	@SuppressWarnings("unused")
	private  synchronized void start()  {
		
		/*
		 * Create the default content manager to be associated with the root composite
		 */
		try {
			
			contentManagers = new HashMap<Composite, ContentManager>();
			
			Composite root	= CompositeImpl.getRootAllComposites();
			rootManager		= new ContentManager(this,root);
			
			contentManagers.put(root,rootManager);
			rootManager.start();
			
		} catch (InvalidConfiguration ignored) {
		}
		
		/*
		 * Register with APAM 
		 */
		ApamManagers.addRelationManager(this,getPriority());
		ApamManagers.addDynamicManager(this);
		ApamManagers.addPropertyManager(this);

		/*
		 * TODO if Dynaman is started or restarted after APAM, should we verify if there
		 * are already created composites? 
		 */
	}
	
	/**
	 * This method is automatically invoked when the manager is invalidated, so APAM is
	 * no longer available
	 */
	@Invalidate
	@SuppressWarnings("unused")
	private synchronized void stop() {
		ApamManagers.removeRelationManager(this);
		ApamManagers.removeDynamicManager(this);
		ApamManagers.removePropertyManager(this);
	}

	/**
	 * Get the manager associated to a composite
	 */
	private synchronized ContentManager getManager(Composite composite) {
		return composite != null ? contentManagers.get(composite) : contentManagers.get(CompositeImpl.getRootAllComposites());
	}
	
	/**
	 * Get a thread safe (stack contained) copy of the current list of managers
	 */
	private synchronized Collection<ContentManager> getManagers() {
		return new ArrayList<ContentManager>(contentManagers.values());
	}

    
	/**
	 * Dynamic manager identifier
	 */
	@Override
	public String getName() {
		return "fr.imag.adele.dynaman";
	}
	
	/**
	 * Ensure this manager has the minimum priority, so that it is called only in case of binding resolution failure.
	 * 
	 */
	@Override
	public int getPriority() {
		return 5;
	}

	@Override
	public void getSelectionPath(Component client, Relation relation, List<RelationManager> selPath) {
        selPath.add(selPath.size(), this);
	}
	
	/**
	 * If this manager is invoked it means that the specified relationship could not be resolved, so we
	 * have to apply the specified dynamic policy.  
	 */
	@Override
	public Resolved<?> resolveRelation(Component client, Relation relation) {
		
		/*
		 * If no policy is specified for the relationship just ignore it.
		 */
		if (relation.getMissingPolicy() == null)
			return null;
		
		/*
		 * In case of retry of a waiting or eager request we simply return to avoid blocking or killing
		 * the unrelated thread that triggered the recalculation
		 * 
		 */
		if (PendingRequest.isRetry())
			return null;
		
		/*
		 * Apply failure policies
		 */
		switch (relation.getMissingPolicy()) {
			case OPTIONAL : {
				return null;
			}
			
			case EXCEPTION : {
				throwMissingException(client,relation);
			}
			
			case WAIT : {
				
				/*
				 * If a field is resolved before the owner is managed, there must be an error just fail
				 */
				Composite context 		= (client instanceof Instance) ? ((Instance)client).getComposite() : CompositeImpl.getRootAllComposites();
				ContentManager manager	= getManager(context);
				if (manager == null)
					return null;
				
				/*
				 * schedule request
				 */
				PendingRequest request = new PendingRequest((ApamResolverImpl)CST.apamResolver, client, relation);

				manager.addPendingRequest(request);
				request.block();
				manager.removePendingRequest(request);
				
				return request.getResolution();
			}
		}
		
		return null;
	}
	
	@Override
	public void addedComponent(Component component) {

		/*
		 * Get the list of currently existing managers
		 */
		Collection<ContentManager> managers = getManagers();

		/*
		 * Create a content manager associated to newly created composites
		 */
		if (component instanceof Composite) {
			
			Composite composite = (Composite) component;
			
			if (getManager(composite) != null) {
				logger.error("Composite already added in APAM "+composite.getName());
				return;
			}

			try {

				ContentManager manager = new ContentManager(this,composite);
				
				/*
				 * Validate there is no conflict in ownership declarations with existing composites
				 */
				for (ContentManager existingManager : managers) {
					Set<OwnedComponentDeclaration> conflicts = manager.getConflictingDeclarations(existingManager);
					if (!conflicts.isEmpty())
						throw new InvalidConfiguration("Invalid owned declaration, conflicts with "+existingManager.getComposite().getName()+":"+conflicts);
				}

				/*
				 * register manager
				 */
				synchronized (this) {
					contentManagers.put(composite,manager);
				}

				manager.start();

				/*
				 * For all the existing instances we consider the impact of the newly created composite
				 * in ownership
				 * 
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
		 * Verify that the context manager is registered in dynaman, otherwise postpone
		 * handling of the event
		 */
		Composite context 			= (component instanceof Instance) ? ((Instance)component).getComposite() : CompositeImpl.getRootAllComposites();
		ContentManager container	= getManager(context);
		if (container == null)
			return;

		/*
		 * Add the dynamic dependencies of the component
		 */
		
		container.updateDynamicDependencies(component);
		
		/*
		 * Verify ownership of newly created instances
		 */
		if (component instanceof Instance) {
			verifyOwnership((Instance) component);
		}

		/*
		 * Verify if this change may impact some pending or dynamic requests.
		 */
		for (ContentManager manager : managers) {
			manager.addedComponent(component);
		}
		
	}

	@Override
	public void removedComponent(Component component) {

		/*
		 * Verify that the context manager is registered in dynaman, otherwise postpone
		 * handling of the event
		 */
		Composite context 			= (component instanceof Instance) ? ((Instance)component).getComposite() : CompositeImpl.getRootAllComposites();
		ContentManager container	= getManager(context);
		if (container == null)
			return;

		/*
		 * Remove the component from the associated content manager
		 */
		container.removedComponent(component);

		/*
		 * Remove a content manager when its composite is removed
		 */
		if (component instanceof Composite) {
			
			ContentManager manager = getManager((Composite)component);
			
			synchronized (this) {
				contentManagers.remove(component);
			}

			manager.dispose();
}
		
		
	}
	
	@Override
	public void addedLink(Link link) {
		
		/*
		 * Verify if this change may impact some pending or dynamic requests.
		 */
		for (ContentManager manager : getManagers()) {
			manager.addedLink(link);
		}
	}


	@Override
	public void removedLink(Link link) {
		/*
		 * Verify if this change may impact some pending or dynamic requests.
		 */
		for (ContentManager manager : getManagers()) {
			manager.removedLink(link);
		}
	}
	
	private void propertyChanged(Component component, String property) {		
		
		/*
		 * If an instance attribute is modified, this may change ownership
		 */
		if (component instanceof Instance) {
			verifyOwnership((Instance) component);
		}

		/*
		 * Verify if this change may impact some existing pending or dynamic requests.
		 */
		for (ContentManager manager : getManagers()) {
			manager.propertyChanged(component, property);
		}

	}
	
	@Override
	public void attributeChanged(Component component, String attr, String newValue, String oldValue) {
		propertyChanged(component,attr);
	}

	@Override
	public void attributeRemoved(Component component, String attr, String oldValue) {
		propertyChanged(component,attr);
	}

	@Override
	public void attributeAdded(Component component, String attr, String newValue) {
		propertyChanged(component,attr);
	}
	
	
	/**
	 * Set the ownership of an instance to one of the requesting composites, signal any detected conflict
	 */
	private void verifyOwnership(Instance instance) {

		/*
		 * Verify that the current container is registered in dynaman, otherwise postpone
		 * handling of the event
		 */
		ContentManager container = getManager(instance.getComposite());
		if (container == null)
			return;
		
		Collection<ContentManager> managers = getManagers();
		ContentManager owner				= container.owns(instance) ? container : null;

		/*
		 * Get the list of composites requesting ownership.
		 */
		List<ContentManager> requesters = new ArrayList<ContentManager>();
		StringBuffer requestersNames	= new StringBuffer();
		
		for (ContentManager manager : managers) {
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

		boolean ownershipChanged = false;

		/*
		 * Revoke ownership to previous owner (if it has changed)
		 */
		if (owner != null && (newOwner == null || ! newOwner.equals(owner))) {
			owner.revokeOwnership(instance);
			ownershipChanged = true;
		}
		
		/*
		 * Grant ownership to new owner
		 */
		if (newOwner != null) {
			newOwner.grantOwnership(instance);
			ownershipChanged = true;
		}
		
		/*
		 * Verify if this change may impact some existing pending or dynamic requests.
		 */
		if (ownershipChanged) {
			for (ContentManager manager : managers) {
				manager.ownershipChanged(instance);
			}
		}
		
	}

	
	/**
	 * Throws the exception associated with a missing relation
	 */
	private static void throwMissingException(Component source, Relation relation) {
		try {

			/*
			 * If no exception is specified throw ResolutionException
			 */
			String exceptionName = relation.getMissingException();
			if (exceptionName == null)
				throw new ResolutionException();
			
			/*
			 * Try to find the component declaring the relation that specified the 
			 * Exception to throw
			 * 
			 * TODO BUG : the class should be loaded using the bundle context of
			 * the component where the relation is declared. This can be either
			 * the specification, or the implementation of the source component,
			 * or a composite in the case of contextual dependencies. The current
			 * implementation may not handle every case.
			 * 
			 * The best solution is to modify relationDeclaration to load the
			 * exception class, but this is not possible at compile time, so we
			 * can not change the signature of relationDeclaration.getMissingException.
			 * 
			 * A possible solution is to move this method to relationDeclaration and
			 * make it work only at runtime, but we need to consider merge of contextual
			 * dependencies and use the correct bundle context.
			 * 
			 * Evaluate changes to relationDeclaration, relation, CoreMetadataParser
			 * and computeEffectiverelation
			 */
			Component declaringComponent = source;
			while (declaringComponent != null) {
				
				RelationDeclaration declaration = declaringComponent.getDeclaration().getRelation(relation.getIdentifier());
				if ( declaration != null && declaration.getMissingException() != null && declaration.getMissingException().equals(exceptionName))
					break;
				
				declaringComponent = declaringComponent.getGroup();
			}
			
			if ( declaringComponent == null && source instanceof Instance) {
				declaringComponent = ((Instance) source).getComposite().getCompType();
			}
			
			if (declaringComponent == null)
				throw new ResolutionException();

			/*
			 * throw the specified exception
			 */
			
			
			Class<?> exceptionClass		= declaringComponent.getApformComponent().getBundle().loadClass(exceptionName);
			Exception exception			= Exception.class.cast(exceptionClass.newInstance());
			
			DynamicManagerImplementation.<RuntimeException>doThrow(exception);
		
		} catch (ClassNotFoundException e) {
			throw new ResolutionException(e);
		} catch (InstantiationException e) {
			throw new ResolutionException(e);
		} catch (IllegalAccessException e) {
			throw new ResolutionException(e);
		}
		
	}
	
	/**
	 * Hack to throw a checked exception from inside the framework without wrapping it.
	 * 
	 * When the formal type parameter E is replaced by the actual type argument RuntimeException the erased signature of
	 * this method seems to throw an unchecked exception, however we throw the original exception.
	 * 
	 * Currently at runtime the specified cast is a NOOP, this may not work depending on the used JVM
	 */
	@SuppressWarnings("unchecked")
	private static <E extends Exception> void doThrow(Exception e) throws E {
		throw (E) e;
	}
	
	
	/**
	 * Dynaman does not have its own model, all the information is in the component declaration.
	 * 
	 */
	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}
	

	@Override
	public void notifySelection(Component client, ResolvableReference resName, String depName, Implementation impl, Instance inst, Set<Instance> insts) {
	}

	@Override
	public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName, String componentName) {
		return null;
	}



}
