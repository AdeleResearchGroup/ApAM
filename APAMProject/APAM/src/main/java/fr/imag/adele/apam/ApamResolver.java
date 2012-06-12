package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apamImpl.APAMImpl;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

public class ApamResolver {

    private static ApamResolver apamResolver = new ApamResolver();

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

    private class DepMult {
        public String        depType = null;
        public Set<Instance> insts   = null;

        public DepMult(String dep, Set<Instance> insts) {
            depType = dep;
            this.insts = insts;
            // System.err.println("new DepMult. dep= " + dep + ". instances= " + insts);
        }
    }

    /**
     * If the client's composite has declared a dependency toward interf or specName; it is a promotion.
     * The client becomes the embedding composite; visibility and scope become the one of the embedding
     * 
     * @param client
     * @param interf or specName
     * @return the composite dependency from the composite.
     */
    private static DepMult getPromotion(Instance client, DependencyDeclaration dependency) {

        // look for a matching dependency in the enclosing composite definition
        DependencyDeclaration promotion = null;
        for (DependencyDeclaration enclosingDependency : client.getComposite().getCompType().getCompoDeclaration().getDependencies()) {

            // TODO Should we have other criteria to match ?
            if (enclosingDependency.getTarget().equals(dependency.getTarget()))
                promotion = enclosingDependency;
        }

        if (promotion == null)
            return null;

        // it is a declared promotion.
        // check cardinality
        String depId = promotion.getIdentifier();
        Set<Instance> dests = client.getComposite().getWireDests(depId); // For composite, the wire name is the dest

        if (!promotion.isMultiple() && (dests != null)) {
            System.err.println("ERROR : wire " + client.getComposite() + " -" + depId + "-> "
                    + " allready existing.");
            return null;
        }
        //        System.err.println("Promoting " + client + " : " + client.getComposite() + " -" + depFound.dependencyName
        //                + "-> ");
        return ApamResolver.apamResolver.new DepMult(depId, dests);
    }

    // if the instance is unused, it will become the main instance of a new composite.
    private static Composite getClientComposite(Instance mainInst) {
        if (mainInst.isUsed())
            return mainInst.getComposite();

        Implementation mainImplem = mainInst.getImpl();
        String newName = mainImplem.getName() + "_Appli";

        CompositeType newCompoT = CompositeTypeImpl.createCompositeType(null, newName, mainImplem.getName(), null,
                null, null);
        // Composite intCompo = CompositeImpl.rootComposite ;
        return new CompositeImpl(newCompoT, null, mainInst, (Map<String, Object>) null,
                newCompoT.getApformImpl().createInstance(null));
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
     * @param specName the *logical* name of that specification. May be null.
     * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
     * @param constraints. Optional. To select the right instance.
     * @param preferences. Optional. To select the right instance.
     * @return
     */
    public static Instance newWireSpec(Instance client, String depName) {

        // Get required resource from dependency declaration, take the declaration declared at the most concrete level
        DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName); 
        if (dependency == null)
            dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);

        if ((dependency == null) && (client.getImpl().getSpec().getApformSpec() != null))
            dependency = client.getImpl().getSpec().getApformSpec().getDeclaration().getDependency(depName);

        if (dependency == null) {
            System.err.println("dependency declaration not found "+depName);
            return null;
        }

        Composite compo = ApamResolver.getClientComposite(client);


        // if it is a promotion, visibility and scope is the one of the embedding composite.
        DepMult depMult = ApamResolver.getPromotion(client, dependency);
        Instance inst = null;
        if (depMult != null) { // it is a promotion
            compo = compo.getComposite();
            if (depMult.insts != null)
                inst = (Instance) depMult.insts.toArray()[0]; // the common instance
        }

