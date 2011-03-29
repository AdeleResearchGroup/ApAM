package fr.imag.adele.apam.samAPIImpl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Specification;

public class ASMSpecBrokerImpl implements ASMSpecBroker{
	
	private Set <ASMSpec> specs = new HashSet <ASMSpec> () ;

	@Override
	public void removeSpec (ASMSpec spec) {
		if (specs.contains (spec)) {
			specs.remove (spec) ;
			spec.remove() ;
		}
	}
	
	public void addSpec (ASMSpec spec) {
		specs.add (spec) ;
	}
	
	@Override
	public ASMSpec getSpec(String[] interfaces) {
		
		interfaces = Util.orderInterfaces(interfaces) ;
		for (ASMSpec spec : specs) {
			if (Util.sameInterfaces (spec.getInterfaceNames(), interfaces)) 
					return spec ;
		}
		return null;
	}

	@Override
	public ASMSpec getSpec(String name) {
		
			for (ASMSpec spec : specs) {
				if (spec.getASMName() == null) {
					if (spec.getSamSpec().getName().equals (name)) return spec ;
				} else {
					if (name.equals(spec.getASMName()))
						return spec ;
				}
			}
			return null ;
		}

	@Override
	public Set<ASMSpec> getSpecs() {
		
		return new HashSet<ASMSpec> (specs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<ASMSpec> getSpecs(Filter goal)
			throws  InvalidSyntaxException {
		Set<ASMSpec> ret = new HashSet<ASMSpec> ();
		for (ASMSpec spec : specs) {
			if (goal.match((Dictionary<String, Object>)spec.getProperties())) 
					ret.add(spec) ;
		}
		return ret ;
	}
	@SuppressWarnings("unchecked")
	@Override
	public ASMSpec getSpec(Filter goal) throws InvalidSyntaxException {
		for (ASMSpec spec : specs) {
			if (goal.match((Dictionary<String, Object>)spec.getProperties())) 
					return spec ;
		}
		return null;
	}

	
	@Override
	public Set<ASMSpec> getUses(ASMSpec specification) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ASMSpec addSpec(Composite compo, String name, Specification samSpec, Attributes properties) {
		ASMSpecImpl spec = new ASMSpecImpl (compo, name, samSpec, properties) ;
		specs.add(spec) ;
		return spec ;
	}

	@Override
	public ASMSpec getSpec (Specification samSpec) {
		for (ASMSpec spec : specs) {
			if (spec.getSamSpec() == samSpec)
				return spec ;
		}
		return null ;
	}


	@Override
	public Set<ASMSpec> getUsesRemote(ASMSpec specification){
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Returns *the first* specification that implements the provided interfaces.
     * WARNING : the same interface can be implemented by different specifications, 
     * and a specification may implement more than one interface : the first spec found is returned. 
     * WARNING : convenient only if a single spec provides that interface; otherwise it is non deterministic.
     * 
     * @param interfaceName : the name of the interface of the required specification. 
     * @return the abstract service
     * @throws ConnectionException the connection exception Returns the
     *             ExportedSpecification exported by this Machine that satisfies
     *             the interfaces.
     */    
	@Override
	public ASMSpec getSpecInterf(String interfaceName) {
		for (ASMSpec spec : specs) {
			String[] interfs = spec.getSamSpec().getInterfaceNames() ;
			for (int i = 0; i < interfs.length; i++) {
				if (interfs[i].equals(interfaceName)) return spec ;
			}
		}
		return null;
	}

	 /**
     * Returns the specification with the given sam name. 
     * @param samName the sam name of the specification
     * @return the abstract service
     */   
	@Override
	public ASMSpec getSpecSamName(String samName)  {
		for (ASMSpec spec : specs) {
			if (spec.getSamSpec().getName().equals (samName)) return spec ;
		}
		return null;
	}

	@Override
	public ASMSpec createSpec(Composite compo, String specName, String[] interfaces, Attributes properties) {
		ASMSpec ret = null;
		try {
			if (ASM.SAMSpecBroker.getSpecification(interfaces) != null) {
				ret = addSpec (compo, specName, ASM.SAMSpecBroker.getSpecification(interfaces), properties) ;
			} else {
				ret = new ASMSpecImpl(compo, specName, null, properties) ;
			}
	    } catch (ConnectionException e) {e.printStackTrace();}
		ret.setShared(ASM.shared2Int(properties)) ;
		return ret ;
	}

}
