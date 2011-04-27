package fr.imag.adele.apam;

import fr.imag.adele.am.Machine;
import fr.imag.adele.am.broker.BrokerBroker;
import fr.imag.adele.apam.ASMImpl.ASMImplBrokerImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstBrokerImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecBrokerImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class CST {

    // Constants
    // value object : address of the iPOJO apam dependency handler
    public static final String         A_DEPHANDLER   = "ApamDependencyHandler";
    public static final String         A_APAMSPECNAME = "ApamSpecName";
    public static final String         A_APAMIMPLNAME = "ApamImplName";

    // indicate in which scope this object is visible. Instance have the visibility of its implementaiton
    public static final String         A_SCOPE        = "SCOPE";
    // visible everywhere
    public static final String         V_GLOBAL       = "GLOBAL";
    // visible in the current appli only
    public static final String         V_APPLI        = "APPLI";
    // visible in the current composite and on composites that depend on the current composite
    public static final String         V_COMPOSITE    = "COMPOSITE";
    // visible in the current composite only
    public static final String         V_LOCAL        = "LOCAL";

    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE     = "MULTIPLE";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED       = "SHARED";
    public static final String         V_TRUE         = "TRUE";
    public static final String         V_FALSE        = "FALSE";

    // Managers
    public static final String         APAMMAN        = "APAMMAN";
    public static final String         SAMMAN         = "SAMMAN";
    public static final String         CONFMAN        = "CONFMAN";
    public static final String         DYNAMAN        = "DYNAMAN";
    public static final String         DISTRIMAN      = "DISTRIMAN";

    // The entry point in the ASM : its brokers
    public static ASMSpecBroker        ASMSpecBroker  = null;
    public static ASMImplBroker        ASMImplBroker  = null;
    public static ASMInstBroker        ASMInstBroker  = null;

    // The entry point in SAM : its brokers
    public static SpecificationBroker  SAMSpecBroker  = null;
    public static ImplementationBroker SAMImplBroker  = null;
    public static InstanceBroker       SAMInstBroker  = null;
    public static DeploymentUnitBroker SAMDUBroker    = null;

    public static APAMImpl             apam;

    public CST(APAMImpl theApam) {
        try {
            CST.ASMSpecBroker = new ASMSpecBrokerImpl();
            CST.ASMImplBroker = new ASMImplBrokerImpl();
            CST.ASMInstBroker = new ASMInstBrokerImpl();

            Machine AM = fr.imag.adele.am.LocalMachine.localMachine;
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
