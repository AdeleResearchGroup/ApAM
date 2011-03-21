package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.am.query.Query;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Specification;

public interface ASMSpecBroker {
	/**
	 * 
	 * @param name the name of the spec, as known by the managers. May be different from SAM
	 * @param samSpec : A SAM specification.
	 * return an ASM Spec
	 */
	public ASMSpec addSpec (Composite compo, String name, Specification samSpec) ;
	//public Specification createSpec (Specification Spec) ;
	
	
	
	/**
	 * return the ASM specification associated with that sam specification
	 * @param samImpl
	 * @return
	 */
	public ASMSpec getSpec (Specification samSpec) ;
	
    /**
     * Called from the local machine, it allows to get information of the
     * distant Machine. returns the exported specification that this Abstract
     * Service requires. WARNING : The required abstract services that are not
     * exported are not returned.
     * @param specification the specification
     * @return The (exported) abstract services required as known by the distant
     *         machine.
     * @throws ConnectionException the connection exception
     */
    public Set<ASMSpec> getUses(ASMSpec specification)
            throws ConnectionException;


    /**
     * Returns the first abstract service that satisfies the goal. If goal is
     * null all the abstract services are supposed to be matched.
      * @param goal the goal
     * @return the abstract service
     * @throws ConnectionException the connection exception Returns the
     *             ExportedSpecification exported by this Machine that satisfies
     *             the goal. If goal is null, returns an arbitrary exported
     *             AbstractService.
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public ASMSpec getSpec(Filter goal)
            throws ConnectionException, InvalidSyntaxException;

    /**
     * Returns the first abstract service that satisfies support all the provided interfaces.
     * 
     * WARNING : the same specification can be implemented by different technologies (SCM). 
     * The list of interfaces is the minimum required to implement a specification. 
     * 
     * @param interfaces : the interfaces of the required specification. 
     * The returned specification must support all the interfaces in the array.
     * The order in which the interfaces are found in the array is nor relevant.
     * Cannot be null nor empty.
     * 
     * @return the abstract service
     * @throws ConnectionException the connection exception Returns the
     *             ExportedSpecification exported by this Machine that satisfies
     *             the interfaces.
     */    
    public ASMSpec getSpec(String [] interfaces)throws ConnectionException ;
 
    
     /**
     * Returns the abstract service with the given name. If name is null returns
     * null.
     * @param name the name
     * @return the abstract service
     * @throws ConnectionException the connection exception Returns the
     *             ExportedAbstractService exported by this Machine with this
     *             name. If name is null, returns null.
     */
    public ASMSpec getSpec(String name)
            throws ConnectionException;

    /**
     * Returns all the abstract service. If no abstract service are matched,
     * returns null.
     * @return the abstract services
     * @throws ConnectionException the connection exception Returns all the
     *             {@link ExportedAbstractService} exported by this Machine. If
     *             none, returns null.
     */

    public Set<ASMSpec> getSpecs() throws ConnectionException;


    /**
     * Returns all the abstract service that satisfies the goal. If goal is null
     * all the abstract services are supposed to be matched. If no abstract
     * service are matched, returns null.
     * @param goal the goal
     * @return the abstract services
     * @throws ConnectionException the connection exception Returns all the
     *             {@link ExportedAbstractService} exported by this Machine that
     *             satisfies the goal. If goal is null, returns all the exported
     *             AbstractService. If none, returns null.
     */
    public Set<ASMSpec> getSpecs(Filter goal)
            throws ConnectionException, InvalidSyntaxException;


    /**
     * Called from the local machine, it allows to get information of the
     * distant Machine. Returns the Abstract Services PID required by that
     * abstract Service on that the Service Implementation implements. WARNING :
     * The required abstract services that are not exported are not returned.
     * @param specification the specification
     * @return the require remote
     * @throws RemoteException *
     * @throws ConnectionException the connection exception
     * @throws ConnectionException the connection exception
     */
    public Set<ASMSpec> getUsesRemote(ASMSpec specification)
            throws ConnectionException;



	
}
