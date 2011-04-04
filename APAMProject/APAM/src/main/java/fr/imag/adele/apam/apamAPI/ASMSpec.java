package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Specification;

public interface ASMSpec extends Attributes {
    public Composite getComposite();

    public String getASMName();

    public String getSAMName();

    public Specification getSamSpec();

    public String getShared();

    public String getClonable();

    /**
     * remove from ASM but does not try to delete in SAM. The mapping is still valid. It deletes all its
     * Implementations. No change of state. May be selected again later.
     */
    public void remove();

    // == adapted from SAM Specification
    /**
     * Return the first {@link ASMImpl} that implement that Abstract Service and with the specified name If name is
     * null, returns null. If more than one service implementation satisfy the method, an arbitrary one is returned.
     * 
     * @param name the name
     * @return the implementation
     * @throws ConnectionException the connection exception
     */
    public ASMImpl getImpl(String implemName);

    /**
     * Return all the {@link ASMImpl} that implement that Abstract Service. If no services implementation are found,
     * returns null.
     * 
     * @return the implementations
     * @throws ConnectionException the connection exception
     */
    public Set<ASMImpl> getImpls();

    /**
     * Returns all the {@link ASMImpl} that implement that Abstract Service and satisfies the goal If no services
     * implementation are found, returns null.
     * 
     * @param goal If null or empty, no constraints.
     * @return the implementations
     * @throws ConnectionException the connection exception
     * @throws InvalidSyntaxException
     */
    public Set<ASMImpl> getImpls(Filter filter) throws InvalidSyntaxException;

    /**
     * Get the service interface.
     * 
     * @return the interface
     */
    public String[] getInterfaceNames();

    /**
     * Return the list of required abstract services. Null if none.
     * 
     * @return the list of required abstract services. Null if none
     * @throws ConnectionException the connection exception
     */
    public Set<ASMSpec> getUses();

}
