package fr.imag.adele.apam;

//import fr.imag.adele.sam.Instance;

public interface DynamicManager extends Manager {

    /**
     * The managers is notified of the apparition of an instance.
     * 
     */
    public abstract void external(Instance inst);


    //    /**
    //     * The manager is notified of the creation of a new instance
    //     * 
    //     */
    //    public abstract void instantiated(Instance inst);

    /**
     * The manager asks to be notified of the creation of an instance or implem in the ASM (or un-hidden)
     * 
     */
    public abstract void addedInApam(Instance newInstance);

    public abstract void addedInApam(Implementation newImplem);

    /**
     * The manager asks to be notified of the removing of a an instance or implem (or hidden)from the ASM
     * 
     */
    public abstract void removedFromApam(Instance lostInstance);

    public abstract void removedFromApam(Implementation lostImplem);

    //    /**
    //     * The manager is notified of the deployment of an implementation
    //     */
    //    public abstract void deployed(CompositeType composite, Implementation implementation);
    //    
    //    /**
    //     * The manager is notified of the uninstall of an implementation
    //     */
    //    public abstract void uninstalled(CompositeType composite, Implementation implementation);
    //    
    //    /**
    //     * The manager is notified of the changed visibility of an implementation
    //     */
    //    public abstract void hidden(CompositeType composite, Implementation implementation);

}