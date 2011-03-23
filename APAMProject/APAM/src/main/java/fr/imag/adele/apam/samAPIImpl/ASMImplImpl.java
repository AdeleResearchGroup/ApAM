package fr.imag.adele.apam.samAPIImpl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.PID;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;


public class ASMImplImpl extends PropertyImpl implements ASMImpl {

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
	public ASMImplImpl(Composite compo, String implName, ASMSpecImpl spec, Implementation impl, ASMInst inst, Properties prop) {
		this.name = implName;
		this.myComposite = compo ;
		if (spec == null) {
			//Create the spec from SAM spec
			try {
				//TODO use new method getSpecification
				//Specification specification = samImpl.getSpecification();
				Specification specification = samImpl.getSpecification();
				spec = new ASMSpecImpl (compo, specification.getName(), specification, (Properties)null) ;
			} catch (ConnectionException e) { e.printStackTrace(); }
		}
		this.mySpec = spec ;
		if (inst != null) { 
			instances.add((ASMInstImpl)inst) ;
		}
		if (impl == null) {
			System.out.println("Sam Implementation cannot be null when creating an imple");
		}
		samImpl = impl ;
		((ASMImplBrokerImpl)myBroker).addImpl(this) ;
		//initialize properties. A fusion of SAM and APAM values
		if (prop != null && !prop.isEmpty()) {
			Map<String, Object> samProp;
			try {
				samProp = samImpl.getProperties();
				for (Object attr : prop.keySet()) {
					if (!samProp.containsKey((String)attr)) {
						samImpl.setProperty((String)attr, prop.get(attr)) ;
					} else { //valeur differente, pas normal !
						if (prop.get(attr) != samProp.get(attr)) {
							System.out.println("Erreur ! arrtibut " + attr + " different in SAM and init val : "
									+ samProp.get(attr) + ", " + prop.get(attr));
							//TODO raffiner. shared, instantiable etc.
						}
					}	
				}
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * From an implementation, create an instance. 
	 * Creates both the SAM and ASM instances with the same properties.
	 * @throws IllegalArgumentException 
	 * @throws UnsupportedOperationException 
	 * @throws ConnectionException 
	 */
	@Override
	public ASMInst createInst(Properties initialproperties)  {
		Instance samInst = null;
		try {
			samInst = samImplBroker.createInstance(samImpl.getPid(), initialproperties);
		} catch (Exception e) {	e.printStackTrace();}

		ASMInstImpl inst = new ASMInstImpl (myComposite, this, initialproperties, samInst) ;
		instances.add(inst) ;
		return inst ;   
	}

	public ASMSpec getSpec()  {
		return mySpec ;
	}

	public ASMInst getInst(String targetName) {
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
	public ASMInst getInst(){
		if ((instances==null) || (instances.size() == 0)) return null ;
		return (ASMInst) instances.toArray()[0] ;
	}

	public Set<ASMInst> getInsts(Filter query) throws InvalidSyntaxException {
		Set<ASMInst> ret = new HashSet<ASMInst>() ;
		for (ASMInst inst : instances) {
				if (query.match((PropertyImpl)inst))
					ret.add(inst) ;
		}
		return ret;
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
	public Set<ASMSpec> getUses() throws ConnectionException {
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