        // inst is not null if it is a multiple promotion. Use the same target.
        if (inst == null) { // normal case. Try to find the instance.
            CompositeType compoType = compo.getCompType();
            Implementation impl = ApamResolver.resolveSpecByResource(compoType, dependency);

            if (impl == null) {
                System.err.println("Failed to resolve " + dependency.getTarget() + " from " + client + "(" + depName
                        + ")");
                // ApamResolver.notifySelection(client, specName, depName, null, null, null);
                return null;
            }

            inst = ApamResolver.resolveImpl(client, compo, impl, dependency);
        }
        //                    Util.toFilter(dependency.getInstanceConstraints()),
        //                    Util.toFilterList(dependency.getInstancePreferences()));
        //        }
        if (inst != null) {
            if (depMult != null) { // it was a promotion, embedding composite must also be linked as the source (client)
                client.getComposite().createWire(inst, depMult.depType);
                System.err.println("Promoting " + client + " : " + client.getComposite() + " -" + depMult.depType
                        + "-> "
                        + inst);
            }
            // in all cases the "real" client instance must be linked
            client.createWire(inst, depName);
        }
        ApamResolver.notifySelection(client, dependency.getTarget(), depName, inst.getImpl(), inst, null);
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
     * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
     * @return
     */
    public static Set<Instance> newWireSpecs(Instance client, String depName) {

        // Get required resource from dependency declaration, take the declaration declared at the most concrete level
        DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName); 
        if (dependency == null)
            dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);

        if ((dependency == null) && (client.getImpl().getSpec().getApformSpec() != null))
            dependency = client.getImpl().getSpec().getApformSpec().getDeclaration().getDependency(depName);

        if (dependency == null) {
            System.err.println("dependency declaration not found "+depName);
            return null;
        }

        Composite compo = ApamResolver.getClientComposite(client);
        // if it is a promotion, visibility and scope is the one of the embedding composite.
        DepMult depMult = ApamResolver.getPromotion(client, dependency);
        Set<Instance> insts = null;
        if (depMult != null) { // it is a promotion
            compo = compo.getComposite();
            if (depMult.insts != null)
                insts = depMult.insts;
        }

        if (insts == null) { // normal case. Try to find the instances.
            // not null if it is a multiple promotion. Use the same target.
            CompositeType compoType = compo.getCompType();
            Implementation impl = ApamResolver.resolveSpecByResource(compoType, dependency);

            //            .getResource(),
            //                    implementationConstraints, implementationPreferences);
            //
            //            if (specName != null)
            //                impl = ApamResolver.resolveSpecByName(compoType, specName, implementationConstraints,
            //                        implementationPreferences);
            //            else {
            //                impl = ApamResolver.resolveSpecByResource(compoType, new InterfaceReference(interfaceName),
            //                        implementationConstraints, implementationPreferences);
            //                specName = interfaceName;
            //            }
            if (impl == null) {
                System.err.println("Failed to resolve " + dependency.getTarget() + " from " + client + "(" + depName
                        + ")");
                // ApamResolver.notifySelection(client, specName, depName, null, null, null);
                return null;
            }
            insts = ApamResolver.resolveImpls(compo, impl, Util.toFilter(dependency.getInstanceConstraints()));
        }
        if ((insts != null) && !insts.isEmpty()) {
            for (Instance inst : insts) {
                if (depMult != null) { // it was a promotion, embedding composite must be linked as the source
                    // (client)
                    client.getComposite().createWire(inst, depMult.depType);
                    System.err.println("Promoting " + client + " : " + client.getComposite() + " -" + depMult.depType
                            + "-> " + inst);
                }
                // in all cases the client must be linked
                client.createWire(inst, depName);
            }
        }
        ApamResolver.notifySelection(client, dependency.getTarget(), depName,
                ((Instance) insts.toArray()[0]).getImpl(), null, insts);
        return insts;
    }

    /**
     * An APAM client instance requires to be wired with an instance of implementation. If found, the instance is
     * returned.
     * 
     * @param client the instance that requires the specification
     * @param samImplName the technical name of implementation to resolve, as returned by SAM.
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
     * @return
     */
    public static Instance newWireImpl(Instance client, String implName, String depName) {
        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        // Get required resource from dependency declaration, take the declaration declared at the most concrete level
        DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName); 
        if (dependency == null)
            dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);

        if ((dependency == null) && (client.getImpl().getSpec().getApformSpec() != null))
            dependency = client.getImpl().getSpec().getApformSpec().getDeclaration().getDependency(depName);

        if (dependency == null) {
            System.err.println("dependency declaration not found "+depName);
            return null;
        }

        // TODO Warning, it may be a promotion, but it is not possible to know, at that point,
        // the spec or interfaces of implName. Should be resolved first, with the current compotype.
        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compType = client.getComposite().getCompType();

        Implementation impl = ApamResolver.findImplByName(compType, implName);
        if (impl == null) {
            System.err.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            // ApamResolver.notifySelection(client, implName, depName, null, null, null);
            return null;
        }

        Instance inst = ApamResolver.resolveImpl(client, compo, impl, dependency);
        // instanceConstraints, instancePreferences);
        //        if (inst != null)
        //            client.createWire(inst, depName);

        //
        ApamResolver.notifySelection(client, impl.getImplDeclaration().getReference(), depName, impl, inst, null);
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
    public static Set<Instance> newWireImpls(Instance client, String implName, String depName) {

        if ((implName == null) || (client == null)) {
            System.err.println("missing client or implementation name");
        }

        // Get required resource from dependency declaration, take the declaration declared at the most concrete level
        DependencyDeclaration dependency = client.getApformInst().getDeclaration().getDependency(depName); 
        if (dependency == null)
            dependency = client.getImpl().getApformImpl().getDeclaration().getDependency(depName);

        if ((dependency == null) && (client.getImpl().getSpec().getApformSpec() != null))
            dependency = client.getImpl().getSpec().getApformSpec().getDeclaration().getDependency(depName);

        if (dependency == null) {
            System.err.println("dependency declaration not found "+depName);
            return null;
        }

        Composite compo = ApamResolver.getClientComposite(client);
        CompositeType compType = compo.getCompType();

        Implementation impl = ApamResolver.findImplByName(compType, implName);
        if (impl == null) {
            System.err.println("Failed to resolve " + implName + " from " + client + "(" + depName + ")");
            // ApamResolver.notifySelection(client, implName, depName, null, null, null);
            return null;
        }

        Set<Instance> insts = ApamResolver
        .resolveImpls(compo, impl, Util.toFilter(dependency.getInstanceConstraints()));
        if ((insts != null) && !insts.isEmpty()) {
            for (Instance inst : insts) {
                client.createWire(inst, depName);
            }
        }
        ApamResolver.notifySelection(client, impl.getImplDeclaration().getReference(), depName, impl, null, insts);
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
    public static List<Manager> computeSelectionPathSpec(CompositeType compTypeFrom, ResolvableReference resource,
            Set<Filter> constraints, List<Filter> preferences) {
        if (APAMImpl.managerList.size() == 0) {
            System.err.println("No manager available. Cannot resolve ");
            return null;
        }
        List<Manager> selectionPath = new ArrayList<Manager>();
        for (int i = 1; i < APAMImpl.managerList.size(); i++) { // start from 1 to skip ApamMan
            APAMImpl.managerList.get(i).getSelectionPathSpec(compTypeFrom, resource, constraints, preferences, selectionPath);
        }
        // To select first in Apam
        selectionPath.add(0, APAMImpl.apamMan);
        return selectionPath;
    }

    public static List<Manager> computeSelectionPathImpl(CompositeType compTypeFrom, String implName) {
        if (APAMImpl.managerList.size() == 0) {
            System.err.println("No manager available. Cannot resolve ");
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
            System.err.println("No manager available. Cannot resolve ");
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
        // it is deployed

        // impl is inside compotype
        compoType.addImpl(impl);

        if (impl.isUsed()) {
            System.out.println("Logicaly deployed " + impl);
        } else {// it was unused so far.
            Apform.setUsedImpl(impl); // Remove it from unused
            if (impl instanceof CompositeType) { // it is a composite type
                // if impl is a composite type, it is embedded inside compoFrom
                ((CompositeTypeImpl) compoType).addEmbedded((CompositeType) impl);
            }
            System.out.println("   deployed " + impl);
        }
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
            System.err.println("No manager available. Cannot resolve ");
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
        List<Manager> selectionPath = ApamResolver.computeSelectionPathSpec(compoTypeFrom, new SpecificationReference(
                specName),
                constraints,
                preferences);
        if (selectionPath.isEmpty()) {
            System.err.println("No manager available. Cannot resolve ");
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
            impl = manager.resolveSpecByResource(compoTypeFrom, new SpecificationReference(specName), constraints,
                    preferences);

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
    public static Implementation resolveSpecByResource(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
        // Get the constraints and preferences by merging declarations
        Set<Filter> implementationConstraints = Util.toFilter(dependency.getImplementationConstraints());
        List<Filter> implementationPreferences = Util.toFilterList(dependency.getImplementationPreferences());

        List<Manager> selectionPath = ApamResolver.computeSelectionPathSpec(compoTypeFrom, dependency.getTarget(),
                implementationConstraints, implementationPreferences);
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.err.println("No manager available. Cannot resolve ");
            return null;
        }

        System.out.println("Looking for an implem with  " + dependency);
        if (compoTypeFrom == null)
            compoTypeFrom = CompositeTypeImpl.getRootCompositeType();
        Implementation impl = null;
        boolean deployed = false;
        for (Manager manager : selectionPath) {
            if (!manager.getName().equals(CST.APAMMAN))
                deployed = true;
            System.out.print(manager.getName() + "  ");
            impl = manager.resolveSpecByResource(compoTypeFrom, dependency.getTarget(),
                    implementationConstraints, implementationPreferences);
            if (impl != null) {
                ApamResolver.deployedImpl(compoTypeFrom, impl, deployed);
                return impl;
            }
        }
        return null;
    }

    /**
     * Look for an instance of "impl" that satisfies the constraints. That instance must be either
     * - shared and visible from "compo", or
     * - instantiated if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     * @return
     */
    public static Instance resolveImpl(Instance client, Composite compo, Implementation impl,
            DependencyDeclaration dependency) {
        Set<Filter> constraints = Util.toFilter(dependency.getImplementationConstraints());
        List<Filter> preferences = Util.toFilterList(dependency.getImplementationPreferences());
        List<Manager> selectionPath = ApamResolver.computeSelectionPathInst(compo, impl, constraints, preferences);
        if ((selectionPath == null) || selectionPath.isEmpty()) {
            System.err.println("No manager available. Cannot resolve ");
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
                client.createWire(inst, dependency.getIdentifier());
                return inst;
            }
        }
        inst = impl.createInst(compo, null);
        System.out.println("instantiated : " + inst);
        client.createWire(inst, dependency.getIdentifier());
        // TODO Notify dynaman

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
            System.err.println("No manager available. Cannot resolve ");
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
    public static void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl,
            Instance inst, Set<Instance> insts) {
        for (Manager manager : APAMImpl.managerList) {
            manager.notifySelection(client, resName, depName, impl, inst, insts);
        }
    }

}
