package fr.imag.adele.apam.apamAPI;

import java.util.List;

public interface ManagersMng {

    /**
     * Adds a manager to Apam.
     * 
     * @param manager
     * @param priority : the relative priority. the lower the interger, the higher the priority. 0 is reserved for
     *            apamman.
     */
    public void addManager(Manager manager, int priority);

    /**
     * 
     * @return the list of known managers
     */
    public List<Manager> getManagers();

    /**
     * 
     * @param manager
     * @return the priortity of that manager. -1 is unknown.
     */
    public int getPriority(Manager manager);

    /**
     * Remove the manager
     * 
     * @param manager
     */
    public void removeManager(Manager manager);

    /**
     * 
     * @param managerName
     * @return The manager of that name. Null if not found.
     */
    public Manager getManager(String managerName);

    /**
     * This manager is expecting the provided implementation or interface to appear. Warning : it is an
     * interface name.
     * 
     * @param impl : The APAM implementation for which an instance is expected
     * @param impl : The interface name for which an instance is expected. It may correspond to more than one Spec
     * @param manager
     */
    public void appearedImplExpected(String samImplName, DynamicManager manager);

    public void appearedInterfExpected(String interf, DynamicManager manager);

    public void appearedImplNotExpected(String samImplName, DynamicManager manager);

    public void appearedInterfNotExpected(String interf, DynamicManager manager);

    /**
     * This manager is interested in knowing which instance disappear.
     * 
     * @param manager
     */
    public void listenLost(DynamicManager manager);

    /**
     * The manager is no longer interested in the disparitions
     * 
     * @param manager
     */
    public void listenNotLost(DynamicManager manager);

    /**
     * This manager is interested in knowing when instance properties have been changed in SAM.
     * 
     * @param manager
     */
    public void listenAttrChanged(AttributeManager manager);

    /**
     * The manager is no longer interested in knowing when instance properties have been changed
     * 
     * @param manager
     */
    public void listenNotAttrChanged(AttributeManager manager);

}
