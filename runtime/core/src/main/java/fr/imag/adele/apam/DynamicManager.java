package fr.imag.adele.apam;

//import fr.imag.adele.sam.Instance;

public interface DynamicManager {

    /**
     * The manager asks to be notified of the creation of an instance or implem in the ASM (or un-hidden)
     * 
     */
    public abstract void addedComponent(Component newComponent);

    /**
     * The manager asks to be notified of the removing of a an instance or implem (or hidden)from the ASM
     * 
     */
    public abstract void removedComponent(Component lostComponent);

    public abstract void removedWire(Wire wire);
    public abstract void addedWire(Wire wire);

}