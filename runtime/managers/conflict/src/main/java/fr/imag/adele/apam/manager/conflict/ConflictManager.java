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
package fr.imag.adele.apam.manager.conflict;

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
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.CompositeImpl;
import fr.imag.adele.apam.impl.PendingRequest;

/**
 * This class is the entry point of the dynamic manager implementation.
 * 
 * 
 * @author vega
 * 
 */
@Instantiate(name = "ConflictManager-Instance")
@org.apache.felix.ipojo.annotations.Component(name = "ConflictManager", immediate = true)
@Provides
public class ConflictManager implements RelationManager, DynamicManager,
	PropertyManager {

    private final static Logger logger = LoggerFactory
	    .getLogger(ConflictManager.class);

    /**
     * A reference to the APAM machine
     */
    @Requires(proxy = false)
    private Apam apam;

    /**
     * The content managers of all composites in APAM
     */
    private Map<Composite, ContentManager> contentManagers;

    /**
     * The content manager associated with the root composite
     */
    private ContentManager rootManager;

    public ConflictManager(BundleContext context) {
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
		logger.error("Composite already added in APAM "
			+ composite.getName());
		return;
	    }

	    try {

		ContentManager manager = new ContentManager(this, composite);

		/*
		 * Validate there is no conflict in ownership declarations with
		 * existing composites
		 */
		for (ContentManager existingManager : managers) {
		    Set<OwnedComponentDeclaration> conflicts = manager
			    .getConflictingDeclarations(existingManager);
		    if (!conflicts.isEmpty()) {
			throw new InvalidConfiguration(
				"Invalid owned declaration, conflicts with "
					+ existingManager.getComposite()
						.getName() + ":" + conflicts);
		    }
		}

		/*
		 * register manager
		 */
		synchronized (this) {
		    contentManagers.put(composite, manager);
		}

		manager.start();

		/*
		 * For all the existing instances we consider the impact of the
		 * newly created composite in ownership
		 */
		for (Instance instance : CST.componentBroker.getInsts()) {
		    verifyOwnership(instance);
		}

	    } catch (InvalidConfiguration error) {

		/*
		 * TODO We should not add the composite in APAM if the content
		 * manager could not be created, but currently there is no way
		 * for a manager to signal an error in creation.
		 */
		logger.error("Error creating content manager for composite "
			+ component.getName(), error);
	    }
	}

	/*
	 * Verify ownership of newly created instances
	 */
	if (component instanceof Instance) {
	    verifyOwnership((Instance) component);
	}

    }

    @Override
    public void addedLink(Link link) {
    }

    @Override
    public void attributeAdded(Component component, String attr, String newValue) {
	propertyChanged(component, attr);
    }

    @Override
    public void attributeChanged(Component component, String attr,
	    String newValue, String oldValue) {
	propertyChanged(component, attr);
    }

    @Override
    public void attributeRemoved(Component component, String attr,
	    String oldValue) {
	propertyChanged(component, attr);
    }

    @Override
    public ComponentBundle findBundle(CompositeType compoType,
	    String bundleSymbolicName, String componentName) {
	return null;
    }

    /**
     * Give access to the APAM reference
     */
    public Apam getApam() {
	return apam;
    }

    /**
     * Get the manager associated to a composite
     */
    private synchronized ContentManager getManager(Composite composite) {
	return composite != null ? contentManagers.get(composite)
		: contentManagers.get(CompositeImpl.getRootAllComposites());
    }

    /**
     * Get a thread safe (stack contained) copy of the current list of managers
     */
    private synchronized Collection<ContentManager> getManagers() {
	return new ArrayList<ContentManager>(contentManagers.values());
    }

    @Override
    public String getName() {
	return "ConflictManager";
    }

    @Override
    public int getPriority() {
	return 5;
    }

    /**
     * For owned instances that could match a resolution request, we must be
     * sure that the grants are respected.
     */
    @Override
    public void getSelectionPath(Component client, RelToResolve relation,
	    List<RelationManager> selPath) {

	if (!relation.getTargetKind().equals(ComponentKind.INSTANCE)) {
	    return;
	}

	/**
	 * Iterate over all owned instances that could satisfy this request, and
	 * verify if it is granted access.
	 * 
	 * WARNING Notice that this is a global validation, irrespective of
	 * composites. We verify all visible instances that could satisfy the
	 * request.
	 */

	PendingRequest request = PendingRequest.isRetry() ? PendingRequest
		.current() : new PendingRequest(CST.apamResolver, client,
		relation.getRelationDefinition());

	for (ContentManager container : getManagers()) {
	    container.verifyGrant(request);
	}
    }

    /**
     * Dynaman does not have its own model, all the information is in the
     * component declaration.
     * 
     */
    @Override
    public void newComposite(ManagerModel model, CompositeType composite) {
    }

    @Override
    public void notifySelection(Component client, ResolvableReference resName,
	    String depName, Implementation impl, Instance inst,
	    Set<Instance> insts) {
    }

    private void propertyChanged(Component component, String property) {

	/*
	 * If an instance attribute is modified, this may change ownership
	 * and/or the state of its container
	 */
	if (component instanceof Instance) {

	    verifyOwnership((Instance) component);

	    ContentManager container = getManager(((Instance) component)
		    .getComposite());
	    if (container != null) {
		container.propertyChanged((Instance) component, property);
	    }
	}

    }

    @Override
    public void removedComponent(Component component) {

	/*
	 * Remove destroyed instance from the content manager of its container
	 */
	if (component instanceof Instance) {
	    ContentManager container = getManager(((Instance) component)
		    .getComposite());
	    if (container != null) {
		container.removedInstance((Instance) component);
	    }
	}

	/*
	 * Remove a content manager when its composite is removed
	 */
	if (component instanceof Composite) {
	    ContentManager manager = getManager((Composite) component);

	    synchronized (this) {
		contentManagers.remove(component);
	    }

	    manager.dispose();
	}
    }

    @Override
    public void removedLink(Link link) {
    }

    /**
     * This manager only handles conflicts, it doesn't resolve relations. It
     * only participates in resolution to add constraints to enforce conflict
     * management rules.
     */
    @Override
    public Resolved<?> resolveRelation(Component client, RelToResolve relation) {
	return null;
    }

    /**
     * This method is automatically invoked when the manager is validated, so we
     * can safely assume that APAM is available
     * 
     */
    @Validate
    private synchronized void start() {

	/*
	 * Create the default content manager to be associated with the root
	 * composite
	 */
	try {

	    contentManagers = new HashMap<Composite, ContentManager>();

	    Composite root = CompositeImpl.getRootAllComposites();
	    rootManager = new ContentManager(this, root);

	    contentManagers.put(root, rootManager);
	    rootManager.start();

	} catch (InvalidConfiguration ignored) {
	}

	/*
	 * Register with APAM
	 */
	ApamManagers.addRelationManager(this, getPriority());
	ApamManagers.addDynamicManager(this);
	ApamManagers.addPropertyManager(this);

	/*
	 * TODO if conflict manager is started or restarted after APAM, should
	 * we verify if there are already created composites?
	 */
    }

    /**
     * This method is automatically invoked when the manager is invalidated, so
     * APAM is no longer available
     */
    @Invalidate
    private synchronized void stop() {
	ApamManagers.removeRelationManager(this);
	ApamManagers.removeDynamicManager(this);
	ApamManagers.removePropertyManager(this);
    }

    /**
     * Set the ownership of an instance to one of the requesting composites,
     * signal any detected conflict
     */
    private void verifyOwnership(Instance instance) {

	/*
	 * Verify that the current container is registered in dynaman, otherwise
	 * postpone handling of the event
	 */
	ContentManager container = getManager(instance.getComposite());
	if (container == null) {
	    return;
	}

	Collection<ContentManager> managers = getManagers();
	ContentManager owner = container.owns(instance) ? container : null;

	/*
	 * Get the list of composites requesting ownership.
	 */
	List<ContentManager> requesters = new ArrayList<ContentManager>();
	StringBuffer requestersNames = new StringBuffer();

	for (ContentManager manager : managers) {
	    if (manager.shouldOwn(instance)) {
		requesters.add(manager);

		requestersNames.append(" ");
		requestersNames.append(manager.getComposite().getName());
	    }
	}

	/*
	 * If there is conflicting requests signal an error
	 * 
	 * TODO In some cases we could do more than simply logging the error.
	 * Perhaps avoiding creating composites that will produce conflicts.
	 */
	if (requesters.size() > 1) {
	    logger.error("Conflict in ownership : composites ("
		    + requestersNames + ") request ownership of instance "
		    + instance.getName());
	}

	/*
	 * If there is no ownership request continue processing event
	 */
	if (requesters.isEmpty()) {
	    return;
	}

	/*
	 * Choose an owner arbitrarily among requesters (try to keep the current
	 * owner if exists)
	 */
	ContentManager newOwner = requesters.isEmpty() ? null : requesters
		.contains(container) ? container : requesters.get(0);

	/*
	 * Revoke ownership to previous owner (if it has changed)
	 */
	if (owner != null && (newOwner == null || !newOwner.equals(owner))) {
	    owner.revokeOwnership(instance);
	}

	/*
	 * Accord ownership to new owner
	 */
	if (newOwner != null) {
	    newOwner.accordOwnership(instance);
	}
    }

}
