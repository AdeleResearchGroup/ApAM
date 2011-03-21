package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.sam.Implementation;

public interface ASMImplBroker {

	/**
	 * return the ASM implementation associated with that sam implementation
	 * @param samImpl
	 * @return
	 */
	public ASMImpl getImpl (Implementation samImpl) ;
	
	/**
	 * add in ASM and existing SAM implem.
	 * @param name the name of the implem, as known by the managers. May be different from SAM
	 * @param impl a SAM implem
	 * @return an ASM implem
	 */
	public ASMImpl addImpl (Composite compo, String name, Implementation samImpl) ;
	
	/**
	 * Deploys and creates both the SAM implem and Spec; and the the corresponding ASM spec and implem
	 * @param name the name of the implem, as known by the managers. May be different from SAM
	 * @param url the location of the executable to deploy
	 * @param type type of executable to deploy (bundle, jar, war, exe ...)
	 * @return an ASM Implem
	 */
	public ASMImpl createImpl (Composite compo, String name, URL url, String type) ; 
	
	
	//=== Adapted from SAM broker
	

  /**
     * Return an (exported) service implementation with the provided name.
     * @param from the pid of the caller
     * @param name of the implementation
     * @return a (exported) service ASMImpl that has the provided name,
     *         null if none.
     * @throws ConnectionException the connection exception
     */
    public ASMImpl getImpl(String implName)
            throws ConnectionException;

    /**
     * Get the implementations.
     * @param from the pid of the caller
     * @return all Service ASMImpls. Null if none.
     */
    public Set<ASMImpl> getImpls() throws ConnectionException,
            ConnectionException;

    /**
     * Return the list of (exported) service implementation that satisfies the
     * goal.
     * @param from the pid of the caller
     * @param goal the filter
     * @return all (exported) service ASMImpls that satisfy the goal.
     *         Null if none.
     * @throws ConnectionException the connection exception
     */
    public Set<ASMImpl> getImpls(Filter goal)
            throws ConnectionException, InvalidSyntaxException;

}
