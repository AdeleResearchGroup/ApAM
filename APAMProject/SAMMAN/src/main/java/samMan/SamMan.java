package samMan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.Machine;
import fr.imag.adele.am.broker.BrokerBroker;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.am.query.QueryLDAPImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.PID;
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
	
	
	private boolean opportunist (ASMSpec spec) {
		return true ;
	}

	private boolean opportunist (ASMImpl impl) {
		return true ;
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
	public List<Manager> getSelectionPath(ASMInst from, ASMSpec to, String depName,
			Filter filter, List<Manager> involved) {
		if (opportunist(to)) {
			involved.add(this) ;
			return involved ;
		}
		return null ;
	}

	@Override
	public List<Manager> getSelectionPath(ASMInst from, ASMImpl to, String depName,
			Filter filter, List<Manager> involved) {
		if (opportunist(to)) {
			involved.add(this) ;
			return involved ;
		}
		return null ;
	}

	@Override
	public ASMInst resolve(ASMInst from, ASMSpec to, String depName, Set<Filter> constraints) {
		try {
			Query query = null;
			Filter filter ;
			if (constraints != null && constraints.size() >0) {
				filter = buildFilter (constraints);
				query = new QueryLDAPImpl(filter.toString());
			}
			Set<Instance> samInsts ;
			samInsts = SAMInstBroker.getInstances(to.getSamSpec(), query ) ;
			if (samInsts != null && samInsts.size()>0) {
				return (ASMInst)samInsts.toArray()[0] ;
			}
		} catch (Exception e) {} ;
		return null;

	}

	@Override
	public ASMInst resolve(ASMInst from, ASMImpl to, String depName, Set<Filter> constraints) {
		try {
			Query query = null;
			Filter filter ;
			if (constraints != null && constraints.size() >0) {
				filter = buildFilter (constraints);
				query = new QueryLDAPImpl(filter.toString());
			}
			Set<Instance> samInsts ;
			Implementation samImpl = SAMImplBroker.getImplementation(to.getSamImpl().getPid() ) ;
			samInsts = samImpl.getInstances(query ) ;
			if (samInsts != null && samInsts.size()>0) {
				return (ASMInst)samInsts.toArray()[0] ;
			}
		} catch (Exception e) {} ;
		return null;
	}

	@Override
	public void appeared(Instance samInstance, ASMImpl impl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appeared(Instance samInstance, String interf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ASMInst lostInst(ASMInst lost) {
		// TODO Auto-generated method stub
		return null;
	}
}
