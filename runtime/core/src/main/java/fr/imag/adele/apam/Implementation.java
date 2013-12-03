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

import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;

public interface Implementation extends Component {

    /**
     * Creates an instance of that implementation, and initialize its properties
     * with the set of provided properties. The actual new service properties
     * are those provided plus those found in the associated sam implementation,
     * plus those in the associated specification.
     * <p>
     * 
     * @param initialproperties
     *            the initial properties
     * @return the instance
     */
    public Instance createInstance(Composite instCompo,
	    Map<String, String> initialproperties);

    /**
     * return the apfform implementation associated with this implementation
     * (same name)
     * 
     * @return
     */
    public ApformImplementation getApformImpl();

    /**
     * 
     * @return the associated ImplementationDeclaration
     */
    public ImplementationDeclaration getImplDeclaration();

    /**
     * Returns all the composite type that contains this implementation. An
     * implementation is contained in all the composite types that deployed
     * (logically or physically) it.
     * 
     * @return
     */
    public Set<CompositeType> getInCompositeType();

    /**
     * Returns an instance arbitrarily selected (ASMInsts) of that service
     * implementation Null if not instance are existing.
     * 
     * @return An instance of that service implementation or null if not
     *         existing.
     */
    public Instance getInst();

    // /**
    // * Returns the implementation currently used by this implementation.
    // *
    // * @return the implementation that this ASMImpl requires.
    // */
    // public Set<Implementation> getUses();
    //
    // /**
    // * Returns the implementation currently using this implementation.
    // *
    // * @return the implementation that use this ASMImpl .
    // */
    // public Set<Implementation> getInvUses();

    /**
     * Returns the instance (ASMInsts)of that implementation having that name.
     * <p>
     * There is no constraint that an service instance has an Id.
     * 
     * @param name
     *            the name
     * @return the service instance
     */
    public Instance getInst(String name);

    /**
     * Returns all the instances (ASMInsts) of that service implementation.
     * Empty if not existing.
     * 
     * @return All instances of that service implementation.
     */

    public Set<Instance> getInsts();

    /**
     * @return the specification that this ASMImpls implements
     */
    public Specification getSpec();

    public boolean isUsed();

}
