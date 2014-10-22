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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;

/**
 */
public interface Component {

	/**
	 * Whether the component can see the target component (visibility control)
	 * 
	 * @param target
	 * @return
	 */
	boolean canSee(Component target);

	/**
	 * A new Link has to be instantiated between the current Component and the
	 * "to" Component, for the relation depName.
	 * 
	 * @param to
	 *            target link
	 * @param dep
	 *            : relation name
	 * @param hasConstraints
	 *            : true if the Relation definition has contraints
	 * @param promotion
	 *            true if it is a promotion
	 * @return true if the link has been created
	 */
	boolean createLink(Component to, RelToResolve dep, boolean hasConstraints, boolean promotion);

	/**
	 * Get the value of all the properties of the component, including those in
	 * the enclosing groups. WARNING : substitutions are not performed.
	 */
	Map<String, Object> getAllProperties();

	/**
	 * Get the value of all the properties of the component, including those in
	 * the enclosing groups The values are transformed into string, but without
	 * substitution
	 */
	Map<String, String> getAllPropertiesString();

	/**
	 * The underlying entity in the execution platform
	 * 
	 * @return
	 */
	ApformComponent getApformComponent();

	/**
	 * Return the type of the attribute, as it is in xml : "${string}" for
	 * substitution, set String. Note use Util.splitType to get the details :
	 * isSet, isSub, type, singletonType and NoSub
	 * 
	 * @param attr
	 * @return
	 */
	AttrType getAttrType(String attr);

	/**
	 * The component declaration
	 * 
	 * @return
	 */
	ComponentDeclaration getDeclaration();

	/**
	 * Give the composite type that physically deployed that component. Warning
	 * 
	 * @return null when unused.
	 */
	CompositeType getFirstDeployed();

	/**
	 * @return the representant of this group member. Null if root
	 *         (Specification)
	 */
	Component getGroup();

	/**
	 * returns the Link toward that destination
	 * 
	 * @param destInst
	 * @return the Link toward that destination
	 */
	Link getInvLink(Component destInst);

	/**
	 * returns the Link for the "depName" link toward that destination Component
	 * 
	 * @param destInst
	 * @param depName
	 * @return the Link for the "depName" link toward that destination Component
	 */
	Link getInvLink(Component destInst, String depName);

	/**
	 * Returns all the Links leading to the current Component.
	 * 
	 * @return all the Links leading to the current Component.
	 */
	Set<Link> getInvLinks();

	/**
	 * Returns all the Links toward that destination.
	 * 
	 * @param destInst
	 * @return all the Links toward that destination.
	 */
	Set<Link> getInvLinks(Component destInst);

	/**
	 * Returns all the Links, for the provided relation, leading to the current
	 * Component.
	 * 
	 * @param depName
	 * @return all the Links, for the provided relation, leading to the current
	 *         Component.
	 */
	Set<Link> getInvLinks(String depName);

	// ====================== Links ==========================

	/**
	 * Return it kind: Specification, implementation or instance
	 * 
	 * @return it kind: Specification, implementation or instance
	 */
	ComponentKind getKind();

	/**
	 * returns a Link with that name (arbitrary if multiple cardinality)
	 * 
	 * @param depName
	 * @return a Link with that name (arbitrary if multiple cardinality)
	 */
	Link getLink(String depName);

	/**
	 * returns the destinations of that relation (if simple cardinality)
	 * 
	 * @param depName
	 * @return the destinations of that relation (if simple cardinality)
	 */
	Component getLinkDest(String depName);

	/**
	 * returns all the destinations of that relation (if multiple cardinality)
	 * 
	 * @param depName
	 * @return all the destinations of that relation (if multiple cardinality)
	 */
	Set<Component> getLinkDests(String depName);

	/**
	 * returns all the Links with that name (if multiple cardinality)
	 * 
	 * @param depName
	 * @return all the Links with that name (if multiple cardinality)
	 */
	Set<Link> getLinks(String depName);

	/**
	 * Get all the dependencies defined at that component level. Empty if none.
	 * Return an unmodifiable collection of dependencies
	 * 
	 * @return an unmodifiable collection of dependencies
	 */
	Collection<RelationDefinition> getLocalRelations();

