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

import java.util.List;
import java.util.Set;

public interface ApamResolver {

	/**
	 * Ask to update an existing component. It will uninstall the bundle in which this component is located. 
	 * If the new bundle does not contain the same components, it will make some side effects. 
	 * @param toUpdate
	 * @return
	 */
	public void updateComponent (String componentName) ;

	
    /**
     * An APAM client instance requires to be wired with one or all the instance that satisfy the dependency.
     * WARNING : in case of interface or message dependency , since more than one specification can implement the same
     * interface, any specification implementing at least the provided interface (technical name of the interface) will
     * be considered satisfactory.
     * If found, the instance(s) are bound is returned.
     * 
     * @param client the instance that requires the resolution
     * @param depName the dependency name. Field for atomic; spec name for complex dep, type for composite.
     * @return
     */
    public boolean resolveWire(Instance client, String depName);


    /**
     * Look for an implementation with a given name "implName", visible from composite Type compo or compoType.
     * if compo or compoType is null, root composite are assumed
     * 
     * @param compoType
     * @param implName
     * @return
     */
    public Instance       findInstByName(Instance client, String instName);

    public Implementation findImplByName(Instance client, String implName);

    public Specification  findSpecByName(Instance client, String specName);

    public Component findComponentByName(Instance client, String compName);

    /**
     * First looks for the specification defined by its name, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param specName
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     *            number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public Implementation resolveSpecByName(Instance client, String specName, Set<String> constraints, List<String> preferences);

    /**
     * First looks for the specification defined by its interface, and then resolve that specification.
     * Returns the implementation that implement the specification and that satisfies the constraints.
     * 
     * @param compoType : the implementation to return must either be visible from compoType, or be deployed.
     * @param interfaceName. The full name of one of the interfaces of the specification.
     *            WARNING : different specifications may share the same interface.
     * @param interfaces. The complete list of interface of the specification. At most one specification can be
     *            selected.
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum number of preferences, taken in the order, and stopping at the first failure.
     * @return
     */
    public Implementation resolveSpecByInterface(Instance client,  String interfaceName, Set<String> constraints, List<String> preferences);
    public Implementation resolveSpecByMessage  (Instance client,  String messageName,   Set<String> constraints, List<String> preferences);

    /**
     * Look for an instance of "impl" that satisfies the constraints. That instance must be either
     * - shared and visible from "compo", or
     * - instantiated if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @param preferences. If more than one implementation satisfies the constraints, returns the one that satisfies the
     *            maximum
     * @return
     */
    public Instance resolveImpl(Instance client, Implementation impl, Set<String> constraints, List<String> preferences);

    /**
     * Look for all the existing instance of "impl" that satisfy the constraints.
     * These instances must be either shared and visible from "compo".
     * If no existing instance can be found, one is created if impl is visible from the composite type.
     * 
     * @param compo. the composite that will contain the instance, if created, or from which the shared instance is
     *            visible.
     * @param impl
     * @param constraints. The constraints to satisfy. They must be all satisfied.
     * @return
     */
    public Set<Instance> resolveImpls(Instance client, Implementation impl, Set<String> constraints);
    
    


}
