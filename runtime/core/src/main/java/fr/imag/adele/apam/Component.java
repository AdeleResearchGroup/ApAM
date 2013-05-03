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
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;

public interface Component {

	//The name of the component
	public String getName();

	//The component declaration
	public ComponentDeclaration getDeclaration () ;
	
	//return all the members of this component. Null if leaf (instance).
	public Set<? extends Component> getMembers ();

	//return the representant of this group member. Null if root (Specification)
	public Component getGroup ();

	//Return it kind: Specification, implementation or instance
	public ComponentKind getKind() ;

	//Returns the provided resources, including those inherited.
	public Set<ResourceReference> getProvidedResources () ;

	//The underlying entity in the execution platform
	public ApformComponent getApformComponent();

	//Give the composite type that physically deployed that component. Warning : null when unused. 
	public CompositeType getFirstDeployed() ;

	//Whether the component is instantiable
	public boolean isInstantiable() ;

	//Whether the component can see the target component (visibility control)
	public boolean canSee(Component target) ;


	//Whether the component is singleton
	public boolean isSingleton() ;

	//Whether the component is shared
	public boolean isShared() ;
	

	 // ====================== Links ==========================

    //returns all the instances this one is Linkd to.
    public Set<Component> getLinkDests();

    //returns the Link toward that destination
    public Link getInvLink(Component destInst);

    //returns the Link for the "depName" link toward that destination Component
    public Link getInvLink(Component destInst, String depName);

    //Returns all the Links toward that destination.
    public Set<Link> getInvLinks(Component destInst);

	// returns all the destinations of that relation (if multiple cardinality)
    public Set<Component> getLinkDests(String depName);

	// returns the destinations of that relation (if simple cardinality)
    public Component getLinkDest(String depName) ;

	// returns all the Links related to that relation (if multiple cardinality)
    public Set<Link> getLinks(String depName);

	// Returns all the Links, for the provided relation, leading to the current
	// Component.
    public Set<Link> getInvLinks(String depName);

    //returns all the Link from the current Component
    public Set<Link> getLinks();

    //Returns all the Links leading to the current Component.
     public Set<Link> getInvLinks();

     //remove that Link.
     public void removeLink(Link link);

     /**
     * A new Link has to be instantiated between the current Component and the "to" Component, for the relation depName.
     * 
     * @param to target link
     * @param depName : relation name
     * @param hasConstraints: true if the Relation definition has contraints
     * @param promotion true if it is a promotion
     * @return  true if the link has been created
     */
     public boolean createLink(Component to, Relation dep, boolean hasConstraints, boolean promotion);

	// public boolean createLink(Component to, relation dep, boolean promotion);

    
    //================== Dependencies =================


	//True if the component matches the filter
	public boolean match(ApamFilter goal);


	// True if the component matches the constraints contained in the relation
	// filters
	public boolean matchRelationConstraints(Relation dep);

	// True if the component matches the Target of this relation
	public boolean matchRelationTarget(Relation dep);

	// True if the component matches the relation (target and constraints)
	public boolean matchRelation(Relation dep);

	// Get the relation that can be applied to this component with this id,
	// including those coming from composite if any.
	//null if not defined
	public Relation getRelation(String id);

	//Get all the dependencies that can be applied to this component, including those coming from composite if any.
	//Empty if none
	public Set<Relation> getRelations();

	//Get all the dependencies defined at that component level. 
	//Empty if none. Return an unmodifiable collection of dependencies
	public Collection<Relation> getLocalRelations();
	
	
	//==================== Properties =============

	//Get the value of a property, the property can be valued in this component or in its defining group
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
	 * groups. 
	 * WARNING : substitutions are not performed.
	 */
	public Map<String, Object> getAllProperties();
	
	/**
	 * Get the value of all the properties of the component, including those in the enclosing
	 * groups
	 * The values are transformed into string, but without substitution
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
	 * Change the values of the specified properties of the component
	 */
	public boolean setAllProperties(Map<String, String> properties);

	/**
	 * Removes the specified property of the component
	 */
	public boolean removeProperty(String attr);
	
	/**
	 * 
	 * @return
	 */
	public Map<String, String> getValidAttributes () ;


}
