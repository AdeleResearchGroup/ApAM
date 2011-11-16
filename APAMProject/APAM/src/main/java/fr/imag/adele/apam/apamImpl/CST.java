package fr.imag.adele.apam.apamImpl;

import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.InstanceBroker;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.Apform2Apam;
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
//    public static final String         SAMMAN                = "SAMMAN";
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
    // string
    public static final String         A_APAMAPPLI           = "ApamApplication";
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
    public static SpecificationBroker  SpecBroker            = null;
    public static ImplementationBroker ImplBroker            = null;
    public static InstanceBroker       InstBroker            = null;
//    public static Apform2Apam          apform2Apam           = null;

    public static DeploymentUnitBroker SAMDUBroker           = null;

    // the Apam entry point.
    public static APAMImpl             apam                  = null;

    public CST(APAMImpl theApam) {
        CST.SpecBroker = new SpecificationBrokerImpl();
        CST.ImplBroker = new ImplementationBrokerImpl();
        CST.InstBroker = new InstanceBrokerImpl();
        CST.InstBroker = new InstanceBrokerImpl();
//        CST.apform2Apam = new Apform2Apam();
        CST.apam = theApam;
    }
}
