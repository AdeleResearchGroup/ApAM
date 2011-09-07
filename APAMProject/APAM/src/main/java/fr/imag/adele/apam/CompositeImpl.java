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
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public class CompositeImpl extends ASMInstImpl implements Composite {

    // Global variable. The actual content of the ASM
    private static Map<String, Composite> composites     = new HashMap<String, Composite>();
    private static Map<String, Composite> rootComposites = new HashMap<String, Composite>();

    private final String                  name;
    private final CompositeType           compType;
    private final ASMImpl                 mainImpl;
    private final ASMInst                 mainInst;
    private final Set<ASMInst>            hasInstance    = new HashSet<ASMInst>();

    // all the dependencies between composites
    private final Set<Composite>          depend         = new HashSet<Composite>();
    private final Set<Composite>          invDepend      = new HashSet<Composite>();        // reverse dependency

    //The father-son delationship
    private final Set<Composite>          sons           = new HashSet<Composite>();
    private final Composite               father;                                           //null if appli

    private CompositeImpl(CompositeType compType, Composite instCompo, ASMInst asmInst) {
        //the composite instance as an ASMInst
        //directly refers to the sam object associated with the main instance.
        super(compType, instCompo, null, asmInst.getSAMInst());

        //because the constructor needs the instance, and the instance need the composite ...
        ((ASMInstImpl) asmInst).setComposite(this);
        name = compType.getName() + "<" + asmInst.getName() + ">";
        this.compType = compType;
        mainInst = asmInst;
        hasInstance.add(mainInst);
        mainImpl = asmInst.getImpl();

        //instCompo is both the father, and the composite that contains the new one, seen as a usual ASMInst.
        if (instCompo != null) {
            instCompo.asInternal().addSon(this);
        } else
            CompositeImpl.rootComposites.put(name, this);
        father = instCompo;
        CompositeImpl.composites.put(name, this);
        setComposite(instCompo); //may be null

    }

    /**
     * Get access to the internal implementation of the wrapped instance
     */
    @Override
    public Internal asInternal() {
        return asInternal();
    }

    public static Collection<Composite> getRootComposites() {
        return Collections.unmodifiableCollection(CompositeImpl.rootComposites.values());
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

    public static Composite createComposite(CompositeType compType, Composite instCompo, Attributes initialproperties,
            ASMInst first) {
        if (compType == null) {
            System.err.println("ERROR :  missing type in createComposite");
            return null;
        }
        ASMInst asmInst;
        if (first != null)
            asmInst = first;
        else
            asmInst = compType.getMainImpl().createInst(null, initialproperties);
        return new CompositeImpl(compType, instCompo, asmInst);
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

    //Father-son relationship management. Hidden, Internal;    
    public void addSon(Composite dest) {
        if (dest == null)
            return;
        if (sons.contains(dest))
            return; // allready existing
        sons.add(dest);
        dest.asInternal().addInvSon(this);
    }

    /**
     * A son can be removed only when deleted. Warning : not checked.
     */
    public boolean removeSon(Composite destination) {
        if (destination == null)
            return false;
        sons.remove(destination);
        destination.asInternal().removeInvSon(this);
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
        if (depend.contains(dest))
            return; // allready existing

        depend.add(dest);
        dest.asInternal().addInvDepend(this);
    }

    /**
     * A composite cannot be isolated. Therefore remove is prohibited if the destination will be isolated.
     */
    @Override
    public boolean removeDepend(Composite destination) {
        if (destination == null)
            return false;
        //        if (!dependsOn(destination))
        //            return false;
        //        if (destination.getDepends().size() < 2)
        //            return false;
        //        
        depend.remove(destination);
        destination.asInternal().removeInvDepend(this);
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

}
