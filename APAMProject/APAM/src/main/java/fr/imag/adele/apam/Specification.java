package fr.imag.adele.apam;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apform.ApformSpecification;
//import fr.imag.adele.apam.util.Attributes;

//import fr.imag.adele.sam.Specification;

public interface Specification extends ConcurrentMap<String, Object> {

    public String getName();

    public String[] getInterfaces();

    /**
     * return the apform specificatrion (if existing !!) associated with this specification.
     * 
     * @return
     */
    public ApformSpecification getApformSpec();

    /**
     * remove from ASM but does not try to delete in SAM. It deletes all its
     * Implementations. No change of state. May be selected again later.
     */
    public void remove();

    /**
     * Return the implementation that implement that specification and has the provided name.
     * 
     * @param implemName the name
     * @return the implementation
     */
    public Implementation getImpl(String implemName);

    /**
     * Return all the implementation of that specification. If no services implementation are found,
     * returns null.
     * 
     * @return the implementations
     * @throws ConnectionException the connection exception
     */
    public Set<Implementation> getImpls();

    /**
     * Returns all the implementations that satisfy the goal. Null if none.
     * 
     * @param goal If null or empty, no constraints.
     * @return the implementations
     * @throws InvalidSyntaxException
     */
    public Set<Implementation> getImpls(Filter filter) throws InvalidSyntaxException;

    /**
     * Returns all the implementations that satisfy all the constraints. Null if none.
     * 
     * @param constraint : the set of filters. If null or empty, no constraints.
     * @return the implementations
     */
    public Set<Implementation> getImpls(Set<Filter> constraints);

    /**
     * Returns the subset of "candidates" that satisfy all the constraints. Null if none.
     * 
     * @param constraint : the set of filters. If null or empty, no constraints.
     * @return the implementations
     */
    public Set<Implementation> getImpls(Set<Implementation> candidates, Set<Filter> constraints);

    /**
     * Return the implementation that matches the constraints and that best satisfies the preferences.
     * 
     * @param constraints
     * @param preferences
     * @return
     */
    public Implementation getImpl(Set<Filter> constraints, List<Filter> preferences);

    /**
     * returns the implementation, in the candidate set, that best satisfies the preferences.
     * 
     * @param candidates
     * @param preferences
     * @return
     */
    public Implementation getPreferedImpl(Set<Implementation> candidates, List<Filter> preferences);

    /**
     * Get the service interface.
     * 
     * @return the interface
     */
    public String[] getInterfaceNames();

    /**
     * Return the list of currently required specification.
     * 
     * @return the list of currently required specification. Null if none
     */
    public Set<Specification> getRequires();

    /**
     * Return the list of specification that currently require that spec.
     * 
     * @return the list of specifications using that spec. Null if none
     * @throws ConnectionException the connection exception
     */
    public Set<Specification> getInvRequires();

    public boolean match(Filter goal);

}
