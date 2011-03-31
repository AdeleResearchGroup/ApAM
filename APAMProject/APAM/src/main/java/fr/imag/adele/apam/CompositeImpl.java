package fr.imag.adele.apam;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;

public class CompositeImpl implements Composite {

	//Global variable. The actual content of the ASM
	private static Map<String, Composite> composites = new HashMap <String, Composite> ();		

	private String name ; // that composite name !	
	//The models associated with this composite 
	private Set <ManagerModel> models = null ;

	//All the specs, implem, instances contained in this composite ! Warning : may be shared.
	private Set<ASMSpec> hasSpecs = new HashSet<ASMSpec> ();
	private Set<ASMImpl> hasImplem = new HashSet<ASMImpl> ();
	private Set<ASMInst> hasInstance = new HashSet<ASMInst> ();	

	//all the dependencies between composites
	private Set<Composite> depends = new HashSet<Composite> ();
	private Set<Composite> invDepend = new HashSet<Composite> (); //reverse dependency



	private CompositeImpl () { } ; //prohibited
	public CompositeImpl (String name, Set <ManagerModel> models) {
		composites.put(name, this);
		this.name = name ;
		this.models = models ;
		Manager man ;
		if (models != null ) {
			for (ManagerModel managerModel : models) { //call the managers to indicate the new composite and the model
				man = ASM.apam.getManager (managerModel.getManagerName()) ;
				if (man != null) {
					man.newComposite(managerModel, this) ;
				}
			}
		}
	}

	@Override
	public Composite createComposite(Composite source, String name, Set <ManagerModel> models) {
		if (source == null) {
			System.out.println("ERROR : Source composite missing");
			return null ;
		}
		if (name == null) {
			System.out.println("ERROR : Composite name missing");
			return null ;
		}
		if (composites.get(name) != null) {
			System.out.println("ERROR : Composite " + name + " allready exists");
			//return null;
			return composites.get(name) ; //WARNING Debug.
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

	@Override
	/**
	 * Only creates the APAM object. No creation in SAM. 
	 * No duplication, if already existing.
	 * If shared, the spec is visible in the whole appli, and is created in the appli. 
	 */
	public void addSpec(ASMSpec spec) {
		if (spec == null) return ;
		if (spec.getShared() == ASM.SHAREABLE) ASM.addSharedSpec(spec) ;
		hasSpecs.add (spec) ;
	}

	@Override
	public void addImpl(ASMImpl impl) {
		if (impl == null) return ;
		if (impl.getShared() == ASM.SHAREABLE) ASM.addSharedImpl(impl) ;
		hasImplem.add(impl) ;	
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
		if (dest == null) return ;
		if (this.depends.contains(dest)) return ; //allready existing
		this.depends.add(dest);
		((CompositeImpl)dest).addInvDepend (this) ;
	}

	/**
	 * A composite cannot be isolated. 
	 * Therefore remove is prohibited if the destination will be isolated. 
	 */
	@Override
	public boolean removeDepend(Composite destination) {
		if (destination == null) return false;
		if (!dependsOn(destination)) return false ;
		if (((CompositeImpl)destination).getInvDepend().size() < 2) return false ;
		((CompositeImpl)destination).removeInvDepend (this) ;
		depends.remove(destination) ;
		return false;
	}

	/**
	 * returns the reverse dependencies !
	 * @return
	 */
	protected Set<Composite> getInvDepend () {
		return Collections.unmodifiableSet(invDepend) ;
	}

	/**
	 * Retire une dependance inverse.
	 * @param origin
	 * @return
	 */
	protected boolean removeInvDepend (Composite origin) {
		if (origin == null) return false ;
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
		if (spec == null) return false ;
		return hasSpecs.contains(spec);
	}

	@Override
	public boolean containsImpl(ASMImpl spec) {
		if (spec == null) return false ;
		return hasImplem.contains(spec) ;
	}

	@Override
	public boolean containsInst(ASMInst inst) {
		if (inst == null) return false ;
		return hasInstance.contains(inst) ;
	}

	/**
	 * Warning, it is the real array !!
	 */
	@Override
	public Set<Composite> getDepend() {
		return Collections.unmodifiableSet (depends);
	}

	@Override
	public Set<ASMSpec> getSpecs() {
		return Collections.unmodifiableSet (hasSpecs);
	}

	@Override
	public Set<ASMImpl> getImpls() {
		return Collections.unmodifiableSet  (hasImplem);
	}

	@Override
	public Set<ASMInst> getInsts() {
		return Collections.unmodifiableSet (hasInstance);
	}


	@Override
	public boolean dependsOn(Composite dest) {
		if (dest == null) return false ;
		return (depends.contains(dest));
	}

	@Override
	public Composite getComposite(String compositeName) {
		if (compositeName == null) return null ;
		return composites.get(compositeName) ;
	}
	@Override
	public ManagerModel getModel(String name) {
		if (name == null) return null ;
		for (ManagerModel model : models) {
			if (model.getName().equals(name)) return model ;
		}
		return null;
	}
	@Override
	public Set<ManagerModel> getModels() {
		return Collections.unmodifiableSet (models);
	}



}
