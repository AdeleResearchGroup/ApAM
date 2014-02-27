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
package fr.imag.adele.apam;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.impl.APAMImpl;
import fr.imag.adele.apam.impl.CompositeTypeImpl;

public class ApamManagers {

	private static Logger logger = LoggerFactory.getLogger(ApamManagers.class);

	/*
	 * A thread-safe set of managers, optionally ordered by a given order.
	 *  
	 */
	private static class ManagerSet<M extends Manager> extends ConcurrentSkipListSet<M> {
		
		private static final long serialVersionUID = 2842254142921026458L;

		public ManagerSet(Comparator<? super M> order) {
			super(order);
		}
		
		public ManagerSet() {
			this(NAME_ORDER);
		}
	} 
	
	/**
	 * The default order for manager sets,is based on the lexicographic order of the name
	 */
	private final static Comparator<Manager> NAME_ORDER = new Comparator<Manager>() {

		@Override
		public int compare(Manager manager1, Manager manager2) {
			return manager1.getName().compareTo(manager2.getName());
		}
	};

	/**
	 * The set of registered managers, classified by the kind of manager
	 */
	private static Set<Manager> managers 						= new ManagerSet<Manager>();
	
	private static Set<ContextualManager> contextualManagers	= new ManagerSet<ContextualManager>();
	private static Set<DeploymentManager> deploymentManagers	= new ManagerSet<DeploymentManager>();
	private static Set<DynamicManager> dynamicManagers 			= new ManagerSet<DynamicManager>();
	private static Set<PropertyManager> propertyManagers 		= new ManagerSet<PropertyManager>();

	/**
	 * This class represents an order for managers based on priorities, as defined for relation managers.
	 * 
	 * 	- If no priority is specified for a manager, it is considered low priority.
	 *  - If two managers have the same priority, the default name based order is used to break tie 
	 */
	private static class PriorityOrder implements Comparator<RelationManager> {

		private final Map<RelationManager,RelationManager.Priority> priorities;
		
		public PriorityOrder() {
			priorities = new HashMap<RelationManager, RelationManager.Priority>();
		}
		
		public synchronized void setPriority(RelationManager manager, RelationManager.Priority priority) {
			priorities.put(manager, priority);
		}
		
		private synchronized RelationManager.Priority getPriority(RelationManager manager) {
			RelationManager.Priority priority = priorities.get(manager);
			return priority != null ? priority : RelationManager.Priority.LOW; 
		}
		
		@Override
		public int compare(RelationManager manager1, RelationManager manager2) {
			
			RelationManager.Priority priority1 = getPriority(manager1);
			RelationManager.Priority priority2 = getPriority(manager2);
			
			return priority1 != priority2 ? priority1.compareTo(priority2) : NAME_ORDER.compare(manager1, manager2);
		}
		
	}
	
	/**
	 * The list of relation managers, with their priorities
	 */
	private static PriorityOrder priorities 					= new PriorityOrder();
	private static SortedSet<RelationManager> relationManagers 	= new ManagerSet<RelationManager>(priorities);


	private static void register(Manager manager) {
		
		if (managers.contains(manager)) {
			return;
		}
		
		managers.add(manager);

		((APAMImpl) CST.apam).managerRegistered(manager);
		logger.info("[" + manager.getName() + "] registered and initialized");

		/*
		 * Initialize contextual managers for all context that were created before the
		 * manager registered
		 */
		if (manager instanceof ContextualManager) {
			initializeContextualManager(ContextualManager.class.cast(manager),CompositeTypeImpl.getRootCompositeType());
		}
		
		if (manager instanceof DeploymentManager)
			deploymentManagers.add((DeploymentManager)manager);
		if (manager instanceof ContextualManager)
			contextualManagers.add((ContextualManager)manager);
	}

	private static void initializeContextualManager(ContextualManager manager, CompositeType context) {
		manager.initializeContext(context);
		for (CompositeType nested : context.getEmbedded()) {
			initializeContextualManager(manager,nested);
		}
	}
	
