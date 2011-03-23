package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamClient;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.DynamicManager;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.samAPIImpl.SamImplEventHandler;
import fr.imag.adele.apam.samAPIImpl.SamInstEventHandler;

public class APAMImpl implements Apam, ApamClient, ManagersMng {

	// A single Appli per APAM so far
	private static Composite appli = null;
	private static ASMImpl main = null;

	//int is the priority
	private Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer> () ;
	private List<Manager> managerList = new ArrayList<Manager> () ;
	
	private SamImplEventHandler implHandler ;
	private SamInstEventHandler instHandler ;
	
	public APAMImpl () {
		implHandler = new SamImplEventHandler() ;
		instHandler = new SamInstEventHandler() ;
	}
	
	@Override
	public ASMInst faultWire(ASMInst client, ASMInst lostInstance, String depName, Integer abort) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * The client requires a wire toward an instance resolution of the spec. 
	 * Returns that instance, if found; null otherwise.
	 * APAM  delegates that request to the managers.
	 * The resolution is performed in three steps. 
	 * First APAM ask each manager, in their priority order to ask for a SelectionPath, 
	 * and optionaly for a list of constraints to satisfy. 
	 * Then APAM checks if a shareable instance of “spec” pertains to the current ASM 
	 * and satisfies the constraints. If so that instance is selected. 
	 * Third, APAM asks, in the order of the SelectionPath, each manager 
	 * to provide and instance that is a resolution of “spec” and that satisfies 
	 * the constraints. If so it is returned.
	 * 
	 * If not found returns null. If a manager ask to abort, returns abort = true.
	 * 
	 */
	@Override
	public ASMInst newWire(ASMInst client, ASMSpec spec, String depName, Integer abort) {
		abort = ASM.FALSE ;
		//first step : compute selection path and constraints
		Set<Filter> constraints = new HashSet<Filter> () ;
		Filter thatfilter = null ;
		List<Manager> selectionPath = new ArrayList<Manager>  () ;
		
		if (managerList.size() == 0) {
			System.out.println("No manager available. Cannot resolve " + spec.getName());
			return null ;
		}
		// Call all managers in their priority order
		//Each manager can change the order of managers in the selectionPath, and even remove.
		//It can add itself or not. If no involved it should do nothing.
		for (int i = 0; i < managerList.size(); i++) {
			selectionPath = managerList.get(i).getSelectionPath(client, spec, depName, thatfilter, selectionPath) ;
			if (thatfilter != null) {
				constraints.add(thatfilter);
			}
		}
		
		// second step : look for a sharable instance that satisfies the constraints
		Set<ASMInst> sharable = ASM.getSharedInsts(spec) ;
		try {
			for (ASMInst inst : sharable){
				boolean satisfies = true ;
				for (Filter filter : constraints) {
					if (!filter.match((PropertyImpl)inst.getProperties())) {
						satisfies = false ;
						break ;
					}
				}
				if (satisfies) {
					//accept only if a wire is possible
					if (client.setWire (inst, depName, constraints))
						return inst ;
				}
			}
		} catch (ConnectionException e) {e.printStackTrace();}
		
		//third step : ask each manager in the order
		ASMInst resolved ;
		for (int i = 0; i < managerList.size(); i++) {
			resolved = managerList.get(i).resolve(client, spec, depName, constraints, abort) ;
			if (resolved != null) {
				//accept only if a wire is possible
				if (client.setWire (resolved, depName, constraints))				
					return resolved ;
			}
			if (abort== ASM.TRUE) return null ;
		} 
		return null ;
	}

