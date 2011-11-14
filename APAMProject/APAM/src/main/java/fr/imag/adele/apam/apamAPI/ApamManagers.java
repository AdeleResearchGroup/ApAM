package fr.imag.adele.apam.apamAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.imag.adele.apam.APAMImpl;
import fr.imag.adele.apam.apform.ApformImpl;
import fr.imag.adele.apam.util.AttributesImpl;

public class ApamManagers {

    private static Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer>();
    public static List<Manager>          managerList  = new ArrayList<Manager>();

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
     * This manager is expecting the provided implementation or interface to appear. Warning : it is an
     * interface name.
     * 
     * @param impl : The APAM implementation for which an instance is expected
     * @param impl : The interface name for which an instance is expected. It may correspond to more than one Spec
     * @param manager
     */
    public static void appearedImplExpected(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null)) {
            System.err.println("ERROR : Missing parameter impl or manager in appearedExpected");
            return;
        }
        ApformImpl.addExpectedImpl(samImplName, manager);
    }

    public static void appearedInterfExpected(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter interf or manager in appearedExpected");
            return;
        }
        ApformImpl.addExpectedInterf(interf, manager);
    }

    public static void appearedImplNotExpected(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter impl or manager in appearedNotExpected");
            return;
        }
        ApformImpl.removeExpectedImpl(samImplName, manager);

    }

    public static void appearedInterfNotExpected(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter interf or manager in appearedNotExpected");
            return;
        }
        ApformImpl.removeExpectedInterf(interf, manager);
    }

    public static void listenLost(DynamicManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenLost");
            return;
        }
        ApformImpl.addLost(manager);
    }

    /**
     * This manager is interested in knowing when instance properties have been changed in SAM.
     * 
     * @param manager
     */
    public static void listenAttrChanged(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenAttrChanged");
            return;
        }
        AttributesImpl.addAttrChanged(manager);
    }

    /**
     * The manager is no longer interested in the disparitions
     * 
     * @param manager
     */
    public static void listenNotLost(DynamicManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenNotLost");
            return;
        }
        ApformImpl.removeLost(manager);
    }

    /**
     * The manager is no longer interested in knowing when instance properties have been changed
     * 
     * @param manager
     */
    public static void listenNotAttrChanged(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenNotAttrChanged");
            return;
        }
        AttributesImpl.removeAttrChanged(manager);
    }

}
