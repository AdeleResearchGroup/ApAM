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

import org.osgi.framework.Filter;

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;

public interface Component {

	/**
	 * The name of the component
	 */
	public String getName();

	/**
	 * The underlying entity in the execution platform
	 */
	public ApformComponent getApformComponent();

	/**
	 * The component declaration
	 */
	public ComponentDeclaration getDeclaration () ;

	/**
	 * Give the composite type that physically deployed that component.
	 * Warning : null when unused. 
	 */
	public CompositeType getFirstDeployed() ;

	/**
	 * Whether the component is instantiable
	 */
	public boolean isInstantiable() ;

	/**
	 * Whether the component is singleton
	 */
	public boolean isSingleton() ;

	/**
	 * Whether the component is shared
	 */
	public boolean isShared() ;


	/**
	 * Match.
	 *
	 * @param goal the goal
	 * @return true is the instance matches the goal
	 */
	public boolean match(String goal);

	/**
	 * Match.
	 *
	 * @param goal the goal
	 * @return true is the instance matches the goal
	 */
	public boolean match(Filter goal);

	/**
	 * return true if the instance matches ALL the constraints in the set.
	 *
	 * @param goals
	 * @return
	 */
	public boolean match(Set<Filter> goals);

	/**
	 * Get the value of a property, the property can be valued in this component or in its
	 * defining group
	 */
	public String getProperty(String attribute);

	/**
	 * Get the value of a property, the property can be valued in this component or in its
	 * defining group
	 * Return will be an object of type int, String, boolean for attributes declared int, String, boolean
	 * 					String for an enumeration.
	 * 
	 * For sets, the return will be an array of the corresponding types. i.e; int[], String[] and so on.
	 */
	public Object getPropertyObject (String attribute);

	/**
	 * Set the value of a property, the property can be valued in this component or in its
	 * defining group
	 * Value must be an int, String, boolean for attributes declared int, String, boolean
	 * 					String for an enumeration.
	 * 
	 * For sets, the value must be an array of the corresponding types. i.e; int[], String[] and so on.
	 * 
	 * If the attribute does not exist, of it the value does not correspond to the attribute type, "false" is returned.
	 */
	public boolean setPropertyObject (String attribute, Object value);

	/**
	 * Set the value of the property for this component
	 */
	public boolean setProperty(String attr, String value);

	/**
	 * Get the value of all the properties of the component, including those in the enclosing
	 * groups
	 */
	public Map<String, Object> getAllProperties();

	/**
	 * Change the value of the specified properties of the component
	 */
	public boolean setAllProperties(Map<String, String> properties);

	/**
	 * Removes the specified property of the component
	 */
	public boolean removeProperty(String attr);

	/**
	 * return all the members of this component. Null if leaf (instance).
	 * @return
	 */
	public Set<? extends Component> getMembers ();

	/**
	 * return the representant of this group member. Null if root (Specification)
	 */
	public Component getGroup ();

	public Map<String, String> getValidAttributes () ;

	public Set<ResourceReference> getAllProvidedResources () ;


}
