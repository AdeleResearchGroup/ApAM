package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.sam.Instance;

public interface ASMInstBroker  {
	/**
	 * add in ASM and existing SAM Instance.
	 * @param compo The composite in which to create the instance. Can be null if the implem is existing is ASM
	 * @param inst a SAM Instance
	 * @return an ASM Instance
	 */
	public ASMInst addInst (Composite compo, Instance samInst) ;
	
	/**
	 * returns the APAM instance related to the provided sam Instance. 
	 * @param samInst A SAM Instance
	 * @return
	 */
	public ASMInst getInst (Instance samInst) ;
	
    /**
     * Return the instances with that name.
      * @param APAM name the instance
     * @return the service instance
     */
    public ASMInst getInst(String instName) throws ConnectionException;

    /**
     * Returns all the instances. Null if none.
     * @param from pid of the caller
     * @return the service instances
     */
    public Set<ASMInst> getInsts() throws ConnectionException;

    /**
     * Return all the instances that implement the specification and that
     * satisfy the goal. Null if none.
     * @param from pid of the caller
     * @param specification a given specification
     * @param goal a name filter, LDAP filter or semantics filter.
     * @return all the instances that implement the abstract service and that
     *         satisfy the goal
     */
    public Set<ASMInst> getInsts(ASMSpec spec, Filter goal)
            throws ConnectionException, InvalidSyntaxException;


    /**
     * Return all the instances that satisfy the goal. Null if none.
     * 
     * @param goal the goal
     * @return the service instances
     */
    public Set<ASMInst> getInsts (Filter goal) throws ConnectionException,
            InvalidSyntaxException;
 
    /**
     * Remove the instances 
     * 
     */
    public void removeInst (ASMInst inst) throws ConnectionException;

}
