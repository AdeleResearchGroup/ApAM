package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Set;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public interface ASMImplBroker {

    /**
     * return the ASM implementation associated with that sam implementation
     * 
     * @param samImpl
     * @return
     */
    public ASMImpl getImpl(Implementation samImpl);

    /**
     * If the sam implementation of name samImplName is found, creates a new implementation, and adds it in the broker.
     * 
     * @param compoType the composite type that will contain that new implementation.
     * @param samImplName : the name of an implementation in Sam.
     * @param properties. The initial properties of that implementation (merged with the sam properties)
     * @return the new created implementation, null if failed.
     */
    public ASMImpl addImpl(CompositeType compoType, String samImplName, Attributes properties);

    /**
     * Deploys and creates both the SAM implem and Spec; and the the corresponding ASM spec and implem
     * 
     * @param implName the name of implementation to resolve.
     * @param url the location of the executable to deploy
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM Implementation
     */
    public ASMImpl createImpl(CompositeType compo, String implName, URL url, Attributes properties);

    public void removeImpl(ASMImpl impl);

    /**
     * Return an implementation with the provided name.
     * 
     * @param the implementation name.
     * @return an ASMImpl that has the provided name, null if none.
     */
    public ASMImpl getImpl(String implName);

    /**
     * Get all the implementations.
     * 
     * @return all ASMImpls. Null if none.
     */
    public Set<ASMImpl> getImpls();

    /**
     * Return the list of service implementation that satisfies the goal.
     * 
     * @param goal the filter
     * @return all ASMImpls that satisfy the goal. Null if none.
     * @throws ConnectionException the connection exception
     */
    public Set<ASMImpl> getImpls(Filter goal) throws InvalidSyntaxException;

    /**
     * returns the list of implementations that implement the specification
     * 
     * @param spec
     * @return
     * @throws InvalidSyntaxException
     */
    public Set<ASMImpl> getImpls(ASMSpec spec);

}
