package fr.imag.adele.apam.samAPIImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.am.MachineID;
import fr.imag.adele.am.eventing.AMEvent;
import fr.imag.adele.am.eventing.AMEventingHandler;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.sam.ImplPID;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.InstPID;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.PID;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.event.EventProperty;

public class SamInstEventHandler implements AMEventingHandler {

	//The managers are waiting for the apparition of an instance of the ASMImpl or implementing the interface
	//In both case, no ASMInst is created.
	static Map<ASMImpl, Set<DynamicManager>> expectedImpls      = new HashMap <ASMImpl, Set<DynamicManager>> () ;
	static Map<String,  Set<DynamicManager>> expectedInterfaces = new HashMap <String,  Set<DynamicManager>> () ;

	//registers the managers that are interested in services that disappear.
	static Set<DynamicManager> listenLost = new HashSet <DynamicManager> () ;

	//contains the apam instance that registered to APAM but not yet created in ASM
	static Map<String, NewInstance> newApamInstance = new HashMap<String, NewInstance> () ;

	static public SamInstEventHandler theInstHandler ;
	public SamInstEventHandler () {
		theInstHandler = this ;
	}

	private class NewInstance {
		ApamDependencyHandler handler;
		String implName = null; 
		String specName = null; 

		public NewInstance (ApamDependencyHandler handler, String implName, String specName) {
			this.handler  = handler;
			this.implName = implName;
			this.specName = specName ;
		}
	}

	public  synchronized void addNewApamInstance (String samName, ApamDependencyHandler handler, String implName, String specName) {
		newApamInstance.put (samName, new NewInstance (handler, implName, specName) ) ;
	}

	public static synchronized void addExpectedImpl (ASMImpl impl, DynamicManager manager) {
		Set<DynamicManager> mans = expectedImpls.get(impl) ;
		if (mans == null) {
			mans = new HashSet<DynamicManager> () ;
			mans.add(manager) ;
			expectedImpls.put (impl, mans);
		} else {
			mans.add(manager) ;
		}
	}

	public static synchronized void addExpectedInterf (String interf, DynamicManager manager) {
		Set<DynamicManager> mans = expectedInterfaces.get(interf) ;
		if (mans == null) {
			mans = new HashSet<DynamicManager> () ;
			mans.add(manager) ;
			expectedInterfaces.put (interf, mans);
		} else {
			mans.add(manager) ;
		}
	}

	static public synchronized void addLost (DynamicManager manager) {
		listenLost.add(manager) ;
	}

	@Override
	public Query getQuery() throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineID getTargetedMachineID() throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineID getMachineID() {
		// TODO Auto-generated method stub
		return null;
	}

	//executed when a new implementation is detected by SAM. Check if it is an expected impl.
	@Override
	public synchronized void receive(AMEvent amEvent) throws ConnectionException {
		if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_ARRIVAL)){
			PID thePid = (PID)amEvent.getProperty(EventProperty.PID) ;
			if (thePid.getType() != PID.INSTANCE) return ;
			InstPID instPid = (InstPID)amEvent.getProperty(EventProperty.PID) ;
			Instance samInst = ASM.SAMInstBroker.getInstance(instPid) ;
			String samName = samInst.getName();

			if (newApamInstance.containsKey(samName)) { //It is an APAM instance either under creation, or auto appear
				NewInstance inst = newApamInstance.get(samName) ;
				samInst.setProperty(ASM.APAMDEPENDENCYHANDLER, inst.handler) ;
				samInst.setProperty(ASM.APAMSPECNAME, inst.specName) ;
				samInst.setProperty(ASM.APAMIMPLNAME, inst.implName) ;
				newApamInstance.remove(samName) ;
			} 

			Implementation samImpl = samInst.getImplementation() ;
			ASMImpl impl = ASM.ASMImplBroker.getImpl (samImpl) ;
			if (expectedImpls.keySet().contains (impl)) {
				for (DynamicManager manager : expectedImpls.get(impl)) {
					manager.appeared(samInst, impl) ;
				}
				expectedImpls.remove(impl);
				return ;
			} 
			Specification samSpec = samInst.getSpecification() ;
			String[] interfs = samSpec.getInterfaceNames() ;
			for (int i = 0; i < interfs.length; i++) {
				if (expectedInterfaces.get (interfs[i]) != null) {
					for (DynamicManager manager : expectedInterfaces.get(interfs[i])) {
						manager.appeared(samInst, interfs[i]) ;
					}
					expectedInterfaces.remove(interfs[i]);
				}
				return ;
			}
		}
		if (amEvent.getProperty(EventProperty.TYPE).equals(EventProperty.TYPE_DEPARTURE)){
			PID thePid = (PID)amEvent.getProperty(EventProperty.PID) ;
			if (thePid.getType() != PID.INSTANCE) return ;
			InstPID instPid = (InstPID)amEvent.getProperty(EventProperty.PID) ;
			Instance samInst = ASM.SAMInstBroker.getInstance(instPid) ;

			ASMInst inst = ASM.ASMInstBroker.getInst (samInst) ;
			if (inst == null) return ;
			//set state lost to inst and propagates. In fact deletes that instance.
			inst.remove () ;
			ASMInst newInst = null;
			ASMInst temp ;
			//notifies interested managers
			if (listenLost.contains (inst)) {
				for (DynamicManager manager : listenLost) {
					temp = manager.lostInst(inst) ;
					if (temp != null) newInst = temp ;
				}
			} 
			// Change the dependency for all APAM specific clients
			ApamDependencyHandler handler ;
			for (ASMInst client : inst.getClients()) {
				handler = client.getDependencyHandler() ;
				if (handler != null) { // An apam client
					if (newInst != null) { //should substitute old to new instance in all clients.
						handler.substWire (inst, newInst, client.getWire(inst).getDepName() ) ;
					} else {
						handler.remWire (inst, client.getWire(inst).getDepName()) ;
					}
				}
			}
		}
	}

}

//	public boolean isExpected (String implName) {
//		return expectedImpls.contains(implName) ;
//	} 

//IN DYNAMAN
//
//	public Instance getinstance (String expected) throws ConnectionException {
//
//		//if allready here
//		Instance instance = samInstBroker.getInstance(expected);
//		if (instance != null)
//			return instance;
//
//		//not yet here. Wait for it.
//		synchronized(this) {
//			expectedInsts.add(expected);
//			try {
//				while (expectedInsts.contains(expected)) {
//					this.wait();
//				}
//				//The expected impl arrived.
//				instance = samInstBroker.getInstance(expected);
//				if (instance == null) //should never occur
//					System.out.println("wake up but imlementation is not present "+expected);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//
//		return instance;
//	}

