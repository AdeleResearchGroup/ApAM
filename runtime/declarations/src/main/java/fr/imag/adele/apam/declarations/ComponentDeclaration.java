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
package fr.imag.adele.apam.declarations;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;

/**
 * This class represents the common description of a component at all levels of
 * abstraction (specification, implementation, composite or instance)
 * 
 * @author vega
 * 
 */
public abstract class ComponentDeclaration {

	/**
	 * The identifier of the service provider
	 */
	private final String name;

	/**
	 * The reference to this component declaration
	 */
	private final ComponentReference<?> reference;

	/**
	 * Whether this declaration has inherited features
	 */
	private final boolean isEffective;
	
	/**
	 * The resources provided by this service
	 */
	protected final Set<ResourceReference> providedResources;

	/**
	 * The resources required by this service
	 */
	private final Set<RelationDeclaration> relations;

	/**
	 * The properties describing this service provider
	 */
	private final Map<String, String> properties;
	private final List<PropertyDefinition> definitions;

	/**
	 * Whether instances of this component must be accessed exclusively by a
	 * single client at the time
	 */
	private boolean isExclusive;
	private boolean isDefinedExclusive;

	/**
	 * Whether we can create instances of this component
	 */
	private boolean isInstantiable;
	private boolean isDefinedInstantiable;

	/**
	 * Whether there can be a single instance of this component in the execution
	 * platform
	 */
	private boolean isSingleton;
	private boolean isDefinedSingleton;

	/**
	 * Whether the instance of this component can be shared by different clients
	 */
	private boolean isShared;
	private boolean isDefinedShared;

	
	private ComponentDeclaration(String name, boolean isEffective) {

		assert name != null;

		this.name = name;
		reference = generateReference();

		/*
		 * provided resources
		 */
		providedResources = new HashSet<ResourceReference>();

		/*
		 * declarations
		 */
		relations 		= new HashSet<RelationDeclaration>();
		definitions 	= new ArrayList<PropertyDefinition>();

		/*
		 * property values
		 */
		properties 		= new HashMap<String, String>();

		/*
		 * Initialize default value for global properties
		 */
		this.isInstantiable 		= true;
		this.isExclusive 			= false;
		this.isSingleton 			= false;
		this.isShared 				= true;
		
		this.isDefinedInstantiable 	= false;
		this.isDefinedExclusive 	= false;
		this.isDefinedSingleton 	= false;
		this.isDefinedShared 		= false;

		this.isEffective			= isEffective;
	}

	/**
	 * The usual constructor for a new declaration
	 */
	protected ComponentDeclaration(String name) {
		this(name,false);
	}

	/**
	 * Copy constructor, creates a clone of a given component declaration, this is
	 * used for computing effective declarations
	 */
	protected ComponentDeclaration(ComponentDeclaration original) {
		
		/*
		 * initialize clone
		 */
		this(original.name, true);
		
		/*
		 * provided resources
		 */
		this.providedResources.addAll(original.providedResources);

		/*
		 * declarations
		 */
		this.relations.addAll(original.relations);
		this.definitions.addAll(original.definitions);

		/*
		 * property values
		 */
		this.properties.putAll(original.properties);
		
		/*
		 * Initialize default value for global properties
		 */
		this.isInstantiable 		= original.isInstantiable;
		this.isExclusive 			= original.isExclusive;
		this.isSingleton 			= original.isSingleton;
		this.isShared 				= original.isShared;
		
		this.isDefinedInstantiable 	= original.isDefinedInstantiable;
		this.isDefinedExclusive 	= original.isDefinedExclusive;
		this.isDefinedSingleton 	= original.isDefinedSingleton;
		this.isDefinedShared 		= original.isDefinedShared;
	}
	
	
	/**
	 * Generates a unique resource identifier to reference this declaration.
	 * 
	 * 
	 * NOTE IMPORTANT this method is invoked at the beginning of the constructor when only
	 * the name filed is available.
	 * 
	 * The generated reference must have the appropriate kind to reflect the abstraction level
	 * of the component, and the generic type of the reference must be the class of the defining
	 * declaration. 
	 */
	protected abstract ComponentReference<?> generateReference();

	/**
	 * Get the reference to this declaration
	 */
	public ComponentReference<?> getReference() {
		return reference;
	}
	
	/**
	 * Get the name of the provider
	 */
	public String getName() {
		return name;
	}

	/**
	 * The kind of this component
	 */
	public final ComponentKind getKind() {
		return getReference().getKind();
	}

	/**
	 * Get the reference to the parent group of this declaration
	 */
	public abstract ComponentReference<?> getGroup();

