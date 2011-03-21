package fr.imag.adele.apam.samAPIImpl;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ApamDependencyHandler;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.sam.Instance;


public class ASMInstImpl extends PropertyImpl implements ASMInst {

	/** The logger. */
	private static Logger logger = Logger.getLogger(ASMInstImpl.class);
	private static ASMInstBroker myBroker = null ;

	private String name ;
	private ASMImpl myImpl ;
	private Composite myComposite ;
	private Instance samInst ;
	private ApamDependencyHandler depHandler ;

	//The known attributes and their default value
	private int state = ASM.ACTIVE ;
	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;

	private Map <ASMInst, Wire> wires = new HashMap <ASMInst, Wire> () ;			//the currently used instances
	private Map <ASMInst, Wire> invWires = new HashMap <ASMInst, Wire> () ;			
	private Map <ASMInst, Wire> missingWires = new HashMap <ASMInst, Wire> () ;		//wires toward lost instances

	/* WARNING : to call before any other call */
	public static void init () {
		myBroker = ASM.ASMInstBroker ;
	}


	public ASMInstImpl (Composite compo, ASMImpl impl, Properties initialproperties, Instance samInst) {
		this.myImpl = impl ;
		this.myComposite = compo ;
		if (samInst == null) {
			System.out.println("erreur : sam instance cannot be null on ASM instance constructor");
			return ;
		}
		this.samInst = samInst ;
		this.name = samInst.getName () ;

		//initialize properties. A fusion of SAM and APAM values
		if (initialproperties != null && !initialproperties.isEmpty()) {
			Map<String, Object> samProp;
			try {
				samProp = samInst.getProperties();
				for (Object attr : initialproperties.keySet()) {
					if (!samProp.containsKey((String)attr)) {
						samInst.setProperty((String)attr, initialproperties.get(attr)) ;
					} else { //valeur differente, pas normal !
						if (initialproperties.get(attr) != samProp.get(attr)) {
							System.out.println("Erreur ! arttibut " + attr + " different in SAM and init val : "
									+ samProp.get(attr) + ", " + initialproperties.get(attr));
							//TODO raffiner. shared, instantiable etc.
						}
					}	
				}
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Instance#getImplementation()
	 */
	public ASMImpl getImpl() {
		return myImpl ;
	}

	public String getName() {
		return name ;
	}


	public Object getServiceObject() throws ConnectionException {
		return samInst.getServiceObject();
	}


	/**
	 * returns the connections towards the service instances actually used.
	 * return only APAM wires.  
	 * for SAM wires the sam instance
	 */
	public Set<ASMInst> getWires() {
		return wires.keySet()  ;
	}

	public Set<Filter> getWireConstraint(ASMInst to) {
		if (wires.get(to) == null) return null ;
		return wires.get(to).getConstraints();
	}
	
	public boolean setWire (ASMInst to, String depName, Set<Filter> constraints) {
		if (wires.get(to) != null) return true ;
		if (!Wire.checkNewWire(this, to)) return false ; 
		Wire wire = new Wire (this, to, depName, constraints);
		if (to.getState() == ASM.LOST){
			missingWires.put(to, wire) ;
		} else {
			wires.put(to, wire) ;
		}
		((ASMInstImpl)to).addInvWire (wire) ;
		return true ;
	}

	public boolean setWire (ASMInst to, String depName, Filter filter) {
		Set<Filter> constraints = new HashSet<Filter> () ;
		constraints.add(filter);
		return setWire (to, depName, constraints) ;
	}
	/**
	 * A new client uses this instance. Can turn the instance back to active if idle or removed.
	 * @param client
	 */
	private void addInvWire (Wire wire) {
		ASMInst from = wire.getSource() ;
		if ( invWires.get (from) != null) return ;
		invWires.put (from, wire) ;
		if (from.getState() == ASM.ACTIVE && ( state == ASM.IDLE || state == ASM.REMOVED)) {
			setState (ASM.ACTIVE);
		}
	}

	/**
	 * The removed wire is not considered as lost; the client may still be active. 
	 * The state is not changed.
	 * @param inst
	 */
	public void removeWire (ASMInst inst) {
		if (wires.get(inst) == null) return ;
		wires.remove(inst) ;
		((ASMInstImpl)inst).removeInvWire (this) ;
	}


	public void removeInvWire (ASMInst inst) {
		if (wires.get(inst) == null) return ;
		invWires.remove(inst) ;
		if (invWires.isEmpty()) {
			setState(ASM.IDLE) ;
		}
	}

	/**
	 * internal. Not in the interface.
	 * 
	 * From * to removed => deletes all wires and invWires (client => fail, wired idle?)
	 * 
	 * From Active to * => Propagate to all wired
	 * From * to Active => Propagate to all wired 
	 * 
	 */
	private void setState (int newState) {
		if (newState == ASM.REMOVED) {
			for (ASMInst client : invWires.keySet()) {
				((ASMInstImpl)client).removeWire(this) ;
			}
			for (ASMInst wired : wires.keySet()) {
				((ASMInstImpl)wired).removeInvWire(this) ;
			}
			state = newState ;
			return ;
		}
		if (state == ASM.ACTIVE) {
			for (ASMInst wired : wires.keySet()) {
				((ASMInstImpl)wired).inactiveClient(this) ;
			}
			if (newState == ASM.LOST) {
				for (ASMInst client : invWires.keySet()) {
					((ASMInstImpl)client).lostWired(this) ;
				}
			}
			state = newState ;
			return ;
		}
		if (newState == ASM.ACTIVE) {
			for (ASMInst wired : wires.keySet()) {
				((ASMInstImpl)wired).activeClient(this) ;
			}
			for (ASMInst client : invWires.keySet()) {
				((ASMInstImpl)client).foundWired(this) ;
			}
			state = newState ;
			return ;
		}
	}

	private void foundWired (ASMInst wired) {
		missingWires.remove (wired) ;
	}
	
	/**
	 * Object "to" disappeared. Its wire must be moved to the list missingWire.
	 * @param to
	 */
	private void lostWired (ASMInst to) {
		if (wires.get (to) == null) return ;
		missingWires.put (to, wires.get (to)) ;
		wires.remove(to) ;
	}
	/**
	 * Internal.
	 * The client that has a wire toward this instance is no longer in state "active".
	 * turn to idle if no other active client, and propagate. 
	 */
	private void inactiveClient (ASMInst inst) {
		if (invWires.get(inst) == null) return ;
		for (ASMInst client : invWires.keySet()) {
			if (client.getState()== ASM.ACTIVE) return ;
		} //No active clients : turn to Idle
		setState (ASM.IDLE) ;
		for (ASMInst wiredInst : wires.keySet()) { //propagate
			((ASMInstImpl)wiredInst).inactiveClient(this) ;
		}
	}

	/**
	 * Internal.
	 * The client that has a wire toward this instance is back to state "active".
	 * If IDLE turn to active and propagate. 
	 */
	private void activeClient (ASMInst inst) {
		if (invWires.get(inst) == null) return ;
		if (state == ASM.IDLE) {
			// if (samInst.exist())
			setState (ASM.ACTIVE) ;
			for (ASMInst wiredInst : wires.keySet()) {
				((ASMInstImpl)wiredInst).activeClient(this) ;
			}
		}
	}

	public void delete() throws UnsupportedOperationException,
	ConnectionException {
		samInst.delete() ;
		remove ();
	}

	public ASMSpec getSpec() throws ConnectionException {
		return myImpl.getSpec() ;
	}

	@Override
	public String getASMName() {
		return name;
	}

	@Override
	public Composite getComposite() {
		return myComposite;
	}

	@Override
	public Instance getSAMInst() {
		return samInst;
	}

	/**
	 * remove from ASM but does not try to delete in SAM. The mapping is still valid.
	 * It deletes the wires, and turns to "idle" the isolated instances, and transitively.
	 * It deleted the invWires, which turns the callers in the "fault" mode : 
	 * 		next call will try to resolve toward another instance. 
	 */
	@Override
	public void remove() {
		state = ASM.REMOVED ;
		for (ASMInst instance : wires.keySet()) {
			((ASMInstImpl)instance).removeInvWire (this) ; //May become idle if no other invWire
		}
		wires.clear() ;
		for (ASMInst instance : invWires.keySet()) {
			((ASMInstImpl)instance).removeWire(this) ; //The real instance should be called to be sure this wire is removed
		}
		invWires.clear () ;
	}

	@Override
	public void ungetService() throws ConnectionException {
//		for (ASMInst inst : wires.keySet()) {
//			removeWire(inst) ;
//		}
		samInst.ungetService() ;
	}

	@Override
	public int getClonable() {
		return clonable;
	}

	@Override
	public int getShared() {
		return shared;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public void setClonable(int clonable) {
		if (clonable == ASM.TRUE || clonable == ASM.FALSE) this.clonable = clonable ;
	}

	@Override
	public void setShared(int shared) {
		if ((shared >=0) && (shared <= myImpl.getShared())) this.shared = shared ;
	}

	@Override
	public boolean match(Filter goal)  {
		try {
		return goal.match((PropertyImpl)getProperties());
		} catch (Exception e) {}
		return false ;
	}

	@Override
	public ApamDependencyHandler getDependencyHandler() {
		return depHandler ;
	}

	@Override
	public void setDependencyHandler(ApamDependencyHandler handler) {
		depHandler = handler ;
	}

	@Override
	public void lost() {
		setState (ASM.LOST) ;
	}

	@Override
	public Set<ASMInst> getClients() {
		return invWires.keySet();
	}

	@Override
	public Wire getWire(ASMInst destInst) {
		return wires.get(destInst);
	}

}
