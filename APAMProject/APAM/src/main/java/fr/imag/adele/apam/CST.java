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
    public static final String         COMPOSITE             = "Composite";

    // The name of the attribute containing the dependency handler address (an
    // object of type ApamDepndencyHandler)
    public static final String         APAMDEPENDENCYHANDLER = "ApamDependencyHandler";
    public static final String         APAMSPECNAME          = "ApamSpecName";
    public static final String         APAMIMPLNAME          = "ApamImplName";

    public static final String         ASMMAN                = "ASMMAN";
    public static final String         SAMMAN                = "SAMMAN";
    public static final String         CONFMAN               = "CONFMAN";
    public static final String         DYNAMAN               = "DYNAMAN";
    public static final String         DISTRIMAN             = "DISTRIMAN";

    // Clonable attribute of services (Spec, Implem, Instance)
    public static final int            TRUE                  = 0;
    public static final int            FALSE                 = 1;

    // The entry point in the ASM : its brokers
    public static ASMSpecBroker        ASMSpecBroker         = null;
    public static ASMImplBroker        ASMImplBroker         = null;
    public static ASMInstBroker        ASMInstBroker         = null;

    // The entry point in SAM
    public static SpecificationBroker  SAMSpecBroker         = null;
    public static ImplementationBroker SAMImplBroker         = null;
    public static InstanceBroker       SAMInstBroker         = null;
    public static DeploymentUnitBroker SAMDUBroker           = null;

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
