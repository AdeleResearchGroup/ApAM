package fr.imag.adele.apam.apamAPI;

//import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public interface ASMInstBroker {

    /**
     * adds in ASM and existing SAM Instance.
     * 
     * @param instCompo The composite in which to create the instance. Cannot be null.
     * @param implCompo The composite in which to create the implementation. Can be null if the implem is existing is
     *            ASM
     * @param inst a SAM Instance
     * @param implName the *logical* name of the associated implementation. May be different from SAM. May be null.
     * @param specName the *logical* name of the associated specification; different from SAM. May be null.
     * @param properties . optional : the initial properties
     * @return an ASM Instance
     */
    public ASMInst addInst(Composite implCompo, Composite instCompo, Instance samInst, String implName,
            String specName,
            Attributes properties);

    /**
     * returns the APAM instance related to the provided sam Instance.
     * 
     * @param samInst A SAM Instance
     * @return
     */
    public ASMInst getInst(Instance samInst);

    /**
     * Return the instances with that name.
     * 
     * @param instName name the instance (same as sam name)
     * @return the service instance
     */
    public ASMInst getInst(String instName);

    /**
     * Returns all the instances. Null if none.
     * 
     * @return the service instances
     */
    public Set<ASMInst> getInsts();

    /**
     * Return all the instances that implement the specification and that satisfy the goal. Null if none.
     * 
     * @param spec a given specification
     * @param goal a name filter, LDAP filter.
     * @return all the instances that implement the specification and that satisfy the goal
     */
    public Set<ASMInst> getInsts(ASMSpec spec, Filter goal) throws InvalidSyntaxException;

    /**
     * Return all the instances that satisfy the goal. Null if none.
     * 
     * @param goal the goal
     * @return the service instances
     */
    public Set<ASMInst> getInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Remove the instances
     * 
     */
    public void removeInst(ASMInst inst);

}
