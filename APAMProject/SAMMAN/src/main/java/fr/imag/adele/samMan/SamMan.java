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
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.sam.Instance;
import fr.imag.adele.sam.Specification;
import fr.imag.adele.sam.broker.ImplementationBroker;
import fr.imag.adele.sam.broker.InstanceBroker;
import fr.imag.adele.sam.broker.SpecificationBroker;
import fr.imag.adele.sam.deployment.broker.DeploymentUnitBroker;

public class SamMan implements Manager {

	//The entry point in SAM
	public static ImplementationBroker SAMImplBroker = null;
	public static InstanceBroker SAMInstBroker = null;

	/*
	 * The reference to APAM, injected by iPojo
	 */
	private ManagersMng apam;

	/**
	 * SANMAN activated, register with APAM
	 */
	public void start() {
		apam.addManager(this,0);
		SAMImplBroker = ASM.SAMImplBroker ;
		SAMInstBroker = ASM.SAMInstBroker ;
	}

	public void stop() {
		apam.removeManager(this);
	}


	// TODO Must read the opportunist model and build the list of opportunist specs.
	//If empty, all is supposed to be opportunist ??? 
	private static Set<String> opportunismNames = new HashSet<String> () ;
	private static Set<Filter> filters = new HashSet<Filter> () ;
	private static boolean specOpportunist = true ;		//if the model says all specifications  are opportunist
	private static boolean implOpportunist = true ;  	//if the model says all implementations are opportunist


	private boolean opportunistSpec (String specName) {
		if (specOpportunist) return true ;
		return true ; //TODO waiting for the models
	}

	private boolean opportunistImpl (String implName) {
		if (implOpportunist) return true ;
		return opportunismNames.contains (implName) ;
	}

	@Override
	public List<Manager> getSelectionPathSpec (ASMInst from, String interfaceName, String specName, String depName, Filter filter, 
			List<Manager> involved) {
		if (opportunistSpec(specName)) {
			involved.add(this) ;
		}
		return involved ;
	}

	@Override
	public List<Manager> getSelectionPathImpl (ASMInst from, String samImplName, String implName, String depName, Filter filter, 
			List<Manager> involved) {
		if (opportunistImpl(implName)) involved.add(this) ;

		return involved ;
	}

	@Override
	public ASMInst resolveSpec (ASMInst from, String interfaceName, String specName, String depName, Set<Filter> constraints) {
		if (interfaceName == null && (specName == null)) {
			System.out.println("missing parameter interfaceName or specName");
			return null ;
		}
		if (from == null) {
			System.out.println("ERROR : missing parameter from in resolveSpec");
			return null ;
		}

		try {
			Query query = null;
			Filter filter ;
			ASMSpec spec ;
			Set<Instance> samInsts ;

			if (constraints != null && constraints.size() >0) {
				filter = Util.buildFilter (constraints);
				query = new QueryLDAPImpl(filter.toString());
			}

			if (specName != null) {
				spec = ASM.ASMSpecBroker.getSpec(specName) ;
			} else {
				spec = ASM.ASMSpecBroker.getSpecInterf(interfaceName) ;
			}
			Instance theInstance = null ;
			if (spec == null) { // No ASM spec known. Look for a SAM spec
				if (interfaceName == null) return null ; // no Way
				samInsts = SAMInstBroker.getInstances () ;
				for (Instance instance : samInsts) {
					Specification samSpec = instance.getSpecification();
					if (samSpec != null) {
						String [] interfs = samSpec.getInterfaceNames() ;
						for (int i = 0; i < interfs.length; i++) {
							if (interfs[i].equals(interfaceName)) {
								theInstance = instance ;
							}
						}
					}
				} 		
			} else { //We know the Sam specification
				samInsts = SAMInstBroker.getInstances(spec.getSamSpec(), query) ;
			}
			if (samInsts != null && samInsts.size()>0) {
				theInstance = (Instance)samInsts.toArray()[0] ;
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
		if (samImplName == null && (implName == null)) {
			System.out.println("ERROR : missing parameter samImplName or implName in resolveImpl");
			return null ;
		}
		if (from == null) {
			System.out.println("ERROR : missing parameter from in resolveImpl");
			return null ;
		}
		
		try {
			Query query = null;
			Filter filter ;
			ASMImpl impl ;
			Set<Instance> samInsts ;

			if (constraints != null && constraints.size() >0) {
				filter = Util.buildFilter (constraints);
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
