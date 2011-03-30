package fr.imag.adele.samMan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.Machine;
import fr.imag.adele.am.broker.BrokerBroker;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.am.query.QueryLDAPImpl;
import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class SamMan implements Manager {

	//The entry point in SAM
	public static SpecificationBroker SAMSpecBroker = null;
	public static ImplementationBroker SAMImplBroker = null;
	public static InstanceBroker SAMInstBroker = null;
	public static DeploymentUnitBroker SAMDUBroker = null;

	public SamMan () {
		try {
			Machine AM = fr.imag.adele.am.LocalMachine.localMachine ;
			BrokerBroker bb = AM.getBrokerBroker() ;
			SAMSpecBroker = (SpecificationBroker) bb.getBroker(SpecificationBroker.SPECIFICATIONBROKERNAME) ;
			SAMImplBroker = (ImplementationBroker) bb.getBroker(ImplementationBroker.NAME) ;
			SAMInstBroker = (InstanceBroker) bb.getBroker(InstanceBroker.NAME) ;
			SAMDUBroker   = (DeploymentUnitBroker) bb.getBroker(DeploymentUnitBroker.NAME) ;
		} catch (Exception e) {}
	}


	// Must read the opportunist model and build the list of opportunist specs.
	//If empty, all is supposed to be opportunist ??? 
	Set<String> opportunismNames = new HashSet<String> () ;
	Set<Filter> filters = new HashSet<Filter> () ;


	private boolean opportunistSpec (String specName) {

		return true ;
	}

	private boolean opportunistImpl (String implName) {
		return opportunismNames.contains (implName) ;
	}

	static public String ANDLDAP(String... params) {
		StringBuilder sb = new StringBuilder("(&");
		for (String p : params) {
			sb.append(p);
		}
		sb.append(")");
		return sb.toString();
	}

	private Filter buildFilter (Set<Filter> filters) {
		if (filters == null || filters.size() == 0) return null ;
		String ldap = null;
		for (Filter f : filters) {
			if (ldap == null) {
				ldap = f.toString() ;
			}
			else {
				ldap = ANDLDAP(ldap, f.toString()) ;
			}
		}
		Filter ret = null ;
		try {
			ret = org.osgi.framework.FrameworkUtil.createFilter (ldap);
		} catch (InvalidSyntaxException e) {
			System.out.print("Invalid filters : ");
			for (Filter f : filters) {
				System.out.println("   " + f.toString()); ;
			}
			e.printStackTrace();
		} 
		return ret ;
	}

	@Override
	public List<Manager> getSelectionPathSpec (ASMInst from, String interfaceName, String specName, String depName, Filter filter, 
			List<Manager> involved) {
		if (opportunistSpec(specName)) {
			involved.add(this) ;
			return involved ;
		}
		return null ;
	}

	@Override
	public List<Manager> getSelectionPathImpl (ASMInst from, String samImplName, String implName, String depName, Filter filter, 
			List<Manager> involved) {
		if (opportunistImpl(implName)) {
			involved.add(this) ;
			return involved ;
		}
		return null ;
	}

	@Override
	public ASMInst resolveSpec (ASMInst from, String interfaceName, String specName, String depName, Set<Filter> constraints) {
		try {
			Query query = null;
			Filter filter ;
			ASMSpec spec ;
			Set<Instance> samInsts ;

			if (constraints != null && constraints.size() >0) {
				filter = buildFilter (constraints);
				query = new QueryLDAPImpl(filter.toString());
			}

			if ((specName != null) && (ASM.ASMSpecBroker.getSpec(specName) != null)) {
				spec = ASM.ASMSpecBroker.getSpec(specName) ;
			} else {
				spec = ASM.ASMSpecBroker.getSpecInterf(interfaceName) ;
			}
			Instance theInstance = null ;
			if (spec == null) { // No ASM spec known.
				samInsts = SAMInstBroker.getInstances () ;
				for (Instance instance : samInsts) {
					Specification samSpec = instance.getSpecification();
					String [] interfs = samSpec.getInterfaceNames() ;
					for (int i = 0; i < interfs.length; i++) {
						if (interfs[i].equals(interfaceName)) {
							theInstance = instance ;
						}
					}
				}
			} else { //We know the Sam specification
				samInsts = SAMInstBroker.getInstances(spec.getSamSpec(), query ) ;
				if (samInsts != null && samInsts.size()>0) {
					theInstance = (Instance)samInsts.toArray()[0] ;
				}
			}
			if (theInstance == null) return null ;
			if (ASM.ASMInstBroker.getInst(theInstance) != null) {
				return ASM.ASMInstBroker.getInst(theInstance) ;
			}
			return ASM.ASMInstBroker.addInst(from.getComposite(), theInstance, null, specName, null) ;
		} catch (Exception e) {} ;
		return null;
	}

	@Override
	public ASMInst resolveImpl (ASMInst from, String samImplName, String implName, String depName, Set<Filter> constraints) {
		try {
			Query query = null;
			Filter filter ;
			ASMImpl impl ;
			Set<Instance> samInsts ;

			if (constraints != null && constraints.size() >0) {
				filter = buildFilter (constraints);
				query = new QueryLDAPImpl(filter.toString());
			}

			if ((implName != null) && (ASM.ASMImplBroker.getImpl(implName) != null)) {
				impl = ASM.ASMImplBroker.getImpl(implName) ;
			} else {
				impl = ASM.ASMImplBroker.getImplSamName(implName) ;
			}
			Instance theInstance = null ;
			if (impl == null) return null ;
			//We know the Sam specification
			
			samInsts = SAMImplBroker.getInstances(impl.getSamImpl().getPid(), query) ;
			if ((samInsts == null) || (samInsts.size() == 0)) return null ;
			theInstance = (Instance)samInsts.toArray()[0] ;
			if (ASM.ASMInstBroker.getInst(theInstance) != null) {
				return ASM.ASMInstBroker.getInst(theInstance) ;
			}
			return ASM.ASMInstBroker.addInst(from.getComposite(), theInstance, implName, null, null) ;
		} catch (Exception e) {} ;
		return null;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void newComposite(ManagerModel model, Composite composite) {
		
	}


}
