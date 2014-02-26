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


/**
 * Interface that each relation manager MUST implement. Used by APAM to resolve
 * the dependencies and manage the application.
 * 
 * @author Jacky
 * 
 */

public interface RelationManager extends Manager {

	/**
	 * If several relation managers are registered their order is important, so we
	 * use priorities to have some control over this order.
	 * 
	 * NOTE WARNING constants are enumerated in descending order of priority
	 */
	public enum Priority  {
		HIGHEST,
		HIGH,
		MEDIUM,
		LOW,
		LOWEST  
	}
	
	/**
	 * Provided that a relation resolution is required by client, each manager
	 * is asked if it want to be involved. If this manager is not involved, it
	 * does nothing. Even if the manager is not involved in resolution, it can
	 *  *add* constraints or preferences that will used during the resolution.
	 * 
	 * @param relToResolve
	 *            the relation to resolve. It contains the target type and name;
	 *            and the constraints.
	 */
	public boolean beginResolving(RelToResolve relToResolve);


	/**
	 * Performs a complete resolution of the relation.
	 * 
	 * 
	 * @param relToResolve
	 *            a relation declaration containing the type and name of the
	 *            relation target. It can be -the specification Name (new
	 *            SpecificationReference (specName)) -an implementation name
	 *            (new ImplementationRefernece (name) -an interface name (new
	 *            InterfaceReference (interfaceName)) -a message name (new
	 *            MessageReference (dataTypeName)) - or any future resource ...
	 */
	public Resolved<?> resolve(RelToResolve relToResolve);

}
