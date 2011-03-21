package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.am.Property;
import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.sam.Instance;

public interface ASMInst extends Property {
	
	public Composite getComposite () ;
	
	public String getASMName () ;
	
	// The SAM instance associated with this Apam one.
	public Instance getSAMInst () ;
	
	/**
	 * Apam native client (real iPOJO instances) have an Apam Dependency Handler managing wires.
	 * Those instances have a call back toward these real instance. 
	 * @param handler
	 */
	public void setDependencyHandler (ApamDependencyHandler handler) ;
	public ApamDependencyHandler getDependencyHandler () ;
	
	/**
	 * The relation wires is established between two ASM instances as consequences of resolutions and state.
	 * Wires are internally managed, may be different from SAM wires.
	 * If additional wires are found ... TBD
	 * If some wires are not in SAM ... TBD
	 * @return
	 */
	public Set<ASMInst> getWires () ; 
	public Wire getWire (ASMInst detInst) ;
	public boolean setWire (ASMInst to, String depName, Set<Filter> constraints) ;
	public boolean setWire (ASMInst to, String depName, Filter filter) ;
	public void removeWire (ASMInst inst, String depName) ; 
	public void substWire (ASMInst oldTo, ASMInst newTo, String depName) ;

	/**
	 * remove from ASM but does not try to delete in SAM. The mapping is still valid.
	 * It deletes the wires, and turns to "idle" the isolated instances, and transitively.
	 * It deleted the invWires, which turns the callers in the "fault" mode : 
	 * 		next call will try to resolve toward another instance. 
	 */
	public void remove () ;
	public void lost () ;
	
	public int getShared () ;
	public void setShared (int shared) ; //must be less or equal than its implem state
	public int getClonable () ;
	public void setClonable (int clonable) ;
	public Set<ASMInst> getClients () ;
	
	//== from SAM interface
    public ASMSpec getSpec() throws ConnectionException;

    /**
     * Delete this instance. Warning, this method can raise an
     * {@link UnsupportedOperationException}.
     * 
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws ConnectionException the connection exception
     */
    public void delete() throws UnsupportedOperationException,
            ConnectionException;

    /**
     * Get the implementation.
     * 
     * @return the associated Service implementation
     */
    public ASMImpl getImpl() throws ConnectionException;

    /**
     * Method getName return the Id of that service implementation. If the
     * current service has no Id, the String "UNNAMED" is returned.
     * 
     * @return the name
     */
    public String getName();

     /**
     * Method getServiceObject returns an object that can be casted to the
     * associated interface, and on which the interface methods can be directly
     * called. The object can be the service itself or a proxy for a remote
     * service. It is the fast way for synchronous service invocation. <BR>
     * <b>WARNING: </b> In the case of dynamic service, the service object must
     * not be allocated in
     * 
     * @return the service object, return null, if the object no longer exists.
     */
    public Object getServiceObject() throws ConnectionException;
     
    /**
     * Match.
     * 
     * @param goal the goal
     * @return true is the instance matches the goal
     */
    public boolean match(Filter goal);

    /**
     * This method must be call after the use a service (getServiceObject).<BR>
     * Releases the service object referenced.
     * 
     * @throws ConnectionException the connection exception
     */
    public void ungetService() throws ConnectionException;

}