	/**
	 * Get the reference to the parent group of this declaration, including the specific
	 * range of versions
	 */
	public abstract VersionedReference<?> getGroupVersioned();

	/**
	 * Whether this declaration has merged the inherited features of its group
	 */
	public boolean isEffective() {
		return isEffective;
	}
	
	/**
	 * Computes the effective declaration that is the result of merging this declaration with
	 * the definitions in its group.
	 * 
 	 * @see #inheritFrom(ComponentDeclaration) for a description of the merging algorithm
 	 * common to all kinds of components
	 * 
	 */
	public final ComponentDeclaration getEffectiveDeclaration(ComponentDeclaration group) {
		
		/*
		 * First we clone this declaration, we need to use reflection because we want to create
		 * an object of the same exact class as this, so we assume a clone constructor is defined
		 * for each subclass of ComponentDeclaration. 
		 */
		try {
			Constructor<? extends ComponentDeclaration> cloneConstructor = this.getClass().getDeclaredConstructor(this.getClass());
			
			ComponentDeclaration effective = cloneConstructor.newInstance(this);
			effective.inheritFrom(group);
			return effective;
			
		} catch (Exception e) {
			/*
			 * This should never happen, the component declaration hierarchy has been completely defined
			 */
			e.printStackTrace();
			return null;
		} 
	}
	
	/**
	 * Perform the standard inheritance of component features from the group.
	 * 
	 * The following elements are propagated from the group
	 * 
	 * 1) provided resources
	 * 2) property and relation declarations
	 * 3) valued properties
	 * 
	 * The following elements in this declaration can refine definitions in the group
	 * 
	 * 1) add constraints and instrumentation to relations
	 * 2) add instrumentation to property definitions

	 * Notice that this method is invoked by {@link #getEffectiveDeclaration(ComponentDeclaration)}
	 * on a clone of the original declaration.
	 * 
	 * NOTE this method is intended to be redefined in subclasses to implement the specific
	 * refinements depending on the kind of component. 
	 */
	protected void inheritFrom(ComponentDeclaration group) {
		
		if (this.getGroup() == null && group != null)
			throw new IllegalArgumentException("Component "+ getName() +": trying to refine from invalid group "+group);

		if (this.getGroup() != null && group == null)
			throw new IllegalArgumentException("Component "+ getName() +": trying to refine from invalid null group");
		
		if (this.getGroup() != null && group != null && !this.getGroup().equals(group.getReference()))
			throw new IllegalArgumentException("Component "+ getName() +": trying to refine from invalid group "+group);
		
		if (group == null)
			return;
		
		/*
		 *  Inherit the list of provided resources, only if an explicit list was not specified
		 */
		if (getProvidedResources().isEmpty())
			this.getProvidedResources().addAll(group.getProvidedResources());
		
		/*
		 * For relation definitions, verify inheritance and refinement
		 */
		for (RelationDeclaration relation : group.getRelations()) {
			
			RelationDeclaration refinement = this.getRelation(relation.getIdentifier());
			if (refinement == null) {
				this.getRelations().add(new RelationDeclaration(relation));
			}
			else {
				this.getRelations().remove(refinement);
				this.getRelations().add(relation.refinedBy(refinement));
			}
		}
		
		/*
		 * For property definitions, verify inheritance and refinement
		 */
		for (PropertyDefinition property : group.getPropertyDefinitions()) {
			
			PropertyDefinition refinement = this.getPropertyDefinition(property.getName());
			if (refinement == null) {
				this.getPropertyDefinitions().add(property);
			}
			else {
				this.getPropertyDefinitions().remove(refinement);
				this.getPropertyDefinitions().add(property.refinedBy(refinement));
			}
		}
		
		/*
		 * Property values in the group propagate and override this definition
		 */
		for (Map.Entry<String, String> property : group.getProperties().entrySet()) {
			this.getProperties().put(property.getKey(),property.getValue());
		}
	}
	
	/**
	 * Get the declared dependencies of this component
	 */
	public Set<RelationDeclaration> getRelations() {
		return relations;
	}

	
	/**
	 * Get a relation declaration by name
	 */
	public RelationDeclaration getRelation(String id) {
		for (RelationDeclaration relation : relations) {
			if (relation.getIdentifier().equals(id)) {
				return relation;
			}
		}

		return null;
	}

	/**
	 * Get a relation declaration by reference
	 */
	public RelationDeclaration getRelation(RelationDeclaration.Reference relation) {
		if (!this.getReference().equals(relation.getDeclaringComponent())) {
			return null;
		}

		return getRelation(relation.getIdentifier());
	}
	
