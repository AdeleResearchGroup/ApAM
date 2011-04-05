package fr.imag.adele.samMan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.query.Query;
import fr.imag.adele.am.query.QueryLDAPImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
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

    // The entry point in SAM
    public static ImplementationBroker SAMImplBroker = null;
    public static InstanceBroker       SAMInstBroker = null;

    /*
     * The reference to APAM, injected by iPojo
     */
    private ManagersMng                apam;

    /**
     * SANMAN activated, register with APAM
     */
    public void start() {
        apam.addManager(this, 0);
        SamMan.SAMImplBroker = ASM.SAMImplBroker;
        SamMan.SAMInstBroker = ASM.SAMInstBroker;
    }

    public void stop() {
        apam.removeManager(this);
    }

    // TODO Must read the opportunist model and build the list of opportunist specs.
    // If empty, all is supposed to be opportunist ???
    private static Set<String> opportunismNames = new HashSet<String>();
    private static Set<Filter> filters          = new HashSet<Filter>();
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
    public List<Manager> getSelectionPathSpec(ASMInst from, String interfaceName, String specName, String depName,
            Filter filter, List<Manager> involved) {
        if (opportunistSpec(specName)) {
            involved.add(this);
        }
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, String samImplName, String implName, String depName,
            Filter filter, List<Manager> involved) {
        if (opportunistImpl(implName))
            involved.add(this);

        return involved;
    }

    @Override
    public ASMInst resolveSpec(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> constraints) {
        if ((interfaceName == null) && (specName == null)) {
            System.out.println("missing parameter interfaceName or specName");
            return null;
        }
        if (from == null) {
            System.out.println("ERROR : missing parameter from in resolveSpec");
            return null;
        }

        try {
            Query query = null;
            Filter filter;
            ASMSpec asmSpec;
            Set<Instance> samInsts;

            if ((constraints != null) && (constraints.size() > 0)) {
                filter = Util.buildFilter(constraints);
                query = new QueryLDAPImpl(filter.toString());
            }

            if (specName != null) {
                asmSpec = ASM.ASMSpecBroker.getSpec(specName);
            } else {
                asmSpec = ASM.ASMSpecBroker.getSpecInterf(interfaceName);
            }

            Instance theInstance = null;
            if (asmSpec == null) { // No ASM spec known. Look for a SAM instance
                if (interfaceName == null)
                    return null; // no Way
                samInsts = SamMan.SAMInstBroker.getInstances();
                for (Instance instance : samInsts) {
                    Specification samSpec = instance.getSpecification();
                    if (samSpec != null) {
                        String[] interfs = samSpec.getInterfaceNames();
                        for (String interf : interfs) {
                            if (interf.equals(interfaceName)) {
                                theInstance = instance;
                            }
                        }
                    }
                }
            } else { // We know the ASM specification
                if (asmSpec.getSamSpec() != null) { // Is sam spec known ?
                    samInsts = SamMan.SAMInstBroker.getInstances(asmSpec.getSamSpec(), query);
                    if ((samInsts != null) && (samInsts.size() > 0)) {
                        theInstance = (Instance) samInsts.toArray()[0];
                    }
                }
            }

            // Last chance look for an implementation that implement the interface.
            if ((theInstance == null) && (interfaceName != null)) {
                Set<Implementation> samImpls = SamMan.SAMImplBroker.getImplementations();
                for (Implementation impl : samImpls) {
                    Specification samSpec = impl.getSpecification();
                    if (samSpec != null) {
                        for (String interf : samSpec.getInterfaceNames()) {
                            if (interf.equals(interfaceName)) { // We got an implementation
                                theInstance = impl.createInstance(null); // create an instance
                                break;
                            }
                        }
                    }
                    if (theInstance != null)
                        break;
                }
            }

            if (theInstance == null)
                return null;

            // we have found a sam Instance.
            if (ASM.ASMInstBroker.getInst(theInstance) != null) {
                return ASM.ASMInstBroker.getInst(theInstance);
            }
            return ASM.ASMInstBroker.addInst(from.getComposite(), theInstance, null, specName, null);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public ASMInst resolveImpl(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints) {
        if ((samImplName == null) && (implName == null)) {
            System.out.println("ERROR : missing parameter samImplName or implName in resolveImpl");
            return null;
        }
        if (from == null) {
            System.out.println("ERROR : missing parameter from in resolveImpl");
            return null;
        }

        try {
            Query query = null;
            Filter filter;
            ASMImpl impl;
            Set<Instance> samInsts;

            if ((constraints != null) && (constraints.size() > 0)) {
                filter = Util.buildFilter(constraints);
                query = new QueryLDAPImpl(filter.toString());
            }
            // Is the impl known by Apam (either a sam name or logical name) ?
            if ((implName != null) && (ASM.ASMImplBroker.getImpl(implName) != null)) {
                impl = ASM.ASMImplBroker.getImpl(implName);
            } else {
                impl = ASM.ASMImplBroker.getImplSamName(implName);
            }
            if (impl == null)
                return null;

            samInsts = SamMan.SAMImplBroker.getInstances(impl.getSamImpl().getImplPid(), query);
            if ((samInsts == null) || (samInsts.size() == 0))
                return null;
            Instance theInstance = (Instance) samInsts.toArray()[0];
            if (ASM.ASMInstBroker.getInst(theInstance) != null) {
                return ASM.ASMInstBroker.getInst(theInstance);
            }
            return ASM.ASMInstBroker.addInst(from.getComposite(), theInstance, implName, null, null);
        } catch (Exception e) {
        }

        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void newComposite(ManagerModel model, Composite composite) {

    }

}
