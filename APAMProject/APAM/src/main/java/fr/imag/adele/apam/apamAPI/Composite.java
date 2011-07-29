package fr.imag.adele.apam.apamAPI;

/**
 * Interface used by APAM and managers to manage the composites.
 * 
 * @author Jacky
 * 
 */

import java.util.Set;

import fr.imag.adele.apam.ManagerModel;

public interface Composite {

    // public boolean deleteComposite (Composite comp) ;
    // public boolean deleteComposite (String name) ;
    public String getName();

    /*
     * return the application it pertains to.
     */
    public Application getApplication();

    /**
     * Creates a composite in the application in which the source pertains. A single composite with this name can exist
     * in an application. Return null if name conflicts.
     * 
     * @param source The origin of the dependency relationship. The embedding composite.
     * @param name the symbolic name. Unique in this application
     * @param models optionnal : the associated models.
     * @return
     */
    public Composite createComposite(Composite father, String name, Set<ManagerModel> models);

    public void addDepend(Composite destination);

    public boolean removeDepend(Composite destination);

    public Set<Composite> getDepend();

    public boolean dependsOn(Composite destination);

    public Composite getFather();

    public Set<Composite> getSons();

    public ManagerModel getModel(String name);

    public Set<ManagerModel> getModels();

    /**
     * Adds the existing specification to the current composite.
     * 
     * @param spec an existing spec in ASM
     */
    public void addSpec(ASMSpec spec);

    /**
     * 
     * @param spec an existing spec in ASM
     * @return true if the spec is contained in the current composite
     */
    public boolean containsSpec(ASMSpec spec);

    /**
     * @return all the specs of that composite
     */
    public Set<ASMSpec> getSpecs();

    public void addImpl(ASMImpl impl);

    public boolean containsImpl(ASMImpl spec);

    public Set<ASMImpl> getImpls();

    public void addInst(ASMInst inst);

    public boolean containsInst(ASMInst inst);

    public Set<ASMInst> getInsts();

    //TO remove ...

    public void setMainSpec(ASMSpec spec);

    public void setMainImpl(ASMImpl impl);

    public void setMainInst(ASMInst inst);

    public ASMSpec getMainSpec();

    public ASMImpl getMainImpl();

    public ASMInst getMainInst();

}
