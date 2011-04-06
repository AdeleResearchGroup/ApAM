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
     * @param name the *logical* name of the implem, as known by the managers. May be different from SAM
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param samImpl a SAM implementation
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM implementation
     */
    public ASMImpl
            addImpl(Composite compo, String implName, String samImplName, String specName, Attributes properties);

    /**
     * Deploys and creates both the SAM implem and Spec; and the the corresponding ASM spec and implem
     * 
     * @param implName the *logical* name of implementation to resolve. May be different from SAM. May be null.
     * @param url the location of the executable to deploy
     * @param type type of executable to deploy (bundle, jar, war, exe ...)
     * @param specName the *logical* name of that specification; different from SAM. May be null.
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM Implementation
     */
    public ASMImpl createImpl(Composite compo, String implName, URL url, String type, String specName,
            Attributes properties);

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
     * Returns the ASM implementation with the given sam name. WARNING : that implementation may be present in SAM but
     * not in ASM, in which case return null.
     * 
     * @param samName the sam name of the implementation
     * @return the ASM implementation
     */
    public ASMImpl getImplSamName(String samName);

    /**
     * Get the implementations.
     * 
     * @return all Service ASMImpls. Null if none.
     */
    public Set<ASMImpl> getImpls();

    /**
     * Return the list of (exported) service implementation that satisfies the goal.
     * 
     * @param goal the filter
     * @return all (exported) service ASMImpls that satisfy the goal. Null if none.
     * @throws ConnectionException the connection exception
     */
    public Set<ASMImpl> getImpls(Filter goal) throws InvalidSyntaxException;

    /**
     * returns all implementaitons implementing spec, and with shared = shareable
     * 
     * @param spec the specification of the returned instance
     * @param appli Can be null. The returned instance must be sharable inside the given appli (shared = appli)
     * @param compo Can be null. The returned instance must be sharable inside the given composite (shared = local)
     * @return
     */
    public Set<ASMImpl> getShareds(ASMSpec spec, Application appli, Composite compo);

    /**
     * returns one implementation implementing Spec, and shareable inside appli.
     * 
     * @param spec the specification of the returned instance
     * @param appli Can be null. The returned instance must be sharable inside the given appli (shared = appli)
     * @param compo Can be null. The returned instance must be sharable inside the given composite (shared = local)
     * @return
     */
    public ASMImpl getShared(ASMSpec spec, Application appli, Composite compo);

}
