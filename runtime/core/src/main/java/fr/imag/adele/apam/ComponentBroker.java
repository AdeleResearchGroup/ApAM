/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam;

import java.net.URL;
import java.util.Map;
import java.util.Set;

//import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;

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

    public Instance getInstService (Object service) ; 
    
//    public void disappearedComponent (Component component) ;
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
    public Set<Specification>  getSpecs(ApamFilter goal) ;
    public Set<Implementation> getImpls(ApamFilter goal) ;
    public Set<Instance>       getInsts(ApamFilter goal) ;
    
    /**
     * If the component is not present, wait for the apparition of the component.
     * WARNING: may be locked forever !
     * @param name
     * @return
     */
    public Component getWaitComponent(String name) ;

    /**
     * If the component is not present, wait for the apparition of the component.
     * WARNING: may be locked forever !
     * @param name
     * @param timeout
     * @return
     */
    public Component getWaitComponent(String name,long timeout) ;


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


    /**
     *  Uninstall the bundle containing the provided component.
     *  WARNING: It removes also all the components inside that bundle.
     *  WARNING: It removes the wires towards the removed component, but at next access to 
     *  these components, Apam may re-install the same bundle (or another one having the needed component). 
     * @param component
     */
	public void componentBundleRemove (Component component);

}
