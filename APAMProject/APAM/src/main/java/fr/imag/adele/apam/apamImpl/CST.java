package fr.imag.adele.apam.apamImpl;

import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.InstanceBroker;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.Apform2ApamImpl;
import fr.imag.adele.apam.apformAPI.Apform2Apam;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class CST {

    // Constants
    // value object : address of the iPOJO apam dependency handler
    public static final String         A_DEPHANDLER          = "ApamDependencyHandler";
    public static final String         A_APAMSPECNAME        = "ApamSpecName";

    // indicate in which scope this object is visible. Scope for instances, implscope for implems.
    public static final String         A_SCOPE               = "SCOPE";
    public static final String         A_VISIBLE             = "VISIBLE";
    public static final String         A_INSTANTIABLE        = "INSTANTIABLE";
    // visible everywhere
    public static final String         V_GLOBAL              = "GLOBAL";
    // visible in the current appli only
    public static final String         V_APPLI               = "APPLI";
    // visible in the current composite and on composites that depend on the current composite
    public static final String         V_COMPOSITE           = "COMPOSITE";
    // visible in the current composite only
    public static final String         V_LOCAL               = "LOCAL";

    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE            = "MULTIPLE";
    // remotable indicates that the instance can be used from a remote machine
    public static final String         A_REMOTABLE           = "REMOTABLE";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED              = "SHARED";
    // for boolean attributes
    public static final String         V_TRUE                = "TRUE";
    public static final String         V_FALSE               = "FALSE";

    // Managers
    public static final String         APAMMAN               = "APAMMAN";
    public static final String         SAMMAN                = "SAMMAN";
    public static final String         CONFMAN               = "CONFMAN";
    public static final String         DYNAMAN               = "DYNAMAN";
    public static final String         DISTRIMAN             = "DISTRIMAN";
    public static final String         OBRMAN                = "OBRMAN";

    public static final String         ROOTCOMPOSITETYPE     = "rootCompositeType";

    // Property names of composites
    // Capability
    public static final String         CAPABILITY_COMPONENT  = "apam-component";
    // Capability
    public static final String         CAPABILITY_INTERFACE  = "apam-interface";
    // Value boolean
    public static final String         A_COMPOSITE           = "apam-composite";
    // String
    public static final String         A_MAIN_IMPLEMENTATION = "apam-main-implementation";

    // Composite properties
    // boolean
    public static final String         A_INTERNALIMPL        = "internalImplementations";
    // boolean
    public static final String         A_INTERNALINST        = "internalInstances";
    // List<ManagerModel>
    public static final String         A_MODELS              = "apam-models";
    // List<String>
//    public static final String         A_GLOBALSCOPE         = "globalScope";
    // List<String>
    public static final String         A_APPLISCOPE          = "appliScope";
    // List<String>
    public static final String         A_COMPOSITESCOPE      = "compositeScope";
    // List<String>
    public static final String         A_LOCALSCOPE          = "localScope";
    // List<String>
    public static final String         A_LOCALVISIBLE        = "localVisible";
    // List<String>
    public static final String         A_COMPOSITEVISIBLE    = "compositeVisible";
    // List<String>
//    public static final String         A_GLOBALVISIBLE       = "globalVisible";
    // List<DependencyModel>
    public static final String         A_DEPENDENCIES        = "dependencies";

    // The entry point in the ASM : its brokers
    public static SpecificationBroker  ASMSpecBroker         = null;
    public static ImplementationBroker ASMImplBroker         = null;
    public static InstanceBroker       ASMInstBroker         = null;
//    public static ApformImpl           apform                = null;
    public static Apform2Apam          apform2Apam           = null;

    // public static ApformInstance apformInst = null;
//    public static ApformSpecification  apformSpec            = null;

//    // The entry point in SAM : its brokers
//    public static SpecificationBroker  SAMSpecBroker         = null;
//    public static ImplementationBroker SAMImplBroker         = null;
//    public static InstanceBroker       SAMInstBroker         = null;
    public static DeploymentUnitBroker SAMDUBroker           = null;

    // the Apam entry point.
    public static APAMImpl             apam                  = null;

    // the implementation event handler. It installs an implem and waits for its apparition in SAM
//    public static SamImplEventHandler  implEventHandler      = null;

    public CST(APAMImpl theApam) {
        CST.ASMSpecBroker = new SpecificationBrokerImpl();
        CST.ASMImplBroker = new ImplementationBrokerImpl();
        CST.ASMInstBroker = new InstanceBrokerImpl();
        CST.ASMInstBroker = new InstanceBrokerImpl();
//        CST.apform = new ApformImpl();
        CST.apform2Apam = new Apform2ApamImpl();
        CST.apam = theApam;

        // CST.apformSpec = new AformSpecificationImpl();
//        CST.apformImpl = new ApformImplementationImpl();
//        CST.apformInst = new ApformInstanceImpl();

//            Machine AM = fr.imag.adele.am.LocalMachine.localMachine;
//            EventingEngine eventingEngine = AM.getEventingEngine();
//            CST.implEventHandler = new SamImplEventHandler();
//            eventingEngine.subscribe(CST.implEventHandler, EventProperty.TOPIC_IMPLEMENTATION);
//
//            BrokerBroker bb = AM.getBrokerBroker();
//            CST.SAMSpecBroker = (SpecificationBroker) bb.getBroker(SpecificationBroker.SPECIFICATIONBROKERNAME);
//            CST.SAMImplBroker = (ImplementationBroker) bb.getBroker(ImplementationBroker.IMPLEMENTATIONBROKERNAME);
//            CST.SAMInstBroker = (InstanceBroker) bb.getBroker(InstanceBroker.INSTANCEBROKERNAME);
//            CST.SAMDUBroker = (DeploymentUnitBroker) bb.getBroker(DeploymentUnitBroker.DEPLOYMENTUNITBROKERNAME);

    }
}