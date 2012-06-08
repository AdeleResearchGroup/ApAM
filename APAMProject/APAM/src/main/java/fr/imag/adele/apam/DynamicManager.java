package fr.imag.adele.apam;

//import fr.imag.adele.sam.Instance;

public interface DynamicManager extends Manager {

    /**
     * The managers is notified of the apparition of an instance.
     * 
     */
    public abstract void appeared(Instance inst);
    
    /**
     * The manager is notified of the creation of a new instance
     * 
     */
    public abstract void instantiated(Instance inst);

    /**
     * The manager is notified of the disappearance of a an instance 
     * 
     */
    public abstract void disappeared(Instance lost);
    
    /**
     * The manager is notified of the destruction of a an instance 
     * 
     */
    public abstract void deleted(Instance lost);
    
    /**
     * The manager is notified of the deployment of an implementation
     */
    public abstract void deployed(CompositeType composite, Implementation implementation);
    
    /**
     * The manager is notified of the uninstall of an implementation
     */
    public abstract void uninstalled(CompositeType composite, Implementation implementation);
    
    /**
     * The manager is notified of the changed visibility of an implementation
     */
    public abstract void hidden(CompositeType composite, Implementation implementation);
    
}