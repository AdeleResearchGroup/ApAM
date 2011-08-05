package fr.imag.adele.apam;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;

public class CompositeImpl implements Composite, Composite.Internal {


    // Global variable. The actual content of the ASM
    private static Map<String, Composite> composites  = new HashMap<String, Composite>();

    private String                        name;
    // The application it pertains to.
    private Application                   appli;
    // The models associated with this composite
    private Set<ManagerModel>             models      = null;

    // All the specs, implem, instances contained in this composite ! Warning :
    // may be shared.
    private final Set<ASMSpec>            hasSpecs    = new HashSet<ASMSpec>();
    private final Set<ASMImpl>            hasImplem   = new HashSet<ASMImpl>();
    private final Set<ASMInst>            hasInstance = new HashSet<ASMInst>();

    // all the dependencies between composites
    private final Set<Composite>          depends     = new HashSet<Composite>();
    private final Set<Composite>          invDepend   = new HashSet<Composite>();        // reverse dependency

    //The father-son delationship
    private final Set<Composite>          sons        = new HashSet<Composite>();
    private Composite                     father      = null;                            //null if appli

    // To have different names
    private int                           nbSameName  = 0;

    public String getNewName(String name) {
        String newName = name + "-" + nbSameName;
        nbSameName++;
        return newName;
    }

    private CompositeImpl() {
    }; // prohibited

    public CompositeImpl(String name, Composite father, Application application, Set<ManagerModel> models) {
        
    	// TODO We need to review this with the new specification of CompExType and CompExInst
    	CompositeImpl same = (CompositeImpl) CompositeImpl.composites.get(name);
        if (same != null) {
            name = same.getNewName(name);
        }
        
        // Register composite with the application and the global registry
        CompositeImpl.composites.put(name, this);
        this.name = name;
        appli = application;
        ((ApplicationImpl) appli).addComposite(this);
        
        this.models = models;
        if (father != null) { //father is null when creating an application, or instance composite.
            this.father = father;
            this.father.addDepend(this);
            this.father.asInternal().addSon(this);
        }

        if (models != null) {
            Manager man;
            for (ManagerModel managerModel : models) { // call the managers to indicate the new composite and the model
                man = CST.apam.getManager(managerModel.getManagerName());
                if (man != null) {
                    man.newComposite(managerModel, this);
                }
            }
        }
    }

    @Override
    public Composite createComposite(String name, Set<ManagerModel> models) {
        if (name == null) {
            System.out.println("ERROR : Composite name missing");
            return null;
        }
        if (this.getApplication().getComposite(name) != null) {
            System.out.println("ERROR : Composite " + name + " allready exists");
            return this.getApplication().getComposite(name);
        }

        return new CompositeImpl(name, this, this.getApplication(), models);
    }

    /**
     * Give access to the internal representation of this Composite
     */
    @Override
    public final Internal asInternal() {
    	return this;
    }


    @Override
    public String getName() {
        return name;
    }

    // @Override

    /**
     * 2 Pbs : delete the dependent composite. delete the contained objects ? Warning if shared. state ?
     * 
     */
    // public boolean deleteComposite(String compositeName) {
    //
    // return false;
    // }

    @Override
    /**
     * Only creates the APAM object. No creation in SAM. 
     * No duplication, if already existing.
     */
    public void addSpec(ASMSpec spec) {
        if (spec == null)
            return;
        hasSpecs.add(spec);
    }

    @Override
    public void addImpl(ASMImpl impl) {
        if (impl == null)
            return;
        hasImplem.add(impl);
        //TODO retirer  de l'ancien ???
    }

    /**
     * Attention : instance SAM ou instance APAM
     */
    @Override
    public void addInst(ASMInst inst) {
        if (inst == null) {
            System.err.println("ERROR : shoudl provide a real instance to addInst in composite");
            return;
        }
        hasInstance.add(inst);
    }

    //Father-son relationship management. Hidden, Internal;    
    public void addSon(Composite dest) {
        if (dest == null)
            return;
        if (sons.contains(dest))
            return; // allready existing
        sons.add(dest);
    }

    /**
     * A son can be removed only when deleted. Warning : not checked.
     */
    public boolean removeSon(Composite destination) {
        if (destination == null)
            return false;
        sons.remove(destination);
        return true;
    }

    /**
     * returns the father !
     * 
     * @return
     */
    @Override
    public Composite getFather() {
        return father;
    }

    @Override
    public Set<Composite> getSons() {
        return Collections.unmodifiableSet(sons);
    }

    // Composite Dependency management ===============
    @Override
    public void addDepend(Composite dest) {
        if (dest == null)
            return;
        if (depends.contains(dest))
            return; // allready existing
        
        depends.add(dest);
        dest.asInternal().addInvDepend(this);
    }

    /**
     * A composite cannot be isolated. Therefore remove is prohibited if the destination will be isolated.
     */
    @Override
    public boolean removeDepend(Composite destination) {
        if (destination == null)
            return false;
        if (!dependsOn(destination))
            return false;
        if (destination.getDependents().size() < 2)
            return false;
        
        depends.remove(destination);
        destination.asInternal().removeInvDepend(this);
        return true;
    }

    /**
     * returns the reverse dependencies !
     * 
     * @return
     */
    @Override
    public Set<Composite> getDependents() {
        return Collections.unmodifiableSet(invDepend);
    }

    /**
     * Retire une dependance inverse.  Hidden, Internal; 
     * 
     * @param origin
     * @return
     */
    public boolean removeInvDepend(Composite origin) {
        if (origin == null)
            return false;
        invDepend.remove(origin);
        return true;
    }

    /**
     * Ajoute une dépendance inverse.  Hidden, Internal; 
     * 
     * @param origin
     * @return
     */
    @Override
    public void addInvDepend(Composite origin) {
        invDepend.add(origin);
        return;
    }

    @Override
    public boolean containsSpec(ASMSpec spec) {
        if (spec == null)
            return false;
        return hasSpecs.contains(spec);
    }

    @Override
    public boolean containsImpl(ASMImpl spec) {
        if (spec == null)
            return false;
        return hasImplem.contains(spec);
    }

    @Override
    public boolean containsInst(ASMInst inst) {
        if (inst == null)
            return false;
        return hasInstance.contains(inst);
    }

    /**
     * Warning, it is the real array !!
     */
    @Override
    public Set<Composite> getDepend() {
        return Collections.unmodifiableSet(depends);
    }

    @Override
    public Set<ASMSpec> getSpecs() {
        return Collections.unmodifiableSet(hasSpecs);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return Collections.unmodifiableSet(hasImplem);
    }

    @Override
    public Set<ASMInst> getInsts() {
        return Collections.unmodifiableSet(hasInstance);
    }

    @Override
    public boolean dependsOn(Composite dest) {
        if (dest == null)
            return false;
        return (depends.contains(dest));
    }

    @Override
    public Application getApplication() {
        return appli;
    }

    @Override
    public ManagerModel getModel(String name) {
        if (name == null)
            return null;
        for (ManagerModel model : models) {
            if (model.getName().equals(name))
                return model;
        }
        return null;
    }

    @Override
    public Set<ManagerModel> getModels() {
        return Collections.unmodifiableSet(models);
    }

}
