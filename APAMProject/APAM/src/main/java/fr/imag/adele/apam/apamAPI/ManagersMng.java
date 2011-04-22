package fr.imag.adele.apam.apamAPI;

import java.util.List;

public interface ManagersMng {
    public void addManager(Manager manager, int priority);

    public List<Manager> getManagers();

    public int getPriority(Manager manager);

    public void removeManager(Manager manager);

    public Manager getManager(String managerName);

    /**
     * This manager is expecting the apparation of the provided implementation or interface. Warning : it is an
     * interface name, not a Spec ID.
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

    public void listenNotLost(DynamicManager manager);

    /**
     * This manager is interested in knowing when instance properties have been changed in SAM.
     * 
     * @param manager
     */
    public void listenAttrChanged(AttributeManager manager);

    public void listenNotAttrChanged(AttributeManager manager);

}
