package fr.imag.adele.apam.apamAPI;

import java.util.Set;

/**
 * Interface that APAM Client instances (handlers, annotation or proxies) MUST implement.
 * Used to manage the wires.
 * 
 * @author Jacky
 * 
 */

public interface ApamDependencyHandler {

//    /**
//     * Give the client its own identity. To be provided as parameter later on to know who calls.
//     * 
//     * @param inst The ASMInstance object in the ASM, representing that client instance.
//     */
//    public void SetIdentifier(ASMInst inst);
//
//    /**
//     * provide the destination real address for the provided dependency.
//     * Usually performed as the return of method newWire (when lazy)
//     * 
//     * @param dependency Name of the dependency (spec name)
//     * @param destInst. Real address of the destination.
//     * @return False if it cannot be performed
//     */
//    public boolean setWire(ASMInst destInst, String depName);
//
//    /**
//     * Remove a wire. That dependency is no longer valid (disappear or other reason)
//     * 
//     * @param dependency name of that dependency
//     * @param destInst the old destination object (if multiple).
//     * @return false if it could not be performed.
//     */
//    public boolean remWire(ASMInst destInst, String depName);
//
//    /**
//     * Change a dependency by another one.
//     * 
//     * @param dependency
//     * @param oldDestInst the previous destination. Can be null if cardinality one.
//     * @param newDestInst The new destination.
//     * @return
//     */
//    public boolean substWire(ASMInst oldDestInst, ASMInst newDestInst, String depName);

}
