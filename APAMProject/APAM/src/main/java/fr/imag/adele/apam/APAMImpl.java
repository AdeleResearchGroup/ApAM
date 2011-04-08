package fr.imag.adele.apam;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.samAPIImpl.SamInstEventHandler;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class APAMImpl implements Apam, ApamClient, ManagersMng {

    // The applications
    private static Map<String, Application> applications = new ConcurrentHashMap<String, Application>();

    // int is the priority
    private static Map<Manager, Integer>    managersPrio = new HashMap<Manager, Integer>();
    private static List<Manager>            managerList  = new ArrayList<Manager>();

    public APAMImpl() {
        new ASM(this);
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
        dumpApam();
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
        } else
            dumpApam();
        return allInst;
    }

    public ASMInst newWireSpec0(ASMInst client, String interfaceName, String specName, String depName,
            boolean multiple, Set<ASMInst> allInst) {
        Set<Filter> constraints = new HashSet<Filter>();
        Filter thatfilter = null;
        List<Manager> selectionPath = new ArrayList<Manager>();
        // Set<ASMInst> allInst = new HashSet<ASMInst>();

        // first step : compute selection path and constraints
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        // Call all managers in their priority order
        // Each manager can change the order of managers in the selectionPath, and even remove.
        // It can add itself or not. If no involved it should do nothing.
        for (int i = 0; i < APAMImpl.managerList.size(); i++) {
            selectionPath = APAMImpl.managerList.get(i).getSelectionPathSpec(client, interfaceName, specName, depName,
                    thatfilter, selectionPath);
            if (thatfilter != null) {
                constraints.add(thatfilter);
            }
        }

        // second step : look for a sharable instance that satisfies the constraints
        if (specName != null) {
            ASMSpec spec = null;
            spec = ASM.ASMSpecBroker.getSpec(specName);
            if (spec != null) {
                Set<ASMInst> sharable = ASM.ASMInstBroker.getShareds(spec, client.getComposite().getApplication(),
                        client.getComposite());
                for (ASMInst inst : sharable) {
                    boolean satisfies = true;
                    for (Filter filter : constraints) {
                        if (!filter.match((AttributesImpl) inst.getProperties())) {
                            satisfies = false;
                            break;
                        }
                    }
                    if (satisfies) { // accept only if a wire is possible
                        if (client.setWire(inst, depName, constraints)) {
                            allInst.add(inst);
                            if (!multiple)
                                return inst;
                        }
                    }
                }
                if (!allInst.isEmpty())
                    return null; // we found at least one

                // try to find a sharable implementation and instantiate.
                Set<ASMImpl> sharedImpl = ASM.ASMImplBroker.getShareds(spec, client.getComposite().getApplication(),
                        client.getComposite());
                for (ASMImpl impl : sharedImpl) {
                    boolean satisfies = true;
                    for (Filter filter : constraints) {
                        if (!filter.match((AttributesImpl) impl.getProperties())) {
                            satisfies = false;
                            break;
                        }
                    }
                    if (satisfies) { // This implem is sharable and satisfies the constraints. Instantiate.
                        ASMInst inst = impl.createInst(null);
                        // accept only if a wire is possible
                        if (client.setWire(inst, depName, constraints)) {
                            // At most one instantiation, even if multiple
                            allInst.add(inst);
                            if (!multiple)
                                return inst;
                        }
                    }
                }
                if (!allInst.isEmpty())
                    return null; // we found at least one
            }
        }

        // third step : ask each manager in the order
        Set<ASMInst> insts = null;
        ASMInst inst = null;
        for (int i = 0; i < APAMImpl.managerList.size(); i++) {
            if (!multiple) {
                inst = APAMImpl.managerList.get(i).resolveSpec(client, interfaceName, specName, depName, constraints);
                if ((inst != null) && (client.setWire(inst, depName, constraints)))
                    return inst;
            } else {
                insts = APAMImpl.managerList.get(i).resolveSpecs(client, interfaceName, specName, depName, constraints);
                if (insts != null) {
                    for (ASMInst ins : insts) {
                        if (client.setWire(ins, depName, constraints)) {
                            allInst.add(ins);
                        }
                    }
                }
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
        Filter thatfilter = null;
        List<Manager> selectionPath = new ArrayList<Manager>();

        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        for (int i = 0; i < APAMImpl.managerList.size(); i++) {
            selectionPath = APAMImpl.managerList.get(i).getSelectionPathImpl(client, samImplName, implName, depName,
                    thatfilter, selectionPath);
            if (thatfilter != null) {
                constraints.add(thatfilter);
            }
        }

        // second pass : look for a sharable instance that satisfies the constraints
        if (implName != null) {
            ASMImpl impl = null;
            impl = ASM.ASMImplBroker.getImpl(implName);
            if (impl != null) {
                Set<ASMInst> sharable = ASM.ASMInstBroker.getShareds(impl, client.getComposite().getApplication(),
                        client.getComposite());
                for (ASMInst inst : sharable) {
                    boolean satisfies = true;
                    for (Filter filter : constraints) {
                        if (!filter.match((AttributesImpl) inst.getProperties())) {
                            satisfies = false;
                            break;
                        }
                    }
                    if (satisfies) { // accept only if a wire is possible
                        if (client.setWire(inst, depName, constraints)) {
                            if (multiple)
                                allInst.add(inst);
                            else
                                return inst;
                        }
                    }

                }
                // The impl does not have sharable instance. try to instanciate.
                boolean satisfies = true;
                for (Filter filter : constraints) {
                    if (!filter.match((AttributesImpl) impl.getProperties())) {
                        satisfies = false;
                        break;
                    }
                }

                if (satisfies) { // This implem is sharable and satisfies the constraints. Instantiate.
                    ASMInst inst = impl.createInst(null);
                    // accept only if a wire is possible
                    if (client.setWire(inst, depName, constraints))
                        if (multiple) // At most one instantiation, even if multiple
                            allInst.add(inst);
                    return inst; // If not we have created an instance unused ! delete it ?
                }
            }
        }

        // third step : ask each manager in the order
        ASMInst resolved;
        for (int i = 0; i < APAMImpl.managerList.size(); i++) {
            if (multiple) {
                allInst = APAMImpl.managerList.get(i).resolveImpls(client, samImplName, implName, depName, constraints);
                if (!allInst.isEmpty()) {
                    for (ASMInst ins : allInst) {
                        if (!client.setWire(ins, depName, constraints)) {
                            allInst.remove(ins);
                        }
                    }
                    return null;
                }
            } else {
                resolved = APAMImpl.managerList.get(i).resolveImpl(client, samImplName, implName, depName, constraints);
                if (resolved != null) {
                    // accept only if a wire is possible
                    if (client.setWire(resolved, depName, constraints))
                        return resolved;
                }
            }
        }
        return null;
    }

    @Override
    public void addManager(Manager manager, int priority) {
        if (APAMImpl.managerList.size() == 0) {
            APAMImpl.managerList.add(manager);
        } else {
            for (int i = 0; i < APAMImpl.managerList.size(); i++) {
                if (priority >= APAMImpl.managerList.get(i).getPriority()) {
                    APAMImpl.managerList.add(i, manager);
                }
            }
        }
        APAMImpl.managersPrio.put(manager, new Integer(priority));
    }

    @Override
    public List<Manager> getManagers() {
        return new ArrayList<Manager>(APAMImpl.managerList);
    }

    @Override
    public void removeManager(Manager manager) {
        APAMImpl.managersPrio.remove(manager);
        APAMImpl.managerList.remove(manager);
    }

    @Override
    public Application createAppli(String appliName, Set<ManagerModel> models, String implName, URL url, String type,
            String specName, Attributes properties) {
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
     * (compositeName), associates its models (modles). Then install an implementation (implName) from its URL,
     * considered as the application Main.
     * 
     * @param compositeName The name of the root composite.
     * @param models The manager models
     * @param implName The logical name for the Application Main implementation
     * @param implUrl Location of the Main executable.
     * @param implType Type of packaging for main executable.
     * @param specName optional : the logical name of the associated specification
     * @param specUrl Location of the code (interfaces) associated with the main specification.
     * @param specType Type of packaging for the code (interfaces) associated with the main specification.
     * @param properties The initial properties for the Implementation.
     * @return The new created application.
     */
    @Override
    public Application createAppli(String appliName, Set<ManagerModel> models, String implName, URL implUrl,
            String implType, String specName, URL specUrl, String specType, String[] interfaces, Attributes properties) {
        if ((appliName == null) || (implUrl == null) || (implType == null) || (specName == null)
                || (interfaces == null)) {
            System.err.println("ERROR : missing parameters for create application");
            return null;
        }
        if (getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
        }

        if (APAMImpl.applications.get(appliName) != null)
            appliName = ((ApplicationImpl) APAMImpl.applications.get(appliName)).getNewName();

        Application appli = new ApplicationImpl(appliName, models, implName, implUrl, implType, specName, specUrl,
                specType, interfaces, properties);
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
        SamInstEventHandler.theInstHandler.addNewApamInstance(samInstanceName, client, implName, specName);
    }

    @Override
    public void appearedExpected(ASMImpl impl, DynamicManager manager) {
        if ((impl == null) || (manager == null)) {
            System.err.println("ERROR : Missing parameter impl or manager in appearedExpected");
            return;
        }
        SamInstEventHandler.addExpectedImpl(impl, manager);
    }

    @Override
    public void appearedExpected(String interf, DynamicManager manager) {
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
            System.out.println("ERROR : Missing parameter manager in getManager");
            return null;
        }
        return APAMImpl.managerList.get(APAMImpl.managerList.lastIndexOf(managerName));
    }

    @Override
    public void appearedNotExpected(ASMImpl impl, DynamicManager manager) {
        if ((impl == null) || (manager == null)) {
            System.out.println("ERROR : Missing parameter impl or manager in appearedNotExpected");
            return;
        }
        SamInstEventHandler.removeExpectedImpl(impl, manager);

    }

    @Override
    public void appearedNotExpected(String interf, DynamicManager manager) {
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
    public void dumpAppli(String name) {
        for (Application appli : getApplications()) {
            if (appli.getName().equals(name)) {
                System.out.println("Application : " + appli.getName() + "  Main : " + appli.getMainImpl());
                dumpComposite(appli.getMainComposite(), "  ");
                System.out.println("\nState: ");
                dumpState(appli.getMainImpl().getInst(), "  ", "");
                break;
            }
        }
    }

    @Override
    public void dumpApam() {
        for (Application appli : getApplications()) {
            System.out.println("Application : " + appli.getName() + "  Main : " + appli.getMainImpl());
            dumpComposite(appli.getMainComposite(), "  ");
            System.out.println("\nState: ");
            dumpState(appli.getMainImpl().getInst(), "  ", "");
        }
    }

    @Override
    public void dumpState(ASMInst inst, String indent, String dep) {
        if (inst == null)
            return;
        System.out.println(indent + dep + ": " + inst + " " + inst.getImpl() + " " + inst.getSpec());
        indent = indent + "  ";
        for (ASMInst to : inst.getWires()) {
            dumpState(to, indent, inst.getWire(to).depName);
        }
    }

    public void dumpComposite(Composite compo, String indent) {
        if (compo == null)
            return;
        System.out.println(indent + compo.getName());
        indent = indent + "  ";
        for (Composite comp : compo.getDepend()) {
            dumpComposite(comp, indent);
        }
    }
}
