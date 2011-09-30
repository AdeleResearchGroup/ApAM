package fr.imag.adele.apam.apamAPI;

//import java.util.Properties;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public interface ASMImpl extends Attributes {

    /**
     * Returns all the composite type that contains this implementation.
     * An implementation is contained in all the composite types that deployed (logically or physically) it.
     * 
     * @return
     */
    public Set<CompositeType> getInCompositeType();

    public String getName();

    /**
     * return the sam implementation associated with this implementation (same name)
     * 
     * @return
     */
    public Implementation getSamImpl();

    /**
     * return the value of the shared attribute
     * 
     * @return
     */
    public String getShared();

    /**
     * returns the vazlue of the scope attribute
     * 
     * @return
     */
    public String getScope();

    public void remove();

    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * The actual new service properties are those provided plus those found in the associated sam implementation, plus
     * those in the associated specification.
     * <p>
     * 
     * @param initialproperties the initial properties
     * @return the instance
     */
    public ASMInst createInst(Composite compo, Attributes initialproperties);

    /**
     * @return the specification that this ASMImpls implements
     */
    public ASMSpec getSpec();

    /**
     * Returns the implementation currently used by this implementation.
     * 
     * @return the implementation that this ASMImpl requires.
     */
    public Set<ASMImpl> getUses();

    /**
     * Returns the implementation currently using this implementation.
     * 
     * @return the implementation that use this ASMImpl .
     */
    public Set<ASMImpl> getInvUses();

    /**
     * Returns the instance (ASMInsts)of that implementation having that name.
     * <p>
     * There is no constraint that an service instance has an Id.
     * 
     * @param name the name
     * @return the service instance
     */
    public ASMInst getInst(String name);

    /**
     * Returns all the instances (ASMInsts) of that service implementation Null if not existing.
     * 
     * @return All instances of that service implementation or null if not existing.
     */
    public Set<ASMInst> getInsts();

    /**
     * Returns an instance arbitrarily selected (ASMInsts) of that service implementation Null if not instance are
     * existing.
     * 
     * @return An instance of that service implementation or null if not existing.
     */
    public ASMInst getInst();

    /**
     * Returns all the instances of that implementation that satisfy the provided Goal,
     * if existing. Null if not existing.
     * 
     * @param goal the goal
     * @return All instances satisfying the goal
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public Set<ASMInst> getInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns all the instances of that implementation that satisfy all the filters.
     * Null if not existing.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     */
    public Set<ASMInst> getInsts(Set<Filter> constraints);

    /**
     * Returns all the instances of that implementation that satisfy all the filters.
     * Null if not existing.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     * @param preferences. Return the instance that matches the maximum number of constraints, taken in the order
     */

    public ASMInst getInst(Set<Filter> constraints, List<Filter> preferences);

    /**
     * Among the instance in "candidates", return the instance that matches the maximum number of constraints, taken in
     * the order
     * 
     * @param preferences. The preferences ordered from the most important (first).
     */
    public ASMInst getPreferedInst(Set<ASMInst> candidates, List<Filter> preferences);

    /**
     * Checks if is an instantiator.
     * 
     * @return true if method createASMInst is supported
     */
    public boolean isInstantiable();

}
