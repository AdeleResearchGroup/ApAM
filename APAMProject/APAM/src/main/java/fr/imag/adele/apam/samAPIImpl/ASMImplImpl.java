package fr.imag.adele.apam.samAPIImpl;

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

	private static ASMImplBroker myBroker = null ;
	private static ASMInstBroker instBroker = null ;
	private static ImplementationBroker samImplBroker = null ;
	private static InstanceBroker samInstBroker = null ;

	private String name ;
	private ASMSpec mySpec ;
	private Composite myComposite ;
	private Implementation samImpl = null ;
	//    private Map<String, Object> properties ;
	private Set<ASMInstImpl> instances = new HashSet <ASMInstImpl> ();

//	private int state = ASM.ACTIVE ;
	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;

	//	private Set<ASMInst> uses = new HashSet <ASMInst> () ;
	//	private Set<ASMInst> invUses = new HashSet <ASMInst> () ;

	public static void init () {
		myBroker = ASM.ASMImplBroker ;
		instBroker = ASM.ASMInstBroker ;
		samImplBroker = ASM.SAMImplBroker ;
		samInstBroker = ASM.SAMInstBroker ;
	}

	/**
	 * Instantiate a new service implementation.
	 * 
	 * @param instance the ASM instance
	 * @param name CADSE name
	 *           
	 */                
	public ASMImplImpl(Composite compo, String name, ASMSpecImpl spec, Implementation impl, ASMInst inst, Properties prop) {
		this.name = name;
		this.myComposite = compo ;
		if (spec == null) {
			try {
				Specification specification = (Specification)samImpl.getSpecifications().toArray()[0] ;
				spec = new ASMSpecImpl (compo, specification.getName(), 
						specification, (Properties)null) ;
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
		this.mySpec = (ASMSpec)spec ;
		if (inst != null) { 
			instances.add((ASMInstImpl)inst) ;
		}
		if (impl == null) {
			System.out.println("Sam Implementation cannot be null when creating an imple");
		}
		samImpl = impl ;

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

	public Implementation getSamImpl () {
		return samImpl ;
	}

	public ASMImplImpl(Composite compo, String name, ASMSpecImpl spec, Implementation impl) {
		this.name = name;
		this.myComposite = compo ;
		if (spec == null) {
			try {
				Specification specification = (Specification)samImpl.getSpecifications().toArray()[0] ;
				spec = new ASMSpecImpl (compo, specification.getName(), 
						specification, null) ;
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
		}
		this.mySpec = (ASMSpec)spec ;
		if (impl == null) {
			System.out.println("Sam Implementation cannot be null when creating an imple");
		}
		samImpl   = impl ;
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
		} catch (ConnectionException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		ASMInstImpl inst = new ASMInstImpl (myComposite, this, initialproperties, samInst) ;
		instances.add(inst) ;
		return inst ;   
	}

	public ASMSpec getSpec()  {
		return mySpec ;
	}

//	/*
//	 * (non-Javadoc)
//	 * @see fr.imag.adele.sam.Implementation#getImplementationClass()
//	 */
//	public String getImplementationClass() {
//		return samImpl.getPid().getId();
//	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Implementation#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Implementation#getPid()
	 */
	public PID getPid() {
		return samImpl.getPid() ;
	}

	//    public Set<Implementation> getWires () {
	//    	Set<Implementation> ret = new HashSet <Implementation> ();
	//    	try {
	//    		Instance inst = getInstance () ;
	//    		for (Instance instreq : inst.getWires()) {
	//    			ret.add(instreq.getImplementation()) ;
	//    		}
	//    	} catch (ConnectionException e) {} ; // impossible
	//    	return ret;
	//    }
	//
	//    public Set<Specification> getUses() {
	//    	Set<Specification> ret = new HashSet <Specification> ();
	//    	for (ASMInstImpl inst : instances) {
	//			ret.addAll(inst.getRequires ()) ;
	//		}
	//        return ret;
	//    }

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Implementation#getInstance(java.lang.String)
	 */
	public ASMInst getInst(String targetName) {
		for (ASMInst inst : instances) {
			if (inst.getName().equals(targetName)) return inst ;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Implementation#getInstances()
	 */
	public Set<ASMInst> getInsts(){
		return new HashSet <ASMInst> (instances) ;
	}

	/**
	 * returns the first instance only.
	 */
	public ASMInst getInste(){
		if ((instances!=null) && (instances.size() > 0)) return null ;
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

	public boolean isInstantiator() {
		return true;
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

//	@Override
//	public int getState() {
//		return state;
//	}
//
//	/**
//	 * internal. Not in the interface.
//	 * @param state
//	 */
//	private void setState (int state) {
//		if (state >= 0 && state <= 3) this.state = state ;
//		else {
//			System.out.println("erreur. invalid state: " + state );
//		}
//	}


	@Override
	public void setClonable(int clonable) {
		if (clonable == ASM.TRUE || clonable == ASM.FALSE) this.clonable = clonable ;
	}

	@Override
	public void setShared(int shared) {
		if ((shared >=0) && (shared <= mySpec.getShared())) this.shared = shared ;
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
	}

	@Override
	public String getImplClass() {
		return samImpl.getImplementationClass() ;
	}

	@Override
	public boolean isInstantiable() {
		return samImpl.isInstantiator();
	}

	@Override
	public ASMInst getInst() throws ConnectionException {
		if (instances.size() > 0)
			return (ASMInst)instances.toArray()[0] ;
		return null;
	}

//	@Override
//	public Set<ASMInst> getInsts() throws ConnectionException {
//		return new HashSet<ASMInst> (instances) ;
//	}
//
//	@Override
//	public Set<ASMInst> getInsts(Filter goal) throws ConnectionException,
//			InvalidSyntaxException {
//		Set<ASMInst> ret = new HashSet<ASMInst> () ;
//		for (ASMInst inst : instances) {
//			if (goal.match(inst.getDictionary()))
//					ret.add (inst) ;
//		}
//		return ret;
//	}
}
