package fr.imag.adele.apam.samAPIImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.Property;
import fr.imag.adele.am.exception.ConnectionException;
//import fr.imag.adele.am.impl.PropertyImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.ApamProperty;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Specification;


public class ASMSpecImpl extends ApamProperty implements ASMSpec{

	private String name ;
	private Composite myComposite ;
	private Specification samSpec = null ;
	private Set<ASMImpl> implementations = new HashSet <ASMImpl> ();

	private int shared = ASM.SHAREABLE ;
	private int clonable = ASM.TRUE ;

//	private static Logger logger = Logger.getLogger(ASMSpecImpl.class);

	public void setASMName (String logicalName) {
		if (logicalName == null || logicalName == "") return ;
		if (name == null) {
			name = logicalName ;
			return ;
		}
		if (!name.equals(logicalName)) {
			System.out.println("changing logical name, from " + name + " to " + logicalName);
			name = logicalName ;
		}
	}

	
	public ASMSpecImpl  (Composite compo, String specName, Specification samSpec, Attributes props) {
		this.myComposite = compo ;
		this.name = specName ;		//may be null
		this.samSpec = samSpec ; 	//may be null
		((ASMSpecBrokerImpl)ASM.ASMSpecBroker).addSpec (this) ;		
		try {
			//initialize properties. A fusion of SAM and APAM values
			if (samSpec != null) {
				this.setProperties(Util.mergeProperties (props, samSpec.getProperties())) ;
				return ;
			} else this.setProperties(props.getProperties()) ;
		} catch (ConnectionException e) {
			e.printStackTrace();
		}
		compo.addSpec(this) ;
	}



	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.sam.Specification#getASMImpl(java.lang.String)
	 */
	@Override
	public ASMImpl getImpl(String name) {
		for (ASMImpl impl : implementations) {
			if (impl.getASMName().equals (name)) return impl ;
		}
		return null ;
	}		

	@Override
	public Set<ASMImpl> getImpls(Filter filter)
	throws InvalidSyntaxException {
		Set<ASMImpl> ret = new HashSet<ASMImpl> () ;
		for (ASMImpl impl : implementations) {
			if (filter.match((ApamProperty)impl.getProperties())) {
				ret.add(impl);
			}
		}
		return ret ;
	}


	@Override
	public String[] getInterfaceNames()  {
		return samSpec.getInterfaceNames();
	}

	@Override
	public Set<ASMSpec> getUses()  {
		return null;
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
		for (ASMImpl impl : implementations) {
			if (impl.getShared() > newShared) {
				System.out.println("cannot change shared prop of " + getASMName() + " som impl have higher shared prop");
				return ; //do not change anything
			}
		}
		if (newShared <= 0) return ;
		if (shared == ASM.SHAREABLE && newShared != ASM.SHAREABLE) {
			ASM.removeSharedSpec(this) ;
		}
		this.shared = newShared ;
	}

	@Override
	public Set<ASMImpl> getImpls()  {
		return Collections.unmodifiableSet(implementations);
	}

	@Override
	public String getSAMName() {
		return samSpec.getName();
	}
	
	public void setSamSpec (Specification samSpec) {
		this.samSpec = samSpec ;
	}

}