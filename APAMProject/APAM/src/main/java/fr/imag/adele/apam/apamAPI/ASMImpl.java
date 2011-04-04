package fr.imag.adele.apam.apamAPI;

//import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public interface ASMImpl extends Attributes {
    public Composite getComposite();

    public String getASMName();

    public void setASMName(String logicalName);

    public String getSAMName();

    public Implementation getSamImpl();

    /**
     * The relation uses is established between two ASM implementations. On getUses, APAM tries to update the list with
     * the information provided by SAM. If additional uses are found ... If some uses are not in SAM ...
     * 
     * @return
     */

    // public Set<ASMImpl> getUses () ;
    // Uses are dynamically computed from the actual wires.
    // public boolean addUses (ASMImpl impl) ;
    // public boolean removeUses (ASMImpl impl) ;

    public String getShared();

    public String getClonable();

    public void setClonable(String clonable);

    /**
     * remove from ASM but does not try to delete in SAM. The mapping is still valid. It deletes all instances. No
     * change of state. May be selected again later.
     */
    public void remove();

    // ====

    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * The actual new service properties are those provided plus those found in the associated implementation, plus
     * those in the associated specification.
     * <p>
     * It throws exception UnsupportedOperationException if the current implementation object does not support that
     * functionality.
     * 
     * @param initialproperties the initial properties
     * @return the instance
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws ConnectionException the connection exception
     */
    public ASMInst createInst(Attributes initialproperties);

    /**
     * Get the abstract services.
     * 
     * @return the specification that this ASMImpls implements
     * @throws ConnectionException the connection exception
     */
    public ASMSpec getSpec();

    /**
     * Returns the specifications currently used by this implementation.
     * 
     * @return the specification that this ASMImpl requires.
     * @throws ConnectionException the connection exception
     */
    public Set<ASMSpec> getUses();

    /**
     * Returns an instance (ASMInsts)of that Service implementation that satisfies the provided Goal, if existing. Null
     * if not existing. It throws exception UnsupportedOperationException if the current service implementation object
     * does not support that functionality.
     * <p>
     * There is no constraint that an service instance has an Id.
     * 
     * @param name the name
     * @return the service instance
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws ConnectionException the connection exception
     */
    public ASMInst getInst(String name);

    /**
     * Returns all the instances (ASMInsts) of that service implementation Null if not existing. It throws exception
     * UnsupportedOperationException if the current service implementation object does not support that functionality.
     * <p>
     * 
     * @return All instances of that service implementation or null if not existing.
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws ConnectionException the connection exception
     */
    public Set<ASMInst> getInsts();

    /**
     * Returns an instance arbitrarily selected (ASMInsts) of that service implementation Null if not instance are
     * existing. It throws exception UnsupportedOperationException if the current service implementation object does not
     * support that functionality.
     * <p>
     * 
     * @return All instances of that service implementation or null if not existing.
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws ConnectionException the connection exception
     */
    public ASMInst getInst();

    /**
     * Returns all the instances (a SAM object ASMInsts)of that Service implementation that satisfy the provided Goal,
     * if existing. Null if not existing. It throws exception UnsupportedOperationException if the current service
     * implementation object does not support that functionality.
     * <p>
     * 
     * @param goal the goal
     * @return All instances satisfying the goal
     * @throws UnsupportedOperationException the unsupported operation exception
     * @throws ConnectionException the connection exception
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public Set<ASMInst> getInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Checks if is an instantiator.
     * 
     * @return true if method createASMInst is supported
     */
    public boolean isInstantiable();

}
