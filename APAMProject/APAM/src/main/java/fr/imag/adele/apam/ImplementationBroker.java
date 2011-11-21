package fr.imag.adele.apam;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

//import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apform.ApformImplementation;

//import fr.imag.adele.apam.util.Attributes;

public interface ImplementationBroker {

    /**
     * return the ASM implementation associated with that sam implementation
     * 
     * @param samImpl
     * @return
     */
    public Implementation getImpl(ApformImplementation apfImpl);

    /**
     * If the sam implementation of name samImplName is found, creates a new implementation, and adds it in the broker.
     * 
     * @param compoType the composite type that will contain that new implementation.
     * @param apfImpl : the name of an implementation in Apform, not created yet .
     * @param properties. The initial properties of that implementation (merged with the sam properties)
     * @return the new created implementation, null if failed.
     */
//    public ASMImpl addImpl(CompositeType compoType, String apfName, Attributes properties);

//    public ASMImpl addImpl(CompositeType compo, ApformImplementation apfImpl, Attributes properties);

    /**
     * Deploys and creates both the SAM implem and Spec; and the the corresponding ASM spec and implem
     * 
     * @param implName the name of implementation to resolve.
     * @param url the location of the executable to deploy
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM Implementation
     */
    public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, Object> properties);

    public void removeImpl(Implementation impl);

    /**
     * Return an implementation with the provided name.
     * 
     * @param the implementation name.
     * @return an ASMImpl that has the provided name, null if none.
     */
    public Implementation getImpl(String implName);

    /**
     * Get all the implementations.
     * 
     * @return all ASMImpls. Null if none.
     */
    public Set<Implementation> getImpls();

    /**
     * Return the list of service implementation that satisfies the goal.
     * 
     * @param goal the filter
     * @return all ASMImpls that satisfy the goal. Null if none.
     * @throws ConnectionException the connection exception
     */
    public Set<Implementation> getImpls(Filter goal) throws InvalidSyntaxException;

    /**
     * returns the list of implementations that implement the specification
     * 
     * @param spec
     * @return
     * @throws InvalidSyntaxException
     */
    public Set<Implementation> getImpls(Specification spec);

}
