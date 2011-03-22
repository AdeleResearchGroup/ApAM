package fr.imag.adele.apam.samAPIImpl;

import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Specification ;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.ipojo.util.LDAP;

import java.rmi.RemoteException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.LocalMachine;
import fr.imag.adele.am.Machine;
import fr.imag.adele.am.Property;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMInstBroker;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Composite;


public class ASMSpecImpl extends PropertyImpl implements ASMSpec{

	private String name ;
    private Composite myComposite ;
    private Specification samSpec = null ;
    private Set<ASMImplImpl> implementations = new HashSet <ASMImplImpl> ();
    
	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;
	
	private static Logger logger = Logger.getLogger(ASMSpecImpl.class);
	
	public ASMSpecImpl  (Composite compo, String name, Specification samSpec, Properties prop) {
		this.myComposite = compo ;
		this.name = name ;
		this.samSpec = samSpec ;
		
		//initialize properties. A fusion of SAM and APAM values
		if (prop != null && !prop.isEmpty()) {
			Map<String, Object> samProp;
			try {
				samProp = samSpec.getProperties();
				for (Object attr : prop.keySet()) {
					if (!samProp.containsKey((String)attr)) {
						samSpec.setProperty((String)attr, prop.get(attr)) ;
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

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Specification#getASMImpl(java.lang.String)
	 */
	@Override
	public ASMImpl getImpl(String name) {
		for (ASMImplImpl impl : implementations) {
			if (impl.getName().equals (name)) return impl ;
		}
		return null ;
	}		

	@Override
	public Set<ASMImpl> getImpls(Filter filter)
	throws InvalidSyntaxException, ConnectionException {
		Set<ASMImpl> ret = new HashSet<ASMImpl> () ;
		for (ASMImplImpl impl : implementations) {
			if (filter.match((Dictionary<String,Object>)impl.getProperties())) {
				ret.add(impl);
			}
		}
		return ret ;
	}


	@Override
	public String[] getInterfaceNames() throws ConnectionException {
		 String interf = samSpec.getInterfaceName () ;
		 String [] ret = {interf} ; ;
		 return ret ;
//		return samSpec.getInterfaceNames();
	}

	@Override
	public Class[] getInterfaces() throws ConnectionException {
		Class [] ret = { samSpec.getInterface() } ;
		return ret ;
//		return samSpec.getInterfaces() ; 
	}

	@Override
	public String getName() {
		return name ; 
	}

	@Override
	public Set<ASMSpec> getUses()  {
		return null;
	}


	@Override
	public String toString() {
		return "SPECIFICATION: " + name ;
	}
	
	@Override	
	public Map<String, Object> getProperties() throws ConnectionException  {
		return samSpec.getProperties() ;
	}
	
	@SuppressWarnings("unchecked")
	public Dictionary<String, Object> getDictionary() 	throws ConnectionException {
		return (Dictionary<String, Object>)samSpec.getProperties() ;
	}


	@Override
	public void removeProperty(String key) throws ConnectionException  {
		samSpec.removeProperty(key) ;
	}

	@Override
	public void setProperties(Map<String, Object> properties) throws ConnectionException {
		samSpec.setProperties(properties) ;
	}

	@Override
	public Object getProperty(String key) throws ConnectionException  {
		return samSpec.getProperty(key) ;
	}

	@Override
	public void setProperty(String key, Object value) throws ConnectionException {
		samSpec.setProperty	(key, value) ;
	}

	@Override
	public String getASMName() {
		return name;
	}

	@Override
	public int getClonable() {
		return clonable;
	}

	@Override
	public Composite getComposite() {
		return myComposite;
	}

	@Override
	public Specification getSamSpec() {
		return samSpec;
	}

	@Override
	public int getShared() {
		return shared;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClonable(int clonable) {
		this.clonable = clonable ;
	}

	@Override
	public void setShared(int newShared) {
		if (shared == newShared) return ;
		if (newShared > this.shared) {
			System.out.println("cannot have shared value greated than specification");
			return ;
		}
		if (shared == ASM.SHAREABLE) {
			ASM.removeSharedSpec (this) ;
			shared = newShared ;
		}
	}

	@Override
	public Set<ASMImpl> getImpls() throws ConnectionException {
		return new HashSet<ASMImpl> (implementations);
	}

}