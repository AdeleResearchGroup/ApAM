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

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;

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
	public boolean match(ApamFilter goal);


	/**
	 * return true if the component satisfies the constraints expressed in the reference : in general a dependency.
	 * @param dep
	 * @return
	 */
	public boolean matchDependencyConstraints (Dependency dep) ;
	/**
	 * return true if the instance matches ALL the constraints in the set.
	 *
	 * @param goals
	 * @return
	 */
	public boolean match(Set<String> goals);

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
	 * For sets, the value must be an array of the corresponding types. i.e; Set<Integer>, Set<String> and so on.
	 * 
	 * If the attribute does not exist, of it the value does not correspond to the attribute type, "false" is returned.
	 */
	public boolean setProperty(String attr, Object value);

	/**
	 * Get the value of all the properties of the component, including those in the enclosing
	 * groups
	 */
	public Map<String, Object> getAllProperties();
	
	/**
	 * Get the value of all the properties of the component, including those in the enclosing
	 * groups
	 */
	public Map<String, String> getAllPropertiesString() ;


	/**
	 * Return the type of the attribute, as it is in xml : "${string}" for substitution, set String.
	 * Note use Util.splitType to get the details :  isSet, isSub, type, singletonType and NoSub
	 * @param attr
	 * @return
	 */
	public AttrType getAttrType (String attr) ;
	
	
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