	@Override
	public ASMInst newWire(ASMInst client, ASMImpl impl, String depName, Integer abort) {
		abort = ASM.FALSE ;
		
		//first step : compute selection path and constraints
		Set<Filter> constraints = new HashSet<Filter> () ;
		Filter thatfilter = null ;
		List<Manager> selectionPath = new ArrayList<Manager>  () ;

		if (managerList.size() == 0) {
			System.out.println("No manager available. Cannot resolve " + impl.getASMName());
			return null ;
		}
		for (int i = 0; i < managerList.size(); i++) {
			selectionPath = managerList.get(i).getSelectionPath(client, impl, depName, thatfilter, selectionPath) ;
			if (thatfilter != null) {
				constraints.add(thatfilter);
			}
		}
		// second pass : look for a shareable instance that satisfies the constraints
		Set<ASMInst> sharable = ASM.getSharedInsts(impl) ;
		try {
			for (ASMInst inst : sharable){
				boolean satisfies = true ;
				for (Filter filter : constraints) {
					if (!filter.match((PropertyImpl)inst.getProperties())) {
						satisfies = false ;
						break ;
					}
				}
				if (satisfies) {
					//accept only if a wire is possible
					if (client.setWire (inst, depName, constraints))
						return inst ;
				}
			}

		} catch (ConnectionException e) { e.printStackTrace();}
		
		//third step : ask each manager in the order
		ASMInst resolved ;
		for (int i = 0; i < managerList.size(); i++) {
			resolved = managerList.get(i).resolve(client, impl, depName, constraints, abort) ;
			if (resolved != null) {
				//accept only if a wire is possible
				if (client.setWire (resolved, depName, constraints))				
					return resolved ;
			}
			if (abort== ASM.TRUE) return null ;
		} 
		return null ;
	}

	@Override
	public void addManager(Manager manager, int priority) {
		if (managerList.size()==0) {
			managerList.add(manager) ;
		} else {
			for (int i = 0; i < managerList.size(); i++) {
				if (priority >= managerList.get(i).getPriority()) {
					managerList.add(i, manager) ;
				}
			}
		}
		managersPrio.put (manager, new Integer (priority)) ;
	}

	@Override
	public List<Manager> getManagers() {
		return new ArrayList<Manager> (managerList);
	}

	@Override
	public void removeManager(Manager manager) {
		managersPrio.remove(manager);
		managerList.remove(manager) ;
	}

	@Override
	public void createAppli(Composite appli, ASMImpl main) {
		this.appli = appli ;
		this.main = main ;
	}

	public void execute () {
		//TODO
	}
	
	@Override
	public Composite createAppli(String compositeName, ASMImpl main, Set <ManagerModel> models) {
		if ( appli != null) {
			System.out.println("Application allready existing");   
			return null ;
		}
		Composite appli = new CompositeImpl (compositeName, models) ;
		this.main = main ; 
		return appli ;
	}

	
	public  Composite getAppli () {
		return appli ;
	}
	public ASMImpl getAppliMain () {
		return main ;
	}

	@Override
	public int getPriority(Manager manager) {
		return managersPrio.get (manager);
	}

	/**
	 * called by an APAM client dependency handler when it initialises.
	 * Since the client in in the middel of its creation, the Sam instance and the ASM inst are not created yet.
	 * We simply record in the instance event handler that this instance will "appear"; 
	 * at that time we will record the cleinet address in a property of that instance ASM.ApamDependencyHandlerAddress
	 * It is only in the ASMInst constructor that the ASM instance will be connected to its handler.
	 */
	@Override
	public void newClientCallBack(String samInstanceName, ApamDependencyHandler client) {
		SamInstEventHandler.addNewApamInstance(samInstanceName, client) ;
	}

	@Override
	public void appearedExpected(ASMImpl impl, DynamicManager manager) {
		SamInstEventHandler.addExpectedImpl(impl, manager) ;
	}

	@Override
	public void appearedExpected(String interf, DynamicManager manager) {
		SamInstEventHandler.addExpectedInterf(interf, manager) ;
	}

	@Override
	public void listenLost(DynamicManager manager) {
		SamInstEventHandler.addLost(manager) ;
	}



}
