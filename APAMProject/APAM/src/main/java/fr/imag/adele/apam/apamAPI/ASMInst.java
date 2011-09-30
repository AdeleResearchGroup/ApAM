package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public interface ASMInst extends Attributes {

    /**
     * Returns the composite to which this instance pertains.
     */
    public Composite getComposite();

    // The composite at the end of the "father" relationship chain.
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
     * The relation wires is established between two ASM instances. Wires are internally managed.
     */

    /**
     * returns all the instances this one is wired to.
     */
    public Set<ASMInst> getWireDests();

    /**
     * returns the wire toward that destination
     * 
     * @param destInst
     * @return
     */
    public Wire getWire(ASMInst destInst);

    /**
     * returns the wire for hte "depName" dependency toward that destination instance
     * 
     * @param destInst : the instance destination of that dependency
     * @param depName : name of the dependency
     * @return
     */
    public Wire getWire(ASMInst destInst, String depName);

    /**
     * Retruns all hte wires toward that destination.
     * 
     * @param destInst
     * @return
     */
    public Set<Wire> getWires(ASMInst destInst);

    /**
     * returns all the destinations of that dependency (if multiple cardinality)
     * 
     * @param depName
     * @return
     */
    public Set<ASMInst> getWireDests(String depName);

    /**
     * returns all the wires related to that dependency (if multiple cardinality)
     * 
     * @param depName
     * @return
     */
    public Set<Wire> getWires(String dependencyName);

    /**
     * Returns all the wires, for hte provided dependency, leading to the current instance.
     * 
     * @param depName
     * @return
     */
    public Set<Wire> getInvWires(String depName);

    /**
     * returns all the wire from the current instance
     * 
     * @return
     */
    public Set<Wire> getWires();

    /**
     * Returns all the wires leading to the current instance.
     * 
     * @param depName
     * @return
     */
    public Set<Wire> getInvWires();

    /**
     * A new wire has to be instantiated between the current instance and the to instance, for the dependency called
     * depName.
     * 
     * @param to : the destination of the wire by the depName dependency
     * @param depName : name of the dependency
     * @return
     */
    public boolean createWire(ASMInst to, String depName);

    /**
     * remove that wire.
     * 
     * @param wire
     */
    public void removeWire(Wire wire);

    /**
     * returns the value of the shared attribute
     * 
     * @return
     */
    public String getShared();

    /*
     * returns the value of hte scope attribute
     */
    public String getScope();

    /**
     * returns the specification of that instance
     * 
     * @return
     */
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
