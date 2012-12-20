package fr.imag.adele.apam;

import java.util.Set;


public interface Composite extends Instance {

    /**
     * returns the main instance
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


    /**
     * 
     * @return the existing depend relationships
     */
    public Set<Composite> getDepend();

    /**
     * 
     * @return the composite that depend on this one.
     */
    public Set<Composite> getInvDepend();

    /**
     * returns true if this composite depends on "destination"
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
     * @return the set of sons of this composite (i.e. the implementations it contains that are composites.
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
     * return true if the instance is contained in the current one.
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

}
