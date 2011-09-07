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
     * add in ASM and existing SAM implem.
     * 
     * @param implName the name of implementation to resolve.
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM implementation
     */
    public ASMImpl addImpl(CompositeType compo, String implName, String specName, Attributes properties);

    /**
     * Deploys and creates both the SAM implem and Spec; and the the corresponding ASM spec and implem
     * 
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param url the location of the executable to deploy
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM Implementation
     */
    public ASMImpl createImpl(CompositeType compo, String implName, URL url, String specName, Attributes properties);

    public void removeImpl(ASMImpl impl);

    /**
     * Return an (exported) service implementation with the provided name. For those implementation with no logical
     * name, looks for the SAM name.
     * 
     * @param name the *logical* implementation name, or sam name if no logical name.
     * @return a (exported) service ASMImpl that has the provided name, null if none.
     * @throws ConnectionException the connection exception
     */
    public ASMImpl getImpl(String implName);

    /**
     * Get the implementations.
     * 
     * @return all Service ASMImpls. Null if none.
     */
    public Set<ASMImpl> getImpls();

    /**
     * Return the list of service implementation that satisfies the goal.
     * 
     * @param goal the filter
     * @return all (exported) service ASMImpls that satisfy the goal. Null if none.
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
