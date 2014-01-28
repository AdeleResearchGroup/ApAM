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
package fr.imag.adele.apam.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.InjectedPropertyPolicy;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.Substitute;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.util.Visible;

public abstract class ComponentImpl extends ConcurrentHashMap<String, Object>
		implements Component, Comparable<Component> {

	/**
	 * An exception that can be thrown in the case of problems while creating a
	 * component
	 */
	public static class InvalidConfiguration extends Exception {

		private static final long serialVersionUID = 1L;

		public InvalidConfiguration(String message) {
			super(message);
		}

		public InvalidConfiguration(String message, Throwable cause) {
			super(message, cause);
		}

		public InvalidConfiguration(Throwable cause) {
			super(cause);
		}

	}

	private final Object componentId = new Object(); // only for hashCode

	private static final long serialVersionUID = 1L;

	protected static Logger logger = LoggerFactory
			.getLogger(ComponentImpl.class);
	private final ApformComponent apform;

	private final ComponentDeclaration declaration;

	// Contains the composite type that was the first to physically deploy that
	// component.
	private CompositeType firstDeployed;

	// Set of Dependencies
	private Map<String, RelationDefinition> relDef = new HashMap<String, RelationDefinition>();
	protected final Set<Link> links = Collections
			.newSetFromMap(new ConcurrentHashMap<Link, Boolean>());

	protected final Set<Link> invlinks = Collections
			.newSetFromMap(new ConcurrentHashMap<Link, Boolean>());

	public ComponentImpl(ApformComponent apform) throws InvalidConfiguration {
		if (apform == null) {
			throw new InvalidConfiguration(
					"Null apform instance while creating component");
		}

		this.apform = apform;
		this.declaration = apform.getDeclaration();
	}

	/**
	 * Whether the component is instantiable
	 */
	@Override
	public boolean canSee(Component target) {
		return Visible.isVisible(this, target);
	}

	/**
	 * TODO Assumes that all components are in the same name space, including
	 * instance !!
	 */
	@Override
	public int compareTo(Component that) {
		return this.getName().compareTo(that.getName());
	}

	@Override
	public boolean createLink(Component to, RelToResolve dep,
			boolean hasConstraints, boolean promotion) {
		// Not a relation
		if (!dep.isRelation()) {
			return true;
		}

		if (CST.isFinalRelation(dep.getName())) {
			logger.error("CreateLink: cannot create predefine relation "
					+ dep.getName());
			return false;
		}

		if ((to == null) || (dep == null)) {
			logger.error("CreateLink: Source or target are null ");
			return false;
		}
		if (!promotion && !canSee(to)) {
			logger.error("CreateLink: Source  " + this
					+ " does not see its target " + to);
			return false;
		}
		if (this.getKind() != dep.getSourceKind()) {
			logger.error("CreateLink: Source kind " + getKind()
					+ " is not compatible with relation sourceType "
					+ dep.getSourceKind());
			return false;
		}
		if (to.getKind() != dep.getTargetKind()) {
			logger.error("CreateLink: Target kind " + to.getKind()
					+ " is not compatible with relation targetType "
					+ dep.getTargetKind());
			return false;
		}

		String depName = dep.getName();

		for (Link link : links) { // check if it already exists
			if ((link.getDestination() == to) && link.getName().equals(depName)) {
				// It exists, do nothing.
				return true;
			}
		}

		// creation
		if (!getApformComponent().checkLink(to, depName)) {
			logger.error("CreateLink: INTERNAL ERROR: link from " + this
					+ " to " + to
					+ " could not be created in the real instance.");
			return false;
		}

		Link link = new LinkImpl(this, to, dep, hasConstraints, promotion);
		links.add(link);
		((ComponentImpl) to).invlinks.add(link);
		getApformComponent().setLink(to, depName);

		/*
		 * if "to" is an instance in the unused pull, move it to the from
		 * composite.
		 */
		if (this instanceof Instance && to instanceof Instance
				&& !((Instance) to).isUsed()) {
			((InstanceImpl) to).setOwner(((Instance) this).getComposite());
		}

		// TODO What to do if it is a link towards an unused implem or spec ?
		// Nothing ?
		// TODO Does isUsed (and shared) limited to the wires ?

		// Notify Dynamic managers that a new link has been created
		for (DynamicManager manager : ApamManagers.getDynamicManagers()) {
			manager.addedLink(link);
		}

		return true;
	}

	/**
	 * Components are uniquely represented in the Apam state model, so we use
	 * reference equality in all comparisons.
	 */
	@Override
	public final boolean equals(Object o) {
		return (this == o);
	}

	/**
	 * To be called when the object is fully loaded and chained. Terminate the
	 * generic initialization : computing the dependencies, and the properties
	 * 
	 * @param initialProperties
	 */
	public void finishInitialize(Map<String, String> initialProperties) {
		initializeDependencies();
		initializeProperties(initialProperties);
		initializeResources();
	}

	/**
	 * Get the value of all the properties in the component.
	 * 
	 */
	@Override
	public Map<String, Object> getAllProperties() {
		return Collections.unmodifiableMap(this);
	}

	@Override
	public Map<String, String> getAllPropertiesString() {
		Map<String, String> ret = new HashMap<String, String>();
		for (Entry<String, Object> e : this.entrySet()) {
			if (!Attribute.isFinalAttribute(e.getKey())) {
				ret.put(e.getKey(), Util.toStringAttrValue(e.getValue()));
			}
		}
		return ret;
	}

	@Override
	public final ApformComponent getApformComponent() {
		return apform;
	}
	
	/**
	 * Tries to find the definition of a property, the property can be valued in this component
	 * or in its defining group
	 * 
	 * @param attribute
	 * @return
	 */	
	@Override
	public PropertyDefinition getPropertyDefinition(String attribute) {
		return getAttrDefinition(attribute);
	}
	

	/**
	 * Tries to find the definition of attribute "attr" associated with
	 * component "component". Returns null if the attribute is not explicitly
	 * defined
	 * 
	 * @param component
	 * @param attr
	 * @return
	 */
	public PropertyDefinition getAttrDefinition(String attr) {

		// PropertyDefinition definition =
		// getDeclaration().getPropertyDefinition(attr);
		// if (definition != null) {
		// return definition;
		// }

		PropertyDefinition definition = null;
		Component group = this; // .getGroup();
		while (group != null) {
			definition = group.getDeclaration().getPropertyDefinition(attr);
			if (definition != null) {
				return definition;
			}
			group = group.getGroup();
		}
		return null;
	}

	@Override
	public AttrType getAttrType(String attr) {
		PropertyDefinition attrDef = getAttrDefinition(attr);
		if (attrDef == null) {
			if (Attribute.isFinalAttribute(attr)) {
				return Attribute.splitType("string");
			}
			return null;
		}
		return Attribute.splitType(attrDef.getType());
	}

	@Override
	public final ComponentDeclaration getDeclaration() {
		return declaration;
	}

	/**
	 * Return the first link having this name, on any ancestor, but do not try
	 * to resolve is none are found
	 * 
	 * @param relName
	 * @return
	 */
	public Link getExistingLink(String relName) {
		Component group = this;
		while (group != null) {
			for (Link link : ((ComponentImpl) group).getLocalLinks()) {
				if (link.getName().equals(relName)) {
					return link;
				}
			}
			group = group.getGroup();
		}
		return null;
	}

	/**
	 * Return all the link of that names, but do not resolve if nore are found
	 * 
	 * @param relName
	 * @return
	 */
	public Set<Link> getExistingLinks(String relName) {
		Set<Link> dests = new HashSet<Link>();
		Component group = this;
		while (group != null) {
			for (Link link : ((ComponentImpl) group).getLocalLinks()) {
				if (link.getName().equals(relName)) {
					dests.add(link);
				}
			}
			group = group.getGroup();
		}
		return dests;
	}

	private Set<Component> getFInalLinkDests(String depName) {

		Set<Component> dests = new HashSet<Component>();
		// if (depName.equals(CST.REL_GROUP)) {
		// dests.add(getGroup()) ;
		// return dests ;
		// }
		// if (depName.equals(CST.REL_MEMBERS)) {
		// dests.addAll(getMembers()) ;
		// return dests ;
		// }
		if (depName.equals(CST.REL_COMPOSITE)) {
			if (this instanceof Instance) {
				dests.add(((Instance) this).getComposite());
			}
			return dests;
		}
		if (depName.equals(CST.REL_COMPOTYPE)) {
			if (this instanceof Instance) {
				dests.add(((Instance) this).getComposite().getCompType());
			} else if (this instanceof Implementation) {
				dests.addAll(((Implementation) this).getInCompositeType());
			}
			return dests;
		}
		if (depName.equals(CST.REL_CONTAINS)) {
			if (this instanceof Composite) {
				dests.addAll(((Composite) this).getContainInsts());
			} else if (this instanceof CompositeType) {
				dests.addAll(((CompositeType) this).getImpls());
			}
			return dests;
		}

		if (depName.equals(CST.REL_SPEC)) {
			if (this instanceof Specification) {
				dests.add(this);
			} else if (this instanceof Implementation) {
				dests.add(((Implementation) this).getSpec());
			} else if (this instanceof Instance) {
				dests.add(((Instance) this).getSpec());
			}
			return dests;
		}
		if (depName.equals(CST.REL_IMPL)) {
			if (this instanceof Implementation) {
				dests.add(this);
			} else if (this instanceof Instance) {
				dests.add(((Instance) this).getImpl());
			}
			return dests;
		}
		if (depName.equals(CST.REL_INST)) {
			if (this instanceof Instance) {
				dests.add(this);
			}
			return dests;
		}

		if (depName.equals(CST.REL_IMPLS)) {
			if (this instanceof Specification) {
				dests.addAll(this.getMembers());
			} else if (this instanceof Implementation) {
				dests.addAll(((Implementation) this).getSpec().getMembers());
			} else {
				dests.addAll(((Instance) this).getSpec().getMembers());
			}
			return dests;
		}
		if (depName.equals(CST.REL_INSTS)) {
			if (this instanceof Implementation) {
				dests.addAll(this.getMembers());
			} else if (this instanceof Instance) {
				dests.addAll(((Instance) this).getImpl().getMembers());
			}
			return dests;
		}
		if (depName.equals(CST.REL_INST)) {
			if (this instanceof Instance) {
				dests.add(this);
			}
			return dests;
		}
		return dests;
	}

	@Override
	public CompositeType getFirstDeployed() {
		return firstDeployed == null ? CompositeTypeImpl.getRootCompositeType()
				: firstDeployed;
	}

	@Override
	public Link getInvLink(Component destInst) {
		if (destInst == null) {
			return null;
		}
		for (Link link : invlinks) {
			if (link.getDestination() == destInst) {
				return link;
			}
		}
		return null;
	}

	@Override
	public Link getInvLink(Component destInst, String depName) {
		if (destInst == null) {
			return null;
		}
		for (Link link : invlinks) {
			if ((link.getDestination() == destInst)
					&& (link.getName().equals(depName))) {
				return link;
			}
		}
		return null;
	}

	@Override
	public Set<Link> getInvLinks() {
		return Collections.unmodifiableSet(invlinks);
	}

	@Override
	public Set<Link> getInvLinks(Component destInst) {
		if (destInst == null) {
			return null;
		}
		Set<Link> w = new HashSet<Link>();
		for (Link link : invlinks) {
			if (link.getDestination() == destInst) {
				w.add(link);
			}
		}
		return w;
	}

	@Override
	public Set<Link> getInvLinks(String depName) {
		Set<Link> w = new HashSet<Link>();
		for (Link link : invlinks) {
			if ((link.getDestination() == this)
					&& (link.getName().equals(depName))) {
				w.add(link);
			}
		}
		return w;
	}

	@Override
	public Link getLink(String relName) {
		Component group = this;
		while (group != null) {
			for (Link link : ((ComponentImpl) group).getLocalLinks()) {
				if (link.getName().equals(relName)) {
					return link;
				}
			}
			group = group.getGroup();
		}

		// None are present. Try to resolve
		RelationDefinition rel = getRelation(relName);
		if (rel == null) {
			logger.error("relation " + relName + " undefined for " + this);
			return null;
		}
		Component source = rel.getRelSource(this);
		CST.apamResolver.resolveLink(source, rel);
		return getExistingLink(relName);
	}

	/**
	 * resolve
	 */
	@Override
	public Component getLinkDest(String depName) {
		Link link = getLink(depName);
		return (link == null) ? null : link.getDestination();
	}

	// =================================== Links =========
	/**
	 * returns the connections towards the service instances actually used.
	 * return only APAM links.
	 */
	@Override
	public Set<Component> getLinkDests(String depName) {
		if (depName == null || depName.isEmpty()) {
			return null;
		}

		if (CST.isFinalRelation(depName)) {
			return getFInalLinkDests(depName);
		}

		Set<Component> dests = new HashSet<Component>();
		for (Link link : getLinks(depName)) {
			if (link.getName().equals(depName)) {
				dests.add(link.getDestination());
			}
		}
		return dests;
	}

	/**
	 * Inherits and resolve
	 */
	@Override
	public Set<Link> getLinks(String relName) {
		Set<Link> dests = getExistingLinks(relName);
		if (!dests.isEmpty()) {
			return dests;
		}

		// None are present. Try to resolve
		RelationDefinition rel = getRelation(relName);
		if (rel == null) {
			logger.error("relation " + relName + " undefined for " + this);
			return null;
		}

		// if (!rel.isLazy())
		// return dests ;

		Component source = rel.getRelSource(this);
		CST.apamResolver.resolveLink(source, rel);
		return getExistingLinks(relName);
	}

	/**
	 * Only return the links instantiated on that source component (no
	 * inheritance, no resolution)
	 */
	public Set<Link> getLocalLinks() {
		return Collections.unmodifiableSet(links);
	}

	protected RelationDefinition getLocalRelation(String id) {
		return relDef.get(id);
	}

	@Override
	public Collection<RelationDefinition> getLocalRelations() {
		return Collections.unmodifiableCollection(relDef.values());
	}

	@Override
	public final String getName() {
		return declaration.getName();
	}

	/**
	 * Get the value of the property.
	 * 
	 * Attributes are supposed to be correct and inherited statically
	 * 
	 */
	@Override
	public String getProperty(String attr) {
		return Util.toStringAttrValue(getPropertyObject(attr));
	}

	/**
	 * Get the value of a property, the property can be valued in this component
	 * or in its defining group Return will be an object of type int, String,
	 * boolean for attributes declared int, String, boolean String for an
	 * enumeration.
	 * 
	 * For sets, the return will be an array of the corresponding types. i.e;
	 * int[], String[] and so on. Returns null if attribute is not defined or
	 * not set.
	 */
	@Override
	public Object getPropertyObject(String attribute) {
		Object value = get(attribute);
		if (value == null) {
			return null;
			// all attributes, including default values are already there
			// if(value==null && getDeclaration()!=null)
			// value=getDeclaration().getProperty(attribute);
		}

		return Substitute.substitute(attribute, value, this);
	}

	@Override
	public Set<ResourceReference> getProvidedResources() {
		return Collections.unmodifiableSet(this.getDeclaration()
				.getProvidedResources());
	}

	// Set<ResourceReference> allResources = new HashSet<ResourceReference> () ;
	// Component current = this ;
	// while (current != null) {
	// if (current.getDeclaration().getProvidedResources() != null)
	// allResources.addAll (current.getDeclaration().getProvidedResources()) ;
	// current = current.getGroup() ;
	// }
	// return allResources ;
	// }

	/**
	 * WARNING : no resolution
	 */
	@Override
	public Set<Component> getRawLinkDests() {
		Set<Component> dests = new HashSet<Component>();

		for (Link link : getRawLinks()) {
			dests.add(link.getDestination());
		}
		return dests;
	}

	/**
	 * No resolution
	 * 
	 * @param depName
	 * @return
	 */
	public Set<Component> getRawLinkDests(String depName) {
		if (depName == null || depName.isEmpty()) {
			return null;
		}

		if (CST.isFinalRelation(depName)) {
			return getFInalLinkDests(depName);
		}

		Set<Component> dests = new HashSet<Component>();
		for (Link link : getRawLinks()) {
			if (link.getName().equals(depName)) {
				dests.add(link.getDestination());
			}
		}
		return dests;
	}

	/**
	 * Warning : do not resolve !
	 */
	@Override
	public Set<Link> getRawLinks() {
		Set<Link> allLinks = new HashSet<Link>();
		Component group = this;
		while (group != null) {
			allLinks.addAll(((ComponentImpl) group).getLocalLinks());
			group = group.getGroup();
		}
		return allLinks;
	}

	/**
	 * Return the relation with name "id" if it can be applied to this
	 * component.
	 * 
	 * A relation D can be applied on a component source if D.Id == id D.source
	 * must be the name of source or of an ancestor of source, and D.SourceKind
	 * == source.getKind.
	 * 
	 * Looks in the group, and then in the composite type, if source in an
	 * instance in all composite types if source is an implem.
	 * 
	 * @param source
	 * @param id
	 * @return
	 */
	@Override
	public RelationDefinition getRelation(String id) {
		RelationDefinition dep = null;
		Component group = this;
		while (group != null) {
			dep = ((ComponentImpl) group).getLocalRelation(id);
			if (dep != null) {
				return dep;
			}
			group = group.getGroup();
		}

		// Looking for composite definitions.
		if (this instanceof Instance) {
			CompositeType comptype = ((Instance) this).getComposite()
					.getCompType();
			dep = comptype.getCtxtRelation(this, id);
			if (dep != null) {
				return dep;
			}
		}
		if (this instanceof Implementation) {
			for (CompositeType comptype : ((Implementation) this)
					.getInCompositeType()) {
				dep = comptype.getCtxtRelation(this, id);
				if (dep != null) {
					return dep;
				}
			}
		}
		return null;
	}

	@Override
	public Set<RelationDefinition> getRelations() {
		Set<RelationDefinition> relDefs = new HashSet<RelationDefinition>();
		Set<String> processed = new HashSet<String>();

		Component group = this;
		while (group != null) {

			for (RelationDefinition relDef : group.getLocalRelations()) {
				if (!processed.contains(relDef.getName())) {
					relDefs.add(relDef);
					processed.add(relDef.getName());
				}
			}

			group = group.getGroup();
		}

		// Looking for composite definitions.
		if (this instanceof Instance) {
			CompositeType comptype = ((Instance) this).getComposite()
					.getCompType();

			for (RelationDefinition relDef : comptype.getCtxtRelations(this)) {
				if (!processed.contains(relDef.getName())) {
					relDefs.add(relDef);
					processed.add(relDef.getName());
				}
			}

		}
		if (this instanceof Implementation) {
			for (CompositeType comptype : ((Implementation) this)
					.getInCompositeType()) {
				for (RelationDefinition relDef : comptype
						.getCtxtRelations(this)) {
					if (!processed.contains(relDef.getName())) {
						relDefs.add(relDef);
						processed.add(relDef.getName());
					}
				}
			}
		}

		return relDefs;
	}

	@Override
	public Map<String, String> getValidAttributes() {
		Map<String, String> ret = new HashMap<String, String>();
		for (PropertyDefinition def : declaration.getPropertyDefinitions()) {
			ret.put(def.getName(), def.getType());
		}
		if (getGroup() != null) {
			ret.putAll(getGroup().getValidAttributes());
		}
		return ret;
	}

	/**
	 * Override to make hash code conform to the equality definition
	 */
	@Override
	public final int hashCode() {
		return componentId.hashCode();
	}

	/**
	 * Provided a component, compute its effective relations, adding group
	 * constraint and flags. It is supposed to be correct !! No failure expected
	 * 
	 * Does not add those dependencies defined "above" nor the composite ones;
	 * except for the implementation definition that are overridden by the
	 * current composite (for instances) that are duplicated and copied at the
	 * instance level
	 * 
	 */
	private void initializeDependencies() {

		/*
		 * First we need to compute the list of relations that must be locally
		 * defined in this component. We consider locally defined relation
		 * declarations and overridden inherited relations.
		 */
		Set<RelationDeclaration> overrides = null;
		if (this instanceof Instance) {
			overrides = ((Instance) this).getComposite().getCompType()
					.getCompoDeclaration().getOverridenDependencies();
		}

		Set<RelationDeclaration> localRelations = new HashSet<RelationDeclaration>();
		Set<String> processed = new HashSet<String>();

		Component group = this;
		while (group != null) {

			for (RelationDeclaration relationDeclaration : group
					.getDeclaration().getDependencies()) {

				/*
				 * Ignore relations already processed at a lower level
				 */
				if (processed.contains(relationDeclaration.getIdentifier())) {
					continue;
				}

				/*
				 * Check overridden relations
				 */
				boolean matchOverride = false;
				for (RelationDeclaration override : overrides != null ? overrides
						: Collections.<RelationDeclaration> emptySet()) {
					if (matchOverride(relationDeclaration, override)) {
						relationDeclaration = relationDeclaration
								.overriddenBy(override);
						matchOverride = true;
					}
				}

				/*
				 * Process locally declared and inherited overridden relations
				 */
				if (group == this || matchOverride) {
					localRelations.add(relationDeclaration);
					processed.add(relationDeclaration.getIdentifier());
				}

			}

			group = group.getGroup();
		}

		/*
		 * Define all the local relations of this component
		 */
		for (RelationDeclaration relationDeclaration : localRelations) {

			/*
			 * Local declarations may be partial definitions, we need to compute
			 * the complete declaration by refining the ancestor definition.
			 */
			RelationDefinition base = this.getRelation(relationDeclaration
					.getIdentifier());
			relationDeclaration = (base == null) ? relationDeclaration
					: ((RelationDefinitionImpl) base)
							.refinedBy(relationDeclaration);

			relDef.put(relationDeclaration.getIdentifier(),
					new RelationDefinitionImpl(relationDeclaration));
		}

	}

	/**
	 * to be called once the Apam entity is fully initialized. Computes all its
	 * attributes, including inheritance.
	 */
	private void initializeProperties(Map<String, String> initialProperties) {
		/*
		 * get the initial attributes from declaration and overriden initial
		 * properties
		 */
		Map<String, String> props = new HashMap<String, String>(
				getDeclaration().getProperties());
		if (initialProperties != null) {
			props.putAll(initialProperties);
		}

		// First add the valid attributes.
		for (Map.Entry<String, String> entry : props.entrySet()) {
			PropertyDefinition def = validDef(entry.getKey(), true);
			if (def != null) {
				Object val = Attribute.checkAttrType(entry.getKey(),
						entry.getValue(), def.getType());
				if (val != null) {
					put(entry.getKey(), val);
				}
			}
		}

		// then add those coming from its group, avoiding overloads.
		ComponentImpl group = (ComponentImpl) getGroup();
		if (group != null) {
			for (String attr : group.getAllProperties().keySet()) {
				if (get(attr) == null) {
					put(attr, group.get(attr));
				}
			}
		}

		/*
		 * Add the default values specified in the group for properties not
		 * explicitly specified
		 */
		if (group != null) {
			for (PropertyDefinition definition : group.getDeclaration()
					.getPropertyDefinitions()) {
				if (definition.getDefaultValue() != null
						&& get(definition.getName()) == null
						&& definition.getInjected() != InjectedPropertyPolicy.INTERNAL) {
					Object val = Attribute.checkAttrType(definition.getName(),
							definition.getDefaultValue(), definition.getType());
					if (val != null) {
						put(definition.getName(), val);
					}
				}
			}
		}

		/*
		 * Set the attribute for the final attributes
		 */
		put(CST.SHARED, Boolean.toString(isShared()));
		put(CST.SINGLETON, Boolean.toString(isSingleton()));
		put(CST.INSTANTIABLE, Boolean.toString(isInstantiable()));

		/*
		 * Finally add the specific attributes. Should be the only place where
		 * instanceof is used.
		 */
		put(CST.NAME, apform.getDeclaration().getName());
		if (this instanceof Specification) {
			put(CST.SPECNAME, apform.getDeclaration().getName());
		} else if (this instanceof Implementation) {
			put(CST.IMPLNAME, apform.getDeclaration().getName());
			if (this instanceof CompositeType) {
				put(CST.APAM_COMPOSITETYPE, CST.V_TRUE);
			}
		} else if (this instanceof Instance) {
			put(CST.INSTNAME, apform.getDeclaration().getName());
			if (this instanceof Composite) {

				Composite composite = (Composite) this;
				put(CST.APAM_COMPOSITE, CST.V_TRUE);
				if (composite.getMainInst() != null) {
					put(CST.APAM_MAIN_INSTANCE, composite.getMainInst()
							.getName());
				}
			}
		}

		// and propagate, to the platform and to members, in case the spec has
		// been created after the implem
		for (Map.Entry<String, Object> entry : this.entrySet()) {
			for (Component member : getMembers()) {
				((ComponentImpl) member).propagate(entry.getKey(),
						entry.getValue());
			}
		}
	}

	private void initializeResources() {
		Set<ResourceReference> resources = getDeclaration()
				.getProvidedResources();
		Component group = this.getGroup();
		while (group != null) {
			if (group.getDeclaration().getProvidedResources() != null) {
				resources.addAll(group.getDeclaration().getProvidedResources());
			}
			group = group.getGroup();
		}
	}

	/**
	 * Whether this component is an ancestor of the specified component
	 */
	@Override
	public boolean isAncestorOf(Component member) {

		assert member != null;

		Component ancestor = member.getGroup();
		while (ancestor != null && !ancestor.equals(this)) {
			ancestor = ancestor.getGroup();
		}

		return ancestor != null;
	}

	/**
	 * Whether this component is a descendant of the specified component
	 */
	@Override
	public boolean isDescendantOf(Component group) {
		assert group != null;
		return group.isAncestorOf(this);
	}

	/**
	 * Whether the component is instantiable
	 */
	@Override
	public boolean isInstantiable() {
		if (declaration.isDefinedInstantiable() || getGroup() == null) {
			return declaration.isInstantiable();
		}
		return getGroup().isInstantiable();
	}

	/**
	 * Whether the component is shared
	 */
	@Override
	public boolean isShared() {
		if (declaration.isDefinedShared() || getGroup() == null) {
			return declaration.isShared();
		}
		return getGroup().isShared();
	}

	/**
	 * Whether the component is singleton
	 */
	@Override
	public boolean isSingleton() {
		if (declaration.isDefinedSingleton() || getGroup() == null) {
			return declaration.isSingleton();
		}
		return getGroup().isSingleton();
	}

	public boolean isSubstitute(String attr) {
		PropertyDefinition def = getDeclaration().getPropertyDefinition(attr);
		return (def != null && (def.getDefaultValue().charAt(0) == '$' || def
				.getDefaultValue().charAt(0) == '@'));
	}

	/**
	 * The link keeps the relToResolve used for its resolution.
	 * 
	 * @param incoming
	 * @return
	 */
	public boolean isValidLink(Link incoming) {
		RelToResolve relToResolve = incoming.getRelToResolve();
		return relToResolve.matchRelationConstraints(incoming.getDestination());
	}

	@Override
	public boolean match(ApamFilter goal) {
		if (goal == null) {
			return true;
		}
		return goal.match(this);
	}

	@Override
	public boolean match(String goal) {
		if (goal == null) {
			return true;
		}
		ApamFilter f = ApamFilter.newInstance(goal);
		if (f == null) {
			return false;
		}
		return goal == null || f.match(this.getAllProperties());
	}

	/**
	 * Given a relation declared in this component, checks if the provided
	 * override relation matches the relation declaration.
	 * 
	 * To be applied on a component C, the override must be such that : id
	 * matches the override id source must be the name of C or of an ancestor of
	 * C. target must be the same type (resource of component, and its name must
	 * match).
	 * 
	 */
	public boolean matchOverride(RelationDeclaration relation,
			RelationDeclaration override) {

		// Overrides are currently only valid for instance
		boolean match = (this instanceof Instance);
		if (!match) {
			return false;
		}

		// Check if Ids are compatible
		match = relation.getIdentifier().matches(override.getIdentifier());
		if (!match) {
			return false;
		}

		// Check if override source matches this component or one of its
		// ancestors
		// If no source id is specified in the override it is considered to
		// always
		// match

		match = (override.getSourceName() == null);
		Component group = this;
		while (group != null && !match) {
			match = group.getName().matches(override.getSourceName());
			group = group.getGroup();
		}

		if (!match) {
			return false;
		}

		/*
		 * Check if targets are compatible Same target: the same specification,
		 * the same implementation or same resource name with a matching
		 */
		// same nature: direct comparison
		match = relation.getTarget().getClass().equals(override.getTarget().getClass()) && 
				relation.getTarget().getName().matches(override.getTarget().getName());

		return match;
	}

	@Override
	public boolean matchRelation(RelToResolve dep) {
		return dep.matchRelation(this);
	}

	@Override
	public boolean matchRelationConstraints(RelToResolve dep) {
		return dep.matchRelationConstraints(dep.getTargetKind(),
				this.getAllProperties());
	}

	@Override
	public boolean matchRelationTarget(RelToResolve dep) {
		return dep.matchRelationTarget(this);
	}

	/**
	 * set the value, update apform and the platform, notify managers and
	 * propagates to the members, recursively
	 * 
	 * @param com
	 *            the component on which ot set the attribute
	 * @param attr
	 *            attribute name
	 * @param value
	 *            attribute value
	 */

	private void propagate(String attr, Object value) {
		if (value == null) {
			return;
		}
		Object oldValue = get(attr);
		if (oldValue != null && oldValue.equals(value.toString())) {
			return;
		}

		// Change value
		put(attr, value);

		// Notify the execution platform
		if (get(attr) == value) {
			getApformComponent().setProperty(attr, value.toString());
		}

		/*
		 * notify property managers
		 */
		if (oldValue == null) {
			ApamManagers.notifyAttributeAdded(this, attr, value.toString());
		} else {
			ApamManagers.notifyAttributeChanged(this, attr, value.toString(),
					oldValue.toString());
		}

		// Propagate to members recursively
		for (Component member : getMembers()) {
			((ComponentImpl) member).propagate(attr, value);
		}
	}

	/*
	 * Filter evaluation on the properties of this component
	 */

	/**
	 * TODO. Should we notify at all levels ?
	 * 
	 * @param ent
	 * @param attr
	 */
	private void propagateRemove(String attr) {

		remove(attr);
		for (Component member : getMembers()) {
			((ComponentImpl) member).propagateRemove(attr);
		}
	}

	/**
	 * This methods adds a newly created component to the Apam state model, so
	 * that it is visible to the external API
	 */
	public abstract void register(Map<String, String> initialProperties)
			throws InvalidConfiguration;

	public void removeInvLink(Link link) {
		invlinks.remove(link);
		// if (invLinks.isEmpty()) {
		/*
		 * This instance is no longer used. We do not set it unused
		 * setUsed(false); setOwner(CompositeImpl.getRootAllComposites());
		 * 
		 * Because it must stay in the same composite since it may be the target
		 * of an "OWN" clause, and must not be changed. In case it will be
		 * re-used (local).
		 */
		// }
	}

	// @Override
	public void removeLink(Link link) {
		if (getApformComponent().remLink(link.getDestination(), link.getName())) {
			links.remove(link);
			// TODO distriman: check if we have the destination implementation,
			// is this the right way to do it?

			// if(link.getDestination().getImpl()!=null)
			// ((ImplementationImpl)
			// getImpl()).removeUses(link.getDestination().getImpl());

		} else {
			logger.error("INTERNAL ERROR: link from " + this + " to "
					+ link.getDestination()
					+ " could not be removed in the real instance.");
		}
	}

	/**
	 * Removes the specifed property
	 * 
	 */
	@Override
	public boolean removeProperty(String attr) {
		return removeProperty(attr, false);
	}

	/**
	 * Warning: to be used only by Apform for removing internal attributes. Only
	 * Inhibits the message "Attribute " + attr +
	 * " is an program field attribute and cannot be removed.");
	 */
	public boolean removeProperty(String attr, boolean forced) {

		String oldValue = getProperty(attr);

		if (oldValue == null) {
			logger.error("ERROR: \"" + attr + "\" not instanciated");
			return false;
		}

		if (Attribute.isFinalAttribute(attr)) {
			logger.error("ERROR: \"" + attr + "\" is a final attribute");
			return false;
		}

		if (Attribute.isReservedAttributePrefix(attr)) {
			logger.error("ERROR: \"" + attr + "\" is a reserved attribute");
			return false;
		}

		PropertyDefinition propDef = getAttrDefinition(attr);
		if (propDef != null && propDef.getField() != null && !forced) {
			logger.error("In " + this + " attribute " + attr
					+ " is a program field and cannot be removed.");
			return false;
		}

		if (getGroup() != null && getGroup().getProperty(attr) != null) {
			logger.error("In " + this + " attribute " + attr
					+ " inherited and cannot be removed.");
			return false;
		}

		// it is ok, remove it and propagate to members, recursively
		propagateRemove(attr);

		// TODO. Should we notify at all levels ?
		ApamManagers.notifyAttributeRemoved(this, attr, oldValue);

		return true;
	}

	/**
	 * Sets all the values of the specified properties
	 * 
	 * We validate all attributes before actually modifying the value to avoid
	 * partial modifications ?
	 */
	@Override
	public boolean setAllProperties(Map<String, String> properties) {
		for (Map.Entry<String, String> entry : properties.entrySet()) {

			if (!setProperty(entry.getKey(), entry.getValue())) {
				return false;
			}
		}
		return true;
	}

	public void setFirstDeployed(CompositeType father) {
		firstDeployed = father;
	}

	/**
	 * Set the value of the property in the Apam state model. Changing an
	 * attribute notifies the property manager, the underlying execution
	 * platform (apform), and propagates to members
	 */
	@Override
	public boolean setProperty(String attr, Object value) {
		return setProperty(attr, value, false);
	}

	/**
	 * Warning: to be used only by Apform for setting internal attributes. Only
	 * Inhibits the message "Attribute " + attr +
	 * " is an internal field attribute and cannot be set.");
	 * 
	 * @param attr
	 * @param value
	 * @param forced
	 * @return
	 */
	public boolean setProperty(String attr, Object value, boolean forced) {
		/*
		 * Validate that the property is defined and the value is valid Forced
		 * means that we can set field attribute
		 */
		PropertyDefinition def = validDef(attr, forced);
		if (def == null) {
			return false;
		}
		// At initialization, all valid attributes are ok for specs
		Object val = Attribute.checkAttrType(attr, value, def.getType());
		if (val == null) {
			return false;
		}

		/*
		 * Force recalculation of dependencies that may have been invalidated by
		 * the property change. This must be done before notification and
		 * propagation, otherwise we risk to remove links updated by managers.
		 * 
		 * We remove only those links that are now invalid problematic for those
		 * that are not lazy links : assychronous messages, .. and avoiding
		 * unnecessary work TODO Check if this must be done for all links or
		 * only dynamic links
		 */
		Object oldValue = get(attr);
		put(attr, value);

		for (Link incoming : getInvLinks()) {
			// If still valid, do nothing
			if (incoming.hasConstraints() && !isValidLink(incoming)) {
				// remove
				incoming.remove();
				// recreate. Why ?
				// CST.apamResolver.resolveLink (incoming.getSource(),
				// incoming.getDefinition()) ;
			}
		}

		// If outgoing constraints have substitution, the link may be now
		// invalid
		for (Link outgoing : getLocalLinks()) {
			if (!!!((RelationDefinitionImpl) outgoing.getRelDefinition())
					.isStaticImplemConstraints()
					|| !!!((RelationDefinitionImpl) outgoing.getRelDefinition())
							.isStaticInstConstraints()) {
				outgoing.remove();
			}
		}

		// WARNING : undo the change because propagate needs the initial state
		if (oldValue == null) {
			remove(attr);
		} else {
			put(attr, oldValue);
		}

		// does the change, notifies managers, changes the platform and
		// propagate to members
		this.propagate(attr, val);

		return true;
	}

	// ==============================================
	@Override
	public String toString() {
		return getKind() + " " + getName();
	}

	/**
	 * This method removes a component from the Apam state model, it must ensure
	 * that the component is no longer referenced by any other component or
	 * visible by the external API. Should be called ONLY from the Broker.
	 */
	abstract void unregister();

	/**
	 * An attribute is valid if declared in an ancestor, and not set in an
	 * ancestor. Check if the value is conforming to its type (string, int,
	 * boolean). Internal attribute (associated with a field) cannot be set.
	 * Must be called on the level above.
	 * 
	 * Checks if attr is correctly defined for component ent it must be
	 * explicitly defined in its upper groups, for the top group, it must be
	 * already existing.
	 * 
	 * boolean Forced = true can be set only by Apform. Needed when an internal
	 * attribute is set in the program, Apform propagates the value to the
	 * attribute.
	 * 
	 * @param attr
	 * @param value
	 * @return
	 */
	public PropertyDefinition validDef(String attr, boolean forced) {
		if (Attribute.isFinalAttribute(attr)) {
			logger.error("In " + this + ", cannot redefine final attribute \""
					+ attr + "\"");
			return null;
		}

		if (Attribute.isReservedAttributePrefix(attr)) {
			logger.error("In " + this + ", attribute\"" + attr
					+ "\" is reserved");
			return null;
		}

		PropertyDefinition definition = this.getAttrDefinition(attr);
		if (definition == null) {
			logger.error("In " + this + ", attribute \"" + attr
					+ "\" is undefined.");
			return null;
		}

		/*
		 * Internal field attributes cannot be set
		 */
		if (definition.getInjected() == InjectedPropertyPolicy.INTERNAL
				&& !forced) {
			logger.error("In " + this + ", attribute \"" + attr
					+ "\" is an internal field attribute and cannot be set.");
			return null;
		}

		/*
		 * if the same attribute exists above, it is a redefinition.
		 */
		ComponentImpl group = (ComponentImpl) this.getGroup();
		if (group != null && group.get(attr) != null) {
			// If the attribute above is the default value, it is allowed to
			// change it
			if (!group.get(attr).equals(definition.getDefaultValue())) {
				logger.error("In " + this + ", cannot redefine attribute \""
						+ attr + "\"");
				return null;
			}
		}

		return definition;
	}

}
