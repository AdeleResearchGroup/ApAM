package fr.imag.adele.apam;


import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.samAPIImpl.ASMImplImpl;

public class CompositeImpl implements Composite {

	//Global variable. The actual content of the ASM
	private static Map<String, Composite> composites = new HashMap <String, Composite> ();	
	
//	private Logger logger = Logger.getLogger(CompositeImpl.class);
//	private static final InstanceBroker samInstBroker = ASM.SAMInstBroker ;
//	private static final ImplementationBroker samImplBroker = ASM.SAMImplBroker ;
//	private static final ASMInstBroker instBroker = ASM.ASMInstBroker ;
	private static final ASMImplBroker implBroker = ASM.ASMImplBroker ;
//	private static final ASMSpecBroker specBroker = ASM.ASMSpecBroker ;
	

	private String name ; // its name !	
	//The models associated with this composite (appli or not)
	private Set <ManagerModel> models = null ;
	
	//All the specs, imple, instance" contained in this composite ! Warning : may be shared.
	private Set<ASMSpec> hasSpecs = new HashSet<ASMSpec> ();
	private Set<ASMImpl> hasImplem = new HashSet<ASMImpl> ();
	private Set<ASMInst> hasInstance = new HashSet<ASMInst> ();	
	
	//all the dependencies
	private Set<Composite> depends = new HashSet<Composite> ();
	private Set<Composite> invDepend = new HashSet<Composite> (); //reverse dependency
	
	

	private CompositeImpl () { } ; //prohibited
	public CompositeImpl (String name, Set <ManagerModel> models) {
		if (composites.get(name) != null) {
			System.out.println("Composite " + name + " allready exists");
			return ;
		}
		composites.put(name, this);
		this.name = name ;
		this.models = models ;
	}

	@Override
	public Composite createComposite(Composite source, String name, Set <ManagerModel> models) {
		if (composites.get(name) != null) {
			System.out.println("Composite " + name + " allready exists");
			return null;
		}
		if (source == null) {
			System.out.println("Source composite missing");
		}
		Composite comp = new CompositeImpl (name, models) ;
		source.addDepend(comp) ;
		return comp;
	}

	@Override
	public String getName() {
		return name;
	}

//	@Override
	
	/**
	 * 2 Pbs : delete the dependent composite.
	 * delete the contained objects ? Warning if shared. 
	 * state ?
	 * 
	 */
//	public boolean deleteComposite(String compositeName) {
//	
//		return false;
//	}
	

///**
// * Add a dependency relationship between source and destination.
// * A single one allowed. 
// * Only called by addDepend
// */
//	private void addInvDepend(Composite source) { //not in the interface
//		this.fathers.add(source);
//	}


	@Override
	/**
	 * Only creates the APAM object. No creation in SAM. (create spec is not available in SAM).
	 * Warning : the PID is not initialised.
	 * No duplication, if already existing.
	 * If shared, the spec is visible in the whole appli, and is created in the appli. 
	 */
	public void addSpec(ASMSpec spec) {
		//ASMSpec spec = specBroker.addSpec(this, name, samSpec) ;
		if (spec.getShared() == ASM.SHAREABLE) ASM.addSharedSpec(spec) ;
		hasSpecs.add (spec) ;
	}


	/**
	 * Attention : instance SAM ou instance APAM
	 */
	@Override
	public void addInst(ASMInst inst) {
		if (inst == null) {
			System.out.println("shoudl provide a real instance to addInst in composite");
			return  ;
		}
		if (inst.getShared() == ASM.SHAREABLE) ASM.addSharedInst(inst) ;
		hasInstance.add(inst) ;
	}

	
//Composite Dependency management ===============
	public void addDepend(Composite dest) { 
		if (this.depends.contains(dest)) return ; //allready existing
		this.depends.add(dest);
		((CompositeImpl)dest).addInvDepend (this) ;
	}

//	private void addInvDepend (Composite origin) {
//		fathers.add (origin) ;
//	}
	/**
	 * A composite cannot be isolated. 
	 * Therefore this is prohibited if the destination will be isolated. 
	 */
	@Override
	public boolean removeDepend(Composite destination) {
		if (!dependsOn(destination)) return false ;
		if (((CompositeImpl)destination).getInvDepend().size() < 2) return false ;
		((CompositeImpl)destination).removeInvDepend (this) ;
		depends.remove(destination) ;
		return false;
	}

	/**
	 * retourne les depends inverses. Attention on retourne le vrai tableau !
	 * @return
	 */
	protected Set<Composite> getInvDepend () {
		return invDepend ;
	}
	
	/**
	 * Retire une dependance inverse.
	 * @param origin
	 * @return
	 */
	protected boolean removeInvDepend (Composite origin) {
		invDepend.remove(origin);
		return true ;
	}
	/**
	 * Ajoute une dépendance inverse.
	 * @param origin
	 * @return
	 */
	protected void addInvDepend (Composite origin) {
		invDepend.add(origin);
		return ;
	}
	
	@Override
	public boolean containsSpec(ASMSpec spec) {
		return hasSpecs.contains(spec);
	}
	
	@Override
	public boolean containsImpl(ASMImpl spec) {
		return hasImplem.contains(spec) ;
	}

	@Override
	public boolean containsInst(ASMInst inst) {
		return hasInstance.contains(inst) ;
	}

/**
 * Warning, it is the real array !!
 */
	@Override
	public Set<Composite> getDepend() {
		return new HashSet<Composite> (depends);
	}

	@Override
	public Set<ASMSpec> getSpecs() {
		return new HashSet<ASMSpec> (hasSpecs);
	}

	@Override
	public Set<ASMImpl> getImpls() {
		return new HashSet<ASMImpl> (hasImplem);
	}

	@Override
	public Set<ASMInst> getInsts() {
		return new HashSet<ASMInst> (hasInstance);
	}


	@Override
	public boolean dependsOn(Composite dest) {
		return (depends.contains(dest));
	}

	@Override
	public Composite getComposite(String compositeName) {
		return composites.get(compositeName) ;
	}
	@Override
	public void addImpl(ASMImpl impl) {
		hasImplem.add(impl) ;	
	}
	@Override
	public ManagerModel getModel(String name) {
		for (ManagerModel model : models) {
			if (model.getName().equals(name)) return model ;
		}
		return null;
	}
	@Override
	public Set<ManagerModel> getModels() {
		return new HashSet<ManagerModel> (models);
	}
	


}
