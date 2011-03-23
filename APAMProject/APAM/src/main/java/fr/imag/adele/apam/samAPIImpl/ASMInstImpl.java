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
	private static ASMInstBroker myBroker = ASM.ASMInstBroker ;

	private String name ;
	private ASMImpl myImpl ;
	private Composite myComposite ;
	private Instance samInst ;
	private ApamDependencyHandler depHandler ;

	//The known attributes and their default value
	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;

	private Map <ASMInst, Wire> wires = new HashMap <ASMInst, Wire> () ;			//the currently used instances
	private Map <ASMInst, Wire> invWires = new HashMap <ASMInst, Wire> () ;			

	public ASMInstImpl (Composite compo, ASMImpl impl, Properties initialproperties, Instance samInst) {
		this.myImpl = impl ;
		this.myComposite = compo ;
		if (samInst == null) {
			System.out.println("erreur : sam instance cannot be null on ASM instance constructor");
			return ;
		}
		this.samInst = samInst ;
		this.name = samInst.getName () ;
		((ASMInstBrokerImpl)ASM.ASMInstBroker).addInst (this) ;
		//Check if it is an APAM instance
		try {
			ApamDependencyHandler handler = (ApamDependencyHandler)samInst.getProperty(ASM.APAMDEPENDENCYHANDLER);
			if (handler != null) { //it is an Apam instance
				depHandler = handler ;
				handler.SetIdentifier(this) ;
			}
		} catch (ConnectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

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

//	public String getName() {
//		return name ;
//	}


	public Object getServiceObject() throws ConnectionException {
		return samInst.getServiceObject();
	}


	/**
	 * returns the connections towards the service instances actually used.
	 * return only APAM wires.  
	 * for SAM wires the sam instance
	 */
	@Override
	public Set<ASMInst> getWires() {
		return wires.keySet()  ;
	}

	public Set<Filter> getWireConstraint(ASMInst to) {
		if (wires.get(to) == null) return null ;
		return wires.get(to).getConstraints();
	}

	@Override
	public boolean setWire (ASMInst to, String depName, Filter filter) {
		Set<Filter> constraints = new HashSet<Filter> () ;
		constraints.add(filter);
		return setWire (to, depName, constraints) ;
	}

	@Override
	public boolean setWire (ASMInst to, String depName, Set<Filter> constraints) {
		if (wires.get(to) != null) return true ;
		if (!Wire.checkNewWire(this, to)) return false ; 
		Wire wire = new Wire (this, to, depName, constraints);
		wires.put(to, wire) ;
		((ASMInstImpl)to).setInvWire (this, wire) ;
		if (depHandler != null) {
			depHandler.setWire(to, depName) ;
		}
		return true ;
	}

	//Not in the interface
	private void setInvWire (ASMInst from, Wire wire) {
		invWires.put (from, wire) ;
	}

	/**
	 * The removed wire is not considered as lost; the client may still be active. 
	 * The state is not changed.
	 * @param to
	 */
	public void removeWire (ASMInst to) {
		removeWire (to, null) ;
	}

	private void removeWire (ASMInst to, ASMInst newTo) {
		Wire wire = wires.get(to) ;
		if (wire == null) return ;
		wires.remove(to) ;
		((ASMInstImpl)to).removeInvWire (this) ;
		if (depHandler != null) {
			if (newTo == null) {
				depHandler.remWire(to, wire.getDepName()) ;
			} else {
				depHandler.substWire(to, newTo, wire.getDepName()) ;
			}
		}
	}

	private void removeInvWire (ASMInst from) {
		if (invWires.get(from) == null) return ;
		invWires.remove(from) ;
		if (invWires.isEmpty()) { //This instance ins no longer used. Delete it => remove all its wires 
			for (ASMInst dest : wires.keySet() ) {
				removeWire (dest, null) ;
			}
			try {
				myBroker.removeInst(this);
				samInst.delete() ;
			} catch (ConnectionException e) {
				e.printStackTrace();
			} 
		}
	}

	/**
	 * remove from ASM
	 * It deletes the wires, which deletes the isolated used instances, and transitively.
	 * It deleted the invWires, which removes the associated real dependency : 
	 */
	@Override
	public void remove() {
		//The fact the instance is no longer used deletes it. It is done in removeWire.
		for (ASMInst client : invWires.keySet()) {
			((ASMInstImpl)client).removeWire(this, null) ; 
		}
	}

	@Override
	public void substWire(ASMInst oldTo, ASMInst newTo, String depName) {
		new Wire (this, newTo, depName, wires.get(oldTo).getConstraints()) ;
		removeWire (oldTo, newTo) ;
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

	@Override
	public int getClonable() {
		return clonable;
	}

	@Override
	public int getShared() {
		return shared;
	}

	@Override
	public void setClonable(int clonable) {
		if (clonable == ASM.TRUE || clonable == ASM.FALSE) this.clonable = clonable ;
	}

	@Override
	public void setShared(int newShared) {
		if (((newShared < 0) || newShared > myImpl.getShared())) return ; // do not change 
		if (shared == ASM.SHAREABLE && newShared != ASM.SHAREABLE) {
			ASM.removeSharedInst(this) ;
		}
		this.shared = newShared ;
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
	public Set<ASMInst> getClients() {
		return invWires.keySet();
	}

	@Override
	public Wire getWire(ASMInst destInst) {
		return wires.get(destInst);
	}
}
