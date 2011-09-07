package fr.imag.adele.apam.apamAPI;

import java.util.Set;
import fr.imag.adele.apam.ManagerModel;

public interface CompositeType extends ASMImpl {

    public ASMImpl getMainImpl();

    public ManagerModel getModel(String name);

    public Set<ManagerModel> getModels();

    public void addImport(CompositeType destination);

    public boolean removeImport(CompositeType destination);

    public Set<CompositeType> getImport();

    public boolean imports(CompositeType destination);

    public void addImpl(ASMImpl impl);

    public boolean containsImpl(ASMImpl spec);

    public Set<ASMImpl> getImpls();

    /**
     * This is a privileged interface that must be provided by all implementations of Composite in
     * order to be able to automatically handle consistent bidirectional relationships.
     * 
     * It ensures that import associations are kept coherent.
     * 
     * Implementing this interface ensures a minimal compatibility among the many possible different
     * implementations of Composite. Implementations of Composite must ensure that it should always
     * be possible to get a internal representation for its instances.
     * 
     * @author vega
     * 
     */
    public interface Internal extends CompositeType {

        /**
         * Adds a new dependent composite.
         * 
         * This should be called as a side effect of adding a dependency to this Composite
         */
        public void addInvImport(CompositeType dependent);

        /**
         * Removes an existing dependent composite.
         * 
         * This should be called as a side effect of removing a dependency to this Composite
         */
        public boolean removeInvImport(CompositeType dependent);

    }

    /**
     * Gets the internal representation of this Composite
     * 
     */
    public Internal asInternal();
}
