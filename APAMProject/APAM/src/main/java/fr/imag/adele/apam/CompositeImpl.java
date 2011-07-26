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

public class CompositeImpl implements Composite {

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

    //For executable composites
    private ASMSpec                       mainSpec    = null;
    private ASMImpl                       mainImpl    = null;
    private ASMInst                       mainInst    = null;

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
        CompositeImpl same = (CompositeImpl) CompositeImpl.composites.get(name);
        if (same != null) {
            name = same.getNewName(name);
        }
        CompositeImpl.composites.put(name, this);
        this.name = name;
        appli = application;
        ((ApplicationImpl) appli).addComposite(this);
        this.models = models;
        if (father != null) { //father is null when creating an application, or instance composite.
            ((CompositeImpl) father).addSon(this);
            this.father = father;
            ((CompositeImpl) father).addDepend(this);
            addInvDepend(father);
        }
        Manager man;
        if (models != null) {
            for (ManagerModel managerModel : models) { // call the managers to indicate the new composite and the model
                man = CST.apam.getManager(managerModel.getManagerName());
                if (man != null) {
                    man.newComposite(managerModel, this);
                }
            }
        }
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

    //Father-son relationship management. Hidden;    
    public void addSon(Composite dest) {
        if (dest == null)
            return;
        if (sons.contains(dest))
            return; // allready existing
        sons.add(dest);
        ((CompositeImpl) dest).addInvDepend(this);
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
        ((CompositeImpl) dest).addInvDepend(this);
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
        if (((CompositeImpl) destination).getInvDepend().size() < 2)
            return false;
        ((CompositeImpl) destination).removeInvDepend(this);
        depends.remove(destination);
        return false;
    }

    /**
     * returns the reverse dependencies !
     * 
     * @return
     */
    protected Set<Composite> getInvDepend() {
        return Collections.unmodifiableSet(invDepend);
    }

    /**
     * Retire une dependance inverse.
     * 
     * @param origin
     * @return
     */
    protected boolean removeInvDepend(Composite origin) {
        if (origin == null)
            return false;
        invDepend.remove(origin);
        return true;
    }

    /**
     * Ajoute une dépendance inverse.
     * 
     * @param origin
     * @return
     */
    protected void addInvDepend(Composite origin) {
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

    public ASMSpec getMainSpec() {
        return mainSpec;
    }

    public ASMInst getMainInst() {
        return mainInst;
    }

    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    public void setMainSpec(ASMSpec spec) {
        mainSpec = spec;
    }

    public void setMainInst(ASMInst inst) {
        mainInst = inst;
    }

    public void setMainImpl(ASMImpl impl) {
        mainImpl = impl;
    }

}
