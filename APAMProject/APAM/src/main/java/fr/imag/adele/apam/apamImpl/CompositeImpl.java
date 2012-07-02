package fr.imag.adele.apam.apamImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.apform.ApformInstance;

public class CompositeImpl extends InstanceImpl implements Composite {

    // Global variable.
    private static Map<String, Composite> composites    = new ConcurrentHashMap<String, Composite>();
    private static Composite              rootComposite = new CompositeImpl();

    private final CompositeType           compType;
    private final Implementation          mainImpl;
    private final Instance                mainInst;
    private final Composite               appliComposite;                                  // root of father rel
    private final Set<Instance>           hasInstance   = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

    // the dependencies between composites
    private final Set<Composite>          depend        = Collections.newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());
    private final Set<Composite>          invDepend     = Collections.newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());  // reverse dependency

    // The father-son relationship
    private final Set<Composite>          sons          = Collections
    .newSetFromMap(new ConcurrentHashMap<Composite, Boolean>());
    private Composite                     father;                                          // null if appli

    /**
     * This constructor is only available for the root composite. The root composite is the only composite
     * without an associated main instance.
     */
    private CompositeImpl() {
        super();
        mainImpl = null;
        mainInst = null;
        compType = CompositeTypeImpl.getRootCompositeType(this);
        appliComposite = null;
    }

    //    public static Composite getRootComposite() {
    //        return CompositeImpl.rootComposite;
    //    }

    public static CompositeImpl newCompositeImpl(CompositeType compType, Composite instCompo,
            Instance externalMainInst, Map<String, Object> initialproperties, ApformInstance apfInst) {
        if (instCompo == null) {
            instCompo = CompositeImpl.rootComposite;
        }
        return new CompositeImpl(compType, instCompo, externalMainInst, initialproperties, apfInst);
    }

    private CompositeImpl(CompositeType compType, Composite instCompo, Instance externalMainInst,
            Map<String, Object> initialproperties, ApformInstance apfInst) {

        // First create the composite, as a normal instance
        super(compType, instCompo, initialproperties, apfInst);
        put(CST.A_COMPOSITE, CST.V_TRUE);
        // initialize as a composite
        this.compType = compType;

        mainImpl = compType.getMainImpl();

        // if it is a composite created from an unused inst that calls Apam;
        // create the main instance with this composite as container.
        // Each composite has a different main instance; do not try to reuse an existing instance.
        if (externalMainInst == null) { // normal case
            externalMainInst = compType.getMainImpl().createInst(this, null /*initialproperties */);
        }
        mainInst = externalMainInst;
        ((InstanceImpl) mainInst).put(CST.A_SHARED, CST.V_FALSE);
        Apform.setUsedInst(mainInst); // useful ??

        // instCompo is both the father, and the composite that contains the new one, seen as a usual ASMInst.
        ((CompositeImpl) instCompo).addSon(this);
        CompositeImpl.composites.put(getName(), this);

        // initialize the composite as ASMInst
        hasInstance.add(mainInst);

        // if it is a root composite
        if (instCompo.getAppliComposite() == null) {
            appliComposite = this;
            father = null;
        } else
            appliComposite = instCompo.getAppliComposite();

        ((InstanceBrokerImpl) CST.InstBroker).addInst(this);
    }

    public static Composite getRootAllComposites() {
        return CompositeImpl.rootComposite;
    }

    public static Collection<Composite> getRootComposites() {
        return Collections.unmodifiableSet(CompositeImpl.rootComposite.getSons());
    }

    public static Collection<Composite> getComposites() {
        return Collections.unmodifiableCollection(CompositeImpl.composites.values());
    }

    public static Composite getComposite(String name) {
        return CompositeImpl.composites.get(name);
    }


    @Override
    public String getName() {
        return getApformInst() != null ? super.getName() : "rootComposite";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Instance getMainInst() {
        return mainInst;
    }

    /**
     * Overrides the instance method. A composite has no object, returns the main instance object
     */
    @Override
    public Object getServiceObject() {
        assert (mainInst != null);
        return mainInst.getApformInst().getServiceObject();
    }

    @Override
    public Implementation getMainImpl() {
        return mainImpl;
    }

    @Override
    public CompositeType getCompType() {
        return compType;
    }

    @Override
    public void addContainInst(Instance inst) {
        assert (inst != null);

        hasInstance.add(inst);
    }

    // Father-son relationship management. Hidden, Internal;
    public void addSon(Composite dest) {
        if (dest == null)
            return;
        if (sons.contains(dest))
            return; // allready existing
        sons.add(dest);
        ((CompositeImpl) dest).addInvSon(this);
    }

    /**
     * A son can be removed only when deleted. Warning : not checked.
     */
    public boolean removeSon(Composite destination) {
        if (destination == null)
            return false;
        sons.remove(destination);
        ((CompositeImpl) destination).removeInvSon(this);
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

    @Override
    public Composite getAppliComposite() {
        return appliComposite;
    }

    // Composite Dependency management ===============
    @Override
    public void addDepend(Composite dest) {
        if (dest == null)
            return;
        if (depend.contains(dest))
            return; // allready existing

        depend.add(dest);
        ((CompositeImpl) dest).addInvDepend(this);
    }

    /**
     * A composite cannot be isolated. Therefore remove is prohibited if the destination will be isolated.
     */
    private boolean removeDepend(Composite destination) {
        if (destination == null)
            return false;
        // if (!dependsOn(destination))
        // return false;
        // if (destination.getDepends().size() < 2)
        // return false;
        //
        depend.remove(destination);
        ((CompositeImpl) destination).removeInvDepend(this);
        return true;
    }

    /**
     * returns the reverse dependencies !
     * 
     * @return
     */
    @Override
    public Set<Composite> getInvDepend() {
        return Collections.unmodifiableSet(invDepend);
    }

    /**
     * Retire une dependance inverse. Hidden, Internal;
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
     * Ajoute une dépendance inverse. Hidden, Internal;
     * 
     * @param origin
     * @return
     */

    public void addInvDepend(Composite origin) {
        invDepend.add(origin);
        return;
    }

    @Override
    public boolean containsInst(Instance inst) {
        if (inst == null)
            return false;
        return hasInstance.contains(inst);
    }

    @Override
    public Set<Composite> getDepend() {
        return Collections.unmodifiableSet(depend);
    }

    @Override
    public Set<Instance> getContainInsts() {
        return Collections.unmodifiableSet(hasInstance);
    }

    @Override
    public boolean dependsOn(Composite dest) {
        if (dest == null)
            return false;
        return (depend.contains(dest));
    }

    @Override
    public ManagerModel getModel(String name) {
        return compType.getModel(name);
    }

    @Override
    public Set<ManagerModel> getModels() {
        return compType.getModels();
    }

    public void addInvSon(Composite father) {
        this.father = father;
    }

    public void removeInvSon(Composite father) {
        this.father = null;
    }

    public void removeInst(Instance inst) {
        hasInstance.remove(inst);
    }

}
