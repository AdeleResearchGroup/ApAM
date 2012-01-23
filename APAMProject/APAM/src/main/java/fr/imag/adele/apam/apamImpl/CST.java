package fr.imag.adele.apam.apamImpl;

import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.InstanceBroker;
import fr.imag.adele.apam.SpecificationBroker;
import fr.imag.adele.apam.apform.Apform2Apam;

//import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class CST {

    // Constants "A_" means attribute name; "V_" means attribute value
    public static final String         A_SPECIFICATION       = "specification";
    public static final String         A_IMPLEMENTATION      = "implementation";
    public static final String         A_INSTANCE            = "instance";

    public static final String         A_APAMSPECNAME        = "ApamSpecName";

    public static String[]             predefAttributes      = { CST.A_SCOPE, CST.A_VISIBLE, CST.A_INSTANTIABLE,
        CST.A_MULTIPLE, CST.A_REMOTABLE, CST.A_SHARED,
        CST.A_INTERNALIMPL, CST.A_INTERNALINST, CST.A_MODELS, CST.A_APPLISCOPE,
        CST.A_COMPOSITESCOPE, CST.A_LOCALSCOPE,
        CST.A_LOCALVISIBLE, CST.A_COMPOSITEVISIBLE, CST.A_DEPENDENCIES };

    // indicate in which scope this object is visible. Scope for instances, implscope for implems.
    public static final String         A_SCOPE               = "scope";
    public static final String         A_VISIBLE             = "visible";
    public static final String         A_INSTANTIABLE        = "instantiable";
    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE            = "multiple";
    // remotable indicates that the instance can be used from a remote machine
    public static final String         A_REMOTABLE           = "remotable";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED              = "shared";
    // Composite properties
    // boolean
    public static final String         A_INTERNALIMPL        = "internalImplementations";
    // boolean
    public static final String         A_INTERNALINST        = "internalInstances";
    // List<ManagerModel>
    public static final String         A_MODELS              = "apam-models";
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
    public static final String         A_DEPENDENCIES        = "dependencies";

    // Attributes that cannot be changed by users
    public static final String[]       finalAttributes       = { CST.A_SPECNAME, CST.A_IMPLNAME, CST.A_INSTNAME,
        CST.A_COMPOSITE, CST.A_MAIN_IMPLEMENTATION };
    public static final String         A_SPECNAME            = "spec-name";
    public static final String         A_IMPLNAME            = "impl-name";
    public static final String         A_INSTNAME            = "ins-name";
    public static final String         A_MAIN_IMPLEMENTATION = "apam-main-implementation";
    // Value boolean
    public static final String         A_COMPOSITE           = "apam-composite";

    // for Visible and scope attributes attributes
    public static final String         V_GLOBAL              = "global";
    // visible in the current appli only
    public static final String         V_APPLI               = "appli";
    // visible in the current composite and on composites that depend on the current composite
    public static final String         V_COMPOSITE           = "composite";
    // visible in the current composite only
    public static final String         V_LOCAL               = "local";
    public static final String         V_TRUE                = "true";
    public static final String         V_FALSE               = "false";

    // Managers
    public static final String         APAMMAN               = "APAMMAN";
    public static final String         CONFMAN               = "CONFMAN";
    public static final String         DYNAMAN               = "DYNAMAN";
    public static final String         DISTRIMAN             = "DISTRIMAN";
    public static final String         OBRMAN                = "OBRMAN";

    public static final String         ROOTCOMPOSITETYPE     = "rootCompositeType";

    // Property names of composites
    //    // Capability
    //    public static final String         CAPABILITY_IMPLEMENTATION  = "apam-implementation";
    //    // Capability
    //    public static final String         CAPABILITY_SPECIFICATION  = "apam-specification";
    //    // Value boolean
    //    public static final String         CAPABILITY_COMPONENT  = "apam-component";
    //    // string
    //    public static final String         A_APAMAPPLI           = "ApamApplication";
    //    // String
    //    public static final String         A_MAIN_IMPLEMENTATION = "apam-main-implementation";


    // The entry point in the ASM : its brokers
    public static SpecificationBroker  SpecBroker            = null;
    public static ImplementationBroker ImplBroker            = null;
    public static InstanceBroker       InstBroker            = null;

    // the Apam entry point.
    public static APAMImpl             apam                  = null;

    public CST(APAMImpl theApam) {
        CST.SpecBroker = new SpecificationBrokerImpl();
        CST.ImplBroker = new ImplementationBrokerImpl();
        CST.InstBroker = new InstanceBrokerImpl();
        //        CST.InstBroker = new InstanceBrokerImpl();
        //        CST.apform2Apam = new Apform2Apam();
        CST.apam = theApam;
    }
}
