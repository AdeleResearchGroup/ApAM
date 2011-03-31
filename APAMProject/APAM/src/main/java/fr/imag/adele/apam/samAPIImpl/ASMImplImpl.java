package fr.imag.adele.apam.samAPIImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.Property;
import fr.imag.adele.am.exception.ConnectionException;
//import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.AttributesImpl;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.broker.ImplementationBroker;


public class ASMImplImpl extends AttributesImpl implements ASMImpl {

	private static ASMImplBroker myBroker = ASM.ASMImplBroker ;
	private static ImplementationBroker samImplBroker = ASM.SAMImplBroker ; ;

	private String name ;
	private ASMSpec mySpec ;
	private Composite myComposite ;
	private Implementation samImpl = null ;

	private Set<ASMInst> instances = new HashSet <ASMInst> ();

	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;

	/**
	 * Instantiate a new service implementation.
	 * 
	 * @param instance the ASM instance
	 * @param name CADSE name
	 *           
	 */                
	public ASMImplImpl(Composite compo, String implName, ASMSpecImpl spec, Implementation impl, Attributes prop) {
		this.name = implName;
		this.myComposite = compo ;
		try {
			this.mySpec = spec ;
			if (impl == null) {
				System.out.println("Sam Implementation cannot be null when creating an imple");
			}
			samImpl = impl ;
			((ASMImplBrokerImpl)myBroker).addImpl(this) ;
			this.setProperties( Util.mergeProperties (prop, impl.getProperties())) ;

		} catch (ConnectionException e) { e.printStackTrace(); }
		compo.addImpl(this) ;
	}	


	/**
	 * From an implementation, create an instance. 
	 * Creates both the SAM and ASM instances with the same properties.
	 * @throws IllegalArgumentException 
	 * @throws UnsupportedOperationException 
	 * @throws ConnectionException 
	 */
	@Override
	public ASMInst createInst(Attributes initialproperties)  {
		Instance samInst = null;
		try {
			samInst = samImplBroker.createInstance(samImpl.getPid(), (Properties)initialproperties);
		} catch (Exception e) {	e.printStackTrace();}

		ASMInstImpl inst = new ASMInstImpl (myComposite, this, initialproperties, samInst) ;
		instances.add(inst) ;
		return inst ;   
	}

	@Override
	public ASMSpec getSpec()  {
		return mySpec ;
	}

	@Override
	public ASMInst getInst(String targetName) {
		if (targetName == null) return null ;
		for (ASMInst inst : instances) {
			if (inst.getASMName().equals(targetName)) return inst ;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Implementation#getInstances()
	 */
	public Set<ASMInst> getInsts(){
		return Collections.unmodifiableSet(instances) ;
		//return new HashSet <ASMInst> (instances) ;
	}

	/**
	 * returns the first instance only.
	 */
	@Override
	public ASMInst getInst(){
		if ((instances==null) || (instances.size() == 0)) return null ;
		return (ASMInst) instances.toArray()[0] ;
	}

	@Override
	public Set<ASMInst> getInsts(Filter query) throws InvalidSyntaxException {
		if (query == null) return getInsts() ;
		Set<ASMInst> ret = new HashSet<ASMInst>() ;
		for (ASMInst inst : instances) {
				if (query.match((AttributesImpl)inst))
					ret.add(inst) ;
		}
		return ret;
	}

	@Override
	public String getASMName() {
		return name;
	}

	public void setASMName (String logicalName) {
		if (logicalName == null || logicalName == "") return ;
		if (name == null) {
			name = logicalName ;
			return ;
		}
		if (!name.equals(logicalName)) {
			System.out.println("changign logical name, from " + name + " to " + logicalName);
			name = logicalName ;
		}
	}
	
	@Override
	public Composite getComposite() {
		return myComposite;
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
		for (ASMInst inst : instances) {
			if (inst.getShared() > newShared) {
				System.out.println("cannot change shared prop of " + getASMName() + " som instances have higher shared prop");
				return ; //do not change anything
			}
		}
		if (((newShared < 0) || newShared > mySpec.getShared())) return ; // do not change 
		this.shared = newShared ;
		if (shared == ASM.SHAREABLE && newShared != ASM.SHAREABLE) {
			ASM.removeSharedImpl(this) ;
		}
	}

	@Override
	public Set<ASMSpec> getUses()  {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * remove from ASM but does not try to delete in SAM. The mapping is still valid.
	 * It deletes all instances.
	 * No change of state. May be selected again later. 
	 */

	@Override
	public void remove() {
		for (ASMInst inst : instances) {
			inst.remove () ;
		}
		myBroker.removeImpl (this) ;
	}

	@Override
	public Implementation getSamImpl () {
		return samImpl ;
	}
	
	@Override
	public boolean isInstantiable() {
		return samImpl.isInstantiator();
	}

	@Override
	public String getSAMName() {
		return samImpl.getName ();
	}

}
