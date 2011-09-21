package fr.imag.adele.apam;

import fr.imag.adele.am.Machine;
import fr.imag.adele.am.broker.BrokerBroker;
import fr.imag.adele.am.eventing.EventingEngine;
import fr.imag.adele.apam.ASMImpl.ASMImplBrokerImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstBrokerImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecBrokerImpl;
import fr.imag.adele.apam.ASMImpl.SamImplEventHandler;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;
import fr.imag.adele.sam.event.EventProperty;

public class CST {

    // Constants
    // value object : address of the iPOJO apam dependency handler
    public static final String         A_DEPHANDLER                           = "ApamDependencyHandler";
    public static final String         A_APAMSPECNAME                         = "ApamSpecName";
    public static final String         A_APAMIMPLNAME                         = "ApamImplName";

    // indicate in which scope this object is visible. Scope for instances, implscope for implems.
    public static final String         A_SCOPE                                = "SCOPE";
    public static final String         A_IMPLSCOPE                            = "IMPLSCOPE";
    public static final String         V_LOCALSCOPE                           = "LOCALSCOPE";
    // visible everywhere
    public static final String         V_GLOBAL                               = "GLOBAL";
    // visible in the current appli only
    public static final String         V_APPLI                                = "APPLI";
    // visible in the current composite and on composites that depend on the current composite
    public static final String         V_COMPOSITE                            = "COMPOSITE";
    // visible in the current composite only
    public static final String         V_LOCAL                                = "LOCAL";

    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE                             = "MULTIPLE";
    // remotable indicates that the instance can be used from a remote machine
    public static final String         A_REMOTABLE                            = "REMOTABLE";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED                               = "SHARED";
    // for boolean attributes
    public static final String         V_TRUE                                 = "TRUE";
    public static final String         V_FALSE                                = "FALSE";

    // Managers
    public static final String         APAMMAN                                = "APAMMAN";
    public static final String         SAMMAN                                 = "SAMMAN";
    public static final String         CONFMAN                                = "CONFMAN";
    public static final String         DYNAMAN                                = "DYNAMAN";
    public static final String         DISTRIMAN                              = "DISTRIMAN";
    public static final String         OBRMAN                                 = "OBRMAN";

    public static final String         ROOTCOMPOSITETYPE                      = "rootCompositeType";
    // Property names of composites
    // Capability
    public static final String         CAPABILITY_COMPONENT                   = "apam-component";
    // Capability
    public static final String         CAPABILITY_INTERFACE                   = "apam-interface";
    // Value boolean
    public static final String         PROPERTY_COMPOSITE                     = "apam-composite";
    // String
    // public static final String PROPERTY_IMPLEMENTATION_NAME = "apam-implementation";
    // String
    public static final String         PROPERTY_COMPOSITE_MAIN_IMPLEMENTATION = "apam-main-implementation";
    // String
    public static final String         PROPERTY_COMPOSITE_MAIN_SPECIFICATION  = "apam-specification";
    // List<ManagerModel>
    public static final String         PROPERTY_COMPOSITE_MODELS              = "apam-models";

    // The entry point in the ASM : its brokers
    public static ASMSpecBroker        ASMSpecBroker                          = null;
    public static ASMImplBroker        ASMImplBroker                          = null;
    public static ASMInstBroker        ASMInstBroker                          = null;

    // The entry point in SAM : its brokers
    public static SpecificationBroker  SAMSpecBroker                          = null;
    public static ImplementationBroker SAMImplBroker                          = null;
    public static InstanceBroker       SAMInstBroker                          = null;
    public static DeploymentUnitBroker SAMDUBroker                            = null;

    // the Apam entry point.
    public static APAMImpl             apam                                   = null;

    // the implementation event handler. It installs an implem and waits for its apparition in SAM
    public static SamImplEventHandler  implEventHandler                       = null;

    public CST(APAMImpl theApam) {
        try {
            CST.ASMSpecBroker = new ASMSpecBrokerImpl();
            CST.ASMImplBroker = new ASMImplBrokerImpl();
            CST.ASMInstBroker = new ASMInstBrokerImpl();

            Machine AM = fr.imag.adele.am.LocalMachine.localMachine;
            EventingEngine eventingEngine = AM.getEventingEngine();
            CST.implEventHandler = new SamImplEventHandler();
            eventingEngine.subscribe(CST.implEventHandler, EventProperty.TOPIC_IMPLEMENTATION);

            BrokerBroker bb = AM.getBrokerBroker();
            CST.SAMSpecBroker = (SpecificationBroker) bb.getBroker(SpecificationBroker.SPECIFICATIONBROKERNAME);
            CST.SAMImplBroker = (ImplementationBroker) bb.getBroker(ImplementationBroker.IMPLEMENTATIONBROKERNAME);
            CST.SAMInstBroker = (InstanceBroker) bb.getBroker(InstanceBroker.INSTANCEBROKERNAME);
            CST.SAMDUBroker = (DeploymentUnitBroker) bb.getBroker(DeploymentUnitBroker.DEPLOYMENTUNITBROKERNAME);

            CST.apam = theApam;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
