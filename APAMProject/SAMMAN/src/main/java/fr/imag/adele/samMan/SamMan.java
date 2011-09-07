package fr.imag.adele.samMan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.am.query.QueryLDAPImpl;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;

public class SamMan implements Manager {

    /*
     * The reference to APAM, injected by iPojo
     */
    private ManagersMng apam;

    @Override
    public String getName() {
        return "SAMMAN";
    }

    /**
     * SANMAN activated, register with APAM
     */
    public void start() {
        apam.addManager(this, 0);
    }

    public void stop() {
        apam.removeManager(this);
    }

    // TODO Must read the opportunist model and build the list of opportunist specs.
    // If empty, all is supposed to be opportunist ???
    private static Set<String> opportunismNames = new HashSet<String>();
    private static boolean     specOpportunist  = true;                 // if the model says all specifications are
    // opportunist
    private static boolean     implOpportunist  = true;                 // if the model says all implementations are

    // opportunist

    private boolean opportunistSpec(String specName) {
        if (SamMan.specOpportunist)
            return true;
        return true; // TODO waiting for the models
    }

    private boolean opportunistImpl(String implName) {
        if (SamMan.implOpportunist)
            return true;
        return SamMan.opportunismNames.contains(implName);
    }

    @Override
    public List<Manager> getSelectionPathSpec(ASMInst from, CompositeType composite, String interfaceName,
            String specName,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> involved) {
        if (opportunistSpec(specName)) {
            involved.add(this);
        }
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, CompositeType compType, String implName,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
        if (opportunistImpl(implName))
            selPath.add(this);

        return selPath;
    }

