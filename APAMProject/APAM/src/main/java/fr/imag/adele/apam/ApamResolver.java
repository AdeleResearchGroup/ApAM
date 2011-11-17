package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamImpl.APAMImpl;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.apform.Apform;

public class ApamResolver {

    /**
     * In the case a client realizes that a dependency disappeared, it has to call this method. APAM will try to resolve
     * the problem (DYNAMAM in practice), and return a new instance.
     * 
     * @param client the instance that looses it dependency
     * @param lostInstance the instance that disappeared.
     * @return
     */
    public static Instance faultWire(Instance client, Instance lostInstance, String depName) {
        // TODO Auto-generated method stub
        return null;
    }

    // if the instance is unused, it will become the main instance of a new composite.
    private static Composite getClientComposite(Instance mainInst) {
        if (mainInst.isUsed())
            return mainInst.getComposite();

        Implementation mainImplem = mainInst.getImpl();
        String newName = mainImplem.getName() + "_Appli";

        CompositeType newCompoT = CompositeTypeImpl.createCompositeType(null, newName, mainImplem.getName(), null,
                null, null);
        return new CompositeImpl(newCompoT, null, mainInst, null);
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
    public static Instance newWireSpec(Instance client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {

        if ((client == null) || ((interfaceName == null) && (specName == null))) {
            System.err.println("missing client, name or interface");
            // may succed if an instance starts and not yet registered
            return null;
        }

        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compoType = compo.getCompType();

        Implementation impl = null;
        if (specName != null)
            impl = ApamResolver.resolveSpecByName(compoType, specName, constraints, preferences);
        else {
            impl = ApamResolver.resolveSpecByInterface(compoType, interfaceName, null, constraints, preferences);
            specName = interfaceName;
        }
        if (impl == null) {
            System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            ApamResolver.notifySelection(client, specName, depName, null, null, null);
            return null;
        }

        Instance inst = ApamResolver.resolveImpl(compo, impl, constraints, preferences);
        if (inst != null)
            client.createWire(inst, depName);
        ApamResolver.notifySelection(client, specName, depName, impl, inst, null);
        return inst;
    }

    /**
     * An APAM client instance requires to be wired with all the instance implementing the specification and satisfying
     * the constraints.
     * WARNING : if no specification name is provided, since more than one specification can implement the same
     * interface,
     * any specification implementing at least the provided interface (technical name of the interface) will be
     * considered satisfactory.
     * If found, the instance is returned.
     * 
     * @param client the instance that requires the specification
     * @param interfaceName the name of one of the interfaces of the specification to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param depName the name of the dependency; different from SAM. May be null.
     * @param constraints The constraints for this resolution.
     * @param preferences The preferences for this resolution.
     * @return
     */
    public static Set<Instance> newWireSpecs(Instance client, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {

        if ((client == null) || ((interfaceName == null) && (specName == null))) {
            new Exception("missing client, name or interface").printStackTrace();
        }

        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compoType = compo.getCompType();

        Implementation impl = null;
        if (specName != null)
            impl = ApamResolver.resolveSpecByName(compoType, specName, constraints, preferences);
        else {
            impl = ApamResolver.resolveSpecByInterface(compoType, interfaceName, null, constraints, preferences);
            specName = interfaceName;
        }
        if (impl == null) {
            System.out.println("Failed to resolve " + specName + " from " + client + "(" + depName + ")");
            ApamResolver.notifySelection(client, specName, depName, null, null, null);
            return Collections.emptySet();
        }

        Set<Instance> insts = ApamResolver.resolveImpls(compo, impl, constraints);
        if ((insts != null) && !insts.isEmpty()) {
            for (Instance inst : insts) {
                client.createWire(inst, depName);
            }
        }
        ApamResolver.notifySelection(client, specName, depName, impl, null, insts);
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
    public static Instance newWireImpl(Instance client, String implName, String depName,
            Set<Filter> constraints, List<Filter> preferences) {
        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compType = client.getComposite().getCompType();

        Implementation impl = ApamResolver.findImplByName(compType, implName);
        if (impl == null) {
            System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            ApamResolver.notifySelection(client, implName, depName, null, null, null);
            return null;
        }

        Instance inst = ApamResolver.resolveImpl(compo, impl, constraints, preferences);
        if (inst != null)
            client.createWire(inst, depName);
        ApamResolver.notifySelection(client, implName, depName, impl, inst, null);
        return inst;
    }

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name
     * @param constraints The constraints for this resolution.
     * @return
     */
    public static Set<Instance> newWireImpls(Instance client, String implName, String depName,
            Set<Filter> constraints) {

        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compType = compo.getCompType();

        Implementation impl = ApamResolver.findImplByName(compType, implName);
        if (impl == null) {
            System.out.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            ApamResolver.notifySelection(client, implName, depName, null, null, null);
            return null;
        }

        Set<Instance> insts = ApamResolver.resolveImpls(compo, impl, constraints);
        if ((insts != null) && !insts.isEmpty()) {
            for (Instance inst : insts) {
                client.createWire(inst, depName);
            }
        }
        ApamResolver.notifySelection(client, implName, depName, impl, null, insts);
        return insts;
    }

    /**
     * Before to resolve a specification (i.e. to select one of its implementations)
     * defined by one interface, all its interfaces, or its name, this method is called to
     * know which managers are involved, and what are the constraints and preferences set by the managers to this
     * resolution.
     * 
     * @param compTypeFrom : the origin of this resolution.
     * @param interfaceName : the full name of one of the interfaces of the specification.
     * @param interfaces : the full list of interfaces of the specificaiton.
     * @param specName : the name of the specification.
     * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
     * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
     * @return : the managers that will be called for that resolution.
     */
    public static List<Manager> computeSelectionPathSpec(CompositeType compTypeFrom, String interfaceName,
            String[] interfaces, String specName, Set<Filter> constraints, List<Filter> preferences) {
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        List<Manager> selectionPath = new ArrayList<Manager>();
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            APAMImpl.managerList.get(i).getSelectionPathSpec(compTypeFrom, interfaceName,
                    interfaces, specName, constraints, preferences, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);
        return selectionPath;
    }

    public static List<Manager> computeSelectionPathImpl(CompositeType compTypeFrom, String implName) {
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

    /**
     * Before to resolve an implementation (i.e. to select one of its instance), this method is called to
     * know which managers are involved, and what are the constraints and preferences set by the managers to this
     * resolution.
     * 
     * @param compTypeFrom : the origin of this resolution.
     * @param impl : the implementation to resolve.
     * @param constraints : the constraints added by the managers. A (empty) set must be provided as parameter.
     * @param preferences : the preferences added by the managers. A (empty) list must be provided as parameter.
     * @return : the managers that will be called for that resolution.
     */
    public static List<Manager> computeSelectionPathInst(Composite compoFrom, Implementation impl,
            Set<Filter> constraints, List<Filter> preferences) {
        if (APAMImpl.managerList.size() == 0) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

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
     * Impl has been deployed, it becomes embedded in compoType.
     * If physically deployed, it is in the Unused list. remove.
     * 
     * @param compoType
     * @param impl
     */
    public static void deployedImpl(CompositeType compoType, Implementation impl, boolean deployed) {
        // it was not deployed
        if (!deployed && impl.isUsed()) {
            System.out.println(" : selected " + impl);
            return;
        }

        if (!impl.isUsed()) {
            System.out.println(" : deployed " + impl);
        } else {
            System.out.println("Logicaly deployed " + impl);
        }

        // it was unused so far. Remove it from unused
        if (!impl.isUsed()) {
            Apform.setUsedImpl(impl);
        }
        // impl is inside compotype
        compoType.addImpl(impl);
        // if impl is a composite type, it is embedded inside compoFrom
        if (impl instanceof CompositeType) { // it is a composite
            ((CompositeTypeImpl) compoType).addEmbedded((CompositeType) impl);
        }
        // }
    }

    /**
     * Look for an implementation with a given name "implName", visible from composite Type compoType.
     * 
     * @param compoType
     * @param implName
     * @return
     */
    public static Implementation findImplByName(CompositeType compoTypeFrom, String implName) {

        List<Manager> selectionPath = ApamResolver.computeSelectionPathImpl(compoTypeFrom, implName);
        if (selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (compoTypeFrom == null)
            compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
        Implementation impl = null;
        System.out.println("Looking for implementation " + implName + ": ");
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.print(manager.getName() + "  ");
            impl = manager.findImplByName(compoTypeFrom, implName);

            if (impl != null) {
                ApamResolver.deployedImpl(compoTypeFrom, impl, deployed);
                return impl;
            }
        }
        return null;
    }

    /**
     * First looks for the specification defined by its name, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param specName
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     *            number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public static Implementation resolveSpecByName(CompositeType compoTypeFrom, String specName,
            Set<Filter> constraints,
            List<Filter> preferences) {
        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        List<Manager> selectionPath = ApamResolver.computeSelectionPathSpec(compoTypeFrom, null, null, specName,
                constraints,
                preferences);
        if (selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (compoTypeFrom == null)
            compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
        Implementation impl = null;
        System.out.println("Looking for an implem implementing " + specName + ": ");
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.println(manager.getName() + "  ");
            impl = manager.resolveSpecByName(compoTypeFrom, specName, constraints, preferences);

            if (impl != null) {
                ApamResolver.deployedImpl(compoTypeFrom, impl, deployed);
                return impl;
            }
        }
        return null;
    }

    /**
     * First looks for the specification defined by its interface, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param interfaceName. The full name of one of the interfaces of the specification.
     *            WARNING : different specifications may share the same interface.
     * @param interfaces. The complete list of interface of the specification. At most one specification can be
     *            selected.
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     *            number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public static Implementation resolveSpecByInterface(CompositeType compoTypeFrom, String interfaceName,
            String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences) {
        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        List<Manager> selectionPath = ApamResolver.computeSelectionPathSpec(compoTypeFrom, interfaceName, interfaces,
                null,
                constraints, preferences);
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (compoTypeFrom == null)
            compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
        Implementation impl = null;
        if (interfaceName != null)
            System.out.println("Looking for an implem with interface " + interfaceName);
        else
            System.out.println("Looking for an implem with interfaces " + interfaces);
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.print(manager.getName() + "  ");
            impl = manager.resolveSpecByInterface(compoTypeFrom, interfaceName, interfaces, constraints, preferences);
            if (impl != null) {
                ApamResolver.deployedImpl(compoTypeFrom, impl, deployed);
                return impl;
            }
        }
        return null;
    }

    /**
     * Look for an instance of "impl" that satisfies the constraints. That instance must be either shared and visible
     * from "compo",
     * or instantiated if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     * @return
     */
    public static Instance resolveImpl(Composite compo, Implementation impl, Set<Filter> constraints,
            List<Filter> preferences) {
        if (constraints == null)
            constraints = new HashSet<Filter>();
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        List<Manager> selectionPath = ApamResolver.computeSelectionPathInst(compo, impl, constraints, preferences);
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }

        if (compo == null)
            compo = CompositeImpl.getRootAllComposites();
        Instance inst = null;
        System.out.println("Looking for an instance of " + impl + ": ");
        for (Manager manager : selectionPath) {
            System.out.print(manager.getName() + "  ");
            inst = manager.resolveImpl(compo, impl, constraints, preferences);
            if (inst != null) {
                System.out.println("selected : " + inst);
                return inst;
            }
        }
        inst = impl.createInst(compo, null);
        System.out.println("instantiated : " + inst);
        return inst;
    }

    /**
     * Look for all the existing instance of "impl" that satisfy the constraints.
     * These instances must be either shared and visible from "compo".
     * If no existing instance can be found, one is created if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @return
     */
    public static Set<Instance> resolveImpls(Composite compo, Implementation impl, Set<Filter> constraints) {

        if (impl == null) {
            System.err.println("impl is null in resolveImpls");
            return null;
        }
        if (constraints == null)
            constraints = new HashSet<Filter>();
        List<Manager> selectionPath = ApamResolver.computeSelectionPathInst(compo, impl, constraints, null);

        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.out.println("No manager available. Cannot resolve ");
            return null;
        }
        if (compo == null)
            compo = CompositeImpl.getRootAllComposites();

        Set<Instance> insts = null;
        System.out.println("Looking for instances of " + impl + ": ");
        for (Manager manager : selectionPath) {
            System.out.print(manager.getName() + "  ");
            insts = manager.resolveImpls(compo, impl, constraints);
            if ((insts != null) && !insts.isEmpty()) {
                System.out.println("selected " + insts);
                return insts;
            }
        }
        if (insts == null)
            insts = new HashSet<Instance>();
        if (insts.isEmpty()) {
            insts.add(impl.createInst(compo, null));
        }
        System.out.println("instantiated " + (insts.toArray()[0]));
        return insts;
    }

    /**
     * Once the resolution terminated, either sucessfull or not, the managers are notified of the current
     * selection.
     * Currently, the managers cannot "undo" nor change the current selection.
     * 
     * @param spec
     * @param impl
     * @param inst
     * @param insts
     */
    public static void notifySelection(Instance client, String resName, String depName, Implementation impl,
            Instance inst,
            Set<Instance> insts) {
        for (Manager manager : APAMImpl.managerList) {
//            System.out.print("  notify to : " + manager.getName());
            manager.notifySelection(client, resName, depName, impl, inst, insts);
        }
//        System.out.println("");
    }

}
