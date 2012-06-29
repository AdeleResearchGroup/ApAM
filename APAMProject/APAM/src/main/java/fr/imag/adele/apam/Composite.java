package fr.imag.adele.apam;

import java.util.Set;


public interface Composite extends Instance {

    /**
     * retiurns the main instance
     * 
     * @return
     */
    public Instance getMainInst();

    /**
     * 
     * @return the main implementation
     */
    public Implementation getMainImpl();

    /**
     * 
     * @return the type of that composite
     */
    public CompositeType getCompType();

    /**
     * Overrides the instance method. Instead to return apfor.getserviceobject, return the main instance object
     */
    @Override
    public Object getServiceObject();

    /**
     * Adds a new depend relationship toward "destination"
     * 
     * @param destination
     */
    public void addDepend(Composite destination);

    //    /**
    //     * removes a "depend" relationship toward destination
    //     * 
    //     * @param destination
    //     * @return
    //     */
    //    public boolean removeDepend(Composite destination);

    /**
     * 
     * @return the existing depend relatiponships
     */
    public Set<Composite> getDepend();

    /**
     * 
     * @return the composite that depend on this one.
     */
    public Set<Composite> getInvDepend();

    /**
     * returns true if htis composite depends on "destination"
     * 
     * @param destination
     * @return
     */
    public boolean dependsOn(Composite destination);

    /**
     * 
     * @return the father composite i.e. the composite that contains this one.
     */
    public Composite getFather();

    /**
     * 
     * @return the set of sons of this compositre (i.e. the implementations it ocntains that are composites.
     */
    public Set<Composite> getSons();

    /**
     * returns the model with the "name"
     * 
     * @param name
     * @return
     */
    public ManagerModel getModel(String name);

    /**
     * 
     * @return all the models for that composite type
     */
    public Set<ManagerModel> getModels();

    /**
     * adds a "contains" relationship toward the instance.
     * 
     * @param inst
     */
    public void addContainInst(Instance inst);

    /**
     * retuirn true if the instance is contained in the current one.
     * 
     * @param inst
     * @return
     */
    public boolean containsInst(Instance inst);

    /**
     * return all the instances contained in the current composite.
     * 
     * @return
     */
    public Set<Instance> getContainInsts();

    //    /**
    //     * 
    //     * @return true if the attribute "internalInstances" is set to "true"
    //     */
    //    public boolean isInternal();

}
