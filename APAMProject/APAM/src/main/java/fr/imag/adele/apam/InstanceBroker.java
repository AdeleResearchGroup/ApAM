package fr.imag.adele.apam;

//import java.util.Properties;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.apform.ApformInstance;

public interface InstanceBroker {

    /**
     * adds in ASM an existing SAM Instance.
     * 
     * @param compo The composite in which to create the instance. Cannot be null.
     * @param inst a SAM Instance
     * @param properties . optional : the initial properties
     * @return an ASM Instance
     */
    public Instance addInst(Composite compo, ApformInstance apformInst, Map<String,Object> properties);

    /**
     * Return the instances with that name.
     * 
     * @param instName name the instance (same as sam name)
     * @return the service instance
     */
    public Instance getInst(String instName);

    /**
     * Returns all the instances. empty if none.
     * 
     * @return the service instances
     */
    public Set<Instance> getInsts();

    //    /**
    //     * Returns all the sharable instances. empty if none.
    //     * 
    //     * @return the service instances
    //     */
    //    public Set<Instance> getSharableInsts();

    /**
     * Return all the instances that implement the specification and that satisfy the goal. Null if none.
     * 
     * @param spec a given specification
     * @param goal a name filter, LDAP filter.
     * @return all the instances that implement the specification and that satisfy the goal
     */
    public Set<Instance> getInsts(Specification spec, Filter goal) throws InvalidSyntaxException;

    /**
     * Return all the instances that satisfy the goal. Null if none.
     * 
     * @param goal the goal
     * @return the service instances
     */
    public Set<Instance> getInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Remove the instances
     * 
     */
    //    public void removeInst(Instance inst);

}