	private static void unregister(Manager manager) {

		boolean managerRemoved = managers.remove(manager);

		if (managerRemoved && manager.getName() != null) {
			logger.info("[" + manager.getName() + " unregistered]");
		} else {
			logger.error("[" + manager.getName() + " could NOT be unregistered]");
		}
		
		if (manager instanceof DeploymentManager)
			deploymentManagers.remove(manager);
		if (manager instanceof ContextualManager)
			contextualManagers.remove((ContextualManager)manager);

	}

	/**
	 * Adds a new manager to listen for dynamic events
	 * 
	 */
	public static void addDynamicManager(DynamicManager manager) {
		register(manager);
		ApamManagers.dynamicManagers.add(manager);
	}

	/**
	 * This manager is interested in knowing when instance properties have been
	 * changed.
	 * 
	 * @param manager
	 */
	public static void addPropertyManager(PropertyManager manager) {
		register(manager);
		ApamManagers.propertyManagers.add(manager);
	}

	/**
	 * Adds a manager to Apam.
	 * 
	 * @param manager
	 * @param priority
	 *            : the relative priority. the lower the interger, the higher
	 *            the priority. 0 is reserved for apamman.
	 */
	public static void addRelationManager(RelationManager manager, RelationManager.Priority priority) {
		register(manager);
		
		priorities.setPriority(manager, priority);
		relationManagers.add(manager);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static Set<DynamicManager> getDynamicManagers() {
		return Collections.unmodifiableSet(dynamicManagers);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static Set<DeploymentManager> getDeploymentManagers() {
		return Collections.unmodifiableSet(deploymentManagers);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static Set<ContextualManager> getContextualManagers() {
		return Collections.unmodifiableSet(contextualManagers);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static Set<PropertyManager> getPropertyManagers() {
		return Collections.unmodifiableSet(propertyManagers);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static SortedSet<RelationManager> getRelationManagers() {
		return Collections.unmodifiableSortedSet(relationManagers);
	}


	/*
	 * Notification events for dynamic events
	 */
	public static void notifyAddedInApam(Component newComponent) {
		for (DynamicManager manager : ApamManagers.dynamicManagers) {
			manager.addedComponent(newComponent);
		}
	}

	/*
	 * Notification events for property changes
	 */
	public static void notifyAttributeAdded(Component component, String attr, String value) {
		for (PropertyManager manager : ApamManagers.propertyManagers) {
			manager.attributeAdded(component, attr, value);
		}
	}

	public static void notifyAttributeChanged(Component component, String attr, String newValue, String oldValue) {
		for (PropertyManager manager : ApamManagers.propertyManagers) {
			manager.attributeChanged(component, attr, newValue, oldValue);
		}
	}

	public static void notifyAttributeRemoved(Component component, String attr, String oldValue) {
		for (PropertyManager manager : ApamManagers.propertyManagers) {
			manager.attributeRemoved(component, attr, oldValue);
		}
	}

	public static void notifyRemovedFromApam(Component lostComponent) {
		for (DynamicManager manager : ApamManagers.dynamicManagers) {
			manager.removedComponent(lostComponent);
		}
	}


	public static void removeDynamicManager(DynamicManager manager) {
		unregister(manager);
		boolean managerRemoved = ApamManagers.dynamicManagers.remove(manager);
		if (!managerRemoved) {
			logger.error("impossible to remove dynamic manager {}", manager.getName());
		}
	}

	/**
	 * The manager is no longer interested in knowing when instance properties
	 * have been changed
	 * 
	 * @param manager
	 */
	public static void removePropertyManager(PropertyManager manager) {
		unregister(manager);
		boolean managerRemoved = ApamManagers.propertyManagers.remove(manager);
		if (!managerRemoved) {
			logger.error("impossible to remove property manager {}", manager.getName());
		}
	}

	/**
	 * Remove the manager
	 * 
	 * @param manager
	 */
	public static void removeRelationManager(RelationManager manager) {
		unregister(manager);
		boolean removedManager = ApamManagers.relationManagers.remove(manager);
		if (!removedManager) {
			logger.error("impossible to remove manager {} from main list", manager.getName());
		}

	}

}
