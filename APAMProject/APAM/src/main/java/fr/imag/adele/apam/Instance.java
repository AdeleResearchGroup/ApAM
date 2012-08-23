package fr.imag.adele.apam;

import java.util.Set;

import fr.imag.adele.apam.apform.ApformInstance;

public interface Instance extends Component {

    /**
     * Returns the composite to which this instance pertains.
     */
    public Composite getComposite();

    /**
     *  The composite at the end of the "father" relationship chain.
     */
    public Composite getAppliComposite();

    
    /**
     * The Apform instance associated with this Apam one.
     */
    public ApformInstance getApformInst();

    /**
     * returns all the instances this one is wired to.
     */
    public Set<Instance> getWireDests();

    /**
     * returns the wire toward that destination
     * 
     */
    public Wire getInvWire(Instance destInst);

    /**
     * returns the wire for hthe "depName" dependency toward that destination instance
     * 
     * @param destInst : the instance destination of that dependency
     * @param depName : name of the dependency
     * @return
     */
    public Wire getInvWire(Instance destInst, String depName);

    /**
     * Returns all the wires toward that destination.
     * 
     * @param destInst
     * @return
     */
    public Set<Wire> getInvWires(Instance destInst);

    /**
     * returns all the destinations of that dependency (if multiple cardinality)
     * 
     * @param depName
     * @return
     */
    public Set<Instance> getWireDests(String depName);


    /**
     * returns all the wires related to that dependency (if multiple cardinality)
     * 
     * @param depName
     * @return
     */
    public Set<Wire> getWires(String dependencyName);

    /**
     * Returns all the wires, for the provided dependency, leading to the current instance.
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
     * return all the dependency for which the destination implements the provided specification
     * 
     * @param spec
     */
    public Set<Wire> getWires(Specification spec);

    /**
     * Returns all the wires leading to the current instance.
     * 
     * @param depName
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
    public boolean createWire(Instance to, String depName);

    /**
     * remove that wire.
     * 
     * @param wire
     */
    public void removeWire(Wire wire);

    //    /**
    //     * returns the value of the shared attribute
    //     * 
    //     * @return
    //     */
    //    public String getShared();

    /**
     * returns the value of the shared attribute
     * 
     * @return
     */
    public boolean isSharable();

    public boolean isUsed();


    /**
     * returns the specification of that instance
     * 
     */
    public Specification getSpec();

    /**
     * Get the implementation.
     * 
     */
    public Implementation getImpl();

    /**
     * Method getServiceObject returns an object that can be casted to the associated interface, and on which the
     * interface methods can be directly called. The object can be the service itself or a proxy for a remote service.
     * It is the fast way for synchronous service invocation.
     * 
     * @return the service object, return null, if the object no longer exists.
     */
    public Object getServiceObject();

}