	/**
	 * return all the members of this component. Null if leaf (instance).
	 * 
	 * @return all the members of this component. Null if leaf (instance).
	 */
	Set<? extends Component> getMembers();

	/**
	 * The name of the component
	 * 
	 * @return The name of the component
	 */
	String getName();

	/**
	 * Get the value of a property, the property can be valued in this component
	 * or in its defining group
	 * 
	 * @param attribute
	 * @return
	 */
	String getProperty(String attribute);

	/**
	 * Tries to find the definition of a property, the property can be valued in this component
	 * or in its defining group
	 * 
	 * @param attribute
	 * @return
	 */
	public PropertyDefinition getPropertyDefinition(String attribute);

	/**
	 * Get the value of a property, the property can be valued in this component
	 * or in its defining group Return will be an object of type int, String,
	 * boolean for attributes declared int, String, boolean String for an
	 * enumeration. For sets, the return will be an array of the corresponding
	 * types. i.e; int[], String[] and so on.
	 */
	Object getPropertyObject(String attribute);

	/**
	 * Returns the provided resources, including those inherited.
	 * 
	 * @return the provided resources, including those inherited.
	 */
	Set<ResourceReference> getProvidedResources();

	/**
	 * returns all the instances this one is Linkd to.
	 * 
	 * @return all the instances this one is Linkd to.
	 */
	Set<Component> getRawLinkDests();

	// ================== Relationships =================

	/**
	 * returns all the Link from the current Component
	 * 
	 * @return all the Link from the current Component
	 */
	Set<Link> getRawLinks();

	/**
	 * Get the relation that can be applied to this component with this id,
	 * including those coming from composite if any. null if not defined
	 * 
	 * @param id
	 * @return
	 */
	RelationDefinition getRelation(String id);

	/**
	 * Get all the dependencies that can be applied to this component, including
	 * those coming from composite if any. Empty if none
	 * 
	 * @return
	 */
	Set<RelationDefinition> getRelations();

	/**
	 * @return
	 */
	Map<String, String> getValidAttributes();

	/**
	 * Whether this component is an anscestor of the specified member
	 * 
	 * @param member
	 * @return
	 */
	boolean isAncestorOf(Component member);

	/**
	 * Whether this component is a descendant of the specified group
	 * 
	 * @param group
	 * @return
	 */
	boolean isDescendantOf(Component group);

	/**
	 * Whether the component is instantiable
	 * 
	 * @return
	 */
	boolean isInstantiable();

	/**
	 * Whether the component is shared
	 * 
	 * @return
	 */
	boolean isShared();

	// ==================== Properties =============

	/**
	 * Whether the component is singleton
	 * 
	 * @return
	 */
	boolean isSingleton();

	/**
	 * return true if the component matches the filter
	 * 
	 * @param goal
	 * @return true if the component matches the filter
	 */
	boolean match(ApamFilter goal);

	/**
	 * Return true if the component matches the filter
	 * 
	 * @return true if the component matches the filter
	 */
	boolean match(String goal);

	/**
	 * return true if the component matches the relation (target and
	 * constraints)
	 * 
	 * @param dep
	 * @return true if the component matches the relation (target and
	 *         constraints)
	 */
	boolean matchRelation(RelToResolve dep);

	/**
	 * return true if the component matches the constraints contained in the
	 * relation filters
	 * 
	 * @param dep
	 * @return true if the component matches the constraints contained in the
	 *         relation filters
	 */
	boolean matchRelationConstraints(RelToResolve dep);

	/**
	 * Return true if the component matches the Target of this relation
	 * 
	 * @param dep
	 * @return True if the component matches the Target of this relation
	 */
	boolean matchRelationTarget(RelToResolve dep);

	/**
	 * Removes the specified property of the component
	 */
	boolean removeProperty(String attr);

	/**
	 * Change the values of the specified properties of the component
	 */
	boolean setAllProperties(Map<String, String> properties);

	/**
	 * Set the value of a property, the property can be valued in this component
	 * or in its defining group Value must be an int, String, boolean for
	 * attributes declared int, String, boolean String for an enumeration. For
	 * sets, the value must be an array of the corresponding types. i.e;
	 * Set<Integer>, Set<String> and so on. If the attribute does not exist, of
	 * it the value does not correspond to the attribute type, "false" is
	 * returned.
	 */
	boolean setProperty(String attr, Object value);

}
