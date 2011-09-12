package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public interface ASMInst extends Attributes {

    //The composite in which this instance pertains.
    //WARNING : may be null. A root composite, as an ASMInst, does not pertain to any other Composite
    public Composite getComposite();

    //The composite at the end of the "father" relationship chain. 
    public Composite getRootComposite();

    public String getName();

    // The SAM instance associated with this Apam one.
    public Instance getSAMInst();

    /**
     * Apam native client (real iPOJO instances) have an Apam Dependency Handler managing wires. Those instances have a
     * call back toward these real instance.
     * 
     * @param handler
     */
    public void setDependencyHandler(ApamDependencyHandler handler);

    public ApamDependencyHandler getDependencyHandler();

    /**
     * The relation wires is established between two ASM instances. Wires are internally managed, may be different from
     * SAM wires.
     * 
     * @return
     */
    public Set<ASMInst> getWireDests();

    public Wire getWire(ASMInst destInst);

    public Wire getWire(ASMInst destInst, String depName);

    public Set<Wire> getWires(ASMInst destInst);

    public Set<ASMInst> getWireDests(String depName);

    public Set<Wire> getWires(String dependencyName);

    public Set<Wire> getInvWires(String depName);

    public Set<Wire> getWires();

    public Set<Wire> getInvWires();

    /**
     * A new wire has to be instantiated between the current instance and the to instance, for the dependency called
     * dep.
     * If "to" was instantiated during this resolution, deployed is true. Should be false by default.
     * 
     * @param to : the destination of the wire by the depName dependency
     * @param depName : name of the dependency
     * @param deployed : to was deployed (logically or physically) during this resolution. false if unknown.
     * @return
     */
    public boolean createWire(ASMInst to, String depName, boolean deployed);

    public void removeWire(Wire wire);

    public void remove();

    public String getShared();

    public String getScope();

    public ASMSpec getSpec();

    /**
     * Get the implementation.
     * 
     * @return the associated Service implementation
     */
    public ASMImpl getImpl();

    /**
     * Method getServiceObject returns an object that can be casted to the associated interface, and on which the
     * interface methods can be directly called. The object can be the service itself or a proxy for a remote service.
     * It is the fast way for synchronous service invocation. <BR>
     * <b>WARNING: </b> In the case of dynamic service, the service object must not be allocated in
     * 
     * @return the service object, return null, if the object no longer exists.
     */
    public Object getServiceObject();

    /**
     * Match.
     * 
     * @param goal the goal
     * @return true is the instance matches the goal
     */
    public boolean match(Filter goal);

}
