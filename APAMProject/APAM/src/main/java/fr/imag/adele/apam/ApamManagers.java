package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.impl.APAMImpl;
import fr.imag.adele.apam.impl.CompositeTypeImpl;

public class ApamManagers {

    private static Logger logger = LoggerFactory.getLogger(ApamManagers.class);
	
    private static Map<DependencyManager, Integer> dependencyManagersPrio = new HashMap<DependencyManager, Integer>();
    private static List<DependencyManager>          dependencyManagerList  = new ArrayList<DependencyManager>();

    /**
     * The list of dynamic manager listeners
     * 
     * TODO this is a global list that is not filtered by the event that is expected, so that managers can
     * get spurious notifications. We should add some way to classify listeners according to the expected
     * event.
     */
    private static Set<DynamicManager> dynamicManagers = new ConcurrentSkipListSet<DynamicManager>();
    /**
     * The list of component property listeners
     */
    private static Set<PropertyManager> propertyManagers = new ConcurrentSkipListSet<PropertyManager>();


    /**
     * Adds a manager to Apam.
     * 
     * @param manager
     * @param priority : the relative priority. the lower the interger, the higher the priority. 0 is reserved for
     *            apamman.
     */
    public static void addDependencyManager(DependencyManager manager, int priority) {
        if ((priority < 0) && !manager.getName().equals(CST.APAMMAN)) {
            logger.error("invalid priority" + priority + ". 0 assumed");
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
        
        ManagerModel rootModel = CompositeTypeImpl.getRootCompositeType().getModel(manager.getName());
        if (rootModel != null) {
        	manager.newComposite(rootModel, CompositeTypeImpl.getRootCompositeType());
        }
        
        ApamManagers.dependencyManagersPrio.put(manager, new Integer(priority));
    }

    public static DependencyManager getManager(String managerName) {
        if (managerName == null) {
            logger.error("ERROR : Missing parameter manager in getManager");
            return null;
        }
        for (DependencyManager man : APAMImpl.managerList) {
            if (man.getName().equals(managerName))
                return man;
        }
        return null;
    }

    /**
     * 
     * @return the list of known managers
     */
    public static List<DependencyManager> getManagers() {
        return Collections.unmodifiableList(APAMImpl.managerList);
    }

    /**
     * Remove the manager
     * 
     * @param manager
     */
    public static void removeDependencyManager(DependencyManager manager) {
        ApamManagers.dependencyManagersPrio.remove(manager);
        ApamManagers.dependencyManagerList.remove(manager);
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
        if (manager == null) {
            logger.error("ERROR : Missing parameter manager in addDynamicManager");
            return;
        }
        ApamManagers.dynamicManagers.add(manager);
    }

    public static void removeDynamicManager(DynamicManager manager) {
        if  (manager == null) {
            logger.error("ERROR : Missing parameter interf or manager in appearedExpected");
            return;
        }
        ApamManagers.dynamicManagers.remove(manager);
    }

    /**
     * This manager is interested in knowing when instance properties have been changed.
     * 
     * @param manager
     */
    public static void addPropertyManager(PropertyManager manager) {
        if (manager == null) {
            logger.error("ERROR : Missing parameter manager in addAttributeListener");
            return;
        }
        ApamManagers.propertyManagers.add(manager);
    }

    /**
     * The manager is no longer interested in knowing when instance properties have been changed
     * 
     * @param manager
     */
    public static void removePropertyManager(PropertyManager manager) {
        if (manager == null) {
           logger.error("ERROR : Missing parameter manager in removeAttributeListener");
            return;
        }

        ApamManagers.propertyManagers.remove(manager);
    }

    /*
     * Notification events for property changes
     */

    public static void notifyAttributeAdded(Component component, String attr, Object value) {
        for (PropertyManager manager : ApamManagers.propertyManagers) {
            manager.attributeAdded(component, attr, value);
        }	
    }

    public static void notifyAttributeChanged(Component component, String attr, Object newValue, Object oldValue) {
        for (PropertyManager manager : ApamManagers.propertyManagers) {
            manager.attributeChanged(component, attr, newValue, oldValue);
        }	
    }

    public static void notifyAttributeRemoved(Component component, String attr, Object oldValue) {
        for (PropertyManager manager : ApamManagers.propertyManagers) {
            manager.attributeRemoved(component, attr, oldValue);
        }	
    }

    /*
     * Notification events for dynamic events
     */
    public static void notifyExternal(Instance inst) {
        for (DynamicManager manager : ApamManagers.dynamicManagers) {
            manager.external(inst);
        }
    }

    public static void notifyAddedInApam(Instance newInst) {
        for (DynamicManager manager : ApamManagers.dynamicManagers) {
            manager.addedInApam(newInst);
        }
    }

    public static void notifyAddedInApam(Implementation newImpl) {
        for (DynamicManager manager : ApamManagers.dynamicManagers) {
            manager.addedInApam(newImpl);
        }
    }

    public static void notifyRemovedFromApam(Instance lostInst) {
        for (DynamicManager manager : ApamManagers.dynamicManagers) {
            manager.removedFromApam(lostInst);
        }
    }

    public static void notifyRemovedFromApam(Implementation lostImpl) {
        for (DynamicManager manager : ApamManagers.dynamicManagers) {
            manager.removedFromApam(lostImpl);
        }
    }
    //
    //
    //    public static void notifyDeleted(Instance lost) {
    //        for (DynamicManager manager : ApamManagers.dynamicManagers) {
    //            manager.deleted(lost);
    //        }
    //    }
    //
    //    public static void notifyDeployed(CompositeType composite, Implementation implementation) {
    //        for (DynamicManager manager : ApamManagers.dynamicManagers) {
    //            manager.deployed(composite,implementation);
    //        }
    //    }
    //
    //    public static void notifyUninstalled(CompositeType composite, Implementation implementation) {
    //        for (DynamicManager manager : ApamManagers.dynamicManagers) {
    //            manager.uninstalled(composite,implementation);
    //        }
    //    }
    //
    //    public static void notifyHidden(CompositeType composite, Implementation implementation){
    //        for (DynamicManager manager : ApamManagers.dynamicManagers) {
    //            manager.hidden(composite,implementation);
    //        }
    //    }


}
