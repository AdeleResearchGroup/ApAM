package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import fr.imag.adele.apam.apamImpl.APAMImpl;

public class ApamManagers {

    private static Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer>();
    private static List<Manager>          managerList  = new ArrayList<Manager>();

    /**
     * Adds a manager to Apam.
     * 
     * @param manager
     * @param priority : the relative priority. the lower the interger, the higher the priority. 0 is reserved for
     *            apamman.
     */
    public static void addManager(Manager manager, int priority) {
        if ((priority < 0) && !manager.getName().equals(APAMImpl.apamMan.getName())) {
            System.err.println("invalid priority" + priority + ". 0 assumed");
            priority = 0;
        }
        boolean inserted = false;
        for (int i = 0; i < APAMImpl.managerList.size(); i++) {
            if (priority <= APAMImpl.managerList.get(i).getPriority()) {
                APAMImpl.managerList.add(i, manager);
                inserted = true;
                break;
            }
        }
        if (!inserted) { // at the end
            APAMImpl.managerList.add(manager);
        }
        ApamManagers.managersPrio.put(manager, new Integer(priority));
    }

    public static Manager getManager(String managerName) {
        if (managerName == null) {
            System.err.println("ERROR : Missing parameter manager in getManager");
            return null;
        }
        for (Manager man : APAMImpl.managerList) {
            if (man.getName().equals(managerName))
                return man;
        }
        return null;
    }

    /**
     * 
     * @return the list of known managers
     */
    public static List<Manager> getManagers() {
        return Collections.unmodifiableList(APAMImpl.managerList);
    }

    /**
     * Remove the manager
     * 
     * @param manager
     */
    public static void removeManager(Manager manager) {
        ApamManagers.managersPrio.remove(manager);
        ApamManagers.managerList.remove(manager);
    }

    /**
     * 
     * @param manager
     * @return the priortity of that manager. -1 is unknown.
     */
    public static int getPriority(Manager manager) {
        return ApamManagers.managersPrio.get(manager);
    }

    /**
     * The list of dynamic manager listeners
     * 
     * TODO this is a global list that is not filtered by the event that is expected, so that managers can
     * get spurious notifications. We should add some way to classify listeners according to the expected
     * event.
     */
    private static Set<DynamicManager> dynamicManagers = new ConcurrentSkipListSet<DynamicManager>();

    /**
     * Adds a new manager to listen for dynamic events
     * 
     */
    public static void addDynamicManager(DynamicManager manager) {
        if (manager == null) {
            System.err.println("ERROR : Missing parameter manager in addDynamicManager");
            return;
        }
        dynamicManagers.add(manager);
    }

    public static void removeDynamicManager(DynamicManager manager) {
        if  (manager == null) {
            System.out.println("ERROR : Missing parameter interf or manager in appearedExpected");
            return;
        }
        dynamicManagers.remove(manager);
    }

    /**
     * The list of component property listeners
     */
    private static Set<AttributeManager> attributeListeners = new ConcurrentSkipListSet<AttributeManager>();
    
    /**
     * This manager is interested in knowing when instance properties have been changed.
     * 
     * @param manager
     */
    public static void addAttributeListener(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in addAttributeListener");
            return;
        }
        attributeListeners.add(manager);
    }
    
    /**
     * The manager is no longer interested in knowing when instance properties have been changed
     * 
     * @param manager
     */
    public static void removeAttributeListener(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in removeAttributeListener");
            return;
        }
        
        attributeListeners.remove(manager);
    }

    /*
     * Notification events for property changes
     */
	public static void attributeChanged(Instance inst, String attr, Object newValue) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(inst, attr, newValue);
		}	
	}
	
	public static void attributeRemoved(Instance inst, String attr, Object oldValue){
		for (AttributeManager manager : attributeListeners) {
			manager.attributeRemoved(inst, attr, oldValue);
		}	
	}

	public static void attributeAdded(Instance inst, String attr, Object value) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(inst, attr, value);
		}	
	}

	public static void attributeChanged(Implementation impl, String attr, Object newValue) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(impl, attr, newValue);
		}	
	}
	
	public static void attributeRemoved(Implementation impl, String attr, Object oldValue) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeRemoved(impl, attr, oldValue);
		}	
	}

	public static void attributeAdded(Implementation impl, String attr, Object value) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(impl, attr, value);
		}	
	}

	public static void attributeChanged(Specification spec, String attr, Object newValue){
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(spec, attr, newValue);
		}	
	}

	public static void attributeRemoved(Specification spec, String attr, Object oldValue) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeRemoved(spec, attr, oldValue);
		}	
	}

	public static void attributeAdded(Specification spec, String attr, Object value) {
		for (AttributeManager manager : attributeListeners) {
			manager.attributeAdded(spec, attr, value);
		}	
	}
	
    /*
     * Notification events for dynamic events
     */
    public static void appeared(Instance inst) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.appeared(inst);
		}
    }
    
    public static void instantiated(Instance inst) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.instantiated(inst);
		}
    }

    public static void disappeared(Instance lost) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.disappeared(lost);
		}
    }
    
    public static void deleted(Instance lost) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.deleted(lost);
		}
    }
    
    public static void deployed(CompositeType composite, Implementation implementation) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.deployed(composite,implementation);
		}
    }
    
    public static void uninstalled(CompositeType composite, Implementation implementation) {
    	for (DynamicManager manager : dynamicManagers) {
			manager.uninstalled(composite,implementation);
		}
    }
    
    public static void hidden(CompositeType composite, Implementation implementation){
    	for (DynamicManager manager : dynamicManagers) {
			manager.hidden(composite,implementation);
		}
    }
    
	
}
