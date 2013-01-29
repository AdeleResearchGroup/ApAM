package fr.imag.adele.apam;

import java.util.Set;

import fr.imag.adele.apam.declarations.CompositeDeclaration;

public interface CompositeType extends Implementation {
    /**
     * 
     * @return the main implementation
     */
    public Implementation getMainImpl();

    /**
     * 
     * @return the declaration of this composite
     */
    public CompositeDeclaration getCompoDeclaration();

    /**
     * 
     * @param name
     * @return the model of that name
     */
    public ManagerModel getModel(String name);

    /**
     * returns all the models
     * 
     * @return
     */
    public Set<ManagerModel> getModels();


    /**
     * Adds an "import" relationship towards "destination". Not in the interface
     * 
     * @param destination
     */
    public void addImport(CompositeType destination);

    /**
     * returns all the "import" relationships
     * 
     * @return
     */
    public Set<CompositeType> getImport();

    /**
     * return true if the composite type import "destination"
     * 
     * @param destination
     * @return
     */
    public boolean isFriend(CompositeType destination);

    /**
     * return true if the current type contains "impl"
     * 
     * @param spec
     * @return
     */
    public boolean containsImpl(Implementation impl);

    /**
     * return all the implementation contained in this type
     * 
     * @return
     */
    public Set<Implementation> getImpls();

    
    /**
     * return the composite types embedded in the current one.
     * 
     * @return
     */
    public Set<CompositeType> getEmbedded();

    /**
     * returns the composite types that contain this one.
     * 
     * @return
     */
    public Set<CompositeType> getInvEmbedded();

}