    private Set<Instance> getSamInstanceInterf(String interfaceName) {
        Set<Instance> insts = new HashSet<Instance>();
        try {
            for (Instance instance : CST.SAMInstBroker.getInstances()) {
                Specification samSpec = instance.getSpecification();
                if (samSpec != null) {
                    String[] interfs = samSpec.getInterfaceNames();
                    for (String interf : interfs) {
                        if (interf.equals(interfaceName)) { // we found one
                            insts.add(instance);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return insts;
    }

    private Set<Instance> getSamInstanceInterfaces(String[] interfaces) {
        Set<Instance> insts = new HashSet<Instance>();
        try {
            for (Instance instance : CST.SAMInstBroker.getInstances()) {
                Specification samSpec = instance.getSpecification();
                if (samSpec != null) {
                    String[] interfs = samSpec.getInterfaceNames();
                    if (Util.sameInterfaces(interfs, interfaces)) {
                        insts.add(instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return insts;
    }

    @Override
    public ASMInst resolveSpec(Composite instComposite, String interfaceName, String specName,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveSpec0(instComposite, interfaceName, null, specName, constraints, preferences, false,
                null);
    }

    @Override
    public Set<ASMInst> resolveSpecs(Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveSpec0(instComposite, interfaceName, null, specName, constraints, preferences, true, allInst);
        return allInst;
    }

    /**
     * At least one of : specName, interfaceName, interfaces is required.
     * Only one is used, they are considered in that order.
     * 
     * @param instComposite
     * @param interfaceName. Optional : a specification containing at least this interface is considered satisfactory.
     * @param interfaces. Optional : we need a specification with exactly the interfaces provided in the array.
     * @param specName. Optional : name of the specification. If provided interfaces are not checked.
     * @param constraints. Optional
     * @param preferences. Optional
     * @param multiple
     * @param allInst
     * @return
     */
    public ASMInst resolveSpec0(Composite instComposite, String interfaceName, String[] interfaces,
            String specName, Set<Filter> constraints, List<Filter> preferences, boolean multiple, Set<ASMInst> allInst) {
        if ((interfaceName == null) && (specName == null) && (interfaces == null)) {
            System.err.println("ERROR : missing parameter interfaceName or specName");
            return null;
        }
        CompositeType implComposite = null;
        if (instComposite != null) {
            implComposite = instComposite.getCompType();
        }

        try {
            ASMSpec asmSpec;
            if (specName != null) {
                asmSpec = CST.ASMSpecBroker.getSpec(specName);
            } else {
                if (interfaceName != null)
                    asmSpec = CST.ASMSpecBroker.getSpecInterf(interfaceName);
                else
                    asmSpec = CST.ASMSpecBroker.getSpec(interfaces);
            }

            // Look by its specification
            Set<Instance> instInterf = new HashSet<Instance>();
            if (asmSpec != null) { // Look by its sam interface
                Specification spec = asmSpec.getSamSpec();
                if (spec != null) { // Is sam spec known ?
                    for (Instance inst : CST.SAMInstBroker.getInstances()) {
                        if (inst.getSpecification() == spec)
                            instInterf.add(inst);
                    }
                }
            } else { // Look by its interface
                if (interfaceName != null) {
                    instInterf = getSamInstanceInterf(interfaceName);
                } else {
                    if (interfaces != null) {
                        instInterf = getSamInstanceInterfaces(interfaces);
                    }
                }
            }
            // eliminate those instances that have an Apam impl. it has already been checked by ApamMan
            Set<Instance> allInstances = new HashSet<Instance>();
            for (Instance in : instInterf) {
                if (CST.ASMImplBroker.getImpl(in.getImplementation()) == null)
                    allInstances.add(in);
            }

            // check if it satisfies the constraints
            Set<Instance> matchInsts;
            if ((constraints == null) || constraints.isEmpty()) {
                matchInsts = allInstances;
            } else {
                matchInsts = new HashSet<Instance>();
                Filter filter = Util.buildFilter(constraints);
                Query query = new QueryLDAPImpl(filter.toString());
                for (Instance inst : allInstances) {
                    if (inst.match(query))
                        matchInsts.add(inst);
                }
            }

            ASMInst returnInst = null;
            for (Instance inst : matchInsts) {
                // ignore the Apam instances, they have been checked by ApamMan
                if (CST.ASMInstBroker.getInst(inst) == null) {
                    returnInst = CST.ASMInstBroker.addInst(instComposite, inst, null, specName, null);
                    if (multiple)
                        allInst.add(returnInst);
                    else
                        return returnInst;
                }
            }

            // we have found a sam Instance.
            if (returnInst != null)
                return null;

            // Last chance look for an implementation that implements the interface(s).
            if ((interfaceName != null) || (interfaces != null)) {
                for (Implementation impl : CST.SAMImplBroker.getImplementations()) {
                    // if it is an Apam impl, it has already been checked by ApamMan
                    if (CST.ASMImplBroker.getImpl(impl) != null)
                        continue;
                    Specification samSpec = impl.getSpecification();
                    if (samSpecMatchInterface(samSpec, interfaceName, interfaces)) {
                        String apamSpecName = (String) impl
                                        .getProperty(CST.PROPERTY_COMPOSITE_MAIN_SPECIFICATION);
                        // activate the implementation in APAM, an create a new instance by APAM API.
                        // This will take care of the case of composites
                        ASMImpl asmImpl = CST.ASMImplBroker.addImpl(implComposite, impl.getName(),
                                        apamSpecName, null);
                        returnInst = asmImpl.createInst(instComposite, null);
                        if (multiple) {
                            allInst.add(returnInst);
                        } else
                            return returnInst;
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean samSpecMatchInterface(Specification samSpec, String interfaceName, String[] interfaces) {
        if (samSpec == null)
            return false;
        if (interfaceName != null) {
            for (String interf : samSpec.getInterfaceNames()) {
                if (interf.equals(interfaceName)) {
                    return true;
                }
            }
        }
        return Util.sameInterfaces(samSpec.getInterfaceNames(), interfaces);
    }

    // the resolution from an Apam impl has been checked by ApamMan.
    // If ApamMan could nor resol, SamMan either.
    // Do nothing
    @Override
    public ASMInst resolveImpl(CompositeType implComposite, Composite instComposite, String implName,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveImpl0(implComposite, instComposite, implName, constraints, preferences, false, null);
    }

    // the resolution from an Apam impl has been checked by ApamMan.
    // If ApamMan could nor resol, SamMan either.
    // Do nothing
    @Override
    public Set<ASMInst> resolveImpls(CompositeType implComposite, Composite instComposite, String implName,
            Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInst = new HashSet<ASMInst>();
        resolveImpl0(implComposite, instComposite, implName, constraints, preferences, true, allInst);
        return allInst;
    }

    private ASMInst resolveImpl0(CompositeType implComposite, Composite instComposite, String samImplName,
            Set<Filter> constraints, List<Filter> preferences, boolean multiple, Set<ASMInst> allInsts) {
        try {
            Implementation samImpl = CST.SAMImplBroker.getImplementation(samImplName);
            if ((samImpl == null) || (CST.ASMImplBroker.getImpl(samImplName) != null))
                return null;

            //the implementation has been found. Create it in Apam.
            String apamSpecName = (String) samImpl.getProperty(CST.PROPERTY_COMPOSITE_MAIN_SPECIFICATION);
            ASMImpl asmImpl = CST.ASMImplBroker.addImpl(implComposite, samImplName, apamSpecName, null);

            //Now look for the instances to return.
            Set<Instance> samInsts = new HashSet<Instance>();
            //Eliminate the apam instances, they have been checked allready.
            for (Instance instance : samImpl.getInstances()) {
                if (CST.ASMInstBroker.getInst(instance) == null)
                    samInsts.add(instance);
            }

            // check if it satisfies the constraints
            Set<Instance> matchInsts;
            if ((constraints == null) || constraints.isEmpty()) {
                matchInsts = samInsts;
            } else {
                matchInsts = new HashSet<Instance>();
                Filter filter = Util.buildFilter(constraints);
                Query query = new QueryLDAPImpl(filter.toString());
                for (Instance inst : samInsts) {
                    if (inst.match(query))
                        matchInsts.add(inst);
                }
            }

            ASMInst returnInst = null;
            //No instance found, instantiate.
            if (matchInsts.isEmpty()) {
                returnInst = asmImpl.createInst(instComposite, null);
                if (multiple) {
                    allInsts.add(returnInst);
                }
                return returnInst;
            }

            //create in Apam and return the apam instances.
            for (Instance inst : matchInsts) {
                returnInst = CST.ASMInstBroker.addInst(instComposite, inst, null, apamSpecName, null);
                if (multiple)
                    allInsts.add(returnInst);
                else
                    return returnInst;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void newComposite(ManagerModel model, CompositeType composite) {

    }

    @Override
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints) {
        return initConstraints;
    }

    @Override
    public ASMImpl resolveImplByName(Composite composite, String implName) {
        return null;
    }

    @Override
    public ASMImpl resolveSpecByName(Composite composite, String specName, Set<Filter> constraints,
            List<Filter> preferences) {
        ASMInst inst = resolveSpec0(composite, null, null, specName,
                constraints, preferences, false, null);
        if (inst != null)
            return inst.getImpl();
        return null;
    }

    @Override
    public ASMImpl resolveSpecByInterface(Composite composite, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences) {
        ASMInst inst = resolveSpec0(composite, interfaceName, interfaces, null,
                 constraints, preferences, false, null);
        if (inst != null)
            return inst.getImpl();
        return null;
    }

}
