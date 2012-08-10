package fr.imag.adele.apam;

//import fr.imag.adele.sam.Instance;

public interface DynamicManager extends DependencyManager {

    /**
     * The managers is notified of the apparition of an external instance or implem.
     * Occurs with devices are discovered and third party deployed / started bundles,
     * or when executing the "instance" primitive from an XML definition.
     * 
     */
    public abstract void external(Instance inst);

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

}