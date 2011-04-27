package fr.imag.adele.apam;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.ASMImpl.SamInstEventHandler;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class APAMImpl implements Apam, ApamClient, ManagersMng {

    // The applications
    private static Map<String, Application> applications = new ConcurrentHashMap<String, Application>();
    private static Manager                  apamMan;

    // int is the priority
    private static Map<Manager, Integer>    managersPrio = new HashMap<Manager, Integer>();
    private static List<Manager>            managerList  = new ArrayList<Manager>();

    public APAMImpl() {
        new CST(this);
        APAMImpl.apamMan = new ApamMan();
        addManager(APAMImpl.apamMan, -1); // -1 to be sure it is not in the main loop
    }

    @Override
    public ASMInst faultWire(ASMInst client, ASMInst lostInstance, String depName) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * An APAM client instance requires to be wired with an instance implementing the specification. WARNING : if no
     * logical name is provided, since more than one specification can implement the same interface, any specification
     * implementing the provided interface (technical name of the interface) will be considered satisfactory. If found,
     * the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @return
     */
    @Override
    public ASMInst newWireSpec(ASMInst client, String interfaceName, String specName, String depName) {
        ASMInst inst = newWireSpec0(client, interfaceName, specName, depName, false, null);
        if (inst == null) {
            if (specName != null)
                System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            if (interfaceName != null)
                System.out.println("Failed to resolve " + interfaceName + " from " + client + "(" + depName + ")");
            return null;
        }
        return inst;
    }

    @Override
    public Set<ASMInst> newWireSpecs(ASMInst client, String interfaceName, String specName, String depName) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        newWireSpec0(client, interfaceName, specName, depName, true, allInst);
        if (allInst.isEmpty()) {
            if (specName != null)
                System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            if (interfaceName != null)
                System.out.println("Failed to resolve " + interfaceName + " from " + client + "(" + depName + ")");
        }
        return allInst;
    }

    public ASMInst newWireSpec0(ASMInst client, String interfaceName, String specName, String depName,
            boolean multiple, Set<ASMInst> allInst) {
        Set<Filter> constraints = new HashSet<Filter>();

        // first step : compute selection path and constraints
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        // Call all managers in their priority order
        // Each manager can change the order of managers in the selectionPath, and even remove.
        // It can add itself or not. If no involved it should do nothing.
        List<Manager> selectionPath = new ArrayList<Manager>();
        List<Manager> previousSelectionPath;
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // Start from one to skip ApamMan. Added after the loop.
            previousSelectionPath = selectionPath;
            selectionPath = APAMImpl.managerList.get(i).getSelectionPathSpec(client, interfaceName, specName, depName,
                    constraints, selectionPath);
            if (selectionPath == null)
                selectionPath = previousSelectionPath;
        }
        // To select first the APAM manager
        selectionPath.add(0, APAMImpl.apamMan);

        // third step : ask each manager in the order
        Set<ASMInst> insts = null;
        ASMInst inst = null;
        for (Manager manager : selectionPath) {
            if (multiple) {
                insts = manager.resolveSpecs(client, interfaceName, specName, depName, constraints);
                if (insts != null) {
                    for (ASMInst ins : insts) {
                        if (client.createWire(ins, depName)) {
                            allInst.add(ins);
                        }
                    }
                }
            } else {
                inst = manager.resolveSpec(client, interfaceName, specName, depName, constraints);
                if ((inst != null) && (client.createWire(inst, depName)))
                    return inst;
            }
        }
        return null;
    }

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name
     * @return
     */
    @Override
    public ASMInst newWireImpl(ASMInst client, String samImplName, String implName, String depName) {
        ASMInst inst = newWireImpl0(client, samImplName, implName, depName, false, null);
        if (inst == null) {
            if (implName != null)
                System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            if (samImplName != null)
                System.out.println("Failed to resolve " + samImplName + " from " + client + "(" + depName + ")");
        }
        return inst;
    }

    @Override
    public Set<ASMInst> newWireImpls(ASMInst client, String samImplName, String implName, String depName) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        ASMInst inst = newWireImpl0(client, samImplName, implName, depName, true, allInst);
        if (allInst.isEmpty()) {
            if (implName != null)
                System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            if (samImplName != null)
                System.out.println("Failed to resolve " + samImplName + " from " + client + "(" + depName + ")");
        }
        return allInst;
    }

    public ASMInst newWireImpl0(ASMInst client, String samImplName, String implName, String depName, boolean multiple,
            Set<ASMInst> allInst) {

        // first step : compute selection path and constraints
        Set<Filter> constraints = new HashSet<Filter>();
        List<Manager> selectionPath = new ArrayList<Manager>();

        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            selectionPath = APAMImpl.managerList.get(i).getSelectionPathImpl(client, samImplName, implName, depName,
                    constraints, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);

        // third step : ask each manager in the order

        Set<ASMInst> insts = null;
        ASMInst inst = null;
        for (Manager manager : selectionPath) {
            if (multiple) {
                insts = manager.resolveImpls(client, samImplName, implName, depName, constraints);
                if (insts != null) {
                    for (ASMInst ins : insts) {
                        if (client.createWire(ins, depName)) {
                            allInst.add(ins);
                        }
                    }
                }
            } else {
                inst = manager.resolveImpl(client, samImplName, implName, depName, constraints);
                if ((inst != null) && (client.createWire(inst, depName)))
                    return inst;
            }
        }
        return null;
    }

    @Override
    public void addManager(Manager manager, int priority) {
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
        APAMImpl.managersPrio.put(manager, new Integer(priority));
    }

    @Override
    public List<Manager> getManagers() {
        return Collections.unmodifiableList(APAMImpl.managerList);
    }

    @Override
    public void removeManager(Manager manager) {
        APAMImpl.managersPrio.remove(manager);
        APAMImpl.managerList.remove(manager);
    }

    @Override
    public Application createAppliDeployImpl(String appliName, Set<ManagerModel> models, String implName, URL url,
            String type, String specName, Attributes properties) {
        if ((appliName == null) || (url == null) || (type == null)) {
            System.err.println("ERROR : missing parameters for create application");
            return null;
        }
        if (getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
        }

        if (APAMImpl.applications.get(appliName) != null)
            appliName = ((ApplicationImpl) APAMImpl.applications.get(appliName)).getNewName();

        Application appli = new ApplicationImpl(appliName, models, implName, url, type, specName, properties);
        if (appli != null)
            APAMImpl.applications.put(appliName, appli);
        return appli;
    }

    @Override
    public Application createAppli(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties) {
        if (appliName == null) {
            System.err.println("ERROR : appli Name is missing in create Appli");
            return null;
        }
        if (getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
            // return null ;
            return null;
        }

        Application appli = new ApplicationImpl(appliName, models, samImplName, implName, specName, properties);
        if (appli != null)
            APAMImpl.applications.put(appliName, appli);
        return appli;
    }

    /**
     * Creates an application from scratch, by deploying an implementation. First creates the root composites
     * (compositeName), associates its models (models). Then install an implementation (implName) from its URL,
     * considered as the application Main. All parameters are mandatory.
     * 
     * @param compositeName The name of the root composite.
     * @param models The manager models
     * @param specName optional : the logical name of the associated specification
     * @param specUrl Location of the code (interfaces) associated with the main specification.
     * @param specType Type of packaging for the code (interfaces) associated with the main specification.
     * @param properties The initial properties for the Implementation.
     * @return The new created application.
     */
    @Override
    public Application createAppliDeploySpec(String appliName, Set<ManagerModel> models, String specName, URL specUrl,
            String specType, String[] interfaces, Attributes properties) {
        if ((appliName == null) || (specName == null) || (specUrl == null) || (specType == null)
                || (interfaces == null)) {
            System.err.println("ERROR : missing parameters for create application");
            return null;
        }
        if (getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
        }

        if (APAMImpl.applications.get(appliName) != null)
            appliName = ((ApplicationImpl) APAMImpl.applications.get(appliName)).getNewName();

        Application appli = new ApplicationImpl(appliName, models, specName, specUrl, specType, interfaces, properties);
        if (appli != null)
            APAMImpl.applications.put(appliName, appli);
        return appli;
    }

    @Override
    public Application getApplication(String name) {
        for (Application appli : APAMImpl.applications.values()) {
            if (name.equals(appli.getName()))
                return appli;
        }
        return null;
    }

    @Override
    public Set<Application> getApplications() {
        return new HashSet<Application>(APAMImpl.applications.values());
    }

    @Override
    public int getPriority(Manager manager) {
        return APAMImpl.managersPrio.get(manager);
    }

    @Override
    public Composite createComposite(Composite source, String name, Set<ManagerModel> models) {
        if (source == null) {
            System.out.println("ERROR : Source composite missing");
            return null;
        }
        if (name == null) {
            System.out.println("ERROR : Composite name missing");
            return null;
        }
        if (source.getApplication().getComposite(name) != null) {
            System.out.println("ERROR : Composite " + name + " allready exists");
            return null;
        }
        Composite comp = new CompositeImpl(name, source.getApplication(), models);
        ((ApplicationImpl) source.getApplication()).addComposite(comp);
        source.addDepend(comp);
        return comp;
    }

    /**
     * called by an APAM client dependency handler when it initializes. Since the client is in the middle of its
     * creation, the Sam instance and the ASM inst are not created yet. We simply record in the instance event handler
     * that this instance will "appear"; at that time we will record the client address in a property of that instance
     * ASM.ApamDependencyHandlerAddress It is only in the ASMInst constructor that the ASM instance will be connected to
     * its handler.
     */
    @Override
    public void
            newClientCallBack(String samInstanceName, ApamDependencyHandler client, String implName, String specName) {
        if ((samInstanceName == null) || (client == null)) {
            System.err.println("ERROR : Missing parameter samInstanceName or client in newClientCallBack");
            return;
        }
        SamInstEventHandler.addNewApamInstance(samInstanceName, client, implName, specName);
    }

    @Override
    public void appearedImplExpected(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null)) {
            System.err.println("ERROR : Missing parameter impl or manager in appearedExpected");
            return;
        }
        SamInstEventHandler.addExpectedImpl(samImplName, manager);
    }

    @Override
    public void appearedInterfExpected(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter interf or manager in appearedExpected");
            return;
        }
        SamInstEventHandler.addExpectedInterf(interf, manager);
    }

    @Override
    public void listenLost(DynamicManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenLost");
            return;
        }
        SamInstEventHandler.addLost(manager);
    }

    @Override
    public void listenAttrChanged(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenAttrChanged");
            return;
        }
        AttributesImpl.addAttrChanged(manager);
    }

    @Override
    public Manager getManager(String managerName) {
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

    @Override
    public void appearedImplNotExpected(String samImplName, DynamicManager manager) {
        if ((samImplName == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter impl or manager in appearedNotExpected");
            return;
        }
        SamInstEventHandler.removeExpectedImpl(samImplName, manager);

    }

    @Override
    public void appearedInterfNotExpected(String interf, DynamicManager manager) {
        if ((interf == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter interf or manager in appearedNotExpected");
            return;
        }
        SamInstEventHandler.removeExpectedInterf(interf, manager);
    }

    @Override
    public void listenNotLost(DynamicManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenNotLost");
            return;
        }
        SamInstEventHandler.removeLost(manager);
    }

    @Override
    public void listenNotAttrChanged(AttributeManager manager) {
        if (manager == null) {
            System.out.println("ERROR : Missing parameter manager in listenNotAttrChanged");
            return;
        }
        AttributesImpl.removeAttrChanged(manager);
    }

    @Override
    public ASMSpecBroker getSpecBroker() {
        return CST.ASMSpecBroker;
    }

    @Override
    public ASMImplBroker getImplBroker() {
        return CST.ASMImplBroker;
    }

    @Override
    public ASMInstBroker getInstBroker() {
        return CST.ASMInstBroker;
    }
}
