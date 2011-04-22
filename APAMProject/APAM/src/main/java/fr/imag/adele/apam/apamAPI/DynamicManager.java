package fr.imag.adele.apam.apamAPI;

import fr.imag.adele.sam.Instance;

public interface DynamicManager extends Manager {

    /**
     * The managers is notified of the apparition of an instance of ASMImpl or implementing the interface APAM did not
     * create any ASMInst. If needed, the manager should call APAM for an ins / impl creation.
     * 
     * @param samInstance The instance that appeared.
     * @param impl The ASM impl of the instance that appeared (if existing)
     * @param interf If no ASM impl is existing for that instance, the interface is provided. Warning : it is an
     *            interface (the one in the OSGi registry), not a Spec.
     */
    public abstract void appeared(Instance samInstance);

    /**
     * The instance "lost" disappeared. The disappeared ASM instance is turned to the "lost" state (and propagated) If
     * an instance is returned, all clients handlers of the disappeared instance are notified to substitute the
     * disappeared instance buy the new one. If null is returned, all clients handlers of the disappeared instance are
     * notified to remove that wire.
     * 
     * @param lost the ASM instance that disappeared
     */
    public abstract ASMInst lostInst(ASMInst lost);

}