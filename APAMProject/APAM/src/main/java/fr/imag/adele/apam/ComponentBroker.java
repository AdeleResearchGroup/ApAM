package fr.imag.adele.apam;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;

public interface ComponentBroker {
 
     
    /**
     * Returns the component with the given logical name. 
     * 
     * @param name the logical name of the specification
     * @return the component or null if not deployed
     */
    public Component      getComponent(String name);
    public Specification  getSpec(String name);
    public Implementation getImpl(String implName);
    public Instance       getInst(String instName);

    /**
     * Returns all the components.
     * 
     */
   // public Set<Component> getComponents();
    public Set<Specification> getSpecs();
    public Set<Implementation> getImpls();
    public Set<Instance> getInsts();

    /**
     * Returns all the specifications that satisfies the goal. If goal is null
     * all the specifications are supposed to be matched.
     * 
     * @param goal the goal
     * @return the specifications
     */
    //public Set<Component>      getComponents(Filter goal) ;
    public Set<Specification>  getSpecs(Filter goal) ;
    public Set<Implementation> getImpls(Filter goal) ;
    public Set<Instance>       getInsts(Filter goal) ;
    
    /**
     * If the component is not present, wait for the apparition of the component.
     * WARNING: may be locked forever !
     * @param name
     * @return
     */
    public Component getWaitComponent(String name) ;

    
    /// specification

    /**
     * 
     * @param resource
     * @return the first specification that implements the provided resource ! WARNING this is peligrous.
     */
    public Specification getSpecResource(ResolvableReference resource);

    /**
     * Adds a new specification to the Apam state from its underlying platform definition
     * 
     * @param samSpec : A Apform specification.
     */
    public Specification addSpec(ApformSpecification apfSpec);

    /**
     * Creates a specification. 
     * 
     * @param specName the *logical* name of that specification
     * @param interfaces the list of interfaces this spec implements
     * @param properties : The initial properties.
     *            return an ASM Specification
     */
    public Specification createSpec(String specName, Set<ResourceReference> resources,
            Map<String, String> properties);

     
    /**
     * Adds a new implementation to the Apam state from its underlying platform definition
     * 
     * @param compoType the composite type that will contain that new implementation.
     * @param apfImpl : the apform implementation.
     */
    public Implementation addImpl(CompositeType compo, ApformImplementation apfImpl);

    /**
     * Deploys and creates an implementation
     * 
     * @param implName the name of implementation to resolve.
     * @param url the location of the executable to deploy
     * @param properties . optional : the initial properties for that implementation
     * @return an ASM Implementation
     */
    public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, String> properties);

    /**
     * adds in ASM an existing platform Instance.
     * 
     * @param compo The composite in which to create the instance. Cannot be null.
     * @param inst a SAM Instance
     * @param properties . optional : the initial properties
     * @return an ASM Instance
     */
    public Instance addInst(Composite compo, ApformInstance apformInst);


}
