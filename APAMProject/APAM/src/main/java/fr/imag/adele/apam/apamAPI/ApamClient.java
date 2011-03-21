package fr.imag.adele.apam.apamAPI;

/**
 * Interface called by client instances. 
 * @author Jacky
 *
 */
public interface ApamClient {

	/**
	 * An APAM client instance requires to be wired with an instance implementing the specification.
	 * If found, the instance is returned. 
	 * @param client the instance that requires the specification
	 * @param spec the specification to resolve
	 * @param abort if true, the application should be aborted.
	 * @return
	 */
	public ASMInst newWire (ASMInst client, ASMSpec spec, String depName) ;
	
	/**
	 * An APAM client instance requires to be wired with an instance of implementation.
	 * If found, the instance is returned. 
	 * @param client the instance that requires the specification
	 * @param impl the implementation to resolve
	 * @param depName the dependency name
	 * @param abort if true, the application should be aborted.
	 * @return
	 */
	public ASMInst newWire (ASMInst client, ASMImpl impl, String depName) ;

	/**
	 * In the case a client realizes that a dependency disappeared, it has to call this method.
	 * APAM will try to resolve the problem (DYNAMAM in practice), and return a new instance.
	 * @param client the instance that looses it dependency
	 * @param lostInstance the instance that disappeared.
	 * @param abort if true the application should fail.
	 * @return
	 */
	public ASMInst faultWire (ASMInst client, ASMInst lostInstance, String depName) ;
	
	/**
	 * This method has to be called by a client instance when it is created. 
	 * It allows APAM to know where is the dependency manager attached to the instance. 
	 * This dependency manager (an iPOJO Handler currently) must implement the ApamDependencyHandler interface.   
	 * @param instanceName
	 * @param client
	 */
	public void newClientCallBack (String instanceName, ApamDependencyHandler client) ;
	
	
}
