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
     * @param name the symbolic name. Unique in this application
     * @param models optional : the associated models.
     */
    public Composite createComposite(String name, Set<ManagerModel> models);

    public void addDepend(Composite destination);

    public boolean removeDepend(Composite destination);

    public Set<Composite> getDepend();

    public boolean dependsOn(Composite destination);
    
    public Set<Composite> getDependents();

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

    /**
     * This is a privileged interface that must be provided by all implementations of Composite in 
     * order to be able to automatically handle consistent bidirectional relationships.
     * 
     * It ensures that father/sons and depends/dependents associations are kept coherent.
     * 
     * Implementing this interface ensures a minimal compatibility among the many possible different
     * implementations of Composite. Implementations of Composite must ensure that it should always
     * be possible to get a internal representation for its instances.
     * 
     * @author vega
     *
     */
    public interface Internal extends Composite {
    	
    	/**
    	 * Registers a new child with the this composite.
    	 * 
    	 * This should be called as a side effect of creating a new Composite specifying
    	 * this as a father.
    	 */
        public void addSon(Composite child);

        /**
         * Unregisters an existing child from the list of sons of this composite.
         * 
         * Notice that this means that the passed composite has no longer a father, so
         * it is no longer valid. 
         * 
         * This should be called as side effect of deleting a Composite.
         * 
         * TODO should we allow moving a composite to another parent?
         */
        public boolean removeSon(Composite child);

        /**
         * Adds a new dependent composite.
         * 
    	 * This should be called as a side effect of adding a dependency to this Composite
         */
        public void addInvDepend(Composite dependent);

        /**
         * Removes an existing dependent composite.
         * 
    	 * This should be called as a side effect of removing a dependency to this Composite
         */
       public boolean removeInvDepend(Composite dependent);
       
    }
    
    /**
     * Gets the internal representation of this Composite
     * 
     */
    public Internal asInternal();

}
