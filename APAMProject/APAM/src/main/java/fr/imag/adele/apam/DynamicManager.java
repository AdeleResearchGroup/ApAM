package fr.imag.adele.apam;

//import fr.imag.adele.sam.Instance;

public interface DynamicManager extends Manager {

    /**
     * The managers is notified of the apparition of an instance of ASMImpl or implementing the interface.
     * 
     * @param inst The instance that appeared.
     */
    public abstract void appeared(Instance inst);

    /**
     * The instance "lost" disappeared. The disappeared ASM instance is turned to the "lost" state (and propagated) If
     * an instance is returned, all clients handlers of the disappeared instance are notified to substitute the
     * disappeared instance the new one. If null is returned, all clients handlers of the disappeared instance are
     * notified to remove that wire.
     * 
     * @param lost the ASM instance that disappeared
     */
    public abstract Instance lostInst(Instance lost);

}