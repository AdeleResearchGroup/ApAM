package fr.imag.adele.apam;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.util.Attributes;

public class CompositeImpl extends ASMInstImpl implements Composite {

    // Global variable.
    private static Map<String, Composite> composites    = new HashMap<String, Composite>();
    private static Composite              rootComposite = new CompositeImpl();

    private final String                  name;
    private final CompositeType           compType;
    private final ASMImpl                 mainImpl;
    private final ASMInst                 mainInst;
    private final Composite               myRootComposite;
    private final Set<ASMInst>            hasInstance   = new HashSet<ASMInst>();

    // the dependencies between composites
    private final Set<Composite>          depend        = new HashSet<Composite>();
    private final Set<Composite>          invDepend     = new HashSet<Composite>();        // reverse dependency

    // The father-son relationship
    private final Set<Composite>          sons          = new HashSet<Composite>();
    private Composite                     father;                                          // null if appli

    private CompositeImpl() {
        super();
        name = "rootComposite";
        mainImpl = null;
        mainInst = null;
        compType = null;
        myRootComposite = null;
    }

    private CompositeImpl(CompositeType compType, Composite instCompo, Attributes initialproperties) {
        // First create the composite, as an ASMInst empty
        super();

        // initialize as a composite
        name = ((CompositeTypeImpl) compType).getNewInstName();
        this.compType = compType;
        mainImpl = compType.getMainImpl();

        // instCompo is both the father, and the composite that contains the new one, seen as a usual ASMInst.
        ((CompositeImpl) instCompo).addSon(this);
        CompositeImpl.composites.put(name, this);

        // create the main instance with this composite as container. Do not try to reuse an existing instance.
        // Each composite has a different main instance
        ASMInst asmInst = compType.getMainImpl().createInst(this, initialproperties);
        // initialize the composite as ASMInst
        // name = compType.getName() + "<" + asmInst.getName() + ">";
        mainInst = asmInst;
        hasInstance.add(mainInst);

        // if it is a root composite
        if (instCompo.getRootComposite() == null) {
            myRootComposite = this;
            father = null;
        } else
            myRootComposite = instCompo.getRootComposite();

        // terminate the ASMInst initialisation
        instConstructor(compType, instCompo, initialproperties, asmInst.getSAMInst());
    }

    /**
     * Get access to the internal implementation of the wrapped instance
     */
//    @Override
//    public Internal asInternal() {
//        return this;
//    }

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
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static Composite createComposite(CompositeType compType, Composite instCompo, Attributes initialproperties) {
        if (compType == null) {
            System.err.println("ERROR :  missing type in createComposite");
            return null;
        }
        if (instCompo == null)
            instCompo = CompositeImpl.rootComposite;
        return new CompositeImpl(compType, instCompo, initialproperties);
    }

    @Override
    public ASMInst getMainInst() {
        return mainInst;
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    @Override
    public CompositeType getCompType() {
        return compType;
    }

    @Override
    public void addContainInst(ASMInst inst) {
        if (inst == null) {
            System.err.println("ERROR : shoudl provide a real instance to addInst in composite");
            return;
        }
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
    public Composite getRootComposite() {
        return myRootComposite;
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
    @Override
    public boolean removeDepend(Composite destination) {
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
    public boolean containsInst(ASMInst inst) {
        if (inst == null)
            return false;
        return hasInstance.contains(inst);
    }

    @Override
    public Set<Composite> getDepend() {
        return Collections.unmodifiableSet(depend);
    }

    @Override
    public Set<ASMInst> getContainInsts() {
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

    @Override
    public boolean isInternal() {
        return ((CompositeTypeImpl) compType).getInternalInst();
    }

}
