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
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.declarations.ResolvableReference;

/**
 * Interface that each relation manager MUST implement. Used by APAM to resolve
 * the dependencies and manage the application.
 * 
 * @author Jacky
 * 
 */

public interface RelationManager extends Manager {
	
	/**
	 * Provided that a relation resolution is required by client, each manager
	 * is asked if it want to be involved. If this manager is not involved, it
	 * does nothing. If involved, it must return the list "selPath" including
	 * itself somewhere (the order is important). It can *add* constraints or
	 * preferences that will used by each manager during the resolution.
	 * 
	 * @param client
	 *            the client asking for a resolution
	 * @param relation
	 *            the relation to resolve. It contains the target type and name;
	 *            and the constraints.
	 * @param selPath
	 *            the managers currently involved in this resolution.
	 */
	public void getSelectionPath(Component source, Relation relation,  List<RelationManager> selPath);

	/**
	 * Performs a complete resolution of the relation.
	 * 
	 * The manager is asked to find the "right" implementations and instances
	 * for the provided relation. If relation is simple (not multiple), returns
	 * a singleton, and a single element in insts.
	 * 
	 * WARNING: can return instances but no implementation, or vice versa (e.g.
	 * implementations are not visible, but their instances are visible).
	 * 
	 * @param client
	 *            the instance asking for the resolution (and where to create
	 *            implementation, if needed). Cannot be null.
	 * @param relation
	 *            a relation declaration containing the type and name of the
	 *            relation target. It can be -the specification Name (new
	 *            SpecificationReference (specName)) -an implementation name
	 *            (new ImplementationRefernece (name) -an interface name (new
	 *            InterfaceReference (interfaceName)) -a message name (new
	 *            MessageReference (dataTypeName)) - or any future resource ...
	 * @param insts
	 *            : an empty set in input, or null. If null, do not try to find
	 *            the instances.
	 * @return the implementations if resolved, null otherwise
	 * @return in insts, the valid instances, null if none.
	 */
	public Resolved<?> resolveRelation(Component source, Relation relation);

	/**
	 * Once the resolution terminated, either successful or not, the managers
	 * are notified of the current selection. Currently, the managers cannot
	 * "undo" nor change the current selection.
	 * 
	 * @param client
	 *            the client of that resolution
	 * @param resName
	 *            : either the interfaceName, the spec name or the
	 *            implementation name to resolve depending on the fact
	 *            newWireSpec or newWireImpl has been called.
	 * @param depName
	 *            : the relation to resolve.
	 * @param impl
	 *            : the implementation selected
	 * @param inst
	 *            : the instance selected (null if cardinality multiple)
	 * @param insts
	 *            : the set of instances selected (null if simple cardinality)
	 */
	public void notifySelection(Component client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts);


	public interface ComponentBundle {
		URL    getBundelURL () ;
		public Set<String> getComponents ();
	}

	public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName);

}