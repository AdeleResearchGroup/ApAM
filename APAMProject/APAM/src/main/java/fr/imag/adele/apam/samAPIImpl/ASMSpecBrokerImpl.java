package fr.imag.adele.apam.samAPIImpl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import util.Util;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.ASMSpecBroker;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.sam.Specification;

public class ASMSpecBrokerImpl implements ASMSpecBroker{
	
	
//	private Logger logger = Logger.getLogger(OSGiInstanceBroker.class);
//	private static final InstanceBroker samInstBroker = ASM.SAMInstBroker ;
//	private static final ImplementationBroker samImplBroker = ASM.SAMImplBroker ;
//	private static final SpecificationBroker samSpecBroker = ASM.SAMSpecBroker ;
//	private static final ASMInstBroker instBroker = ASM.ASMInstBroker ;
//	private static final ASMImplBroker implBroker = ASM.ASMImplBroker ;
//	private static final ASMSpecBroker specBroker = ASM.ASMSpecBroker ;
//	private static final String name = "ASMSpecificationBroker" ;
//	private Map<SpecPID, ASMSpecImpl> PID2Spec = new ConcurrentHashMap<SpecPID, ASMSpecImpl> () ;

	private Set <ASMSpec> Specs = new HashSet <ASMSpec> () ;


	@Override
	public ASMSpec getSpec(String[] interfaces)
			throws ConnectionException {
		
		interfaces = Util.orderInterfaces(interfaces) ;
		for (ASMSpec spec : Specs) {
			if (Util.sameInterfaces (spec.getInterfaceNames(), interfaces)) 
					return spec ;
		}
		return null;
	}

	@Override
	public ASMSpec getSpec(String name)
			throws ConnectionException {
		for (ASMSpec spec : Specs) {
			if (spec.getName().equals(name)) 
				return spec ;
		}
		return null;
	}

	@Override
	public Set<ASMSpec> getSpecs()
			throws ConnectionException {
		
		return new HashSet<ASMSpec> (Specs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<ASMSpec> getSpecs(Filter goal)
			throws ConnectionException, InvalidSyntaxException {
		Set<ASMSpec> ret = new HashSet<ASMSpec> ();
		for (ASMSpec spec : Specs) {
			if (goal.match((Dictionary<String, Object>)spec.getProperties())) 
					ret.add(spec) ;
		}
		return ret ;
	}
	@SuppressWarnings("unchecked")
	@Override
	public ASMSpec getSpec(Filter goal) throws ConnectionException,
			InvalidSyntaxException {
		for (ASMSpec spec : Specs) {
			if (goal.match((Dictionary<String, Object>)spec.getProperties())) 
					return spec ;
		}
		return null;
	}

	
	@Override
	public Set<ASMSpec> getUses(ASMSpec specification)
			throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ASMSpec addSpec(Composite compo, String name, Specification samSpec) {
		ASMSpecImpl spec = new ASMSpecImpl (compo, name, samSpec, null) ;
		Specs.add(spec) ;
		return spec ;
	}

	@Override
	public ASMSpec getSpec (Specification samSpec) {
		for (ASMSpec spec : Specs) {
			if (spec.getSamSpec() == samSpec)
				return spec ;
		}
		return null ;
	}


	@Override
	public Set<ASMSpec> getUsesRemote(ASMSpec specification)
			throws ConnectionException {
		// TODO Auto-generated method stub
		return null;
	}

}