	/**
	 * Get the properties describing this provider
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Get the value of a property
	 */
	public String getProperty(String property) {
		return properties.get(property);
	}


	/**
	 * Get the property definitions defined by this component
	 */
	public List<PropertyDefinition> getPropertyDefinitions() {
		return definitions;
	}
	
	/**
	 * Get the named property definition, if declared
	 */
	public PropertyDefinition getPropertyDefinition(String propertyName) {
		for (PropertyDefinition definition : definitions) {
			if (definition.getName().equalsIgnoreCase(propertyName)) {
				return definition;
			}
		}
		return null;
	}

	/**
	 * Get the named property definition, if declared
	 */
	public PropertyDefinition getPropertyDefinition(PropertyDefinition.Reference property) {
		if (!this.getReference().equals(property.getDeclaringComponent())) {
			return null;
		}

		return getPropertyDefinition(property.getIdentifier());
	}
	
	
	/**
	 * Whether the specified property is defined in this component
	 */
	public boolean isDefined(String propertyName) {
		return getPropertyDefinition(propertyName) != null;
	}


	/**
	 * Get the provided resources
	 */
	public Set<ResourceReference> getProvidedResources() {
		return providedResources;
	}

	/**
	 * Get the provided resources of a given kind, for example Services or
	 * Messages.
	 * 
	 * We use subclasses of ResourceReference as tags to identify kinds of
	 * resources. To add a new kind of resource a new subclass must be added.
	 * 
	 * Notice that we return a set of resource references but typed to
	 * particular subtype of references, the unchecked downcast is then safe at
	 * runtime.
	 */
	public <T extends ResourceReference> Set<T> getProvidedResources(Class<T> kind) {
		Set<T> resources = new HashSet<T>();
		for (ResourceReference resourceReference : providedResources) {
			if (kind.isInstance(resourceReference)) {
				resources.add(kind.cast(resourceReference));
			}
		}
		return resources;
	}

	public boolean isDefinedExclusive() {
		return isDefinedExclusive;
	}

	public boolean isDefinedInstantiable() {
		return isDefinedInstantiable;
	}

	public boolean isDefinedShared() {
		return isDefinedShared;
	}

	public boolean isDefinedSingleton() {
		return isDefinedSingleton;
	}

	/**
	 * Whether the component is exclusive
	 */
	public boolean isExclusive() {
		return isExclusive;
	}

	/**
	 * Whether the component is instantiable
	 */
	public boolean isInstantiable() {
		return isInstantiable;
	}


	/**
	 * Whether the component is shared
	 */
	public boolean isShared() {
		return isShared;
	}

	/**
	 * Whether the component is singleton
	 */
	public boolean isSingleton() {
		return isSingleton;
	}

	/**
	 * Displays the declaration on screen, indented. Same as toString, plus the
	 * indentation.
	 * 
	 * @param indent
	 *            : a number of white characters as indentation.
	 */
	public String printDeclaration(String indent) {
		String nl = "\n" + indent;
		StringBuffer ret = new StringBuffer();
		ret.append(indent + " Declaration of " + name);
		if (providedResources.size() != 0) {
			ret.append(nl + "   Provided resources: ");
			for (ResourceReference resRef : providedResources) {
				ret.append(nl + "      " + resRef);
			}
		}
		if (relations.size() != 0) {
			ret.append(nl + "   Dependencies: \n");
			for (RelationDeclaration resRef : relations) {
				ret.append(resRef.printRelationDeclaration(indent + "   ")
						+ "\n");
			}
		}
		// if (properties.size() != 0) {
		// ret.append(nl + "   Properties: ");
		// for (Object resRef : properties.keySet()) {
		// ret.append(nl + "      " + (String) resRef + " = " +
		// properties.get(resRef));
		// }
		// }
		if (definitions.size() != 0) {
			ret.append(nl + "   Attribute definitions ");
			for (PropertyDefinition resRef : definitions) {
				ret.append(nl + "      " + resRef);
			}
		}

		return ret.toString();
	}


	public void setExclusive(boolean isExclusive) {
		this.isExclusive = isExclusive;
		this.isDefinedExclusive = true;
	}

	public void setInstantiable(boolean isInstantiable) {
		this.isInstantiable = isInstantiable;
		this.isDefinedInstantiable = true;
	}

	public void setShared(boolean isShared) {
		this.isShared = isShared;
		this.isDefinedShared = true;
	}

	public void setSingleton(boolean isSingleton) {
		this.isSingleton = isSingleton;
		this.isDefinedSingleton = true;
	}

	@Override
	public String toString() {
		return printDeclaration("");
	}

}
