package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public interface ASMInst extends Attributes {

    public Composite getComposite();

    public String getASMName();

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

    public boolean createWire(ASMInst to, String depName);

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
