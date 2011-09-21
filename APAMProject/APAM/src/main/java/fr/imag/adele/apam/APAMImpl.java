package fr.imag.adele.apam;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.ASMImpl.SamInstEventHandler;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.CompositeImpl;
import fr.imag.adele.sam.Implementation;

public class APAMImpl implements Apam, ApamClient, ManagersMng {

    // The applications
    private static Manager               apamMan;

    // int is the priority
    private static Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer>();
    private static List<Manager>         managerList  = new ArrayList<Manager>();

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
     * If the client (the instance that needs the resolution) is
     * not null, a wire is created between the client and the resolved instance(s).
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param depName. Optional. Name ot hte dependency between client and the instance to resolve.
     * @param constraints. Optional. To select the right instance.
     * @param preferences. Optional. To select the right instance.
     * @return
     */
    @Override
    public ASMInst newWireSpec(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {

        if ((client == null) || ((interfaceName == null) && (specName == null))) {
            System.err.println("missing client, name or interface");
            // may succed if a sam instance starts and not yet registered
            return null;
        }

        CompositeType compoType = client.getComposite().getCompType();
        Composite compo = client.getComposite();

        ASMImpl impl = null;
        if (specName != null)
            impl = resolveSpecByName(compoType, specName, constraints, preferences);
        else
            impl = resolveSpecByInterface(compoType, interfaceName, null, constraints, preferences);
        if (impl == null) {
            System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            return null;
        }

        ASMInst inst = resolveImpl(compo, impl, constraints, preferences);
        if (inst != null)
            client.createWire(inst, depName);
        return inst;
    }

    @Override
    public Set<ASMInst> newWireSpecs(ASMInst client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {

        if ((client == null) || ((interfaceName == null) && (specName == null))) {
            new Exception("missing client, name or interface").printStackTrace();
        }

        CompositeType compoType = client.getComposite().getCompType();
        Composite compo = client.getComposite();

        ASMImpl impl = null;
        if (specName != null)
            impl = resolveSpecByName(compoType, specName, constraints, preferences);
        else
            impl = resolveSpecByInterface(compoType, interfaceName, null, constraints, preferences);
        if (impl == null) {
            System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            return Collections.emptySet();
        }

        Set<ASMInst> insts = resolveImpls(compo, impl, constraints, preferences);
        if ((insts != null) && !insts.isEmpty()) {
            for (ASMInst inst : insts) {
                client.createWire(inst, depName);
            }
        }
        return insts;
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
    public ASMInst newWireImpl(ASMInst client, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {
        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        CompositeType compType = client.getComposite().getCompType();
        Composite compo = client.getComposite();

        ASMImpl impl = findImplByName(compType, implName);
        if (impl == null) {
            System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            return null;
        }

        ASMInst inst = resolveImpl(compo, impl, constraints, preferences);
        if (inst != null)
            client.createWire(inst, depName);
        return inst;
    }

    @Override
    public Set<ASMInst> newWireImpls(ASMInst client, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {

        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        CompositeType compType = client.getComposite().getCompType();
        Composite compo = client.getComposite();

        ASMImpl impl = findImplByName(compType, implName);
        if (impl == null) {
            System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            return null;
        }

//        selectionPath.clear();
//        selectionPath = computeSelectionPathImpl(client, implName, constraints, preferences, selectionPath);      
        Set<ASMInst> insts = resolveImpls(compo, impl, constraints, preferences);
        if ((insts != null) && !insts.isEmpty()) {
            for (ASMInst inst : insts) {
                client.createWire(inst, depName);
            }
        }
        return insts;
    }

    // public Set<ASMInst> newWireSpecsxx(ASMInst client, String interfaceName, String specName, String depName,
    // Set<Filter> constraints, List<Filter> preferences) {
    // Set<ASMInst> allInst = new HashSet<ASMInst>();
    // newWireSpec0(client, null, interfaceName, specName, depName, constraints, preferences, true, allInst);
    //
    // if (allInst.isEmpty()) {
    // if (specName != null)
    // System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
    // if (interfaceName != null)
    // System.out.println("Failed to resolve " + interfaceName + " from " + client + "(" + depName + ")");
    // }
    // return allInst;
    // }
    //
    // public ASMInst newWireSpec0(ASMInst client, CompositeType compType, String interfaceName,
    // String specName, String depName, Set<Filter> constraints, List<Filter> preferences, boolean multiple,
    // Set<ASMInst> allInst) {
    //
    // Composite compInst = null; // may be null if first instance.
    // if ((client != null) && (client.getComposite() != null)) {
    // compType = client.getComposite().getCompType();
    // compInst = client.getComposite();
    // }
    // if ((compType == null)) {
    // new Exception("missing composite or client in newWire :" + client).printStackTrace();
    // return null;
    // // System.out.println("WARNING : missing composite in newWireSpec0");
    // }
    //
    // // first step : compute selection path and constraints
    // if (APAMImpl.managerList.size() == 0) {
    // System.out.println("No manager available. Cannot resolve ");
    // return null;
    // }
    // // Call all managers in their priority order
    // // Each manager can change the order of managers in the selectionPath, and even remove.
    // // It can add itself or not. If no involved it should do nothing.
    // List<Manager> selectionPath = new ArrayList<Manager>();
    // List<Manager> previousSelectionPath = new ArrayList<Manager>();
    // for (int i = 1; i < APAMImpl.managerList.size(); i++) { // Start from one to skip ApamMan. Added after the loop.
    // previousSelectionPath = selectionPath;
    // selectionPath = APAMImpl.managerList.get(i).getSelectionPathSpec(client, interfaceName,
    // specName, constraints, preferences, selectionPath);
    // if (selectionPath == null)
    // selectionPath = previousSelectionPath;
    // }
    // // To select first the APAM manager
    // selectionPath.add(0, APAMImpl.apamMan);
    //
    // // System.out.println("selection path : ");
    // // for (Manager man : selectionPath) {
    // // System.out.print(" " + man.getName());
    // // }
    // // third step : ask each manager in the order
    // Set<ASMInst> insts = null;
    // ASMInst inst = null;
    // System.out.println("Resolving spec interface " + interfaceName + ", specName: " + specName + ": ");
    // boolean deployed = false;
    // for (Manager manager : selectionPath) {
    // if (!manager.getName().equals(CST.APAMMAN))
    // deployed = true;
    // System.out.println(" " + manager.getName());
    // if (multiple) {
    // insts = manager.resolveSpecs(compInst, interfaceName, specName,
    // constraints, preferences);
    // if (insts != null) {
    // System.out.print("   Got : ");
    // for (ASMInst ins : insts) {
    // if (client.createWire(ins, depName, deployed)) {
    // allInst.add(ins);
    // System.out.print(ins + " ");
    // }
    // }
    // System.out.println("");
    // }
    // if (!allInst.isEmpty())
    // return null;
    // } else {
    // inst = manager.resolveSpec(compInst, interfaceName, specName, constraints,
    // preferences);
    // if ((inst != null) && (client.createWire(inst, depName, deployed))) {
    // System.out.println("   Got : " + inst);
    // return inst;
    // }
    // }
    // }
    // return null;
    // }

    private List<Manager> computeSelectionPathSpec(CompositeType compTypeFrom, String interfaceName,
            String[] interfaces,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        List<Manager> selectionPath = new ArrayList<Manager>();
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            APAMImpl.managerList.get(i).getSelectionPathSpec(compTypeFrom, interfaceName,
                    interfaces, specName, constraints, preferences, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);
        return selectionPath;
    }

    private List<Manager> computeSelectionPathImpl(CompositeType compTypeFrom, String implName) {
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        List<Manager> selectionPath = new ArrayList<Manager>();
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            APAMImpl.managerList.get(i).getSelectionPathImpl(compTypeFrom, implName, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);
        return selectionPath;
    }

    private List<Manager> computeSelectionPathInst(Composite compoFrom, ASMImpl impl,
            Set<Filter> constraints, List<Filter> preferences) {
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        List<Manager> selectionPath = new ArrayList<Manager>();
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            APAMImpl.managerList.get(i).getSelectionPathInst(compoFrom, impl, constraints,
                    preferences, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);
        return selectionPath;
    }

    /**
     * Resolve.
     * 
     * @param client. The instance that needs the resolution. Can be null.
     *            If not null, a wire is created between the client and the resolved instance(s).
     * @param implComposite. Optional. The composite type that will use (or contain) the resolved implementation.
     * @param instComposite. Optional. The composite containing the client.
     * @param implName. Name of the implem to resolve.
     * @param depName. Optional. Name ot hte dependency between client and the instance to resolve.
     * @param constraints. Optional. To select the right instance.
     * @param preferences. Optional. To select the right instance.
     * @param multiple. Select all the existing instances (which satisfy the constriants)
     * @param deployed. The implementation has been deployed.
     * @param allInst. If multiple = true, contains the resolved instances.
     * @return
     */

    //
    // public ASMInst newWireImpl1(ASMInst client, CompositeType implComposite, Composite instComposite,
    // String implName, String depName, Set<Filter> constraints, List<Filter> preferences,
    // boolean multiple, Set<ASMInst> allInst) {
    //
    // if (constraints == null)
    // constraints = new HashSet<Filter>();
    // if (preferences == null)
    // preferences = new ArrayList<Filter>();
    //
    // if ((client != null) && (client.getComposite() != null)) {
    // instComposite = client.getComposite();
    // implComposite = instComposite.getCompType();
    // }
    //
    // // first step : compute selection path and constraints
    // List<Manager> selectionPath = new ArrayList<Manager>();
    //
    // if (APAMImpl.managerList.size() == 0) {
    // System.out.println("No manager available. Cannot resolve ");
    // return null;
    // }
    // for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
    // selectionPath = APAMImpl.managerList.get(i).getSelectionPathImpl(client, implComposite,
    // implName, constraints, preferences, selectionPath);
    // }
    // // To select first in Apam
    // selectionPath.add(0, APAMImpl.apamMan);
    //
    // // System.out.print("selection path : ");
    // // for (Manager man : selectionPath) {
    // // System.out.print(" " + man.getName());
    // // }
    //
    // // third step : ask each manager in the order
    //
    // Set<ASMInst> insts = null;
    // ASMInst inst = null;
    // System.out.println("Resolving implementation: " + implName + ": ");
    // boolean deployed = false;
    // ASMImpl impl = null;
    // for (Manager manager : selectionPath) {
    // if (!manager.getName().equals(CST.APAMMAN))
    // deployed = true;
    // System.out.println("  " + manager.getName());
    // impl = findImplByName(implComposite, implName);
    // if (impl != null) {
    // if (deployed)
    // deployedImpl(implComposite, impl);
    // break;
    // }
    // }
    // if (impl == null)
    // return null;
    // for (Manager manager : selectionPath) {
    // if (multiple) {
    // insts = manager.resolveImpls(instComposite, impl, constraints, preferences);
    // if (insts != null) {
    // System.out.print("   Got : ");
    // for (ASMInst ins : insts) {
    // if (client.createWire(ins, depName, deployed)) {
    // allInst.add(ins);
    // System.out.print(ins + " ");
    // }
    // }
    // System.out.println("");
    // if (!allInst.isEmpty())
    // return null;
    // }
    // } else {
    // inst = manager.resolveImpl(instComposite, impl, constraints, preferences);
    // if (inst != null) { //found a solution. Is it Ok ?
    // System.out.println("   Got : " + inst.getName());
    // if (client == null) { //only for resolving by name
    // return inst;
    // } else {
    // if (client.createWire(inst, depName, deployed)) {
    // return inst;
    // }
    // }
    // }
    // }
    // }
    // return null;
    // }

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

    @Override
    public CompositeType createCompositeType(Implementation samImpl) {
        return CompositeTypeImpl.createCompositeType(null, samImpl);
    }

    @Override
    public CompositeType createCompositeType(String name, String mainImplName,
            Set<ManagerModel> models, Attributes attributes) {
        return CompositeTypeImpl.createCompositeType(null, name, mainImplName, null,
                models, attributes);
    }

    @Override
    public CompositeType createCompositeType(String name, String mainImplName, Set<ManagerModel> models,
            URL mainBundle, String specName, Attributes attributes) {
        return CompositeTypeImpl.createCompositeType(null, name, models, mainImplName, mainBundle, specName,
                attributes);
    }

    @Override
    public Composite startAppli(String compositeName) {
        ASMImpl compoType = findImplByName(null, compositeName);
        if (compoType == null)
            return null;
        if (compoType instanceof CompositeType)
            return startAppli((CompositeType) compoType);
        System.err.println("ERROR : " + compoType.getName() + " is not a composite.");
        return null;
    }

    @Override
    public Composite startAppli(URL compoURL, String compositeName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Composite startAppli(CompositeType composite) {
        return (Composite) composite.createInst(null, null);
    }

    /**
     * Impl has been deployed, it becomes embedded in compoType
     * 
     * @param compoType
     * @param impl
     */
    private void deployedImpl(CompositeType compoType, ASMImpl impl) {
        // if (compoType != null) {
        // impl is inside compotype
        compoType.addImpl(impl);
        // if impl is a composite type, it is embedded inside compoFrom
        if (impl instanceof CompositeType) { // || (returnedInst.getComposite().getMainInst() == returnedInst)) {
                                             // //it is a composite
            ((CompositeTypeImpl) compoType).addEmbedded((CompositeType) impl);
        }
        // }
    }

    @Override
    public ASMImpl findImplByName(CompositeType compoType, String implName) {
        List<Manager> selectionPath = computeSelectionPathImpl(compoType, implName);
        return findImplByName(compoType, implName, selectionPath);
    }

    @Override
    public ASMImpl findImplByName(CompositeType compoType, String implName, List<Manager> selectionPath) {

        if ((selectionPath == null) || (selectionPath.size() == 0)) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        if (compoType == null)
            compoType = CompositeTypeImpl.getRootCompositeType();
        ASMImpl impl = null;
        System.out.println("Looking for implementation: " + implName + ": ");
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.println("  " + manager.getName());
            impl = manager.findImplByName(compoType, implName);
            if (impl != null) {
                if (deployed)
                    deployedImpl(compoType, impl);
                return impl;
            }
        }
        return null;
    }

    @Override
    public ASMImpl resolveSpecByName(CompositeType compoTypeFrom, String specName, Set<Filter> constraints,
            List<Filter> preferences) {
        List<Manager> selectionPath = computeSelectionPathSpec(compoTypeFrom, null, null, specName, constraints,
                preferences);
        return resolveSpecByName(compoTypeFrom, specName, constraints, preferences, selectionPath);
    }

    @Override
    public ASMImpl resolveSpecByName(CompositeType compoType, String specName, Set<Filter> constraints,
            List<Filter> preferences, List<Manager> selectionPath) {
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        ASMImpl impl = null;
        System.out.println("Looking for an implem implementing " + specName + ": ");
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.println("  " + manager.getName());
            impl = manager.resolveSpecByName(compoType, specName, constraints, preferences);
            if (impl != null) {
                if (deployed)
                    deployedImpl(compoType, impl);
                return impl;
            }
        }
        return null;
    }

    @Override
    public ASMImpl resolveSpecByInterface(CompositeType compoTypeFrom, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences) {
        List<Manager> selectionPath = computeSelectionPathSpec(compoTypeFrom, interfaceName, interfaces, null,
                constraints, preferences);
        return resolveSpecByInterface(compoTypeFrom, interfaceName, interfaces, constraints, preferences, selectionPath);
    }

    @Override
    public ASMImpl resolveSpecByInterface(CompositeType compoType, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selectionPath) {
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        ASMImpl impl = null;
        if (interfaceName != null)
            System.out.println("Looking for an implem with interface " + interfaceName);
        else
            System.out.println("Looking for an implem with interfaces " + interfaces);
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.println("  " + manager.getName());
            impl = manager.resolveSpecByInterface(compoType, interfaceName, interfaces, constraints, preferences);
            if (impl != null) {
                if (deployed)
                    deployedImpl(compoType, impl);
                return impl;
            }
        }
        return null;
    }

    @Override
    public ASMInst resolveImpl(Composite compo, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences) {
        List<Manager> selectionPath = computeSelectionPathInst(compo, impl, constraints, preferences);
        return resolveImpl(compo, impl, constraints, preferences, selectionPath);
    }

    @Override
    public ASMInst resolveImpl(Composite compo, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences,
            List<Manager> selectionPath) {
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        ASMInst inst = null;
        System.out.println("Looking for an instance of: " + impl + ": ");
        for (Manager manager : selectionPath) {
            System.out.println("  " + manager.getName());
            inst = manager.resolveImpl(compo, impl, constraints, preferences);
            if (inst != null) {
                System.out.println("selected : " + inst);
                return inst;
            }
        }
        if (inst == null) {
            inst = impl.createInst(compo, null);
            System.out.println("instantiated : " + inst);
            return inst;
        }
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite compo, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences) {
        List<Manager> selectionPath = computeSelectionPathInst(compo, impl, constraints, preferences);
        return resolveImpls(compo, impl, constraints, preferences, selectionPath);
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite compo, ASMImpl impl, Set<Filter> constraints, List<Filter> preferences,
            List<Manager> selectionPath) {
        if (impl == null) {
            System.err.println("impl is null in resolveImpls");
            return null;
        }
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();

        Set<ASMInst> insts = null;
        System.out.println("Looking for instances of: " + impl + ": ");
        for (Manager manager : selectionPath) {
            System.out.println("  " + manager.getName());
            insts = manager.resolveImpls(compo, impl, constraints);
            if ((insts != null) && !insts.isEmpty()) {
                System.out.println("selected " + insts);
                return insts;
            }
        }
        if (insts == null)
            insts = new HashSet<ASMInst>();
        if (insts.isEmpty()) {
            insts.add(impl.createInst(compo, null));
        }
        System.out.println("instantiated " + (insts.toArray()[0]));
        return insts;
    }

    @Override
    public CompositeType getCompositeType(String name) {
        return CompositeTypeImpl.getCompositeType(name);
    }

    @Override
    public Collection<CompositeType> getCompositeTypes() {
        return CompositeTypeImpl.getCompositeTypes();
    }

    @Override
    public Collection<CompositeType> getRootCompositeTypes() {
        return CompositeTypeImpl.getRootCompositeTypes();
    }

    @Override
    public Composite getComposite(String name) {
        return CompositeImpl.getComposite(name);
    }

    @Override
    public Collection<Composite> getComposites() {
        return CompositeImpl.getComposites();
    }

    @Override
    public Collection<Composite> getRootComposites() {
        return CompositeImpl.getRootComposites();
    }

}
