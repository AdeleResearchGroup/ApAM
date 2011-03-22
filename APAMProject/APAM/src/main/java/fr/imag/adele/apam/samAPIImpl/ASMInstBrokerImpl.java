package fr.imag.adele.apam.samAPIImpl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.LocalMachine;
import fr.imag.adele.am.Machine;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.eventing.EventingEngine;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.event.EventProperty;

public class ASMInstBrokerImpl implements ASMInstBroker {

//	private Logger logger = Logger.getLogger(ASMInstBrokerImpl.class);
//	private static final InstanceBroker samInstBroker = ASM.SAMInstBroker ;

	private static final ASMImplBroker implBroker = ASM.ASMImplBroker ;

	private Set<ASMInstImpl> instances = new HashSet< ASMInstImpl> () ;
	
	
	//EVENTS
	private SamInstEventHandler handler ;

	public void init () {
		try {
			Machine machine = LocalMachine.localMachine;
			EventingEngine eventingEngine = machine.getEventingEngine();
			handler = new SamInstEventHandler();
			eventingEngine.subscribe(handler, EventProperty.TOPIC_INSTANCE);
		} catch (Exception e) {}
	}

	public void stopSubscribe (AMEventingHandler handler) {
		try {
		Machine machine = LocalMachine.localMachine;
		EventingEngine eventingEngine = machine.getEventingEngine();
		eventingEngine.unsubscribe(handler, EventProperty.TOPIC_INSTANCE) ;
		} catch (Exception e) {}
	}


	@Override
	public ASMInst getInst(String instName)
	throws ConnectionException {
		for (ASMInst inst : instances) {
			if (inst.getName().equals(instName)) {
				return  inst;
			}
		}
		return null ;
	}
// End EVENTS

	@Override
	public Set<ASMInst> getInsts() throws ConnectionException {
		return  new HashSet<ASMInst> (instances) ;
	}

	@Override
	public Set<ASMInst> getInsts(ASMSpec spec, Filter goal) throws ConnectionException, InvalidSyntaxException {
		Set<ASMInst> ret = new HashSet<ASMInst> ();
		for (ASMInst inst : instances) {
			if ((inst.getSpec() == spec) && goal.match((PropertyImpl)inst.getProperties())) 
				ret.add(inst) ;
		}
		return ret ;
	}

	@Override
	public Set<ASMInst> getInsts(Filter goal)
	throws ConnectionException, InvalidSyntaxException {
		Set<ASMInst> ret = new HashSet<ASMInst> ();
		for (ASMInst inst : instances) {
			if (goal.match((PropertyImpl)inst.getProperties())) 
				ret.add(inst) ;
		}
		return ret ;
	}

	@Override
	public ASMInst addInst(Composite compo, Instance samInst)  {
		ASMImpl impl = null ;
		try {
			impl = ASM.ASMImplBroker.getImpl(samInst.getImplementation()) ;
			if (impl == null) { // create the implem also
				if (compo == null) {
					System.out.println("No implementation for the instance, and composite not provided");
					return null ;
				}
				impl = implBroker.addImpl (compo, samInst.getImplementation().getName(), samInst.getImplementation()) ;
			}

		ASMInstImpl newInst  = new ASMInstImpl (compo, impl, null, samInst) ;
		instances.add (newInst) ;
		return newInst;
		}
		catch (ConnectionException e) {
			e.printStackTrace();
		}
		return null ;
	}

	@Override
	public ASMInst getInst(Instance samInst) {
		for (ASMInst inst : instances) {
			if (inst.getSAMInst() == samInst) 
				return inst ;
		}
		return null;
	}

	@Override
	public void removeInst(ASMInst inst) throws ConnectionException {
		inst.remove();
		instances.remove(inst) ;
	}
}
