package fr.imag.adele.apam.apamAPI;

/**
 * Interface used by APAM and managers to manage the composites.
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
     * Creates an application with name the appli name and also the name of the first composite. Main is the
     * implementation to start.
     * 
     * @param name name of the appli.
     * @param main name of the imple to start.
     * @param start if true, instantiate main and start it.
     * @return an Application State model.
     */

    public void addDepend(Composite destination);

    public boolean removeDepend(Composite destination);

    public Set<Composite> getDepend();

    public boolean dependsOn(Composite destination);

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

}
