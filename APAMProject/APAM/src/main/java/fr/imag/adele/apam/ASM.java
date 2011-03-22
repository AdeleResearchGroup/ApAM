package fr.imag.adele.apam;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.Machine;
import fr.imag.adele.am.broker.BrokerBroker;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.samAPIImpl.ASMImplBrokerImpl;
import fr.imag.adele.apam.samAPIImpl.ASMInstBrokerImpl;
import fr.imag.adele.apam.samAPIImpl.ASMSpecBrokerImpl;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class ASM {
	
// Constants
	public static final String COMPOSITE = "Composite" ;
	//The PID indicating that the caller is APAM or an APAM manager.
//	public static final PID    PID = new ApamPID () ;
	
	// State attribute of services (Spec, Implem, Instance)
//	public static final int ACTIVE  = 0 ;	//Used : destination of Requires, Uses or Wire.
//	public static final int IDLE    = 1 ;	//Not used : no Requires, Uses or Wire lead to this object
//	public static final int LOST    = 2 ;	//The corresponding object disappeared in SAM
//	public static final int REMOVED = 3 ;	//The object has been removed explicitly
//	public static final int INACTIVE= 4 ;	//The object is inactive as seen by iPOJO
//	public static final int WAITING = 5 ;	//The object is waiting for a wire
	
	//The name of the attribute containing the dependency handler address (an object of type ApamDepndencyHandler)
	public static final String ApamDependencyHandlerAddress = "ApamDependencyHandlerAddress" ;
	
	//Shared attribute of services (Spec, Implem, Instance)
	public static final int SHAREABLE = 3 ;	//Usable by anybody
	public static final int APPLI     = 2 ;	//Usable only from composite that can see the current composite
	public static final int LOCAL     = 1 ;	//Usable only inside current composite
	public static final int PRIVATE   = 0 ;	//Single use (unique Requires, Uses or Wire)
	
	//Clonable attribute of services (Spec, Implem, Instance)
	public static final int TRUE  = 0 ;
	public static final int FALSE = 1 ;
			
	//The entry point in the ASM : its brokers
	public static ASMSpecBroker ASMSpecBroker = null;
	public static ASMImplBroker ASMImplBroker = null;
	public static ASMInstBroker ASMInstBroker = null;

	//The entry point in SAM
	public static SpecificationBroker SAMSpecBroker = null;
	public static ImplementationBroker SAMImplBroker = null;
	public static InstanceBroker SAMInstBroker = null;
	public static DeploymentUnitBroker SAMDUBroker = null;

	private APAMImpl apam ;
	
	public ASM () {
		try {
			ASMSpecBroker = new ASMSpecBrokerImpl () ;
			ASMImplBroker = new ASMImplBrokerImpl () ;
			ASMInstBroker = new ASMInstBrokerImpl () ;

			Machine AM = fr.imag.adele.am.LocalMachine.localMachine ;
			BrokerBroker bb = AM.getBrokerBroker() ;
			SAMSpecBroker = (SpecificationBroker) bb.getBroker(SpecificationBroker.SPECIFICATIONBROKERNAME) ;
			SAMImplBroker = (ImplementationBroker) bb.getBroker(ImplementationBroker.NAME) ;
			SAMInstBroker = (InstanceBroker) bb.getBroker(InstanceBroker.NAME) ;
			SAMDUBroker   = (DeploymentUnitBroker) bb.getBroker(DeploymentUnitBroker.NAME) ;

			apam = new APAMImpl () ;
		} catch (Exception e) {}
	}

	//Keep list of shared (shareable) services
	public static Set<ASMSpec> sharedSpecs = new HashSet <ASMSpec> () ;
	public static Set<ASMImpl> sharedImpls = new HashSet <ASMImpl> () ;
	public static Set<ASMInst> sharedInsts = new HashSet <ASMInst> () ;

	public static Set<ASMInst> getSharedInsts (ASMSpec spec) {
		Set<ASMInst> ret = new HashSet<ASMInst> () ;
		try {
		for (ASMInst inst : sharedInsts) {
				if (inst.getSpec() == spec) {
					ret.add(inst) ;
				}
		}
		return ret ;
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
		return null ;
	}
	
	public static Set<ASMInst> getSharedInsts (ASMImpl impl) {
		Set<ASMInst> ret = new HashSet<ASMInst> () ;
		try {
		for (ASMInst inst : sharedInsts) {
				if (inst.getImpl() == impl) {
					ret.add(inst) ;
				}
		}
		return ret ;
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
		return null ;
	}

	
	public static void addSharedInst (ASMInst inst) {
		sharedInsts.add (inst) ;
	}
	public static void removeSharedInst (ASMInst inst) {
		sharedInsts.remove(inst) ;
	}
	public static void addSharedImpl (ASMImpl impl) {
		sharedImpls.add(impl) ;
	}
	public static void removeSharedImpl (ASMImpl impl) {
		sharedImpls.remove(impl) ;
	}
	public static void addSharedSpec (ASMSpec spec) {
		sharedSpecs.add (spec) ;
	}
	public static void removeSharedSpec (ASMSpec spec) {
		sharedSpecs.remove (spec) ;
	}

}
