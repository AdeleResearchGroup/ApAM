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

import fr.imag.adele.apam.impl.CompositeTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class ApamManagers {

	private static Logger logger = LoggerFactory.getLogger(ApamManagers.class);

	/*
	 * The list of all managers
	 */
	private static Set<Manager> managers = new ConcurrentSkipListSet<Manager>(new Comparator<Manager>() {

		@Override
		public int compare(Manager manager1, Manager manager2) {
			return manager1.hashCode() - manager2.hashCode();
		}
	});

	/**
	 * The list of dependency managers, with their priorities
	 */
	private static Map<DependencyManager, Integer> dependencyManagersPrio	= new HashMap<DependencyManager, Integer>();
	private static List<DependencyManager>         dependencyManagers  		= new ArrayList<DependencyManager>();


	/**
	 * The list of dynamic manager listeners
	 * 
	 */
	private static Set<DynamicManager> dynamicManagers = new ConcurrentSkipListSet<DynamicManager>(new Comparator<DynamicManager>() {

		@Override
		public int compare(DynamicManager manager1, DynamicManager manager2) {
			return manager1.hashCode() - manager2.hashCode();
		}
	});

	/**
	 * The list of component property listeners
	 */
	private static Set<PropertyManager> propertyManagers = new ConcurrentSkipListSet<PropertyManager>(new Comparator<PropertyManager>() {

		@Override
		public int compare(PropertyManager manager1, PropertyManager manager2) {
			return manager1.hashCode() - manager2.hashCode();
		}
	});


	private static void register (Manager manager) {
		if (manager == null) {
			logger.error("ERROR : Missing parameter manager in  register Manager");
			return;
		}
		if (managers.contains(manager)) return ;
		managers.add(manager) ;
		
		//Managers without name are simple listeners.
		if (manager.getName() != null) {
			logger.info("[" + manager.getName() + "] registered an initialized") ;
			ManagerModel rootModel = CompositeTypeImpl.getRootCompositeType().getModel(manager.getName());
			// the root model maybe null
			manager.newComposite(rootModel, CompositeTypeImpl.getRootCompositeType());
		}
	}

	private static void unregister (Manager manager) {
		if (manager == null) {
			logger.error("ERROR : Missing parameter manager in  unregister Manager");
			return;
		}
		if (!managers.contains(manager)) {
			return ;
		}
		managers.remove(manager) ;
		if (manager.getName() != null) {
			logger.info("[" + manager.getName() + " unregistered]") ;
		}
	}

	/**
	 * Adds a manager to Apam.
	 * 
	 * @param manager
	 * @param priority : the relative priority. the lower the interger, the higher the priority. 0 is reserved for
	 *            apamman.
	 */
	public static void addDependencyManager(DependencyManager manager, int priority) {

		register (manager) ;
		if (manager == null) return ;

		if ((priority < 0) && !(manager.getName().equals(CST.APAMMAN) || manager.getName().equals(CST.UPDATEMAN))) {
			logger.error("invalid priority: " + priority + ">= 0 assumed");
			priority = 0;
		}
		
		boolean inserted = false;
		for (int i = 0; i < dependencyManagers.size(); i++) {
			if (priority <= dependencyManagers.get(i).getPriority()) {
				dependencyManagers.add(i, manager);
				inserted = true;
				break;
			}
		}
		if (!inserted) { // put it at the end
			dependencyManagers.add(manager) ;
		}

		ApamManagers.dependencyManagersPrio.put(manager, Integer.valueOf(priority));
	}

	public static DependencyManager getManager(String managerName) {
		if (managerName == null) {
			logger.error("ERROR : Missing parameter manager in getManager");
			return null;
		}
		for (DependencyManager man : dependencyManagers) {
			if (man.getName().equals(managerName))
				return man;
		}
		return null;
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static Set<Manager> getManagers() {
		return Collections.unmodifiableSet(managers);
	}

	/**
	 * 
	 * @return the list of known managers
	 */
	public static List<DependencyManager> getDependencyManagers() {
		return Collections.unmodifiableList(dependencyManagers);
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
	public static Set<PropertyManager> getPropertyManagers() {
		return Collections.unmodifiableSet(propertyManagers);
	}

	/**
	 * Remove the manager
	 * 
	 * @param manager
	 */
	public static void removeDependencyManager(DependencyManager manager) {
		unregister(manager) ;
		ApamManagers.dependencyManagersPrio.remove(manager);
		ApamManagers.dependencyManagers.remove(manager);
	}

	/**
	 * 
	 * @param manager
	 * @return the priortity of that manager. -1 is unknown.
	 */
	public static int getPriority(DependencyManager manager) {
		return ApamManagers.dependencyManagersPrio.get(manager);
	}


	/**
	 * Adds a new manager to listen for dynamic events
	 * 
	 */
	public static void addDynamicManager(DynamicManager manager) {
		register(manager);
		if (manager != null) 
			ApamManagers.dynamicManagers.add(manager);
	}

	public static void removeDynamicManager(DynamicManager manager) {
		unregister(manager);
		if  (manager != null) 
			ApamManagers.dynamicManagers.remove(manager);
	}

	/**
	 * This manager is interested in knowing when instance properties have been changed.
	 * 
	 * @param manager
	 */
	public static void addPropertyManager(PropertyManager manager) {
		register(manager);
		if (manager != null) 
			ApamManagers.propertyManagers.add(manager);
	}

	/**
	 * The manager is no longer interested in knowing when instance properties have been changed
	 * 
	 * @param manager
	 */
	public static void removePropertyManager(PropertyManager manager) {
		unregister(manager);
		if (manager != null) 
			ApamManagers.propertyManagers.remove(manager);
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

	/*
	 * Notification events for dynamic events
	 */
	public static void notifyAddedInApam(Component newComponent) {
		for (DynamicManager manager : ApamManagers.dynamicManagers) {
			manager.addedComponent(newComponent);
		}
	}

	public static void notifyRemovedFromApam(Component lostComponent) {
		for (DynamicManager manager : ApamManagers.dynamicManagers) {
			manager.removedComponent(lostComponent);
		}
	}
}
